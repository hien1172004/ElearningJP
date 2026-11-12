-- =============================================================================
-- V64: Cờ "thay đổi lớn" khi đưa bài lên bản học viên (docs/lesson-review-design.md mục 6)
-- -----------------------------------------------------------------------------
-- Mục đích: ADMIN đánh dấu lần duyệt là thay đổi lớn → học viên đã hoàn thành bài nhận thông báo
-- LESSON_UPDATED (khi khóa đang xuất bản). Lưu lại trên yêu cầu duyệt để lịch sử hiển thị.
-- ADMIN "Lưu bài" (không qua yêu cầu) chỉ gửi kèm cờ trong thao tác, không lưu dòng nào.
-- =============================================================================

ALTER TABLE lesson_review_requests
    ADD COLUMN IF NOT EXISTS major_change BOOLEAN NOT NULL DEFAULT FALSE;
