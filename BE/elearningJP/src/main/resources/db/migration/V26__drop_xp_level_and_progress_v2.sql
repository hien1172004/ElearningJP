-- ============================================================
-- V26: Xoa xp_level + Progress v2 — leaderboard + goal_met logic
-- ============================================================
-- 1. Xoa cot xp_level (da bi loai khoi he thong)
-- 2. Bang app_config chua cac tham so he thong (VD: lookup_ceiling)
-- 3. Cot lessons_completed de danh so bai hoan thanh (leaderboard)
-- 4. Cap nhat goal_met: >= 1 lesson HOAC >= 1 lookup HOAC >= 5 phut hoc
-- 5. Index cho bang xep hang tuan

-- 1. Xoa cot xp_level khoi bang user_gamification.
ALTER TABLE user_gamification DROP COLUMN IF EXISTS xp_level;

-- 2. Xoa cot lookup_ceiling (thay bang bang app_config)
ALTER TABLE user_daily_stats DROP COLUMN IF EXISTS lookup_ceiling;

-- 3. Bang cau hinh he thong (key-value, global)
CREATE TABLE IF NOT EXISTS app_config (
    key  VARCHAR(64) PRIMARY KEY,
    value VARCHAR(255) NOT NULL,
    description VARCHAR(255),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Cac tham so mac dinh
INSERT INTO app_config (key, value, description) VALUES
    ('lookup_ceiling',     '20',    'So lan tra tu toi da tinh vao "tu da hoc" trong 1 ngay'),
    ('lookup_xp',          '2',     'So XP nhan khi tra tu (khi chua vuot tran)'),
    ('lesson_xp',          '10',    'So XP nhan khi hoan thanh bai hoc'),
    ('min_study_seconds',  '300',   'So giay hoc toi thieu de goal_met = true (5 phut)')
ON CONFLICT (key) DO NOTHING;

-- 4. Cot moi: dem so bai hoan thanh trong ngay (dung cho leaderboard)
ALTER TABLE user_daily_stats
    ADD COLUMN IF NOT EXISTS lessons_completed INT NOT NULL DEFAULT 0;

-- 5. Index: bang xep hang tuan
CREATE INDEX IF NOT EXISTS idx_user_daily_stats_stat_date
    ON user_daily_stats (stat_date DESC);

CREATE INDEX IF NOT EXISTS idx_user_daily_stats_week_leaderboard
    ON user_daily_stats (stat_date, lookups DESC, lessons_completed DESC)
    WHERE lookups > 0 OR lessons_completed > 0;
