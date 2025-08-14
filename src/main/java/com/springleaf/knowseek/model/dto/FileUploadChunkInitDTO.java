package com.springleaf.knowseek.model.dto;

import jakarta.validation.constraints.NotBlank;
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

}
