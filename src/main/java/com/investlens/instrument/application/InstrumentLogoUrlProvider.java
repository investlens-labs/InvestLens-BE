package com.investlens.instrument.application;

import com.investlens.instrument.domain.Instrument;
import com.investlens.instrument.domain.InstrumentMarket;
import com.investlens.instrument.infrastructure.InstrumentLogoProperties;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class InstrumentLogoUrlProvider {
    public static final String ATTRIBUTION_URL = "https://logo.dev";

    private final InstrumentLogoProperties properties;

    public InstrumentLogoUrlProvider(InstrumentLogoProperties properties) {
        this.properties = properties;
    }

    public String get(Instrument instrument) {
        if (!properties.enabled() || properties.publishableKey().isBlank()) return null;

        String symbol = instrument.getMarket() == InstrumentMarket.KR
                ? instrument.getTicker() + ".KQ"
                : instrument.getTicker();
        String encodedSymbol = URLEncoder.encode(symbol, StandardCharsets.UTF_8).replace("+", "%20");

        return UriComponentsBuilder.fromUriString(properties.baseUrl())
                .pathSegment("ticker", encodedSymbol)
                .queryParam("token", properties.publishableKey())
                .queryParam("size", properties.size())
                .queryParam("format", "png")
                .queryParam("retina", true)
                .queryParam("fallback", "monogram")
                .build(true)
                .toUriString();
    }
}
