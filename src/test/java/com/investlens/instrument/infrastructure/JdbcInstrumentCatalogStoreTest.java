package com.investlens.instrument.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.investlens.instrument.application.InstrumentCatalogItem;
import com.investlens.instrument.domain.InstrumentMarket;
import com.investlens.instrument.domain.InstrumentType;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class JdbcInstrumentCatalogStoreTest {
    @Test
    void insertsUpdatesAndDeactivatesCatalogEntries() {
        var dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:catalog-store;MODE=PostgreSQL;DB_CLOSE_DELAY=-1", "sa", "");
        var jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.execute("""
                CREATE TABLE instruments (
                    id UUID PRIMARY KEY, ticker VARCHAR(16) NOT NULL UNIQUE, company_name VARCHAR(200) NOT NULL,
                    type VARCHAR(20) NOT NULL, market VARCHAR(10) NOT NULL, active BOOLEAN NOT NULL,
                    catalog_synced_at TIMESTAMP WITH TIME ZONE, created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
                )
                """);
        jdbcTemplate.update("""
                INSERT INTO instruments VALUES
                (RANDOM_UUID(), 'OLD', 'Old Company', 'STOCK', 'US', TRUE, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """);
        var store = new JdbcInstrumentCatalogStore(jdbcTemplate);

        store.synchronize(List.of(
                new InstrumentCatalogItem("AAPL", "Apple Inc.", InstrumentType.STOCK, InstrumentMarket.US),
                new InstrumentCatalogItem("069500", "KODEX 200", InstrumentType.ETF, InstrumentMarket.KR)
        ), Instant.now());

        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM instruments", Integer.class)).isEqualTo(3);
        assertThat(jdbcTemplate.queryForObject("SELECT active FROM instruments WHERE ticker = 'OLD'", Boolean.class))
                .isFalse();
        assertThat(jdbcTemplate.queryForObject("SELECT market FROM instruments WHERE ticker = '069500'", String.class))
                .isEqualTo("KR");
    }
}
