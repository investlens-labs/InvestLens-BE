package com.investlens.instrument.application;

import com.investlens.instrument.domain.Instrument;

public interface InstrumentChartSourcePort {
    InstrumentChartData fetch(Instrument instrument, ChartRange range);
}
