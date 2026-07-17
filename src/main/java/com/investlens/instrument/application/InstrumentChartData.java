package com.investlens.instrument.application;

import java.util.List;

public record InstrumentChartData(
        String currency,
        String timezone,
        double currentPrice,
        double previousClose,
        int exchangeDataDelayedBy,
        List<ChartPoint> points
) {
    public InstrumentChartData {
        points = List.copyOf(points);
    }
}
