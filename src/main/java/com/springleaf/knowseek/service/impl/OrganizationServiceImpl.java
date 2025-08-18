package com.springleaf.knowseek.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.springleaf.knowseek.exception.BusinessException;
import com.springleaf.knowseek.mapper.OrganizationMapper;
import com.springleaf.knowseek.mapper.UserMapper;
import com.springleaf.knowseek.mapper.UserOrganizationMapper;
import com.springleaf.knowseek.model.dto.OrganizationAddDTO;
import com.springleaf.knowseek.model.dto.OrganizationAddSubDTO;
import com.springleaf.knowseek.model.dto.OrganizationAssignDTO;
import com.springleaf.knowseek.model.dto.OrganizationPageDTO;
import com.springleaf.knowseek.model.dto.OrganizationUpdateDTO;
import com.springleaf.knowseek.model.entity.Organization;
import com.springleaf.knowseek.model.entity.User;
import com.springleaf.knowseek.model.entity.UserOrganization;
import com.springleaf.knowseek.model.vo.OrganizationListVO;
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
    
    @Override
    public void addSubOrg(OrganizationAddSubDTO organizationAddSubDTO) {
        long userId = StpUtil.getLoginIdAsLong();
        String orgTag = organizationAddSubDTO.getOrgTag();
        String subOrgTag = organizationAddSubDTO.getSubOrgTag();
        
        // 检查当前组织是否存在
        Organization parentOrg = organizationMapper.selectByTag(orgTag);
        if (parentOrg == null) {
            throw new BusinessException("当前组织不存在");
        }
        
        // 检查下级组织标签是否已存在
        if (organizationMapper.selectByTag(subOrgTag) != null) {
            throw new BusinessException("下级组织标签已存在");
        }
        
        log.info("用户 {} 为组织 {} 添加下级组织 {}", userId, orgTag, subOrgTag);
        
        // 创建下级组织
        Organization subOrg = new Organization();
        subOrg.setTag(subOrgTag);
        subOrg.setName(subOrgTag); // 默认使用标签作为名称，可以后续编辑
        subOrg.setParentId(parentOrg.getId());
        subOrg.setCreatedBy(userId);
        
        organizationMapper.insert(subOrg);
    }
    
    @Override
    public void updateOrg(OrganizationUpdateDTO organizationUpdateDTO) {
        long userId = StpUtil.getLoginIdAsLong();
        String orgTag = organizationUpdateDTO.getOrgTag();
        String orgName = organizationUpdateDTO.getOrgName();
        String parentOrgTag = organizationUpdateDTO.getParentOrgTag();
        String description = organizationUpdateDTO.getDescription();
        
        // 检查要编辑的组织是否存在
        Organization organization = organizationMapper.selectByTag(orgTag);
        if (organization == null) {
            throw new BusinessException("该组织不存在");
        }
        
        // 如果提供了上级组织标签，则检查上级组织是否存在
        Long parentOrgId = null;
        if (parentOrgTag != null && !parentOrgTag.isEmpty()) {
            Organization parentOrg = organizationMapper.selectByTag(parentOrgTag);
            if (parentOrg == null) {
                throw new BusinessException("上级组织不存在");
            }
            parentOrgId = parentOrg.getId();
            
            // 检查是否形成循环引用
            if (organization.getId().equals(parentOrgId)) {
                throw new BusinessException("不能将组织自身设为上级组织");
            }
        }
        
        log.info("用户 {} 编辑组织 {}", userId, orgTag);
        
        // 更新组织信息
        if (orgName != null && !orgName.isEmpty()) {
            organization.setName(orgName);
        }
        if (parentOrgId != null) {
            organization.setParentId(parentOrgId);
        }
        if (description != null) {
            organization.setDescription(description);
        }
        
        organizationMapper.updateById(organization);
    }
    
    @Override
    public PageInfo<OrganizationListVO> listAllOrganizations(OrganizationPageDTO pageDTO) {
        log.info("查询所有未删除的组织列表，页码：{}，每页数量：{}", pageDTO.getPageNum(), pageDTO.getPageSize());
        
        // 使用PageHelper进行分页查询
        PageHelper.startPage(pageDTO.getPageNum(), pageDTO.getPageSize());
        List<Organization> organizationList = organizationMapper.selectAllNotDeleted();
        
        // 将Organization对象转换为OrganizationListVO对象
        List<OrganizationListVO> voList = new ArrayList<>();
        for (Organization org : organizationList) {
            OrganizationListVO vo = new OrganizationListVO();
            vo.setId(org.getId());
            vo.setTag(org.getTag());
            vo.setName(org.getName());
            
            // 获取父组织名称
            if (org.getParentId() != null) {
                Organization parentOrg = organizationMapper.selectById(org.getParentId());
                if (parentOrg != null) {
                    vo.setParentName(parentOrg.getName());
                }
            }
            
            // 获取创建者用户名
            if (org.getCreatedBy() != null) {
                User user = userMapper.selectById(org.getCreatedBy());
                if (user != null) {
                    vo.setCreatorUsername(user.getUsername());
                }
            }
            
            voList.add(vo);
        }
        
        // 创建分页信息对象
        PageInfo<OrganizationListVO> pageInfo = new PageInfo<>(voList);
        // 手动设置总记录数，因为我们转换了列表
        pageInfo.setTotal(new PageInfo<>(organizationList).getTotal());
        
        return pageInfo;
    }
}
