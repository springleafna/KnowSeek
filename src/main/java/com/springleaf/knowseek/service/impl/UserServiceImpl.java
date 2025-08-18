package com.springleaf.knowseek.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.springleaf.knowseek.constans.DefaultOrgConstant;
import com.springleaf.knowseek.enums.UserRoleEnum;
import com.springleaf.knowseek.exception.BusinessException;
import com.springleaf.knowseek.mapper.OrganizationMapper;
import com.springleaf.knowseek.mapper.UserMapper;
import com.springleaf.knowseek.mapper.UserOrganizationMapper;
import com.springleaf.knowseek.model.dto.UserLoginDTO;
import com.springleaf.knowseek.model.dto.UserRegisterDTO;
import com.springleaf.knowseek.model.entity.Organization;
import com.springleaf.knowseek.model.entity.User;
import com.springleaf.knowseek.model.entity.UserOrganization;
import com.springleaf.knowseek.model.vo.UserInfoVO;
import com.springleaf.knowseek.model.vo.UserLoginVO;
import com.springleaf.knowseek.service.UserService;
import com.springleaf.knowseek.utils.PasswordUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final OrganizationMapper organizationMapper;
    private final UserOrganizationMapper userOrganizationMapper;

    @Override
    public UserLoginVO login(UserLoginDTO loginDTO) {
        String username = loginDTO.getUsername();
        User user = userMapper.selectByUsername(username);
        if (user == null) {
            throw new BusinessException("用户名或密码错误");
        }
        if (!PasswordUtil.verifyPassword(loginDTO.getPassword(), user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }

        // 登录
        StpUtil.login(user.getId());
        String token = StpUtil.getTokenValue();
        
        // 记录登录日志
        String role = user.getRole();
        if (UserRoleEnum.ADMIN.getValue().equals(role)) {
            log.info("{} 管理员登录成功", username);
        } else {
            log.info("{} 普通用户登录成功", username);
        }

        return new UserLoginVO(token);
    }

    @Transactional
    @Override
    public void register(UserRegisterDTO registerDTO) {
        log.info("新用户注册");
        String username = registerDTO.getUsername();
        User existUser = userMapper.selectByUsername(username);
        if (existUser != null) {
            throw new BusinessException("用户名已存在");
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(PasswordUtil.encryptPassword(registerDTO.getPassword()));
        user.setRole(UserRoleEnum.USER.getValue());
        // 执行新增用户返回用户ID
        userMapper.insert(user);
        Long newUserId = user.getId();

                // 用户注册成功后会自动获得一个该用户的默认组织 DEFAULT_ORG_%USERNAME
        String defaultOrgTag = String.format(DefaultOrgConstant.DEFAULT_ORG_TAG, username);
        Organization organization = new Organization();
        organization.setTag(defaultOrgTag);
        organization.setName(DefaultOrgConstant.DEFAULT_ORG_NAME);
        organization.setDescription(DefaultOrgConstant.DEFAULT_ORG_DESC);
        organization.setParentId(null);
        organization.setCreatedBy(DefaultOrgConstant.DEFAULT_ORG_CREATE);
        // 执行新增组织，返回组织ID
        organizationMapper.insert(organization);
        Long newOrganizationId = organization.getId();

                // 添加到用户组织关联表中
        UserOrganization userOrganization = new UserOrganization();
        userOrganization.setUserId(newUserId);
        userOrganization.setOrganizationId(newOrganizationId);
        userOrganizationMapper.insert(userOrganization);

        // 将创建的默认组织设置为用户的主组织
        userMapper.setPrimaryOrgId(newOrganizationId, newUserId);
        log.info("新用户注册成功");
    }

    @Override
    public void logout() {
        StpUtil.logout();
        log.info("用户 {} 退出登录", StpUtil.getLoginIdAsLong());
    }

    @Override
    public UserInfoVO getUserInfo() {
        long userId = StpUtil.getLoginIdAsLong();
        User user = userMapper.selectById(userId);
        UserInfoVO userInfoVO = new UserInfoVO();
        userInfoVO.setRole(user.getRole());
        userInfoVO.setUsername(user.getUsername());
        Long primaryOrgId = user.getPrimaryOrgId();
        if (primaryOrgId != null) {
            userInfoVO.setPrimaryOrgName(organizationMapper.selectById(primaryOrgId).getName());
        }else {
            userInfoVO.setPrimaryOrgName(null);
        }
        log.info("获取用户信息 {}", userInfoVO);
        return userInfoVO;
    }
}
