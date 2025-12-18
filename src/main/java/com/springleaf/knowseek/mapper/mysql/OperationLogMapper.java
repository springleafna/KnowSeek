package com.springleaf.knowseek.mapper.mysql;

import com.springleaf.knowseek.model.dto.OperationLogPageDTO;
import com.springleaf.knowseek.model.entity.OperationLog;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface OperationLogMapper {

    void insert(OperationLog record);

    /**
     * 条件分页查询操作日志
     */
    List<OperationLog> selectByCondition(OperationLogPageDTO pageDTO);
}
