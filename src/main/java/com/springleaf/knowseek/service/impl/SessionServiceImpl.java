package com.springleaf.knowseek.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.springleaf.knowseek.exception.BusinessException;
import com.springleaf.knowseek.mapper.mysql.SessionMapper;
import com.springleaf.knowseek.model.dto.SessionCreateDTO;
import com.springleaf.knowseek.model.dto.SessionUpdateDTO;
import com.springleaf.knowseek.model.entity.Session;
import com.springleaf.knowseek.model.vo.SessionVO;
import com.springleaf.knowseek.service.MessageService;
import com.springleaf.knowseek.service.SessionService;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 会话服务实现类
 */
@Service
public class SessionServiceImpl implements SessionService {

    @Resource
    private SessionMapper sessionMapper;

    @Resource
    private MessageService messageService;

    @Override
    public SessionVO createSession(SessionCreateDTO createDTO) {
        // 获取当前登录用户ID
        Long userId = StpUtil.getLoginIdAsLong();

        // 创建会话实体
        Session session = new Session();
        session.setUserId(userId);
        session.setSessionName(createDTO.getSessionName());
        session.setMetadata(createDTO.getMetadata());
        session.setCreatedAt(LocalDateTime.now());
        session.setUpdatedAt(LocalDateTime.now());
        session.setIsActive(true);

        // 插入数据库
        sessionMapper.insert(session);

        // 转换为VO返回
        return convertToVO(session);
    }

    @Override
    public SessionVO getSessionById(Long id) {
        // 获取当前登录用户ID
        Long userId = StpUtil.getLoginIdAsLong();

        // 查询会话
        Session session = sessionMapper.selectById(id);
        if (session == null) {
            throw new BusinessException("会话不存在");
        }

        // 验证会话所属用户
        if (!session.getUserId().equals(userId)) {
            throw new BusinessException("无权访问该会话");
        }

        // 转换为VO返回
        return convertToVO(session);
    }

    @Override
    public List<SessionVO> getCurrentUserSessions() {
        // 获取当前登录用户ID
        Long userId = StpUtil.getLoginIdAsLong();

        // 查询用户的所有会话
        List<Session> sessions = sessionMapper.selectByUserId(userId);

        // 转换为VO列表返回
        return sessions.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public boolean updateSession(SessionUpdateDTO updateDTO, Long userId) {

        // 查询会话
        Session session = sessionMapper.selectById(updateDTO.getId());
        if (session == null) {
            throw new BusinessException("会话不存在");
        }

        // 验证会话所属用户
        if (!session.getUserId().equals(userId)) {
            throw new BusinessException("无权修改该会话");
        }

        // 更新会话信息
        if (updateDTO.getSessionName() != null) {
            session.setSessionName(updateDTO.getSessionName());
        }
        if (updateDTO.getIsActive() != null) {
            session.setIsActive(updateDTO.getIsActive());
        }
        if (updateDTO.getMetadata() != null) {
            session.setMetadata(updateDTO.getMetadata());
        }
        session.setUpdatedAt(LocalDateTime.now());

        // 更新数据库
        return sessionMapper.updateById(session) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteSession(Long id) {
        // 获取当前登录用户ID
        Long userId = StpUtil.getLoginIdAsLong();

        // 查询会话
        Session session = sessionMapper.selectById(id);
        if (session == null) {
            throw new BusinessException("会话不存在");
        }

        // 验证会话所属用户
        if (!session.getUserId().equals(userId)) {
            throw new BusinessException("无权删除该会话");
        }

        // 删除会话的所有消息
        messageService.deleteMessagesBySessionId(id);

        // 删除会话
        return sessionMapper.deleteById(id) > 0;
    }

    /**
     * 将实体转换为VO
     * @param session 会话实体
     * @return 会话VO
     */
    private SessionVO convertToVO(Session session) {
        if (session == null) {
            return null;
        }
        SessionVO vo = new SessionVO();
        BeanUtils.copyProperties(session, vo);
        return vo;
    }
}