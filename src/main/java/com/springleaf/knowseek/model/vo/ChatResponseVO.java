package com.springleaf.knowseek.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChatResponseVO {
    
    private String message;
    
    private String conversationId;
    
    private LocalDateTime timestamp;
    
    private String role;
    
    private Boolean fromKnowledgeBase;
}