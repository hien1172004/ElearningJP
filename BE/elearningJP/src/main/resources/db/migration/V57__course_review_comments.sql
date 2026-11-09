-- =============================================================================
-- V57: Nhận xét duyệt theo bài học
-- -----------------------------------------------------------------------------
-- Mục đích: ADMIN (chỉ duyệt, không soạn) ghi nhận xét gắn với từng bài / mục của khóa
-- trong lượt duyệt đang chờ; giáo viên thấy nhận xét ở đúng bài đó và đánh dấu đã sửa.
-- Lý do từ chối chung vẫn nằm ở course_review_requests.review_note.
--
--   review_request_id : lượt duyệt chứa nhận xét
--   lesson_id         : NULL = nhận xét chung cho cả khóa
--   section_id        : NULL = cả bài
--   resolved          : giáo viên đã sửa xong
-- =============================================================================

CREATE TABLE IF NOT EXISTS course_review_comments (
    id BIGSERIAL PRIMARY KEY,
    review_request_id BIGINT NOT NULL REFERENCES course_review_requests (id) ON DELETE CASCADE,
    course_id BIGINT NOT NULL REFERENCES courses (id) ON DELETE CASCADE,
    lesson_id BIGINT REFERENCES lessons (id) ON DELETE SET NULL,
    section_id BIGINT REFERENCES lesson_sections (id) ON DELETE SET NULL,
    content VARCHAR(2000) NOT NULL,
    author_id BIGINT REFERENCES users (id) ON DELETE SET NULL,
    resolved BOOLEAN NOT NULL DEFAULT FALSE,
    resolved_by BIGINT REFERENCES users (id) ON DELETE SET NULL,
    resolved_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_course_review_comments_request
    ON course_review_comments (review_request_id, created_at);
CREATE INDEX IF NOT EXISTS idx_course_review_comments_lesson
    ON course_review_comments (lesson_id);
