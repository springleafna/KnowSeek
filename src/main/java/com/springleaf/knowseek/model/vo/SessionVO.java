package com.springleaf.knowseek.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 会话VO
 */
@Data
public class SessionVO {

    /**
     * 会话唯一ID
     */
    private Long id;

    /**
     * 会话名称
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