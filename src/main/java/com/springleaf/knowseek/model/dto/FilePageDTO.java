package com.springleaf.knowseek.model.dto;

import lombok.Data;

/**
 * 文件分页查询DTO
 */
@Data
public class FilePageDTO {

    /**
     * 当前页码
     */
    private Integer pageNum = 1;

    /**
     * 每页数量
     */
    private Integer pageSize = 10;

    /**
     * 组织名称（模糊搜索）
     */
    private String OrgName;

    /**
     * 文件名称（模糊搜索）
     */
    private String fileName;


}
