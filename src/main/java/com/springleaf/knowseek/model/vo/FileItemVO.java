package com.springleaf.knowseek.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文件列表项VO
 */
@Data
public class FileItemVO {

    private Long id;

    private String fileName;

    /**
     * 文件类型扩展名
     */
    private String type;

    /**
     * 格式化后的文件总大小（例如：1.8 MB）
     */
    private String totalSize;

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
     */
    private String status;

    /**
     * 知识库名称
     */
    private String knowledgeBaseName;

    /**
     * 是否公开：
     * true  - 公开文件（无需认证访问）
     * false - 私有文件（需权限校验）
     */
    private Boolean isPublic;

    /**
     * 创建时间，记录文件上传请求的起始时间
     */
    private LocalDateTime createdAt;

}
