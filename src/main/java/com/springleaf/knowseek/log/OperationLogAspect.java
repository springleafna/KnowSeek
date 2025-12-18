package com.springleaf.knowseek.log;

import cn.dev33.satoken.stp.StpUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.springleaf.knowseek.common.Result;
import com.springleaf.knowseek.model.entity.OperationLog;
import com.springleaf.knowseek.utils.IPAddressUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 操作日志切面
 * 拦截带有 @OperationLogRecord 注解的方法，自动记录操作日志
 */
@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class OperationLogAspect {

    private final OperationLogService operationLogService;
    private final ObjectMapper objectMapper;

    @Around("@annotation(operationLogRecord)")
    public Object around(ProceedingJoinPoint joinPoint, OperationLogRecord operationLogRecord) throws Throwable {
        long startTime = System.currentTimeMillis();

        // 获取请求信息
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes != null ? attributes.getRequest() : null;

        // 获取方法签名
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();

        // 构建日志对象
        OperationLog operationLog = new OperationLog();
        operationLog.setModuleName(operationLogRecord.moduleName());
        operationLog.setOperationType(operationLogRecord.operationType().getDescription());
        operationLog.setDescription(operationLogRecord.description());
        operationLog.setOperationTime(LocalDateTime.now());

        // 设置请求信息
        if (request != null) {
            operationLog.setRequestUrl(request.getRequestURI());
            operationLog.setRequestMethod(request.getMethod());
            operationLog.setIpAddress(IPAddressUtil.getClientIpAddress(request));
        }

        // 设置用户ID（可能未登录）
        try {
            operationLog.setUserId(StpUtil.getLoginIdAsLong());
        } catch (Exception e) {
            // 用户未登录，设置为0
            operationLog.setUserId(0L);
        }

        // 设置请求参数
        try {
            String params = getRequestParams(joinPoint, signature);
            operationLog.setRequestParams(params);
        } catch (Exception e) {
            log.warn("获取请求参数失败: {}", e.getMessage());
            operationLog.setRequestParams("{}");
        }

        Object result;
        try {
            // 执行目标方法
            result = joinPoint.proceed();

            // 记录成功信息
            operationLog.setResponseResult("success");
            if (result instanceof Result<?> r) {
                operationLog.setResponseMessage(r.getMessage());
            }

            return result;
        } catch (Throwable e) {
            // 记录失败信息
            operationLog.setResponseResult("fail");
            String errorMsg = e.getMessage();
            if (errorMsg != null && errorMsg.length() > 500) {
                errorMsg = errorMsg.substring(0, 500);
            }
            operationLog.setResponseMessage(errorMsg);
            throw e;
        } finally {
            // 计算执行耗时
            long executionTime = System.currentTimeMillis() - startTime;
            operationLog.setExecutionTime(executionTime);

            // 异步保存日志
            operationLogService.saveAsync(operationLog);
        }
    }

    /**
     * 获取请求参数
     */
    private String getRequestParams(ProceedingJoinPoint joinPoint, MethodSignature signature) {
        try {
            String[] paramNames = signature.getParameterNames();
            Object[] paramValues = joinPoint.getArgs();

            if (paramNames == null || paramNames.length == 0) {
                return "{}";
            }

            Map<String, Object> params = new HashMap<>();
            for (int i = 0; i < paramNames.length; i++) {
                Object value = paramValues[i];
                // 过滤敏感参数和不可序列化的对象
                if (isSensitiveParam(paramNames[i])) {
                    params.put(paramNames[i], "******");
                } else if (isIgnoredType(value)) {
                    params.put(paramNames[i], "[" + value.getClass().getSimpleName() + "]");
                } else {
                    params.put(paramNames[i], value);
                }
            }

            String json = objectMapper.writeValueAsString(params);
            // 限制参数长度，避免数据库字段溢出
            if (json.length() > 2000) {
                json = json.substring(0, 2000) + "...(truncated)";
            }
            return json;
        } catch (Exception e) {
            log.warn("序列化请求参数失败: {}", e.getMessage());
            return "{}";
        }
    }

    /**
     * 判断是否为敏感参数
     */
    private boolean isSensitiveParam(String paramName) {
        String lowerName = paramName.toLowerCase();
        return lowerName.contains("password")
                || lowerName.contains("pwd")
                || lowerName.contains("secret")
                || lowerName.contains("token")
                || lowerName.contains("key");
    }

    /**
     * 判断是否为需要忽略的类型
     */
    private boolean isIgnoredType(Object value) {
        return value instanceof HttpServletRequest
                || value instanceof HttpServletResponse
                || value instanceof MultipartFile
                || value instanceof MultipartFile[];
    }
}
