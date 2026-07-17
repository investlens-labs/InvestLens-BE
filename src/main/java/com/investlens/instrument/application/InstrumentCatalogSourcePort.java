package com.investlens.instrument.application;

import java.util.List;

public interface InstrumentCatalogSourcePort {
    List<InstrumentCatalogItem> fetchAll();
}
