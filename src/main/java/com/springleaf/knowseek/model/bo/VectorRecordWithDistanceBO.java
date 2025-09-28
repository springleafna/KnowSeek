package com.springleaf.knowseek.model.bo;

import lombok.Data;

@Data
public class VectorRecordWithDistanceBO {

    private Long id;
    private Long userId;
    private Long knowledgeBaseId;
    private Long fileId;
    private float[] embedding;
    private String chunkText;
    private Integer chunkIndex;
    private Double distance; // pgvector 返回的距离（越小越相似）
}
