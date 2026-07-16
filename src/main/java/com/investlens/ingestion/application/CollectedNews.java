package com.investlens.ingestion.application;

import java.time.Instant;

public record CollectedNews(String source, String url, String title, String content, Instant publishedAt) {
}
