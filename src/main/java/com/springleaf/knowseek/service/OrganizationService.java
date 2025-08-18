package com.springleaf.knowseek.service;

import com.springleaf.knowseek.model.dto.OrganizationAddDTO;
import com.springleaf.knowseek.model.dto.OrganizationAssignDTO;
import com.springleaf.knowseek.model.vo.OrganizationVO;
import jakarta.validation.Valid;

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
}
