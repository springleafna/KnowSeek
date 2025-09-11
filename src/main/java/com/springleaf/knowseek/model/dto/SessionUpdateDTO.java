package com.springleaf.knowseek.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 更新会话DTO
 */
@Data
public class SessionUpdateDTO {

    /**
     * 会话ID
     */
    @NotNull(message = "会话ID不能为空")
    private Long id;

    /**
     * 会话名称
     */
    private String sessionName;

    /**
     * 是否活跃
     */
    private Boolean isActive;

    /**
     * 扩展字段，如模型版本、温度等配置
     */
    private String metadata;
}