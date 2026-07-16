package com.investlens.portfolio.domain;

import com.investlens.common.domain.BaseTimeEntity;
import com.investlens.instrument.domain.Instrument;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;

@Entity
@Table(
        name = "portfolio_items",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_portfolio_items_user_instrument",
                columnNames = {"user_id", "instrument_id"}
        )
)
public class PortfolioItem extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "instrument_id", nullable = false)
    private Instrument instrument;

    protected PortfolioItem() {
    }

    public PortfolioItem(UUID userId, Instrument instrument) {
        this.userId = userId;
        this.instrument = instrument;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public Instrument getInstrument() {
        return instrument;
    }
}
