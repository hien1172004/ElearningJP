-- =============================================================================
-- V58: Gỡ đề thi gắn khóa theo luồng duyệt
-- -----------------------------------------------------------------------------
-- Mục đích: đề gắn khóa đã được duyệt (học viên đang thi) không bị xóa ngay khi giáo viên
-- bấm xóa — chỉ đánh dấu chờ gỡ; đề bị xóa mềm thật khi ADMIN duyệt lần tới, cùng lúc với
-- các thay đổi khác của khóa. Đề chưa từng được duyệt vẫn xóa ngay như cũ.
--   pending_removal : giáo viên đã yêu cầu gỡ, chờ lần duyệt khóa (hủy được trước khi duyệt)
-- =============================================================================

ALTER TABLE exams
    ADD COLUMN IF NOT EXISTS pending_removal BOOLEAN NOT NULL DEFAULT FALSE;
