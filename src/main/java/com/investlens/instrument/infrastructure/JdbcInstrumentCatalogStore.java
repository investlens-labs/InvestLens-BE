package com.investlens.instrument.infrastructure;

import com.investlens.instrument.application.InstrumentCatalogItem;
import com.investlens.instrument.application.InstrumentCatalogStorePort;
import com.investlens.instrument.domain.InstrumentMarket;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JdbcInstrumentCatalogStore implements InstrumentCatalogStorePort {
    private static final String UPDATE_SQL = """
            UPDATE instruments
               SET company_name = ?, type = ?, market = ?, active = TRUE,
                   catalog_synced_at = ?, updated_at = CURRENT_TIMESTAMP
             WHERE ticker = ?
            """;
    private static final String INSERT_SQL = """
            INSERT INTO instruments
                (id, ticker, company_name, type, market, active, catalog_synced_at, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, TRUE, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            """;

    private final JdbcTemplate jdbcTemplate;

    public JdbcInstrumentCatalogStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public synchronized void synchronize(List<InstrumentCatalogItem> instruments, Instant synchronizedAt) {
        Timestamp timestamp = Timestamp.from(synchronizedAt);
        Set<String> existingTickers = new HashSet<>(
                jdbcTemplate.queryForList("SELECT ticker FROM instruments", String.class));
        List<InstrumentCatalogItem> existing = instruments.stream()
                .filter(item -> existingTickers.contains(item.ticker())).toList();
        List<InstrumentCatalogItem> added = instruments.stream()
                .filter(item -> !existingTickers.contains(item.ticker())).toList();

        jdbcTemplate.batchUpdate(UPDATE_SQL, existing, 500, (statement, item) -> {
            statement.setString(1, item.companyName());
            statement.setString(2, item.type().name());
            statement.setString(3, item.market().name());
            statement.setTimestamp(4, timestamp);
            statement.setString(5, item.ticker());
        });
        jdbcTemplate.batchUpdate(INSERT_SQL, added, 500, (statement, item) -> {
            statement.setObject(1, UUID.randomUUID());
            statement.setString(2, item.ticker());
            statement.setString(3, item.companyName());
            statement.setString(4, item.type().name());
            statement.setString(5, item.market().name());
            statement.setTimestamp(6, timestamp);
        });
        for (InstrumentMarket market : InstrumentMarket.values()) {
            jdbcTemplate.update("""
                    UPDATE instruments
                       SET active = FALSE, updated_at = CURRENT_TIMESTAMP
                     WHERE market = ? AND (catalog_synced_at IS NULL OR catalog_synced_at < ?)
                    """, market.name(), timestamp);
        }
    }
}
