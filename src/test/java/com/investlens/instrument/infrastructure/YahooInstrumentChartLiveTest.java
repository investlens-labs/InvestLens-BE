package com.investlens.instrument.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.investlens.instrument.application.ChartRange;
import com.investlens.instrument.domain.Instrument;
import com.investlens.instrument.domain.InstrumentMarket;
import com.investlens.instrument.domain.InstrumentType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.web.client.RestClient;

@EnabledIfEnvironmentVariable(named = "RUN_LIVE_CHART_TEST", matches = "true")
class YahooInstrumentChartLiveTest {
    @Test
    void fetchesUsAndKoreanChartData() {
        var properties = new MarketDataProperties(true, null, null);
        var client = new YahooInstrumentChartClient(properties, RestClient.builder(), new ObjectMapper());

        var apple = client.fetch(new Instrument("AAPL", "Apple", InstrumentType.STOCK, InstrumentMarket.US),
                ChartRange.ONE_MONTH);
        var samsung = client.fetch(new Instrument("005930", "삼성전자", InstrumentType.STOCK, InstrumentMarket.KR),
                ChartRange.ONE_MONTH);

        assertThat(apple.currency()).isEqualTo("USD");
        assertThat(apple.points()).isNotEmpty();
        assertThat(samsung.currency()).isEqualTo("KRW");
        assertThat(samsung.points()).isNotEmpty();
    }
}
