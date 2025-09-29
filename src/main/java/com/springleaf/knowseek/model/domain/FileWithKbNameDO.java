package com.springleaf.knowseek.model.domain;

import com.springleaf.knowseek.model.entity.FileUpload;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 包含知识库名称的文件信息DO，用于MyBatis联合查询
 */
@Data
@EqualsAndHashCode(callSuper = true) // 如果继承了实体类
public class FileWithKbNameDO extends FileUpload {

    /**
     * 知识库名称
     */
    private String knowledgeBaseName;
}
