package com.springleaf.knowseek.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * AI模型信息展示VO
 * 提供给前端的模型详细信息，用于模型选择界面
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelInfoVO {

    /** 模型名称 */
    private String modelName;

    /** 模型显示名称 */
    private String displayName;

    /** 服务提供商 */
    private String provider;

    /** 最大令牌数 */
    private int maxTokens;

    /** 是否支持流式输出 */
    private boolean supportStream;

    /** 是否支持函数调用 */
    private boolean supportFunction;

    /** 每令牌成本 */
    private BigDecimal costPerToken;

    /** 当前是否可用 */
    private boolean isAvailable;

    /** 模型描述信息 */
    private String description;
}