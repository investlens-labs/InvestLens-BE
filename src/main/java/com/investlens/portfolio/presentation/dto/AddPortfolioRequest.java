package com.investlens.portfolio.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

@Schema(description = "포트폴리오 종목 등록 요청")
public record AddPortfolioRequest(
        @NotNull(message = "종목 ID는 필수입니다.")
        @Schema(description = "등록할 종목 ID", example = "f2df1e9c-99d9-4f66-9594-2013526cf41d")
        UUID instrumentId
) {
}
