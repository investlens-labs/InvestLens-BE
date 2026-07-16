package com.investlens.ingestion.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.investlens.instrument.domain.Instrument;
import com.investlens.instrument.domain.InstrumentType;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;

class NewsIngestionServiceTest {
    @Test
    void tickerMatchingUsesTokenBoundaries() {
        var voo = new Instrument("VOO", "Vanguard S&P 500 ETF", InstrumentType.ETF);
        var instruments = Map.of("VOO", voo);

        assertThat(NewsIngestionService.matchTickers(
                new CollectedNews("test", "url", "Voodoo economics", "unrelated", Instant.now()), instruments))
                .isEmpty();
        assertThat(NewsIngestionService.matchTickers(
                new CollectedNews("test", "url", "VOO sees inflows", "ETF demand", Instant.now()), instruments))
                .containsExactly("VOO");
    }
}
