package com.springleaf.knowseek.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.springleaf.knowseek.constans.ChatRedisKeyConstant;
import com.springleaf.knowseek.model.dto.ChatRequestDTO;
import com.springleaf.knowseek.model.dto.CreateConversationDTO;
import com.springleaf.knowseek.model.vo.ChatResponseVO;
import com.springleaf.knowseek.model.vo.ConversationVO;
import com.springleaf.knowseek.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final DashScopeChatModel chatModel;

    private final StringRedisTemplate stringRedisTemplate;

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
            
            // 删除会话数据但保留会话元数据（只清空消息，不删除会话）
            String conversationKey = String.format(ChatRedisKeyConstant.CONVERSATION_KEY_PREFIX, userId, conversationId);
            stringRedisTemplate.delete(conversationKey);
            
            // 更新会话元数据，重置消息计数
            ConversationVO conversation = getConversationMeta(userId, conversationId);
            if (conversation != null) {
                conversation.setMessageCount(0);
                conversation.setLastMessage(null);
                conversation.setLastMessageTime(null);
                
                String metaKey = String.format(ChatRedisKeyConstant.CONVERSATION_META_KEY_PREFIX, userId, conversationId);
                String metaJson = JSON.toJSONString(conversation);
                stringRedisTemplate.opsForValue().set(metaKey, metaJson, CONVERSATION_EXPIRE_HOURS, TimeUnit.HOURS);
            }
        }
    }

    private List<Message> getConversationHistory(long userId, String conversationId) {
        if (conversationId == null) {
            return new ArrayList<>();
        }

        String key = String.format(ChatRedisKeyConstant.CONVERSATION_KEY_PREFIX, userId, conversationId);
        String historyJson = stringRedisTemplate.opsForValue().get(key);

        if (historyJson == null) {
            return new ArrayList<>();
        }

        try {
            // 使用 fastjson2 解析 JSON 数组
            JSONArray jsonArray = JSON.parseArray(historyJson);
            List<Message> messages = new ArrayList<>();
            
            for (int i = 0; i < jsonArray.size(); i++) {
                JSONObject messageObj = jsonArray.getJSONObject(i);
                String messageType = messageObj.getString("messageType");
                String text = messageObj.getString("text");
                
                if ("USER".equals(messageType)) {
                    messages.add(new UserMessage(text));
                } else if ("ASSISTANT".equals(messageType)) {
                    messages.add(new AssistantMessage(text));
                }
            }
            
            return messages;
        } catch (Exception e) {
            log.warn("反序列化对话历史失败，尝试清理旧数据: {}", e.getMessage());
            // 如果反序列化失败，删除这个键并返回空列表
            stringRedisTemplate.delete(key);
            return new ArrayList<>();
        }
    }

    private String saveConversation(long userId, String conversationId, List<Message> messages) {
        if (conversationId == null) {
            conversationId = UUID.randomUUID().toString();
        }

        try {
            String key = String.format(ChatRedisKeyConstant.CONVERSATION_KEY_PREFIX, userId, conversationId);
            // 使用 fastjson2 将 Message 列表序列化为 JSON 字符串，自动包含类型信息
            String messagesJson = JSON.toJSONString(messages);

            stringRedisTemplate.opsForValue().set(
                    key,
                    messagesJson,
                    CONVERSATION_EXPIRE_HOURS,
                    TimeUnit.HOURS
            );
            
            // 保存或更新会话元数据
            saveOrUpdateConversationMetadata(userId, conversationId, messages);
            
        } catch (Exception e) {
            log.error("序列化对话历史失败", e);
            throw new RuntimeException("保存对话失败", e);
        }

        return conversationId;
    }
    
    private void saveOrUpdateConversationMetadata(long userId, String conversationId, List<Message> messages) {
        try {
            String metaKey = String.format(ChatRedisKeyConstant.CONVERSATION_META_KEY_PREFIX, userId, conversationId);
            String userConversationsKey = String.format(ChatRedisKeyConstant.USER_CONVERSATIONS_KEY_PREFIX, userId);
            
            // 创建或更新会话元数据
            ConversationVO conversationVO = new ConversationVO();
            conversationVO.setConversationId(conversationId);
            
            // 如果是新会话，设置创建时间和默认标题
            String existingMeta = stringRedisTemplate.opsForValue().get(metaKey);
            if (existingMeta == null) {
                conversationVO.setCreateTime(LocalDateTime.now());
                // 使用第一条用户消息作为标题（截取前30个字符）
                String title = extractTitleFromMessages(messages);
                conversationVO.setTitle(title);
            } else {
                // 如果是已存在的会话，保留原有的创建时间和标题
                ConversationVO existing = JSON.parseObject(existingMeta, ConversationVO.class);
                conversationVO.setCreateTime(existing.getCreateTime());
                conversationVO.setTitle(existing.getTitle());
            }
            
            // 更新最后消息和时间
            if (!messages.isEmpty()) {
                Message lastMessage = messages.get(messages.size() - 1);
                conversationVO.setLastMessage(lastMessage.getText());
                conversationVO.setLastMessageTime(LocalDateTime.now());
            }
            
            conversationVO.setMessageCount(messages.size());
            
            // 保存会话元数据
            String metaJson = JSON.toJSONString(conversationVO);
            stringRedisTemplate.opsForValue().set(metaKey, metaJson, CONVERSATION_EXPIRE_HOURS, TimeUnit.HOURS);
            
            // 将会话ID添加到用户会话列表
            stringRedisTemplate.opsForSet().add(userConversationsKey, conversationId);
            stringRedisTemplate.expire(userConversationsKey, CONVERSATION_EXPIRE_HOURS, TimeUnit.HOURS);
            
        } catch (Exception e) {
            log.error("保存会话元数据失败", e);
        }
    }
    
    private String extractTitleFromMessages(List<Message> messages) {
        for (Message message : messages) {
            if (message instanceof UserMessage) {
                String text = message.getText();
                if (text != null && !text.trim().isEmpty()) {
                    return text.length() > 30 ? text.substring(0, 30) + "..." : text;
                }
            }
        }
        return "新对话";
    }
    
    @Override
    public List<ConversationVO> getUserConversations() {
        try {
            long userId = StpUtil.getLoginIdAsLong();
            String userConversationsKey = String.format(ChatRedisKeyConstant.USER_CONVERSATIONS_KEY_PREFIX, userId);
            
            Set<String> conversationIds = stringRedisTemplate.opsForSet().members(userConversationsKey);
            if (conversationIds == null || conversationIds.isEmpty()) {
                return new ArrayList<>();
            }
            
            List<ConversationVO> conversations = new ArrayList<>();
            for (String conversationId : conversationIds) {
                ConversationVO conversation = getConversationMeta(userId, conversationId);
                if (conversation != null) {
                    conversations.add(conversation);
                }
            }
            
            // 按最后消息时间降序排列
            conversations.sort((a, b) -> {
                if (a.getLastMessageTime() == null && b.getLastMessageTime() == null) return 0;
                if (a.getLastMessageTime() == null) return 1;
                if (b.getLastMessageTime() == null) return -1;
                return b.getLastMessageTime().compareTo(a.getLastMessageTime());
            });
            
            return conversations;
        } catch (Exception e) {
            log.error("获取用户会话列表失败", e);
            return new ArrayList<>();
        }
    }
    
    @Override
    public ConversationVO createConversation(CreateConversationDTO createDTO) {
        try {
            long userId = StpUtil.getLoginIdAsLong();
            String conversationId = UUID.randomUUID().toString();
            
            ConversationVO conversationVO = new ConversationVO();
            conversationVO.setConversationId(conversationId);
            conversationVO.setTitle(createDTO.getTitle() != null ? createDTO.getTitle() : "新对话");
            conversationVO.setCreateTime(LocalDateTime.now());
            conversationVO.setMessageCount(0);
            
            // 保存会话元数据
            String metaKey = String.format(ChatRedisKeyConstant.CONVERSATION_META_KEY_PREFIX, userId, conversationId);
            String metaJson = JSON.toJSONString(conversationVO);
            stringRedisTemplate.opsForValue().set(metaKey, metaJson, CONVERSATION_EXPIRE_HOURS, TimeUnit.HOURS);
            
            // 添加到用户会话列表
            String userConversationsKey = String.format(ChatRedisKeyConstant.USER_CONVERSATIONS_KEY_PREFIX, userId);
            stringRedisTemplate.opsForSet().add(userConversationsKey, conversationId);
            stringRedisTemplate.expire(userConversationsKey, CONVERSATION_EXPIRE_HOURS, TimeUnit.HOURS);
            
            return conversationVO;
        } catch (Exception e) {
            log.error("创建会话失败", e);
            throw new RuntimeException("创建会话失败: " + e.getMessage());
        }
    }
    
    @Override
    public void deleteConversation(String conversationId) {
        try {
            long userId = StpUtil.getLoginIdAsLong();
            
            // 删除会话数据
            String conversationKey = String.format(ChatRedisKeyConstant.CONVERSATION_KEY_PREFIX, userId, conversationId);
            stringRedisTemplate.delete(conversationKey);
            
            // 删除会话元数据
            String metaKey = String.format(ChatRedisKeyConstant.CONVERSATION_META_KEY_PREFIX, userId, conversationId);
            stringRedisTemplate.delete(metaKey);
            
            // 从用户会话列表中移除
            String userConversationsKey = String.format(ChatRedisKeyConstant.USER_CONVERSATIONS_KEY_PREFIX, userId);
            stringRedisTemplate.opsForSet().remove(userConversationsKey, conversationId);
            
        } catch (Exception e) {
            log.error("删除会话失败", e);
            throw new RuntimeException("删除会话失败: " + e.getMessage());
        }
    }
    
    @Override
    public ConversationVO getConversation(String conversationId) {
        try {
            long userId = StpUtil.getLoginIdAsLong();
            return getConversationMeta(userId, conversationId);
        } catch (Exception e) {
            log.error("获取会话信息失败", e);
            return null;
        }
    }
    
    private ConversationVO getConversationMeta(long userId, String conversationId) {
        try {
            String metaKey = String.format(ChatRedisKeyConstant.CONVERSATION_META_KEY_PREFIX, userId, conversationId);
            String metaJson = stringRedisTemplate.opsForValue().get(metaKey);
            
            if (metaJson != null) {
                return JSON.parseObject(metaJson, ConversationVO.class);
            }
            return null;
        } catch (Exception e) {
            log.error("获取会话元数据失败", e);
            return null;
        }
    }
}