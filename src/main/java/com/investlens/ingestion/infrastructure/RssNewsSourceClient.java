package com.investlens.ingestion.infrastructure;

import com.investlens.ingestion.application.CollectedNews;
import com.investlens.ingestion.application.NewsSourcePort;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class RssNewsSourceClient implements NewsSourcePort {
    private static final Logger log = LoggerFactory.getLogger(RssNewsSourceClient.class);
    private static final int MAX_RSS_BYTES = 2_000_000;
    private final RssFeedProperties properties;
    private final RssNewsParser parser;
    private final RestClient restClient;

    public RssNewsSourceClient(RssFeedProperties properties, RestClient.Builder builder, RssNewsParser parser) {
        this.properties = properties;
        this.parser = parser;
        var requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.timeout());
        requestFactory.setReadTimeout(properties.timeout());
        this.restClient = builder.requestFactory(requestFactory).build();
    }

    @Override
    public List<CollectedNews> collect() {
        List<CollectedNews> collected = new ArrayList<>();
        for (RssFeedProperties.Feed feed : properties.feeds()) {
            try {
                String xml = restClient.get().uri(feed.url()).exchange((request, response) -> {
                    if (!response.getStatusCode().is2xxSuccessful()) {
                        throw new IllegalStateException("RSS returned " + response.getStatusCode());
                    }
                    byte[] bytes = response.getBody().readNBytes(MAX_RSS_BYTES + 1);
                    if (bytes.length > MAX_RSS_BYTES) throw new IllegalArgumentException("RSS payload is too large");
                    return new String(bytes, StandardCharsets.UTF_8);
                });
                if (xml == null || xml.isBlank()) throw new IllegalArgumentException("RSS payload is empty");
                collected.addAll(parse(feed.name(), xml));
            } catch (Exception e) {
                log.warn("RSS collection failed for {}: {}", feed.name(), e.getMessage());
            }
        }
        return List.copyOf(collected);
    }

    List<CollectedNews> parse(String source, String xml) throws Exception {
        return parser.parse(source, xml);
    }
}
