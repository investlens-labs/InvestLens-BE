package com.investlens.instrument.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.investlens.common.error.BusinessException;
import com.investlens.common.error.ErrorCode;
import com.investlens.instrument.application.ChartPoint;
import com.investlens.instrument.application.ChartRange;
import com.investlens.instrument.application.InstrumentChartData;
import com.investlens.instrument.application.InstrumentChartSourcePort;
import com.investlens.instrument.domain.Instrument;
import com.investlens.instrument.domain.InstrumentMarket;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class YahooInstrumentChartClient implements InstrumentChartSourcePort {
    private static final int MAX_PAYLOAD_BYTES = 5_000_000;
    private final MarketDataProperties properties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public YahooInstrumentChartClient(MarketDataProperties properties, RestClient.Builder builder,
                                      ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        var requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.timeout());
        requestFactory.setReadTimeout(properties.timeout());
        this.restClient = builder.baseUrl(properties.baseUrl()).requestFactory(requestFactory).build();
    }

    @Override
    public InstrumentChartData fetch(Instrument instrument, ChartRange range) {
        if (!properties.enabled()) {
            throw new BusinessException(ErrorCode.EXTERNAL_SERVICE_ERROR, "시세 데이터 연동이 비활성화되어 있습니다.");
        }
        if (instrument.getMarket() == InstrumentMarket.US) {
            return fetchSymbol(instrument.getTicker().replace('.', '-'), range);
        }
        for (String suffix : List.of(".KS", ".KQ")) {
            InstrumentChartData data = fetchSymbolOrNull(instrument.getTicker() + suffix, range);
            if (data != null) return data;
        }
        throw new BusinessException(ErrorCode.EXTERNAL_SERVICE_ERROR, "해당 종목의 시세 데이터를 찾을 수 없습니다.");
    }

    private InstrumentChartData fetchSymbol(String symbol, ChartRange range) {
        InstrumentChartData data = fetchSymbolOrNull(symbol, range);
        if (data == null) {
            throw new BusinessException(ErrorCode.EXTERNAL_SERVICE_ERROR, "해당 종목의 시세 데이터를 찾을 수 없습니다.");
        }
        return data;
    }

    private InstrumentChartData fetchSymbolOrNull(String symbol, ChartRange range) {
        String body = restClient.get()
                .uri(uriBuilder -> uriBuilder.path("/v8/finance/chart/{symbol}")
                        .queryParam("range", range.providerRange())
                        .queryParam("interval", range.interval())
                        .queryParam("events", "div,splits")
                        .build(symbol))
                .header("User-Agent", "Mozilla/5.0 InvestLens/1.0")
                .exchange((request, response) -> {
                    if (response.getStatusCode().value() == 404 || response.getStatusCode().value() == 422) return null;
                    if (!response.getStatusCode().is2xxSuccessful()) {
                        throw new IllegalStateException("Market data returned " + response.getStatusCode());
                    }
                    byte[] bytes = response.getBody().readNBytes(MAX_PAYLOAD_BYTES + 1);
                    if (bytes.length > MAX_PAYLOAD_BYTES) throw new IllegalArgumentException("Market data payload is too large");
                    return new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
                });
        return body == null ? null : parse(body);
    }

    InstrumentChartData parse(String body) {
        try {
            JsonNode chart = objectMapper.readTree(body).path("chart");
            if (!chart.path("error").isNull() && !chart.path("error").isMissingNode()) return null;
            JsonNode result = chart.path("result").path(0);
            if (result.isMissingNode()) return null;
            JsonNode meta = result.path("meta");
            JsonNode timestamps = result.path("timestamp");
            JsonNode quote = result.path("indicators").path("quote").path(0);
            List<ChartPoint> points = new ArrayList<>();
            for (int i = 0; i < timestamps.size(); i++) {
                Double close = nullableDouble(quote.path("close").path(i));
                if (close == null) continue;
                points.add(new ChartPoint(
                        timestamps.path(i).asLong(),
                        valueOr(quote.path("open").path(i), close),
                        valueOr(quote.path("high").path(i), close),
                        valueOr(quote.path("low").path(i), close),
                        close,
                        quote.path("volume").path(i).asLong(0)
                ));
            }
            if (points.isEmpty()) return null;
            double currentPrice = meta.path("regularMarketPrice").asDouble(points.get(points.size() - 1).close());
            double previousClose = meta.path("chartPreviousClose").asDouble(currentPrice);
            return new InstrumentChartData(
                    meta.path("currency").asText(""),
                    meta.path("exchangeTimezoneName").asText("UTC"),
                    currentPrice,
                    previousClose,
                    meta.path("exchangeDataDelayedBy").asInt(0),
                    points
            );
        } catch (Exception e) {
            throw new IllegalArgumentException("Market data response is invalid", e);
        }
    }

    private static Double nullableDouble(JsonNode node) {
        return node.isNumber() ? node.asDouble() : null;
    }

    private static double valueOr(JsonNode node, double fallback) {
        Double value = nullableDouble(node);
        return value == null ? fallback : value;
    }
}
