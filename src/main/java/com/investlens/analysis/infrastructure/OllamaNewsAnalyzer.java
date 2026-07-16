package com.investlens.analysis.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.investlens.analysis.application.NewsAnalysisResult;
import com.investlens.analysis.application.NewsAnalyzerPort;
import com.investlens.ingestion.application.CollectedNews;
import com.investlens.news.domain.ImpactDirection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.io.IOException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@ConditionalOnProperty(prefix = "app.ai", name = "enabled", havingValue = "true")
public class OllamaNewsAnalyzer implements NewsAnalyzerPort {
    private static final int MAX_RESPONSE_BYTES = 1_000_000;
    private final AiProperties properties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public OllamaNewsAnalyzer(AiProperties properties, RestClient.Builder builder, ObjectMapper objectMapper) {
        this.properties = properties;
        var requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.timeout());
        requestFactory.setReadTimeout(properties.timeout());
        this.restClient = builder.requestFactory(requestFactory).baseUrl(properties.baseUrl()).build();
        this.objectMapper = objectMapper;
    }

    @Override
    public NewsAnalysisResult analyze(CollectedNews news, Set<String> allowedTickers) {
        String prompt = """
                당신은 금융 뉴스 분석기입니다. 아래 뉴스를 한국어로 번역·요약하고 허용된 종목에 대한 영향 가능성만 분석하세요.
                주가를 예측하거나 투자 조언을 하지 마세요. score는 1~5, direction은 POSITIVE/NEUTRAL/NEGATIVE 중 하나입니다.
                반드시 JSON 객체만 반환하세요. 키: translatedTitle, translatedContent, summary, marketContext, impacts.
                impacts 원소 키: ticker, direction, score, reason.
                허용 티커: %s
                제목: %s
                내용: %s
                """.formatted(allowedTickers, limit(news.title(), 700), limit(news.content(), 20_000));
        Map<String, Object> request = Map.of(
                "model", properties.model(),
                "stream", false,
                "format", "json",
                "prompt", prompt
        );
        JsonNode response = restClient.post().uri("/api/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .exchange((httpRequest, httpResponse) -> {
                    if (!httpResponse.getStatusCode().is2xxSuccessful()) {
                        throw new IllegalStateException("Ollama returned " + httpResponse.getStatusCode());
                    }
                    byte[] bytes = httpResponse.getBody().readNBytes(MAX_RESPONSE_BYTES + 1);
                    if (bytes.length > MAX_RESPONSE_BYTES) throw new IllegalArgumentException("Ollama response is too large");
                    try {
                        return objectMapper.readTree(bytes);
                    } catch (IOException e) {
                        throw new IllegalArgumentException("Ollama response is not JSON", e);
                    }
                });
        try {
            JsonNode result = objectMapper.readTree(requiredText(response, "response"));
            List<NewsAnalysisResult.Impact> impacts = new ArrayList<>();
            for (JsonNode item : result.path("impacts")) {
                String ticker = requiredText(item, "ticker").strip().toUpperCase();
                if (!allowedTickers.contains(ticker)) continue;
                ImpactDirection direction = ImpactDirection.valueOf(requiredText(item, "direction").toUpperCase());
                int score = item.path("score").asInt();
                if (score < 1 || score > 5) throw new IllegalArgumentException("AI impact score is out of range");
                impacts.add(new NewsAnalysisResult.Impact(ticker, direction, score, requiredText(item, "reason")));
            }
            if (impacts.isEmpty()) throw new IllegalArgumentException("AI returned no valid impact");
            return new NewsAnalysisResult(requiredText(result, "translatedTitle"),
                    optionalText(result, "translatedContent", news.content()), requiredText(result, "summary"),
                    requiredText(result, "marketContext"), properties.model(), List.copyOf(impacts));
        } catch (RuntimeException | com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalStateException("Invalid Ollama analysis response", e);
        }
    }

    private static String requiredText(JsonNode node, String field) {
        if (node == null || node.path(field).asText().isBlank()) throw new IllegalArgumentException("Missing AI field: " + field);
        return node.path(field).asText();
    }

    private static String optionalText(JsonNode node, String field, String fallback) {
        String value = node.path(field).asText();
        return value.isBlank() ? fallback : value;
    }

    private static String limit(String value, int maxLength) {
        if (value == null) return "";
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
