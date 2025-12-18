package com.springleaf.knowseek.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作日志VO
 */
@Data
public class OperationLogVO {

    /**
     * 日志ID
     */
    private Long id;

    /**
     * 操作用户ID
     */
    private Long userId;

    /**
     * 模块名称
     */
    private String moduleName;

    /**
     * IP地址
     */
    private String ipAddress;

    /**
     * 操作类型
     */
    private String operationType;

    /**
     * 操作描述
     */
    private String description;

    /**
     * 请求路径
     */
    private String requestUrl;

    /**
     * 请求方式
     */
    private String requestMethod;

    /**
     * 请求参数
     */
    private String requestParams;

    /**
     * 响应结果（success/fail）
     */
    private String responseResult;

    /**
     * 响应信息
     */
    private String responseMessage;

    /**
     * 操作时间
     */
    private LocalDateTime operationTime;

    /**
     * 执行耗时（毫秒）
     */
    private Long executionTime;
}
