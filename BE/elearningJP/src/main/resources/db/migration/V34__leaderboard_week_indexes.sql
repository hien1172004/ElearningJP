-- ============================================================
-- V28: Index cho leaderboard tuần (Redis Sorted Set là index dẫn xuất từ DB)
-- ============================================================
-- Muc dich:
-- 1. Index (stat_date, user_id) phuc vu GROUP BY user_id theo tuần
--    cho cac query:
--      - findAllScoresByWeek          (rebuild ZSET tu DB)
--      - findTopUsersByWeek            (du phong)
--      - countUsersWithScoreInWeek    (ZCARD fallback)
--      - countUsersWithScoreGreaterThan (ZCOUNT fallback)
--    Co index nay, Postgres quet theo stat_date roi GROUP BY user_id
--    khong can sort lai — tranh "sort tuoc" tren tap du lieu lon.
--
-- 2. Partial index chi lay dong co diem > 0 — dung cho HAVING score > 0
--    trong cac query leaderboard, giam so dong phai xu ly.
--
-- Khong them cot moi — chi them index.

-- 1. Index (stat_date, user_id) cho group-by theo tuần
CREATE INDEX IF NOT EXISTS idx_user_daily_stats_date_user
    ON user_daily_stats (stat_date, user_id);

-- 2. Partial index: dong co diem > 0 (co lookups_scored hoac lessons_completed)
CREATE INDEX IF NOT EXISTS idx_user_daily_stats_week_scored
    ON user_daily_stats (stat_date, user_id)
    WHERE lookups_scored > 0 OR lessons_completed > 0;
