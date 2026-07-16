package com.investlens.ingestion.infrastructure;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.news-ingestion")
public record RssFeedProperties(boolean enabled, String cron, Duration timeout, List<Feed> feeds) {
    public RssFeedProperties {
        if (cron == null || cron.isBlank()) cron = "0 0/30 * * * *";
        if (timeout == null) timeout = Duration.ofSeconds(10);
        feeds = feeds == null ? List.of() : List.copyOf(feeds);
    }
    public record Feed(String name, String url) {}
}
