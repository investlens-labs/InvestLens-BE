package com.investlens.analysis.application;

import com.investlens.news.domain.ImpactDirection;
import java.util.List;

public record NewsAnalysisResult(
        String translatedTitle,
        String translatedContent,
        String summary,
        String marketContext,
        String modelName,
        List<Impact> impacts
) {
    public record Impact(
            String ticker,
            ImpactDirection direction,
            int score,
            String reason,
            Integer upProbability,
            Integer downProbability,
            Integer neutralProbability
    ) {
        public Impact(String ticker, ImpactDirection direction, int score, String reason) {
            this(ticker, direction, score, reason, null, null, null);
        }
    }
}
