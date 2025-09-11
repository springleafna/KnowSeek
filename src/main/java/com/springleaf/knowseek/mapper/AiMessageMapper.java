package com.springleaf.knowseek.mapper;

import com.springleaf.knowseek.model.entity.AiMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AiMessageMapper {

    /**
     * 插入消息
     * @param aiMessage 消息实体
     * @return 影响行数
     */
    int insert(AiMessage aiMessage);

    /**
     * 根据ID查询消息
     * @param messageId 消息ID
     * @return 消息实体
     */
    AiMessage selectById(Long messageId);

    /**
     * 根据会话ID查询消息列表
     * @param sessionId 会话ID
     * @return 消息列表
     */
    List<AiMessage> selectBySessionId(Long sessionId);

    /**
     * 根据会话ID和角色查询消息列表
     * @param sessionId 会话ID
     * @param role 角色
     * @return 消息列表
     */
    List<AiMessage> selectBySessionIdAndRole(@Param("sessionId") Long sessionId, @Param("role") String role);

    /**
     * 删除会话的所有消息
     * @param sessionId 会话ID
     * @return 影响行数
     */
    int deleteBySessionId(Long sessionId);
}