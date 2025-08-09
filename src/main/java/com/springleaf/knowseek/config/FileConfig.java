package com.springleaf.knowseek.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "file")
@Data
public class FileConfig {
    private String uploadDir;
    private long chunkSize;
}
