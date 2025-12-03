package com.springleaf.knowseek.mq.parser.impl;

import com.springleaf.knowseek.mq.parser.AbstractFileParserStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.BlockingQueue;

/**
 * 纯文本文件解析器
 * 专门处理纯文本的“真·流式”方法，内存占用最低。
 * 并增加残留缓冲区处理，避免在流的边界切断句子
 */
@Slf4j
@Component("txt")
public class TxtFileParser extends AbstractFileParserStrategy {

    @Override
    public void parse(InputStream inputStream, BlockingQueue<String> chunkQueue) {
        log.info("执行纯文本文件解析器，采用 纯文本流式 进行处理。");

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8),
                BUFFER_SIZE)) {
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
        } catch (IOException e) {
            throw new RuntimeException(e);
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
            // 寻找最后一个换行符，保证处理的是完整的行
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
            putToQueue(chunkQueue, chunk);
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
}
