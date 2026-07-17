package com.investlens.instrument.application;

import java.time.Instant;
import org.springframework.stereotype.Service;

@Service
public class InstrumentCatalogSyncService {
    private final InstrumentCatalogSourcePort source;
    private final InstrumentCatalogStorePort store;

    public InstrumentCatalogSyncService(InstrumentCatalogSourcePort source, InstrumentCatalogStorePort store) {
        this.source = source;
        this.store = store;
    }

    public int synchronize() {
        var instruments = source.fetchAll();
        store.synchronize(instruments, Instant.now());
        return instruments.size();
    }
}
