package com.springleaf.knowseek.mapper;

import com.springleaf.knowseek.model.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserMapper {

    /**
     * 插入用户
     * @param user 用户实体
     * @return 影响行数
     */
    int insert(User user);

    /**
     * 根据ID查询用户
     * @param id 用户ID
     * @return 用户实体
     */
    User selectById(Long id);

    /**
     * 根据用户名查询用户
     * @param username 用户名
     * @return 用户实体
     */
    User selectByUsername(String username);

    /**
     * 更新用户信息
     * @param user 用户实体
     * @return 影响行数
     */
    int updateById(User user);

    /**
     * 删除用户
     * @param id 用户ID
     * @return 影响行数
     */
    int deleteById(Long id);

    /**
     * 查询所有用户
     * @return 用户列表
     */
    List<User> selectAll();
    
    /**
     * 根据用户名模糊查询用户
     * @param username 用户名
     * @return 用户列表
     */
    List<User> selectByUsernameContaining(String username);

    /**
     * 查询用户总数
     * @return 用户总数
     */
    int count();

    /**
     * 设置主组织ID
     */
    void setPrimaryOrgId(@Param("orgId") Long orgId, @Param("userId") Long userId);
}
