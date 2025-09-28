package com.springleaf.knowseek.service.factory;

import com.springleaf.knowseek.exception.UnsupportedProviderException;
import com.springleaf.knowseek.model.vo.ModelInfoVO;
import com.springleaf.knowseek.service.strategy.AIModelStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 模型工厂管理器 - 工厂模式管理类
 * 统一管理所有AI模型工厂，负责路由到具体的工厂实现
 * 支持多厂商模型的统一创建和管理
 */
@Slf4j
@Service
public class ModelFactoryManager {

    private final Map<String, AIModelFactory> factories;

    public ModelFactoryManager(List<AIModelFactory> factoryList) {
        // 将所有工厂实现按提供商名称进行映射
        this.factories = factoryList.stream()
                .collect(Collectors.toMap(
                        AIModelFactory::getProviderName,
                        Function.identity()
                ));
        log.info("已注册模型工厂: {}", factories.keySet());
    }

    /**
     * 创建指定的AI模型实例
     * @param modelName 模型名称
     * @return AI模型策略实例
     */
    public AIModelStrategy createModel(String modelName) {
        String provider = extractProvider(modelName);
        AIModelFactory factory = factories.get(provider);
        if (factory == null) {
            throw new UnsupportedProviderException("不支持的服务商: " + provider);
        }
        return factory.createModel(modelName);
    }

    public List<ModelInfoVO> getAllAvailableModels() {
        return factories.values().stream()
                .flatMap(factory -> factory.getSupportedModels().stream()
                        .map(modelName -> {
                            try {
                                AIModelStrategy strategy = factory.createModel(modelName);
                                return convertToModelInfoVO(strategy);
                            } catch (Exception e) {
                                log.warn("获取模型信息失败: {}", modelName, e);
                                return null;
                            }
                        })
                        .filter(model -> model != null))
                .collect(Collectors.toList());
    }

    public boolean isModelSupported(String modelName) {
        try {
            createModel(modelName);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private String extractProvider(String modelName) {
        if (modelName.startsWith("qwen-")) {
            return "alibaba";
        } else if (modelName.startsWith("gpt-")) {
            return "openai";
        } else if (modelName.startsWith("ernie-")) {
            return "baidu";
        }
        return "unknown";
    }

    private ModelInfoVO convertToModelInfoVO(AIModelStrategy strategy) {
        var capability = strategy.getCapability();
        return ModelInfoVO.builder()
                .modelName(capability.getModelName())
                .displayName(capability.getDisplayName())
                .provider(capability.getProvider())
                .maxTokens(capability.getMaxTokens())
                .supportStream(capability.isSupportStream())
                .supportFunction(capability.isSupportFunction())
                .costPerToken(capability.getCostPerToken())
                .isAvailable(strategy.isAvailable())
                .description(String.format("%s - 最大令牌: %d, 支持流式: %s",
                        capability.getDisplayName(),
                        capability.getMaxTokens(),
                        capability.isSupportStream() ? "是" : "否"))
                .build();
    }
}