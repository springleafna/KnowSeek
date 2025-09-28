package com.springleaf.knowseek.service.handler;

import com.springleaf.knowseek.model.dto.ModelSelectionRequest;
import com.springleaf.knowseek.model.dto.ModelSelectionResult;

/**
 * 模型选择处理器抽象类 - 责任链模式
 * 定义模型选择的责任链处理逻辑，支持多级模型选择策略
 * 处理顺序：用户偏好 -> 智能推荐 -> 默认模型 -> 兜底模型
 */
public abstract class ModelSelectionHandler {
    protected ModelSelectionHandler nextHandler;

    /**
     * 设置责任链中的下一个处理器
     */
    public void setNext(ModelSelectionHandler handler) {
        this.nextHandler = handler;
    }

    /**
     * 处理模型选择请求
     * @param request 模型选择请求
     * @return 模型选择结果
     */
    public abstract ModelSelectionResult handle(ModelSelectionRequest request);

    /**
     * 传递给下一个处理器
     */
    protected ModelSelectionResult passToNext(ModelSelectionRequest request) {
        if (nextHandler != null) {
            return nextHandler.handle(request);
        }
        return ModelSelectionResult.failed("无可用模型");
    }
}