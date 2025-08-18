package com.springleaf.knowseek.model.dto;

import lombok.Data;

/**
 * 组织分页查询DTO
 */
@Data
public class OrganizationPageDTO {

    /**
     * 当前页码
     */
    private Integer pageNum = 1;

    /**
     * 每页数量
     */
    private Integer pageSize = 10;
}