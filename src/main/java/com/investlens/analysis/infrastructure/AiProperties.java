package com.investlens.analysis.infrastructure;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.ai")
public record AiProperties(boolean enabled, String baseUrl, String model, Duration timeout) {
    public AiProperties {
        if (baseUrl == null || baseUrl.isBlank()) baseUrl = "http://localhost:11434";
        if (model == null || model.isBlank()) model = "gemma3:4b";
        if (timeout == null) timeout = Duration.ofSeconds(60);
    }
}
