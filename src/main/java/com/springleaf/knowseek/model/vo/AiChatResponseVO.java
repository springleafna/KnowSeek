package com.springleaf.knowseek.model.vo;

import lombok.Data;

/**
 * AI对话响应VO
 */
@Data
public class AiChatResponseVO {

    /**
     * 会话ID
     */
    private Long sessionId;

    /**
     * 会话名称
     */
    private String sessionName;

    /**
     * 助手回复内容
     */
    private String content;

    /**
     * 消息ID
     */
    private Long messageId;

    /**
     * 元数据，如模型信息、耗时等
     */
    private String metadata;
}