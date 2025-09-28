package com.springleaf.knowseek.model.entity;

import com.springleaf.knowseek.model.dto.ChatRequestDTO;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class ModelConfig {
    private String modelName;
    private String provider;
    private Double temperature;
    private Integer maxTokens;
    private Double topP;
    private Boolean enableSearch;
    private List<String> stopWords;
    private Map<String, Object> customParams;

    public static class ModelConfigBuilder {
        public ModelConfigBuilder fromChatRequest(ChatRequestDTO request) {
            this.modelName = request.getModelName();
            return this;
        }

        public ModelConfigBuilder withDefaults(String defaultModel) {
            if (this.modelName == null || this.modelName.trim().isEmpty()) {
                this.modelName = defaultModel;
            }
            if (this.temperature == null) {
                this.temperature = 0.7;
            }
            if (this.maxTokens == null) {
                this.maxTokens = 2000;
            }
            if (this.topP == null) {
                this.topP = 0.8;
            }
            if (this.enableSearch == null) {
                this.enableSearch = false;
            }
            return this;
        }
    }
}