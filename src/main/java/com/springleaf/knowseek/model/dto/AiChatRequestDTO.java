package com.springleaf.knowseek.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * AI对话请求DTO
 */
@Data
public class AiChatRequestDTO {

    /**
     * 会话ID，如果为空则创建新会话
     */
    private Long sessionId;

    /**
     * 用户消息内容
     */
    @NotBlank(message = "消息内容不能为空")
    private String content;

    /**
     * 模型配置等元数据
     */
    private String metadata;
}