package com.springleaf.knowseek.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 文件上传状态枚举
 * 对应数据库字段：file_upload.status
 * 类型：TINYINT
 */
@Getter
@AllArgsConstructor
public enum UploadStatusEnum {

    /**
     * 上传完成（已合并）
     */
    COMPLETED(0, "上传完成"),

    /**
     * 初始化完成
     */
    INITIALIZED(1, "初始化完成"),

    /**
     * 上传中（分片上传进行中）
     */
    UPLOADING(2, "上传中"),

    /**
     * 暂停上传
     */
    PAUSED(3, "暂停"),

    /**
     * 取消上传
     */
    CANCELED(4, "取消上传"),

    /**
     * 上传失败（网络中断、校验失败等）
     */
    FAILED(5, "上传失败");

    private final int status;
    private final String description;

    /**
     * 根据状态码获取枚举实例
     * @param status 状态码
     * @return 对应的枚举对象，若无匹配则返回 null
     */
    public static UploadStatusEnum of(int status) {
        for (UploadStatusEnum value : UploadStatusEnum.values()) {
            if (value.status == status) {
                return value;
            }
        }
        return null;
    }

    /**
     * 判断是否为“上传完成”
     */
    public boolean isCompleted(int status) {
        return status == COMPLETED.getStatus();
    }

    /**
     * 判断是否为“初始化完成”
     */
    public boolean isInitialized() {
        return this == INITIALIZED;
    }

    /**
     * 判断是否为“上传中”
     */
    public boolean isUploading() {
        return this == UPLOADING;
    }

    /**
     * 判断是否为“暂停上传”
     */
    public boolean isPaused() {
        return this == PAUSED;
    }

    /**
     * 判断是否为“取消上传”
     */
    public boolean isCanceled() {
        return this == CANCELED;
    }

    /**
     * 判断是否为“上传失败”
     */
    public boolean isFailed() {
        return this == FAILED;
    }

    @Override
    public String toString() {
        return "UploadStatus{" +
                "status=" + status +
                ", description='" + description + '\'' +
                '}';
    }
}
