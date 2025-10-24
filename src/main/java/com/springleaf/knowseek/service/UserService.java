package com.springleaf.knowseek.service;

import com.github.pagehelper.PageInfo;
import com.springleaf.knowseek.model.dto.UserLoginDTO;
import com.springleaf.knowseek.model.dto.UserPageDTO;
import com.springleaf.knowseek.model.dto.UserRegisterDTO;
import com.springleaf.knowseek.model.vo.UserInfoVO;
import com.springleaf.knowseek.model.vo.UserListVO;
import com.springleaf.knowseek.model.vo.UserLoginVO;
import jakarta.validation.constraints.NotNull;

public interface UserService {

    /**
     * 用户登录
     *
     * @param loginDTO 登录信息
     * @return 登录结果，包含token和用户信息
     */
    UserLoginVO login(UserLoginDTO loginDTO);

    /**
     * 用户注册
     */
    void register(UserRegisterDTO registerDTO);

    /**
     * 用户退出登录
     */
    void logout();

    /**
     * 获取用户信息
     */
    UserInfoVO getUserInfo();
    
    /**
     * Admin：分页查询用户列表
     * @param pageDTO 分页参数
     * @return 分页用户列表
     */
    PageInfo<UserListVO> listUsers(UserPageDTO pageDTO);

    /**
     * Admin：重置用户密码
     */
    void resetPassword(Long id);

    /**
     * Admin：删除用户
     */
    void deleteUser(Long id);
}
