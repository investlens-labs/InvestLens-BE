package com.investlens.instrument.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.investlens.instrument.domain.Instrument;
import com.investlens.instrument.domain.InstrumentMarket;
import com.investlens.instrument.domain.InstrumentType;
import com.investlens.instrument.infrastructure.InstrumentLogoProperties;
import org.junit.jupiter.api.Test;

class InstrumentLogoUrlProviderTest {
    @Test
    void createsUnitedStatesTickerLogoUrl() {
        var provider = new InstrumentLogoUrlProvider(properties(true, "pk_test"));

        String logoUrl = provider.get(new Instrument(
                "AAPL", "Apple Inc.", InstrumentType.STOCK, InstrumentMarket.US));

        assertThat(logoUrl)
                .startsWith("https://img.logo.dev/ticker/AAPL?")
                .contains("token=pk_test", "size=64", "format=png", "retina=true", "fallback=monogram");
    }

    @Test
    void addsKoreanExchangeSuffix() {
        var provider = new InstrumentLogoUrlProvider(properties(true, "pk_test"));

        String logoUrl = provider.get(new Instrument(
                "005930", "삼성전자", InstrumentType.STOCK, InstrumentMarket.KR));

        assertThat(logoUrl).startsWith("https://img.logo.dev/ticker/005930.KQ?");
    }

    @Test
    void returnsNullWhenPublishableKeyIsMissing() {
        var provider = new InstrumentLogoUrlProvider(properties(true, ""));

        String logoUrl = provider.get(new Instrument(
                "AAPL", "Apple Inc.", InstrumentType.STOCK, InstrumentMarket.US));

        assertThat(logoUrl).isNull();
    }

    private InstrumentLogoProperties properties(boolean enabled, String key) {
        return new InstrumentLogoProperties(enabled, "https://img.logo.dev", key, 64);
    }
}
