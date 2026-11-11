-- =============================================================================
-- V60: Nâng cấp Snapshot Versioning cho Đề thi Public (Độc lập & Thi thử JLPT)
-- -----------------------------------------------------------------------------
-- 1. Thêm cột changelog để ghi lại lịch sử lý do cập nhật giữa các phiên bản (V1, V2...)
-- 2. Đánh index tối ưu truy vấn phiên bản đề thi
-- =============================================================================

ALTER TABLE exam_published_versions
    ADD COLUMN IF NOT EXISTS changelog VARCHAR(1000);

CREATE INDEX IF NOT EXISTS idx_exam_published_versions_exam_id
    ON exam_published_versions (exam_id);

