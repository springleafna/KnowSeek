package com.springleaf.knowseek.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户列表VO
 */
@Data
public class UserListVO {

    /**
     * 用户ID
     */
    private Long id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 用户角色：ADMIN/USER
     */
    private String role;

    /**
     * 用户主组织名称
     */
    private String primaryOrgName;

    /**
     * 用户注册时间
     */
    private LocalDateTime createdAt;
}