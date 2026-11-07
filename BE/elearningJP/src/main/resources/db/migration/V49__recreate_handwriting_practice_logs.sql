-- =============================================================================
-- V49: Lịch sử luyện viết Kanji (handwriting_practice_logs) — dựng lại bảng
-- -----------------------------------------------------------------------------
-- Bảng tạo từ V1 chưa từng được ghi dữ liệu (không có repository/API nào dùng) và
-- cấu trúc không khớp entity, nên tạo lại theo nhu cầu luyện viết trong bài học:
--
-- - character: lưu thẳng ký tự (chữ trong bài học thuộc lesson_kanji_items, chữ ở
--   từ điển có thể không nằm trong writing_characters → không ràng buộc FK ký tự)
-- - lesson_id: luyện trong bài nào (NULL = luyện ở từ điển / sổ tay)
-- - practice_mode: GUIDED (Hanzi-Writer, điểm theo số lần sai) | FREEHAND (AI KanjiVG)
-- - input_mode: MOUSE (chuột/cảm ứng) | AIR (viết bằng tay qua camera, ngưỡng chấm nới)
--   → 2 thang điểm / 2 ngưỡng khác nhau, thống kê phải tách theo 2 cột này.
-- =============================================================================

DROP TABLE IF EXISTS handwriting_practice_logs;

CREATE TABLE handwriting_practice_logs (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    lesson_id BIGINT REFERENCES lessons (id) ON DELETE SET NULL,
    character VARCHAR(8) NOT NULL,
    practice_mode VARCHAR(10) NOT NULL CHECK (practice_mode IN ('GUIDED', 'FREEHAND')),
    input_mode VARCHAR(10) NOT NULL CHECK (input_mode IN ('MOUSE', 'AIR')),
    total_score NUMERIC(5, 2) NOT NULL CHECK (total_score BETWEEN 0 AND 100),
    stroke_count_expected INT,
    stroke_count_drawn INT,
    total_mistakes INT,
    is_passed BOOLEAN NOT NULL DEFAULT FALSE,
    detail_json JSONB,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

-- Tổng hợp điểm của user cho 1 chữ (trong 1 bài học hoặc ngoài bài học)
CREATE INDEX IF NOT EXISTS idx_hw_logs_user_char ON handwriting_practice_logs (user_id, character, lesson_id);
-- Thống kê theo bài học (giáo viên xem tiến độ luyện viết)
CREATE INDEX IF NOT EXISTS idx_hw_logs_lesson ON handwriting_practice_logs (lesson_id, created_at DESC);
-- Lịch sử của user, mới nhất trước
CREATE INDEX IF NOT EXISTS idx_hw_logs_user_created ON handwriting_practice_logs (user_id, created_at DESC);
