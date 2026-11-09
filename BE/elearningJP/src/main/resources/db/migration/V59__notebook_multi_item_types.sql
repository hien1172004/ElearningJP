-- =============================================================================
-- Migration V59: Sổ tay lưu được 3 loại kiến thức (Từ vựng / Chữ Hán / Ngữ pháp)
-- -----------------------------------------------------------------------------
-- - notebook_words: thêm item_type + liên kết tới chữ Hán (writing_characters),
--   ngữ pháp (grammar_points) và bài học nguồn (lessons).
-- - notebooks: thêm bộ đếm theo loại (vocab/kanji/grammar) cạnh word_count (tổng).
-- - Chuyển dữ liệu cũ: trước đây trang Từ điển lưu chữ Hán / ngữ pháp thành "từ"
--   với part_of_speech = 'Chữ Hán' / 'Ngữ pháp' → đổi sang đúng loại + liên kết lại.
-- =============================================================================

ALTER TABLE notebook_words
    ADD COLUMN IF NOT EXISTS item_type VARCHAR(10) NOT NULL DEFAULT 'VOCAB',
    ADD COLUMN IF NOT EXISTS character_id BIGINT REFERENCES writing_characters(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS grammar_id BIGINT REFERENCES grammar_points(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS source_lesson_id BIGINT REFERENCES lessons(id) ON DELETE SET NULL;

ALTER TABLE notebook_words DROP CONSTRAINT IF EXISTS chk_notebook_words_item_type;
ALTER TABLE notebook_words
    ADD CONSTRAINT chk_notebook_words_item_type CHECK (item_type IN ('VOCAB', 'KANJI', 'GRAMMAR'));

-- ----- Chuyển dữ liệu cũ: Chữ Hán -----
UPDATE notebook_words
SET item_type = 'KANJI'
WHERE part_of_speech = 'Chữ Hán';

UPDATE notebook_words nw
SET character_id = wc.id
FROM writing_characters wc
WHERE nw.item_type = 'KANJI'
  AND nw.character_id IS NULL
  AND wc."character" = nw.word
  AND wc.deleted = FALSE;

-- ----- Chuyển dữ liệu cũ: Ngữ pháp (cùng tên → lấy bản có id nhỏ nhất) -----
UPDATE notebook_words
SET item_type = 'GRAMMAR'
WHERE part_of_speech = 'Ngữ pháp';

UPDATE notebook_words nw
SET grammar_id = g.id
FROM (
    SELECT DISTINCT ON (title) id, title
    FROM grammar_points
    WHERE deleted = FALSE
    ORDER BY title, id
) g
WHERE nw.item_type = 'GRAMMAR'
  AND nw.grammar_id IS NULL
  AND g.title = nw.word;

-- Chữ Hán / ngữ pháp từng bị auto-link nhầm sang 1 từ vựng cùng mặt chữ → gỡ liên kết
UPDATE notebook_words
SET vocabulary_id = NULL
WHERE item_type <> 'VOCAB';

CREATE INDEX IF NOT EXISTS idx_notebook_words_type ON notebook_words(notebook_id, item_type) WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_notebook_words_character_id ON notebook_words(character_id) WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_notebook_words_grammar_id ON notebook_words(grammar_id) WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_notebook_words_source_lesson_id ON notebook_words(source_lesson_id) WHERE deleted = FALSE;

-- ----- Bộ đếm theo loại trên sổ tay -----
ALTER TABLE notebooks
    ADD COLUMN IF NOT EXISTS vocab_count INT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS kanji_count INT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS grammar_count INT NOT NULL DEFAULT 0;

UPDATE notebooks n
SET vocab_count   = c.vocab_count,
    kanji_count   = c.kanji_count,
    grammar_count = c.grammar_count,
    word_count    = c.vocab_count + c.kanji_count + c.grammar_count
FROM (
    SELECT notebook_id,
           COUNT(*) FILTER (WHERE item_type = 'VOCAB')   AS vocab_count,
           COUNT(*) FILTER (WHERE item_type = 'KANJI')   AS kanji_count,
           COUNT(*) FILTER (WHERE item_type = 'GRAMMAR') AS grammar_count
    FROM notebook_words
    WHERE deleted = FALSE
    GROUP BY notebook_id
) c
WHERE c.notebook_id = n.id;
