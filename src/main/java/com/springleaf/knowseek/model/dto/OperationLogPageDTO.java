package com.springleaf.knowseek.model.dto;

import lombok.Data;

/**
 * 操作日志分页查询DTO
 */
@Data
public class OperationLogPageDTO {

    /**
     * 当前页码
     */
    private Integer pageNum = 1;

    /**
     * 每页数量
     */
    private Integer pageSize = 10;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 模块名称
     */
    private String moduleName;

    /**
     * 操作类型
     */
    private String operationType;

    /**
     * 响应结果（success/fail）
     */
    private String responseResult;

    /**
     * 开始时间（yyyy-MM-dd HH:mm:ss）
     */
    private String startTime;

    /**
     * 结束时间（yyyy-MM-dd HH:mm:ss）
     */
    private String endTime;
}
