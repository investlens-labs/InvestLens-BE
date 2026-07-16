package com.investlens.news.domain;

import com.investlens.instrument.domain.Instrument;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;

@Entity
@Table(name = "news_related_instruments",
        uniqueConstraints = @UniqueConstraint(columnNames = {"news_id", "instrument_id"}))
public class NewsRelatedInstrument {
    @Id
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "news_id", nullable = false)
    private NewsArticle news;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "instrument_id", nullable = false)
    private Instrument instrument;

    protected NewsRelatedInstrument() {}

    public NewsRelatedInstrument(NewsArticle news, Instrument instrument) {
        if (news == null || instrument == null) throw new IllegalArgumentException("news and instrument are required");
        this.id = UUID.randomUUID();
        this.news = news;
        this.instrument = instrument;
    }

    public Instrument getInstrument() { return instrument; }
}
