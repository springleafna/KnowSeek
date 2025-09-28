package com.springleaf.knowseek.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelCapability {
    private String modelName;
    private String provider;
    private String displayName;
    private int maxTokens;
    private boolean supportStream;
    private boolean supportFunction;
    private List<String> supportedLanguages;
    private BigDecimal costPerToken;
    private boolean isAvailable;
}