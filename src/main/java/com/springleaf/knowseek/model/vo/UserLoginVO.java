package com.springleaf.knowseek.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 用户登录VO
 */
@Data
@AllArgsConstructor
public class UserLoginVO {

    /**
     * 登录token
     */
    private final String token;
}
