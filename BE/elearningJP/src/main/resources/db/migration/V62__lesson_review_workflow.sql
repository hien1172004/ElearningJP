-- =============================================================================
-- V62: Duyệt xuất bản theo từng bài học (docs/lesson-review-design.md)
-- -----------------------------------------------------------------------------
-- Mục đích: giáo viên gửi duyệt THEO BÀI (chọn nhiều bài một lần); ADMIN duyệt / từ chối từng bài
-- hoặc tự "Lưu" một bài lên bản live. Bản live vẫn là snapshot V56; bản nháp vẫn là các bảng soạn.
-- Luồng duyệt theo cả khóa (V55) chạy song song tới khi FE chuyển hẳn sang duyệt theo bài.
--
-- lessons:
--   review_status           EDITING | PENDING_REVIEW — bài chờ duyệt chỉ đọc với MỌI người (kể cả ADMIN).
--   has_unpublished_changes bản nháp của bài khác bản live (bài mới tạo = TRUE).
--   draft_revision          tăng mỗi lần ghi nội dung bài; gửi duyệt phải khớp số này (chống
--                           "gửi duyệt và sửa cùng lúc" — chỉ 1 thao tác thành công).
--   draft_updated_by(_name), draft_updated_at: người / lúc sửa bản nháp của bài gần nhất.
--   Các cột này chỉ ghi bằng câu lệnh UPDATE có điều kiện (không qua entity) để 2 thao tác
--   chồng nhau được Postgres tự xếp hàng theo khóa dòng.
--
-- lesson_review_requests: lịch sử gửi duyệt theo bài.
--   batch_id       : các bài gửi cùng 1 lượt (1 thông báo cho ADMIN).
--   draft_revision : bản nháp lúc gửi.
--   status         : PENDING | APPROVED | REJECTED | WITHDRAWN; mỗi bài tối đa 1 PENDING.
--
-- Dữ liệu cũ: bài chưa có trong bản live, hoặc thuộc khóa đang có thay đổi chưa duyệt
-- (không biết bài nào đổi) → coi như có thay đổi.
-- =============================================================================

ALTER TABLE lessons
    ADD COLUMN IF NOT EXISTS review_status VARCHAR(20) NOT NULL DEFAULT 'EDITING',
    ADD COLUMN IF NOT EXISTS has_unpublished_changes BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS draft_revision BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS draft_updated_by BIGINT REFERENCES users (id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS draft_updated_by_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS draft_updated_at TIMESTAMPTZ;

ALTER TABLE lessons DROP CONSTRAINT IF EXISTS chk_lessons_review_status;
ALTER TABLE lessons
    ADD CONSTRAINT chk_lessons_review_status CHECK (review_status IN ('EDITING', 'PENDING_REVIEW'));

UPDATE lessons l
SET has_unpublished_changes = (
        l.in_published_version = FALSE
        OR EXISTS (SELECT 1 FROM courses c WHERE c.id = l.course_id AND c.has_unpublished_changes = TRUE)
    );

CREATE INDEX IF NOT EXISTS idx_lessons_draft_updated_by ON lessons (draft_updated_by);

CREATE TABLE IF NOT EXISTS lesson_review_requests (
    id BIGSERIAL PRIMARY KEY,
    lesson_id BIGINT NOT NULL REFERENCES lessons (id) ON DELETE CASCADE,
    course_id BIGINT NOT NULL REFERENCES courses (id) ON DELETE CASCADE,
    batch_id UUID NOT NULL,
    submitted_by BIGINT REFERENCES users (id) ON DELETE SET NULL,
    note VARCHAR(1000),
    draft_revision BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'WITHDRAWN')),
    reviewed_by BIGINT REFERENCES users (id) ON DELETE SET NULL,
    review_note VARCHAR(1000),
    reviewed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

-- Mỗi bài tối đa 1 yêu cầu đang chờ
CREATE UNIQUE INDEX IF NOT EXISTS uq_lesson_review_requests_pending
    ON lesson_review_requests (lesson_id) WHERE status = 'PENDING';

-- Lịch sử theo khóa / bài (mới nhất trước) và hàng chờ duyệt của ADMIN (cũ nhất trước)
CREATE INDEX IF NOT EXISTS idx_lesson_review_requests_course
    ON lesson_review_requests (course_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_lesson_review_requests_lesson
    ON lesson_review_requests (lesson_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_lesson_review_requests_status
    ON lesson_review_requests (status, created_at);
