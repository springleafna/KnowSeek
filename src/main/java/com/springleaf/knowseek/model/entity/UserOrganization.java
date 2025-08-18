package com.springleaf.knowseek.model.entity;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 用户组织关联实体类
 */
@Data
@AllArgsConstructor
public class UserOrganization {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 组织ID
     */
    private Long organizationId;
}
