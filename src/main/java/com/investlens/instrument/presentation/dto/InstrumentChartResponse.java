package com.investlens.instrument.presentation.dto;

import com.investlens.instrument.application.ChartRange;
import com.investlens.instrument.application.InstrumentChartData;
import com.investlens.instrument.domain.Instrument;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.UUID;

@Schema(description = "종목 기간별 가격 차트")
public record InstrumentChartResponse(
        UUID instrumentId,
        String ticker,
        @Schema(example = "1M") String range,
        @Schema(example = "1d") String interval,
        @Schema(example = "KRW") String currency,
        @Schema(example = "Asia/Seoul") String timezone,
        double currentPrice,
        double previousClose,
        double change,
        double changeRate,
        @Schema(description = "거래소 데이터 지연 시간(초)") int exchangeDataDelayedBy,
        List<ChartPointResponse> points
) {
    public InstrumentChartResponse {
        points = List.copyOf(points);
    }

    public static InstrumentChartResponse from(Instrument instrument, ChartRange range, InstrumentChartData data) {
        double change = data.currentPrice() - data.previousClose();
        double changeRate = data.previousClose() == 0 ? 0 : change / data.previousClose() * 100;
        return new InstrumentChartResponse(
                instrument.getId(), instrument.getTicker(), range.value(), range.interval(), data.currency(),
                data.timezone(), data.currentPrice(), data.previousClose(), change, changeRate,
                data.exchangeDataDelayedBy(), data.points().stream().map(ChartPointResponse::from).toList()
        );
    }
}
