-- ============================================================
-- V37: Doi cong thuc diem leaderboard sang "tu MOI da hoc"
-- ============================================================
-- Muc dich:
-- 1) Diem xep hang tuan truoc day:
--      score = lookups_scored * lookup_points + lessons_completed * lesson_points
--    nay doi thanh:
--      score = new_items_learned * learned_item_points + lessons_completed * lesson_points
--
-- 2) Vi sao doi:
--    - lookups_scored bi gioi han boi lookup_ceiling (20/ngay) → diem bi "tran",
--      va con phu thuoc vao viec tra lai tu cu (dem ca lan tra trung).
--    - new_items_learned = so MUC MOI hoc lan dau (nguon: user_learned_items,
--      UNIQUE constraint) → do "hoc that" chinh xac, khong the spam (moi muc chi
--      tinh 1 lan), va khong bi tran theo ngay.
--
-- 3) lookups_scored van duoc GIU LAI va tiep tuc duoc ghi — nhung chi con dung de
--    gioi han XP (lookup_ceiling), KHONG con tham gia tinh diem leaderboard.
--
-- 4) Redis key bump v2 -> v3 o tang code (LeaderboardService.KEY_PREFIX) vi y nghia
--    score thay doi. Key v2 tu het TTL 56 ngay.

-- 1. Them app_config key diem cho 1 tu MOI da hoc
INSERT INTO app_config (key, value, description) VALUES
    ('learned_item_points', '1', 'So diem cong vao leaderboard cho 1 tu MOI da hoc (muc hoc lan dau)')
ON CONFLICT (key) DO NOTHING;

-- 2. Partial index cho query leaderboard: chi lay dong co dong gop diem
--    (tu moi hoc hoac bai hoc hoan thanh).
CREATE INDEX IF NOT EXISTS idx_user_daily_stats_week_learned
    ON user_daily_stats (stat_date, user_id)
    WHERE new_items_learned > 0 OR lessons_completed > 0;

-- 3. Xoa partial index cu (tao o V34) — dieu kien loc theo lookups_scored, khong
--    con dung cho cong thuc diem moi nen khong bao gio duoc Postgres chon.
DROP INDEX IF EXISTS idx_user_daily_stats_week_scored;
