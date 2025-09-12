package com.springleaf.knowseek.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 分片文件合并DTO
 */
@Data
public class FileUploadCompleteDTO {

    /**
     * 用于唯一标识文件
     */
    @NotBlank(message = "uploadId不能为空")
    private String uploadId;

    /**
     * 分片总数
     */
    @NotNull(message = "分片总数不能为空")
    Integer chunkTotalSize;
}
