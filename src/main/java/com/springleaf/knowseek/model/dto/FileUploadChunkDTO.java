package com.springleaf.knowseek.model.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
    @Min(value = 1, message = "分片总数不能小于1")
    @Max(value = 10000, message = "分片总数不能大于10000")
    private Integer chunkIndex;

    /**
     * ETag
     */
    @NotNull(message = "分片ETag不能为空")
    private String ETag;

    /**
     * 分片大小
     */
    @NotNull(message = "分片大小不能为空")
    private Long chunkSize;
}
