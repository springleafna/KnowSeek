package com.springleaf.knowseek.service.impl;

import com.springleaf.knowseek.mapper.pgvector.VectorRecordMapper;
import com.springleaf.knowseek.model.entity.VectorRecord;
import com.springleaf.knowseek.service.VectorRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class VectorRecordServiceImpl implements VectorRecordService {

    private final VectorRecordMapper vectorRecordMapper;


    @Override
    public void insertVectorRecord(VectorRecord record) {
        vectorRecordMapper.insert(record);
    }
}
