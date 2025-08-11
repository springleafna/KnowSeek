package com.springleaf.knowseek.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 文件上传初始化VO
 */
@Data
@AllArgsConstructor
public class UploadInitVO {

    /**
     * 是否已上传完成（可用于秒传）
     */
    private boolean uploaded;

    /**
     * 上传 ID（仅当 !uploaded 时有效）
     */
    private String uploadId;
}
