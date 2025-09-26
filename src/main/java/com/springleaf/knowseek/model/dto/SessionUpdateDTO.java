package com.springleaf.knowseek.model.dto;

import jakarta.validation.constraints.NotBlank;
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
    @NotBlank(message = "会话名称不能为空")
    private String sessionName;
}