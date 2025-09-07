package com.springleaf.knowseek.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class KnowledgeBaseUpdateDTO {

    @NotNull(message = "知识库ID不能为空！")
    private Long id;

    private String name;

    private String description;
}
