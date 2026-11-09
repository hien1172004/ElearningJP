-- =============================================================================
-- V55: Luồng duyệt xuất bản khóa học
-- -----------------------------------------------------------------------------
-- Mục đích: giáo viên được giao soạn xong thì gửi yêu cầu duyệt; chỉ ADMIN quyết định
-- xuất bản / chuyển nháp. Trạng thái khóa tách 2 chiều độc lập:
--   - is_published (đã có): học viên có thấy khóa hay không.
--   - review_status: trạng thái bản đang soạn
--       EDITING        — đang soạn, giáo viên được sửa
--       PENDING_REVIEW — đã gửi duyệt, mọi giáo viên bị khóa sửa tới khi ADMIN quyết định / rút yêu cầu
--   - has_unpublished_changes: bản đang soạn có thay đổi chưa được duyệt/xuất bản.
--
-- course_review_requests: lịch sử từng lượt gửi duyệt.
--   status: PENDING | APPROVED | REJECTED | WITHDRAWN
--   Mỗi khóa chỉ có tối đa 1 yêu cầu PENDING (partial unique index).
--
-- Dữ liệu cũ: khóa hiện có nhận review_status = EDITING; khóa đang xuất bản coi như
-- không có thay đổi chờ duyệt, khóa nháp coi như có thay đổi (cần gửi duyệt để xuất bản).
-- =============================================================================

ALTER TABLE courses
    ADD COLUMN IF NOT EXISTS review_status VARCHAR(20) NOT NULL DEFAULT 'EDITING',
    ADD COLUMN IF NOT EXISTS has_unpublished_changes BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE courses DROP CONSTRAINT IF EXISTS chk_courses_review_status;
ALTER TABLE courses
    ADD CONSTRAINT chk_courses_review_status CHECK (review_status IN ('EDITING', 'PENDING_REVIEW'));

UPDATE courses SET has_unpublished_changes = TRUE WHERE is_published = FALSE;

CREATE TABLE IF NOT EXISTS course_review_requests (
    id BIGSERIAL PRIMARY KEY,
    course_id BIGINT NOT NULL REFERENCES courses (id) ON DELETE CASCADE,
    submitted_by BIGINT REFERENCES users (id) ON DELETE SET NULL,
    note VARCHAR(1000),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'WITHDRAWN')),
    reviewed_by BIGINT REFERENCES users (id) ON DELETE SET NULL,
    review_note VARCHAR(1000),
    reviewed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

-- Mỗi khóa tối đa 1 yêu cầu đang chờ
CREATE UNIQUE INDEX IF NOT EXISTS uq_course_review_requests_pending
    ON course_review_requests (course_id) WHERE status = 'PENDING';

-- Lịch sử theo khóa (mới nhất trước) và hàng chờ duyệt của ADMIN
CREATE INDEX IF NOT EXISTS idx_course_review_requests_course
    ON course_review_requests (course_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_course_review_requests_status
    ON course_review_requests (status, created_at);
