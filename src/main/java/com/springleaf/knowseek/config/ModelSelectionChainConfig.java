package com.springleaf.knowseek.config;

import com.springleaf.knowseek.service.handler.ModelSelectionHandler;
import com.springleaf.knowseek.service.handler.impl.DefaultModelHandler;
import com.springleaf.knowseek.service.handler.impl.UserPreferenceHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 模型选择责任链配置 - 责任链模式配置
 * 配置模型选择的责任链处理顺序和Bean注册
 */
@Slf4j
@Configuration
public class ModelSelectionChainConfig {

    /**
     * 构建模型选择责任链
     * 处理顺序：用户偏好处理器 -> 默认模型处理器
     */
    @Bean
    public ModelSelectionHandler modelSelectionChain(
            UserPreferenceHandler userHandler,
            DefaultModelHandler defaultHandler) {

        // 构建责任链: 用户偏好 -> 默认模型
        userHandler.setNext(defaultHandler);

        log.info("模型选择责任链已初始化: UserPreference -> Default");
        return userHandler;
    }
}