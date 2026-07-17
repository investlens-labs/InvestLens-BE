package com.investlens.instrument.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.investlens.common.error.BusinessException;
import com.investlens.instrument.domain.Instrument;
import com.investlens.instrument.domain.InstrumentMarket;
import com.investlens.instrument.domain.InstrumentType;
import com.investlens.instrument.infrastructure.InstrumentRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class InstrumentChartServiceTest {
    @Test
    void cachesChartResponsesForTheSameInstrumentAndRange() {
        var repository = mock(InstrumentRepository.class);
        var source = mock(InstrumentChartSourcePort.class);
        var service = new InstrumentChartService(repository, source);
        var id = UUID.randomUUID();
        var instrument = new Instrument("AAPL", "Apple", InstrumentType.STOCK, InstrumentMarket.US);
        var data = new InstrumentChartData("USD", "America/New_York", 210, 200, 0,
                List.of(new ChartPoint(100, 200, 211, 199, 210, 1000)));
        when(repository.findById(id)).thenReturn(Optional.of(instrument));
        when(source.fetch(instrument, ChartRange.ONE_MONTH)).thenReturn(data);

        var first = service.get(id, "1M");
        var second = service.get(id, "1m");

        assertThat(first).isSameAs(second);
        assertThat(first.change()).isEqualTo(10);
        assertThat(first.changeRate()).isEqualTo(5);
        verify(source).fetch(instrument, ChartRange.ONE_MONTH);
    }

    @Test
    void rejectsUnsupportedRangesBeforeCallingExternalSource() {
        var repository = mock(InstrumentRepository.class);
        var source = mock(InstrumentChartSourcePort.class);
        var service = new InstrumentChartService(repository, source);

        assertThatThrownBy(() -> service.get(UUID.randomUUID(), "10Y"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("지원하지 않는 차트 기간입니다.");
        verifyNoInteractions(repository, source);
    }
}
