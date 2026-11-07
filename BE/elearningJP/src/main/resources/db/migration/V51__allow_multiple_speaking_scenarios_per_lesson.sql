-- =============================================================================
-- Flyway Database Migration: V50__allow_multiple_speaking_scenarios_per_lesson.sql
-- Module: Curriculum & Speaking Integration (Nhiều kịch bản hội thoại trong 1 bài học)
-- Engine: PostgreSQL 15+
-- =============================================================================

-- 1. Bỏ ràng buộc 1 bài học chỉ có 1 kịch bản (nếu đã tồn tại)
ALTER TABLE lesson_speaking_scenarios DROP CONSTRAINT IF EXISTS uq_lesson_speaking_scenario;

-- 2. Bổ sung thứ tự đoạn hội thoại trong bài học (order_index: 1, 2, 3...)
ALTER TABLE lesson_speaking_scenarios ADD COLUMN IF NOT EXISTS order_index INT NOT NULL DEFAULT 1;

-- 3. Ràng buộc cặp (lesson_id, scenario_id) là duy nhất để tránh gán trùng 1 kịch bản 2 lần trong cùng 1 bài học
ALTER TABLE lesson_speaking_scenarios DROP CONSTRAINT IF EXISTS uq_lesson_speaking_scenario_item;
ALTER TABLE lesson_speaking_scenarios ADD CONSTRAINT uq_lesson_speaking_scenario_item UNIQUE(lesson_id, scenario_id);

-- 4. Đánh chỉ mục tìm kiếm và sắp xếp theo bài học & thứ tự
CREATE INDEX IF NOT EXISTS idx_lesson_speaking_scenarios_order ON lesson_speaking_scenarios(lesson_id, order_index ASC);

