package com.springleaf.knowseek.config;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "aliyun.oss")
@Data
public class OssConfig {

    private String endpoint;
    private String bucketName;

    @Bean
    public OSS ossClient() {
        // 直接从环境变量获取敏感信息
        String accessKeyId = System.getenv("OSS_ACCESS_KEY_ID");
        String accessKeySecret = System.getenv("OSS_ACCESS_KEY_SECRET");

        if (accessKeyId == null || accessKeySecret == null) {
            throw new IllegalStateException("OSS_ACCESS_KEY_ID and OSS_ACCESS_KEY_SECRET must be set as environment variables");
        }

        return new OSSClientBuilder()
                .build(endpoint, accessKeyId, accessKeySecret);
    }
}
