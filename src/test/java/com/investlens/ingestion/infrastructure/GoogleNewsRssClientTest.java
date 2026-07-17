package com.investlens.ingestion.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.investlens.instrument.domain.Instrument;
import com.investlens.instrument.domain.InstrumentMarket;
import com.investlens.instrument.domain.InstrumentType;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class GoogleNewsRssClientTest {
    @Test
    void buildsLocalizedKoreanSearchUrlWithLookback() {
        var client = client();

        String url = client.buildUrl(new Instrument(
                "005930", "삼성전자", InstrumentType.STOCK, InstrumentMarket.KR));

        assertThat(url)
                .contains("news.google.com/rss/search")
                .contains("%EC%82%BC%EC%84%B1%EC%A0%84%EC%9E%90")
                .contains("when:90d", "hl=ko", "gl=KR", "ceid=KR:ko");
    }

    @Test
    void removesNasdaqDirectorySuffixFromUnitedStatesCompanyName() {
        var client = client();

        String url = client.buildUrl(new Instrument(
                "AAPL", "Apple Inc. - Common Stock", InstrumentType.STOCK, InstrumentMarket.US));

        assertThat(url).contains("Apple%20Inc.", "AAPL", "when:90d", "hl=en-US")
                .doesNotContain("Common%20Stock");
    }

    private GoogleNewsRssClient client() {
        var properties = new NewsSearchProperties(true, null, Duration.ofSeconds(1),
                Duration.ofMinutes(30), 90, 20);
        return new GoogleNewsRssClient(properties, new RssNewsParser(), RestClient.builder());
    }
}
