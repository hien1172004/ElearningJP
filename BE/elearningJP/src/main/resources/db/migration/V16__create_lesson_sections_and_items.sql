-- =============================================================================
-- Flyway Database Migration: V16__create_lesson_sections_and_items.sql
-- Module: Lesson Management (Lesson, Section, Item)
-- =============================================================================

-- 1. Refactor curriculum — tách lesson_items thành lesson_sections + items
-- Tạo: lesson_sections, lesson_kanji_items, lesson_kanji_related_vocabulary,
--       lesson_vocabulary_items, lesson_vocabulary_examples,
--       lesson_grammar_items, lesson_grammar_examples
-- 2. Bổ sung: lesson_quizzes.section_id
-- 3. Hợp nhất logic từ file cũ V13__add_quiz_question_types.sql (vẫn còn trên
--    develop). Toàn bộ thao tác dùng IF NOT EXISTS nên idempotent nếu DB đã
--    apply V13 cũ trước đó — thêm các cột multi-format câu hỏi
--    (question_type, correct_text_answer, matching_data, match_key) + index.
-- -----------------------------------------------------------------------------
-- 1. lesson_sections — nhóm nội dung trong lesson
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS lesson_sections (
    id BIGSERIAL PRIMARY KEY,
    lesson_id BIGINT NOT NULL REFERENCES lessons(id) ON DELETE CASCADE,
    section_type VARCHAR(20) NOT NULL,
    title VARCHAR(255),
    order_index INT NOT NULL DEFAULT 1,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_lesson_section_type
        CHECK (section_type IN ('KANJI','VOCABULARY','GRAMMAR','TEST'))
);

CREATE INDEX IF NOT EXISTS idx_lesson_sections_lesson
    ON lesson_sections(lesson_id);
CREATE INDEX IF NOT EXISTS idx_lesson_sections_type
    ON lesson_sections(section_type);


