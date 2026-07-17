package com.investlens.analysis.application;

import com.investlens.news.domain.NewsLanguage;
import com.investlens.news.domain.ImpactDirection;
import java.util.List;
import java.util.UUID;

public interface NewsLocalizationPort {
    List<Result> localize(List<Request> articles, NewsLanguage language);

    record Request(UUID newsId, String title, String content, String ticker, String companyName) {}

    record Result(
            UUID newsId,
            String translatedTitle,
            String summary,
            ImpactDirection direction,
            int score,
            String reason,
            String modelName,
            boolean aiAnalyzed
    ) {}
}
