package com.springleaf.knowseek.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.springleaf.knowseek.constans.RagConstant;
import com.springleaf.knowseek.mapper.mysql.FileUploadMapper;
import com.springleaf.knowseek.mapper.mysql.KnowledgeBaseMapper;
import com.springleaf.knowseek.mapper.mysql.UserMapper;
import com.springleaf.knowseek.mapper.pgvector.VectorRecordMapper;
import com.springleaf.knowseek.model.bo.VectorRecordSearchBO;
import com.springleaf.knowseek.model.bo.VectorRecordWithDistanceBO;
import com.springleaf.knowseek.model.dto.MessageCreateDTO;
import com.springleaf.knowseek.model.dto.ChatRequestDTO;
import com.springleaf.knowseek.model.dto.SessionCreateDTO;
import com.springleaf.knowseek.model.dto.SessionUpdateDTO;
import com.springleaf.knowseek.model.entity.VectorRecord;
import com.springleaf.knowseek.model.vo.MessageVO;
import com.springleaf.knowseek.model.vo.ChatResponseVO;
import com.springleaf.knowseek.model.vo.SessionVO;
import com.springleaf.knowseek.service.MessageService;
import com.springleaf.knowseek.service.ChatService;
import com.springleaf.knowseek.service.SessionService;
import com.springleaf.knowseek.utils.PromptSecurityGuardUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final DashScopeChatModel chatModel;
    private final SessionService sessionService;
    private final MessageService messageService;
    private final VectorRecordMapper vectorRecordMapper;
    private final UserMapper userMapper;
    private final EmbeddingModel embeddingModel;
    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final FileUploadMapper fileUploadMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatResponseVO chat(ChatRequestDTO requestDTO) {
        try {
            Long currentUserId = StpUtil.getLoginIdAsLong();

            // 获取或创建会话
            SessionVO sessionVO = getOrCreateSession(requestDTO.getSessionId());
            Long sessionId = sessionVO.getId();

            // 获取会话历史消息
            List<Message> messages = getSessionMessages(sessionId);
            boolean isFirstMessage = messages.isEmpty();

            // 添加用户消息
            messages.add(new UserMessage(requestDTO.getMessage()));

            // 保存用户消息到数据库
            saveUserMessage(sessionId, requestDTO.getMessage(), currentUserId);

            // 调用AI获取回复
            Prompt prompt = new Prompt(messages);
            ChatResponse response = chatModel.call(prompt);
            String assistantResponse = response.getResult().getOutput().getText();

            // 保存AI回复到数据库
            saveAssistantMessage(sessionId, assistantResponse, currentUserId);

            // 如果是用户的第一次提问，生成会话标题
            if (isFirstMessage) {
                generateAndUpdateSessionTitle(sessionId, requestDTO.getMessage(), currentUserId);
            }

            // 构建响应
            ChatResponseVO responseVO = new ChatResponseVO();
            responseVO.setMessage(assistantResponse);
            responseVO.setSessionId(sessionId);
            responseVO.setTimestamp(LocalDateTime.now());
            responseVO.setRole("assistant");
            responseVO.setFromKnowledgeBase(false);

            return responseVO;
        } catch (Exception e) {
            log.error("AI对话失败", e);
            throw new RuntimeException("AI对话失败: " + e.getMessage());
        }
    }

    @Override
    public SseEmitter streamChat(ChatRequestDTO requestDTO) {
        SseEmitter emitter = new SseEmitter(600000L);

        // 使用 AtomicReference 来持有 Disposable 对象，以便在回调中访问
        final AtomicReference<Disposable> subscriptionRef = new AtomicReference<>();
        final StringBuilder fullResponse = new StringBuilder();

        try {
            Long currentUserId = StpUtil.getLoginIdAsLong();

            // 获取或创建会话
            SessionVO sessionVO = getOrCreateSession(requestDTO.getSessionId());
            Long sessionId = sessionVO.getId();

            // 获取会话历史消息
            List<Message> messages = getSessionMessages(sessionId);
            boolean isFirstMessage = messages.isEmpty();

            String userQuestion = requestDTO.getMessage();

            // 输入安全检测
            if (PromptSecurityGuardUtil.isInputMalicious(userQuestion)) {
                // 直接返回拦截响应，不调用模型
                ChatResponseVO blockedResponse = new ChatResponseVO();
                blockedResponse.setMessage(PromptSecurityGuardUtil.INJECTION_BLOCKED_RESPONSE);
                blockedResponse.setSessionId(sessionId);
                blockedResponse.setTimestamp(LocalDateTime.now());
                blockedResponse.setRole("assistant");
                blockedResponse.setFromKnowledgeBase(false);

                try {
                    emitter.send(SseEmitter.event().data(blockedResponse));
                    emitter.complete();
                } catch (IOException e) {
                    log.error("Failed to send blocked response", e);
                    emitter.completeWithError(e);
                }
                return emitter;
            }

            // RAG 检索知识
            Boolean useKnowledgeBase = requestDTO.getUseKnowledgeBase();
            if (useKnowledgeBase != null && useKnowledgeBase) {

                float[] queryVector = embeddingModel.embed(userQuestion);
                Long primaryKnowledgeBaseId = userMapper.selectById(currentUserId).getPrimaryKnowledgeBaseId();

                // 即使不检索，也要准备 kbName
                String kbName = knowledgeBaseMapper.getNameById(primaryKnowledgeBaseId);
                String knowledgeContext = ""; // 默认为空

                VectorRecordSearchBO searchBO = new VectorRecordSearchBO();
                searchBO.setUserId(currentUserId);
                searchBO.setKnowledgeBaseId(primaryKnowledgeBaseId);
                searchBO.setTopK(RagConstant.TOPK);
                searchBO.setQueryVector(queryVector);

                // 执行检索
                List<VectorRecord> relevantRecords = performEnhancedRetrieval(searchBO);

                // 构建知识上下文并加入 messages
                if (!relevantRecords.isEmpty()) {
                    // 按 file_id 分组 chunks
                    Map<Long, List<VectorRecord>> recordsByFile = relevantRecords.stream()
                            .collect(Collectors.groupingBy(VectorRecord::getFileId));

                    StringBuilder kbBuilder = new StringBuilder();
                    // 为每个文件单独标注
                    for (Map.Entry<Long, List<VectorRecord>> entry : recordsByFile.entrySet()) {
                        String fileName = getFileDisplayName(entry.getKey());

                        // 提取当前文件中所有 chunk 的 chunkIndex
                        String chunkIndices = entry.getValue().stream()
                                .map(record -> String.valueOf(record.getChunkIndex()))
                                .collect(Collectors.joining(","));

                        // 拼接来源文件名 + chunk 索引
                        kbBuilder.append("【来源文件: ").append(fileName);
                        if (!chunkIndices.isEmpty()) {
                            kbBuilder.append(" (chunks: ").append(chunkIndices).append(")");
                        }
                        kbBuilder.append("】\n");

                        // 拼接 chunk 文本内容
                        for (VectorRecord record : entry.getValue()) {
                            kbBuilder.append("- ").append(record.getChunkText()).append("\n");
                        }
                        kbBuilder.append("\n");
                    }

                    knowledgeContext = kbBuilder.toString();
                    log.info("检索到的相关知识：{}", knowledgeContext);
                }
                // 无论是否有知识，都添加 system prompt
                String systemPrompt = PromptSecurityGuardUtil.buildSecureSystemPrompt(knowledgeContext, kbName);
                messages.add(new SystemMessage(systemPrompt));
            }

            // 添加用户真实提问
            messages.add(new UserMessage(userQuestion));

            // 保存用户消息到数据库
            saveUserMessage(sessionId, requestDTO.getMessage(), currentUserId);

            // 调用AI流式获取回复
            Prompt prompt = new Prompt(messages);
            Flux<ChatResponse> responseFlux = chatModel.stream(prompt);

            // 这个任务会在 SSE 连接终止时执行
            Runnable cleanupAndSave = () -> {
                log.info("SSE connection for session {} is closing. Disposing AI model subscription.", sessionId);
                Disposable subscription = subscriptionRef.get();
                if (subscription != null && !subscription.isDisposed()) {
                    subscription.dispose(); // 取消对AI模型的订阅
                }

                // 保存已收到的部分或全部回复
                if (!fullResponse.isEmpty()) {
                    saveAssistantMessage(sessionId, fullResponse.toString(), currentUserId);
                    if (isFirstMessage) {
                        generateAndUpdateSessionTitle(sessionId, requestDTO.getMessage(), currentUserId);
                    }
                }
            };

            // 将清理任务注册到 SseEmitter 的终止事件上
            emitter.onCompletion(cleanupAndSave);
            emitter.onTimeout(cleanupAndSave);
            emitter.onError((throwable) -> {
                log.error("SSE emitter for session {} encountered an error.", sessionId, throwable);
                // onError 也会隐式触发 onCompletion, 但为了明确和记录日志，这里单独处理
                cleanupAndSave.run();
            });

            // 订阅AI模型流，并将订阅对象保存到 AtomicReference
            Disposable disposable = responseFlux.subscribe(
                    // 1. onNext: 接收到新数据块时的处理
                    chatResponse -> {
                        try {
                            String content = chatResponse.getResult().getOutput().getText();
                            if (content == null) return; // 有时模型会返回 null content

                            fullResponse.append(content);

                            // 输出安全检测
                            if (PromptSecurityGuardUtil.isOutputUnsafe(content)) {
                                log.warn("Detected unsafe model output, replacing with fallback response.");
                                content = PromptSecurityGuardUtil.SAFE_FALLBACK_RESPONSE;
                                fullResponse.setLength(0);
                                fullResponse.append(content);
                            }

                            ChatResponseVO responseVO = new ChatResponseVO();
                            responseVO.setMessage(content);
                            responseVO.setSessionId(sessionId);
                            responseVO.setTimestamp(LocalDateTime.now());
                            responseVO.setRole("assistant");
                            responseVO.setFromKnowledgeBase(useKnowledgeBase);

                            // 发送事件到客户端
                            emitter.send(SseEmitter.event().data(responseVO));

                            // 如果检测到不安全内容，则发送后立即终止
                            if (PromptSecurityGuardUtil.SAFE_FALLBACK_RESPONSE.equals(content)) {
                                emitter.complete();
                            }

                        } catch (IOException e) {
                            // 这个异常通常在客户端关闭连接后，后端再尝试 send 时抛出
                            log.warn("Failed to send SSE message to client for session {}, likely client disconnected.", sessionId);
                            // 不需要手动调用 cleanupAndSave.run()，因为 emitter 的 onError/onCompletion 会被自动触发
                            // 直接 re-throw 或者包装成一个 RuntimeException 来终止流
                            throw new RuntimeException("Client disconnected", e);
                        }
                    },
                    // 2. onError: AI模型流发生错误时的处理
                    error -> {
                        log.error("AI model stream for session {} failed.", sessionId, error);
                        emitter.completeWithError(error); // 这会触发 emitter 的 onCompletion/onError 回调
                    },
                    // 3. onComplete: AI模型流正常结束时的处理
                    () -> {
                        log.info("AI model stream for session {} completed successfully.", sessionId);
                        emitter.complete(); // 这会触发 emitter 的 onCompletion 回调
                    }
            );

            // 将创建的订阅关系存入引用，以便回调函数可以访问并取消它
            subscriptionRef.set(disposable);

        } catch (Exception e) {
            log.error("Failed to initialize stream chat for session {}.", requestDTO.getSessionId(), e);
            emitter.completeWithError(e);
        }

        return emitter;
    }

    private SessionVO getOrCreateSession(Long sessionId) {
        if (sessionId == null) {
            // 创建新会话
            SessionCreateDTO createDTO = new SessionCreateDTO();
            createDTO.setSessionName("新对话");
            return sessionService.createSession(createDTO);
        } else {
            // 获取已有会话
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

    /**
     * 生成并更新会话标题
     * @param sessionId 会话ID
     * @param userMessage 用户的第一次提问
     */
    private void generateAndUpdateSessionTitle(Long sessionId, String userMessage, Long userId) {
        try {
            // 构建标题生成的提示词
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

            // 调用AI生成标题
            Prompt prompt = new Prompt(new UserMessage(titlePrompt));
            ChatResponse response = chatModel.call(prompt);
            String generatedTitle = response.getResult().getOutput().getText().trim();

            // 更新会话标题
            SessionUpdateDTO updateDTO = new SessionUpdateDTO();
            updateDTO.setId(sessionId);
            updateDTO.setSessionName(generatedTitle);
            sessionService.updateSession(updateDTO, userId);

            log.info("已为会话 {} 生成标题: {}", sessionId, generatedTitle);
        } catch (Exception e) {
            log.error("生成会话标题失败，使用默认标题", e);
            // 如果生成失败，使用用户消息的前10个字符作为标题
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

    /**
     * 召回流程：
     * 先从数据库全局召回满足相似度阈值的 TopK 个候选分片，按文件分组后筛选出最相关的的前 MAX_FILES 个文件，最后在这些文件中各保留前 CHUNKS_PER_FILE 个最匹配的分片。
     */
    private List<VectorRecord> performEnhancedRetrieval(VectorRecordSearchBO searchBO) {
        // Step 1: 召回候选（带 distance）
        List<VectorRecordWithDistanceBO> candidates = vectorRecordMapper.findTopKByEmbeddingWithDistance(searchBO);

        if (candidates.isEmpty()) {
            return Collections.emptyList();
        }

        // Step 2: 按 file_id 分组
        Map<Long, List<VectorRecordWithDistanceBO>> groupedByFile = candidates.stream()
                .collect(Collectors.groupingBy(VectorRecordWithDistanceBO::getFileId));

        // Step 3: 为每个文件计算“代表分数”——取最小 distance（即最相关 chunk 的距离）
        List<FileRelevance> fileRelevances = new ArrayList<>();
        for (Map.Entry<Long, List<VectorRecordWithDistanceBO>> entry : groupedByFile.entrySet()) {
            List<VectorRecordWithDistanceBO> chunks = entry.getValue();
            // 找该文件中最相关的 chunk 的 distance
            double bestDistance = chunks.stream()
                    .mapToDouble(VectorRecordWithDistanceBO::getDistance)
                    .min()
                    .orElse(Double.MAX_VALUE);
            fileRelevances.add(new FileRelevance(entry.getKey(), bestDistance, chunks));
        }

        // Step 4: 按文件相关性排序（distance 越小越相关）
        fileRelevances.sort(Comparator.comparing(FileRelevance::getBestDistance));

        // Step 5: 选择最相关的 MAX_FILES 个文件
        List<VectorRecord> selected = new ArrayList<>();

        for (int i = 0; i < Math.min(RagConstant.MAX_FILES, fileRelevances.size()); i++) {
            List<VectorRecordWithDistanceBO> chunks = fileRelevances.get(i).getChunks();
            // Step 6: 对该文件内的 chunks 按 distance 排序，取 CHUNKS_PER_FILE 个分片
            List<VectorRecordWithDistanceBO> topChunks = chunks.stream()
                    .sorted(Comparator.comparing(VectorRecordWithDistanceBO::getDistance))
                    .limit(RagConstant.CHUNKS_PER_FILE)
                    .toList();

            // 转换为原始 VectorRecord（去掉 distance）
            selected.addAll(topChunks.stream().map(this::convertToVectorRecord).toList());
        }

        return selected;
    }

    // 辅助类
    @Data
    @AllArgsConstructor
    private static class FileRelevance {
        private Long fileId;
        private double bestDistance;
        private List<VectorRecordWithDistanceBO> chunks;
    }

    // 转换方法
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