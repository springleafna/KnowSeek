package com.springleaf.knowseek.service;

import com.springleaf.knowseek.model.dto.MessageCreateDTO;
import com.springleaf.knowseek.model.vo.MessageVO;

import java.util.List;

/**
 * AI消息服务接口
 */
public interface MessageService {

    /**
     * 创建消息
     * @param createDTO 创建消息DTO
     * @return 消息VO
     */
    MessageVO createMessage(MessageCreateDTO createDTO, Long userId);

    /**
     * 获取消息详情
     * @param messageId 消息ID
     * @return 消息VO
     */
    MessageVO getMessageById(Long messageId);

    /**
     * 获取会话的消息列表
     * @param sessionId 会话ID
     * @return 消息VO列表
     */
    List<MessageVO> getMessagesBySessionId(Long sessionId);

    /**
     * 删除会话的所有消息
     * @param sessionId 会话ID
     * @return 是否成功
     */
    boolean deleteMessagesBySessionId(Long sessionId);
}