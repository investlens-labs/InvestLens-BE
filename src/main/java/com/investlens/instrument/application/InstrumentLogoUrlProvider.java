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

        boolean koreanMarket = instrument.getMarket() == InstrumentMarket.KR;
        String lookupType = koreanMarket ? "name" : "ticker";
        String lookupValue = koreanMarket ? instrument.getCompanyName() : instrument.getTicker();
        String encodedValue = URLEncoder.encode(lookupValue, StandardCharsets.UTF_8).replace("+", "%20");

        return UriComponentsBuilder.fromUriString(properties.baseUrl())
                .pathSegment(lookupType, encodedValue)
                .queryParam("token", properties.publishableKey())
                .queryParam("size", properties.size())
                .queryParam("format", "png")
                .queryParam("retina", true)
                .queryParam("fallback", "monogram")
                .build(true)
                .toUriString();
    }
}
