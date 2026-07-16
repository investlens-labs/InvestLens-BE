package com.investlens.ingestion.application;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.investlens.analysis.application.NewsAnalysisResult;
import com.investlens.analysis.application.NewsAnalyzerPort;
import com.investlens.instrument.domain.Instrument;
import com.investlens.instrument.domain.InstrumentType;
import com.investlens.instrument.infrastructure.InstrumentRepository;
import com.investlens.news.domain.ImpactDirection;
import com.investlens.news.domain.NewsArticle;
import com.investlens.news.infrastructure.NewsArticleRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class NewsIngestionRetryTest {
    @Test
    void retriesPreviouslyFailedArticleInsteadOfSkippingItForever() {
        var source = mock(NewsSourcePort.class);
        var analyzer = mock(NewsAnalyzerPort.class);
        var instrumentRepository = mock(InstrumentRepository.class);
        var newsRepository = mock(NewsArticleRepository.class);
        var persistence = mock(NewsPersistenceService.class);
        var service = new NewsIngestionService(source, analyzer, instrumentRepository, newsRepository, persistence);
        var instrument = new Instrument("NVDA", "NVIDIA", InstrumentType.STOCK);
        var collected = new CollectedNews("source", "https://example.test/retry", "NVIDIA news", "body", Instant.now());
        var failed = new NewsArticle("source", collected.url(), collected.title(), collected.content(), collected.publishedAt());
        failed.failAnalysis("temporary failure");
        var result = new NewsAnalysisResult("번역", "본문", "요약", "맥락", "test-model",
                List.of(new NewsAnalysisResult.Impact("NVDA", ImpactDirection.POSITIVE, 4, "회복")));
        when(instrumentRepository.findAll()).thenReturn(List.of(instrument));
        when(source.collect()).thenReturn(List.of(collected));
        when(newsRepository.findByCanonicalUrl(collected.url())).thenReturn(Optional.of(failed));
        when(analyzer.analyze(any(), any())).thenReturn(result);

        service.collectAndAnalyze();

        verify(analyzer).analyze(any(), any());
        verify(persistence).complete(failed.getId(), result, java.util.Map.of("NVDA", instrument));
    }
}
