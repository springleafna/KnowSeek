package com.springleaf.knowseek.model.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * AI对话消息实体类
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AiMessage {

    /**
     * 消息唯一ID
     */
    private Long id;

    /**
     * 所属会话ID
     */
    private Long sessionId;

    /**
     * 消息角色：用户/助手/系统
     */
    private String role;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 消息创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 扩展字段，如模型、耗时、插件调用等
     */
    private String metadata;
}