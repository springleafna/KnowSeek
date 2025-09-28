package com.springleaf.knowseek.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.springleaf.knowseek.common.Result;
import com.springleaf.knowseek.model.vo.ModelInfoVO;
import com.springleaf.knowseek.service.factory.ModelFactoryManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * AI模型管理控制器
 * 提供模型查询、检测、推荐等API接口
 * 支持多厂商AI模型的统一管理
 */
@Slf4j
@RestController
@RequestMapping("/model")
@SaCheckLogin
@RequiredArgsConstructor
public class ModelController {

    private final ModelFactoryManager factoryManager;

    /**
     * 获取所有可用的AI模型列表
     */
    @GetMapping("/available")
    public Result<List<ModelInfoVO>> getAvailableModels() {
        try {
            List<ModelInfoVO> models = factoryManager.getAllAvailableModels();
            log.info("获取可用模型列表，共{}个模型", models.size());
            return Result.success(models);
        } catch (Exception e) {
            log.error("获取可用模型列表失败", e);
            return Result.error("获取模型列表失败：" + e.getMessage());
        }
    }

    /**
     * 检查指定模型是否支持
     */
    @GetMapping("/check/{modelName}")
    public Result<Boolean> checkModelSupport(@PathVariable String modelName) {
        try {
            boolean supported = factoryManager.isModelSupported(modelName);
            log.info("检查模型{}支持状态: {}", modelName, supported);
            return Result.success(supported);
        } catch (Exception e) {
            log.error("检查模型支持状态失败: {}", modelName, e);
            return Result.error("检查模型失败：" + e.getMessage());
        }
    }

    /**
     * 获取推荐的默认模型
     * TODO: 后续可基于用户历史使用情况进行个性化推荐
     */
    @GetMapping("/recommendation")
    public Result<String> getRecommendedModel() {
        try {
            // 目前返回系统默认推荐模型
            String recommendedModel = "qwen-plus";
            log.info("推荐模型: {}", recommendedModel);
            return Result.success(recommendedModel);
        } catch (Exception e) {
            log.error("获取推荐模型失败", e);
            return Result.error("获取推荐模型失败：" + e.getMessage());
        }
    }

    /**
     * 设置用户偏好模型（预留接口）
     * TODO: 需要实现用户偏好存储服务
     */
    @PostMapping("/preference")
    public Result<Void> setModelPreference(@RequestParam String modelName) {
        try {
            // 检查模型是否支持
            if (!factoryManager.isModelSupported(modelName)) {
                return Result.error("不支持的模型: " + modelName);
            }

            // TODO: 实现用户偏好存储
            // preferenceService.setUserPreference(StpUtil.getLoginIdAsLong(), modelName);

            log.info("设置用户偏好模型: {}", modelName);
            return Result.success();
        } catch (Exception e) {
            log.error("设置用户偏好模型失败: {}", modelName, e);
            return Result.error("设置偏好失败：" + e.getMessage());
        }
    }

    /**
     * 获取用户偏好模型（预留接口）
     * TODO: 需要实现用户偏好存储服务
     */
    @GetMapping("/preference")
    public Result<String> getUserModelPreference() {
        try {
            // TODO: 从数据库获取用户偏好
            // String preference = preferenceService.getUserPreferredModel(StpUtil.getLoginIdAsLong());
            String preference = "qwen-plus"; // 临时返回默认值

            log.info("获取用户偏好模型: {}", preference);
            return Result.success(preference);
        } catch (Exception e) {
            log.error("获取用户偏好模型失败", e);
            return Result.error("获取偏好失败：" + e.getMessage());
        }
    }
}