-- ============================================================
-- V38: Muc tieu so tu moi/tuan do user tu chon
-- ============================================================
-- Muc dich: thay muc tieu fix cung 5 tu/ngay bang muc tieu rieng cua tung user.
--
-- NULL = user chua chon muc tieu -> FE hien trang chon muc tieu khi vao Tien do
-- lan dau; trong luc do he thong van tinh theo mac dinh 5 tu/ngay.
--
-- Muc tieu ngay = CEIL(weekly_goal_words / 7), duoc chot vao
-- user_daily_stats.goal_words cua tung ngay nen lich su cu khong doi.

ALTER TABLE user_profiles
    ADD COLUMN IF NOT EXISTS weekly_goal_words INT;
