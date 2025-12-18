package com.springleaf.knowseek.log;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.springleaf.knowseek.mapper.mysql.OperationLogMapper;
import com.springleaf.knowseek.model.dto.OperationLogPageDTO;
import com.springleaf.knowseek.model.entity.OperationLog;
import com.springleaf.knowseek.model.vo.OperationLogVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 操作日志服务
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class OperationLogService {

    private final OperationLogMapper operationLogMapper;

    /**
     * 异步保存日志到数据库
     */
    @Async
    public void saveAsync(OperationLog operationLog) {
        try {
            operationLogMapper.insert(operationLog);
        } catch (Exception e) {
            log.error("保存操作日志失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 分页查询操作日志
     */
    public PageInfo<OperationLogVO> listOperationLogs(OperationLogPageDTO pageDTO) {
        PageHelper.startPage(pageDTO.getPageNum(), pageDTO.getPageSize());

        List<OperationLog> logList = operationLogMapper.selectByCondition(pageDTO);
        PageInfo<OperationLog> pageInfo = new PageInfo<>(logList);

        List<OperationLogVO> voList = logList.stream().map(this::convertToVO).toList();

        PageInfo<OperationLogVO> resultPageInfo = new PageInfo<>();
        BeanUtils.copyProperties(pageInfo, resultPageInfo, "list");
        resultPageInfo.setList(voList);

        return resultPageInfo;
    }

    /**
     * 实体转 VO
     */
    private OperationLogVO convertToVO(OperationLog operationLog) {
        OperationLogVO vo = new OperationLogVO();
        BeanUtils.copyProperties(operationLog, vo);
        return vo;
    }
}
