package com.springleaf.knowseek.service.impl;

import com.springleaf.knowseek.mapper.pgvector.VectorRecordMapper;
import com.springleaf.knowseek.model.bo.VectorBO;
import com.springleaf.knowseek.model.entity.VectorRecord;
import com.springleaf.knowseek.service.VectorRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class VectorRecordServiceImpl implements VectorRecordService {

    private final VectorRecordMapper vectorRecordMapper;

    @Override
    public void saveVectorRecord(List<String> chunks, List<float[]> vectors, VectorBO vectorBO) {
        VectorRecord record = VectorRecord.builder()
                .fileId(vectorBO.getFileId())
                .userId(vectorBO.getUserId())
                .organizationId(vectorBO.getOrganizationId())
                .knowledgeBaseId(vectorBO.getKnowledgeBaseId())
                .build();
        for (int i = 0; i < chunks.size(); i++) {
            record.setChunkIndex(i + 1);
            record.setChunkText(chunks.get(i));
            record.setEmbedding(vectors.get(i));
            vectorRecordMapper.insert(record);
        }
    }
}
