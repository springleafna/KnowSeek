package com.springleaf.knowseek.mq.consumer;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.springleaf.knowseek.enums.UploadStatusEnum;
import com.springleaf.knowseek.mapper.mysql.FileUploadMapper;
import com.springleaf.knowseek.model.bo.VectorBO;
import com.springleaf.knowseek.mq.event.BaseEvent;
import com.springleaf.knowseek.mq.event.FileVectorizeEvent;
import com.springleaf.knowseek.mq.parser.FileParserFactory;
import com.springleaf.knowseek.service.EmbeddingService;
import com.springleaf.knowseek.service.VectorRecordService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
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

    @Resource
    private FileUploadMapper fileUploadMapper;

    @Resource
    private FileParserFactory fileParserFactory;

    private final ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();
    private static final int BATCH_PROCESS_SIZE = 10; // 批量处理文本块数量
    private static final int QUEUE_TIMEOUT_SECONDS = 600; // 队列等待超时设为 10 分钟（600 秒），防止大文件解析慢导致误判

    @RabbitListener(queues = "${spring.rabbitmq.custom.vectorize.queue}")
    public void listener(String message, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag,
                         @Header(value = "x-death", required = false) List<Map<String, Object>> xDeath) {
        long startTime = System.currentTimeMillis();
        // 提前声明 fileId，用于失败时更新状态
        Long fileId = null;

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
            fileId = messageData.getVectorBO().getFileId();
            if (fileId == null) {
                throw new IllegalArgumentException("fileId 不能为空");
            }

            log.info("开始流式处理文件，文件名称：{}，文件类型: {}，文件地址: {}", fileName, extension, location);
            // 更新文件上传状态为“处理中”
            safeUpdateStatus(fileId, UploadStatusEnum.PROCESSING);
            // 使用流式处理
            processFileStreaming(location, extension, vectorBO);

            long processingTime = System.currentTimeMillis() - startTime;
            log.info("文档流式处理完成，耗时: {} ms", processingTime);

            // 更新文件上传状态为“处理完成”
            safeUpdateStatus(fileId, UploadStatusEnum.PROCESSING_COMPLETED);

        } catch (Exception e) {
            long processingTime = System.currentTimeMillis() - startTime;
            int retryCount = getRetryCount(xDeath);
            log.error("监听[消费者] 文件向量化任务，消费失败 topic: {} message: {}，耗时: {} ms，重试次数: {}",
                    topic, message, processingTime, retryCount, e);

            // 更新文件上传状态为“处理失败”
            if (fileId != null) {
                safeUpdateStatus(fileId, UploadStatusEnum.PROCESSING_FAILED);
                // 记录失败信息到数据库或日志系统（可选）
                recordFailureInfo(message, e, retryCount);
            } else {
                log.warn("fileId 为空，无法更新状态，原始消息: {}", message);
            }
            recordFailureInfo(message, e, retryCount);

            throw e;
        }
    }

    // 安全更新状态（加 try-catch 避免 DB 异常影响消息确认）
    private void safeUpdateStatus(Long fileId, UploadStatusEnum status) {
        try {
            fileUploadMapper.updateUploadStatus(fileId, status.getStatus());
        } catch (Exception dbEx) {
            log.error("更新文件状态失败，fileId: {}, status: {}", fileId, status, dbEx);
        }
    }

    /**
     * 流式处理文件向量化
     */
    private void processFileStreaming(String fileUrl, String extension, VectorBO vectorBO) {
        BlockingQueue<String> chunkQueue = new LinkedBlockingQueue<>(100);
        BlockingQueue<ChunkWithVector> vectorQueue = new LinkedBlockingQueue<>(100);

        // 异步执行三个阶段：下载+分块、向量化、存储
        CompletableFuture<Void> downloadFuture = CompletableFuture.runAsync(
                () -> {
                    try {
                        downloadAndChunkStreaming(fileUrl, extension, chunkQueue);
                    } catch (Exception e) {
                        log.error("下载和分块失败", e);
                        putToQueueWithTimeout(chunkQueue, "ERROR");
                        throw new RuntimeException("下载失败", e);
                    }
                }, executorService);

        CompletableFuture<Void> vectorizeFuture = CompletableFuture.runAsync(
                () -> {
                    try {
                        vectorizeChunks(chunkQueue, vectorQueue);
                    } catch (Exception e) {
                        log.error("向量化失败", e);
                        putToQueueWithTimeout(vectorQueue, new ChunkWithVector("ERROR", null));
                        throw new RuntimeException("向量化失败", e);
                    }
                }, executorService);

        CompletableFuture<Void> storageFuture = CompletableFuture.runAsync(
                () -> {
                    try {
                        storeVectors(vectorQueue, vectorBO);
                    } catch (Exception e) {
                        log.error("存储失败", e);
                        throw new RuntimeException("存储失败", e);
                    }
                }, executorService);

        try {
            // 等待所有任务完成，设置超时时间
            CompletableFuture.allOf(downloadFuture, vectorizeFuture, storageFuture)
                    .get(QUEUE_TIMEOUT_SECONDS, TimeUnit.SECONDS); // 10分钟超时
        } catch (Exception e) {
            log.error("流式处理超时或失败（超过 {} 秒）", QUEUE_TIMEOUT_SECONDS, e);
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
        HttpURLConnection connection;

        try {
            log.info("开始流式下载和解析文件: {}", fileUrl);

            URL url = new URL(fileUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(30000);

            try (InputStream inputStream = connection.getInputStream()) {
                // 根据文件类型选择不同的处理策略
                fileParserFactory.getParserByExtension(extension).parse(inputStream, chunkQueue);
                log.info("文件解析和分块完成: {}", fileUrl);
            } finally {
                connection.disconnect();
                // 确保在所有处理完成后发送结束信号
                putToQueueWithTimeout(chunkQueue, "EOF");
                log.info("已发送EOF信号。");
            }

        } catch (Exception e) {
            log.error("流式下载或解析文件失败: {}", fileUrl, e);
            putToQueueWithTimeout(chunkQueue, "ERROR");
            throw new RuntimeException("流式下载或解析失败", e);
        }
    }

    /**
     * 异步向量化处理
     */
    private void vectorizeChunks(BlockingQueue<String> chunkQueue, BlockingQueue<ChunkWithVector> vectorQueue) {
        try {
            List<String> batch = new ArrayList<>();
            String chunk;

            while (true) {
                // 等待最多 10 分钟
                chunk = chunkQueue.poll(QUEUE_TIMEOUT_SECONDS, TimeUnit.SECONDS);

                if (chunk == null) {
                    log.error("向量化线程等待文本块超过 {} 秒，判定为异常，终止处理", QUEUE_TIMEOUT_SECONDS);
                    putToQueueWithTimeout(vectorQueue, new ChunkWithVector("ERROR", null));
                    break;
                }

                if ("EOF".equals(chunk)) {
                    // 处理最后一批
                    if (!batch.isEmpty()) {
                        processBatch(batch, vectorQueue);
                    }
                    putToQueueWithTimeout(vectorQueue, new ChunkWithVector("EOF", null));
                    break;
                }

                if ("ERROR".equals(chunk)) {
                    putToQueueWithTimeout(vectorQueue, new ChunkWithVector("ERROR", null));
                    break;
                }

                batch.add(chunk);

                // 批量处理
                if (batch.size() >= BATCH_PROCESS_SIZE) {
                    processBatch(batch, vectorQueue);
                    batch.clear();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("向量化线程被中断");
            putToQueueWithTimeout(vectorQueue, new ChunkWithVector("ERROR", null));
        } catch (Exception e) {
            log.error("向量化处理失败", e);
            putToQueueWithTimeout(vectorQueue, new ChunkWithVector("ERROR", null));
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
                putToQueueWithTimeout(vectorQueue, new ChunkWithVector(batch.get(i), vectors.get(i)));
            }
            log.info("批次向量化完成，生成 {} 个向量", vectors.size());
        } catch (Exception e) {
            log.error("批次向量化失败", e);
            throw new RuntimeException("批次向量化失败", e);
        }
    }

    /**
     * 异步批量存储到pgvector —— 同样使用 10 分钟兜底超时
     */
    private void storeVectors(BlockingQueue<ChunkWithVector> vectorQueue, VectorBO vectorBO) {
        try {
            List<String> chunkBatch = new ArrayList<>();
            List<float[]> vectorBatch = new ArrayList<>();
            ChunkWithVector item;

            // 全局记录器记录总的分片数
            int globalChunkIndex = 1;

            while (true) {
                item = vectorQueue.poll(QUEUE_TIMEOUT_SECONDS, TimeUnit.SECONDS);

                if (item == null) {
                    log.error("存储线程等待向量超过 {} 秒，判定为异常，终止处理", QUEUE_TIMEOUT_SECONDS);
                    break;
                }

                if ("EOF".equals(item.chunk())) {
                    // 处理最后一批
                    if (!chunkBatch.isEmpty()) {
                        vectorRecordService.saveVectorRecord(chunkBatch, vectorBatch, globalChunkIndex, vectorBO);
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
                        vectorRecordService.saveVectorRecord(chunkBatch, vectorBatch, globalChunkIndex, vectorBO);
                        globalChunkIndex += chunkBatch.size(); // 累加
                        log.info("批量存储完成，共 {} 个向量", chunkBatch.size());
                        chunkBatch.clear();
                        vectorBatch.clear();
                    }
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
     * 带中断处理和超时的阻塞 put
     * 防止队列满时永久阻塞（虽然有外部超时控制，但这样更安全）
     */
    private <T> void putToQueueWithTimeout(BlockingQueue<T> queue, T item) {
        try {
            // 尝试放入，最多等待 60 秒（通常很快，除非死锁）
            boolean success = queue.offer(item, 60, TimeUnit.SECONDS);
            if (!success) {
                log.error("严重错误：队列堵塞超过60秒，丢弃数据以防止系统挂起。Item: {}", item);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("放入队列被中断");
        }
    }

    /**
     * 文本块和向量的包装类
     */
    public record ChunkWithVector(String chunk, float[] vector) {
    }
}
