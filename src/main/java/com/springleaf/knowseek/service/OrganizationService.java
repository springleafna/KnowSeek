package com.springleaf.knowseek.service;

import com.springleaf.knowseek.model.vo.OrganizationVO;

import java.util.List;

public interface OrganizationService {

    /**
     * 选择主组织标签
     */
    void choosePrimaryOrg(String orgTag);

    /**
     * 获取用户组织列表
     * @return
     */
    List<OrganizationVO> getUserAllOrg();
}
