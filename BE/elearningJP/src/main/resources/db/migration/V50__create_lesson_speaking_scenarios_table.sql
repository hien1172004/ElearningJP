-- =============================================================================
-- Flyway Database Migration: V49__create_lesson_speaking_scenarios_table.sql
-- Module: Curriculum & Speaking Integration (Luyện nói hội thoại trong bài học)
-- Engine: PostgreSQL 15+
-- =============================================================================

-- 1. Bảng liên kết giữa Bài học (lessons) và Kịch bản luyện nói (ai_scenario)
CREATE TABLE IF NOT EXISTS lesson_speaking_scenarios (
    id BIGSERIAL PRIMARY KEY,
    lesson_id BIGINT NOT NULL REFERENCES lessons(id) ON DELETE CASCADE,
    scenario_id BIGINT NOT NULL REFERENCES ai_scenario(id) ON DELETE CASCADE,
    min_turns INT NOT NULL DEFAULT 3,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_lesson_speaking_scenario UNIQUE(lesson_id)
);

CREATE INDEX IF NOT EXISTS idx_lesson_speaking_scenarios_lesson ON lesson_speaking_scenarios(lesson_id);
CREATE INDEX IF NOT EXISTS idx_lesson_speaking_scenarios_scenario ON lesson_speaking_scenarios(scenario_id);

-- 2. Bổ sung lesson_id vào ai_session để theo dõi tiến độ luyện nói gắn liền bài học
ALTER TABLE ai_session ADD COLUMN IF NOT EXISTS lesson_id BIGINT REFERENCES lessons(id) ON DELETE SET NULL;
CREATE INDEX IF NOT EXISTS idx_ai_session_lesson_user ON ai_session(lesson_id, user_id) WHERE deleted = FALSE;
