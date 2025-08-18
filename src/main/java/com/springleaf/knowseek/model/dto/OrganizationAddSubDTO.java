package com.springleaf.knowseek.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Admin：添加组织下级DTO
 */
@Data
public class OrganizationAddSubDTO {

    /**
     * 当前组织标签
     */
    @NotBlank(message = "当前组织标签不能为空或空字符串")
    private String orgTag;

    /**
     * 下级组织标签
     */
    @NotBlank(message = "下级组织标签不能为空或空字符串")
    private String subOrgTag;
}