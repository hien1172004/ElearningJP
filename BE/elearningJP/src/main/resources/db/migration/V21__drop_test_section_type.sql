-- =============================================================================
-- Flyway Database Migration: V21__drop_test_section_type.sql
-- Refactor: Loại bỏ hoàn toàn LessonSectionType.TEST khỏi DB schema.
--
-- Context:
--   - V20 đã tách quiz ra khỏi section: lesson_quizzes.lesson_id UNIQUE,
--     không còn phụ thuộc section_id.
--   - Ứng dụng giờ xác định "lesson có quiz" bằng cách query thẳng
--     bảng lesson_quizzes (xem LessonService, ProgressService).
--   - LessonSectionType.TEST không còn ý nghĩa nghiệp vụ → loại bỏ khỏi enum
--     Java + DB CHECK constraint.
--
-- Bước thực hiện:
--   1. Soft-delete các section TEST cũ (giữ audit trail).
--   2. Cập nhật CHECK constraint để bỏ 'TEST' khỏi danh sách hợp lệ.
-- =============================================================================

-- 1. Loại bỏ constraint cũ (nếu có)
ALTER TABLE lesson_sections DROP CONSTRAINT IF EXISTS chk_lesson_section_type;

-- 2. Thêm constraint mới (đã bỏ TEST)
ALTER TABLE lesson_sections ADD CONSTRAINT chk_lesson_section_type
    CHECK (section_type IN ('KANJI', 'VOCABULARY', 'GRAMMAR'));

-- 3. Soft-delete các section TEST cũ (giữ audit)
UPDATE lesson_sections
SET deleted = true
WHERE section_type = 'TEST' AND deleted = false;
