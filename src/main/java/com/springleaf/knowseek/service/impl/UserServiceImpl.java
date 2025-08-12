package com.springleaf.knowseek.service.impl;

import com.springleaf.knowseek.model.dto.UserLoginDTO;
import com.springleaf.knowseek.model.dto.UserRegisterDTO;
import com.springleaf.knowseek.model.vo.UserInfoVO;
import com.springleaf.knowseek.model.vo.UserLoginVO;
import com.springleaf.knowseek.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {
    @Override
    public UserLoginVO login(UserLoginDTO loginDTO) {
        return null;
    }

    @Override
    public void register(UserRegisterDTO registerDTO) {

    }

    @Override
    public void logout() {

    }

    @Override
    public UserInfoVO getUserInfo() {
        return null;
    }
}
