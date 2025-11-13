package com.springleaf.knowseek.config;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "aliyun.oss")
@Data
public class OssConfig {

    private String endpoint;
    private String bucketName;
    private String accessKeyId;
    private String accessKeySecret;
    /**
     * 预签名URL过期时间：15分钟
     */
    private Long presignedUrlExpiration;

    @Bean
    public OSS ossClient() {
        if (accessKeyId == null || accessKeySecret == null) {
            throw new IllegalStateException("OSS_ACCESS_KEY_ID and OSS_ACCESS_KEY_SECRET must be set as environment variables");
        }

        return new OSSClientBuilder()
                .build(endpoint, accessKeyId, accessKeySecret);
    }
}
