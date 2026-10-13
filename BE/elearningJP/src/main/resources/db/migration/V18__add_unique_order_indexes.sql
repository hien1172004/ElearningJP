-- Flyway Database Migration: V18__add_unique_order_indexes.sql
-- Module: Lesson Management (Lesson, Section, Item)
-- =============================================================================

-- 1. Đảm bảo tính duy nhất của order_index trong lessons và lesson_sections
-- 2. Đảm bảo tính duy nhất của order_index trong lesson_sections
-- 3. Đảm bảo tính duy nhất của order_index trong lesson_kanji_items
-- 4. Đảm bảo tính duy nhất của order_index trong lesson_vocabulary_items
-- 5. Đảm bảo tính duy nhất của order_index trong lesson_grammar_items
-- 6. Tạo PARTIAL UNIQUE INDEXES
-- 7. Ghi chú cho backend (Java):
--    Khi insert/update order_index trùng → Postgres sẽ ném
--    org.springframework.dao.DataIntegrityViolationException
--    Service cần BẮT exception này và map sang ErrorCode.LESSON_ORDER_CONFLICT
--    để trả về HTTP 409 Conflict thân thiện với client thay vì 500.
--    Ngoài ra, trước khi insert, service nên check tồn tại để:
--      - Tránh để DB ném exception (giảm log noise)
--      - Chủ động shift các bản ghi phía sau +1 (UX mượt hơn)
-- =============================================================================

-- 1. DEDUPLICATE lessons (cập nhật order_index để không trùng trong cùng course)
-- 2. DEDUPLICATE lesson_sections
-- 3. DEDUPLICATE lesson_kanji_items
-- 4. DEDUPLICATE lesson_vocabulary_items
-- 5. DEDUPLICATE lesson_grammar_items
-- 6. Tạo PARTIAL UNIQUE INDEXES
-- 7. Ghi chú cho backend (Java):
--    Khi insert/update order_index trùng → Postgres sẽ ném
--    org.springframework.dao.DataIntegrityViolationException
--    Service cần BẮT exception này và map sang ErrorCode.LESSON_ORDER_CONFLICT
DO $$
DECLARE
    course_rec RECORD;
    lesson_rec RECORD;
    new_idx INT;
BEGIN
    FOR course_rec IN
        SELECT DISTINCT course_id FROM lessons WHERE deleted = FALSE
    LOOP
        new_idx := 1;
        FOR lesson_rec IN
            SELECT id FROM lessons
            WHERE course_id = course_rec.course_id AND deleted = FALSE
            ORDER BY order_index ASC, id ASC
        LOOP
            IF lesson_rec.id IS NOT NULL THEN
                UPDATE lessons
                SET order_index = new_idx
                WHERE id = lesson_rec.id;
                new_idx := new_idx + 1;
            END IF;
        END LOOP;
    END LOOP;
END $$;

-- =============================================================================
-- BƯỚC 2: DEDUPLICATE lesson_sections
-- =============================================================================
DO $$
DECLARE
    lesson_rec RECORD;
    section_rec RECORD;
    new_idx INT;
BEGIN
    FOR lesson_rec IN
        SELECT DISTINCT lesson_id FROM lesson_sections WHERE deleted = FALSE
    LOOP
        new_idx := 1;
        FOR section_rec IN
            SELECT id FROM lesson_sections
            WHERE lesson_id = lesson_rec.lesson_id AND deleted = FALSE
            ORDER BY order_index ASC, id ASC
        LOOP
            IF section_rec.id IS NOT NULL THEN
                UPDATE lesson_sections
                SET order_index = new_idx
                WHERE id = section_rec.id;
                new_idx := new_idx + 1;
            END IF;
        END LOOP;
    END LOOP;
END $$;

-- =============================================================================
-- BƯỚC 3: DEDUPLICATE lesson_kanji_items
-- =============================================================================
DO $$
DECLARE
    section_rec RECORD;
    item_rec RECORD;
    new_idx INT;
