package com.springleaf.knowseek.model.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户实体类
 */
@Data
public class User {

    /**
     * 用户唯一标识
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
     * 用户角色
     */
    private String role;

    /**
     * 用户所属组织标签，多个用逗号分隔
     */
    private String orgTags;

    /**
     * 用户主组织标签
     */
    private String primaryOrg;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
