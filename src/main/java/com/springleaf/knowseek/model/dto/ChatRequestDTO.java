package com.springleaf.knowseek.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class ChatRequestDTO {

    @NotBlank(message = "消息内容不能为空")
    private String message;

    private Long sessionId;

    private String conversationId;

    private Boolean useKnowledgeBase;
}