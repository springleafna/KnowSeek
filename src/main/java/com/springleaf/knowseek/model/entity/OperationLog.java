package com.springleaf.knowseek.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 操作日志实体类
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OperationLog {

    /**
     * 自增ID
     */
    private Long id;

    /**
     * 操作用户 ID
     */
    private Long userId;

    /**
     * 模块名称（如：用户管理）
     */
    private String moduleName;

    /**
     * IP 地址
     */
    private String ipAddress;

    /**
     * 操作类型（登录、登出、新增、删除、修改、查询、导入、导出、上传、下载）
     */
    private String operationType;

    /**
     * 描述
     */
    private String description;

    /**
     * 请求路径（如/user/login）
     */
    private String requestUrl;

    /**
     * 请求方式（GET、POST、DELETE、PUT）
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

    /**
     * 用户角色（关联查询字段，非持久化）
     */
    private String roleName;
}
