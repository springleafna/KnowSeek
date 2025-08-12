package com.springleaf.knowseek.model.dto;

import lombok.Data;

/**
 * 分片文件合并DTO
 */
@Data
public class FileUploadCompleteDTO {

    String fileName;
    String uploadId;
    Integer chunkTotalSize;
}
