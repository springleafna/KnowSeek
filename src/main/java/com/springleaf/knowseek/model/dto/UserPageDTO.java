package com.springleaf.knowseek.model.dto;

import lombok.Data;

/**
 * 用户分页查询DTO
 */
@Data
public class UserPageDTO {

    /**
     * 当前页码
     */
    private Integer pageNum = 1;

    /**
     * 每页数量
     */
    private Integer pageSize = 10;
    
    /**
     * 用户名（模糊搜索）
     */
    private String username;
}