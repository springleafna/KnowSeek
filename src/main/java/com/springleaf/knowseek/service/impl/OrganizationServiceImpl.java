package com.springleaf.knowseek.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.springleaf.knowseek.exception.BusinessException;
import com.springleaf.knowseek.mapper.OrganizationMapper;
import com.springleaf.knowseek.mapper.UserMapper;
import com.springleaf.knowseek.mapper.UserOrganizationMapper;
import com.springleaf.knowseek.model.entity.Organization;
import com.springleaf.knowseek.model.entity.UserOrganization;
import com.springleaf.knowseek.model.vo.OrganizationVO;
import com.springleaf.knowseek.service.OrganizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrganizationServiceImpl implements OrganizationService {

    private final OrganizationMapper organizationMapper;

    private final UserOrganizationMapper userOrganizationMapper;

    private final UserMapper userMapper;


    @Override
    public void choosePrimaryOrg(String orgTag) {
        long userId = StpUtil.getLoginIdAsLong();
        log.info("用户 {} 选择主组织，组织标签 {}", userId, orgTag);
        Organization organization = organizationMapper.selectByTag(orgTag);
        if (organization == null) {
            throw new BusinessException("该组织不存在");
        }
        userMapper.setPrimaryOrgId(organization.getId(), userId);

    }

    @Override
    public List<OrganizationVO> getUserAllOrg() {
        long userId = StpUtil.getLoginIdAsLong();
        List<UserOrganization> userOrganizations = userOrganizationMapper.selectByUserId(userId);
        if (userOrganizations.isEmpty()) {
            return Collections.emptyList();
        }

        // TODO:待优化成多表联查
        List<Long> orgIdList = userOrganizations.stream()
                .map(UserOrganization::getOrganizationId)
                .toList();

        List<OrganizationVO> organizationVOList = new ArrayList<>();
        for (Long orgId : orgIdList) {
            Organization organization = organizationMapper.selectById(orgId);
            OrganizationVO organizationVO = new OrganizationVO();
            BeanUtils.copyProperties(organization, organizationVO);
            organizationVOList.add(organizationVO);
        }
        return organizationVOList;
    }
}
