package com.springleaf.knowseek.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Admin：创建组织DTO
 */
@Data
public class OrganizationAddDTO {

    /**
     * 组织标签
     */
    @NotBlank(message = "组织标签不能为空或空字符串")
    private String orgTag;

    /**
     * 组织名称
     */
    @NotBlank(message = "组织名称不能为空或空字符串")
    private String orgName;

    /**
     * 上级组织标签，可以为空
     */
    private String parentOrgTag;

    /**
     * 组织描述，可以为空
     */
    private String description;
}