BEGIN
    FOR section_rec IN
        SELECT DISTINCT section_id FROM lesson_kanji_items WHERE deleted = FALSE
    LOOP
        new_idx := 1;
        FOR item_rec IN
            SELECT id FROM lesson_kanji_items
            WHERE section_id = section_rec.section_id AND deleted = FALSE
            ORDER BY order_index ASC, id ASC
        LOOP
            IF item_rec.id IS NOT NULL THEN
                UPDATE lesson_kanji_items
                SET order_index = new_idx
                WHERE id = item_rec.id;
                new_idx := new_idx + 1;
            END IF;
        END LOOP;
    END LOOP;
END $$;

-- =============================================================================
-- BƯỚC 4: DEDUPLICATE lesson_vocabulary_items
-- =============================================================================
DO $$
DECLARE
    section_rec RECORD;
    item_rec RECORD;
    new_idx INT;
BEGIN
    FOR section_rec IN
        SELECT DISTINCT section_id FROM lesson_vocabulary_items WHERE deleted = FALSE
    LOOP
        new_idx := 1;
        FOR item_rec IN
            SELECT id FROM lesson_vocabulary_items
            WHERE section_id = section_rec.section_id AND deleted = FALSE
            ORDER BY order_index ASC, id ASC
        LOOP
            IF item_rec.id IS NOT NULL THEN
                UPDATE lesson_vocabulary_items
                SET order_index = new_idx
                WHERE id = item_rec.id;
                new_idx := new_idx + 1;
            END IF;
        END LOOP;
    END LOOP;
END $$;

-- =============================================================================
-- BƯỚC 5: DEDUPLICATE lesson_grammar_items
-- =============================================================================
DO $$
DECLARE
    section_rec RECORD;
    item_rec RECORD;
    new_idx INT;
BEGIN
    FOR section_rec IN
        SELECT DISTINCT section_id FROM lesson_grammar_items WHERE deleted = FALSE
    LOOP
        new_idx := 1;
        FOR item_rec IN
            SELECT id FROM lesson_grammar_items
            WHERE section_id = section_rec.section_id AND deleted = FALSE
            ORDER BY order_index ASC, id ASC
        LOOP
            IF item_rec.id IS NOT NULL THEN
                UPDATE lesson_grammar_items
                SET order_index = new_idx
                WHERE id = item_rec.id;
                new_idx := new_idx + 1;
            END IF;
        END LOOP;
    END LOOP;
END $$;

-- =============================================================================
-- BƯỚC 6: TẠO PARTIAL UNIQUE INDEXES
-- =============================================================================

-- 1) lessons: UNIQUE (course_id, order_index) khi chưa xoá
CREATE UNIQUE INDEX IF NOT EXISTS uq_lessons_course_order_active
    ON lessons (course_id, order_index)
    WHERE deleted = FALSE;

-- 2) lesson_sections: UNIQUE (lesson_id, order_index) khi chưa xoá
CREATE UNIQUE INDEX IF NOT EXISTS uq_lesson_sections_lesson_order_active
    ON lesson_sections (lesson_id, order_index)
    WHERE deleted = FALSE;

-- 3) lesson_kanji_items
CREATE UNIQUE INDEX IF NOT EXISTS uq_lesson_kanji_items_section_order_active
    ON lesson_kanji_items (section_id, order_index)
    WHERE deleted = FALSE;

-- 4) lesson_vocabulary_items
CREATE UNIQUE INDEX IF NOT EXISTS uq_lesson_vocab_items_section_order_active
    ON lesson_vocabulary_items (section_id, order_index)
    WHERE deleted = FALSE;

-- 5) lesson_grammar_items
CREATE UNIQUE INDEX IF NOT EXISTS uq_lesson_grammar_items_section_order_active
    ON lesson_grammar_items (section_id, order_index)
    WHERE deleted = FALSE;

