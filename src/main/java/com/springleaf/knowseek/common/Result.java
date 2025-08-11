package com.springleaf.knowseek.common;

import com.springleaf.knowseek.enums.ResultCodeEnum;
import lombok.Data;

/**
 * 统一响应结果
 * @param <T>
 */
@Data
public class Result<T> {

    private Integer code;
    private String message;
    private T data;

    public Result(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public Result(ResultCodeEnum resultCodeEnum, T data) {
        this.code = resultCodeEnum.getCode();
        this.message = resultCodeEnum.getMessage();
        this.data = data;
    }

    public static <T> Result<T> success(T data) {
        return new Result<>(ResultCodeEnum.SUCCESS.getCode(), ResultCodeEnum.SUCCESS.getMessage(), data);
    }

    public static Result<Void> success() {
        return new Result<>(ResultCodeEnum.SUCCESS.getCode(), ResultCodeEnum.SUCCESS.getMessage(), null);
    }

    public static Result<Void> successMsg(String message) {
        return new Result<>(ResultCodeEnum.SUCCESS.getCode(), message, null);
    }

    public static <T> Result<T> error(String message) {
        return new Result<>(ResultCodeEnum.ERROR.getCode(), message, null);
    }

    public static <T> Result<T> error(ResultCodeEnum resultCodeEnum) {
        return new Result<>(resultCodeEnum.getCode(), resultCodeEnum.getMessage(), null);
    }

    public static <T> Result<T> error(Integer code, String message) {
        return new Result<>(code, message, null);
    }
}
