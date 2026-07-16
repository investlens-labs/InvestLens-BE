package com.investlens.instrument.presentation.dto;

import com.investlens.instrument.domain.Instrument;
import com.investlens.instrument.domain.InstrumentType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "투자 종목")
public record InstrumentResponse(
        @Schema(example = "f2df1e9c-99d9-4f66-9594-2013526cf41d") UUID id,
        @Schema(example = "NVDA") String ticker,
        @Schema(example = "NVIDIA Corporation") String companyName,
        @Schema(example = "STOCK") InstrumentType type
) {
    public static InstrumentResponse from(Instrument instrument) {
        return new InstrumentResponse(
                instrument.getId(),
                instrument.getTicker(),
                instrument.getCompanyName(),
                instrument.getType()
        );
    }
}
