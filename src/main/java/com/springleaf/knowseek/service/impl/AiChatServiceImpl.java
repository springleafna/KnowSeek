package com.springleaf.knowseek.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.springleaf.knowseek.exception.BusinessException;
import com.springleaf.knowseek.model.dto.AiChatRequestDTO;
import com.springleaf.knowseek.model.dto.AiMessageCreateDTO;
import com.springleaf.knowseek.model.dto.SessionCreateDTO;
import com.springleaf.knowseek.model.vo.AiChatResponseVO;
import com.springleaf.knowseek.model.vo.AiMessageVO;
import com.springleaf.knowseek.model.vo.SessionVO;
import com.springleaf.knowseek.service.AiChatService;
import com.springleaf.knowseek.service.AiMessageService;
import com.springleaf.knowseek.service.SessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


/**
 * AI对话服务实现类
 */
@RequiredArgsConstructor
@Service
public class AiChatServiceImpl implements AiChatService {

    private final SessionService sessionService;

    private final AiMessageService aiMessageService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiChatResponseVO chat(AiChatRequestDTO requestDTO) {
        // 获取当前登录用户ID
        Long userId = StpUtil.getLoginIdAsLong();
        
        // 会话ID，如果为空则创建新会话
        Long sessionId = requestDTO.getSessionId();
        SessionVO sessionVO;
        
        if (sessionId == null) {
            // 创建新会话
            SessionCreateDTO createDTO = new SessionCreateDTO();
            createDTO.setSessionName("新对话"); // 默认会话名称
            createDTO.setMetadata(requestDTO.getMetadata());
            sessionVO = sessionService.createSession(createDTO);
            sessionId = sessionVO.getId();
        } else {
            // 获取已有会话
            sessionVO = sessionService.getSessionById(sessionId);
            if (sessionVO == null) {
                throw new BusinessException("会话不存在");
            }
        }
        
        // 保存用户消息
        AiMessageCreateDTO userMessageDTO = new AiMessageCreateDTO();
        userMessageDTO.setSessionId(sessionId);
        userMessageDTO.setRole("user");
        userMessageDTO.setContent(requestDTO.getContent());
        userMessageDTO.setMetadata(requestDTO.getMetadata());
        aiMessageService.createMessage(userMessageDTO);
        
        // TODO: 调用AI服务获取回复，这里简单模拟
        String aiReply = "这是AI的回复: " + requestDTO.getContent();
        String aiMetadata = "{\"model\":\"gpt-3.5-turbo\",\"tokens\":150,\"time\":0.8}";
        
        // 保存AI回复消息
        AiMessageCreateDTO aiMessageDTO = new AiMessageCreateDTO();
        aiMessageDTO.setSessionId(sessionId);
        aiMessageDTO.setRole("assistant");
        aiMessageDTO.setContent(aiReply);
        aiMessageDTO.setMetadata(aiMetadata);
        AiMessageVO aiMessageVO = aiMessageService.createMessage(aiMessageDTO);
        
        // 构建响应
        AiChatResponseVO responseVO = new AiChatResponseVO();
        responseVO.setSessionId(sessionId);
        responseVO.setSessionName(sessionVO.getSessionName());
        responseVO.setContent(aiReply);
        responseVO.setMessageId(aiMessageVO.getMessageId());
        responseVO.setMetadata(aiMetadata);
        
        return responseVO;
    }
}