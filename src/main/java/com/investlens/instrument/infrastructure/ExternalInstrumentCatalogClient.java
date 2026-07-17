package com.investlens.instrument.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.investlens.instrument.application.InstrumentCatalogItem;
import com.investlens.instrument.application.InstrumentCatalogSourcePort;
import com.investlens.instrument.domain.InstrumentMarket;
import com.investlens.instrument.domain.InstrumentType;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.HtmlUtils;

@Component
public class ExternalInstrumentCatalogClient implements InstrumentCatalogSourcePort {
    private static final Charset EUC_KR = Charset.forName("EUC-KR");
    private static final int MAX_PAYLOAD_BYTES = 15_000_000;
    private static final Pattern TABLE_ROW = Pattern.compile("<tr[^>]*>(.*?)</tr>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern TABLE_CELL = Pattern.compile("<td[^>]*>(.*?)</td>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern HTML_TAG = Pattern.compile("<[^>]+>");

    private final InstrumentCatalogProperties properties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public ExternalInstrumentCatalogClient(InstrumentCatalogProperties properties, RestClient.Builder builder,
                                           ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        var requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.timeout());
        requestFactory.setReadTimeout(properties.timeout());
        this.restClient = builder.requestFactory(requestFactory).build();
    }

    @Override
    public List<InstrumentCatalogItem> fetchAll() {
        Map<String, InstrumentCatalogItem> instruments = new LinkedHashMap<>();
        var nasdaq = parseNasdaqDirectory(fetch(properties.nasdaqListedUrl(), StandardCharsets.UTF_8), true);
        var otherUs = parseNasdaqDirectory(fetch(properties.otherUsListedUrl(), StandardCharsets.UTF_8), false);
        var koreanStocks = parseKrxStocks(fetch(properties.krxListedUrl(), EUC_KR));
        var koreanEtfs = parseKoreanEtfs(fetch(properties.krEtfUrl(), EUC_KR), objectMapper);
        requireMinimum("Nasdaq", nasdaq, 3_000);
        requireMinimum("other US exchanges", otherUs, 3_000);
        requireMinimum("KRX stocks", koreanStocks, 1_500);
        requireMinimum("Korean ETFs", koreanEtfs, 500);
        nasdaq.forEach(item -> instruments.put(item.ticker(), item));
        otherUs.forEach(item -> instruments.put(item.ticker(), item));
        koreanStocks.forEach(item -> instruments.put(item.ticker(), item));
        koreanEtfs.forEach(item -> instruments.put(item.ticker(), item));
        return List.copyOf(instruments.values());
    }

    private static void requireMinimum(String source, List<InstrumentCatalogItem> items, int minimum) {
        if (items.size() < minimum) {
            throw new IllegalStateException(source + " catalog response is incomplete: " + items.size());
        }
    }

    private String fetch(String url, Charset charset) {
        return restClient.get().uri(url).header("User-Agent", "InvestLens/1.0").exchange((request, response) -> {
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new IllegalStateException("Instrument catalog returned " + response.getStatusCode());
            }
            byte[] bytes = response.getBody().readNBytes(MAX_PAYLOAD_BYTES + 1);
            if (bytes.length > MAX_PAYLOAD_BYTES) {
                throw new IllegalArgumentException("Instrument catalog payload is too large");
            }
            return new String(bytes, charset);
        });
    }

    static List<InstrumentCatalogItem> parseNasdaqDirectory(String body, boolean nasdaqListed) {
        String[] lines = body.lines().filter(line -> !line.isBlank()).toArray(String[]::new);
        if (lines.length < 2) return List.of();
        String[] headers = lines[0].split("\\|", -1);
        Map<String, Integer> indexes = indexByName(headers);
        String symbolHeader = nasdaqListed ? "Symbol" : "ACT Symbol";
        List<InstrumentCatalogItem> result = new ArrayList<>();
        for (int i = 1; i < lines.length; i++) {
            String[] values = lines[i].split("\\|", -1);
            if (values.length != headers.length || !"N".equals(value(values, indexes, "Test Issue"))) continue;
            String ticker = normalizeTicker(value(values, indexes, symbolHeader));
            String name = normalizeName(value(values, indexes, "Security Name"));
            if (ticker.isBlank() || name.isBlank()) continue;
            InstrumentType type = "Y".equals(value(values, indexes, "ETF")) ? InstrumentType.ETF : InstrumentType.STOCK;
            result.add(new InstrumentCatalogItem(ticker, name, type, InstrumentMarket.US));
        }
        return result;
    }

    static List<InstrumentCatalogItem> parseKrxStocks(String html) {
        List<InstrumentCatalogItem> result = new ArrayList<>();
        var rows = TABLE_ROW.matcher(html);
        while (rows.find()) {
            List<String> cells = new ArrayList<>();
            var cellMatcher = TABLE_CELL.matcher(rows.group(1));
            while (cellMatcher.find()) cells.add(cleanHtml(cellMatcher.group(1)));
            if (cells.size() < 3) continue;
            String ticker = normalizeTicker(cells.get(2));
            String name = normalizeName(cells.get(0));
            if (!ticker.matches("[0-9A-Z]{6}") || name.isBlank()) continue;
            result.add(new InstrumentCatalogItem(ticker, name, InstrumentType.STOCK, InstrumentMarket.KR));
        }
        return result;
    }

    static List<InstrumentCatalogItem> parseKoreanEtfs(String json, ObjectMapper objectMapper) {
        try {
            var items = objectMapper.readTree(json).path("result").path("etfItemList");
            List<InstrumentCatalogItem> result = new ArrayList<>();
            for (var item : items) {
                String ticker = normalizeTicker(item.path("itemcode").asText());
                String name = normalizeName(item.path("itemname").asText());
                if (ticker.matches("[0-9A-Z]{6}") && !name.isBlank()) {
                    result.add(new InstrumentCatalogItem(ticker, name, InstrumentType.ETF, InstrumentMarket.KR));
                }
            }
            return result;
        } catch (Exception e) {
            throw new IllegalArgumentException("Korean ETF catalog response is invalid", e);
        }
    }

    private static Map<String, Integer> indexByName(String[] headers) {
        Map<String, Integer> indexes = new LinkedHashMap<>();
        for (int i = 0; i < headers.length; i++) indexes.put(headers[i].strip(), i);
        return indexes;
    }

    private static String value(String[] values, Map<String, Integer> indexes, String key) {
        Integer index = indexes.get(key);
        return index == null || index >= values.length ? "" : values[index].strip();
    }

    private static String cleanHtml(String value) {
        return HtmlUtils.htmlUnescape(HTML_TAG.matcher(value).replaceAll(" ")).replaceAll("\\s+", " ").strip();
    }

    private static String normalizeTicker(String ticker) {
        String normalized = ticker.strip().toUpperCase(Locale.ROOT);
        return normalized.length() <= 16 ? normalized : "";
    }

    private static String normalizeName(String name) {
        String normalized = name.replaceAll("\\s+", " ").strip();
        return normalized.length() <= 200 ? normalized : normalized.substring(0, 200);
    }
}
