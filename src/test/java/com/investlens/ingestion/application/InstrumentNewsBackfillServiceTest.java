package com.investlens.ingestion.application;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.investlens.ingestion.infrastructure.NewsSearchProperties;
import com.investlens.instrument.domain.Instrument;
import com.investlens.instrument.domain.InstrumentMarket;
import com.investlens.instrument.domain.InstrumentType;
import com.investlens.instrument.infrastructure.InstrumentRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class InstrumentNewsBackfillServiceTest {
    @Test
    void refreshesOnlyOnceWithinRefreshInterval() {
        var instrumentRepository = mock(InstrumentRepository.class);
        var source = mock(InstrumentNewsSourcePort.class);
        var ingestion = mock(NewsIngestionService.class);
        var properties = new NewsSearchProperties(true, null, Duration.ofSeconds(1),
                Duration.ofHours(1), 90, 20);
        var service = new InstrumentNewsBackfillService(properties, instrumentRepository, source, ingestion);
        var instrument = new Instrument("AAPL", "Apple", InstrumentType.STOCK, InstrumentMarket.US);
        var instrumentId = UUID.randomUUID();
        var article = new CollectedNews("Google News", "https://example.test/apple", "Apple news", "body",
                Instant.now());
        when(instrumentRepository.findById(instrumentId)).thenReturn(Optional.of(instrument));
        when(source.collect(instrument)).thenReturn(List.of(article));

        service.refreshIfNeeded(instrumentId);
        service.refreshIfNeeded(instrumentId);

        verify(source).collect(instrument);
        verify(ingestion).ingestForInstrument(instrument, List.of(article));
        verify(instrumentRepository, org.mockito.Mockito.times(2)).findById(instrumentId);
        verifyNoMoreInteractions(source, ingestion);
    }
}
