package com.investlens.instrument.presentation.dto;

import com.investlens.instrument.domain.Instrument;
import com.investlens.instrument.domain.InstrumentType;
import com.investlens.instrument.domain.InstrumentMarket;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "투자 종목")
public record InstrumentResponse(
        @Schema(example = "f2df1e9c-99d9-4f66-9594-2013526cf41d") UUID id,
        @Schema(example = "NVDA") String ticker,
        @Schema(example = "NVIDIA Corporation") String companyName,
        @Schema(example = "STOCK") InstrumentType type,
        @Schema(example = "US") InstrumentMarket market,
        @Schema(description = "종목 로고 이미지 URL. 설정되지 않은 경우 null", example = "https://img.logo.dev/ticker/NVDA?token=...")
        String logoUrl,
        @Schema(description = "무료 로고 사용 시 표시할 출처 링크", example = "https://logo.dev")
        String logoAttributionUrl
) {
    public static InstrumentResponse from(Instrument instrument, String logoUrl, String logoAttributionUrl) {
        return new InstrumentResponse(
                instrument.getId(),
                instrument.getTicker(),
                instrument.getCompanyName(),
                instrument.getType(),
                instrument.getMarket(),
                logoUrl,
                logoUrl == null ? null : logoAttributionUrl
        );
    }
}
