package com.springleaf.knowseek.controller;

import com.springleaf.knowseek.common.Result;
import com.springleaf.knowseek.model.vo.OrganizationVO;
import com.springleaf.knowseek.service.OrganizationService;
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
    @GetMapping("/setPrimary")
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
}
