package com.springleaf.knowseek.enums;

import lombok.Getter;

@Getter
public enum ResultCodeEnum {

    SUCCESS(0, "操作成功"),
    ERROR(1, "操作失败"),
    
    USER_NOT_EXIST(1001, "用户不存在"),
    PASSWORD_ERROR(1002, "密码错误"),
    USER_DISABLED(1003, "用户已被禁用"),
    USER_ALREADY_EXIST(1004, "用户已存在"),
    OPERATION_FAILED(1005, "操作失败"),
    UNAUTHORIZED(1006, "未授权"),
    FORBIDDEN(1007, "禁止访问"),
    
    PARAM_ERROR(5001, "参数错误"),
    SYSTEM_ERROR(5002, "系统错误");

    private final Integer code;
    private final String message;

    ResultCodeEnum(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

}
