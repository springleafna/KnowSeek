package com.springleaf.knowseek.log;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 操作类型枚举
 */
@Getter
@AllArgsConstructor
public enum OperationType {

    LOGIN("登录"),
    LOGOUT("登出"),
    INSERT("新增"),
    DELETE("删除"),
    UPDATE("修改"),
    QUERY("查询"),
    IMPORT("导入"),
    EXPORT("导出"),
    UPLOAD("上传"),
    DOWNLOAD("下载"),
    OTHER("其他");

    private final String description;
}
