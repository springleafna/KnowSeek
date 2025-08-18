package com.springleaf.knowseek.model.vo;

import lombok.Data;

/**
 * 组织列表VO
 */
@Data
public class OrganizationListVO {

    /**
     * 组织ID
     */
    private Long id;

    /**
     * 组织唯一标签名
     */
    private String tag;

    /**
     * 组织名称
     */
    private String name;

    /**
     * 父组织名称
     */
    private String parentName;

    /**
     * 创建者用户名
     */
    private String creatorUsername;
}