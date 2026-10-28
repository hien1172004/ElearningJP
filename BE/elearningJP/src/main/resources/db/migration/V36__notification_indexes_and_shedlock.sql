-- ============================================================================
-- V35 — Module Thông báo: index + chống trùng nhắc nhở theo ngày + ShedLock
--
-- Bảng notifications đã có sẵn từ V1__init_schema.sql (id, user_id, title, body,
-- type VARCHAR(30), data_json JSONB, is_read, deleted, deleted_at,
-- created_at TIMESTAMPTZ, updated_at, version) — migration này KHÔNG tạo lại bảng,
-- chỉ thêm index phục vụ truy vấn và khoá chống trùng.
-- ============================================================================

-- 1. Danh sách thông báo của user (mới nhất trước) — cho GET /notifications
CREATE INDEX IF NOT EXISTS idx_notifications_user_created
    ON notifications (user_id, created_at DESC);

-- 2. Đếm / lấy nhanh thông báo CHƯA ĐỌC — partial index rất nhỏ vì đa số đã đọc
CREATE INDEX IF NOT EXISTS idx_notifications_user_unread
    ON notifications (user_id)
    WHERE is_read = false AND deleted = false;

-- 3. Chốt chặn cuối chống trùng thông báo nhắc nhở theo NGÀY (giờ VN).
--    Lớp 1 (application) là existsByUserIdAndTypeAndCreatedAtBetween;
--    lớp 2 (DB) là unique index này — khi 2 instance vượt check cùng lúc,
--    instance thua nhận DataIntegrityViolationException và scheduler bỏ qua.
--    created_at là TIMESTAMPTZ nên phải quy đổi về ngày theo Asia/Ho_Chi_Minh;
--    timezone(text, timestamptz) là IMMUTABLE nên hợp lệ trong index expression.
CREATE UNIQUE INDEX IF NOT EXISTS uq_notifications_daily_reminder
    ON notifications (user_id, type, ((created_at AT TIME ZONE 'Asia/Ho_Chi_Minh')::date))
    WHERE type IN ('SRS_DUE', 'STREAK_WARNING') AND deleted = false;

-- 4. Bảng khoá ShedLock (schema chuẩn do thư viện quy định)
CREATE TABLE IF NOT EXISTS shedlock (
    name       VARCHAR(64)  NOT NULL PRIMARY KEY,
    lock_until TIMESTAMP    NOT NULL,
    locked_at  TIMESTAMP    NOT NULL,
    locked_by  VARCHAR(255) NOT NULL
);
