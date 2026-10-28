-- ============================================================
-- V34: Bang user_learned_items — chong trung "từ đã học"
-- ============================================================
-- Muc dich:
-- 1) Tach "từ đã học" (MỚI) khoi "số lần tra từ" (TỔNG):
--    - user_daily_stats.lookups          = TỔNG số lần tra (kể cả tra lại từ cũ)
--    - user_daily_stats.new_items_learned = số MỤC MỚI học lần đầu
-- 2) Bang nay la "nguon su that" cho viec chong trung: mot muc chi
--    duoc dem la "đã học" DONG 1 LAN (lan dau tien).
-- 3) UNIQUE (user_id, target_type, target_key) -> INSERT ... ON CONFLICT
--    DO NOTHING tra ve 1 (moi) hoac 0 (da hoc truoc do) -> atomic, khong
--    can read-then-write (tranh race khi user tra cung 1 tu o nhieu tab).
--
-- Ghi chu: KHONG them cot moi vao user_daily_stats — da co san
-- new_items_learned (ta V1) chua duoc dung, dung lai lam "số từ mới".

-- 1. Bang user_learned_items
CREATE TABLE IF NOT EXISTS user_learned_items (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    -- VOCAB | KANJI | GRAMMAR
    target_type     VARCHAR(16)  NOT NULL,
    -- Khoa danh tinh chong trung: 'id:<targetId>' hoac 'ref:<sourceRef>'
    target_key      VARCHAR(160) NOT NULL,
    -- ID doi tuong (vocabulary.id / grammar_points.id) — NULL voi KANJI
    target_id       BIGINT,
    -- Chuoi tham chieu (ky tu Han) — NULL voi VOCAB/GRAMMAR
    source_ref      VARCHAR(100),
    first_learned_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 2. RANG buoc chong trung — nguon su that cua "từ đã học"
CREATE UNIQUE INDEX IF NOT EXISTS uq_user_learned_items_target
    ON user_learned_items (user_id, target_type, target_key);

-- 3. Index phuc vu truy van theo loai (vd: bao cao "toan bo KANJI da hoc")
CREATE INDEX IF NOT EXISTS idx_user_learned_items_user_type
    ON user_learned_items (user_id, target_type);
