package com.springleaf.knowseek.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;

/**
 * 文件上传初始化VO
 */
@Data
@AllArgsConstructor
public class UploadInitVO {

    /**
     * 上传状态：
     * 0-上传完成：提醒用户文件已存在
     * 1-上传中：提醒用户文件正在上传中
     * 2-上传失败：允许用户重新上传
     */
    private int status;

    /**
     * 上传 ID（仅当第一次上传时存在）
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
