package com.springleaf.knowseek.model.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文件上传记录实体类
 * 对应数据库表：file_upload
 * 用于记录用户上传的文件信息，支持断点续传与秒传（基于 MD5）
 */
@Data
public class FileUpload {
    /**
     * 主键
     * 对应字段：id，BIGINT NOT NULL AUTO_INCREMENT
     */
    private Long id;

    /**
     * 文件 MD5 值，用于唯一标识文件内容
     * 对应字段：file_md5，VARCHAR(32) NOT NULL
     */
    private String fileMd5;

    /**
     * 文件原始名称
     * 对应字段：file_name，VARCHAR(255) NOT NULL
     */
    private String fileName;

    /**
     * 文件总大小（字节）
     * 对应字段：total_size，BIGINT NOT NULL
     */
    private Long totalSize;

    /**
     * 上传状态：
     * 0 - 上传完成
     * 1 - 初始化完成
     * 2 - 上传中
     * 3 - 暂停上传
     * 4 - 取消上传
     * 5 - 上传失败
     * 6 - 处理中
     * 7 - 处理失败
     * 8 - 处理完成
     * 对应字段：status，TINYINT NOT NULL DEFAULT 0
     */
    private Integer status;

    /**
     * 用户 ID，标识文件所属用户
     * 对应字段：user_id，BIGINT NOT NULL
     */
    private Long userId;

    /**
     * 知识库 ID，标识文件所属知识库
     * 对应字段：knowledge_base_id，BIGINT NOT NULL
     */
    private Long knowledgeBaseId;

    /**
     * 组织标签，用于标识用户所属组织（可选）
     * 对应字段：org_tag，VARCHAR(50) DEFAULT NULL
     */
    private String orgTag;

    /**
     * 是否公开：
     * true  - 公开文件（无需认证访问）
     * false - 私有文件（需权限校验）
     * 对应字段：is_public，BOOLEAN NOT NULL DEFAULT FALSE
     */
    private Boolean isPublic;

    /**
     * 阿里云OSS文件地址
     * 对应字段：location，VARCHAR(255) DEFAULT NULL
     */
    private String location;

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

    /**
     * 删除标志（0正常 1删除）
     */
    private Boolean deleted;

}
