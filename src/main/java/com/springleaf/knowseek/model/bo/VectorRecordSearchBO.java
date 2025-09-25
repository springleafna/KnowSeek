package com.springleaf.knowseek.model.bo;

import lombok.Data;

@Data
public class VectorRecordSearchBO {

    private Long userId;

    private Long knowledgeBaseId;

    private int topK;

    private float[] queryVector;
}
