package com.springleaf.knowseek.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.springleaf.knowseek.common.Result;
import com.springleaf.knowseek.enums.ResultCodeEnum;
import com.springleaf.knowseek.exception.BusinessException;
import com.springleaf.knowseek.mapper.UserMapper;
import com.springleaf.knowseek.model.dto.UserLoginDTO;
import com.springleaf.knowseek.model.dto.UserRegisterDTO;
import com.springleaf.knowseek.model.entity.User;
import com.springleaf.knowseek.model.vo.UserInfoVO;
import com.springleaf.knowseek.model.vo.UserLoginVO;
import com.springleaf.knowseek.service.UserService;
import com.springleaf.knowseek.utils.PasswordUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;

    @Override
    public UserLoginVO login(UserLoginDTO loginDTO) {
        User user = userMapper.selectByUsername(loginDTO.getUsername());
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (!PasswordUtil.verifyPassword(loginDTO.getPassword(), user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }

        StpUtil.login(user.getId());
        String token = StpUtil.getTokenValue();

        return new UserLoginVO(token);
    }

    @Override
    public void register(UserRegisterDTO registerDTO) {
        String username = registerDTO.getUsername();
        User existUser = userMapper.selectByUsername(username);
        if (existUser != null) {
            throw new BusinessException("用户名已存在");
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(PasswordUtil.encryptPassword(registerDTO.getPassword()));

        int result = userMapper.insert(user);
        if (result < 1) {
            throw new BusinessException("用户注册失败");
        }

    }

    @Override
    public void logout() {

    }

    @Override
    public UserInfoVO getUserInfo() {
        return null;
    }
}
