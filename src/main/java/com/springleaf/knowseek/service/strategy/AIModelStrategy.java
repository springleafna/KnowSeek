package com.springleaf.knowseek.service.strategy;

import com.springleaf.knowseek.model.entity.ModelCapability;
import com.springleaf.knowseek.model.entity.ModelConfig;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatResponse;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * AI模型策略接口 - 策略模式
 * 定义所有AI模型的统一行为，支持运行时动态切换不同的模型实现
 */
public interface AIModelStrategy {

    /**
     * 同步调用AI模型
     */
    ChatResponse chat(List<Message> messages, ModelConfig config);

    /**
     * 流式调用AI模型
     */
    Flux<ChatResponse> streamChat(List<Message> messages, ModelConfig config);

    /**
     * 获取模型名称
     */
    String getModelName();

    /**
     * 获取服务提供商
     */
    String getProvider();

    /**
     * 检查模型是否可用
     */
    boolean isAvailable();

    /**
     * 获取模型能力信息
     */
    ModelCapability getCapability();

    /**
     * 判断是否支持指定模型名称
     */
    boolean supports(String modelName);
}