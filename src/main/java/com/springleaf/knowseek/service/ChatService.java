package com.springleaf.knowseek.service;

import com.springleaf.knowseek.model.dto.ChatRequestDTO;
import com.springleaf.knowseek.model.dto.CreateConversationDTO;
import com.springleaf.knowseek.model.vo.ChatResponseVO;
import com.springleaf.knowseek.model.vo.ConversationVO;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

public interface ChatService {
    
    ChatResponseVO chat(ChatRequestDTO requestDTO);
    
    SseEmitter streamChat(ChatRequestDTO requestDTO);
    
    void clearConversation(String conversationId);
    
    // 会话管理功能
    List<ConversationVO> getUserConversations();
    
    ConversationVO createConversation(CreateConversationDTO createDTO);
    
    void deleteConversation(String conversationId);
    
    ConversationVO getConversation(String conversationId);
}