package com.investlens.instrument.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.investlens.instrument.domain.InstrumentMarket;
import com.investlens.instrument.domain.InstrumentType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.web.client.RestClient;

@EnabledIfEnvironmentVariable(named = "RUN_LIVE_CATALOG_TEST", matches = "true")
class ExternalInstrumentCatalogLiveTest {
    @Test
    void fetchesKoreanAndUsStocksAndEtfsFromExternalSources() {
        var properties = new InstrumentCatalogProperties(true, null, null, null, null, null, null);
        var client = new ExternalInstrumentCatalogClient(properties, RestClient.builder(), new ObjectMapper());

        var instruments = client.fetchAll();

        assertThat(instruments.stream().filter(item -> item.market() == InstrumentMarket.US).count())
                .isGreaterThan(5_000);
        assertThat(instruments.stream().filter(item -> item.market() == InstrumentMarket.KR).count())
                .isGreaterThan(2_000);
        assertThat(instruments).anyMatch(item -> item.market() == InstrumentMarket.KR
                && item.type() == InstrumentType.STOCK && item.ticker().equals("005930"));
        assertThat(instruments).anyMatch(item -> item.market() == InstrumentMarket.KR
                && item.type() == InstrumentType.ETF && item.ticker().equals("069500"));
        assertThat(instruments).anyMatch(item -> item.market() == InstrumentMarket.US
                && item.type() == InstrumentType.STOCK && item.ticker().equals("AAPL"));
        assertThat(instruments).anyMatch(item -> item.market() == InstrumentMarket.US
                && item.type() == InstrumentType.ETF && item.ticker().equals("QQQ"));
    }
}
