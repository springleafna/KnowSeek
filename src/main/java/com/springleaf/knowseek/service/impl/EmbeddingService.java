package com.springleaf.knowseek.service.impl;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class EmbeddingService {
    private final EmbeddingModel embeddingModel;

    public EmbeddingService(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    public List<List<Double>> embedTexts(List<String> texts) {
        List<float[]> embeddings = embeddingModel.embed(texts);
        return embeddings.stream()
                .map(floatArray -> {
                    List<Double> doubleList = new ArrayList<>();
                    for (float f : floatArray) {
                        doubleList.add((double) f);
                    }
                    return doubleList;
                })
                .collect(java.util.stream.Collectors.toList());
    }
}
