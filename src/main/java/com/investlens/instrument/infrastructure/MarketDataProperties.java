package com.investlens.instrument.infrastructure;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.market-data")
public record MarketDataProperties(boolean enabled, String baseUrl, Duration timeout) {
    public MarketDataProperties {
        if (baseUrl == null || baseUrl.isBlank()) baseUrl = "https://query1.finance.yahoo.com";
        if (timeout == null) timeout = Duration.ofSeconds(10);
    }
}
