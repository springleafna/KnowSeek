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
     * 0-上传完成：提醒用户该文件已存在
     * 1-初始化完成：提醒用户该文件正在上传中
     * 2-上传中：提醒用户文件正在上传中
     * 3-暂停上传：提醒用户该文件上传已暂停可继续上传
     * 4-取消上传：提醒用户该文件已取消上传可重新上传
     * 5-上传失败：提醒用户该文件上传失败可重新上传
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
