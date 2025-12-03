package com.springleaf.knowseek.mq.parser.impl;

import com.springleaf.knowseek.mq.parser.AbstractFileParserStrategy;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.concurrent.BlockingQueue;

/**
 * pdf文件解析器
 */
@Slf4j
@Component("pdf")
public class PdfFileParser extends AbstractFileParserStrategy {

    private String lastPageTail = "";
    
    @Override
    public void parse(InputStream inputStream, BlockingQueue<String> chunkQueue) {
        log.info("执行PDF文件解析器，采用 临时文件 +  Apache PDFBox依赖 进行处理。");

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
        } catch (IOException e) {
            throw new RuntimeException(e);
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
        // 将上一页的尾部加到当前页开头
        String combinedText = lastPageTail + text;
        List<String> chunks = splitTextRecursively(combinedText);

        // 更新 lastPageTail 为当前页末尾（用于下一页）
        if (!chunks.isEmpty()) {
            String lastChunk = chunks.get(chunks.size() - 1);
            int tailLength = Math.min(lastChunk.length(), CHUNK_OVERLAP);
            lastPageTail = lastChunk.substring(lastChunk.length() - tailLength);
        } else {
            lastPageTail = "";
        }

        // 将生成的所有块放入队列
        for (String chunk : chunks) {
            putToQueue(chunkQueue, chunk);
        }

        log.info("从Tika提取的文本中生成了 {} 个文本块", chunks.size());
    }
}
