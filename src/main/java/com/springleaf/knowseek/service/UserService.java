package com.springleaf.knowseek.service;

import com.springleaf.knowseek.model.dto.UserLoginDTO;
import com.springleaf.knowseek.model.dto.UserRegisterDTO;
import com.springleaf.knowseek.model.vo.UserInfoVO;
import com.springleaf.knowseek.model.vo.UserLoginVO;

public interface UserService {

    /**
     * 用户登录
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
}
