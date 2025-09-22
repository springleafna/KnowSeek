package com.springleaf.knowseek.mapper.mysql;

import com.springleaf.knowseek.model.entity.Session;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface SessionMapper {

    /**
     * 创建会话
     * @param session 会话实体
     * @return 影响行数
     */
    int insert(Session session);

    /**
     * 根据ID查询会话
     * @param id 会话ID
     * @return 会话实体
     */
    Session selectById(Long id);

    /**
     * 根据用户ID查询会话列表
     * @param userId 用户ID
     * @return 会话列表
     */
    List<Session> selectByUserId(Long userId);

    /**
     * 更新会话信息
     * @param session 会话实体
     * @return 影响行数
     */
    int updateById(Session session);

    /**
     * 更新会话活跃状态
     * @param id 会话ID
     * @param isActive 是否活跃
     * @return 影响行数
     */
    int updateActiveStatus(@Param("id") Long id, @Param("isActive") Boolean isActive);

    /**
     * 删除会话
     * @param id 会话ID
     * @return 影响行数
     */
    int deleteById(Long id);
}