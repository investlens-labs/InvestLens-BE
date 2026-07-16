package com.investlens.analysis.infrastructure;

import com.investlens.analysis.application.NewsAnalysisResult;
import com.investlens.analysis.application.NewsAnalyzerPort;
import com.investlens.ingestion.application.CollectedNews;
import com.investlens.news.domain.ImpactDirection;
import java.util.Set;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.ai", name = "enabled", havingValue = "false", matchIfMissing = true)
public class LocalFallbackNewsAnalyzer implements NewsAnalyzerPort {
    @Override
    public NewsAnalysisResult analyze(CollectedNews news, Set<String> allowedTickers) {
        String excerpt = news.content() == null || news.content().isBlank() ? news.title() : news.content();
        if (excerpt.length() > 500) excerpt = excerpt.substring(0, 500);
        var impacts = allowedTickers.stream()
                .map(ticker -> new NewsAnalysisResult.Impact(ticker, ImpactDirection.NEUTRAL, 1,
                        "AI 분석이 비활성화되어 관련성만 표시했습니다."))
                .toList();
        return new NewsAnalysisResult(news.title(), excerpt, excerpt,
                "Ollama를 활성화하면 한국어 시장 영향 분석이 제공됩니다.", "local-fallback", impacts);
    }
}
