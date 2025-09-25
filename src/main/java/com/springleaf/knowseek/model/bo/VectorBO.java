package com.springleaf.knowseek.model.bo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class VectorBO {

    private Long fileId;

    private Long organizationId;

    private Long userId;

    private Long knowledgeBaseId;
}
