package com.springleaf.knowseek.model.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * AI对话会话实体类
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Session {

    /**
     * 会话唯一ID
     */
    private Long id;

    /**
     * 所属用户ID
     */
    private Long userId;

    /**
     * 会话名称，便于用户识别
     */
    private String sessionName;

    /**
     * 会话创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 最后活跃时间
     */
    private LocalDateTime updatedAt;

    /**
     * 是否活跃
     */
    private Boolean isActive;

    /**
     * 扩展字段，如模型版本、温度等配置
     */
    private String metadata;
}