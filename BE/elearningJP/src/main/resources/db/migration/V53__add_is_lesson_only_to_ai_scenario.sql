-- Migration V52: Phân tách kịch bản hội thoại bài học và kịch bản sảnh luyện nói tự do
ALTER TABLE ai_scenario ADD COLUMN IF NOT EXISTS is_lesson_only BOOLEAN NOT NULL DEFAULT FALSE;

-- Cập nhật các kịch bản AI tự tạo cho bài học (LESSON_PRACTICE) thành is_lesson_only = TRUE
UPDATE ai_scenario SET is_lesson_only = TRUE WHERE topic_id = 'LESSON_PRACTICE';

CREATE INDEX IF NOT EXISTS idx_ai_scenario_lesson_only ON ai_scenario(is_lesson_only);

