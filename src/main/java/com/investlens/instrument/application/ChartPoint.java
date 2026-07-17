package com.investlens.instrument.application;

public record ChartPoint(
        long timestamp,
        double open,
        double high,
        double low,
        double close,
        long volume
) {
}
