package com.investlens.news.domain;

import com.investlens.common.domain.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;

@Entity
@Table(name = "news_translations",
        uniqueConstraints = @UniqueConstraint(name = "uk_news_translation_language",
                columnNames = {"news_id", "language"}))
public class NewsTranslation extends BaseTimeEntity {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "news_id", nullable = false)
    private NewsArticle news;

    @Column(nullable = false, length = 10)
    private String language;

    @Column(name = "translated_title", nullable = false, length = 700)
    private String translatedTitle;

    @Column(nullable = false, columnDefinition = "text")
    private String summary;

    @Enumerated(EnumType.STRING)
    @Column(name = "impact_direction", nullable = false, length = 20)
    private ImpactDirection impactDirection;

    @Column(name = "impact_score", nullable = false)
    private int impactScore;

    @Column(name = "impact_reason", nullable = false, columnDefinition = "text")
    private String impactReason;

    @Column(name = "model_name", nullable = false, length = 100)
    private String modelName;

    protected NewsTranslation() {}

    public NewsTranslation(NewsArticle news, NewsLanguage language, String translatedTitle,
                           String summary, ImpactDirection impactDirection, int impactScore,
                           String impactReason, String modelName) {
        this.id = UUID.randomUUID();
        this.news = news;
        this.language = language.code();
        this.translatedTitle = requireText(translatedTitle, "translatedTitle", 700);
        this.summary = requireText(summary, "summary", 2000);
        if (impactDirection == null) throw new IllegalArgumentException("impactDirection must not be null");
        if (impactScore < 1 || impactScore > 5) throw new IllegalArgumentException("impactScore must be between 1 and 5");
        this.impactDirection = impactDirection;
        this.impactScore = impactScore;
        this.impactReason = requireText(impactReason, "impactReason", 2000);
        this.modelName = requireText(modelName, "modelName", 100);
    }

    private static String requireText(String value, String field, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        String stripped = value.strip();
        return stripped.length() <= maxLength ? stripped : stripped.substring(0, maxLength);
    }

    public UUID getNewsId() {
        return news.getId();
    }

    public String getLanguage() {
        return language;
    }

    public String getTranslatedTitle() {
        return translatedTitle;
    }

    public String getSummary() {
        return summary;
    }

    public String getModelName() {
        return modelName;
    }

    public ImpactDirection getImpactDirection() {
        return impactDirection;
    }

    public int getImpactScore() {
        return impactScore;
    }

    public String getImpactReason() {
        return impactReason;
    }
}
