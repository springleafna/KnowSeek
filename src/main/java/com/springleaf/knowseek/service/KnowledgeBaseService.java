package com.springleaf.knowseek.service;

import com.springleaf.knowseek.model.dto.KnowledgeBaseCreateDTO;
import com.springleaf.knowseek.model.dto.KnowledgeBaseUpdateDTO;
import com.springleaf.knowseek.model.vo.FileItemVO;
import com.springleaf.knowseek.model.vo.KnowledgeBaseVO;

import java.util.List;

public interface KnowledgeBaseService {

    /**
     * 获取文件列表
     */
    List<FileItemVO> getFileList(Long id);

    /**
     * 为当前登录用户创建新的知识库
     * @param createDTO 包含知识库名称的DTO
     */
    void createKnowledgeBase(KnowledgeBaseCreateDTO createDTO);

    /**
     * 根据ID删除知识库（逻辑删除）
     * @param id 知识库ID
     */
    void deleteKnowledgeBase(Long id);

    /**
     * 查询当前登录用户的所有未删除的知识库
     * @return 知识库VO列表
     */
    List<KnowledgeBaseVO> listKnowledgeBasesByCurrentUser();

    /**
     * 更新知识库名称
     * @param updateDTO 包含新名称的DTO
     */
    void updateKnowledgeBaseName(KnowledgeBaseUpdateDTO updateDTO);

    /**
     * 根据ID获取知识库详情
     * @param id 知识库ID
     * @return 知识库VO，如果不存在则可能抛出异常或返回null
     */
    KnowledgeBaseVO getKnowledgeBaseById(Long id);

    /**
     * 选中某个知识库设为用户主知识库
     */
    void setPrimaryKnowledgeBase(Long id);
}
