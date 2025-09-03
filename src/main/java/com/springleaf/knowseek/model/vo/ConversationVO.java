package com.springleaf.knowseek.model.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ConversationVO {
    
    private String conversationId;
    
    private String title;
    
    private String lastMessage;
    
    private LocalDateTime lastMessageTime;
    
    private LocalDateTime createTime;
    
    private Integer messageCount;
}