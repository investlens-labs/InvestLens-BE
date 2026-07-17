ALTER TABLE instruments ADD COLUMN market VARCHAR(10);
ALTER TABLE instruments ADD COLUMN active BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE instruments ADD COLUMN catalog_synced_at TIMESTAMP WITH TIME ZONE;

UPDATE instruments SET market = 'US' WHERE market IS NULL;

ALTER TABLE instruments ALTER COLUMN market SET NOT NULL;
ALTER TABLE instruments ADD CONSTRAINT chk_instruments_market CHECK (market IN ('KR', 'US'));

CREATE INDEX idx_instruments_search_filter ON instruments(active, market, type);
