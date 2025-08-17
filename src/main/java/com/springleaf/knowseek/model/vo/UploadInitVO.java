package com.springleaf.knowseek.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;
import java.util.Map;

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

    /**
     * 文件直链/预签名下载地址
     */
    private String location;

    /**
     * 每个分片的直传 URL（仅当 !uploaded 时有效）
     */
    private Map<Integer, String> partUploadUrls; // 每个分片直传 URL
}
