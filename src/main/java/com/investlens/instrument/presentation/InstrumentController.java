package com.investlens.instrument.presentation;

import com.investlens.instrument.application.InstrumentQueryService;
import com.investlens.instrument.domain.InstrumentType;
import com.investlens.instrument.presentation.dto.InstrumentResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/instruments")
@Tag(name = "Instruments", description = "주식 및 ETF 종목 조회 API")
@SecurityRequirement(name = "bearerAuth")
public class InstrumentController {
    private final InstrumentQueryService instrumentQueryService;

    public InstrumentController(InstrumentQueryService instrumentQueryService) {
        this.instrumentQueryService = instrumentQueryService;
    }

    @GetMapping
    @Operation(summary = "종목 검색", description = "티커 또는 종목명으로 주식과 ETF를 검색합니다.")
    public List<InstrumentResponse> search(
            @Parameter(description = "티커 또는 종목명") @RequestParam(required = false) String query,
            @Parameter(description = "종목 유형") @RequestParam(required = false) InstrumentType type
    ) {
        return instrumentQueryService.search(query, type);
    }

    @GetMapping("/{instrumentId}")
    @Operation(summary = "종목 상세 조회")
    public InstrumentResponse get(@PathVariable UUID instrumentId) {
        return instrumentQueryService.get(instrumentId);
    }
}
