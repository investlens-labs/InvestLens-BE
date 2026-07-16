package com.investlens.news.application;

import com.investlens.common.error.BusinessException;
import com.investlens.common.error.ErrorCode;
import com.investlens.news.api.NewsResponses;
import com.investlens.news.domain.ImpactDirection;
import com.investlens.news.infrastructure.NewsArticleRepository;
import com.investlens.portfolio.application.PortfolioInstrumentQueryService;
import java.util.List;
import java.util.UUID;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class NewsQueryService {
    private final NewsArticleRepository newsRepository;
    private final PortfolioInstrumentQueryService portfolioQueryService;

    public NewsQueryService(NewsArticleRepository newsRepository, PortfolioInstrumentQueryService portfolioQueryService) {
        this.newsRepository = newsRepository;
        this.portfolioQueryService = portfolioQueryService;
    }

    public Page<NewsResponses.FeedItem> getFeed(UUID userId, ImpactDirection direction, Integer minScore, int page, int size) {
        if (minScore != null && (minScore < 1 || minScore > 5)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "minScore는 1 이상 5 이하여야 합니다.");
        }
        List<UUID> instrumentIds = portfolioQueryService.findInstrumentIds(userId);
        var pageable = PageRequest.of(page, Math.min(size, 100), Sort.by(Sort.Order.desc("publishedAt"), Sort.Order.desc("id")));
        if (instrumentIds.isEmpty()) return new PageImpl<>(List.of(), pageable, 0);
        Set<UUID> allowedIds = Set.copyOf(instrumentIds);
        return newsRepository.findFeed(instrumentIds, direction, minScore, pageable)
                .map(article -> NewsResponses.FeedItem.from(article, allowedIds, direction, minScore));
    }

    public NewsResponses.Detail getDetail(UUID userId, UUID newsId) {
        List<UUID> instrumentIds = portfolioQueryService.findInstrumentIds(userId);
        if (instrumentIds.isEmpty()) throw new BusinessException(ErrorCode.NEWS_NOT_FOUND);
        Set<UUID> allowedIds = Set.copyOf(instrumentIds);
        return newsRepository.findDetailForInstruments(newsId, instrumentIds)
                .map(article -> NewsResponses.Detail.from(article, allowedIds))
                .orElseThrow(() -> new BusinessException(ErrorCode.NEWS_NOT_FOUND));
    }
}
