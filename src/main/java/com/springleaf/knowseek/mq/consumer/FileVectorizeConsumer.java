package com.springleaf.knowseek.mq.consumer;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.springleaf.knowseek.enums.UploadStatusEnum;
import com.springleaf.knowseek.mapper.mysql.FileUploadMapper;
import com.springleaf.knowseek.model.bo.VectorBO;
import com.springleaf.knowseek.mq.event.BaseEvent;
import com.springleaf.knowseek.mq.event.FileVectorizeEvent;
import com.springleaf.knowseek.service.EmbeddingService;
import com.springleaf.knowseek.service.VectorRecordService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.tika.Tika;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
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

    private final ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();    private static final int BUFFER_SIZE = 8192; // 8KB 缓冲区
    private static final int CHUNK_SIZE = 1000; // 文本块大小
    private static final int CHUNK_OVERLAP = 100; // 重叠大小
    private static final int BATCH_PROCESS_SIZE = 10; // 批量处理文本块数量
    private static final int QUEUE_TIMEOUT_SECONDS = 600; // 队列等待超时设为 10 分钟（600 秒），防止大文件解析慢导致误判

    // 定义纯文本类型，这些类型可以使用最高效的流式读取
    private static final Set<String> PLAIN_TEXT_EXTENSIONS = new HashSet<>(Arrays.asList(
            "txt", "md", "xml", "html", "htm", "json", "csv"
    ));

    // 定义分片分隔符优先级
    // 1. 双换行（段落）
    // 2. 单换行
    // 3. 句子结束符（支持中英文常见标点）
    private static final List<String> SEPARATORS = Arrays.asList(
            "\n\n",
            "\n",
            "。|！|？|\\.|\\!|\\?", // 正则表达式
            " "
    );

    @RabbitListener(queues = "${spring.rabbitmq.custom.vectorize.queue}")
    public void listener(String message, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag,
                         @Header(value = "x-death", required = false) List<Map<String, Object>> xDeath) throws Exception {
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
    private void processFileStreaming(String fileUrl, String extension, VectorBO vectorBO) throws Exception {
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
                if (isPlainText(extension)) {
                    // 对于纯文本，使用最高效的逐行读取流式处理
                    log.info("文件类型 '{}'，采用纯文本流式处理。", extension);
                    processPlainTextStream(inputStream, chunkQueue);
                } else if (extension.equalsIgnoreCase("pdf")) {
                    // 对于PDF，使用临时文件方式处理大文件
                    log.info("文件类型 '{}'，使用 Apache PDFBox 进行解析。", extension);
                    processPdfStream(inputStream, chunkQueue);
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
     *
     * @param inputStream 文件输入流
     * @param chunkQueue  文本块队列
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
     * 提取 pdf 文本
     */
    private void processPdfStream(InputStream inputStream, BlockingQueue<String> chunkQueue) throws IOException {
        Path tempFile = null;

        try {
            // 创建临时文件
            tempFile = Files.createTempFile("pdf_processing_", ".pdf");
            log.debug("创建临时PDF文件: {}", tempFile);

            // 将输入流复制到临时文件
            Files.copy(inputStream, tempFile, StandardCopyOption.REPLACE_EXISTING);

            // 从临时文件加载PDF
            try (PDDocument document = Loader.loadPDF(tempFile.toFile())) {
                PDFTextStripper stripper = new PDFTextStripper();
                int totalPages = document.getNumberOfPages();

                log.info("开始解析PDF，总页数: {}", totalPages);

                for (int page = 1; page <= totalPages; page++) {
                    stripper.setStartPage(page);
                    stripper.setEndPage(page);
                    String pageText = stripper.getText(document);
                    if (pageText != null && !pageText.trim().isEmpty()) {
                        chunkExtractedText(pageText, chunkQueue);
                        log.debug("已处理第 {} 页，文本长度: {}", page, pageText.length());
                    }

                    // 定期提示进度
                    if (page % 10 == 0) {
                        log.info("当前进度： {}/{} 页", page, totalPages);
                    }
                }
                log.info("PDF解析完成，共处理 {} 页", totalPages);
            }
        } finally {
            // 清理临时文件
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                    log.debug("已清理临时文件: {}", tempFile);
                } catch (IOException e) {
                    log.warn("无法删除临时文件: {}", tempFile, e);
                }
            }
        }
    }

    /**
     * 对一个已知的长字符串进行分块处理
     *
     * @param text       要分块的文本
     * @param chunkQueue 文本块队列
     */
    private void chunkExtractedText(String text, BlockingQueue<String> chunkQueue) {
        List<String> chunks = splitTextRecursively(text);

        // 将生成的所有块放入队列
        for (String chunk : chunks) {
            chunkQueue.offer(chunk);
        }

        log.info("从Tika提取的文本中生成了 {} 个文本块", chunks.size());
    }

    /**
     * 专门处理纯文本的“真·流式”方法，内存占用最低。
     * 优化：增加残留缓冲区处理，避免在流的边界切断句子
     *
     * @param inputStream 文件输入流
     * @param chunkQueue  文本块队列
     */
    private void processPlainTextStream(InputStream inputStream, BlockingQueue<String> chunkQueue) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8), BUFFER_SIZE)) {
            StringBuilder buffer = new StringBuilder();
            String line;
            int totalChunks = 0;

            while ((line = reader.readLine()) != null) {
                buffer.append(line).append("\n");

                // 当缓冲区积累到一定大小时进行处理
                // 这里的 buffer 大小建议比 CHUNK_SIZE 大一些，以便找到合适的切分点
                if (buffer.length() >= CHUNK_SIZE * 3) {
                    totalChunks += extractChunksFromBufferSmart(buffer, chunkQueue, false);
                }
            }

            // 处理剩余的所有内容
            if (!buffer.isEmpty()) {
                totalChunks += extractChunksFromBufferSmart(buffer, chunkQueue, true);
            }
            log.info("纯文本流式处理共生成 {} 个文本块", totalChunks);
        }
    }

    /**
     * 智能从缓冲区提取文本块
     * 逻辑：找到缓冲区中最后一个安全的换行点，只处理那之前的数据，剩余数据保留在 StringBuilder 中
     */
    private int extractChunksFromBufferSmart(StringBuilder buffer, BlockingQueue<String> chunkQueue, boolean isLastBuffer) {
        String content = buffer.toString();

        String textToProcess;
        String remainingText = "";

        if (isLastBuffer) {
            textToProcess = content;
            buffer.setLength(0); // 清空
        } else {
            // 寻找最后一个换行符，保证我们处理的是完整的行
            int lastNewline = content.lastIndexOf('\n');
            if (lastNewline > -1) {
                textToProcess = content.substring(0, lastNewline + 1);
                remainingText = content.substring(lastNewline + 1);
            } else {
                // 极端情况：缓冲区全是没换行的一句话，只能硬处理
                textToProcess = content;
            }
        }

        // 调用核心分块方法
        List<String> chunks = splitTextRecursively(textToProcess);

        for (String chunk : chunks) {
            chunkQueue.offer(chunk);
        }

        // 重置缓冲区并填入残留文本
        if (!isLastBuffer) {
            buffer.setLength(0);
            buffer.append(remainingText);

            // 上下文连贯性优化：
            // 如果使用了流式分块，最后生成的 chunks 可能没有与 remainingText 形成重叠。
            // 在这里我们手动保留最后一个 chunk 的一部分作为"前文"，加到 buffer 最前面。
            // 这样下一次处理 buffer 时，开头就是上一次的结尾，实现了跨 buffer 的重叠。
            if (!chunks.isEmpty()) {
                String lastChunk = chunks.get(chunks.size() - 1);
                int keepLen = Math.min(lastChunk.length(), CHUNK_OVERLAP);
                // 插入到 buffer 头部
                buffer.insert(0, lastChunk.substring(lastChunk.length() - keepLen));
            }
        }

        return chunks.size();
    }

    /**
     * 核心分片方法：递归语义分片
     */
    private List<String> splitTextRecursively(String text) {
        return splitTextInternal(text, SEPARATORS);
    }

    /**
     * 内部递归方法
     */
    private List<String> splitTextInternal(String text, List<String> separators) {
        String separator = separators.get(0); // 当前使用的分隔符
        List<String> nextSeparators = separators.subList(1, separators.size()); // 剩余的分隔符

        // 1. 按照当前分隔符分割文本
        List<String> splits = new ArrayList<>();

        // 处理正则表达式特殊字符
        boolean isRegex = separator.contains("|") || separator.contains("\\");

        if (isRegex) {
            // 如果是正则（句子分隔符），我们需要保留分隔符本身（比如句号）
            // 使用 lookbehind 技巧或者手动分割来保留标点
            // 简单起见，这里使用 split 但可能会丢失标点，为了严谨建议使用 Matcher
            // 这里为了性能和代码简洁，使用简单的正则分割，若需保留标点可优化
            String[] rawSplits = text.split("(?<=" + separator + ")");
            Collections.addAll(splits, rawSplits);
        } else {
            String[] rawSplits = text.split(separator);
            for (String s : rawSplits) {
                if(!s.trim().isEmpty()) {
                    splits.add(s + (separator.equals("\n\n") || separator.equals("\n") ? separator : ""));
                }
            }
        }

        // 2. 合并碎片，形成最终的 Chunks
        List<String> goodSplits = new ArrayList<>();

        for (String s : splits) {
            if (s.length() < CHUNK_SIZE) {
                goodSplits.add(s);
            } else {
                // 如果当前碎片依然过大，且还有更细粒度的分隔符，则递归处理
                if (!nextSeparators.isEmpty()) {
                    goodSplits.addAll(splitTextInternal(s, nextSeparators));
                } else {
                    // 如果没有分隔符了，只能硬切（原样保留或按字符切）
                    goodSplits.add(s);
                }
            }
        }

        // 3. 组装 Chunks (Sliding Window with Overlap)
        return mergeSplits(goodSplits, separator);
    }

    /**
     * 合并切分好的小片段，使其长度接近 CHUNK_SIZE，并保持重叠
     */
    private List<String> mergeSplits(List<String> splits, String separator) {
        List<String> docs = new ArrayList<>();
        StringBuilder currentDoc = new StringBuilder();

        // 用于构建重叠部分的队列
        Deque<String> overlapWindow = new LinkedList<>();
        int currentOverlapSize = 0;

        for (String split : splits) {
            // 如果加入当前片段后超过了块大小
            if (currentDoc.length() + split.length() > CHUNK_SIZE) {
                if (!currentDoc.isEmpty()) {
                    String doc = currentDoc.toString().trim();
                    if (!doc.isEmpty()) {
                        docs.add(doc);
                    }

                    // 这里的核心优化：重叠的内容是基于之前的“完整语义片段”构建的，而不是硬切字符
                    currentDoc.setLength(0);

                    // 从 overlapWindow 中恢复内容作为新 chunk 的开头
                    // 这里的 overlapWindow 存的是完整的句子或段落
                    for (String s : overlapWindow) {
                        currentDoc.append(s);
                    }
                }
            }

            currentDoc.append(split);

            // --- 维护重叠窗口 ---
            overlapWindow.addLast(split);
            currentOverlapSize += split.length();

            // 如果窗口过大（超过了设定的重叠大小），移除最早的片段
            // 这样保证 overlapWindow 里始终保留着最近的约 CHUNK_OVERLAP 长度的完整语义片段
            while (currentOverlapSize > CHUNK_OVERLAP && overlapWindow.size() > 1) {
                // 保留至少一个，防止死循环
                String removed = overlapWindow.removeFirst();
                currentOverlapSize -= removed.length();
            }
        }

        // 处理最后一个块
        if (!currentDoc.isEmpty()) {
            String doc = currentDoc.toString().trim();
            if (!doc.isEmpty()) {
                docs.add(doc);
            }
        }

        return docs;
    }

    /**
     * 从缓冲区提取文本块
     */
    private int extractChunksFromBuffer(StringBuilder buffer, BlockingQueue<String> chunkQueue, boolean isLastBuffer) {
        String content = buffer.toString();

        // 调用核心分块方法
        List<String> chunks = splitTextRecursively(content);

        // 将生成的所有块放入队列
        for (String chunk : chunks) {
            chunkQueue.offer(chunk);
        }

        // 更新缓冲区，保留重叠部分
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
                // 等待最多 10 分钟
                chunk = chunkQueue.poll(QUEUE_TIMEOUT_SECONDS, TimeUnit.SECONDS);

                if (chunk == null) {
                    log.error("向量化线程等待文本块超过 {} 秒，判定为异常，终止处理", QUEUE_TIMEOUT_SECONDS);
                    vectorQueue.offer(new ChunkWithVector("ERROR", null));
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
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("向量化线程被中断");
            vectorQueue.offer(new ChunkWithVector("ERROR", null));
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
     * 文本块和向量的包装类
     */
    public record ChunkWithVector(String chunk, float[] vector) {
    }
}
