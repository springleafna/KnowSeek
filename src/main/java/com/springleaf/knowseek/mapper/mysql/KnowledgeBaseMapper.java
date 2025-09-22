package com.springleaf.knowseek.mapper.mysql;

import com.springleaf.knowseek.model.entity.KnowledgeBase;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 知识库 Mapper 接口
 */
public interface KnowledgeBaseMapper {

    /**
     * 新增知识库
     *
     * @return 返回影响的行数
     */
    int insertKnowledgeBase(KnowledgeBase knowledgeBase);

    /**
     * 根据ID逻辑删除知识库
     *
     * @param id 知识库ID
     * @return 返回影响的行数
     */
    int deleteKnowledgeBaseById(@Param("id") Long id);

    /**
     * 根据用户ID获取其所有未删除的知识库列表
     *
     * @param userId 用户ID
     * @return 返回知识库列表
     */
    List<KnowledgeBase> selectAllKnowledgeBase(@Param("userId") Long userId);

    /**
     * 根据ID更新知识库名称（仅当记录未被删除时）
     *
     * @param id   知识库ID
     * @param name 新的知识库名称
     * @return 返回影响的行数
     */
    int updateKnowledgeBaseNameById(@Param("id") Long id, @Param("name") String name, @Param("description") String description);

    /**
     * 根据ID获取未删除的知识库信息
     *
     * @param id 知识库ID
     * @return 返回知识库实体，如果不存在或已删除则返回null
     */
    KnowledgeBase selectKnowledgeBaseById(@Param("id") Long id);

    /**
     * 根据ID获取未删除的知识库名称
     * @param id 知识库ID
     * @return 返回知识库名称
     */
    String getNameById(Long id);
}