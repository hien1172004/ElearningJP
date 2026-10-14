-- =============================================================================
-- Flyway Database Migration: V22__add_unique_quiz_questions_order_index.sql
-- Module: Quiz Management
-- =============================================================================
-- Đảm bảo tính duy nhất của order_index trong quiz_questions
-- (cùng pattern với V18 cho lessons/lesson_sections/items).
--
-- 1. DEDUPLICATE quiz_questions theo (quiz_id, order_index) cho các bản ghi
--    chưa xoá. Thứ tự ưu tiên: order_index ASC, id ASC -> giữ ổn định.
-- 2. Tạo PARTIAL UNIQUE INDEX (quiz_id, order_index) WHERE deleted = FALSE.
-- 3. Nếu service chèn trùng -> Postgres ném DataIntegrityViolationException,
--    service phải BẮT và map sang ErrorCode.QUESTION_ORDER_CONFLICT (HTTP 409)
--    thay vì để 500. Đồng thời service nên check + shift sớm để UX mượt hơn.
-- =============================================================================

-- BƯỚC 1: DEDUPLICATE quiz_questions
DO $$
DECLARE
    quiz_rec RECORD;
    question_rec RECORD;
    new_idx INT;
BEGIN
    FOR quiz_rec IN
        SELECT DISTINCT quiz_id FROM quiz_questions WHERE deleted = FALSE
    LOOP
        new_idx := 1;
        FOR question_rec IN
            SELECT id FROM quiz_questions
            WHERE quiz_id = quiz_rec.quiz_id AND deleted = FALSE
            ORDER BY order_index ASC, id ASC
        LOOP
            IF question_rec.id IS NOT NULL THEN
                UPDATE quiz_questions
                SET order_index = new_idx
                WHERE id = question_rec.id;
                new_idx := new_idx + 1;
            END IF;
        END LOOP;
    END LOOP;
END $$;

-- BƯỚC 2: Tạo PARTIAL UNIQUE INDEX
-- UNIQUE (quiz_id, order_index) chỉ áp dụng cho các bản ghi CHƯA xoá
-- (deleted = FALSE) -> cho phép tái sử dụng order_index của bản ghi đã xoá mềm.
CREATE UNIQUE INDEX IF NOT EXISTS uq_quiz_questions_quiz_order_active
    ON quiz_questions (quiz_id, order_index)
    WHERE deleted = FALSE;
