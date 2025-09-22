package com.springleaf.knowseek.mapper.mysql;

import com.springleaf.knowseek.model.entity.UserOrganization;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface UserOrganizationMapper {

    /**
     * 插入用户组织关联
     * @param userOrganization 用户组织关联实体
     * @return 影响行数
     */
    int insert(UserOrganization userOrganization);

    /**
     * 根据用户ID和组织ID删除关联
     * @param userId 用户ID
     * @param organizationId 组织ID
     * @return 影响行数
     */
    int deleteByUserIdAndOrgId(Long userId, Long organizationId);

    /**
     * 根据用户ID删除该用户的所有组织关联
     * @param userId 用户ID
     * @return 影响行数
     */
    int deleteByUserId(Long userId);

    /**
     * 根据组织ID删除该组织的所有用户关联
     * @param organizationId 组织ID
     * @return 影响行数
     */
    int deleteByOrganizationId(Long organizationId);

    /**
     * 根据用户ID查询该用户所属的所有组织
     * @param userId 用户ID
     * @return 用户组织关联列表
     */
    List<UserOrganization> selectByUserId(Long userId);

    /**
     * 根据组织ID查询该组织下的所有用户
     * @param organizationId 组织ID
     * @return 用户组织关联列表
     */
    List<UserOrganization> selectByOrganizationId(Long organizationId);

    /**
     * 查询用户组织关联总数
     * @param params 查询参数
     * @return 关联总数
     */
    int count(Map<String, Object> params);

    /**
     * 检查用户是否属于某个组织
     * @param userId 用户ID
     * @param organizationId 组织ID
     * @return 是否存在关联
     */
    boolean exists(Long userId, Long organizationId);
}