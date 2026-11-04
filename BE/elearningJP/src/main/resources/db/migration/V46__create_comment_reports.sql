-- =============================================================================
-- V46: Báo cáo vi phạm bình luận (comment_reports)
-- -----------------------------------------------------------------------------
-- Mục đích: người dùng báo cáo bình luận / trả lời vi phạm; ADMIN (mọi bình luận) và
-- TEACHER (bình luận trong bài học thuộc khóa của mình) xem hàng chờ và xử lý.
-- (Bảng content_reports cũ gắn với module discussions đã bị xóa ở V23.)
--
-- - Mỗi báo cáo trỏ tới đúng MỘT trong hai: comment_id hoặc reply_id (CHECK).
-- - Mỗi người chỉ báo cáo một bình luận/trả lời một lần (partial unique index).
-- - reason: SPAM | OFFENSIVE | WRONG_INFO | OFF_TOPIC | OTHER   (enum ReportReason)
-- - status: OPEN | RESOLVED | REJECTED                          (enum ReportStatus)
--   RESOLVED = đã ẩn/xóa nội dung; REJECTED = người kiểm duyệt bỏ qua (không vi phạm).
-- =============================================================================

CREATE TABLE IF NOT EXISTS comment_reports (
    id BIGSERIAL PRIMARY KEY,
    reporter_id BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    comment_id BIGINT REFERENCES comments (id) ON DELETE CASCADE,
    reply_id BIGINT REFERENCES comment_replies (id) ON DELETE CASCADE,
    reason VARCHAR(30) NOT NULL
        CHECK (reason IN ('SPAM', 'OFFENSIVE', 'WRONG_INFO', 'OFF_TOPIC', 'OTHER')),
    detail VARCHAR(500),
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN'
        CHECK (status IN ('OPEN', 'RESOLVED', 'REJECTED')),
    handled_by BIGINT REFERENCES users (id) ON DELETE SET NULL,
    handled_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_comment_reports_one_target
        CHECK ((comment_id IS NOT NULL) <> (reply_id IS NOT NULL))
);

-- Chống báo cáo trùng: 1 người / 1 nội dung
CREATE UNIQUE INDEX IF NOT EXISTS uq_comment_reports_reporter_comment
    ON comment_reports (reporter_id, comment_id) WHERE comment_id IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uq_comment_reports_reporter_reply
    ON comment_reports (reporter_id, reply_id) WHERE reply_id IS NOT NULL;

-- Hàng chờ kiểm duyệt: lọc theo trạng thái rồi gom theo nội dung bị báo cáo
CREATE INDEX IF NOT EXISTS idx_comment_reports_status_comment ON comment_reports (status, comment_id);
CREATE INDEX IF NOT EXISTS idx_comment_reports_status_reply ON comment_reports (status, reply_id);
