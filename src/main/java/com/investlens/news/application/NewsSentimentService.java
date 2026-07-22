package com.investlens.news.application;

import com.investlens.common.error.BusinessException;
import com.investlens.common.error.ErrorCode;
import com.investlens.news.api.NewsResponses;
import com.investlens.news.domain.NewsImpact;
import com.investlens.news.infrastructure.NewsArticleRepository;
import com.investlens.instrument.infrastructure.InstrumentRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class NewsSentimentService {
    private final NewsArticleRepository newsRepository;
    private final InstrumentRepository instrumentRepository;

    public NewsSentimentService(NewsArticleRepository newsRepository,
                                InstrumentRepository instrumentRepository) {
        this.newsRepository = newsRepository;
        this.instrumentRepository = instrumentRepository;
    }

    public NewsResponses.Sentiment getInstrumentSentiment(UUID instrumentId) {
        instrumentRepository.findById(instrumentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INSTRUMENT_NOT_FOUND));
        List<NewsImpact> impacts = newsRepository.findAllWithImpactsByInstrumentId(instrumentId).stream()
                .flatMap(article -> article.getImpacts().stream())
                .filter(impact -> impact.getInstrument().getId().equals(instrumentId))
                .toList();
        List<NewsImpact> analyzed = impacts.stream()
                .filter(impact -> impact.isAiAnalyzed()
                        && impact.getUpProbability() != null
                        && impact.getDownProbability() != null
                        && impact.getNeutralProbability() != null)
                .toList();
        if (analyzed.isEmpty()) {
            return NewsResponses.Sentiment.unavailable(impacts.size());
        }

        int up = roundAverage(analyzed, Probability.UP);
        int down = roundAverage(analyzed, Probability.DOWN);
        int neutral = Math.max(0, 100 - up - down);
        String model = analyzed.stream().map(NewsImpact::getAnalysisModel)
                .filter(value -> value != null && !value.isBlank())
                .distinct().reduce((left, right) -> "mixed").orElse("unknown");
        return new NewsResponses.Sentiment(
                true, analyzed.size(), impacts.size(), up, down, neutral, model,
                "최근 관련 기사에 대한 AI 기반 단기 시장 반응 가능성입니다. 주가 예측이나 투자 조언이 아닙니다.");
    }

    private static int roundAverage(List<NewsImpact> impacts, Probability probability) {
        double average = impacts.stream().mapToInt(probability::value).average().orElse(0);
        return (int) Math.round(average);
    }

    private enum Probability {
        UP {
            @Override int value(NewsImpact impact) { return impact.getUpProbability(); }
        },
        DOWN {
            @Override int value(NewsImpact impact) { return impact.getDownProbability(); }
        };

        abstract int value(NewsImpact impact);
    }
}
