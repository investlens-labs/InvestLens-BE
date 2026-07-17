package com.investlens.instrument.application;

import java.time.Instant;
import java.util.List;

public interface InstrumentCatalogStorePort {
    void synchronize(List<InstrumentCatalogItem> instruments, Instant synchronizedAt);
}
