package com.springleaf.knowseek.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.springleaf.knowseek.model.dto.ChatRequestDTO;
import com.springleaf.knowseek.model.vo.ChatResponseVO;
import com.springleaf.knowseek.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final DashScopeChatModel chatModel;

    private final StringRedisTemplate stringRedisTemplate;

    private final ObjectMapper objectMapper;

    private static final String CONVERSATION_KEY_PREFIX = "conversation:user:";
    private static final long CONVERSATION_EXPIRE_HOURS = 24;

    @Override
    public ChatResponseVO chat(ChatRequestDTO requestDTO) {
        try {
            long userId = StpUtil.getLoginIdAsLong();
            List<Message> messages = getConversationHistory(userId, requestDTO.getConversationId());
            messages.add(new UserMessage(requestDTO.getMessage()));

            Prompt prompt = new Prompt(messages);
            ChatResponse response = chatModel.call(prompt);
            
            String assistantResponse = response.getResult().getOutput().getText();
            messages.add(new AssistantMessage(assistantResponse));

            String conversationId = saveConversation(userId, requestDTO.getConversationId(), messages);
            
            ChatResponseVO responseVO = new ChatResponseVO();
            responseVO.setMessage(assistantResponse);
            responseVO.setConversationId(conversationId);
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
            long userId = StpUtil.getLoginIdAsLong();
            List<Message> messages = getConversationHistory(userId, requestDTO.getConversationId());
            messages.add(new UserMessage(requestDTO.getMessage()));

            Prompt prompt = new Prompt(messages);
            Flux<ChatResponse> responseFlux = chatModel.stream(prompt);
            
            StringBuilder fullResponse = new StringBuilder();
            String conversationId = requestDTO.getConversationId() != null ? 
                requestDTO.getConversationId() : UUID.randomUUID().toString();
            
            responseFlux.subscribe(
                chatResponse -> {
                    try {
                        String content = chatResponse.getResult().getOutput().getText();
                        fullResponse.append(content);
                        
                        ChatResponseVO responseVO = new ChatResponseVO();
                        responseVO.setMessage(content);
                        responseVO.setConversationId(conversationId);
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
                    emitter.completeWithError(error);
                },
                () -> {
                    messages.add(new AssistantMessage(fullResponse.toString()));
                    saveConversation(userId, conversationId, messages);
                    emitter.complete();
                }
            );
        } catch (Exception e) {
            log.error("启动流式对话失败", e);
            emitter.completeWithError(e);
        }
        
        return emitter;
    }

    @Override
    public void clearConversation(String conversationId) {
        if (conversationId != null) {
            long userId = StpUtil.getLoginIdAsLong();
            stringRedisTemplate.delete(CONVERSATION_KEY_PREFIX + userId + ":" + conversationId);
        }
    }

    private List<Message> getConversationHistory(long userId, String conversationId) {
        if (conversationId == null) {
            return new ArrayList<>();
        }

        String key = CONVERSATION_KEY_PREFIX + userId + ":" + conversationId;
        String historyJson = stringRedisTemplate.opsForValue().get(key);

        if (historyJson == null) {
            return new ArrayList<>();
        }

        try {
            // 使用 ObjectMapper 反序列化 JSON 字符串
            List<Message> messages = objectMapper.readValue(historyJson, new TypeReference<List<Message>>() {});
            return messages != null ? messages : new ArrayList<>();
        } catch (JsonProcessingException e) {
            log.error("反序列化对话历史失败", e);
            return new ArrayList<>();
        }
    }

    private String saveConversation(long userId, String conversationId, List<Message> messages) {
        if (conversationId == null) {
            conversationId = UUID.randomUUID().toString();
        }

        try {
            String key = CONVERSATION_KEY_PREFIX + userId + ":" + conversationId;
            // 将 Message 列表序列化为 JSON 字符串
            String messagesJson = objectMapper.writeValueAsString(messages);

            stringRedisTemplate.opsForValue().set(
                    key,
                    messagesJson,
                    CONVERSATION_EXPIRE_HOURS,
                    TimeUnit.HOURS
            );
        } catch (JsonProcessingException e) {
            log.error("序列化对话历史失败", e);
            throw new RuntimeException("保存对话失败", e);
        }

        return conversationId;
    }
}