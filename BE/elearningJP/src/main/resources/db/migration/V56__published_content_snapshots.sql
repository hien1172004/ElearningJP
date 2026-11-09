-- =============================================================================
-- V56: Bản xuất bản (snapshot) của khóa học và đề thi gắn khóa
-- -----------------------------------------------------------------------------
-- Mục đích: học viên học trên bản đã được ADMIN duyệt; giáo viên sửa trên bản đang soạn
-- (các bảng lessons / lesson_sections / ... hiện có) và thay đổi chỉ tới học viên sau khi duyệt.
--
-- course_published_versions: mỗi lần duyệt / ADMIN xuất bản tạo 1 phiên bản (version_no tăng dần).
--   content  = JSON mục lục bài học (thứ tự, có quiz / luyện nói...) tại thời điểm duyệt.
-- course_published_lessons: nội dung JSON từng bài trong phiên bản (section, item, quiz kèm đáp án,
--   luyện nói) — tách theo bài để mở 1 bài không phải đọc cả khóa.
-- courses.published_version_id: phiên bản học viên đang học (NULL = chưa có, đọc bản đang soạn như cũ).
--
-- exam_published_versions: snapshot đề thi gắn khóa (câu hỏi, đáp án, điểm, nhóm điểm).
-- exams.published_version_id: phiên bản học viên đang thi.
-- exam_attempts.published_version_id: phiên bản dùng cho lượt thi đó — chấm điểm / xem lại
--   theo đúng đề học viên đã làm dù đề được duyệt lại giữa chừng.
--
-- lessons.in_published_version: bài có trong phiên bản đang xuất bản — tính tiến độ học viên
--   theo bản đã xuất bản. Chỉ ghi bằng câu lệnh khi tạo snapshot.
--
-- ID nội dung ổn định vì bài / section / item / câu hỏi đều xóa mềm.
-- Khóa đang xuất bản hiện có được tạo snapshot lúc ứng dụng khởi động (PublishedSnapshotBackfill).
-- =============================================================================

CREATE TABLE IF NOT EXISTS course_published_versions (
    id BIGSERIAL PRIMARY KEY,
    course_id BIGINT NOT NULL REFERENCES courses (id) ON DELETE CASCADE,
    version_no INT NOT NULL,
    content JSONB NOT NULL,
    published_by BIGINT REFERENCES users (id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_course_published_versions_no UNIQUE (course_id, version_no)
);

CREATE TABLE IF NOT EXISTS course_published_lessons (
    id BIGSERIAL PRIMARY KEY,
    published_version_id BIGINT NOT NULL REFERENCES course_published_versions (id) ON DELETE CASCADE,
    lesson_id BIGINT NOT NULL REFERENCES lessons (id) ON DELETE CASCADE,
    order_index INT NOT NULL,
    content JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_course_published_lessons UNIQUE (published_version_id, lesson_id)
);

CREATE INDEX IF NOT EXISTS idx_course_published_lessons_lesson
    ON course_published_lessons (lesson_id);

ALTER TABLE courses
    ADD COLUMN IF NOT EXISTS published_version_id BIGINT
        REFERENCES course_published_versions (id) ON DELETE SET NULL;

ALTER TABLE lessons
    ADD COLUMN IF NOT EXISTS in_published_version BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE IF NOT EXISTS exam_published_versions (
    id BIGSERIAL PRIMARY KEY,
    exam_id BIGINT NOT NULL REFERENCES exams (id) ON DELETE CASCADE,
    version_no INT NOT NULL,
    content JSONB NOT NULL,
    published_by BIGINT REFERENCES users (id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_exam_published_versions_no UNIQUE (exam_id, version_no)
);

ALTER TABLE exams
    ADD COLUMN IF NOT EXISTS published_version_id BIGINT
        REFERENCES exam_published_versions (id) ON DELETE SET NULL;

ALTER TABLE exam_attempts
    ADD COLUMN IF NOT EXISTS published_version_id BIGINT
        REFERENCES exam_published_versions (id) ON DELETE SET NULL;
