package com.investlens.ingestion.application;

import com.investlens.common.error.BusinessException;
import com.investlens.common.error.ErrorCode;
import com.investlens.ingestion.infrastructure.NewsSearchProperties;
import com.investlens.instrument.infrastructure.InstrumentRepository;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class InstrumentNewsBackfillService {
    private static final Logger log = LoggerFactory.getLogger(InstrumentNewsBackfillService.class);

    private final NewsSearchProperties properties;
    private final InstrumentRepository instrumentRepository;
    private final InstrumentNewsSourcePort source;
    private final NewsIngestionService ingestionService;
    private final ConcurrentHashMap<UUID, Instant> refreshAfter = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Object> locks = new ConcurrentHashMap<>();

    public InstrumentNewsBackfillService(NewsSearchProperties properties,
                                         InstrumentRepository instrumentRepository,
                                         InstrumentNewsSourcePort source,
                                         NewsIngestionService ingestionService) {
        this.properties = properties;
        this.instrumentRepository = instrumentRepository;
        this.source = source;
        this.ingestionService = ingestionService;
    }

    public void refreshIfNeeded(UUID instrumentId) {
        var instrument = instrumentRepository.findById(instrumentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INSTRUMENT_NOT_FOUND));
        if (!properties.enabled() || !needsRefresh(instrumentId)) return;

        synchronized (locks.computeIfAbsent(instrumentId, ignored -> new Object())) {
            if (!needsRefresh(instrumentId)) return;
            var collected = source.collect(instrument);
            int saved = ingestionService.ingestForInstrument(instrument, collected);
            refreshAfter.put(instrumentId, Instant.now().plus(properties.refreshInterval()));
            log.info("Instrument news refresh completed for {}: {} fetched, {} saved",
                    instrument.getTicker(), collected.size(), saved);
        }
    }

    private boolean needsRefresh(UUID instrumentId) {
        Instant nextRefresh = refreshAfter.get(instrumentId);
        return nextRefresh == null || !nextRefresh.isAfter(Instant.now());
    }
}
