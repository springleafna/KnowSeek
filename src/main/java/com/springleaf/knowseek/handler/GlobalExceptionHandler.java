package com.springleaf.knowseek.handler;

import com.springleaf.knowseek.common.Result;
import com.springleaf.knowseek.exception.BusinessException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理类
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 捕获并处理 BusinessException 异常
     * @param e 业务异常实例
     * @return 统一的错误响应对象
     */
    @ExceptionHandler(BusinessException.class)
    public Result<?> handleBusinessException(BusinessException e) {
        // 将业务异常中的错误码和错误信息封装到 Result 对象中
        return Result.error(e.getCode(), e.getMessage());
    }

    /**
     * 捕获并处理所有其他未处理的异常，作为最后的备用
     * @param e 异常实例
     * @return 统一的错误响应对象
     */
    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e) {
        e.printStackTrace(); // 打印异常栈，便于调试
        return Result.error(500, "系统繁忙，请稍后再试！");
    }
}
