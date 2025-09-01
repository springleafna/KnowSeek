package com.springleaf.knowseek.mq.consumer;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.springleaf.knowseek.mq.event.BaseEvent;
import com.springleaf.knowseek.mq.event.FileVectorizeEvent;
import com.springleaf.knowseek.service.impl.EmbeddingService;
import com.springleaf.knowseek.service.impl.EsStorageService;
import com.springleaf.knowseek.utils.FileUtil;
import jakarta.annotation.Resource;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * 文件上传阿里云OSS后进行文件向量化处理的消费者
 */
@Slf4j
@Component
public class FileVectorizeConsumer {

    @Value("${spring.rabbitmq.topic.file-processing-vectorize}")
    private String topic;

    @Resource
    private EmbeddingService embeddingService;

    @Resource
    private EsStorageService esStorageService;

    private final ExecutorService executorService = Executors.newFixedThreadPool(3);
    private static final int BUFFER_SIZE = 8192; // 8KB 缓冲区
    private static final int CHUNK_SIZE = 1000; // 文本块大小
    private static final int CHUNK_OVERLAP = 100; // 重叠大小
    private static final int BATCH_PROCESS_SIZE = 10; // 批量处理文本块数量
    private static final int QUEUE_TIMEOUT_SECONDS = 30; // 队列超时时间

    @RabbitListener(queuesToDeclare = @Queue(value = "file.processing.vectorize"))
    public void listener(String message) throws Exception {
        long startTime = System.currentTimeMillis();
        try {
            log.info("[消费者] 文件向量化任务正式执行 - 执行消费逻辑，topic: {}, message: {}", topic, message);
            
            // 将消息转换成FileVectorizeMessage对象
            BaseEvent.EventMessage<FileVectorizeEvent.FileVectorizeMessage> eventMessage = JSON.parseObject(message,
                    new TypeReference<BaseEvent.EventMessage<FileVectorizeEvent.FileVectorizeMessage>>() {
                    }.getType());

            FileVectorizeEvent.FileVectorizeMessage messageData = eventMessage.getData();
            String location = messageData.getLocation();
            String fileName = FileUtil.extractFileNameFromUrl(location);
            
            log.info("开始流式处理文件向量化，文件地址: {}", location);
            
            // 使用流式处理
            processFileStreaming(fileName, location);
            
            long processingTime = System.currentTimeMillis() - startTime;
            log.info("文档流式处理完成，耗时: {} ms", processingTime);
            
        } catch (Exception e) {
            long processingTime = System.currentTimeMillis() - startTime;
            log.error("监听[消费者] 文件向量化任务，消费失败 topic: {} message: {}，耗时: {} ms", topic, message, processingTime, e);
            throw e;
        }
    }

