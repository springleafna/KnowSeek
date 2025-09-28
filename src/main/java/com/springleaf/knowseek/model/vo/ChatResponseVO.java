package com.springleaf.knowseek.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChatResponseVO {

    private String message;

    private String conversationId;

    private Long sessionId;

    private LocalDateTime timestamp;

    private String role;

    private Boolean fromKnowledgeBase;

    private String modelName;

    private String provider;

    private String selectionReason;
}