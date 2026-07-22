ALTER TABLE news_impacts ADD COLUMN up_probability INTEGER;
ALTER TABLE news_impacts ADD COLUMN down_probability INTEGER;
ALTER TABLE news_impacts ADD COLUMN neutral_probability INTEGER;

ALTER TABLE news_impacts ADD CONSTRAINT chk_news_impacts_probabilities CHECK (
    (up_probability IS NULL AND down_probability IS NULL AND neutral_probability IS NULL)
    OR (
        up_probability BETWEEN 0 AND 100
        AND down_probability BETWEEN 0 AND 100
        AND neutral_probability BETWEEN 0 AND 100
        AND up_probability + down_probability + neutral_probability = 100
    )
);

ALTER TABLE news_translations ADD COLUMN up_probability INTEGER;
ALTER TABLE news_translations ADD COLUMN down_probability INTEGER;
ALTER TABLE news_translations ADD COLUMN neutral_probability INTEGER;

ALTER TABLE news_translations ADD CONSTRAINT chk_news_translations_probabilities CHECK (
    (up_probability IS NULL AND down_probability IS NULL AND neutral_probability IS NULL)
    OR (
        up_probability BETWEEN 0 AND 100
        AND down_probability BETWEEN 0 AND 100
        AND neutral_probability BETWEEN 0 AND 100
        AND up_probability + down_probability + neutral_probability = 100
    )
);
