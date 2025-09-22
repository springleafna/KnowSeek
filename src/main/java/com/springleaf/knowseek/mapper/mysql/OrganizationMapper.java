package com.springleaf.knowseek.mapper.mysql;

import com.springleaf.knowseek.model.entity.Organization;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface OrganizationMapper {

    /**
     * 插入组织
     * @param organization 组织实体
     * @return 影响行数
     */
    int insert(Organization organization);

    /**
     * 根据ID查询组织
     * @param id 组织ID
     * @return 组织实体
     */
    Organization selectById(Long id);

    /**
     * 根据标签名查询组织
     * @param tag 组织标签名
     * @return 组织实体
     */
    Organization selectByTag(String tag);

    /**
     * 更新组织信息
     * @param organization 组织实体
     * @return 影响行数
     */
    int updateById(Organization organization);

    /**
     * 删除组织（逻辑删除）
     * @param id 组织ID
     * @return 影响行数
     */
    int deleteById(Long id);

    /**
     * 查询所有组织
     * @return 组织列表
     */
    List<Organization> selectAll();

    /**
     * 根据父组织ID查询子组织
     * @param parentId 父组织ID
     * @return 子组织列表
     */
    List<Organization> selectByParentId(Long parentId);

    /**
     * 查询组织总数
     * @param params 查询参数
     * @return 组织总数
     */
    int count(Map<String, Object> params);

    /**
     * 根据组织标签列表获取组织ID列表
     * @param orgTagList 组织标签列表
     * @return 组织ID列表
     */
    List<Long> selectOrgIdtByTags(List<String> orgTagList);
    
    /**
     * 根据ID列表查询组织
     * @param ids 组织ID列表
     * @return 组织实体列表
     */
    List<Organization> selectBatchIds(@Param("list") List<Long> ids);

    /**
     * 查询所有未删除的组织列表
     * @return 组织列表
     */
    List<Organization> selectAllNotDeleted();

    /**
     * 根据条件查询未删除的组织列表
     * @param tag 组织标签（模糊搜索）
     * @param name 组织名称（模糊搜索）
     * @return 组织列表
     */
    List<Organization> selectByCondition(@Param("tag") String tag, @Param("name") String name);
}