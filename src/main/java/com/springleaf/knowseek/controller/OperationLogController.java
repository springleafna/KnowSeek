package com.springleaf.knowseek.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.github.pagehelper.PageInfo;
import com.springleaf.knowseek.common.Result;
import com.springleaf.knowseek.log.OperationLogRecord;
import com.springleaf.knowseek.log.OperationLogService;
import com.springleaf.knowseek.log.OperationType;
import com.springleaf.knowseek.model.dto.OperationLogPageDTO;
import com.springleaf.knowseek.model.vo.OperationLogVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/operationLog")
@RequiredArgsConstructor
public class OperationLogController {

    private final OperationLogService operationLogService;

    /**
     * Admin：分页查询操作日志
     */
    @SaCheckRole("admin")
    @GetMapping("/list")
    @OperationLogRecord(moduleName = "日志管理", operationType = OperationType.QUERY, description = "查询操作日志")
    public Result<PageInfo<OperationLogVO>> listOperationLogs(OperationLogPageDTO pageDTO) {
        PageInfo<OperationLogVO> pageInfo = operationLogService.listOperationLogs(pageDTO);
        return Result.success(pageInfo);
    }
}
