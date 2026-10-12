-- =============================================================================
-- Flyway Database Migration: V19__add_lesson_vocab_item_overrides.sql
-- Module: Lesson Vocabulary Items — thêm trường override cho romaji + audio
-- =============================================================================
-- Thêm 2 cột override mới vào lesson_vocabulary_items:
--   - custom_romaji:   giáo viên ghi đè romaji (ưu tiên nếu khác NULL)
--   - custom_audio_url: giáo viên ghi đè audio URL
--
-- Semantics:
--   - Nếu cột override = NULL → hiển thị lấy từ bảng vocabulary (gốc)
--   - Nếu cột override có giá trị → ưu tiên dùng override
--   - Gửi rỗng từ FE được service map sang NULL (xoá override)
-- =============================================================================

ALTER TABLE lesson_vocabulary_items
    ADD COLUMN IF NOT EXISTS custom_romaji VARCHAR(150);

ALTER TABLE lesson_vocabulary_items
    ADD COLUMN IF NOT EXISTS custom_audio_url VARCHAR(500);
