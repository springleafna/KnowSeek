package com.springleaf.knowseek.model.dto;

import com.springleaf.knowseek.service.strategy.AIModelStrategy;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ModelSelectionResult {
    private boolean success;
    private AIModelStrategy strategy;
    private String reason;
    private String errorMessage;

    public static ModelSelectionResult success(AIModelStrategy strategy, String reason) {
        return new ModelSelectionResult(true, strategy, reason, null);
    }

    public static ModelSelectionResult failed(String errorMessage) {
        return new ModelSelectionResult(false, null, null, errorMessage);
    }
}