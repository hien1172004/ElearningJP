-- =============================================================================
-- Flyway Database Migration: V20__refactor_quiz_lesson_id_only.sql
-- Module: Quiz Refactor
-- =============================================================================
-- Mục tiêu: Quiz chỉ liên kết trực tiếp với Lesson (bỏ section_id)
-- Lý do:
--   1. 1 lesson chỉ có 1 quiz (quy ước nghiệp vụ từ V3)
--   2. Quiz chỉ dùng lesson_id là đủ, không cần qua Section trung gian
--   3. Đơn giản hóa query, giảm JOIN không cần thiết
-- =============================================================================

-- 1. Thêm UNIQUE constraint trên section_id cho các quiz hiện có
--    (đảm bảo 1 section TEST chỉ có 1 quiz - tương lai nếu cần)
--    Chỉ add nếu chưa có data trùng
ALTER TABLE lesson_quizzes
    ADD CONSTRAINT uq_lesson_quizzes_section
    UNIQUE (section_id)
    DEFERRABLE INITIALLY DEFERRED;

-- 2. Cập nhật section_id từ lesson_id cho tất cả quiz hiện có
--    (quiz nào chưa có section_id, tìm TEST section của lesson tương ứng)
UPDATE lesson_quizzes q
SET section_id = (
    SELECT s.id 
    FROM lesson_sections s 
    WHERE s.lesson_id = q.lesson_id 
      AND s.section_type = 'TEST' 
      AND s.deleted = false
    LIMIT 1
)
WHERE q.section_id IS NULL;

-- 3. Xóa column section_id (bỏ liên kết gián tiếp)
ALTER TABLE lesson_quizzes
    DROP COLUMN IF EXISTS section_id;

-- 4. Đảm bảo lesson_id là NOT NULL và UNIQUE (1 lesson = 1 quiz)
--    Nếu đã có constraint thì bỏ qua
ALTER TABLE lesson_quizzes
    ALTER COLUMN lesson_id SET NOT NULL;

-- Kiểm tra xem đã có UNIQUE trên lesson_id chưa, nếu chưa thì thêm
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint 
        WHERE conname = 'lesson_quizzes_lesson_id_key'
    ) THEN
        ALTER TABLE lesson_quizzes
            ADD CONSTRAINT lesson_quizzes_lesson_id_key UNIQUE (lesson_id);
    END IF;
END $$;

-- 5. Xóa index không cần thiết
DROP INDEX IF EXISTS idx_lesson_quizzes_section;