    /**
     * 流式处理文件向量化
     */
    private void processFileStreaming(String fileName, String fileUrl) throws Exception {
        BlockingQueue<String> chunkQueue = new LinkedBlockingQueue<>(100);
        BlockingQueue<ChunkWithVector> vectorQueue = new LinkedBlockingQueue<>(100);
        
        // 异步执行三个阶段：下载+分块、向量化、存储
        CompletableFuture<Void> downloadFuture = CompletableFuture.runAsync(
            () -> {
                try {
                    downloadAndChunkStreaming(fileUrl, chunkQueue);
                } catch (Exception e) {
                    log.error("下载和分块失败", e);
                    chunkQueue.offer("ERROR");
                    throw new RuntimeException("下载失败", e);
                }
            }, executorService);
            
        CompletableFuture<Void> vectorizeFuture = CompletableFuture.runAsync(
            () -> {
                try {
                    vectorizeChunks(chunkQueue, vectorQueue);
                } catch (Exception e) {
                    log.error("向量化失败", e);
                    vectorQueue.offer(new ChunkWithVector("ERROR", null));
                    throw new RuntimeException("向量化失败", e);
                }
            }, executorService);
            
        CompletableFuture<Void> storageFuture = CompletableFuture.runAsync(
            () -> {
                try {
                    storeVectors(fileName, fileUrl, vectorQueue);
                } catch (Exception e) {
                    log.error("存储失败", e);
                    throw new RuntimeException("存储失败", e);
                }
            }, executorService);
        
        try {
            // 等待所有任务完成，设置超时时间
            CompletableFuture.allOf(downloadFuture, vectorizeFuture, storageFuture)
                .get(300, TimeUnit.SECONDS); // 5分钟超时
        } catch (Exception e) {
            log.error("流式处理超时或失败", e);
            // 取消所有任务
            downloadFuture.cancel(true);
            vectorizeFuture.cancel(true);
            storageFuture.cancel(true);
            throw new RuntimeException("流式处理失败", e);
        }
    }
    /**
     * 流式下载和分块处理
     */
    private void downloadAndChunkStreaming(String fileUrl, BlockingQueue<String> chunkQueue) {
        try {
            log.info("开始流式下载文件: {}", fileUrl);
            
            URL url = new URL(fileUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(30000);
            
            try (InputStream inputStream = connection.getInputStream();
                 BufferedReader reader = new BufferedReader(
                     new InputStreamReader(inputStream, StandardCharsets.UTF_8), BUFFER_SIZE)) {
                
                StringBuilder buffer = new StringBuilder();
                String line;
                int totalChunks = 0;
                
                while ((line = reader.readLine()) != null) {
                    buffer.append(line).append("\n");
                    
                    // 当缓冲区大小超过阈值时，进行分块处理
                    if (buffer.length() >= CHUNK_SIZE * 2) {
                        totalChunks += extractChunksFromBuffer(buffer, chunkQueue, false);
                    }
                }
                
                // 处理剩余的缓冲区内容
                if (!buffer.isEmpty()) {
                    totalChunks += extractChunksFromBuffer(buffer, chunkQueue, true);
                }
                
                // 发送结束信号
                chunkQueue.offer("EOF");
                log.info("文件下载完成，共生成 {} 个文本块", totalChunks);
                
            } finally {
                connection.disconnect();
            }
            
        } catch (Exception e) {
            log.error("流式下载文件失败: {}", fileUrl, e);
            chunkQueue.offer("ERROR"); // 发送错误信号
            throw new RuntimeException("流式下载失败", e);
        }
    }

    /**
     * 从缓冲区提取文本块
     */
    private int extractChunksFromBuffer(StringBuilder buffer, BlockingQueue<String> chunkQueue, boolean isLastBuffer) {
        int chunksExtracted = 0;
        String content = buffer.toString();
        
        int start = 0;
        while (start < content.length()) {
            int end = Math.min(start + CHUNK_SIZE, content.length());
            
            // 如果不是最后一个缓冲区且未到文本末尾，尝试在合适的位置断句
            if (!isLastBuffer && end < content.length()) {
                // 在后面100个字符中查找换行符或句号
                int idealEnd = Math.min(end + 100, content.length());
                int lastNewline = content.lastIndexOf('\n', idealEnd);
                int lastPeriod = content.lastIndexOf('。', idealEnd);
                int lastExclamation = content.lastIndexOf('！', idealEnd);
                int lastQuestion = content.lastIndexOf('？', idealEnd);
                
                int bestBreak = Math.max(Math.max(lastNewline, lastPeriod), 
                                       Math.max(lastExclamation, lastQuestion));
                
                if (bestBreak > start + CHUNK_SIZE - CHUNK_OVERLAP) {
                    end = bestBreak + 1;
                }
            }
            
            String chunk = content.substring(start, end).trim();
            if (!chunk.isEmpty()) {
                chunkQueue.offer(chunk);
                chunksExtracted++;
            }
            
            start = Math.max(start + CHUNK_SIZE - CHUNK_OVERLAP, end);
        }
        
        // 更新缓冲区，保留重叠部分
        if (!isLastBuffer && content.length() > CHUNK_OVERLAP) {
            buffer.setLength(0);
            buffer.append(content.substring(content.length() - CHUNK_OVERLAP));
        } else {
            buffer.setLength(0);
        }
        
        return chunksExtracted;
    }
    /**
     * 异步向量化处理
     */
    private void vectorizeChunks(BlockingQueue<String> chunkQueue, BlockingQueue<ChunkWithVector> vectorQueue) {
        try {
            List<String> batch = new ArrayList<>();
            String chunk;
            
            while (true) {
                try {
                    chunk = chunkQueue.poll(QUEUE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                    
                    if (chunk == null) {
                        log.warn("向量化队列超时，可能下载过程出现问题");
                        break;
                    }
                    
                    if ("EOF".equals(chunk)) {
                        // 处理最后一批
                        if (!batch.isEmpty()) {
                            processBatch(batch, vectorQueue);
                        }
                        vectorQueue.offer(new ChunkWithVector("EOF", null)); // 结束信号
                        break;
                    }
                    
                    if ("ERROR".equals(chunk)) {
                        vectorQueue.offer(new ChunkWithVector("ERROR", null)); // 错误信号
                        break;
                    }

                    batch.add(chunk);

                    // 批量处理
                    if (batch.size() >= BATCH_PROCESS_SIZE) {
                        processBatch(batch, vectorQueue);
                        batch.clear();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        } catch (Exception e) {
            log.error("向量化处理失败", e);
            vectorQueue.offer(new ChunkWithVector("ERROR", null));
            throw new RuntimeException("向量化失败", e);
        }
    }

    /**
     * 批量处理向量化
     */
    private void processBatch(List<String> batch, BlockingQueue<ChunkWithVector> vectorQueue) {
        try {
            log.info("开始向量化批次处理，共 {} 个文本块", batch.size());
            List<List<Double>> vectors = embeddingService.embedTexts(new ArrayList<>(batch));
            
            for (int i = 0; i < batch.size(); i++) {
                vectorQueue.offer(new ChunkWithVector(batch.get(i), vectors.get(i)));
            }
            log.info("批次向量化完成，生成 {} 个向量", vectors.size());
        } catch (Exception e) {
            log.error("批次向量化失败", e);
            throw new RuntimeException("批次向量化失败", e);
        }
    }

    /**
     * 异步批量存储到ES
     */
    private void storeVectors(String fileName, String fileLocation, BlockingQueue<ChunkWithVector> vectorQueue) {
        try {
            List<String> chunkBatch = new ArrayList<>();
            List<List<Double>> vectorBatch = new ArrayList<>();
            ChunkWithVector item;
            
            while (true) {
                try {
                    item = vectorQueue.poll(QUEUE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                    
                    if (item == null) {
                        log.warn("存储队列超时，可能向量化过程出现问题");
                        break;
                    }
                    
                    if ("EOF".equals(item.chunk())) {
                        // 处理最后一批
                        if (!chunkBatch.isEmpty()) {
                            esStorageService.saveChunks(fileName, fileLocation, chunkBatch, vectorBatch);
                            log.info("最后一批存储完成，共 {} 个向量", chunkBatch.size());
                        }
                        break;
                    }
                    
                    if ("ERROR".equals(item.chunk())) {
                        log.error("在存储阶段接收到错误信号");
                        break;
                    }
                    
                    if (item.vector() != null) {
                        chunkBatch.add(item.chunk());
                        vectorBatch.add(item.vector());
                        
                        // 批量存储
                        if (chunkBatch.size() >= BATCH_PROCESS_SIZE) {
                            esStorageService.saveChunks(fileName, fileLocation, chunkBatch, vectorBatch);
                            log.info("批量存储完成，共 {} 个向量", chunkBatch.size());
                            chunkBatch.clear();
                            vectorBatch.clear();
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        } catch (Exception e) {
            log.error("存储向量失败", e);
            throw new RuntimeException("存储失败", e);
        }
    }

    /**
     * 应用关闭时清理资源
     */
    @jakarta.annotation.PreDestroy
    public void shutdown() {
        log.info("开始关闭FileVectorizeConsumer资源");
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                log.warn("执行器服务未在30秒内关闭，强制关闭");
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            executorService.shutdownNow();
        }
        log.info("FileVectorizeConsumer资源清理完成");
    }

        /**
         * 文本块和向量的包装类
         */
        private record ChunkWithVector(String chunk, List<Double> vector) {
    }
}
