package com.springleaf.knowseek.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件分片上传DTO
 */
@Data
public class FileUploadChunkDTO {

    /**
     * 用于唯一标识文件
     */
    @NotBlank(message = "uploadId不能为空")
    private String uploadId;

    /**
     * 分片索引，表示当前分片的位置
     */
    @NotNull(message = "分片索引不能为空")
    private Integer chunkIndex;

    /**
     * 文件名
     */
    @NotBlank(message = "文件名不能为空")
    private String fileName;

    /**
     * 分片文件
     */
    @NotNull(message = "分片文件不能为空")
    private MultipartFile file;
}
