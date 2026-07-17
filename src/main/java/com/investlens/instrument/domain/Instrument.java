package com.investlens.instrument.domain;

import com.investlens.common.domain.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "instruments")
public class Instrument extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 16)
    private String ticker;

    @Column(name = "company_name", nullable = false, length = 200)
    private String companyName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InstrumentType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private InstrumentMarket market;

    @Column(nullable = false)
    private boolean active = true;

    protected Instrument() {
    }

    public Instrument(String ticker, String companyName, InstrumentType type) {
        this(ticker, companyName, type, InstrumentMarket.US);
    }

    public Instrument(String ticker, String companyName, InstrumentType type, InstrumentMarket market) {
        this.ticker = ticker;
        this.companyName = companyName;
        this.type = type;
        this.market = market;
    }

    public UUID getId() {
        return id;
    }

    public String getTicker() {
        return ticker;
    }

    public String getCompanyName() {
        return companyName;
    }

    public InstrumentType getType() {
        return type;
    }

    public InstrumentMarket getMarket() {
        return market;
    }

    public boolean isActive() {
        return active;
    }
}
