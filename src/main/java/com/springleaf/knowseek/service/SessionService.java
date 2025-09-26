package com.springleaf.knowseek.service;

import com.springleaf.knowseek.model.dto.SessionCreateDTO;
import com.springleaf.knowseek.model.dto.SessionUpdateDTO;
import com.springleaf.knowseek.model.vo.SessionVO;

import java.util.List;

/**
 * 会话服务接口
 */
public interface SessionService {

    /**
     * 创建会话
     * @param createDTO 创建会话DTO
     * @return 会话VO
     */
    SessionVO createSession(SessionCreateDTO createDTO);

    /**
     * 获取会话详情
     * @param id 会话ID
     * @return 会话VO
     */
    SessionVO getSessionById(Long id);

    /**
     * 获取当前用户的会话列表
     * @return 会话VO列表
     */
    List<SessionVO> getCurrentUserSessions();

    /**
     * 更新会话
     * @param updateDTO 更新会话DTO
     * @return 是否成功
     */
    boolean updateSession(SessionUpdateDTO updateDTO, Long userId);

    /**
     * 删除会话
     * @param id 会话ID
     */
    void deleteSession(Long id);

    /**
     * 删除会话消息记录
     * @param id 会话id
     */
    void deleteMessages(Long id);
}