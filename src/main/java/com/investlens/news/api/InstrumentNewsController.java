package com.investlens.news.api;

import com.investlens.ingestion.application.InstrumentNewsBackfillService;
import com.investlens.news.application.NewsQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/instruments/{instrumentId}/news")
@Tag(name = "Instruments", description = "주식 및 ETF 종목 조회 API")
@SecurityRequirement(name = "bearerAuth")
public class InstrumentNewsController {
    private final InstrumentNewsBackfillService backfillService;
    private final NewsQueryService newsQueryService;

    public InstrumentNewsController(InstrumentNewsBackfillService backfillService,
                                    NewsQueryService newsQueryService) {
        this.backfillService = backfillService;
        this.newsQueryService = newsQueryService;
    }

    @GetMapping
    @Operation(summary = "종목 관련 뉴스 조회",
            description = "과거 관련 기사를 적재하고 발행일 최신순으로 정렬해 선택 언어의 번역 제목, "
                    + "2~3문장 요약, 원문 링크를 반환합니다.")
    public Page<NewsResponses.FeedItem> get(
            @PathVariable UUID instrumentId,
            @RequestParam(defaultValue = "ko") String language,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        backfillService.refreshIfNeeded(instrumentId);
        return newsQueryService.getInstrumentNews(instrumentId, language, page, size);
    }
}
