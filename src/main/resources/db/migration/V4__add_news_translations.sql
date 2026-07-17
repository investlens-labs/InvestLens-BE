CREATE TABLE news_translations (
    id UUID PRIMARY KEY,
    news_id UUID NOT NULL REFERENCES news_articles(id) ON DELETE CASCADE,
    language VARCHAR(10) NOT NULL,
    translated_title VARCHAR(700) NOT NULL,
    summary TEXT NOT NULL,
    impact_direction VARCHAR(20) NOT NULL CHECK (impact_direction IN ('POSITIVE', 'NEUTRAL', 'NEGATIVE')),
    impact_score INTEGER NOT NULL CHECK (impact_score BETWEEN 1 AND 5),
    impact_reason TEXT NOT NULL,
    model_name VARCHAR(100) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_news_translation_language UNIQUE (news_id, language)
);

CREATE INDEX idx_news_translations_lookup ON news_translations(news_id, language);

ALTER TABLE news_impacts ADD COLUMN ai_analyzed BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE news_impacts ADD COLUMN analysis_model VARCHAR(100);
