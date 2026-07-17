package com.investlens.instrument.application;

import com.investlens.common.error.BusinessException;
import com.investlens.common.error.ErrorCode;
import com.investlens.instrument.infrastructure.InstrumentRepository;
import com.investlens.instrument.presentation.dto.InstrumentChartResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class InstrumentChartService {
    private static final int MAX_CACHE_ENTRIES = 1_000;
    private final InstrumentRepository instrumentRepository;
    private final InstrumentChartSourcePort chartSource;
    private final Map<CacheKey, CacheEntry> cache = new ConcurrentHashMap<>();

    public InstrumentChartService(InstrumentRepository instrumentRepository, InstrumentChartSourcePort chartSource) {
        this.instrumentRepository = instrumentRepository;
        this.chartSource = chartSource;
    }

    public InstrumentChartResponse get(UUID instrumentId, String rangeValue) {
        ChartRange range = ChartRange.from(rangeValue);
        var instrument = instrumentRepository.findById(instrumentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INSTRUMENT_NOT_FOUND));
        CacheKey key = new CacheKey(instrumentId, range);
        CacheEntry cached = cache.get(key);
        if (cached != null && cached.expiresAt().isAfter(Instant.now())) return cached.response();

        try {
            var response = InstrumentChartResponse.from(instrument, range, chartSource.fetch(instrument, range));
            maintainCache();
            cache.put(key, new CacheEntry(response, Instant.now().plus(cacheDuration(range))));
            return response;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.EXTERNAL_SERVICE_ERROR, "시세 데이터를 불러오지 못했습니다.");
        }
    }

    private void maintainCache() {
        if (cache.size() < MAX_CACHE_ENTRIES) return;
        Instant now = Instant.now();
        cache.entrySet().removeIf(entry -> entry.getValue().expiresAt().isBefore(now));
        if (cache.size() >= MAX_CACHE_ENTRIES) cache.clear();
    }

    private static Duration cacheDuration(ChartRange range) {
        return switch (range) {
            case ONE_DAY -> Duration.ofSeconds(30);
            case ONE_WEEK -> Duration.ofMinutes(5);
            default -> Duration.ofHours(1);
        };
    }

    private record CacheKey(UUID instrumentId, ChartRange range) {
    }

    private record CacheEntry(InstrumentChartResponse response, Instant expiresAt) {
    }
}
