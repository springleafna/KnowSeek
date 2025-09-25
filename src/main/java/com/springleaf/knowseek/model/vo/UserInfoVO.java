package com.springleaf.knowseek.model.vo;

import lombok.Data;

/**
 * 用户信息VO
 */
@Data
public class UserInfoVO {

    /**
     * 用户名，唯一
     */
    private String username;

    /**
     * 用户角色：ADMIN/USER
     */
    private String role;

    /**
     * 用户主组织ID
     */
    private String primaryOrgName;

    /**
     * 用户主知识库ID
     */
    private Long primaryKbId;
}
