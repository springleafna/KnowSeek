package com.springleaf.knowseek.service.impl;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class OssFileProcessor {
    private final EmbeddingService embeddingService;
    private final EsStorageService esStorageService;

    public OssFileProcessor(EmbeddingService embeddingService,
                            EsStorageService esStorageService) {
        this.embeddingService = embeddingService;
        this.esStorageService = esStorageService;
    }

    public void processOssFile(String ossUrl) throws IOException {
        String text = downloadFromOSS(ossUrl);
        List<String> chunks = splitText(text, 600, 100);
        List<List<Double>> vectors = embeddingService.embedTexts(chunks);
        esStorageService.saveChunks(chunks, vectors);
    }

    private String downloadFromOSS(String ossUrl) throws IOException {
        try (InputStream in = new URL(ossUrl).openStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private List<String> splitText(String text, int chunkSize, int overlap) {
        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(text.length(), start + chunkSize);
            chunks.add(text.substring(start, end));
            start += chunkSize - overlap;
        }
        return chunks;
    }
}

