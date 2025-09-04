package com.springleaf.knowseek.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文件列表项VO
 */
@Data
public class FileItemVO {

    private Long id;

    private String fileMd5;

    private String fileName;

    /**
     * 文件总大小（字节）
     */
    private Long totalSize;

    /**
     * 上传状态：
     * 0 - 上传完成
     * 1 - 上传中
     * 2 - 上传失败
     * 对应字段：status，TINYINT NOT NULL DEFAULT 0
     */
    private Integer status;

    /**
     * 组织标签，用于标识用户所属组织（可选）
     */
    private String orgTag;

    /**
     * 是否公开：
     * true  - 公开文件（无需认证访问）
     * false - 私有文件（需权限校验）
     */
    private Boolean isPublic;

    /**
     * 创建时间，记录文件上传请求的起始时间
     * 自动填充：DEFAULT CURRENT_TIMESTAMP
     * 对应字段：created_at，TIMESTAMP NOT NULL
     */
    private LocalDateTime createdAt;

    /**
     * 合并时间，分片上传时所有分片合并完成的时间
     * 更新时自动设置为当前时间：ON UPDATE CURRENT_TIMESTAMP
     * 对应字段：merged_at，TIMESTAMP NULL DEFAULT NULL
     */
    private LocalDateTime mergedAt;
}
