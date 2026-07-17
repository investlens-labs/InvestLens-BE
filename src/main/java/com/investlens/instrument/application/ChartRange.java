package com.investlens.instrument.application;

import com.investlens.common.error.BusinessException;
import com.investlens.common.error.ErrorCode;
import java.util.Arrays;

public enum ChartRange {
    ONE_DAY("1D", "1d", "5m"),
    ONE_WEEK("1W", "5d", "15m"),
    ONE_MONTH("1M", "1mo", "1d"),
    THREE_MONTHS("3M", "3mo", "1d"),
    ONE_YEAR("1Y", "1y", "1d"),
    FIVE_YEARS("5Y", "5y", "1wk");

    private final String value;
    private final String providerRange;
    private final String interval;

    ChartRange(String value, String providerRange, String interval) {
        this.value = value;
        this.providerRange = providerRange;
        this.interval = interval;
    }

    public String value() {
        return value;
    }

    public String providerRange() {
        return providerRange;
    }

    public String interval() {
        return interval;
    }

    public static ChartRange from(String value) {
        return Arrays.stream(values())
                .filter(range -> range.value.equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REQUEST, "지원하지 않는 차트 기간입니다."));
    }
}
