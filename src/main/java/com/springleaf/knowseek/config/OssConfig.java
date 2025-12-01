package com.springleaf.knowseek.config;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

/**
 * 阿里云 OSS 配置
 */
@Configuration
@ConfigurationProperties(prefix = "aliyun.oss")
@Data
@Validated
public class OssConfig {

    @NotBlank(message = "OSS endpoint 不能为空")
    private String endpoint;

    @NotBlank(message = "OSS bucketName 不能为空")
    private String bucketName;

    @NotBlank(message = "OSS accessKeyId 不能为空")
    private String accessKeyId;

    @NotBlank(message = "OSS accessKeySecret 不能为空")
    private String accessKeySecret;

        /**
         * 预签名URL过期时间：默认15分钟
         */
        @Min(value = 60, message = "预签名URL过期时间必须至少60秒")
        @Max(value = 7 * 24 * 60 * 60, message = "预签名URL过期时间不能超过7天")
        private Long presignedUrlExpiration = 15 * 60L;

    @Bean
    @ConditionalOnMissingBean
    public OSS ossClient() {
        validateConfig();

        try {
            return new OSSClientBuilder()
                    .build(endpoint, accessKeyId, accessKeySecret);
        } catch (Exception e) {
            throw new IllegalStateException("创建OSS客户端失败: " + e.getMessage(), e);
        }
    }

    private void validateConfig() {
        if (StringUtils.isAnyBlank(endpoint, bucketName, accessKeyId, accessKeySecret)) {
            throw new IllegalStateException(
                    "阿里云OSS配置不完整，请检查以下配置项: \n" +
                            "- aliyun.oss.endpoint: " + endpoint + "\n" +
                            "- aliyun.oss.bucket-name: " + bucketName + "\n" +
                            "- aliyun.oss.access-key-id: " + accessKeyId + "\n" +
                            "- aliyun.oss.access-key-secret: " + (accessKeySecret != null ? "***" : "null")
            );
        }
    }

    /**
     * 获取完整的Bucket域名
     */
    public String getBucketDomain() {
        return String.format("https://%s.%s", bucketName, endpoint);
    }
}
