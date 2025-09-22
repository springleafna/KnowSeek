package com.springleaf.knowseek.model.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 向量记录实体类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VectorRecord {

    private Long id;

    private Long userId;

    private Long knowledgeBaseId;

    private Long organizationId;

    private String embedding;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private Integer deleted;
}