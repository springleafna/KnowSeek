package com.springleaf.knowseek.service;

import com.github.pagehelper.PageInfo;
import com.springleaf.knowseek.model.dto.OrganizationAddDTO;
import com.springleaf.knowseek.model.dto.OrganizationAddSubDTO;
import com.springleaf.knowseek.model.dto.OrganizationAssignDTO;
import com.springleaf.knowseek.model.dto.OrganizationPageDTO;
import com.springleaf.knowseek.model.dto.OrganizationUpdateDTO;
import com.springleaf.knowseek.model.vo.OrganizationListVO;
import com.springleaf.knowseek.model.vo.OrganizationTreeVO;
import com.springleaf.knowseek.model.vo.OrganizationVO;

import java.util.List;

public interface OrganizationService {

    /**
     * 选择主组织标签
     */
    void choosePrimaryOrg(String orgTag);

    /**
     * 获取用户组织列表
     */
    List<OrganizationVO> getUserAllOrg();

    /**
     * Admin：创建组织
     */
    void createOrg(OrganizationAddDTO organizationAddDTO);

    /**
     * Admin：为用户分配组织
     */
    void assignOrgToUser(OrganizationAssignDTO organizationAssignDTO);
    
    /**
     * Admin：添加组织下级
     */
    void addSubOrg(OrganizationAddSubDTO organizationAddSubDTO);
    
    /**
     * Admin：编辑组织
     */
    void updateOrg(OrganizationUpdateDTO organizationUpdateDTO);
    
    /**
     * Admin：查询所有未删除的组织列表（分页）
     * @param pageDTO 分页参数
     * @return 分页组织列表
     */
    PageInfo<OrganizationListVO> listAllOrganizations(OrganizationPageDTO pageDTO);
    
    /**
     * Admin：获取组织树形结构
     * @return 组织树形结构列表
     */
    List<OrganizationTreeVO> getOrganizationTree();
}
