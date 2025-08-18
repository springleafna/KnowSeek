package com.springleaf.knowseek.controller;

import com.github.pagehelper.PageInfo;
import com.springleaf.knowseek.common.Result;
import com.springleaf.knowseek.model.dto.OrganizationAddDTO;
import com.springleaf.knowseek.model.dto.OrganizationAddDescDTO;
import com.springleaf.knowseek.model.dto.OrganizationAssignDTO;
import com.springleaf.knowseek.model.dto.OrganizationPageDTO;
import com.springleaf.knowseek.model.dto.OrganizationUpdateDTO;
import com.springleaf.knowseek.model.vo.OrganizationListVO;
import com.springleaf.knowseek.model.vo.OrganizationVO;
import com.springleaf.knowseek.service.OrganizationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/org")
@RequiredArgsConstructor
@Validated
public class OrganizationController {

    private final OrganizationService organizationService;

    /**
     * 选择主组织标签
     */
    @PutMapping("/setPrimary")
    public Result<Void> choosePrimaryOrg(@NotBlank(message = "组织标签不能为空或空字符串") @RequestParam String orgTag) {
        organizationService.choosePrimaryOrg(orgTag);
        return Result.success();
    }

    /**
     * 获取用户组织列表
     */
    @GetMapping("/allOrg")
    public Result<List<OrganizationVO>> getUserAllOrg() {
        List<OrganizationVO> organizationVOList = organizationService.getUserAllOrg();
        return Result.success(organizationVOList);
    }

    /**
     * Admin：创建组织（可选择上级）
     */
    @PostMapping("/create")
    public Result<Void> createOrg(@RequestBody @Valid OrganizationAddDTO organizationAddDTO) {
        organizationService.createOrg(organizationAddDTO);
        return Result.success();
    }

    /**
     * Admin：为用户分配组织
     */
    @PostMapping("/assign")
    public Result<Void> assignOrgToUser(@RequestBody @Valid OrganizationAssignDTO organizationAssignDTO) {
        organizationService.assignOrgToUser(organizationAssignDTO);
        return Result.success();
    }

    /**
     * Admin：添加组织下级
     */
    @PostMapping("/addSub")
    public Result<Void> addSubOrg(@RequestBody @Valid OrganizationAddDescDTO organizationAddDescDTO) {
        organizationService.addSubOrg(organizationAddDescDTO);
        return Result.success();
    }

    /**
     * Admin：编辑组织
     */
    @PutMapping("/update")
    public Result<Void> updateOrg(@RequestBody @Valid OrganizationUpdateDTO organizationUpdateDTO) {
        organizationService.updateOrg(organizationUpdateDTO);
        return Result.success();
    }
    
    /**
     * Admin：查询所有未删除的组织列表（分页）
     */
    @GetMapping("/list")
    public Result<PageInfo<OrganizationListVO>> listAllOrganizations(@Valid OrganizationPageDTO pageDTO) {
        PageInfo<OrganizationListVO> pageInfo = organizationService.listAllOrganizations(pageDTO);
        return Result.success(pageInfo);
    }
}
