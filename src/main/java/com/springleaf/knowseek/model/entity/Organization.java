package com.springleaf.knowseek.model.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 组织实体类
 */
@Data
public class Organization {

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
     * 组织描述
     */
    private String description;

    /**
     * 父组织ID
     */
    private Long parentId;

    /**
     * 创建者ID
     */
    private Long createdBy;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 删除标志（0正常 1删除）
     */
    private Boolean deleted;
}
