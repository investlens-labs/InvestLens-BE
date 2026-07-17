package com.investlens.news.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.investlens.analysis.application.NewsLocalizationPort;
import com.investlens.instrument.domain.Instrument;
import com.investlens.instrument.domain.InstrumentMarket;
import com.investlens.instrument.domain.InstrumentType;
import com.investlens.instrument.infrastructure.InstrumentRepository;
import com.investlens.news.domain.ImpactDirection;
import com.investlens.news.domain.NewsArticle;
import com.investlens.news.domain.NewsImpact;
import com.investlens.news.domain.NewsLanguage;
import com.investlens.news.infrastructure.NewsArticleRepository;
import com.investlens.news.infrastructure.NewsTranslationRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class NewsTranslationPersistenceServiceTest {
    @Autowired NewsTranslationPersistenceService persistenceService;
    @Autowired NewsTranslationRepository translationRepository;
    @Autowired NewsArticleRepository newsRepository;
    @Autowired InstrumentRepository instrumentRepository;

    @Test
    void storesLocalizedSummaryAndReplacesFallbackImpactWithAiAssessment() {
        Instrument instrument = instrumentRepository.saveAndFlush(
                new Instrument("AI01", "AI Test", InstrumentType.STOCK, InstrumentMarket.US));
        NewsArticle article = new NewsArticle(
                "Test", "https://example.test/ai-assessment", "Original title", "Original content", Instant.now());
        article.relateTo(List.of(instrument));
        article.completeAnalysis("Original title", "Original content", "Fallback", "Fallback",
                "local-fallback", List.of(new NewsImpact(
                        instrument, ImpactDirection.NEUTRAL, 1,
                        "AI 분석이 비활성화되어 관련성만 표시했습니다.")));
        article = newsRepository.saveAndFlush(article);

        persistenceService.saveMissing(NewsLanguage.KO, instrument.getId(), List.of(
                new NewsLocalizationPort.Result(
                        article.getId(), "번역 제목", "짧은 요약입니다.",
                        ImpactDirection.POSITIVE, 4, "직접적인 수요 증가가 명시됐습니다.",
                        "gemini-test", true)
        ));

        var translations = translationRepository.findAllByNewsIdInAndLanguage(
                List.of(article.getId()), NewsLanguage.KO.code());
        assertThat(translations).singleElement().satisfies(translation -> {
            assertThat(translation.getTranslatedTitle()).isEqualTo("번역 제목");
            assertThat(translation.getImpactDirection()).isEqualTo(ImpactDirection.POSITIVE);
            assertThat(translation.getImpactScore()).isEqualTo(4);
        });
        NewsImpact updated = newsRepository.findDetailForInstruments(
                article.getId(), List.of(instrument.getId())).orElseThrow().getImpacts().get(0);
        assertThat(updated.isAiAnalyzed()).isTrue();
        assertThat(updated.getDirection()).isEqualTo(ImpactDirection.POSITIVE);
        assertThat(updated.getScore()).isEqualTo(4);
        assertThat(updated.getAnalysisModel()).isEqualTo("gemini-test");
    }
}
