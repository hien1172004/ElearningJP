-- =====================================================================
-- V61: Người / thời điểm sửa bản đang soạn gần nhất của khóa học
-- ---------------------------------------------------------------------
-- Mục đích: phân biệt "giáo viên đang soạn, chưa gửi duyệt" với "chờ duyệt" và cho ADMIN
-- biết ai vừa sửa (vd: "Đang soạn bởi Nguyễn A · 10 phút trước") trước khi bấm Lưu — Lưu chốt
-- toàn bộ bản đang soạn, kể cả phần giáo viên đang sửa dở.
--
-- Bản đang soạn dùng chung cho mọi người soạn → chỉ ghi được người sửa GẦN NHẤT.
--   - draft_updated_by: người sửa gần nhất (tài khoản bị xóa → NULL).
--   - draft_updated_by_name: tên chụp lúc sửa — hiển thị ở danh sách khóa không phải join users
--     (tránh N+1); đổi tên thì cập nhật ở lần sửa sau.
--   - draft_updated_at: thời điểm sửa gần nhất (ghi gộp tối đa ~1 lần/phút/người).
-- Dữ liệu cũ để NULL (không biết ai sửa trước đây).
-- =====================================================================

ALTER TABLE courses
    ADD COLUMN IF NOT EXISTS draft_updated_by BIGINT REFERENCES users (id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS draft_updated_by_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS draft_updated_at TIMESTAMPTZ;

CREATE INDEX IF NOT EXISTS idx_courses_draft_updated_by ON courses (draft_updated_by);
