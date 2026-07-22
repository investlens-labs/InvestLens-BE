package com.investlens.news.application;

import com.investlens.analysis.application.NewsLocalizationPort;
import com.investlens.news.domain.NewsLanguage;
import com.investlens.news.domain.NewsTranslation;
import com.investlens.news.infrastructure.NewsArticleRepository;
import com.investlens.news.infrastructure.NewsTranslationRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NewsTranslationPersistenceService {
    private final NewsTranslationRepository repository;
    private final NewsArticleRepository newsRepository;

    public NewsTranslationPersistenceService(NewsTranslationRepository repository,
                                             NewsArticleRepository newsRepository) {
        this.repository = repository;
        this.newsRepository = newsRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public synchronized void saveMissing(NewsLanguage language, UUID instrumentId,
                                         List<NewsLocalizationPort.Result> results) {
        results.forEach(result -> {
            var article = newsRepository.findById(result.newsId()).orElseThrow();
            var existing = repository.findAllByNewsIdInAndLanguage(
                    List.of(result.newsId()), language.code()).stream().findFirst();
            if (existing.isEmpty()) {
                repository.save(new NewsTranslation(
                        article,
                        language,
                        result.translatedTitle(),
                        result.summary(),
                        result.direction(),
                        result.score(),
                        result.reason(),
                        result.upProbability(),
                        result.downProbability(),
                        result.neutralProbability(),
                        result.modelName()
                ));
            } else {
                existing.get().update(result.translatedTitle(), result.summary(), result.direction(), result.score(),
                        result.reason(), result.upProbability(), result.downProbability(), result.neutralProbability(),
                        result.modelName());
            }
            article.updateImpactAssessment(instrumentId, result.direction(), result.score(),
                    result.reason(), result.modelName(), result.upProbability(),
                    result.downProbability(), result.neutralProbability());
        });
    }
}
