package com.investlens.analysis.infrastructure;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.gemini")
public record GeminiProperties(
        boolean enabled,
        String baseUrl,
        String apiKey,
        String model,
        Duration timeout
) {
    public GeminiProperties {
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "https://generativelanguage.googleapis.com";
        }
        if (model == null || model.isBlank()) {
            model = "gemini-3.5-flash";
        }
        if (timeout == null) {
            timeout = Duration.ofSeconds(60);
        }
        if (enabled && (apiKey == null || apiKey.isBlank())) {
            throw new IllegalArgumentException("GEMINI_API_KEY is required when Gemini is enabled");
        }
    }
}
