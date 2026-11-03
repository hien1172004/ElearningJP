-- =============================================================================
-- Migration V39: Tạo bảng từ vựng sổ tay (notebook_words)
-- =============================================================================

CREATE TABLE IF NOT EXISTS notebook_words (
    id BIGSERIAL PRIMARY KEY,
    notebook_id BIGINT NOT NULL REFERENCES notebooks(id) ON DELETE CASCADE,
    vocabulary_id BIGINT REFERENCES vocabulary(id) ON DELETE SET NULL,
    word VARCHAR(100) NOT NULL,
    reading VARCHAR(100),
    meaning TEXT NOT NULL,
    part_of_speech VARCHAR(50),
    user_note TEXT,
    sino_vietnamese VARCHAR(100),
    example_sentence TEXT,
    example_meaning TEXT,
    audio_url VARCHAR(500),
    jlpt_level VARCHAR(2),
    display_order INT NOT NULL DEFAULT 0,
    
    -- SRS (SM-2) fields
    memory_level VARCHAR(20) NOT NULL DEFAULT 'NOT_LEARNED',
    interval_days INT NOT NULL DEFAULT 0,
    repetition_count INT NOT NULL DEFAULT 0,
    lapse_count INT NOT NULL DEFAULT 0,
    ease_factor NUMERIC(3, 2) NOT NULL DEFAULT 2.50,
    next_review_at TIMESTAMPTZ,
    last_reviewed_at TIMESTAMPTZ,
    
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_notebook_words_notebook_id ON notebook_words(notebook_id) WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_notebook_words_srs_due ON notebook_words(notebook_id, next_review_at) WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_notebook_words_vocab_id ON notebook_words(vocabulary_id) WHERE deleted = FALSE;

