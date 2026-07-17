package com.investlens.ingestion.infrastructure;

import com.investlens.ingestion.application.CollectedNews;
import com.investlens.ingestion.application.InstrumentNewsSourcePort;
import com.investlens.instrument.domain.Instrument;
import com.investlens.instrument.domain.InstrumentMarket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class GoogleNewsRssClient implements InstrumentNewsSourcePort {
    private static final Logger log = LoggerFactory.getLogger(GoogleNewsRssClient.class);
    private static final int MAX_RSS_BYTES = 2_000_000;

    private final NewsSearchProperties properties;
    private final RssNewsParser parser;
    private final RestClient restClient;

    public GoogleNewsRssClient(NewsSearchProperties properties, RssNewsParser parser, RestClient.Builder builder) {
        this.properties = properties;
        this.parser = parser;
        var requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.timeout());
        requestFactory.setReadTimeout(properties.timeout());
        this.restClient = builder.requestFactory(requestFactory)
                .defaultHeader("User-Agent", "InvestLens/0.1")
                .build();
    }

    @Override
    public List<CollectedNews> collect(Instrument instrument) {
        if (!properties.enabled()) return List.of();
        try {
            return collectOrThrow(instrument);
        } catch (Exception e) {
            log.warn("Instrument news search failed for {}: {}", instrument.getTicker(), e.getMessage());
            return List.of();
        }
    }

    List<CollectedNews> collectOrThrow(Instrument instrument) throws Exception {
        String xml = fetchXml(instrument);
        if (xml == null || xml.isBlank()) return List.of();
        return parser.parse("Google News", xml).stream().limit(properties.maxResults()).toList();
    }

    String fetchXml(Instrument instrument) {
        return restClient.get().uri(URI.create(buildUrl(instrument))).accept(MediaType.APPLICATION_XML)
                .exchange((request, response) -> {
                    if (!response.getStatusCode().is2xxSuccessful()) {
                        throw new IllegalStateException("Google News RSS returned " + response.getStatusCode());
                    }
                    byte[] bytes = response.getBody().readNBytes(MAX_RSS_BYTES + 1);
                    if (bytes.length > MAX_RSS_BYTES) {
                        throw new IllegalArgumentException("Google News RSS payload is too large");
                    }
                    return new String(bytes, StandardCharsets.UTF_8);
                });
    }

    String buildUrl(Instrument instrument) {
        boolean korean = instrument.getMarket() == InstrumentMarket.KR;
        String query = korean
                ? "\"" + instrument.getCompanyName() + "\" 주식 when:" + properties.lookbackDays() + "d"
                : "(\"" + cleanUsName(instrument.getCompanyName()) + "\" OR " + instrument.getTicker()
                        + ") stock when:" + properties.lookbackDays() + "d";
        String language = korean ? "ko" : "en";
        String country = korean ? "KR" : "US";
        String hostLanguage = korean ? "ko" : "en-US";
        return UriComponentsBuilder.fromUriString(properties.baseUrl())
                .queryParam("q", query)
                .queryParam("hl", hostLanguage)
                .queryParam("gl", country)
                .queryParam("ceid", country + ":" + language)
                .build()
                .encode(StandardCharsets.UTF_8)
                .toUriString();
    }

    private static String cleanUsName(String companyName) {
        return companyName.replaceFirst("\\s+-\\s+.*$", "").strip();
    }
}
