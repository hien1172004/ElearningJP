-- ============================================================
-- V25: Progress module — extend user_daily_stats + create study_events
-- ============================================================
-- Goals:
-- 1) Track "từ đã học" = dictionary lookups (1 lần tra = 1 từ đã học)
-- 2) Enable top-lookups aggregation from append-only study_events
-- 3) Backfill daily goal_met boolean for streak calendar

-- 1. Mo rong user_daily_stats voi cac cot moi cho Progress page
ALTER TABLE user_daily_stats
    ADD COLUMN IF NOT EXISTS lookups          INT     NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS goal_met         BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS goal_words       INT     NOT NULL DEFAULT 5,
    ADD COLUMN IF NOT EXISTS last_lookup_at   TIMESTAMPTZ;

-- Partial index phuc vu trang Progress: scan nhanh theo (user, date range)
CREATE INDEX IF NOT EXISTS idx_user_daily_stats_user_date
    ON user_daily_stats (user_id, stat_date DESC);

-- 2. Fix schema xp_logs cho khop voi entity XpLog (SoftDeletableEntity)
--    V1 tao bang sai: source/reference_id (VARCHAR) + thieu updated_at/version/deleted.
--    Hibernate ddl-auto=none nen phai sua thu cong o day.
ALTER TABLE xp_logs
    ADD COLUMN IF NOT EXISTS source_type  VARCHAR(30),
    ADD COLUMN IF NOT EXISTS source_ref_id BIGINT,
    ADD COLUMN IF NOT EXISTS updated_at   TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN IF NOT EXISTS deleted       BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS deleted_at    TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS version       BIGINT NOT NULL DEFAULT 0;

-- Entity XpLog khong map cot `source` cu, nen phai bo rang buoc NOT NULL
-- neu giu thi moi lan INSERT tu Hibernate se bi loi NOT NULL violation.
ALTER TABLE xp_logs ALTER COLUMN source DROP NOT NULL;

-- Backfill: cot moi se duoc gan CURRENT_TIMESTAMP cho ca row cu,
-- nen sua lai cho khop created_at de audit trail dung chieu.
UPDATE xp_logs SET source_type = source WHERE source_type IS NULL;
UPDATE xp_logs SET updated_at  = created_at;

CREATE INDEX IF NOT EXISTS idx_xp_logs_user_created
    ON xp_logs (user_id, created_at DESC);

-- Dong bo CHECK enum (XpSource): chi nhan cac gia tri hop le
ALTER TABLE xp_logs DROP CONSTRAINT IF EXISTS xp_logs_source_check;
ALTER TABLE xp_logs ADD CONSTRAINT xp_logs_source_check
    CHECK (source_type IS NULL OR source_type IN
        ('LESSON_COMPLETE', 'SRS_REVIEW', 'DAILY_TASK', 'EXAM',
         'HANDWRITING', 'BADGE', 'STREAK_BONUS', 'OTHER'));

-- 3. Bang study_events — append-only, phan tich theo su kien
CREATE TABLE IF NOT EXISTS study_events (
    id            BIGSERIAL PRIMARY KEY,
    user_id       BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    event_type    VARCHAR(24) NOT NULL,
    target_type   VARCHAR(16),                 -- VOCAB | KANJI | GRAMMAR | NULL (lesson/heartbeat)
    target_id     BIGINT,
    xp_awarded    INT  NOT NULL DEFAULT 0,
    source_ref    VARCHAR(100),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Tra cuu theo user + time
CREATE INDEX IF NOT EXISTS idx_study_events_user_created
    ON study_events (user_id, created_at DESC);

-- Top-lookups: COUNT(*) theo target (user, type, target_id)
CREATE INDEX IF NOT EXISTS idx_study_events_lookup
    ON study_events (user_id, event_type, target_type, target_id)
    WHERE event_type = 'LOOKUP';

-- Lesson-complete feed
CREATE INDEX IF NOT EXISTS idx_study_events_lesson
    ON study_events (user_id, event_type, created_at DESC)
    WHERE event_type = 'LESSON_COMPLETE';
