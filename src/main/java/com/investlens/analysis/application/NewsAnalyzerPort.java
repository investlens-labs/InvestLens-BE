package com.investlens.analysis.application;

import com.investlens.ingestion.application.CollectedNews;
import java.util.Set;

public interface NewsAnalyzerPort {
    NewsAnalysisResult analyze(CollectedNews news, Set<String> allowedTickers);
}
