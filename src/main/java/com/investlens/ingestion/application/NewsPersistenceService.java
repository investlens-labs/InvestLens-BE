package com.investlens.ingestion.application;

import com.investlens.analysis.application.NewsAnalysisResult;
import com.investlens.common.error.BusinessException;
import com.investlens.common.error.ErrorCode;
import com.investlens.instrument.domain.Instrument;
import com.investlens.news.domain.NewsArticle;
import com.investlens.news.domain.NewsImpact;
import com.investlens.news.infrastructure.NewsArticleRepository;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NewsPersistenceService {
    private final NewsArticleRepository newsRepository;

    public NewsPersistenceService(NewsArticleRepository newsRepository) { this.newsRepository = newsRepository; }

    @Transactional
    public NewsArticle createPending(CollectedNews collected, List<Instrument> relatedInstruments) {
        NewsArticle article = new NewsArticle(collected.source(), collected.url(), collected.title(),
                collected.content(), collected.publishedAt());
        article.relateTo(relatedInstruments);
        return newsRepository.saveAndFlush(article);
    }

    @Transactional
    public void complete(UUID articleId, NewsAnalysisResult result, Map<String, Instrument> instruments) {
        NewsArticle article = getArticle(articleId);
        var impacts = result.impacts().stream()
                .map(item -> {
                    Instrument instrument = instruments.get(item.ticker().toUpperCase(Locale.ROOT));
                    if (instrument == null) throw new IllegalArgumentException("Analyzer returned an unapproved ticker");
                    return new NewsImpact(instrument, item.direction(), item.score(), item.reason());
                }).toList();
        if (impacts.isEmpty()) throw new IllegalStateException("Analyzer returned no allowed impacts");
        article.completeAnalysis(result.translatedTitle(), result.translatedContent(), result.summary(),
                result.marketContext(), result.modelName(), impacts);
    }

    @Transactional
    public void fail(UUID articleId, String error) {
        getArticle(articleId).failAnalysis(error);
    }

    private NewsArticle getArticle(UUID id) {
        return newsRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.NEWS_NOT_FOUND));
    }
}
