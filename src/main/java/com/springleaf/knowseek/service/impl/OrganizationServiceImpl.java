package com.springleaf.knowseek.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.springleaf.knowseek.exception.BusinessException;
import com.springleaf.knowseek.mapper.OrganizationMapper;
import com.springleaf.knowseek.mapper.UserMapper;
import com.springleaf.knowseek.mapper.UserOrganizationMapper;
import com.springleaf.knowseek.model.dto.OrganizationAddDTO;
import com.springleaf.knowseek.model.dto.OrganizationAssignDTO;
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

    @Override
    public void createOrg(OrganizationAddDTO organizationAddDTO) {
        long userId = StpUtil.getLoginIdAsLong();
        String orgTag = organizationAddDTO.getOrgTag();
        String orgName = organizationAddDTO.getOrgName();
        String parentOrgTag = organizationAddDTO.getParentOrgTag();
        String description = organizationAddDTO.getDescription();

        Long parentOrgId = null;
        if (parentOrgTag != null) {
            parentOrgId = organizationMapper.selectByTag(parentOrgTag).getId();
        }

        log.info("用户 {} 创建组织，组织标签 {}， 组织名 {}， 组织上级 {}", userId, orgTag, orgName, parentOrgTag != null ? parentOrgTag : "无");

        if (organizationMapper.selectByTag(orgTag) != null) {
            throw new BusinessException("组织标签已存在");
        }
        Organization organization = new Organization();
        organization.setTag(orgTag);
        organization.setName(orgName);
        organization.setParentId(parentOrgId);
        organization.setDescription(description);
        organization.setCreatedBy(userId);

        organizationMapper.insert(organization);
    }

    @Override
    public void assignOrgToUser(OrganizationAssignDTO organizationAssignDTO) {
        Long userId = organizationAssignDTO.getUserId();
        List<String> orgTagList = organizationAssignDTO.getOrgTagList();
        log.info("为用户 {} 分配组织 {}", userId, orgTagList.toArray());
        List<Long> orgIdList = organizationMapper.selectOrgIdtByTags(orgTagList);
        for (Long orgId : orgIdList) {
            userOrganizationMapper.insert(new UserOrganization(userId, orgId));
        }
    }
}
