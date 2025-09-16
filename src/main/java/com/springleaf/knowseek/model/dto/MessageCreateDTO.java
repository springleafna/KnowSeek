package com.springleaf.knowseek.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 创建AI消息DTO
 */
@Data
public class MessageCreateDTO {

    /**
     * 所属会话ID
     */
    @NotNull(message = "会话ID不能为空")
    private Long sessionId;

    /**
     * 消息角色：用户/助手/系统
     */
    @NotBlank(message = "消息角色不能为空")
    private String role;

    /**
     * 消息内容
     */
    @NotBlank(message = "消息内容不能为空")
    private String content;

    /**
     * 扩展字段，如模型、耗时、插件调用等
     */
    private String metadata;
}