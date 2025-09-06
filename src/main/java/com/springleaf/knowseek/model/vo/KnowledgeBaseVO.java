package com.springleaf.knowseek.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class KnowledgeBaseVO {

    private Long id;
    private String name;
    private Long userId;
    private LocalDateTime createdAt;

}
