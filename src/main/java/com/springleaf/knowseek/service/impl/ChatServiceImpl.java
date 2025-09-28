package com.springleaf.knowseek.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.springleaf.knowseek.mapper.mysql.FileUploadMapper;
import com.springleaf.knowseek.mapper.mysql.KnowledgeBaseMapper;
import com.springleaf.knowseek.mapper.mysql.UserMapper;
import com.springleaf.knowseek.mapper.pgvector.VectorRecordMapper;
import com.springleaf.knowseek.model.bo.VectorRecordSearchBO;
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
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
        SseEmitter emitter = new SseEmitter(60000L);

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
                VectorRecordSearchBO searchBO = new VectorRecordSearchBO();
                searchBO.setUserId(currentUserId);
                searchBO.setKnowledgeBaseId(primaryKnowledgeBaseId);
                searchBO.setTopK(3);
                searchBO.setQueryVector(queryVector);

                // 执行检索
                List<VectorRecord> relevantRecords = vectorRecordMapper.findTopKByEmbedding(searchBO);

                // 构建知识上下文并加入 messages
                if (!relevantRecords.isEmpty()) {
                    // 按 file_id 分组 chunks
                    Map<Long, List<VectorRecord>> recordsByFile = relevantRecords.stream()
                            .collect(Collectors.groupingBy(VectorRecord::getFileId));

                    StringBuilder knowledgeContext = new StringBuilder();

                    // 为每个文件单独标注
                    for (Map.Entry<Long, List<VectorRecord>> entry : recordsByFile.entrySet()) {
                        Long fileId = entry.getKey();
                        List<VectorRecord> chunks = entry.getValue();

                        // 需要根据 fileId 查询文件名（建议缓存或批量查）
                        String fileName = getFileDisplayName(fileId);

                        knowledgeContext.append("【来源文件: ").append(fileName).append("】\n");
                        for (VectorRecord record : chunks) {
                            knowledgeContext.append("- ").append(record.getChunkText()).append("\n");
                        }
                        knowledgeContext.append("\n");
                    }

                    log.info("知识上下文: {}", knowledgeContext);
                    String kbName = knowledgeBaseMapper.getNameById(primaryKnowledgeBaseId);
                    String systemPrompt = PromptSecurityGuardUtil.buildSecureSystemPrompt(knowledgeContext.toString(), kbName);
                    messages.add(new SystemMessage(systemPrompt));
                }
            }

            // 添加用户真实提问
            messages.add(new UserMessage(userQuestion));

            // 保存用户消息到数据库
            saveUserMessage(sessionId, requestDTO.getMessage(), currentUserId);

            // 调用AI流式获取回复
            Prompt prompt = new Prompt(messages);
            Flux<ChatResponse> responseFlux = chatModel.stream(prompt);

            StringBuilder fullResponse = new StringBuilder();

            // 添加连接超时和错误处理
            emitter.onTimeout(() -> {
                // 连接超时时也保存已接收的内容
                if (!fullResponse.isEmpty()) {
                    saveAssistantMessage(sessionId, fullResponse.toString(), currentUserId);
                }
            });

            emitter.onError((throwable) -> {
                // 连接错误时也保存已接收的内容
                if (!fullResponse.isEmpty()) {
                    saveAssistantMessage(sessionId, fullResponse.toString(), currentUserId);
                }
            });

            emitter.onCompletion(() -> {
                // 连接完成时保存内容（包括用户主动断开）
                if (!fullResponse.isEmpty()) {
                    saveAssistantMessage(sessionId, fullResponse.toString(), currentUserId);

                    // 如果是用户的第一次提问，生成会话标题
                    if (isFirstMessage) {
                        generateAndUpdateSessionTitle(sessionId, requestDTO.getMessage(), currentUserId);
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
                            fullResponse.setLength(0); // 清空已拼接内容
                            fullResponse.append(content);
                        }

                        ChatResponseVO responseVO = new ChatResponseVO();
                        responseVO.setMessage(content);
                        responseVO.setSessionId(sessionId);
                        responseVO.setTimestamp(LocalDateTime.now());
                        responseVO.setRole("assistant");
                        responseVO.setFromKnowledgeBase(false);

                        emitter.send(SseEmitter.event().data(responseVO));
                    } catch (IOException e) {
                        log.error("发送SSE消息失败", e);
                        emitter.completeWithError(e);
                    }
                },
                error -> {
                    log.error("流式对话失败", error);
                    // 错误时也尝试保存已有内容
                    if (!fullResponse.isEmpty()) {
                        saveAssistantMessage(sessionId, fullResponse.toString(), currentUserId);
                    }
                    emitter.completeWithError(error);
                },
                    emitter::complete
            );
        } catch (Exception e) {
            log.error("启动流式对话失败", e);
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
}