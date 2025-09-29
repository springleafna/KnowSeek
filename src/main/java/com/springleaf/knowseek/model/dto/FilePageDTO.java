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
     * 知识库名称（模糊搜索）
     */
    private String kbName;

    /**
     * 文件名称（模糊搜索）
     */
    private String fileName;

    /**
     * 排序字段：
     * fileName - 文件名
     * totalSize - 文件大小
     * status - 状态
     * createdAt - 上传时间
     * type - 文件类型
     */
    private String sortBy;

    /**
     * 排序顺序：
     * asc - 升序
     * desc - 降序
     */
    private String sortOrder = "desc"; // 默认按创建时间降序
}
