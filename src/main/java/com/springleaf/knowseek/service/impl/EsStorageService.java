package com.springleaf.knowseek.service.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EsStorageService {

    private final ElasticsearchClient esClient;

    public void saveChunks(String fileName, String fileLocation, List<String> chunks, List<float[]> vectors) throws IOException {
        for (int i = 1; i <= chunks.size(); i++) {
            Map<String, Object> doc = new HashMap<>();
            doc.put("content", chunks.get(i));
            doc.put("vector", vectors.get(i));
            doc.put("fileName", fileName);
            doc.put("fileLocation", fileLocation);
            doc.put("chunkIndex", i);
            doc.put("timestamp", System.currentTimeMillis());

            esClient.index(IndexRequest.of(r -> r
                    .index("documents")
                    .id(UUID.randomUUID().toString())
                    .document(doc)
            ));
        }
    }
}

