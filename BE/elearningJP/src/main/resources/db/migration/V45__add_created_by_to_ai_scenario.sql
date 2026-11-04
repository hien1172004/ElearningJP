-- =============================================================================
-- V45: Ghi nhận người tạo kịch bản luyện nói (ai_scenario.created_by)
-- -----------------------------------------------------------------------------
-- Mục đích: phục vụ trang quản lý kịch bản — ADMIN quản lý mọi kịch bản, TEACHER chỉ
-- quản lý kịch bản do chính mình tạo, nên cần biết ai đã tạo từng kịch bản.
--
-- - NULL = kịch bản hệ thống (seed V30) hoặc kịch bản do khách chưa đăng nhập tạo.
-- - ON DELETE SET NULL: xóa tài khoản không kéo theo xóa kịch bản đang được dùng.
-- - Không backfill: các kịch bản hiện có không xác định được người tạo.
-- =============================================================================

ALTER TABLE ai_scenario
    ADD COLUMN IF NOT EXISTS created_by BIGINT REFERENCES users (id) ON DELETE SET NULL;

-- Lọc danh sách quản lý theo người tạo (TEACHER chỉ xem kịch bản của mình)
CREATE INDEX IF NOT EXISTS idx_ai_scenario_created_by ON ai_scenario (created_by);
