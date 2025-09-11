package com.springleaf.knowseek.service;

import com.springleaf.knowseek.model.dto.AiChatRequestDTO;
import com.springleaf.knowseek.model.vo.AiChatResponseVO;

/**
 * AI对话服务接口
 */
public interface AiChatService {

    /**
     * 发送消息并获取AI回复
     * @param requestDTO 对话请求DTO
     * @return 对话响应VO
     */
    AiChatResponseVO chat(AiChatRequestDTO requestDTO);
}