package com.springleaf.knowseek.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 分片暂停上传DTO
 */
@Data
public class FileUploadPauseDTO {

    /**
     * 用于唯一标识文件
     */
    @NotBlank(message = "uploadId不能为空")
    private String uploadId;
}
