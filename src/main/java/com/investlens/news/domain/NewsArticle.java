package com.investlens.news.domain;

import com.investlens.common.domain.BaseTimeEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Entity
@Table(name = "news_articles")
public class NewsArticle extends BaseTimeEntity {
    @Id
    private UUID id;
    @Column(nullable = false, length = 100)
    private String source;
    @Column(nullable = false, columnDefinition = "text")
    private String canonicalUrl;
    @Column(nullable = false, unique = true, length = 64)
    private String canonicalUrlHash;
    @Column(nullable = false, length = 700)
    private String title;
    @Column(columnDefinition = "text")
    private String originalContent;
    @Column(length = 700)
    private String translatedTitle;
    @Column(columnDefinition = "text")
    private String translatedContent;
    @Column(columnDefinition = "text")
    private String summary;
    @Column(columnDefinition = "text")
    private String marketContext;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AnalysisStatus analysisStatus;
    @Column(length = 100)
    private String modelName;
    @Column(nullable = false)
    private Instant publishedAt;
    @Column(length = 1000)
    private String analysisError;

    @OneToMany(mappedBy = "news", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("score DESC")
    private List<NewsImpact> impacts = new ArrayList<>();

    @OneToMany(mappedBy = "news", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<NewsRelatedInstrument> relatedInstruments = new ArrayList<>();

    protected NewsArticle() {}

    public NewsArticle(String source, String canonicalUrl, String title, String originalContent, Instant publishedAt) {
        this.id = UUID.randomUUID();
        this.source = requireText(source, "source");
        this.canonicalUrl = requireText(canonicalUrl, "canonicalUrl");
        this.canonicalUrlHash = sha256(this.canonicalUrl);
        this.title = requireText(title, "title");
        this.originalContent = originalContent;
        this.publishedAt = publishedAt == null ? Instant.now() : publishedAt;
        this.analysisStatus = AnalysisStatus.PENDING;
    }

    public void completeAnalysis(String translatedTitle, String translatedContent, String summary,
                                 String marketContext, String modelName, List<NewsImpact> newImpacts) {
        this.translatedTitle = requireText(translatedTitle, "translatedTitle");
        this.translatedContent = translatedContent;
        this.summary = requireText(summary, "summary");
        this.marketContext = requireText(marketContext, "marketContext");
        this.modelName = requireText(modelName, "modelName");
        this.impacts.clear();
        newImpacts.forEach(impact -> {
            impact.attachTo(this);
            this.impacts.add(impact);
        });
        this.analysisStatus = AnalysisStatus.COMPLETED;
        this.analysisError = null;
    }

    public void relateTo(List<com.investlens.instrument.domain.Instrument> instruments) {
        this.relatedInstruments.clear();
        instruments.forEach(instrument -> this.relatedInstruments.add(new NewsRelatedInstrument(this, instrument)));
    }

    public void failAnalysis(String error) {
        this.analysisStatus = AnalysisStatus.FAILED;
        this.analysisError = error == null ? "Unknown analysis error" : error.substring(0, Math.min(error.length(), 1000));
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " must not be blank");
        return value.strip();
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }

    public UUID getId() { return id; }
    public String getSource() { return source; }
    public String getCanonicalUrl() { return canonicalUrl; }
    public String getTitle() { return title; }
    public String getOriginalContent() { return originalContent; }
    public String getTranslatedTitle() { return translatedTitle; }
    public String getTranslatedContent() { return translatedContent; }
    public String getSummary() { return summary; }
    public String getMarketContext() { return marketContext; }
    public AnalysisStatus getAnalysisStatus() { return analysisStatus; }
    public String getModelName() { return modelName; }
    public Instant getPublishedAt() { return publishedAt; }
    public String getAnalysisError() { return analysisError; }
    public List<NewsImpact> getImpacts() { return List.copyOf(impacts); }
    public List<NewsRelatedInstrument> getRelatedInstruments() { return List.copyOf(relatedInstruments); }
}
