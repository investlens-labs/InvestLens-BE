package com.investlens.analysis.infrastructure;

import com.investlens.analysis.application.NewsLocalizationPort;
import com.investlens.news.domain.ImpactDirection;
import com.investlens.news.domain.NewsLanguage;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.gemini", name = "enabled", havingValue = "false", matchIfMissing = true)
public class LocalNewsLocalizationClient implements NewsLocalizationPort {
    @Override
    public List<Result> localize(List<Request> articles, NewsLanguage language) {
        return articles.stream()
                .map(article -> new Result(article.newsId(), article.title(),
                        excerpt(article.content(), article.title()),
                        ImpactDirection.NEUTRAL, 1,
                        "AI 분석이 비활성화되어 관련성만 표시했습니다.",
                        null, null, null,
                        "local-fallback", false))
                .toList();
    }

    private static String excerpt(String content, String title) {
        String value = content == null || content.isBlank() ? title : content.strip();
        return value.length() <= 400 ? value : value.substring(0, 400);
    }
}
