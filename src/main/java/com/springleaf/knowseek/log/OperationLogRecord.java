package com.springleaf.knowseek.log;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 操作日志记录注解
 * 标注在 Controller 方法上，自动记录操作日志
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OperationLogRecord {

    /**
     * 模块名称
     */
    String moduleName();

    /**
     * 操作类型
     */
    OperationType operationType();

    /**
     * 操作描述
     */
    String description() default "";
}
