package com.springleaf.knowseek.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 聊天请求DTO - 支持模型切换功能
 * 扩展原有聊天请求，新增模型选择参数
 */
@Data
public class ChatRequestDTO {

    @NotBlank(message = "消息内容不能为空")
    private String message;

    private Long sessionId;

    private Boolean useKnowledgeBase;

    /**
     * 指定使用的AI模型名称（可选）
     * 如果不指定，系统将根据责任链模式自动选择合适的模型
     * 支持的模型：qwen-plus, qwen-turbo 等
     */
    private String modelName;
}