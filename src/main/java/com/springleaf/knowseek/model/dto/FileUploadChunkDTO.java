package com.springleaf.knowseek.model.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件分片上传DTO
 */
@Data
public class FileUploadChunkDTO {

    /**
     * 文用于唯一标识文件
     */
    private String uploadId;

    /**
     * 分片索引，表示当前分片的位置
     */
    private Integer chunkIndex;

    /**
     * 文件名
     */
    private String fileName;

    /**
     * 分片文件
     */
    private MultipartFile file;
}
