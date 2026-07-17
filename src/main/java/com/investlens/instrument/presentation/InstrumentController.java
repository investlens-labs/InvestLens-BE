package com.investlens.instrument.presentation;

import com.investlens.instrument.application.InstrumentQueryService;
import com.investlens.instrument.application.InstrumentChartService;
import com.investlens.instrument.domain.InstrumentType;
import com.investlens.instrument.domain.InstrumentMarket;
import com.investlens.instrument.presentation.dto.InstrumentResponse;
import com.investlens.instrument.presentation.dto.InstrumentChartResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;

@Validated
@RestController
@RequestMapping("/api/v1/instruments")
@Tag(name = "Instruments", description = "주식 및 ETF 종목 조회 API")
@SecurityRequirement(name = "bearerAuth")
public class InstrumentController {
    private final InstrumentQueryService instrumentQueryService;
    private final InstrumentChartService instrumentChartService;

    public InstrumentController(InstrumentQueryService instrumentQueryService,
                                InstrumentChartService instrumentChartService) {
        this.instrumentQueryService = instrumentQueryService;
        this.instrumentChartService = instrumentChartService;
    }

    @GetMapping
    @Operation(summary = "종목 검색", description = "외부 데이터에서 동기화한 한국·미국 주식과 ETF를 검색합니다.")
    public List<InstrumentResponse> search(
            @Parameter(description = "티커 또는 종목명") @RequestParam(required = false) String query,
            @Parameter(description = "종목 유형: STOCK 또는 ETF") @RequestParam(required = false) InstrumentType type,
            @Parameter(description = "시장: KR 또는 US") @RequestParam(required = false) InstrumentMarket market,
            @Parameter(description = "조회 개수 (최대 100)") @RequestParam(defaultValue = "50")
            @Min(1) @Max(100) int limit
    ) {
        return instrumentQueryService.search(query, type, market, limit);
    }

    @GetMapping("/{instrumentId}")
    @Operation(summary = "종목 상세 조회")
    public InstrumentResponse get(@PathVariable UUID instrumentId) {
        return instrumentQueryService.get(instrumentId);
    }

    @GetMapping("/{instrumentId}/chart")
    @Operation(summary = "종목 가격 차트 조회", description = "한국·미국 주식과 ETF의 기간별 OHLCV 데이터를 조회합니다.")
    public InstrumentChartResponse chart(
            @PathVariable UUID instrumentId,
            @Parameter(description = "차트 기간", schema = @io.swagger.v3.oas.annotations.media.Schema(
                    allowableValues = {"1D", "1W", "1M", "3M", "1Y", "5Y"}))
            @RequestParam(defaultValue = "1M") String range
    ) {
        return instrumentChartService.get(instrumentId, range);
    }
}
