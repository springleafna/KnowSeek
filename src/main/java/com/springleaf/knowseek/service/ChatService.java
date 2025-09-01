package com.springleaf.knowseek.service;

import com.springleaf.knowseek.model.dto.ChatRequestDTO;
import com.springleaf.knowseek.model.vo.ChatResponseVO;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface ChatService {
    
    ChatResponseVO chat(ChatRequestDTO requestDTO);
    
    SseEmitter streamChat(ChatRequestDTO requestDTO);
    
    void clearConversation(String conversationId);
}