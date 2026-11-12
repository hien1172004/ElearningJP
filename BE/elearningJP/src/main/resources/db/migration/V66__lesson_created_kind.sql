-- =============================================================================
-- V66: Bài mới đang soạn chỉ phía tạo ra thấy (docs/lesson-review-design.md mục 8)
-- -----------------------------------------------------------------------------
-- Mục đích: bài giáo viên tạo mà còn đang soạn (chưa gửi duyệt) thì ADMIN không thấy — kể cả trong danh sách bài,
-- "Duyệt theo bài", "Thay đổi chưa gửi duyệt"; giáo viên gửi duyệt thì ADMIN thấy (để duyệt bản đã gửi). Bài ADMIN tạo
-- chưa lưu thì giáo viên không thấy. Bài đã có trong bản học viên đang học thì cả hai phía đều thấy.
--
-- lessons.created_kind: TEACHER | ADMIN — phía tạo bài; NULL = cả hai phía thấy (bài đã từng lên bản học viên).
--   Quy tắc thấy bài của phía K: in_published_version = TRUE OR created_kind IS NULL OR created_kind = K
--   OR (K = ADMIN AND bài đang có yêu cầu duyệt PENDING) — kiểm ở ứng dụng (LessonRepository#findVisibleByCourseId).
--   Bài vừa lên bản học viên (duyệt / ADMIN lưu / lưu cả khóa) → created_kind = NULL.
--
-- Dữ liệu cũ: bài chưa có trong bản học viên → phía của người sửa gần nhất nếu là ADMIN, còn lại TEACHER
-- (cách A: nháp chung cũ thuộc giáo viên).
-- =============================================================================

ALTER TABLE lessons ADD COLUMN IF NOT EXISTS created_kind VARCHAR(10);

ALTER TABLE lessons DROP CONSTRAINT IF EXISTS chk_lessons_created_kind;
ALTER TABLE lessons
    ADD CONSTRAINT chk_lessons_created_kind CHECK (created_kind IS NULL OR created_kind IN ('TEACHER', 'ADMIN'));

UPDATE lessons l
SET created_kind = CASE
        WHEN EXISTS (SELECT 1 FROM user_role ur JOIN roles r ON r.id = ur.role_id
                     WHERE ur.user_id = l.draft_updated_by AND r.name = 'ADMIN') THEN 'ADMIN'
        ELSE 'TEACHER'
    END
WHERE l.in_published_version = FALSE AND l.created_kind IS NULL;
