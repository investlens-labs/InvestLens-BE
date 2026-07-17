package com.investlens.instrument.application;

import com.investlens.instrument.domain.InstrumentMarket;
import com.investlens.instrument.domain.InstrumentType;

public record InstrumentCatalogItem(
        String ticker,
        String companyName,
        InstrumentType type,
        InstrumentMarket market
) {
}
