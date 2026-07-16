CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(320) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL CHECK (role IN ('USER', 'ADMIN')),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE instruments (
    id UUID PRIMARY KEY,
    ticker VARCHAR(16) NOT NULL UNIQUE,
    company_name VARCHAR(200) NOT NULL,
    type VARCHAR(20) NOT NULL CHECK (type IN ('STOCK', 'ETF')),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE portfolio_items (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    instrument_id UUID NOT NULL REFERENCES instruments(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_portfolio_user_instrument UNIQUE (user_id, instrument_id)
);

CREATE INDEX idx_portfolio_items_user_id ON portfolio_items(user_id);

CREATE TABLE news_articles (
    id UUID PRIMARY KEY,
    source VARCHAR(100) NOT NULL,
    canonical_url TEXT NOT NULL,
    canonical_url_hash VARCHAR(64) NOT NULL UNIQUE,
    title VARCHAR(700) NOT NULL,
    original_content TEXT,
    translated_title VARCHAR(700),
    translated_content TEXT,
    summary TEXT,
    market_context TEXT,
    analysis_status VARCHAR(20) NOT NULL CHECK (analysis_status IN ('PENDING', 'COMPLETED', 'FAILED')),
    model_name VARCHAR(100),
    published_at TIMESTAMP WITH TIME ZONE NOT NULL,
    analysis_error VARCHAR(1000),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_news_published_at ON news_articles(published_at DESC);

CREATE TABLE news_related_instruments (
    id UUID PRIMARY KEY,
    news_id UUID NOT NULL REFERENCES news_articles(id) ON DELETE CASCADE,
    instrument_id UUID NOT NULL REFERENCES instruments(id),
    CONSTRAINT uk_news_related_instrument UNIQUE (news_id, instrument_id)
);

CREATE INDEX idx_news_related_instrument ON news_related_instruments(instrument_id, news_id);

CREATE TABLE news_impacts (
    id UUID PRIMARY KEY,
    news_id UUID NOT NULL REFERENCES news_articles(id) ON DELETE CASCADE,
    instrument_id UUID NOT NULL REFERENCES instruments(id),
    direction VARCHAR(20) NOT NULL CHECK (direction IN ('POSITIVE', 'NEUTRAL', 'NEGATIVE')),
    score INTEGER NOT NULL CHECK (score BETWEEN 1 AND 5),
    reason TEXT NOT NULL,
    CONSTRAINT uk_news_impact_instrument UNIQUE (news_id, instrument_id)
);

CREATE INDEX idx_news_impacts_instrument ON news_impacts(instrument_id, news_id);
CREATE INDEX idx_news_impacts_filter ON news_impacts(instrument_id, direction, score);
