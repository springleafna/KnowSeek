package com.springleaf.knowseek.constans;

/**
 * 用户上传到OSS的文件路径地址常量
 */
public class OssUserFilePathConstant {

    /**
     * 用户文件路径模板：uploads/{userId}/{timestamp}/{fileMd5}{extension}
     * uploads/1002/20250405/def456ghi789.pdf
     */
    public static final String USER_FILE_PATH = "uploads/%s/%s/%s%s";
}
