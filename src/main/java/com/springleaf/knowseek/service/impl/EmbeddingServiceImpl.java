package com.springleaf.knowseek.service.impl;

import com.springleaf.knowseek.service.EmbeddingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddingServiceImpl implements EmbeddingService {

    private final EmbeddingModel embeddingModel;
    private static final long RATE_LIMIT_DELAY_MS = 3500; // 每3.5秒调用一次，确保不超过1800RPM
    private static final int MAX_BATCH_SIZE = 25; // 阿里云 DashScope 的批处理限制

    @Override
    public List<float[]> embedTexts(List<String> texts) {
        List<float[]> allEmbeddings = new ArrayList<>();
        int totalBatches = (texts.size() + MAX_BATCH_SIZE - 1) / MAX_BATCH_SIZE;

        log.info("开始向量化处理，共 {} 个文本，分 {} 批处理", texts.size(), totalBatches);

        // 分批处理文本
        for (int i = 0; i < texts.size(); i += MAX_BATCH_SIZE) {
            int endIndex = Math.min(i + MAX_BATCH_SIZE, texts.size());
            List<String> batch = texts.subList(i, endIndex);
            int batchNumber = (i / MAX_BATCH_SIZE) + 1;
            
            // 限流：除第一批外，每批之间等待
            if (i > 0) {
                try {
                    log.debug("限流等待 {} 毫秒后处理第 {} 批（共 {} 个文本）",
                            RATE_LIMIT_DELAY_MS, batchNumber, batch.size());
                    Thread.sleep(RATE_LIMIT_DELAY_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("向量化处理被中断", e);
                }
            }
            log.debug("正在向量化第 {}/{} 批（{}个文本）",
                    batchNumber, totalBatches, batch.size());

            // 对当前批次进行向量化
            List<float[]> batchEmbeddings = embeddingModel.embed(batch);
            allEmbeddings.addAll(batchEmbeddings);

            log.debug("第 {} 批向量化完成，本批生成 {} 个向量",
                    batchNumber, batchEmbeddings.size());
        }

        log.info("向量化处理完成，共 {} 个文本生成 {} 个向量",
                texts.size(), allEmbeddings.size());
        return allEmbeddings;
    }
}
