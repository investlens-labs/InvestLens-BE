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
                당신은 금융 뉴스 분석기입니다. 아래 뉴스를 한국어로 번역·요약하고 허용된 종목에 대한 영향 가능성을 분석하세요.
                회사의 직접 이슈뿐 아니라 공급망, 주요 고객, 경쟁사, 산업, 규제, 금리·환율 등 거시 환경의 간접 영향도
                기사에 근거가 있을 때만 반영하세요. 기사에 없는 연관성은 추정하지 마세요.
                주가를 예측하거나 투자 조언을 하지 마세요. score는 1~10, direction은 POSITIVE/NEUTRAL/NEGATIVE 중 하나입니다.
                score 기준: 1~2 단순 언급·영향 거의 없음, 3~4 제한적 간접 영향, 5~6 의미 있는 영향,
                7~8 주요 사업·실적에 큰 영향, 9~10 전사적·즉각적으로 매우 중대한 영향입니다.
                upProbability, downProbability, neutralProbability는 이 기사에 대한 단기 시장 반응 가능성을 나타내는
                0~100 정수 퍼센트이며 반드시 합계가 100이어야 합니다. 근거가 약하면 neutralProbability를 가장 높게 설정하세요.
                반드시 JSON 객체만 반환하세요. 키: translatedTitle, translatedContent, summary, marketContext, impacts.
                impacts 원소 키: ticker, direction, score, reason, upProbability, downProbability, neutralProbability.
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
                if (score < 1 || score > 10) throw new IllegalArgumentException("AI impact score is out of range");
                int upProbability = requiredProbability(item, "upProbability");
                int downProbability = requiredProbability(item, "downProbability");
                int neutralProbability = requiredProbability(item, "neutralProbability");
                if (upProbability + downProbability + neutralProbability != 100) {
                    throw new IllegalArgumentException("AI probabilities must sum to 100");
                }
                impacts.add(new NewsAnalysisResult.Impact(ticker, direction, score, requiredText(item, "reason"),
                        upProbability, downProbability, neutralProbability));
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

    private static int requiredProbability(JsonNode node, String field) {
        if (!node.has(field) || !node.get(field).canConvertToInt()) {
            throw new IllegalArgumentException("Missing or invalid probability: " + field);
        }
        int value = node.get(field).asInt();
        if (value < 0 || value > 100) {
            throw new IllegalArgumentException("AI probability is out of range");
        }
        return value;
    }

    private static String limit(String value, int maxLength) {
        if (value == null) return "";
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
