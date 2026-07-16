package com.investlens.ingestion.infrastructure;

import com.investlens.ingestion.application.CollectedNews;
import com.investlens.ingestion.application.NewsSourcePort;
import java.io.StringReader;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.nio.charset.StandardCharsets;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

@Component
public class RssNewsSourceClient implements NewsSourcePort {
    private static final Logger log = LoggerFactory.getLogger(RssNewsSourceClient.class);
    private static final int MAX_RSS_BYTES = 2_000_000;
    private final RssFeedProperties properties;
    private final RestClient restClient;

    public RssNewsSourceClient(RssFeedProperties properties, RestClient.Builder builder) {
        this.properties = properties;
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
        var factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        var document = factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
        var items = document.getElementsByTagName("item");
        List<CollectedNews> result = new ArrayList<>();
        for (int i = 0; i < items.getLength(); i++) {
            Element item = (Element) items.item(i);
            String title = limit(text(item, "title"), 700);
            String link = text(item, "link");
            if (title.isBlank() || link.isBlank()) continue;
            String description = limit(stripHtml(text(item, "description")), 20_000);
            result.add(new CollectedNews(limit(source, 100), link, title, description, parseDate(text(item, "pubDate"))));
        }
        return result;
    }

    private static String text(Element element, String tag) {
        var nodes = element.getElementsByTagName(tag);
        return nodes.getLength() == 0 ? "" : nodes.item(0).getTextContent().strip();
    }

    private static Instant parseDate(String value) {
        try { return ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant(); }
        catch (Exception ignored) { return Instant.now(); }
    }

    private static String stripHtml(String value) {
        return value.replaceAll("<[^>]+>", " ").replaceAll("\\s+", " ").strip();
    }

    private static String limit(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
