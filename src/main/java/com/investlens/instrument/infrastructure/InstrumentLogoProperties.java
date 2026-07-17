package com.investlens.instrument.infrastructure;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.instrument-logo")
public record InstrumentLogoProperties(
        boolean enabled,
        String baseUrl,
        String publishableKey,
        int size
) {
    public InstrumentLogoProperties {
        if (baseUrl == null || baseUrl.isBlank()) baseUrl = "https://img.logo.dev";
        if (publishableKey == null) publishableKey = "";
        if (size <= 0 || size > 800) size = 64;
    }
}
