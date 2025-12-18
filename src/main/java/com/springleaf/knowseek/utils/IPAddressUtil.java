package com.springleaf.knowseek.utils;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 获取网络请求 IP 工具类
 */
public class IPAddressUtil {

    // 常见的表示真实客户端 IP 的 HTTP 请求头（按优先级排序）
    private static final String[] IP_HEADERS = {
            "X-Forwarded-For",
            "X-Real-IP",          // Nginx 常用
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "X-Forwarded",        // 较少见
            "X-Cluster-Client-IP" // 某些集群环境
    };

    public static String getClientIpAddress(HttpServletRequest request) {
        for (String header : IP_HEADERS) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                // X-Forwarded-For 可能包含多个 IP，取第一个（最原始的客户端 IP）
                if (header.equalsIgnoreCase("X-Forwarded-For")) {
                    ip = ip.split(",")[0].trim();
                }
                return ip;
            }
        }
        // 如果所有 header 都没有，回退到 getRemoteAddr()
        return request.getRemoteAddr();
    }
}
