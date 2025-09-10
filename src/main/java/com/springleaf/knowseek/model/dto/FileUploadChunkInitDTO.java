package com.springleaf.knowseek.model.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 分片上传初始化DTO
 */
@Data
public class FileUploadChunkInitDTO {

    /**
     * 文件名
     */
    @NotBlank(message = "上传文件名不能为null或空字符串")
    private String fileName;

    /**
     * 文件MD5
     */
    @NotBlank(message = "上传文件Md5不能为null或空字符串")
    private String fileMd5;

    /**
     * 文件大小
     */
    @NotNull(message = "文件大小不能为空")
    private Long fileSize;

    /**
     * 总分片数
     */
    @NotNull(message = "分片总数不能为空")
    @Min(value = 1, message = "分片总数不能小于1")
    @Max(value = 10000, message = "分片总数不能大于10000")
    private Integer chunkTotal;

    /**
     * 知识库ID
     */
    @NotNull(message = "知识库ID不能为空")
    private Long knowledgeBaseId;
}
