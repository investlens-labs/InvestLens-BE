package com.investlens.portfolio.presentation.dto;

import com.investlens.instrument.domain.Instrument;
import com.investlens.instrument.domain.InstrumentType;
import com.investlens.portfolio.domain.PortfolioItem;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "사용자 포트폴리오 항목")
public record PortfolioItemResponse(
        UUID id,
        UUID instrumentId,
        @Schema(example = "NVDA") String ticker,
        @Schema(example = "NVIDIA Corporation") String companyName,
        @Schema(example = "STOCK") InstrumentType type,
        Instant createdAt
) {
    public static PortfolioItemResponse from(PortfolioItem item) {
        Instrument instrument = item.getInstrument();
        return new PortfolioItemResponse(
                item.getId(),
                instrument.getId(),
                instrument.getTicker(),
                instrument.getCompanyName(),
                instrument.getType(),
                item.getCreatedAt()
        );
    }
}
