package com.springleaf.knowseek.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * AI模型枚举 - 定义系统支持的所有AI模型
 * 包含模型的基本信息：名称、提供商、成本、能力等
 */
@Getter
@AllArgsConstructor
public enum ModelEnum {
    QWEN_PLUS("qwen-plus", "通义千问-Plus", "alibaba", new BigDecimal("0.002"), 2000, true),
    QWEN_TURBO("qwen-turbo", "通义千问-Turbo", "alibaba", new BigDecimal("0.001"), 1500, true),
    GPT_3_5_TURBO("gpt-3.5-turbo", "GPT-3.5 Turbo", "openai", new BigDecimal("0.003"), 4000, true),
    GPT_4("gpt-4", "GPT-4", "openai", new BigDecimal("0.006"), 8000, true),
    ERNIE_BOT("ernie-bot-turbo", "文心一言", "baidu", new BigDecimal("0.002"), 2000, true);

    /** 模型名称 */
    private final String modelName;
    /** 显示名称 */
    private final String displayName;
    /** 服务提供商 */
    private final String provider;
    /** 每令牌成本 */
    private final BigDecimal costPerToken;
    /** 最大令牌数 */
    private final int maxTokens;
    /** 是否支持流式输出 */
    private final boolean supportStream;

    /**
     * 根据模型名称查找枚举
     */
    public static ModelEnum fromModelName(String modelName) {
        for (ModelEnum model : values()) {
            if (model.getModelName().equals(modelName)) {
                return model;
            }
        }
        throw new IllegalArgumentException("不支持的模型: " + modelName);
    }

    /**
     * 获取系统默认模型
     */
    public static ModelEnum getDefaultModel() {
        return QWEN_PLUS;
    }
}