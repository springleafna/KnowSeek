package com.springleaf.knowseek.mq.consumer;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.springleaf.knowseek.mq.event.BaseEvent;
import com.springleaf.knowseek.mq.event.FileVectorizeEvent;
import com.springleaf.knowseek.service.impl.EmbeddingService;
import com.springleaf.knowseek.service.impl.EsStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 文件上传阿里云OSS后进行文件向量化处理的消费者
 */
@Slf4j
@Component
public class FileVectorizeConsumer {

    @Value("${spring.rabbitmq.topic.file-processing-vectorize}")
    private String topic;

    @Autowired
    private EmbeddingService embeddingService;

    @Autowired
    private EsStorageService esStorageService;

    @RabbitListener(queuesToDeclare = @Queue(value = "file.processing.vectorize"))
    public void listener(String message) throws IOException {
        try {
            log.info("[消费者] 文件向量化任务正式执行 - 执行消费逻辑，topic: {}, message: {}", topic, message);
            
            // 将消息转换成FileVectorizeMessage对象
            BaseEvent.EventMessage<FileVectorizeEvent.FileVectorizeMessage> eventMessage = JSON.parseObject(message,
                    new TypeReference<BaseEvent.EventMessage<FileVectorizeEvent.FileVectorizeMessage>>() {
                    }.getType());

            FileVectorizeEvent.FileVectorizeMessage messageData = eventMessage.getData();
            // 获取文件下载地址
            String location = messageData.getLocation();
            
            log.info("开始处理文件向量化，文件地址: {}", location);
            
            // 1. 从OSS下载文件内容
            String fileContent = downloadFileFromUrl(location);
            
            // 2. 文档分块
            List<String> chunks = chunkDocument(fileContent);
            log.info("文档分块完成，共分为 {} 块", chunks.size());
            
            // 3. 向量化处理
            List<List<Double>> vectors = embeddingService.embedTexts(chunks);
            log.info("文档向量化完成");
            
            // 4. 存储到ES
            String fileName = extractFileNameFromUrl(location);
            esStorageService.saveChunks(fileName, location, chunks, vectors);
            log.info("文档向量存储到ES完成");
            
        } catch (Exception e) {
            log.error("监听[消费者] 文件向量化任务，消费失败 topic: {} message: {}", topic, message, e);
            throw e;
        }
    }

    /**
     * 从URL下载文件内容
     */
    private String downloadFileFromUrl(String fileUrl) throws IOException {
        log.info("开始下载文件: {}", fileUrl);
        
        URL url = new URL(fileUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(30000);
        
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
            
            log.info("文件下载完成，内容长度: {}", content.length());
            return content.toString();
        } finally {
            connection.disconnect();
        }
    }

    /**
     * 文档分块处理
     */
    private List<String> chunkDocument(String content) {
        List<String> chunks = new ArrayList<>();
        
        // 简单的按段落分块，每块最大1000字符
        int chunkSize = 1000;
        int overlap = 100; // 重叠部分
        
        for (int i = 0; i < content.length(); i += chunkSize - overlap) {
            int end = Math.min(i + chunkSize, content.length());
            String chunk = content.substring(i, end).trim();
            
            if (!chunk.isEmpty()) {
                chunks.add(chunk);
            }
            
            if (end >= content.length()) {
                break;
            }
        }
        
        return chunks;
    }

    /**
     * 从URL中提取文件名
     */
    private String extractFileNameFromUrl(String url) {
        try {
            String[] parts = url.split("/");
            return parts[parts.length - 1];
        } catch (Exception e) {
            log.warn("无法从URL提取文件名: {}", url);
            return "unknown_file";
        }
    }
}
