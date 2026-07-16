package com.investlens.news.domain;

import com.investlens.instrument.domain.Instrument;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;

@Entity
@Table(name = "news_impacts", uniqueConstraints = @UniqueConstraint(columnNames = {"news_id", "instrument_id"}))
public class NewsImpact {
    @Id
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "news_id", nullable = false)
    private NewsArticle news;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "instrument_id", nullable = false)
    private Instrument instrument;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ImpactDirection direction;
    @Column(nullable = false)
    private int score;
    @Column(nullable = false, columnDefinition = "text")
    private String reason;

    protected NewsImpact() {}

    public NewsImpact(Instrument instrument, ImpactDirection direction, int score, String reason) {
        if (instrument == null) throw new IllegalArgumentException("instrument must not be null");
        if (direction == null) throw new IllegalArgumentException("direction must not be null");
        if (score < 1 || score > 5) throw new IllegalArgumentException("score must be between 1 and 5");
        if (reason == null || reason.isBlank()) throw new IllegalArgumentException("reason must not be blank");
        this.id = UUID.randomUUID();
        this.instrument = instrument;
        this.direction = direction;
        this.score = score;
        this.reason = reason.strip();
    }

    void attachTo(NewsArticle news) { this.news = news; }
    public UUID getId() { return id; }
    public Instrument getInstrument() { return instrument; }
    public ImpactDirection getDirection() { return direction; }
    public int getScore() { return score; }
    public String getReason() { return reason; }
}
