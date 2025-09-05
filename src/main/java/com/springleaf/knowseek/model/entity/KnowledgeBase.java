package com.springleaf.knowseek.model.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 知识库实体类
 */
@Data
public class KnowledgeBase {

    /**
     * 知识库ID
     */
    private Long id;

    /**
     * 知识库名称
     */
    private String name;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 删除标志（0正常 1删除）
     */
    private Boolean deleted;
}
