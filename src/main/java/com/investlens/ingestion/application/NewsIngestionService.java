package com.investlens.ingestion.application;

import com.investlens.analysis.application.NewsAnalyzerPort;
import com.investlens.instrument.domain.Instrument;
import com.investlens.instrument.infrastructure.InstrumentRepository;
import com.investlens.news.domain.NewsArticle;
import com.investlens.news.domain.AnalysisStatus;
import com.investlens.news.infrastructure.NewsArticleRepository;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.dao.DataIntegrityViolationException;

@Service
public class NewsIngestionService {
    private static final Logger log = LoggerFactory.getLogger(NewsIngestionService.class);
    private final NewsSourcePort newsSource;
    private final NewsAnalyzerPort analyzer;
    private final InstrumentRepository instrumentRepository;
    private final NewsArticleRepository newsRepository;
    private final NewsPersistenceService persistenceService;

    public NewsIngestionService(NewsSourcePort newsSource, NewsAnalyzerPort analyzer,
                                InstrumentRepository instrumentRepository, NewsArticleRepository newsRepository,
                                NewsPersistenceService persistenceService) {
        this.newsSource = newsSource;
        this.analyzer = analyzer;
        this.instrumentRepository = instrumentRepository;
        this.newsRepository = newsRepository;
        this.persistenceService = persistenceService;
    }

    public int collectAndAnalyze() {
        Map<String, Instrument> instruments = instrumentRepository.findAll().stream()
                .collect(Collectors.toMap(Instrument::getTicker, it -> it, (a, b) -> a, LinkedHashMap::new));
        int saved = 0;
        for (CollectedNews collected : newsSource.collect()) {
            Set<String> matched = matchTickers(collected, instruments);
            if (matched.isEmpty()) continue;
            Map<String, Instrument> matchedInstruments = matched.stream()
                    .collect(Collectors.toMap(ticker -> ticker, instruments::get));
            NewsArticle article = newsRepository.findByCanonicalUrl(collected.url()).orElse(null);
            if (article != null && article.getAnalysisStatus() == AnalysisStatus.COMPLETED) continue;
            if (article == null) {
                try {
                    article = persistenceService.createPending(collected, List.copyOf(matchedInstruments.values()));
                    saved++;
                } catch (DataIntegrityViolationException duplicateRace) {
                    article = newsRepository.findByCanonicalUrl(collected.url()).orElse(null);
                    if (article == null) continue;
                }
            }
            analyze(article, collected, matched, matchedInstruments);
        }
        return saved;
    }

    public int retryPendingAnalyses() {
        var retryable = newsRepository.findTop50ByAnalysisStatusInOrderByUpdatedAtAsc(
                List.of(AnalysisStatus.PENDING, AnalysisStatus.FAILED));
        for (NewsArticle article : retryable) {
            Map<String, Instrument> instruments = article.getRelatedInstruments().stream()
                    .map(related -> related.getInstrument())
                    .collect(Collectors.toMap(Instrument::getTicker, instrument -> instrument));
            if (instruments.isEmpty()) continue;
            var collected = new CollectedNews(article.getSource(), article.getCanonicalUrl(), article.getTitle(),
                    article.getOriginalContent(), article.getPublishedAt());
            analyze(article, collected, instruments.keySet(), instruments);
        }
        return retryable.size();
    }

    public int ingestForInstrument(Instrument instrument, List<CollectedNews> collectedNews) {
        Map<String, Instrument> instrumentByTicker = Map.of(instrument.getTicker(), instrument);
        Set<String> allowedTickers = instrumentByTicker.keySet();
        int saved = 0;
        for (CollectedNews collected : collectedNews) {
            NewsArticle article = newsRepository.findByCanonicalUrl(collected.url()).orElse(null);
            if (article != null && article.getAnalysisStatus() == AnalysisStatus.COMPLETED) continue;
            if (article == null) {
                try {
                    article = persistenceService.createPending(collected, List.of(instrument));
                    saved++;
                } catch (DataIntegrityViolationException duplicateRace) {
                    article = newsRepository.findByCanonicalUrl(collected.url()).orElse(null);
                    if (article == null || article.getAnalysisStatus() == AnalysisStatus.COMPLETED) continue;
                }
            }
            analyze(article, collected, allowedTickers, instrumentByTicker);
        }
        return saved;
    }

    private void analyze(NewsArticle article, CollectedNews collected, Set<String> matched,
                           Map<String, Instrument> instruments) {
        try {
            var result = analyzer.analyze(collected, matched);
            persistenceService.complete(article.getId(), result, instruments);
        } catch (Exception e) {
            log.warn("News analysis failed for {}: {}", article.getId(), e.getMessage());
            persistenceService.fail(article.getId(), e.getMessage());
        }
    }

    static Set<String> matchTickers(CollectedNews news, Map<String, Instrument> instruments) {
        String haystack = news.title() + " " + (news.content() == null ? "" : news.content());
        return instruments.values().stream()
                .filter(instrument -> containsToken(haystack, instrument.getTicker())
                        || containsIgnoreCase(haystack, instrument.getCompanyName()))
                .map(Instrument::getTicker)
                .collect(Collectors.toUnmodifiableSet());
    }

    private static boolean containsToken(String text, String token) {
        return Pattern.compile("(?i)(?<![A-Z0-9])" + Pattern.quote(token) + "(?![A-Z0-9])")
                .matcher(text).find();
    }

    private static boolean containsIgnoreCase(String text, String value) {
        return text.toLowerCase(java.util.Locale.ROOT).contains(value.toLowerCase(java.util.Locale.ROOT));
    }
}
