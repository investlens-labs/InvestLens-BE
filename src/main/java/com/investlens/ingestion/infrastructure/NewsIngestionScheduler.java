package com.investlens.ingestion.infrastructure;

import com.investlens.ingestion.application.NewsIngestionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class NewsIngestionScheduler {
    private static final Logger log = LoggerFactory.getLogger(NewsIngestionScheduler.class);
    private final RssFeedProperties properties;
    private final NewsIngestionService service;

    public NewsIngestionScheduler(RssFeedProperties properties, NewsIngestionService service) {
        this.properties = properties;
        this.service = service;
    }

    @Scheduled(cron = "${app.news-ingestion.cron:0 0/30 * * * *}")
    public void collect() {
        if (!properties.enabled()) return;
        int retried = service.retryPendingAnalyses();
        int collected = service.collectAndAnalyze();
        log.info("News ingestion completed: {} retried, {} new articles", retried, collected);
    }
}
