package com.springleaf.knowseek.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.springleaf.knowseek.mapper.mysql.FileUploadMapper;
import com.springleaf.knowseek.mapper.mysql.KnowledgeBaseMapper;
import com.springleaf.knowseek.mapper.mysql.UserMapper;
import com.springleaf.knowseek.mapper.pgvector.VectorRecordMapper;
import com.springleaf.knowseek.model.bo.VectorRecordSearchBO;
import com.springleaf.knowseek.model.bo.VectorRecordWithDistanceBO;
import com.springleaf.knowseek.model.dto.*;
import com.springleaf.knowseek.model.entity.ModelConfig;
import com.springleaf.knowseek.model.entity.VectorRecord;
import com.springleaf.knowseek.model.vo.ChatResponseVO;
import com.springleaf.knowseek.model.vo.MessageVO;
import com.springleaf.knowseek.model.vo.SessionVO;
import com.springleaf.knowseek.service.ChatService;
import com.springleaf.knowseek.service.MessageService;
import com.springleaf.knowseek.service.SessionService;
import com.springleaf.knowseek.service.handler.ModelSelectionHandler;
import com.springleaf.knowseek.service.strategy.AIModelStrategy;
import com.springleaf.knowseek.utils.PromptSecurityGuardUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.messages.*;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 智能AI聊天服务实现 - 整合多种设计模式的核心服务
 *
 * 设计模式应用：
 * - 策略模式：动态选择不同的AI模型进行对话
 * - 工厂模式：通过工厂管理器创建模型实例
 * - 责任链模式：智能的模型选择逻辑
 * - 建造者模式：构建复杂的模型配置
 *
 * 核心功能：
 * 1. 支持用户指定模型或智能模型选择
 * 2. 保留完整的RAG知识库检索能力
 * 3. 支持同步和流式对话
 * 4. 完整的安全检测和错误处理
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IntelligentChatServiceImpl implements ChatService {

    // 核心组件注入
    private final ModelSelectionHandler modelSelectionChain;  // 责任链模式
    private final SessionService sessionService;
    private final MessageService messageService;
    // RAG相关组件
    private final VectorRecordMapper vectorRecordMapper;
    private final UserMapper userMapper;
    private final EmbeddingModel embeddingModel;
    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final FileUploadMapper fileUploadMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatResponseVO chat(ChatRequestDTO requestDTO) {
        String requestId = UUID.randomUUID().toString();
        Long currentUserId = StpUtil.getLoginIdAsLong();

        try {
            // 1. 构建配置（建造者模式）
            ModelConfig config = ModelConfig.builder()
                    .fromChatRequest(requestDTO)
                    .withDefaults("qwen-plus")
                    .build();

            // 2. 模型选择（责任链模式）
            ModelSelectionRequest selectionRequest = ModelSelectionRequest.builder()
                    .userId(currentUserId)
                    .specifiedModel(requestDTO.getModelName())
                    .messageContent(requestDTO.getMessage())
                    .useKnowledgeBase(requestDTO.getUseKnowledgeBase())
                    .build();

            ModelSelectionResult selectionResult = modelSelectionChain.handle(selectionRequest);
            if (!selectionResult.isSuccess()) {
                throw new RuntimeException("无可用AI模型: " + selectionResult.getErrorMessage());
            }

            AIModelStrategy selectedModel = selectionResult.getStrategy();
            log.info("选中模型: {} ({}), 选择原因: {}",
                    selectedModel.getModelName(),
                    selectedModel.getProvider(),
                    selectionResult.getReason());

            // 3. 获取或创建会话
            SessionVO sessionVO = getOrCreateSession(requestDTO.getSessionId());
            List<Message> messages = getSessionMessages(sessionVO.getId());
            boolean isFirstMessage = messages.isEmpty();

            // 4. 处理知识库检索（如果需要）
            if (Boolean.TRUE.equals(requestDTO.getUseKnowledgeBase())) {
                handleKnowledgeBaseRetrieval(requestDTO.getMessage(), currentUserId, messages);
            }

            // 5. 添加用户消息
            messages.add(new UserMessage(requestDTO.getMessage()));

            // 6. 保存用户消息到数据库
            saveUserMessage(sessionVO.getId(), requestDTO.getMessage(), currentUserId);

            // 7. 调用AI模型（策略模式）
            long startTime = System.currentTimeMillis();
            ChatResponse response = selectedModel.chat(messages, config);
            long duration = System.currentTimeMillis() - startTime;

            String assistantResponse = response.getResult().getOutput().getText();

            // 8. 保存AI回复到数据库
            saveAssistantMessage(sessionVO.getId(), assistantResponse, currentUserId);

            // 9. 如果是用户的第一次提问，生成会话标题
            if (isFirstMessage) {
                generateAndUpdateSessionTitle(sessionVO.getId(), requestDTO.getMessage(), currentUserId, selectedModel);
            }

            // 10. 构建响应
            return ChatResponseVO.builder()
                    .message(assistantResponse)
                    .sessionId(sessionVO.getId())
                    .timestamp(LocalDateTime.now())
                    .role("assistant")
                    .fromKnowledgeBase(Boolean.TRUE.equals(requestDTO.getUseKnowledgeBase()))
                    .modelName(selectedModel.getModelName())
                    .provider(selectedModel.getProvider())
                    .selectionReason(selectionResult.getReason())
                    .build();

        } catch (Exception e) {
            log.error("AI对话失败，请求ID: {}", requestId, e);
            throw new RuntimeException("AI对话失败: " + e.getMessage(), e);
        }
    }

    @Override
    public SseEmitter streamChat(ChatRequestDTO requestDTO) {
        SseEmitter emitter = new SseEmitter(60000L);
        String requestId = UUID.randomUUID().toString();

        try {
            Long currentUserId = StpUtil.getLoginIdAsLong();

            // 1. 构建配置
            ModelConfig config = ModelConfig.builder()
                    .fromChatRequest(requestDTO)
                    .withDefaults("qwen-plus")
                    .build();

            // 2. 模型选择
            ModelSelectionRequest selectionRequest = ModelSelectionRequest.builder()
                    .userId(currentUserId)
                    .specifiedModel(requestDTO.getModelName())
                    .messageContent(requestDTO.getMessage())
                    .useKnowledgeBase(requestDTO.getUseKnowledgeBase())
                    .build();

            ModelSelectionResult selectionResult = modelSelectionChain.handle(selectionRequest);
            if (!selectionResult.isSuccess()) {
                emitter.completeWithError(new RuntimeException("无可用AI模型: " + selectionResult.getErrorMessage()));
                return emitter;
            }

            AIModelStrategy selectedModel = selectionResult.getStrategy();

            // 3. 获取或创建会话
            SessionVO sessionVO = getOrCreateSession(requestDTO.getSessionId());
            List<Message> messages = getSessionMessages(sessionVO.getId());
            boolean isFirstMessage = messages.isEmpty();

            String userQuestion = requestDTO.getMessage();

            // 4. 输入安全检测
            if (PromptSecurityGuardUtil.isInputMalicious(userQuestion)) {
                ChatResponseVO blockedResponse = ChatResponseVO.builder()
                        .message(PromptSecurityGuardUtil.INJECTION_BLOCKED_RESPONSE)
                        .sessionId(sessionVO.getId())
                        .timestamp(LocalDateTime.now())
                        .role("assistant")
                        .fromKnowledgeBase(false)
                        .modelName(selectedModel.getModelName())
                        .provider(selectedModel.getProvider())
                        .selectionReason("安全拦截")
                        .build();

                try {
                    emitter.send(SseEmitter.event().data(blockedResponse));
                    emitter.complete();
                } catch (IOException e) {
                    log.error("发送安全拦截响应失败", e);
                    emitter.completeWithError(e);
                }
                return emitter;
            }

            // 5. 处理知识库检索
            if (Boolean.TRUE.equals(requestDTO.getUseKnowledgeBase())) {
                handleKnowledgeBaseRetrieval(userQuestion, currentUserId, messages);
            }

            // 6. 添加用户消息
            messages.add(new UserMessage(userQuestion));

            // 7. 保存用户消息到数据库
            saveUserMessage(sessionVO.getId(), requestDTO.getMessage(), currentUserId);

            // 8. 调用AI流式获取回复
            Flux<ChatResponse> responseFlux = selectedModel.streamChat(messages, config);

            StringBuilder fullResponse = new StringBuilder();

            // 添加连接处理
            emitter.onTimeout(() -> {
                if (!fullResponse.isEmpty()) {
                    saveAssistantMessage(sessionVO.getId(), fullResponse.toString(), currentUserId);
                }
            });

            emitter.onError((throwable) -> {
                if (!fullResponse.isEmpty()) {
                    saveAssistantMessage(sessionVO.getId(), fullResponse.toString(), currentUserId);
                }
            });

            emitter.onCompletion(() -> {
                if (!fullResponse.isEmpty()) {
                    saveAssistantMessage(sessionVO.getId(), fullResponse.toString(), currentUserId);
                    if (isFirstMessage) {
                        generateAndUpdateSessionTitle(sessionVO.getId(), requestDTO.getMessage(), currentUserId, selectedModel);
                    }
                }
            });

            responseFlux.subscribe(
                    chatResponse -> {
                        try {
                            String content = chatResponse.getResult().getOutput().getText();
                            fullResponse.append(content);

                            // 输出安全检测
                            if (PromptSecurityGuardUtil.isOutputUnsafe(content)) {
                                log.warn("检测到不安全的模型输出，替换为安全响应。");
                                content = PromptSecurityGuardUtil.SAFE_FALLBACK_RESPONSE;
                                fullResponse.setLength(0);
                                fullResponse.append(content);
                            }

                            ChatResponseVO responseVO = ChatResponseVO.builder()
                                    .message(content)
                                    .sessionId(sessionVO.getId())
                                    .timestamp(LocalDateTime.now())
                                    .role("assistant")
                                    .fromKnowledgeBase(Boolean.TRUE.equals(requestDTO.getUseKnowledgeBase()))
                                    .modelName(selectedModel.getModelName())
                                    .provider(selectedModel.getProvider())
                                    .selectionReason(selectionResult.getReason())
                                    .build();

                            emitter.send(SseEmitter.event().data(responseVO));
                        } catch (IOException e) {
                            log.error("发送SSE消息失败", e);
                            emitter.completeWithError(e);
                        }
                    },
                    error -> {
                        log.error("流式对话失败", error);
                        if (!fullResponse.isEmpty()) {
                            saveAssistantMessage(sessionVO.getId(), fullResponse.toString(), currentUserId);
                        }
                        emitter.completeWithError(error);
                    },
                    emitter::complete
            );
        } catch (Exception e) {
            log.error("启动流式对话失败，请求ID: {}", requestId, e);
            emitter.completeWithError(e);
        }

        return emitter;
    }

    // 以下是私有辅助方法，保持原有逻辑不变
    private SessionVO getOrCreateSession(Long sessionId) {
        if (sessionId == null) {
            SessionCreateDTO createDTO = new SessionCreateDTO();
            createDTO.setSessionName("新对话");
            return sessionService.createSession(createDTO);
        } else {
            return sessionService.getSessionById(sessionId);
        }
    }

    private List<Message> getSessionMessages(Long sessionId) {
        List<MessageVO> messageVOs = messageService.getMessagesBySessionId(sessionId);
        List<Message> messages = new ArrayList<>();

        for (MessageVO messageVO : messageVOs) {
            if ("user".equals(messageVO.getRole())) {
                messages.add(new UserMessage(messageVO.getContent()));
            } else if ("assistant".equals(messageVO.getRole())) {
                messages.add(new AssistantMessage(messageVO.getContent()));
            }
        }

        return messages;
    }

    private void handleKnowledgeBaseRetrieval(String userQuestion, Long currentUserId, List<Message> messages) {
        try {
            float[] queryVector = embeddingModel.embed(userQuestion);
            Long primaryKnowledgeBaseId = userMapper.selectById(currentUserId).getPrimaryKnowledgeBaseId();

            String kbName = knowledgeBaseMapper.getNameById(primaryKnowledgeBaseId);
            String knowledgeContext = "";

            VectorRecordSearchBO searchBO = new VectorRecordSearchBO();
            searchBO.setUserId(currentUserId);
            searchBO.setKnowledgeBaseId(primaryKnowledgeBaseId);
            searchBO.setQueryVector(queryVector);

            List<VectorRecord> relevantRecords = performEnhancedRetrieval(searchBO, queryVector);

            if (!relevantRecords.isEmpty()) {
                Map<Long, List<VectorRecord>> recordsByFile = relevantRecords.stream()
                        .collect(Collectors.groupingBy(VectorRecord::getFileId));

                StringBuilder kbBuilder = new StringBuilder();
                for (Map.Entry<Long, List<VectorRecord>> entry : recordsByFile.entrySet()) {
                    String fileName = getFileDisplayName(entry.getKey());
                    kbBuilder.append("【来源文件: ").append(fileName).append("】\n");
                    for (VectorRecord record : entry.getValue()) {
                        kbBuilder.append("- ").append(record.getChunkText()).append("\n");
                    }
                    kbBuilder.append("\n");
                }

                knowledgeContext = kbBuilder.toString();
                log.info("检索到的相关知识：{}", knowledgeContext);
            }

            String systemPrompt = PromptSecurityGuardUtil.buildSecureSystemPrompt(knowledgeContext, kbName);
            messages.add(new SystemMessage(systemPrompt));
        } catch (Exception e) {
            log.error("知识库检索失败", e);
        }
    }

    private void saveUserMessage(Long sessionId, String content, Long currentUserId) {
        MessageCreateDTO createDTO = new MessageCreateDTO();
        createDTO.setSessionId(sessionId);
        createDTO.setRole("user");
        createDTO.setContent(content);
        messageService.createMessage(createDTO, currentUserId);
    }

    private void saveAssistantMessage(Long sessionId, String content, Long currentUserId) {
        MessageCreateDTO createDTO = new MessageCreateDTO();
        createDTO.setSessionId(sessionId);
        createDTO.setRole("assistant");
        createDTO.setContent(content);
        messageService.createMessage(createDTO, currentUserId);
    }

    private void generateAndUpdateSessionTitle(Long sessionId, String userMessage, Long userId, AIModelStrategy selectedModel) {
        try {
            String titlePrompt = String.format(
                    "请根据用户首次提问，生成一个中文、不超过15字的精准会话标题。要求：\n" +
                            "必须是陈述性短语，非疑问句；\n" +
                            "保留核心动词与对象（如\"撰写\"\"分析\"\"推荐\"）；\n" +
                            "若涉及人名/专有名词，可保留关键术语（如\"Python\"\"雅思\"）；\n" +
                            "避免使用\"我\"\"你\"\"帮\"等主观词；\n" +
                            "若意图模糊，则归纳为通用类别（如\"情绪倾诉\"\"日常闲聊\"）。\n" +
                            "输入：\"%s\"\n" +
                            "输出格式仅输出标题，无需任何其他文字。",
                    userMessage
            );

            List<Message> titleMessages = Arrays.asList(new UserMessage(titlePrompt));
            ModelConfig titleConfig = ModelConfig.builder().withDefaults("qwen-plus").build();
            ChatResponse response = selectedModel.chat(titleMessages, titleConfig);
            String generatedTitle = response.getResult().getOutput().getText().trim();

            SessionUpdateDTO updateDTO = new SessionUpdateDTO();
            updateDTO.setId(sessionId);
            updateDTO.setSessionName(generatedTitle);
            sessionService.updateSession(updateDTO, userId);

            log.info("已为会话 {} 生成标题: {}", sessionId, generatedTitle);
        } catch (Exception e) {
            log.error("生成会话标题失败，使用默认标题", e);
            String fallbackTitle = userMessage.length() > 10 ?
                    userMessage.substring(0, 10) + "..." : userMessage;

            SessionUpdateDTO updateDTO = new SessionUpdateDTO();
            updateDTO.setId(sessionId);
            updateDTO.setSessionName(fallbackTitle);
            sessionService.updateSession(updateDTO, userId);
        }
    }

    private String getFileDisplayName(Long fileId) {
        String fileName = fileUploadMapper.getFileNameById(fileId);
        return StringUtils.defaultIfBlank(fileName, "未知文件_" + fileId);
    }

    private List<VectorRecord> performEnhancedRetrieval(VectorRecordSearchBO searchBO, float[] queryVector) {
        List<VectorRecordWithDistanceBO> candidates = vectorRecordMapper.findTopKByEmbeddingWithDistance(searchBO);

        if (candidates.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, List<VectorRecordWithDistanceBO>> groupedByFile = candidates.stream()
                .collect(Collectors.groupingBy(VectorRecordWithDistanceBO::getFileId));

        List<FileRelevance> fileRelevances = new ArrayList<>();
        for (Map.Entry<Long, List<VectorRecordWithDistanceBO>> entry : groupedByFile.entrySet()) {
            List<VectorRecordWithDistanceBO> chunks = entry.getValue();
            double bestDistance = chunks.stream()
                    .mapToDouble(VectorRecordWithDistanceBO::getDistance)
                    .min()
                    .orElse(Double.MAX_VALUE);
            fileRelevances.add(new FileRelevance(entry.getKey(), bestDistance, chunks));
        }

        fileRelevances.sort(Comparator.comparing(FileRelevance::getBestDistance));

        List<VectorRecord> selected = new ArrayList<>();
        int maxFiles = 2;
        int chunksPerFile = 2;

        for (int i = 0; i < Math.min(maxFiles, fileRelevances.size()); i++) {
            List<VectorRecordWithDistanceBO> chunks = fileRelevances.get(i).getChunks();
            List<VectorRecordWithDistanceBO> topChunks = chunks.stream()
                    .sorted(Comparator.comparing(VectorRecordWithDistanceBO::getDistance))
                    .limit(chunksPerFile)
                    .collect(Collectors.toList());

            selected.addAll(topChunks.stream().map(this::convertToVectorRecord).collect(Collectors.toList()));
        }

        return selected;
    }

    @Data
    @AllArgsConstructor
    private static class FileRelevance {
        private Long fileId;
        private double bestDistance;
        private List<VectorRecordWithDistanceBO> chunks;
    }

    private VectorRecord convertToVectorRecord(VectorRecordWithDistanceBO withDist) {
        VectorRecord record = new VectorRecord();
        record.setId(withDist.getId());
        record.setUserId(withDist.getUserId());
        record.setKnowledgeBaseId(withDist.getKnowledgeBaseId());
        record.setFileId(withDist.getFileId());
        record.setChunkIndex(withDist.getChunkIndex());
        record.setChunkText(withDist.getChunkText());
        record.setEmbedding(withDist.getEmbedding());
        return record;
    }
}