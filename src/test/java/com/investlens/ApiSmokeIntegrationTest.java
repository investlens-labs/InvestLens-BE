package com.investlens;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.investlens.instrument.domain.Instrument;
import com.investlens.instrument.domain.InstrumentType;
import com.investlens.instrument.domain.InstrumentMarket;
import com.investlens.instrument.infrastructure.InstrumentRepository;
import com.investlens.news.domain.ImpactDirection;
import com.investlens.news.domain.NewsArticle;
import com.investlens.news.domain.NewsImpact;
import com.investlens.news.infrastructure.NewsArticleRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ApiSmokeIntegrationTest {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired InstrumentRepository instrumentRepository;
    @Autowired NewsArticleRepository newsRepository;

    @Test
    void signupLoginPortfolioAndOpenApiContractsWork() throws Exception {
        String email = "smoke@investlens.test";
        String password = "secure-password";
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of("email", email, "password", password))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(email));

        String loginBody = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of("email", email, "password", password))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andReturn().getResponse().getContentAsString();
        String token = objectMapper.readTree(loginBody).path("accessToken").asText();

        Instrument instrument = instrumentRepository.findByTicker("NVDA").orElseThrow();
        mockMvc.perform(post("/api/v1/portfolio")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of("instrumentId", instrument.getId()))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ticker").value("NVDA"));
        Instrument qqq = instrumentRepository.findByTicker("QQQ").orElseThrow();
        mockMvc.perform(post("/api/v1/portfolio").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of("instrumentId", qqq.getId()))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/portfolio").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.ticker == 'NVDA')]").isNotEmpty())
                .andExpect(jsonPath("$[?(@.ticker == 'QQQ')]").isNotEmpty());

        String secondEmail = "second@investlens.test";
        mockMvc.perform(post("/api/v1/auth/signup").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of("email", secondEmail, "password", password))))
                .andExpect(status().isCreated());
        String secondLogin = mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of("email", secondEmail, "password", password))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String secondToken = objectMapper.readTree(secondLogin).path("accessToken").asText();
        Instrument apple = instrumentRepository.findByTicker("AAPL").orElseThrow();
        mockMvc.perform(post("/api/v1/portfolio").header("Authorization", "Bearer " + secondToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of("instrumentId", apple.getId()))))
                .andExpect(status().isCreated());
        mockMvc.perform(get("/api/v1/portfolio").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$[?(@.ticker == 'NVDA')]").isNotEmpty())
                .andExpect(jsonPath("$[?(@.ticker == 'QQQ')]").isNotEmpty())
                .andExpect(jsonPath("$[?(@.ticker == 'AAPL')]").isEmpty());

        NewsArticle article = new NewsArticle("Test", "https://example.test/shared", "NVIDIA and Apple news",
                "Original", Instant.now());
        article.relateTo(List.of(instrument, apple));
        article.completeAnalysis("번역", "번역 본문", "요약", "시장 맥락", "test-model",
                List.of(new NewsImpact(instrument, ImpactDirection.POSITIVE, 5, "수요 증가"),
                        new NewsImpact(apple, ImpactDirection.NEGATIVE, 2, "경쟁 심화")));
        newsRepository.saveAndFlush(article);
        mockMvc.perform(get("/api/v1/news").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].impacts.length()").value(1))
                .andExpect(jsonPath("$.content[0].impacts[0].ticker").value("NVDA"));
        mockMvc.perform(get("/api/v1/news/{id}", article.getId()).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.impacts.length()").value(1))
                .andExpect(jsonPath("$.impacts[0].ticker").value("NVDA"));

        NewsArticle privateArticle = new NewsArticle("Test", "https://example.test/private", "NVIDIA only",
                "Original", Instant.now());
        privateArticle.relateTo(List.of(instrument));
        privateArticle.completeAnalysis("번역", "본문", "요약", "맥락", "test-model",
                List.of(new NewsImpact(instrument, ImpactDirection.NEUTRAL, 1, "제한적")));
        newsRepository.saveAndFlush(privateArticle);
        mockMvc.perform(get("/api/v1/news/{id}", privateArticle.getId()).header("Authorization", "Bearer " + secondToken))
                .andExpect(status().isNotFound());

        NewsArticle mixedFilterArticle = new NewsArticle("Test", "https://example.test/mixed-filter",
                "NVIDIA and QQQ mixed impact", "Original", Instant.now());
        mixedFilterArticle.relateTo(List.of(instrument, qqq));
        mixedFilterArticle.completeAnalysis("번역", "본문", "요약", "맥락", "test-model",
                List.of(new NewsImpact(instrument, ImpactDirection.POSITIVE, 1, "낮은 긍정"),
                        new NewsImpact(qqq, ImpactDirection.NEGATIVE, 5, "높은 부정")));
        newsRepository.saveAndFlush(mixedFilterArticle);
        String compoundFilterFeed = mockMvc.perform(get("/api/v1/news").header("Authorization", "Bearer " + token)
                        .param("direction", "POSITIVE").param("minScore", "5"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        org.assertj.core.api.Assertions.assertThat(compoundFilterFeed)
                .doesNotContain(mixedFilterArticle.getId().toString());

        NewsArticle failedArticle = new NewsArticle("Test", "https://example.test/failed", "NVIDIA pending analysis",
                "Original", Instant.now());
        failedArticle.relateTo(List.of(instrument));
        failedArticle.failAnalysis("temporary model outage");
        newsRepository.saveAndFlush(failedArticle);
        String feedBody = mockMvc.perform(get("/api/v1/news").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        org.assertj.core.api.Assertions.assertThat(feedBody).contains(failedArticle.getId().toString(), "FAILED");

        mockMvc.perform(get("/api/v1/instruments").header("Authorization", "Bearer " + token)
                        .param("query", "NVIDIA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ticker").value("NVDA"))
                .andExpect(jsonPath("$[0].market").value("US"));
        instrumentRepository.saveAndFlush(new Instrument("005930", "삼성전자", InstrumentType.STOCK, InstrumentMarket.KR));
        mockMvc.perform(get("/api/v1/instruments").header("Authorization", "Bearer " + token)
                        .param("query", "삼성").param("market", "KR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ticker").value("005930"))
                .andExpect(jsonPath("$[0].market").value("KR"));
        mockMvc.perform(get("/api/v1/instruments").header("Authorization", "Bearer " + token)
                        .param("limit", "101"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/instruments/{instrumentId}/chart", instrument.getId())
                        .header("Authorization", "Bearer " + token).param("range", "10Y"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/news").header("Authorization", "Bearer " + token).param("minScore", "0"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/news").header("Authorization", "Bearer " + token).param("direction", "UNKNOWN"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/news")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/auth/signup']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/news']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/instruments/{instrumentId}/chart']").exists())
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth").exists());
        mockMvc.perform(get("/swagger-ui.html")).andExpect(status().is3xxRedirection());
    }
}
