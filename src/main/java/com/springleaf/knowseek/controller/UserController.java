package com.springleaf.knowseek.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.github.pagehelper.PageInfo;
import com.springleaf.knowseek.common.Result;
import com.springleaf.knowseek.log.OperationLogRecord;
import com.springleaf.knowseek.log.OperationType;
import com.springleaf.knowseek.model.dto.UserLoginDTO;
import com.springleaf.knowseek.model.dto.UserPageDTO;
import com.springleaf.knowseek.model.dto.UserRegisterDTO;
import com.springleaf.knowseek.model.dto.UserUpdatePasswordDTO;
import com.springleaf.knowseek.model.vo.UserInfoVO;
import com.springleaf.knowseek.model.vo.UserListVO;
import com.springleaf.knowseek.model.vo.UserLoginVO;
import com.springleaf.knowseek.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
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
    @OperationLogRecord(moduleName = "用户管理", operationType = OperationType.LOGIN, description = "用户登录")
    public Result<UserLoginVO> login(@RequestBody @Valid UserLoginDTO loginDTO) {
        UserLoginVO userLoginVO = userService.login(loginDTO);
        return Result.success(userLoginVO);
    }

    /**
     * 用户名 密码 注册
     */
    @PostMapping("/register")
    @OperationLogRecord(moduleName = "用户管理", operationType = OperationType.INSERT, description = "用户注册")
    public Result<Void> register(@RequestBody @Valid UserRegisterDTO registerDTO) {
        userService.register(registerDTO);
        return Result.success();
    }

    /**
     * 用户修改密码
     */
    @PutMapping("/updatePassword")
    @OperationLogRecord(moduleName = "用户管理", operationType = OperationType.UPDATE, description = "修改密码")
    public Result<Void> updatePassword(@RequestBody @Valid UserUpdatePasswordDTO updatePasswordDTO) {
        userService.updatePassword(updatePasswordDTO);
        return Result.success();
    }

    /**
     * 退出登录
     */
    @PostMapping("/logout")
    @OperationLogRecord(moduleName = "用户管理", operationType = OperationType.LOGOUT, description = "用户登出")
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

    /**
     * Admin：重置用户密码
     */
    @SaCheckRole("admin")
    @PutMapping("/resetPassword")
    @OperationLogRecord(moduleName = "用户管理", operationType = OperationType.UPDATE, description = "管理员重置用户密码")
    public Result<Void> resetPassword(@NotNull(message = "用户Id不能为空") @RequestParam Long id) {
        userService.resetPassword(id);
        return Result.success();
    }

    /**
     * Admin：删除用户
     */
    @SaCheckRole("admin")
    @DeleteMapping("/delete")
    @OperationLogRecord(moduleName = "用户管理", operationType = OperationType.DELETE, description = "管理员删除用户")
    public Result<Void> deleteUser(@NotNull(message = "用户Id不能为空") @RequestParam Long id) {
        userService.deleteUser(id);
        return Result.success();
    }
}
