package com.springleaf.knowseek.service.strategy.impl;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.springleaf.knowseek.enums.ModelEnum;
import com.springleaf.knowseek.model.entity.ModelCapability;
import com.springleaf.knowseek.model.entity.ModelConfig;
import com.springleaf.knowseek.service.strategy.AIModelStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class QwenTurboStrategy implements AIModelStrategy {

    private final DashScopeChatModel chatModel;

    @Override
    public ChatResponse chat(List<Message> messages, ModelConfig config) {
        try {
            Prompt prompt = new Prompt(messages);
            return chatModel.call(prompt);
        } catch (Exception e) {
            log.error("QwenTurbo模型调用失败", e);
            throw new RuntimeException("QwenTurbo模型调用失败: " + e.getMessage(), e);
        }
    }

    @Override
    public Flux<ChatResponse> streamChat(List<Message> messages, ModelConfig config) {
        try {
            Prompt prompt = new Prompt(messages);
            return chatModel.stream(prompt);
        } catch (Exception e) {
            log.error("QwenTurbo流式调用失败", e);
            return Flux.error(new RuntimeException("QwenTurbo流式调用失败: " + e.getMessage(), e));
        }
    }

    @Override
    public String getModelName() {
        return ModelEnum.QWEN_TURBO.getModelName();
    }

    @Override
    public String getProvider() {
        return ModelEnum.QWEN_TURBO.getProvider();
    }

    @Override
    public boolean isAvailable() {
        try {
            return true;
        } catch (Exception e) {
            log.warn("QwenTurbo模型不可用", e);
            return false;
        }
    }

    @Override
    public ModelCapability getCapability() {
        ModelEnum model = ModelEnum.QWEN_TURBO;
        return ModelCapability.builder()
                .modelName(model.getModelName())
                .provider(model.getProvider())
                .displayName(model.getDisplayName())
                .maxTokens(model.getMaxTokens())
                .supportStream(model.isSupportStream())
                .supportFunction(false)
                .supportedLanguages(Arrays.asList("中文", "英文"))
                .costPerToken(model.getCostPerToken())
                .isAvailable(isAvailable())
                .build();
    }

    @Override
    public boolean supports(String modelName) {
        return ModelEnum.QWEN_TURBO.getModelName().equals(modelName);
    }
}