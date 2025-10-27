package com.springleaf.knowseek.mapper.pgvector;

import com.springleaf.knowseek.model.bo.VectorRecordSearchBO;
import com.springleaf.knowseek.model.bo.VectorRecordWithDistanceBO;
import com.springleaf.knowseek.model.entity.VectorRecord;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface VectorRecordMapper {


    /**
     * 插入一条向量记录
     * @param record 向量记录实体
     * @return 影响行数（通常为1）
     */
    int insert(VectorRecord record);

    List<VectorRecord> findTopKByEmbedding(VectorRecordSearchBO record);

    List<VectorRecordWithDistanceBO> findTopKByEmbeddingWithDistance(VectorRecordSearchBO searchBO);

    /**
     * 根据文件 ID 删除向量数据
     */
    void deleteByFileId(@Param("id") Long id);
}
