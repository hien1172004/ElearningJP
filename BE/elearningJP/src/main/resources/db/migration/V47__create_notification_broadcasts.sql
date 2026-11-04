-- =============================================================================
-- V47: Lịch sử gửi thông báo hàng loạt (notification_broadcasts)
-- -----------------------------------------------------------------------------
-- Mục đích: ADMIN gửi thông báo cho toàn bộ người dùng / theo vai trò / học viên một khóa;
-- TEACHER gửi cho học viên khóa học của mình. Mỗi lần gửi lưu 1 dòng ở đây, còn từng
-- người nhận có 1 bản ghi notifications (type = ANNOUNCEMENT, data_json.broadcastId).
--
-- - audience: ALL_USERS | ROLE | COURSE_STUDENTS   (enum BroadcastAudience)
-- - role: tên role khi audience = ROLE (USER | TEACHER | ADMIN)
-- - course_id: khi audience = COURSE_STUDENTS
-- - link: đường dẫn trong ứng dụng mở khi bấm thông báo (không bắt buộc)
-- =============================================================================

CREATE TABLE IF NOT EXISTS notification_broadcasts (
    id BIGSERIAL PRIMARY KEY,
    sender_id BIGINT REFERENCES users (id) ON DELETE SET NULL,
    title VARCHAR(120) NOT NULL,
    body VARCHAR(1000) NOT NULL,
    audience VARCHAR(30) NOT NULL
        CHECK (audience IN ('ALL_USERS', 'ROLE', 'COURSE_STUDENTS')),
    role VARCHAR(20),
    course_id BIGINT REFERENCES courses (id) ON DELETE SET NULL,
    link VARCHAR(300),
    recipient_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

-- Lịch sử: TEACHER xem các lần gửi của mình, mới nhất trước
CREATE INDEX IF NOT EXISTS idx_notification_broadcasts_sender ON notification_broadcasts (sender_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_notification_broadcasts_created ON notification_broadcasts (created_at DESC);

-- Đẩy realtime sau khi gửi: tìm các thông báo thuộc 1 lần gửi
CREATE INDEX IF NOT EXISTS idx_notifications_broadcast_id
    ON notifications ((data_json ->> 'broadcastId')) WHERE type = 'ANNOUNCEMENT';
