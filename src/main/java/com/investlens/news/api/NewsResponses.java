package com.investlens.news.api;

import com.investlens.news.domain.AnalysisStatus;
import com.investlens.news.domain.ImpactDirection;
import com.investlens.news.domain.NewsArticle;
import com.investlens.news.domain.NewsImpact;
import com.investlens.news.application.NewsLocalizationService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.Set;

public final class NewsResponses {
    private NewsResponses() {}

    public record Sentiment(
            boolean aiAnalyzed,
            int analyzedArticleCount,
            int relatedArticleCount,
            int upPercentage,
            int downPercentage,
            int neutralPercentage,
            String analysisModel,
            String disclaimer
    ) {
        public static Sentiment unavailable(int relatedArticleCount) {
            return new Sentiment(false, 0, relatedArticleCount, 0, 0, 0, null,
                    "AI 분석 결과가 없어 상승·하락 퍼센트를 계산하지 못했습니다.");
        }
    }

    public record Impact(
            UUID instrumentId,
            String ticker,
            String companyName,
            String instrumentType,
            ImpactDirection direction,
            int score,
            String reason,
            boolean aiAnalyzed,
            String analysisModel,
            Integer upProbability,
            Integer downProbability,
            Integer neutralProbability
    ) {
        static Impact from(NewsImpact impact) {
            var instrument = impact.getInstrument();
            return new Impact(instrument.getId(), instrument.getTicker(), instrument.getCompanyName(),
                    instrument.getType().name(), impact.getDirection(), impact.getScore(), impact.getReason(),
                    impact.isAiAnalyzed(), impact.getAnalysisModel(), impact.getUpProbability(),
                    impact.getDownProbability(), impact.getNeutralProbability());
        }

        static Impact localized(NewsImpact impact, NewsLocalizationService.LocalizedView localization) {
            var instrument = impact.getInstrument();
            return new Impact(instrument.getId(), instrument.getTicker(), instrument.getCompanyName(),
                    instrument.getType().name(), localization.direction(), localization.score(),
                    localization.reason(), localization.localized(), localization.modelName(), localization.upProbability(),
                    localization.downProbability(), localization.neutralProbability());
        }
    }

    public record FeedItem(
            UUID id,
            String source,
            String originalUrl,
            String title,
            String translatedTitle,
            String summary,
            String language,
            boolean localized,
            String localizationModel,
            String marketContext,
            AnalysisStatus analysisStatus,
            Instant publishedAt,
            List<Impact> impacts
    ) {
        public static FeedItem from(NewsArticle article, Set<UUID> allowedInstrumentIds,
                                    ImpactDirection direction, Integer minScore) {
            return new FeedItem(article.getId(), article.getSource(), article.getCanonicalUrl(),
                    article.getTitle(), article.getTranslatedTitle(),
                    article.getSummary(), null, article.getTranslatedTitle() != null, article.getModelName(),
                    article.getMarketContext(), article.getAnalysisStatus(), article.getPublishedAt(),
                    article.getImpacts().stream()
                            .filter(impact -> allowedInstrumentIds.contains(impact.getInstrument().getId()))
                            .filter(impact -> direction == null || impact.getDirection() == direction)
                            .filter(impact -> minScore == null || impact.getScore() >= minScore)
                            .map(Impact::from).toList());
        }

        public static FeedItem localized(NewsArticle article,
                                         Set<UUID> allowedInstrumentIds,
                                         NewsLocalizationService.LocalizedView localization) {
            return new FeedItem(article.getId(), article.getSource(), article.getCanonicalUrl(),
                    article.getTitle(), localization.translatedTitle(), localization.summary(),
                    localization.language(), localization.localized(), localization.modelName(),
                    article.getMarketContext(), article.getAnalysisStatus(), article.getPublishedAt(),
                    article.getImpacts().stream()
                            .filter(impact -> allowedInstrumentIds.contains(impact.getInstrument().getId()))
                            .map(impact -> Impact.localized(impact, localization))
                            .toList());
        }
    }

    public record Detail(
            UUID id,
            String source,
            String originalUrl,
            String originalTitle,
            String originalContent,
            String translatedTitle,
            String translatedContent,
            String summary,
            String marketContext,
            AnalysisStatus analysisStatus,
            String modelName,
            Instant publishedAt,
            List<Impact> impacts,
            String disclaimer
    ) {
        public static Detail from(NewsArticle article, Set<UUID> allowedInstrumentIds) {
            return new Detail(article.getId(), article.getSource(), article.getCanonicalUrl(), article.getTitle(),
                    article.getOriginalContent(), article.getTranslatedTitle(), article.getTranslatedContent(),
                    article.getSummary(), article.getMarketContext(), article.getAnalysisStatus(), article.getModelName(),
                    article.getPublishedAt(), article.getImpacts().stream()
                            .filter(impact -> allowedInstrumentIds.contains(impact.getInstrument().getId()))
                            .map(Impact::from).toList(),
                    "이 분석은 투자 조언이나 주가 예측이 아닌 뉴스 기반 영향 가능성 정보입니다.");
        }
    }
}
