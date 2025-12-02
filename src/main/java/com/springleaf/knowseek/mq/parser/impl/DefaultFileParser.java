package com.springleaf.knowseek.mq.parser.impl;

import com.springleaf.knowseek.mq.parser.AbstractFileParserStrategy;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.BlockingQueue;

/**
 * 默认的文件解析器
 * 使用 Apache Tika 从输入流中提取文本，然后进行分块。
 * 这是一个“伪流式”处理：Tika会先在内存中构建文档模型来提取文本。
 */
@Slf4j
@Component("default")
public class DefaultFileParser extends AbstractFileParserStrategy {

    @Override
    public void parse(InputStream inputStream, BlockingQueue<String> chunkQueue) {
        log.info("执行默认文件解析器，采用 Apache Tika 从输入流中提取文本 进行处理。");

        Tika tika = new Tika();
        try {
            // Tika 会处理所有解析逻辑，并返回一个包含所有文本的长字符串
            String extractedText = tika.parseToString(inputStream);

            if (extractedText == null || extractedText.trim().isEmpty()) {
                log.warn("Tika未能从文件中提取任何文本。");
                return;
            }

            // 对提取出的完整文本进行分块
            chunkExtractedText(extractedText, chunkQueue);
        } catch (IOException | TikaException e) {
            log.error("文件解析失败: {}", e.getMessage(), e);
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
}
