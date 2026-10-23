-- ============================================================
-- V27: Tach lookup thanh lookups (tong) va lookups_scored (da tinh diem)
-- ============================================================
-- Muc dich:
-- 1. Cot lookups_scored: so lan tra tu da duoc tinh diem (co tran theo ngay)
-- 2. Cot lookups: tong so lan tra tu (khong tran, de hien thi day du)
-- 3. Leaderboard tinh diem theo: lookups_scored * lookup_points + lessons_completed * lesson_points
-- 4. Xu ly nhu sau:
--    - study_events: ghi nhan moi lan tra (lookups)
--    - daily_stats.lookups: tang moi lan tra
--    - daily_stats.lookups_scored: chi tang khi < ceiling
--    - Redis ZINCRBY: chi goi khi lookups_scored tang (luc nao cung sau commit)

-- 1. Them cot lookups_scored
ALTER TABLE user_daily_stats
    ADD COLUMN IF NOT EXISTS lookups_scored INT NOT NULL DEFAULT 0;

-- 2. Cap nhat app_config: them lookup_points, lesson_points (neu chua co)
INSERT INTO app_config (key, value, description) VALUES
    ('lookup_points', '1', 'So diem cong vao leaderboard cho 1 lan tra tu (khi chua vuot tran)'),
    ('lesson_points', '1', 'So diem cong vao leaderboard khi hoan thanh bai hoc')
ON CONFLICT (key) DO NOTHING;

-- 3. Index cho lookups_scored (phuc vu leaderboard query)
CREATE INDEX IF NOT EXISTS idx_user_daily_stats_lookups_scored
    ON user_daily_stats (stat_date, lookups_scored DESC);
