package com.springleaf.knowseek.model.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * Admin：为用户分配组织DTO
 */
@Data
public class OrganizationAssignDTO {

    /**
     * 用户ID
     */
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    /**
     * 组织标签列表
     */
    @NotEmpty(message = "组织标签列表不能为空")
    private List<String> orgTagList;
}
