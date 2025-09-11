package com.springleaf.knowseek.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 创建会话DTO
 */
@Data
public class SessionCreateDTO {

    /**
     * 会话名称
     */
    @NotBlank(message = "会话名称不能为空")
    private String sessionName;

    /**
     * 扩展字段，如模型版本、温度等配置
     */
    private String metadata;
}