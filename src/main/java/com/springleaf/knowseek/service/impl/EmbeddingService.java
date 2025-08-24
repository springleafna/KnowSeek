package com.springleaf.knowseek.service.impl;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EmbeddingService {
    private final EmbeddingModel embeddingModel;

    public EmbeddingService(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    public List<List<Double>> embedTexts(List<String> texts) {
        //return embeddingModel.embed(texts);
        return null;
    }
}
