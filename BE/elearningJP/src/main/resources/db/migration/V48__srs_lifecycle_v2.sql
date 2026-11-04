-- -----------------------------------------------------------------------------
-- V48: Vòng đời thẻ SRS v2
--   * Trạng thái mới: NEW, RELEARNING, ARCHIVED (cột status là VARCHAR — không cần đổi kiểu)
--   * introduced_at: thời điểm thẻ được ôn lần đầu → giới hạn số thẻ mới mỗi ngày
--   * Chuẩn hoá dữ liệu cũ theo ngưỡng mới (REVIEWING ≥ 6 ngày, MASTERED ≥ 21 ngày, tối đa 365 ngày)
-- -----------------------------------------------------------------------------

ALTER TABLE srs_items ADD COLUMN IF NOT EXISTS introduced_at TIMESTAMPTZ;

ALTER TABLE srs_items ALTER COLUMN status SET DEFAULT 'NEW';

-- Thẻ chưa từng ôn → NEW
UPDATE srs_items
SET status = 'NEW'
WHERE deleted = FALSE
  AND last_reviewed_at IS NULL
  AND repetition_count = 0
  AND lapse_count = 0;

-- Thẻ đã ôn: lấy thời điểm ôn đầu tiên từ log (fallback last_reviewed_at)
UPDATE srs_items s
SET introduced_at = COALESCE(
        (SELECT MIN(l.reviewed_at) FROM srs_review_logs l WHERE l.srs_item_id = s.id),
        s.last_reviewed_at)
WHERE s.status <> 'NEW'
  AND s.introduced_at IS NULL;

-- Chặn trần interval
UPDATE srs_items SET interval_days = 365 WHERE interval_days > 365;

-- Tính lại trạng thái theo ngưỡng mới (chỉ thẻ đã ôn)
UPDATE srs_items
SET status = CASE
        WHEN interval_days >= 21 THEN 'MASTERED'
        WHEN interval_days >= 6  THEN 'REVIEWING'
        WHEN lapse_count > 0     THEN 'RELEARNING'
        ELSE 'LEARNING'
    END
WHERE deleted = FALSE
  AND status IN ('LEARNING', 'REVIEWING', 'MASTERED')
  AND last_reviewed_at IS NOT NULL;

-- Index phục vụ job lưu trữ thẻ quá hạn & đếm thẻ mới trong ngày
CREATE INDEX IF NOT EXISTS idx_srs_items_status_next_review
    ON srs_items (status, next_review_at)
    WHERE deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_srs_items_user_introduced
    ON srs_items (user_id, introduced_at)
    WHERE deleted = FALSE;
