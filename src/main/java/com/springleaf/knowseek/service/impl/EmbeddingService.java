package com.springleaf.knowseek.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class EmbeddingService {
    private final EmbeddingModel embeddingModel;
    private static final long RATE_LIMIT_DELAY_MS = 3500; // 每3.5秒调用一次，确保不超过1800RPM

    public EmbeddingService(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    public List<List<Double>> embedTexts(List<String> texts) {
        final int BATCH_SIZE = 25; // 阿里云 DashScope 的批处理限制
        List<List<Double>> allEmbeddings = new ArrayList<>();
        
        log.info("开始向量化处理，共 {} 个文本，将分 {} 批处理", texts.size(), (texts.size() + BATCH_SIZE - 1) / BATCH_SIZE);
        
        // 分批处理文本
        for (int i = 0; i < texts.size(); i += BATCH_SIZE) {
            int endIndex = Math.min(i + BATCH_SIZE, texts.size());
            List<String> batch = texts.subList(i, endIndex);
            
            // 限流：除第一批外，每批之间等待
            if (i > 0) {
                try {
                    log.info("限流等待 {} 毫秒后处理第 {} 批（共 {} 个文本）", 
                            RATE_LIMIT_DELAY_MS, (i / BATCH_SIZE) + 1, batch.size());
                    Thread.sleep(RATE_LIMIT_DELAY_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("向量化处理被中断", e);
                }
            } else {
                log.info("开始处理第 1 批（共 {} 个文本）", batch.size());
            }
            
            // 对当前批次进行向量化
            List<float[]> batchEmbeddings = embeddingModel.embed(batch);
            
            // 转换为 Double 列表并添加到结果中
            List<List<Double>> batchDoubleEmbeddings = batchEmbeddings.stream()
                    .map(floatArray -> {
                        List<Double> doubleList = new ArrayList<>();
                        for (float f : floatArray) {
                            doubleList.add((double) f);
                        }
                        return doubleList;
                    })
                    .collect(java.util.stream.Collectors.toList());
            
            allEmbeddings.addAll(batchDoubleEmbeddings);
            log.info("第 {} 批处理完成，已生成 {} 个向量", (i / BATCH_SIZE) + 1, allEmbeddings.size());
        }
        
        log.info("所有文本向量化完成，共生成 {} 个向量", allEmbeddings.size());
        return allEmbeddings;
    }
}
