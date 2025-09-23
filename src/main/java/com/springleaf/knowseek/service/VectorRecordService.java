package com.springleaf.knowseek.service;

import com.springleaf.knowseek.model.bo.VectorBO;

import java.util.List;

public interface VectorRecordService {

    void saveVectorRecord(List<String> chunks, List<float[]> vectors, int startChunkIndex, VectorBO vectorBO);
}
