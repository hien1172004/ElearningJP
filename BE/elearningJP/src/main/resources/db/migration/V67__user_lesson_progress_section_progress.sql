-- =============================================================================
-- V67: Lưu tiến độ từng phần trong bài học trên server (trước đây chỉ ở localStorage của trình duyệt)
-- -----------------------------------------------------------------------------
-- section_progress : số mục đã xem theo loại phần, ví dụ {"KANJI":5,"VOCABULARY":3}
-- progress_percent : % hoàn thành cả bài (0-100) — chỉ để hiển thị vòng tròn ở danh sách bài học
-- Dòng progress bị xóa mềm khi reset bài → 2 cột này reset theo, không cần xử lý thêm.
-- =============================================================================

ALTER TABLE user_lesson_progress
    ADD COLUMN IF NOT EXISTS section_progress JSONB NOT NULL DEFAULT '{}'::jsonb,
    ADD COLUMN IF NOT EXISTS progress_percent SMALLINT NOT NULL DEFAULT 0;
