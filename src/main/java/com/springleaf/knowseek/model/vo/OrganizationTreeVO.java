package com.springleaf.knowseek.model.vo;

import lombok.Data;

import java.util.List;

/**
 * 组织树形结构VO
 */
@Data
public class OrganizationTreeVO {

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
     * 父组织ID
     */
    private Long parentId;

    /**
     * 子组织列表
     */
    private List<OrganizationTreeVO> children;
}