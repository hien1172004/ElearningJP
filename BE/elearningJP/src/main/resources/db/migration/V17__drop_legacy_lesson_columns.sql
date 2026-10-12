-- =============================================================================
-- Flyway Database Migration: V17__drop_legacy_lesson_columns.sql
-- Module: Lesson Management (Lesson, Section, Item)
-- =============================================================================

-- 1. Loại bỏ các cột cũ trên bảng lessons
--   - lesson_type        : đã được thay thế bằng lesson_sections.section_type
--   - content_markdown   : nội dung giờ nằm trong sections + items
--   - duration_minutes   : không còn ý nghĩa nghiệp vụ (đã có thời lượng trong section/quiz)
--
-- Chuẩn hoá logic MINI_TEST: thay vì dựa vào lesson.lesson_type = MINI_TEST,
-- giờ "lesson có quiz" = lesson có section TEST (section_type = 'TEST').
-- Đồng thời drop enum LessonType không còn dùng (chỉ còn ExamType.MINI_TEST).
--
-- Drop CHECK constraint trên lesson_type nếu còn tồn tại (do V1 không tạo CHECK
-- nhưng một số môi trường có thể đã add thêm — an toàn vẫn DROP IF EXISTS)
ALTER TABLE lessons DROP CONSTRAINT IF EXISTS chk_lesson_type;

-- Xoá cột cũ (an toàn vì entity/DTO đã được refactor không còn tham chiếu)
ALTER TABLE lessons DROP COLUMN IF EXISTS lesson_type;
ALTER TABLE lessons DROP COLUMN IF EXISTS content_markdown;
ALTER TABLE lessons DROP COLUMN IF EXISTS duration_minutes;

-- Ghi chú: nếu dữ liệu cũ vẫn còn giá trị bestScore/null score trong
-- user_lesson_progress, các giá trị này vẫn hợp lệ — chỉ thay đổi cách
-- xác định "lesson có điểm" thông qua tồn tại section TEST (xem ProgressService).

-- Gợi ý: nếu muốn đảm bảo mọi bản ghi cũ MINI_TEST đã có section TEST tương ứng,
-- chạy data migration thủ công (không bắt buộc — dữ liệu seed đã tạo sẵn section).