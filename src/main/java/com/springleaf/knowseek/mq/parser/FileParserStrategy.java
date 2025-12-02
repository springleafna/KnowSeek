package com.springleaf.knowseek.mq.parser;

import java.io.InputStream;
import java.util.concurrent.BlockingQueue;

/**
 * 文件解析策略接口
 */
public interface FileParserStrategy {

    /**
     * 文件解析
     * @param inputStream 根据 URL下载文件的输入流
     * @param chunkQueue 分块队列
     */
    void parse(InputStream inputStream, BlockingQueue<String> chunkQueue);
}
