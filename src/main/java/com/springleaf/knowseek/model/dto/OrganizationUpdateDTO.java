package com.springleaf.knowseek.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Admin：编辑组织DTO
 */
@Data
public class OrganizationUpdateDTO {

    /**
     * 组织标签
     */
    @NotBlank(message = "组织标签不能为空或空字符串")
    private String orgTag;

    /**
     * 组织名称
     */
    private String orgName;

    /**
     * 上级组织标签
     */
    private String parentOrgTag;

    /**
     * 组织描述
     */
    private String description;
}