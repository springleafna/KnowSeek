package com.springleaf.knowseek.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.github.pagehelper.PageInfo;
import com.springleaf.knowseek.common.Result;
import com.springleaf.knowseek.model.dto.UserLoginDTO;
import com.springleaf.knowseek.model.dto.UserPageDTO;
import com.springleaf.knowseek.model.dto.UserRegisterDTO;
import com.springleaf.knowseek.model.entity.User;
import com.springleaf.knowseek.model.vo.UserInfoVO;
import com.springleaf.knowseek.model.vo.UserListVO;
import com.springleaf.knowseek.model.vo.UserLoginVO;
import com.springleaf.knowseek.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 用户名 密码 登录
     */
    @PostMapping("/login")
    public Result<UserLoginVO> login(@RequestBody @Valid UserLoginDTO loginDTO) {
        UserLoginVO userLoginVO = userService.login(loginDTO);
        return Result.success(userLoginVO);
    }

    /**
     * 用户名 密码 注册
     */
    @PostMapping("/register")
    public Result<Void> register(@RequestBody @Valid UserRegisterDTO registerDTO) {
        userService.register(registerDTO);
        return Result.success();
    }

    /**
     * 退出登录
     */
    @PostMapping("/logout")
    public Result<Void> logout() {
        userService.logout();
        return Result.success();
    }

    /**
     * 获取用户信息
     */
    @GetMapping("/info")
    public Result<UserInfoVO> getUserInfo() {
        UserInfoVO userInfoVO = userService.getUserInfo();
        return Result.success(userInfoVO);
    }
    
    /**
     * Admin：分页查询用户列表
     */
    @SaCheckRole("admin")
    @GetMapping("/list")
    public Result<PageInfo<UserListVO>> listUsers(@Valid UserPageDTO pageDTO) {
        PageInfo<UserListVO> pageInfo = userService.listUsers(pageDTO);
        return Result.success(pageInfo);
    }
}
