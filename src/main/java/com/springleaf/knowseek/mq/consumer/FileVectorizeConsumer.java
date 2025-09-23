package com.springleaf.knowseek.mq.consumer;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.springleaf.knowseek.model.bo.VectorBO;
import com.springleaf.knowseek.mq.event.BaseEvent;
import com.springleaf.knowseek.mq.event.FileVectorizeEvent;
import com.springleaf.knowseek.service.EmbeddingService;
import com.springleaf.knowseek.service.VectorRecordService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.amqp.rabbit.annotation.Argument;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
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
    private VectorRecordService vectorRecordService;

    private final ExecutorService executorService = Executors.newFixedThreadPool(3);
    private static final int BUFFER_SIZE = 8192; // 8KB 缓冲区
    private static final int CHUNK_SIZE = 1000; // 文本块大小
    private static final int CHUNK_OVERLAP = 100; // 重叠大小
    private static final int BATCH_PROCESS_SIZE = 10; // 批量处理文本块数量
    private static final int QUEUE_TIMEOUT_SECONDS = 30; // 队列超时时间

    // 定义纯文本类型，这些类型可以使用最高效的流式读取
    private static final Set<String> PLAIN_TEXT_EXTENSIONS = new HashSet<>(Arrays.asList(
            "txt", "md", "xml", "html", "htm", "json", "csv"
    ));

    @RabbitListener(queuesToDeclare = @Queue(
            value = "file.processing.vectorize",
            durable = "true",
            arguments = {
                    @Argument(name = "x-dead-letter-exchange", value = "file.processing.dlx"),
                    @Argument(name = "x-dead-letter-routing-key", value = "file.processing.vectorize.failed")
            }
    ))
    public void listener(String message, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag,
                        @Header(value = "x-death", required = false) java.util.List<java.util.Map<String, Object>> xDeath) {
        long startTime = System.currentTimeMillis();
        try {
            log.info("[消费者] 文件向量化任务正式执行 - 执行消费逻辑，topic: {}, message: {}", topic, message);
            
            // 将消息转换成FileVectorizeMessage对象
            BaseEvent.EventMessage<FileVectorizeEvent.FileVectorizeMessage> eventMessage = JSON.parseObject(message,
                    new TypeReference<BaseEvent.EventMessage<FileVectorizeEvent.FileVectorizeMessage>>() {
                    }.getType());

            FileVectorizeEvent.FileVectorizeMessage messageData = eventMessage.getData();
            String location = messageData.getLocation();
            String fileName = messageData.getFileName();
            String extension = messageData.getExtension();

            VectorBO vectorBO = messageData.getVectorBO();

            log.info("开始流式处理文件，文件名称：{}，文件类型: {}，文件地址: {}", fileName, extension, location);
            
            // 使用流式处理
            processFileStreaming(fileName, location, extension, vectorBO);
            
            long processingTime = System.currentTimeMillis() - startTime;
            log.info("文档流式处理完成，耗时: {} ms", processingTime);
            
        } catch (Exception e) {
            long processingTime = System.currentTimeMillis() - startTime;
            int retryCount = getRetryCount(xDeath);
            log.error("监听[消费者] 文件向量化任务，消费失败 topic: {} message: {}，耗时: {} ms，重试次数: {}",
                     topic, message, processingTime, retryCount, e);

            // 记录失败信息到数据库或日志系统（可选）
            recordFailureInfo(message, e, retryCount);

            // 不再重新抛出异常，让Spring Boot的重试机制接管
            // 当达到最大重试次数时，消息会自动进入死信队列
        }
    }

    /**
     * 流式处理文件向量化
     */
    private void processFileStreaming(String fileName, String fileUrl, String extension, VectorBO vectorBO) throws Exception {
        BlockingQueue<String> chunkQueue = new LinkedBlockingQueue<>(100);
        BlockingQueue<ChunkWithVector> vectorQueue = new LinkedBlockingQueue<>(100);
        
        // 异步执行三个阶段：下载+分块、向量化、存储
        CompletableFuture<Void> downloadFuture = CompletableFuture.runAsync(
            () -> {
                try {
                    downloadAndChunkStreaming(fileUrl, extension, chunkQueue);
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
                    storeVectors(fileName, fileUrl, vectorQueue, vectorBO);
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
    private void downloadAndChunkStreaming(String fileUrl, String extension, BlockingQueue<String> chunkQueue) {
        try {
            log.info("开始流式下载和解析文件: {}", fileUrl);

            URL url = new URL(fileUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(30000);

            try (InputStream inputStream = connection.getInputStream()) {
                // 根据文件类型选择不同的处理策略
                if (isPlainText(extension)) {
                    // 对于纯文本，使用最高效的逐行读取流式处理
                    log.info("文件类型 '{}'，采用纯文本流式处理。", extension);
                    processPlainTextStream(inputStream, chunkQueue);
                } else {
                    // 对于其他所有复杂格式，使用 Apache Tika 进行解析
                    log.info("文件类型 '{}'，采用 Apache Tika 解析。", extension);
                    processWithTika(inputStream, chunkQueue);
                }

                log.info("文件解析和分块完成: {}", fileUrl);

            } finally {
                connection.disconnect();
                // 确保在所有处理完成后发送结束信号
                chunkQueue.offer("EOF");
                log.info("已发送EOF信号。");
            }

        } catch (Exception e) {
            log.error("流式下载或解析文件失败: {}", fileUrl, e);
            chunkQueue.offer("ERROR"); // 发送错误信号
            throw new RuntimeException("流式下载或解析失败", e);
        }
    }

    /**
     * 判断文件类型是否为纯文本
     */
    private boolean isPlainText(String fileType) {
        return fileType != null && PLAIN_TEXT_EXTENSIONS.contains(fileType.toLowerCase());
    }

    /**
     * 使用 Apache Tika 从输入流中提取文本，然后进行分块。
     * 这是一个“伪流式”处理：Tika会先在内存中构建文档模型来提取文本。
     * @param inputStream 文件输入流
     * @param chunkQueue 文本块队列
     */
    private void processWithTika(InputStream inputStream, BlockingQueue<String> chunkQueue) throws Exception {
        Tika tika = new Tika();
        // Tika 会处理所有解析逻辑，并返回一个包含所有文本的长字符串
        String extractedText = tika.parseToString(inputStream);

        if (extractedText == null || extractedText.trim().isEmpty()) {
            log.warn("Tika未能从文件中提取任何文本。");
            return;
        }

        // 对提取出的完整文本进行分块
        chunkExtractedText(extractedText, chunkQueue);
    }

    /**
     * 对一个已知的长字符串进行分块处理
     * @param text 要分块的文本
     * @param chunkQueue 文本块队列
     */
    private void chunkExtractedText(String text, BlockingQueue<String> chunkQueue) {
        // 调用核心分块方法，因为text是完整内容，所以 isFinalChunk 为 true
        List<String> chunks = chunkContent(text, true);

        // 将生成的所有块放入队列
        for (String chunk : chunks) {
            chunkQueue.offer(chunk);
        }

        log.info("从Tika提取的文本中生成了 {} 个文本块", chunks.size());
    }

    /**
     * 专门处理纯文本的“真·流式”方法，内存占用最低。
     * @param inputStream 文件输入流
     * @param chunkQueue 文本块队列
     */
    private void processPlainTextStream(InputStream inputStream, BlockingQueue<String> chunkQueue) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8), BUFFER_SIZE)) {
            StringBuilder buffer = new StringBuilder();
            String line;
            int totalChunks = 0;

            while ((line = reader.readLine()) != null) {
                buffer.append(line).append("\n");

                if (buffer.length() >= CHUNK_SIZE * 2) {
                    totalChunks += extractChunksFromBuffer(buffer, chunkQueue, false);
                }
            }

            if (!buffer.isEmpty()) {
                totalChunks += extractChunksFromBuffer(buffer, chunkQueue, true);
            }
            log.info("纯文本流式处理共生成 {} 个文本块", totalChunks);
        }
    }

    /**
     * 核心分块方法：将给定内容按智能断句逻辑切分成文本块列表。
     * @param content 要进行分块的文本内容
     * @param isFinalChunk 布尔值，表示这是否是最后一部分内容。如果是，则会处理到末尾；如果不是，会为重叠保留内容。
     * @return 一个包含所有文本块的列表
     */
    private List<String> chunkContent(String content, boolean isFinalChunk) {
        List<String> chunks = new ArrayList<>();
        int start = 0;

        while (start < content.length()) {
            int end = Math.min(start + CHUNK_SIZE, content.length());

            // 智能断句逻辑：如果不是最后一块，且还有后续内容，则尝试寻找最佳断句点
            if (!isFinalChunk && end < content.length()) {
                int idealEnd = Math.min(end + 100, content.length());
                int lastNewline = content.lastIndexOf('\n', idealEnd);
                int lastPeriod = content.lastIndexOf('。', idealEnd);
                int lastExclamation = content.lastIndexOf('！', idealEnd);
                int lastQuestion = content.lastIndexOf('？', idealEnd);

                int bestBreak = Math.max(Math.max(lastNewline, lastPeriod),
                        Math.max(lastExclamation, lastQuestion));

                // 确保断句点在合理范围内，避免切出太小的块
                if (bestBreak > start + CHUNK_SIZE - CHUNK_OVERLAP) {
                    end = bestBreak + 1;
                }
            }

            String chunk = content.substring(start, end).trim();
            if (!chunk.isEmpty()) {
                chunks.add(chunk);
            }

            // 计算下一个分块的起始位置
            start = Math.max(start + CHUNK_SIZE - CHUNK_OVERLAP, end);
        }

        return chunks;
    }

    /**
     * 从缓冲区提取文本块
     */
    private int extractChunksFromBuffer(StringBuilder buffer, BlockingQueue<String> chunkQueue, boolean isLastBuffer) {
        String content = buffer.toString();

        // 调用核心分块方法
        List<String> chunks = chunkContent(content, isLastBuffer);

        // 将生成的所有块放入队列
        for (String chunk : chunks) {
            chunkQueue.offer(chunk);
        }

        // 更新缓冲区，保留重叠部分 (这部分逻辑保持不变，因为它特定于流式处理)
        if (!isLastBuffer && content.length() > CHUNK_OVERLAP) {
            buffer.setLength(0);
            buffer.append(content.substring(content.length() - CHUNK_OVERLAP));
        } else {
            buffer.setLength(0);
        }

        return chunks.size();
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
            List<float[]> vectors = embeddingService.embedTexts(new ArrayList<>(batch));
            
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
    private void storeVectors(String fileName, String fileLocation, BlockingQueue<ChunkWithVector> vectorQueue, VectorBO vectorBO) {
        try {
            List<String> chunkBatch = new ArrayList<>();
            List<float[]> vectorBatch = new ArrayList<>();
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
                            vectorRecordService.saveVectorRecord(chunkBatch, vectorBatch, vectorBO);
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
                            vectorRecordService.saveVectorRecord(chunkBatch, vectorBatch, vectorBO);
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
     * 获取消息重试次数
     */
    private int getRetryCount(List<Map<String, Object>> xDeath) {
        if (xDeath == null || xDeath.isEmpty()) {
            return 0;
        }

        for (Map<String, Object> death : xDeath) {
            Object count = death.get("count");
            if (count instanceof Number) {
                return ((Number) count).intValue();
            }
        }
        return 0;
    }

    /**
     * 记录失败信息
     */
    private void recordFailureInfo(String message, Exception e, int retryCount) {
        try {
            // 这里可以记录到数据库、发送告警等
            log.warn("消息处理失败记录 - 消息: {}, 异常: {}, 重试次数: {}", message, e.getMessage(), retryCount);

            // 如果需要，可以在这里添加业务逻辑：
            // 1. 记录到失败日志表
            // 2. 发送告警通知
            // 3. 更新文件处理状态等
        } catch (Exception ex) {
            log.error("记录失败信息时出现异常", ex);
        }
    }

    /**
     * 文本块和向量的包装类
     */
    private record ChunkWithVector(String chunk, float[] vector) {
    }
}
