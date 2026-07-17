package com.investlens.instrument.infrastructure;

import com.investlens.instrument.application.InstrumentCatalogSyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class InstrumentCatalogScheduler {
    private static final Logger log = LoggerFactory.getLogger(InstrumentCatalogScheduler.class);
    private final InstrumentCatalogProperties properties;
    private final InstrumentCatalogSyncService service;

    public InstrumentCatalogScheduler(InstrumentCatalogProperties properties, InstrumentCatalogSyncService service) {
        this.properties = properties;
        this.service = service;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void synchronizeOnStartup() {
        synchronize();
    }

    @Scheduled(cron = "${app.instrument-catalog.cron:0 0 3 * * *}", zone = "Asia/Seoul")
    public void synchronize() {
        if (!properties.enabled()) return;
        try {
            int count = service.synchronize();
            log.info("Instrument catalog synchronization completed: {} instruments", count);
        } catch (Exception e) {
            log.warn("Instrument catalog synchronization failed; keeping previous catalog: {}", e.getMessage());
        }
    }
}
