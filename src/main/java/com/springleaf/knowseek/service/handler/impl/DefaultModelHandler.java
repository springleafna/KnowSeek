package com.springleaf.knowseek.service.handler.impl;

import com.springleaf.knowseek.enums.ModelEnum;
import com.springleaf.knowseek.model.dto.ModelSelectionRequest;
import com.springleaf.knowseek.model.dto.ModelSelectionResult;
import com.springleaf.knowseek.model.vo.ModelInfoVO;
import com.springleaf.knowseek.service.factory.ModelFactoryManager;
import com.springleaf.knowseek.service.handler.ModelSelectionHandler;
import com.springleaf.knowseek.service.strategy.AIModelStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultModelHandler extends ModelSelectionHandler {

    private final ModelFactoryManager factoryManager;

    @Value("${ai.default-model:qwen-plus}")
    private String defaultModel;

    @Override
    public ModelSelectionResult handle(ModelSelectionRequest request) {
        try {
            AIModelStrategy strategy = factoryManager.createModel(defaultModel);
            if (strategy.isAvailable()) {
                log.info("使用系统默认模型: {}", defaultModel);
                return ModelSelectionResult.success(strategy, "系统默认模型");
            }
        } catch (Exception e) {
            log.error("默认模型不可用: {}", defaultModel, e);
        }

        // 最后的兜底：使用任意可用模型
        return findAnyAvailableModel();
    }

    private ModelSelectionResult findAnyAvailableModel() {
        List<ModelInfoVO> availableModels = factoryManager.getAllAvailableModels();
        for (ModelInfoVO modelInfo : availableModels) {
            try {
                AIModelStrategy strategy = factoryManager.createModel(modelInfo.getModelName());
                if (strategy.isAvailable()) {
                    log.info("使用兜底可用模型: {}", modelInfo.getModelName());
                    return ModelSelectionResult.success(strategy, "兜底可用模型");
                }
            } catch (Exception e) {
                continue;
            }
        }

        // 如果所有模型都不可用，使用枚举默认模型强制返回
        try {
            String fallbackModel = ModelEnum.getDefaultModel().getModelName();
            AIModelStrategy strategy = factoryManager.createModel(fallbackModel);
            log.warn("所有模型检查失败，强制使用默认模型: {}", fallbackModel);
            return ModelSelectionResult.success(strategy, "强制默认模型");
        } catch (Exception e) {
            log.error("强制默认模型也失败", e);
            return ModelSelectionResult.failed("无任何可用模型");
        }
    }
}