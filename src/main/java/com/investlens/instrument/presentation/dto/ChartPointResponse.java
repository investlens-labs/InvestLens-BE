package com.investlens.instrument.presentation.dto;

import com.investlens.instrument.application.ChartPoint;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "OHLCV 차트 데이터")
public record ChartPointResponse(
        @Schema(example = "1784202600", description = "Unix timestamp (초)") long timestamp,
        @Schema(example = "88000") double open,
        @Schema(example = "89500") double high,
        @Schema(example = "87800") double low,
        @Schema(example = "89200") double close,
        @Schema(example = "18230122") long volume
) {
    public static ChartPointResponse from(ChartPoint point) {
        return new ChartPointResponse(point.timestamp(), point.open(), point.high(), point.low(), point.close(), point.volume());
    }
}
