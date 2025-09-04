package com.springleaf.knowseek.service;

import com.springleaf.knowseek.model.vo.FileItemVO;

import java.util.List;

public interface KnowledgeBaseService {

    /**
     * 获取文件列表
     */
    List<FileItemVO> getFileList();
}
