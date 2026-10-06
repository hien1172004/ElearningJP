-- =============================================================================
-- Flyway Database Migration: V2__add_dictionary_expansion.sql
-- Module: Mazii-Style Dictionary & Pop-up Lookup Expansion
-- =============================================================================

-- 1. Bổ sung các cột tìm kiếm còn thiếu cho bảng vocabulary
ALTER TABLE vocabulary 
    ADD COLUMN IF NOT EXISTS romaji VARCHAR(150),
    ADD COLUMN IF NOT EXISTS han_viet VARCHAR(200);

-- 2. Bảng vocabulary_characters: Liên kết từ vựng với các chữ Hán cấu thành (Rất quan trọng cho popup Mazii)
CREATE TABLE IF NOT EXISTS vocabulary_characters (
    id BIGSERIAL PRIMARY KEY,
    vocab_id BIGINT NOT NULL REFERENCES vocabulary(id) ON DELETE CASCADE,
    character_id BIGINT NOT NULL REFERENCES writing_characters(id) ON DELETE CASCADE,
    position INT NOT NULL DEFAULT 1,
    is_ateji BOOLEAN NOT NULL DEFAULT FALSE,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_vocab_char_pos UNIQUE (vocab_id, character_id, position)
);

-- 3. Bảng vocabulary_senses: Quản lý các nghĩa chi tiết của từ vựng (từ đa nghĩa)
CREATE TABLE IF NOT EXISTS vocabulary_senses (
    id BIGSERIAL PRIMARY KEY,
    vocab_id BIGINT NOT NULL REFERENCES vocabulary(id) ON DELETE CASCADE,
    sense_no SMALLINT NOT NULL DEFAULT 1,
    meaning_vi TEXT NOT NULL,
    meaning_en TEXT,
    part_of_speech VARCHAR(50),
    usage_notes TEXT,
    tags JSONB,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

-- 4. Bảng vocabulary_examples: Danh sách ví dụ minh họa (gắn với từ hoặc gắn với nghĩa cụ thể)
CREATE TABLE IF NOT EXISTS vocabulary_examples (
    id BIGSERIAL PRIMARY KEY,
    vocab_id BIGINT NOT NULL REFERENCES vocabulary(id) ON DELETE CASCADE,
    sense_id BIGINT REFERENCES vocabulary_senses(id) ON DELETE SET NULL,
    example_jp TEXT NOT NULL,
    example_vi TEXT NOT NULL,
    example_romaji TEXT,
    audio_url VARCHAR(500),
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    order_index INT NOT NULL DEFAULT 1,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

-- 5. Bảng vocabulary_readings: Nhiều cách đọc của từ (dùng cho các từ đặc biệt/ateji/nhiều cách đọc)
CREATE TABLE IF NOT EXISTS vocabulary_readings (
    id BIGSERIAL PRIMARY KEY,
    vocab_id BIGINT NOT NULL REFERENCES vocabulary(id) ON DELETE CASCADE,
    reading VARCHAR(100) NOT NULL,
    reading_type VARCHAR(20) NOT NULL DEFAULT 'PRIMARY',
    romaji VARCHAR(150),
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    priority INT NOT NULL DEFAULT 1,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

-- 6. Tối ưu Index cho tra cứu nhanh & tìm kiếm tức thì
CREATE INDEX IF NOT EXISTS idx_vocab_word ON vocabulary(word);
CREATE INDEX IF NOT EXISTS idx_vocab_hiragana ON vocabulary(hiragana);
CREATE INDEX IF NOT EXISTS idx_vocab_romaji ON vocabulary(romaji);
CREATE INDEX IF NOT EXISTS idx_vocab_han_viet ON vocabulary(han_viet);
CREATE INDEX IF NOT EXISTS idx_char_character ON writing_characters(character);
CREATE INDEX IF NOT EXISTS idx_char_han_viet ON writing_characters(han_viet);

CREATE INDEX IF NOT EXISTS idx_vocab_chars_vocab_id ON vocabulary_characters(vocab_id);
CREATE INDEX IF NOT EXISTS idx_vocab_chars_char_id ON vocabulary_characters(character_id);
CREATE INDEX IF NOT EXISTS idx_vocab_senses_vocab_id ON vocabulary_senses(vocab_id);
CREATE INDEX IF NOT EXISTS idx_vocab_examples_vocab_id ON vocabulary_examples(vocab_id);
CREATE INDEX IF NOT EXISTS idx_vocab_examples_sense_id ON vocabulary_examples(sense_id);
CREATE INDEX IF NOT EXISTS idx_vocab_readings_vocab_id ON vocabulary_readings(vocab_id);