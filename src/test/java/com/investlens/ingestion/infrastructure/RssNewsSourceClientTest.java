package com.investlens.ingestion.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.springframework.web.client.RestClient;

class RssNewsSourceClientTest {
    @Test
    void parsesRssAndRemovesHtmlFromDescription() throws Exception {
        String rss = """
                <?xml version="1.0"?><rss version="2.0"><channel><item>
                <title>NVIDIA announces new GPU</title><link>https://example.com/news/1</link>
                <description><![CDATA[<b>NVIDIA</b> launched a product.]]></description>
                <pubDate>Wed, 15 Jul 2026 12:00:00 GMT</pubDate>
                </item></channel></rss>
                """;
        var properties = new RssFeedProperties(false, null, Duration.ofSeconds(1), List.of());
        var client = new RssNewsSourceClient(properties, RestClient.builder());

        var result = client.parse("Example", rss);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).title()).isEqualTo("NVIDIA announces new GPU");
        assertThat(result.get(0).content()).isEqualTo("NVIDIA launched a product.");
    }

    @Test
    void rejectsDoctypeAndExternalEntities() {
        String malicious = """
                <?xml version="1.0"?>
                <!DOCTYPE rss [<!ENTITY xxe SYSTEM "file:///etc/passwd">]>
                <rss version="2.0"><channel><item><title>&xxe;</title>
                <link>https://example.com</link></item></channel></rss>
                """;
        var client = new RssNewsSourceClient(
                new RssFeedProperties(false, null, Duration.ofSeconds(1), List.of()), RestClient.builder());

        assertThatThrownBy(() -> client.parse("malicious", malicious)).isInstanceOf(Exception.class);
    }
}
