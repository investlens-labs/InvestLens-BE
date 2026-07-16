package com.investlens.news.api;

import com.investlens.news.application.NewsQueryService;
import com.investlens.news.domain.ImpactDirection;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/news")
@Tag(name = "News", description = "포트폴리오 맞춤 뉴스 및 AI 영향 분석")
@SecurityRequirement(name = "bearerAuth")
public class NewsController {
    private final NewsQueryService newsQueryService;

    public NewsController(NewsQueryService newsQueryService) { this.newsQueryService = newsQueryService; }

    @GetMapping
    @Operation(summary = "맞춤 뉴스 피드", description = "로그인 사용자의 포트폴리오와 관련된 뉴스만 최신순으로 반환합니다.")
    public Page<NewsResponses.FeedItem> feed(
            Authentication authentication,
            @RequestParam(required = false) ImpactDirection direction,
            @RequestParam(required = false) @Min(1) @Max(5) Integer minScore,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return newsQueryService.getFeed(UUID.fromString(authentication.getName()), direction, minScore, page, size);
    }

    @GetMapping("/{newsId}")
    @Operation(summary = "뉴스 상세 조회")
    public NewsResponses.Detail detail(Authentication authentication, @PathVariable UUID newsId) {
        return newsQueryService.getDetail(UUID.fromString(authentication.getName()), newsId);
    }
}
