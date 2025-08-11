package com.springleaf.knowseek.model.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class FileUploadChunkInitDTO {

    /**
     * 文件名
     */
    private String fileName;

    /**
     * 文件总大小
     */
    private Integer totalSize;

}
