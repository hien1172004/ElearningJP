-- =============================================================================
-- V63: Autosave nội dung chưa lưu theo người và theo bài (docs/lesson-review-design.md mục 5)
-- -----------------------------------------------------------------------------
-- Mục đích: form đang soạn dở (item từ vựng / ngữ pháp / chữ Hán, câu hỏi quiz, section, thông tin bài)
-- được lưu tạm lên server để F5 / đóng tab / bị xung đột không mất phần đã gõ.
--
--   - Mỗi (người dùng, bài, form_key) 1 dòng; form_key do FE đặt, vd "vocab:345", "vocab:new:12", "quiz:new".
--   - Không tăng version nội dung bài, không ai khác thấy (kể cả ADMIN), vẫn ghi được khi bài đang chờ duyệt
--     hoặc đang được ADMIN sửa. Giáo viên đã bị gỡ khỏi khóa thì không ghi / đọc được (kiểm ở service).
--   - Chỉ xóa khi lưu thành công hoặc người dùng bấm "Bỏ"; job dọn bản không đụng tới quá 30 ngày.
-- =============================================================================

CREATE TABLE IF NOT EXISTS lesson_autosaves (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    lesson_id BIGINT NOT NULL REFERENCES lessons (id) ON DELETE CASCADE,
    form_key VARCHAR(100) NOT NULL,
    content JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_lesson_autosaves_user_lesson_form UNIQUE (user_id, lesson_id, form_key)
);

-- Dọn bản cũ (job hằng ngày)
CREATE INDEX IF NOT EXISTS idx_lesson_autosaves_updated_at ON lesson_autosaves (updated_at);
-- Xóa theo bài khi bài bị xóa cứng đã có ON DELETE CASCADE; đọc theo (user, lesson) dùng unique index
