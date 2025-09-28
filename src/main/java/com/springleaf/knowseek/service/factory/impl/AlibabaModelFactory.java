package com.springleaf.knowseek.service.factory.impl;

import com.springleaf.knowseek.exception.UnsupportedModelException;
import com.springleaf.knowseek.service.factory.AIModelFactory;
import com.springleaf.knowseek.service.strategy.AIModelStrategy;
import com.springleaf.knowseek.service.strategy.impl.QwenPlusStrategy;
import com.springleaf.knowseek.service.strategy.impl.QwenTurboStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
public class AlibabaModelFactory implements AIModelFactory {

    private final ApplicationContext applicationContext;

    @Override
    public AIModelStrategy createModel(String modelName) {
        switch (modelName.toLowerCase()) {
            case "qwen-plus":
                return applicationContext.getBean(QwenPlusStrategy.class);
            case "qwen-turbo":
                return applicationContext.getBean(QwenTurboStrategy.class);
            default:
                throw new UnsupportedModelException("阿里云不支持的模型: " + modelName);
        }
    }

    @Override
    public List<String> getSupportedModels() {
        return Arrays.asList("qwen-plus", "qwen-turbo");
    }

    @Override
    public String getProviderName() {
        return "alibaba";
    }
}