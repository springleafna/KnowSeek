package com.springleaf.knowseek.mq.parser.impl;

import com.springleaf.knowseek.mq.parser.AbstractFileParserStrategy;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.stereotype.Component;
import org.xml.sax.helpers.DefaultHandler;

import java.io.InputStream;
import java.util.List;
import java.util.concurrent.BlockingQueue;

@Slf4j
@Component("default")
public class DefaultFileParser extends AbstractFileParserStrategy {

    // 建议将 AutoDetectParser 设为单例或静态，因为它包含加载好的配置
    private static final AutoDetectParser PARSER = new AutoDetectParser();

    // 缓冲区阈值：每当提取累积到由 5 个 Chunk 大小时，就进行一次处理
    // 既避免了内存溢出，又保证了有足够的上下文进行语义切分
    private static final int PARSE_BUFFER_THRESHOLD = CHUNK_SIZE * 5;

    @Override
    public void parse(InputStream inputStream, BlockingQueue<String> chunkQueue) {
        log.info("执行默认文件解析器，采用 Apache Tika (SAX流式) 进行处理。");

        try {
            // 1. 创建自定义的 Handler，用于流式接收文本
            StreamingChunkHandler handler = new StreamingChunkHandler(chunkQueue);

            // 2. 元数据容器（虽然暂时不用，但 Tika 需要）
            Metadata metadata = new Metadata();

            // 3. 上下文
            ParseContext context = new ParseContext();

            // 4. 执行解析
            // BodyContentHandler 用于忽略 HTML 标签等，只提取正文
            BodyContentHandler bodyHandler = new BodyContentHandler(handler);

            PARSER.parse(inputStream, bodyHandler, metadata, context);

            // 5. 解析结束后，处理缓冲区中剩余的文本
            handler.flush();

        } catch (Exception e) {
            log.error("文件流式解析失败", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 内部类：自定义 Tika 事件处理器
     * 核心逻辑：像接水管一样，接满一桶水（Buffer）就切分处理一次，而不是等水库（File）全流完。
     */
    private class StreamingChunkHandler extends DefaultHandler {
        private final BlockingQueue<String> queue;
        private final StringBuilder buffer = new StringBuilder();
        private String lastSectionTail = ""; // 用于连接上一次缓冲区的尾巴

        public StreamingChunkHandler(BlockingQueue<String> queue) {
            this.queue = queue;
        }

        @Override
        public void characters(char[] ch, int start, int length) {
            // Tika 解析到文本时会回调此方法
            buffer.append(ch, start, length);

            // 如果缓冲区超过阈值，进行一次“局部切分”
            if (buffer.length() > PARSE_BUFFER_THRESHOLD) {
                processBuffer(false);
            }
        }

        public void flush() {
            // 处理剩余文本
            if (!buffer.isEmpty()) {
                processBuffer(true);
            }
        }

        private void processBuffer(boolean isEnd) {
            String currentText = buffer.toString();

            // 拼接上一次的尾巴，保证跨 Buffer 的句子完整性
            String fullText = lastSectionTail + currentText;

            // 调用父类的递归切分逻辑
            List<String> chunks = splitTextRecursively(fullText);

            // 关键逻辑：处理重叠和尾部
            // 如果不是最后一次 flush，我们需要保留最后一个 Chunk 的一部分作为下一次的开头
            // 或者，由于 splitTextRecursively 可能会产生很多小块，
            // 我们可以简单地把生成的所有 Chunk 发送出去，
            // 但需要注意：递归切分的最后一个 Chunk 可能因为没有结束符而被切断。

            // 优化策略：
            // 直接发送所有完整 Chunk，但保留最后一段可能不完整的文本留在 lastSectionTail

            if (!chunks.isEmpty()) {
                // 将大部分 Chunk 放入队列
                int limit = isEnd ? chunks.size() : chunks.size() - 1;

                for (int i = 0; i < limit; i++) {
                    try {
                        queue.put(chunks.get(i));
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }

                // 处理连接处
                if (!isEnd) {
                    // 如果不是结束，保留最后一个块作为下一次的上下文
                    // 这样可以防止一个长句子正好被 PARSE_BUFFER_THRESHOLD 切断
                    lastSectionTail = chunks.get(chunks.size() - 1);

                    // 只有当保留的尾巴太长时（超过 ChunkSize），才被迫切出去一部分
                    if (lastSectionTail.length() > CHUNK_SIZE) {
                        // 如果太长，说明这块本身就是个完整的大块，直接发走，只留重叠部分
                        try {
                            queue.put(lastSectionTail);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                        //以此处结尾保留重叠
                        lastSectionTail = lastSectionTail.substring(Math.max(0, lastSectionTail.length() - CHUNK_OVERLAP));
                    }
                }
            } else {
                // buffer 里全是空或者没切出来
                lastSectionTail = fullText;
            }

            // 清空缓冲区
            buffer.setLength(0);
        }
    }
}
