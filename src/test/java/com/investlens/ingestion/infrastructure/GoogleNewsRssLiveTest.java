package com.investlens.ingestion.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.investlens.instrument.domain.Instrument;
import com.investlens.instrument.domain.InstrumentMarket;
import com.investlens.instrument.domain.InstrumentType;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.web.client.RestClient;

@EnabledIfEnvironmentVariable(named = "RUN_LIVE_NEWS_TEST", matches = "true")
class GoogleNewsRssLiveTest {
    @Test
    void fetchesPastUnitedStatesAndKoreanInstrumentNews() throws Exception {
        var properties = new NewsSearchProperties(true, null, Duration.ofSeconds(15),
                Duration.ofMinutes(30), 90, 20);
        var client = new GoogleNewsRssClient(properties, new RssNewsParser(), RestClient.builder());

        var appleInstrument = new Instrument(
                "AAPL", "Apple Inc. - Common Stock", InstrumentType.STOCK, InstrumentMarket.US);
        var samsungInstrument = new Instrument(
                "005930", "삼성전자", InstrumentType.STOCK, InstrumentMarket.KR);
        String appleXml = client.fetchXml(appleInstrument);
        String samsungXml = client.fetchXml(samsungInstrument);
        assertThat(appleXml).contains("<item>");
        assertThat(samsungXml).contains("<item>");
        var apple = client.collectOrThrow(appleInstrument);
        var samsung = client.collectOrThrow(samsungInstrument);

        assertThat(apple).isNotEmpty().allMatch(article -> article.url().startsWith("https://news.google.com/"));
        assertThat(samsung).isNotEmpty().allMatch(article -> article.url().startsWith("https://news.google.com/"));
    }
}
