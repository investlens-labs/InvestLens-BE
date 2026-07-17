package com.investlens.ingestion.infrastructure;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.news-search")
public record NewsSearchProperties(
        boolean enabled,
        String baseUrl,
        Duration timeout,
        Duration refreshInterval,
        int lookbackDays,
        int maxResults
) {
    public NewsSearchProperties {
        if (baseUrl == null || baseUrl.isBlank()) baseUrl = "https://news.google.com/rss/search";
        if (timeout == null) timeout = Duration.ofSeconds(10);
        if (refreshInterval == null) refreshInterval = Duration.ofMinutes(30);
        if (lookbackDays <= 0 || lookbackDays > 365) lookbackDays = 90;
        if (maxResults <= 0 || maxResults > 100) maxResults = 20;
    }
}
