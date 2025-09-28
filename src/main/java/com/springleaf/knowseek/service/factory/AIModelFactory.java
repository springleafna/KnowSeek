package com.springleaf.knowseek.service.factory;

import com.springleaf.knowseek.service.strategy.AIModelStrategy;

import java.util.List;

/**
 * AI模型工厂接口 - 抽象工厂模式
 * 定义创建AI模型策略的统一接口，支持不同厂商的模型创建
 */
public interface AIModelFactory {

    /**
     * 创建指定名称的AI模型策略
     * @param modelName 模型名称
     * @return AI模型策略实例
     */
    AIModelStrategy createModel(String modelName);

    /**
     * 获取该工厂支持的所有模型名称
     * @return 支持的模型名称列表
     */
    List<String> getSupportedModels();

    /**
     * 获取服务提供商名称
     * @return 提供商名称
     */
    String getProviderName();
}