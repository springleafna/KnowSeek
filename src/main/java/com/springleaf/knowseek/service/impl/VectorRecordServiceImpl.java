package com.springleaf.knowseek.service.impl;

import com.springleaf.knowseek.mapper.pgvector.VectorRecordMapper;
import com.springleaf.knowseek.model.bo.VectorBO;
import com.springleaf.knowseek.model.entity.VectorRecord;
import com.springleaf.knowseek.service.VectorRecordService;
import com.springleaf.knowseek.utils.VectorUtil;
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
    public void saveVectorRecord(List<String> chunks, List<float[]> vectors, int startChunkIndex, VectorBO vectorBO) {
        for (int i = 0; i < chunks.size(); i++) {
            VectorRecord record = VectorRecord.builder()
                    .fileId(vectorBO.getFileId())
                    .userId(vectorBO.getUserId())
                    .organizationId(vectorBO.getOrganizationId())
                    .knowledgeBaseId(vectorBO.getKnowledgeBaseId())
                    .chunkIndex(startChunkIndex + i)
                    .chunkText(chunks.get(i))
                    .embedding(VectorUtil.vectorToString(vectors.get(i)))
                    .build();

            vectorRecordMapper.insert(record);
        }
    }
}
