package com.springleaf.knowseek.model.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 模型选择请求DTO - 责任链模式使用
 * 封装模型选择所需的所有上下文信息
 */
@Data
@Builder
public class ModelSelectionRequest {
    /** 用户ID */
    private Long userId;

    /** 用户指定的模型名称 */
    private String specifiedModel;

    /** 用户消息内容，用于智能推荐 */
    private String messageContent;

    /** 会话上下文，用于智能推荐 */
    private String sessionContext;

    /** 是否使用知识库，影响模型选择 */
    private Boolean useKnowledgeBase;
}