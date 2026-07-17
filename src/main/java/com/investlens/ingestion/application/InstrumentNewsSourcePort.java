package com.investlens.ingestion.application;

import com.investlens.instrument.domain.Instrument;
import java.util.List;

public interface InstrumentNewsSourcePort {
    List<CollectedNews> collect(Instrument instrument);
}
