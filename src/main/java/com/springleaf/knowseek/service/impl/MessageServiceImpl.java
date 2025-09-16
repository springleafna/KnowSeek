package com.springleaf.knowseek.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.springleaf.knowseek.exception.BusinessException;
import com.springleaf.knowseek.mapper.MessageMapper;
import com.springleaf.knowseek.mapper.SessionMapper;
import com.springleaf.knowseek.model.dto.MessageCreateDTO;
import com.springleaf.knowseek.model.entity.Message;
import com.springleaf.knowseek.model.entity.Session;
import com.springleaf.knowseek.model.vo.MessageVO;
import com.springleaf.knowseek.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * AI消息服务实现类
 */
@RequiredArgsConstructor
@Service
public class MessageServiceImpl implements MessageService {

    private final MessageMapper messageMapper;

    private final SessionMapper sessionMapper;

    @Override
    public MessageVO createMessage(MessageCreateDTO createDTO, Long userId) {

        // 验证会话是否存在且属于当前用户
        Session session = sessionMapper.selectById(createDTO.getSessionId());
        if (session == null) {
            throw new BusinessException("会话不存在");
        }
        if (!session.getUserId().equals(userId)) {
            throw new BusinessException("无权访问该会话");
        }

        // 创建消息实体
        Message message = new Message();
        message.setSessionId(createDTO.getSessionId());
        message.setRole(createDTO.getRole());
        message.setContent(createDTO.getContent());
        message.setMetadata(createDTO.getMetadata());
        message.setCreatedAt(LocalDateTime.now());

        // 插入数据库
        messageMapper.insert(message);

        // 更新会话的最后活跃时间
        session.setUpdatedAt(LocalDateTime.now());
        sessionMapper.updateById(session);

        // 转换为VO返回
        return convertToVO(message);
    }

    @Override
    public MessageVO getMessageById(Long messageId) {
        // 获取当前登录用户ID
        Long userId = StpUtil.getLoginIdAsLong();

        // 查询消息
        Message message = messageMapper.selectById(messageId);
        if (message == null) {
            throw new BusinessException("消息不存在");
        }

        // 验证消息所属会话是否属于当前用户
        Session session = sessionMapper.selectById(message.getSessionId());
        if (session == null || !session.getUserId().equals(userId)) {
            throw new BusinessException("无权访问该消息");
        }

        // 转换为VO返回
        return convertToVO(message);
    }

    @Override
    public List<MessageVO> getMessagesBySessionId(Long sessionId) {
        // 获取当前登录用户ID
        Long userId = StpUtil.getLoginIdAsLong();

        // 验证会话是否存在且属于当前用户
        Session session = sessionMapper.selectById(sessionId);
        if (session == null) {
            throw new BusinessException("会话不存在");
        }
        if (!session.getUserId().equals(userId)) {
            throw new BusinessException("无权访问该会话");
        }

        // 查询会话的所有消息
        List<Message> messages = messageMapper.selectBySessionId(sessionId);

        // 转换为VO列表返回
        return messages.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public boolean deleteMessagesBySessionId(Long sessionId) {
        // 获取当前登录用户ID
        Long userId = StpUtil.getLoginIdAsLong();

        // 验证会话是否存在且属于当前用户
        Session session = sessionMapper.selectById(sessionId);
        if (session == null) {
            throw new BusinessException("会话不存在");
        }
        if (!session.getUserId().equals(userId)) {
            throw new BusinessException("无权操作该会话");
        }

        // 删除会话的所有消息
        return messageMapper.deleteBySessionId(sessionId) >= 0;
    }

    /**
     * 将实体转换为VO
     * @param message 消息实体
     * @return 消息VO
     */
    private MessageVO convertToVO(Message message) {
        if (message == null) {
            return null;
        }
        MessageVO vo = new MessageVO();
        BeanUtils.copyProperties(message, vo);
        return vo;
    }
}