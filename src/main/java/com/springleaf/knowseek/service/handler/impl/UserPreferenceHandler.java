package com.springleaf.knowseek.service.handler.impl;

import com.springleaf.knowseek.model.dto.ModelSelectionRequest;
import com.springleaf.knowseek.model.dto.ModelSelectionResult;
import com.springleaf.knowseek.service.factory.ModelFactoryManager;
import com.springleaf.knowseek.service.handler.ModelSelectionHandler;
import com.springleaf.knowseek.service.strategy.AIModelStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserPreferenceHandler extends ModelSelectionHandler {

    private final ModelFactoryManager factoryManager;

    @Override
    public ModelSelectionResult handle(ModelSelectionRequest request) {
        // 如果用户指定了模型，直接使用
        if (StringUtils.isNotBlank(request.getSpecifiedModel())) {
            try {
                AIModelStrategy strategy = factoryManager.createModel(request.getSpecifiedModel());
                if (strategy.isAvailable()) {
                    log.info("使用用户指定模型: {}", request.getSpecifiedModel());
                    return ModelSelectionResult.success(strategy, "用户指定模型");
                }
            } catch (Exception e) {
                log.warn("用户指定模型不可用: {}", request.getSpecifiedModel(), e);
            }
        }

        // TODO: 这里可以从数据库获取用户偏好模型
        // String preferredModel = preferenceService.getUserPreferredModel(request.getUserId());
        // 暂时跳过用户偏好逻辑

        return passToNext(request);
    }
}