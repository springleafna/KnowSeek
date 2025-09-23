package com.springleaf.knowseek.model.bo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VectorBO {

    private Long fileId;

    private Long organizationId;

    private Long userId;

    private Long knowledgeBaseId;
}
