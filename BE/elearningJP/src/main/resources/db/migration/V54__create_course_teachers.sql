-- =============================================================================
-- V54: Phân công giáo viên cho khóa học (course_teachers)
-- -----------------------------------------------------------------------------
-- Mục đích: chỉ ADMIN tạo khóa học rồi giao cho một hoặc nhiều giáo viên.
-- Mọi giáo viên được giao trong cùng khóa có quyền ngang nhau trên bài học,
-- section, quiz và đề thi gắn với khóa. Quyền quản lý khóa không còn dựa vào
-- courses.created_by (người tạo nay luôn là ADMIN).
--
-- - Mỗi cặp (course_id, teacher_id) chỉ có 1 dòng; gỡ giáo viên = xóa dòng.
-- - assigned_by: ADMIN đã giao (giữ lại kể cả khi tài khoản ADMIN bị xóa).
-- - Không backfill từ created_by: dữ liệu khóa học cũ do người vận hành tự xử lý.
-- =============================================================================

CREATE TABLE IF NOT EXISTS course_teachers (
    id BIGSERIAL PRIMARY KEY,
    course_id BIGINT NOT NULL REFERENCES courses (id) ON DELETE CASCADE,
    teacher_id BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    assigned_by BIGINT REFERENCES users (id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_course_teachers_course_teacher UNIQUE (course_id, teacher_id)
);

-- Giáo viên xem danh sách khóa được giao; lọc phạm vi bình luận / đề thi theo giáo viên
CREATE INDEX IF NOT EXISTS idx_course_teachers_teacher ON course_teachers (teacher_id, course_id);
