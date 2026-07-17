package com.investlens.ingestion.infrastructure;

import com.investlens.ingestion.application.CollectedNews;
import java.io.StringReader;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.springframework.stereotype.Component;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

@Component
public class RssNewsParser {
    public List<CollectedNews> parse(String source, String xml) throws Exception {
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
            String publisher = text(item, "source");
            String itemSource = publisher.isBlank() ? source : source + " · " + publisher;
            String description = limit(stripHtml(text(item, "description")), 20_000);
            result.add(new CollectedNews(limit(itemSource, 100), link, title, description,
                    parseDate(text(item, "pubDate"))));
        }
        return List.copyOf(result);
    }

    private static String text(Element element, String tag) {
        var nodes = element.getElementsByTagName(tag);
        return nodes.getLength() == 0 ? "" : nodes.item(0).getTextContent().strip();
    }

    private static Instant parseDate(String value) {
        try {
            return ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant();
        } catch (Exception ignored) {
            return Instant.now();
        }
    }

    private static String stripHtml(String value) {
        return value.replaceAll("<[^>]+>", " ").replaceAll("\\s+", " ").strip();
    }

    private static String limit(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
