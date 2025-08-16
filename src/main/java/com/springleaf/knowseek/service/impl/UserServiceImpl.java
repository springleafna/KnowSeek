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

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final OrganizationMapper organizationMapper;
    private final UserOrganizationMapper userOrganizationMapper;

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

        // TODO：需要判断当前登录用户是管理员还是普通用户，两种角色有不同权限

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
        Long newUserId = (long) userMapper.insert(user);

        // 用户注册成功后会自动获得一个该用户的默认组织 DEFAULT_ORG_%USERNAME
        String defaultOrgTag = String.format(DefaultOrgConstant.DEFAULT_ORG_TAG, username);
        Organization organization = new Organization();
        organization.setTag(defaultOrgTag);
        organization.setName(DefaultOrgConstant.DEFAULT_ORG_NAME);
        organization.setDescription(DefaultOrgConstant.DEFAULT_ORG_DESC);
        organization.setParentId(null);
        organization.setCreatedBy(DefaultOrgConstant.DEFAULT_ORG_CREATE);
        // 执行新增组织，返回组织ID
        Long newOrganizationId = (long) organizationMapper.insert(organization);

        // 添加到用户组织关联表中
        UserOrganization userOrganization = new UserOrganization();
        userOrganization.setUserId(newUserId);
        userOrganization.setOrganizationId(newOrganizationId);
        userOrganizationMapper.insert(userOrganization);
        log.info("新用户注册成功");
    }

    @Override
    public void logout() {

    }

    @Override
    public UserInfoVO getUserInfo() {
        return null;
    }
}