-- -----------------------------------------------------------------------------
-- 2. lesson_kanji_items — chữ Hán trong bài học
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS lesson_kanji_items (
    id BIGSERIAL PRIMARY KEY,
    section_id BIGINT NOT NULL REFERENCES lesson_sections(id) ON DELETE CASCADE,
    character_id BIGINT NOT NULL REFERENCES writing_characters(id),
    order_index INT NOT NULL DEFAULT 1,
    note TEXT,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_lesson_kanji_section
    ON lesson_kanji_items(section_id);
CREATE INDEX IF NOT EXISTS idx_lesson_kanji_character
    ON lesson_kanji_items(character_id);


-- -----------------------------------------------------------------------------
-- 3. lesson_kanji_related_vocabulary — từ vựng liên quan đến kanji
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS lesson_kanji_related_vocabulary (
    id BIGSERIAL PRIMARY KEY,
    kanji_item_id BIGINT NOT NULL REFERENCES lesson_kanji_items(id) ON DELETE CASCADE,
    vocab_id BIGINT NOT NULL REFERENCES vocabulary(id),
    note TEXT,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_kanji_related_vocab
        UNIQUE (kanji_item_id, vocab_id)
);

CREATE INDEX IF NOT EXISTS idx_kanji_rel_vocab_kanji
    ON lesson_kanji_related_vocabulary(kanji_item_id);
CREATE INDEX IF NOT EXISTS idx_kanji_rel_vocab_vocab
    ON lesson_kanji_related_vocabulary(vocab_id);


-- -----------------------------------------------------------------------------
-- 4. lesson_vocabulary_items — từ vựng trong bài học
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS lesson_vocabulary_items (
    id BIGSERIAL PRIMARY KEY,
    section_id BIGINT NOT NULL REFERENCES lesson_sections(id) ON DELETE CASCADE,
    vocab_id BIGINT NOT NULL REFERENCES vocabulary(id),
    order_index INT NOT NULL DEFAULT 1,
    custom_hiragana VARCHAR(100),
    custom_meaning TEXT,
    image_url VARCHAR(500),
    note TEXT,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_lesson_vocab_section
    ON lesson_vocabulary_items(section_id);
CREATE INDEX IF NOT EXISTS idx_lesson_vocab_vocab
    ON lesson_vocabulary_items(vocab_id);


-- -----------------------------------------------------------------------------
-- 5. lesson_vocabulary_examples — ví dụ riêng do giáo viên chọn
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS lesson_vocabulary_examples (
    id BIGSERIAL PRIMARY KEY,
    vocab_item_id BIGINT NOT NULL REFERENCES lesson_vocabulary_items(id) ON DELETE CASCADE,
    example_jp TEXT NOT NULL,
    example_vi TEXT NOT NULL,
    example_romaji TEXT,
    audio_url VARCHAR(500),
    order_index INT NOT NULL DEFAULT 1,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_lesson_vocab_examples_item
    ON lesson_vocabulary_examples(vocab_item_id);


-- -----------------------------------------------------------------------------
-- 6. lesson_grammar_items — ngữ pháp trong bài học
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS lesson_grammar_items (
    id BIGSERIAL PRIMARY KEY,
    section_id BIGINT NOT NULL REFERENCES lesson_sections(id) ON DELETE CASCADE,
    grammar_id BIGINT NOT NULL REFERENCES grammar_points(id),
    order_index INT NOT NULL DEFAULT 1,
    custom_meaning TEXT,
    custom_structure TEXT,
    note TEXT,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_lesson_grammar_section
    ON lesson_grammar_items(section_id);
CREATE INDEX IF NOT EXISTS idx_lesson_grammar_grammar
    ON lesson_grammar_items(grammar_id);


-- -----------------------------------------------------------------------------
-- 7. lesson_grammar_examples — ví dụ riêng cho ngữ pháp
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS lesson_grammar_examples (
    id BIGSERIAL PRIMARY KEY,
    grammar_item_id BIGINT NOT NULL REFERENCES lesson_grammar_items(id) ON DELETE CASCADE,
    example_jp TEXT NOT NULL,
    example_vi TEXT NOT NULL,
    order_index INT NOT NULL DEFAULT 1,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_lesson_grammar_examples_item
    ON lesson_grammar_examples(grammar_item_id);


-- -----------------------------------------------------------------------------
-- 8. lesson_quizzes — bổ sung section_id (quiz thuộc TEST section)
-- -----------------------------------------------------------------------------
ALTER TABLE lesson_quizzes
    ADD COLUMN IF NOT EXISTS section_id BIGINT
        REFERENCES lesson_sections(id) ON DELETE SET NULL;

-- Bỏ UNIQUE trên lesson_id (cho phép 1 lesson có nhiều quiz qua nhiều section)
ALTER TABLE lesson_quizzes
    DROP CONSTRAINT IF EXISTS lesson_quizzes_lesson_id_key;

CREATE INDEX IF NOT EXISTS idx_lesson_quizzes_section
    ON lesson_quizzes(section_id);
CREATE INDEX IF NOT EXISTS idx_lesson_quizzes_lesson
    ON lesson_quizzes(lesson_id);

-- -----------------------------------------------------------------------------
-- 9. quiz_questions — bổ sung các dạng câu hỏi nâng cao
--    (Hợp nhất logic từ file cũ V13__add_quiz_question_types.sql vẫn còn trên
--     develop. Toàn bộ IF NOT EXISTS nên idempotent. Multi-format:
--     MULTIPLE_CHOICE / FILL_IN_THE_BLANK / MATCHING)
-- -----------------------------------------------------------------------------
ALTER TABLE quiz_questions
    ADD COLUMN IF NOT EXISTS question_type VARCHAR(30) NOT NULL DEFAULT 'MULTIPLE_CHOICE',
    ADD COLUMN IF NOT EXISTS correct_text_answer TEXT,
    ADD COLUMN IF NOT EXISTS matching_data JSONB;

-- -----------------------------------------------------------------------------
-- 10. quiz_options — bổ sung match_key cho dạng MATCHING
-- -----------------------------------------------------------------------------
ALTER TABLE quiz_options
    ADD COLUMN IF NOT EXISTS match_key VARCHAR(100);

-- -----------------------------------------------------------------------------
-- 11. Index hỗ trợ truy vấn lọc theo dạng câu hỏi
-- -----------------------------------------------------------------------------
CREATE INDEX IF NOT EXISTS idx_quiz_questions_type
    ON quiz_questions(question_type);
