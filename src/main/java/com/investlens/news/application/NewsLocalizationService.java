package com.investlens.news.application;

import com.investlens.analysis.application.NewsLocalizationPort;
import com.investlens.news.domain.NewsArticle;
import com.investlens.news.domain.NewsLanguage;
import com.investlens.instrument.domain.Instrument;
import com.investlens.news.infrastructure.NewsTranslationRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class NewsLocalizationService {
    private static final Logger log = LoggerFactory.getLogger(NewsLocalizationService.class);

    private final NewsTranslationRepository repository;
    private final NewsLocalizationPort localizationPort;
    private final NewsTranslationPersistenceService persistenceService;

    public NewsLocalizationService(NewsTranslationRepository repository,
                                   NewsLocalizationPort localizationPort,
                                   NewsTranslationPersistenceService persistenceService) {
        this.repository = repository;
        this.localizationPort = localizationPort;
        this.persistenceService = persistenceService;
    }

    public Map<UUID, LocalizedView> localize(List<NewsArticle> articles, NewsLanguage language,
                                             Instrument instrument) {
        if (articles.isEmpty()) {
            return Map.of();
        }
        List<UUID> newsIds = articles.stream().map(NewsArticle::getId).toList();
        Map<UUID, LocalizedView> localized = repository
                .findAllByNewsIdInAndLanguage(newsIds, language.code())
                .stream()
                .collect(Collectors.toMap(
                        translation -> translation.getNewsId(),
                        translation -> new LocalizedView(
                                language.code(),
                                translation.getTranslatedTitle(),
                                translation.getSummary(),
                                translation.getImpactDirection(),
                                translation.getImpactScore(),
                                translation.getImpactReason(),
                                translation.getModelName(),
                                true
                        ),
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
        List<NewsArticle> missing = articles.stream()
                .filter(article -> !localized.containsKey(article.getId()))
                .toList();
        if (missing.isEmpty()) {
            return Map.copyOf(localized);
        }

        List<NewsLocalizationPort.Request> requests = missing.stream()
                .map(article -> new NewsLocalizationPort.Request(
                        article.getId(), article.getTitle(), article.getOriginalContent(),
                        instrument.getTicker(), instrument.getCompanyName()))
                .toList();
        try {
            List<NewsLocalizationPort.Result> generated = localizationPort.localize(requests, language);
            boolean aiGenerated = generated.stream()
                    .allMatch(NewsLocalizationPort.Result::aiAnalyzed);
            if (aiGenerated) {
                persistenceService.saveMissing(language, instrument.getId(), generated);
            }
            generated.forEach(result -> localized.put(result.newsId(), new LocalizedView(
                    language.code(),
                    result.translatedTitle(),
                    result.summary(),
                    result.direction(),
                    result.score(),
                    result.reason(),
                    result.modelName(),
                    aiGenerated
            )));
        } catch (Exception e) {
            log.warn("News localization failed for language {}: {}", language.code(), e.getMessage());
            Map<UUID, NewsLocalizationPort.Request> requestById = requests.stream()
                    .collect(Collectors.toMap(NewsLocalizationPort.Request::newsId, Function.identity()));
            missing.forEach(article -> {
                var request = requestById.get(article.getId());
                localized.put(article.getId(), fallback(request, language));
            });
        }
        return Map.copyOf(localized);
    }

    private static LocalizedView fallback(NewsLocalizationPort.Request request, NewsLanguage language) {
        String summary = request.content() == null || request.content().isBlank()
                ? request.title()
                : request.content().strip();
        if (summary.length() > 400) {
            summary = summary.substring(0, 400);
        }
        return new LocalizedView(language.code(), request.title(), summary,
                com.investlens.news.domain.ImpactDirection.NEUTRAL, 1,
                "AI 분석을 사용할 수 없어 관련성만 표시했습니다.",
                "localization-unavailable", false);
    }

    public record LocalizedView(
            String language,
            String translatedTitle,
            String summary,
            com.investlens.news.domain.ImpactDirection direction,
            int score,
            String reason,
            String modelName,
            boolean localized
    ) {}
}
