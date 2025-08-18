package com.springleaf.knowseek.config;

import cn.dev33.satoken.stp.StpInterface;
import cn.dev33.satoken.stp.StpUtil;
import com.springleaf.knowseek.enums.UserRoleEnum;
import com.springleaf.knowseek.mapper.UserMapper;
import com.springleaf.knowseek.model.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 自定义权限验证接口扩展
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StpInterfaceImpl implements StpInterface {

    private final UserMapper userMapper;

    /**
     * 返回一个账号所拥有的权限码集合
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        // 本项目暂未使用权限码
        return new ArrayList<>();
    }

    /**
     * 返回一个账号所拥有的角色标识集合
     */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        // 获取用户ID
        Long userId = Long.parseLong(loginId.toString());
        
        // 获取用户信息
        User user = userMapper.selectById(userId);
        if (user == null) {
            return Collections.emptyList();
        }
        
        // 根据用户角色返回对应的角色列表
        String role = user.getRole();
        if (UserRoleEnum.ADMIN.getValue().equals(role)) {
            // 管理员拥有admin和user两个角色
            log.debug("用户 {} 拥有admin和user角色", userId);
            return List.of("admin", "user");
        } else {
            // 普通用户只有user角色
            log.debug("用户 {} 拥有user角色", userId);
            return List.of("user");
        }
    }
}