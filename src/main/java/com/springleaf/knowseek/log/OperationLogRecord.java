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

    /**
     * 是否启用日志记录，默认启用
     * 设置为 false 可临时禁用日志记录
     */
    boolean enabled() default true;

    /**
     * 是否保存请求参数，默认保存
     * 对于大参数请求（如富文本、批量导入）建议设置为 false
     */
    boolean saveParams() default true;

    /**
     * 是否忽略响应结果，默认不忽略
     * 设置为 true 时不记录响应信息
     */
    boolean ignoreResult() default false;
}
