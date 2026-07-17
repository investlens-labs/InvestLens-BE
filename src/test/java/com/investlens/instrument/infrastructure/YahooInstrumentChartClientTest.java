package com.investlens.instrument.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class YahooInstrumentChartClientTest {
    @Test
    void parsesMetadataAndSkipsPointsWithoutClosingPrice() {
        String body = """
                {
                  "chart": {
                    "result": [{
                      "meta": {
                        "currency": "USD",
                        "exchangeTimezoneName": "America/New_York",
                        "regularMarketPrice": 210.5,
                        "chartPreviousClose": 205.0,
                        "exchangeDataDelayedBy": 0
                      },
                      "timestamp": [100, 200, 300],
                      "indicators": {"quote": [{
                        "open": [200.0, null, 208.0],
                        "high": [206.0, null, 212.0],
                        "low": [199.0, null, 207.0],
                        "close": [205.0, null, 210.5],
                        "volume": [1000, null, 2000]
                      }]}
                    }],
                    "error": null
                  }
                }
                """;
        var properties = new MarketDataProperties(true, null, null);
        var client = new YahooInstrumentChartClient(properties, RestClient.builder(), new ObjectMapper());

        var result = client.parse(body);

        assertThat(result.currency()).isEqualTo("USD");
        assertThat(result.currentPrice()).isEqualTo(210.5);
        assertThat(result.previousClose()).isEqualTo(205.0);
        assertThat(result.points()).hasSize(2);
        assertThat(result.points().get(1).timestamp()).isEqualTo(300);
        assertThat(result.points().get(1).volume()).isEqualTo(2000);
    }
}
