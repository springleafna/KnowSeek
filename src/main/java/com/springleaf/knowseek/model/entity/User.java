package com.springleaf.knowseek.model.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户实体类
 */
@Data
public class User {

    /**
     * 用户ID
     */
    private Long id;

    /**
     * 用户名，唯一
     */
    private String username;

    /**
     * 加密后的密码
     */
    private String password;

    /**
     * 用户角色：ADMIN/USER
     */
    private String role;

    /**
     * 用户主组织ID
     */
    private Long primaryOrgId;

    /**
     * 用户主知识库ID
     */
    private Long primaryKnowledgeBaseId;

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
