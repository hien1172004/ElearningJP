-- -----------------------------------------------------------------------------
-- V4: Add indexes and constraints for SRS (Spaced Repetition System) optimization
-- -----------------------------------------------------------------------------

-- 1. Composite Partial Index for querying active due cards by user
CREATE INDEX IF NOT EXISTS idx_srs_items_user_due 
ON srs_items (user_id, next_review_at) 
WHERE deleted = FALSE;

-- 2. Partial Unique Indexes to prevent duplicate active cards per user
CREATE UNIQUE INDEX IF NOT EXISTS uq_srs_items_user_vocab 
ON srs_items (user_id, vocab_id) 
WHERE vocab_id IS NOT NULL AND deleted = FALSE;

CREATE UNIQUE INDEX IF NOT EXISTS uq_srs_items_user_character 
ON srs_items (user_id, character_id) 
WHERE character_id IS NOT NULL AND deleted = FALSE;

CREATE UNIQUE INDEX IF NOT EXISTS uq_srs_items_user_grammar 
ON srs_items (user_id, grammar_id) 
WHERE grammar_id IS NOT NULL AND deleted = FALSE;

-- 3. Indexes for review logs analytics and history tracking
CREATE INDEX IF NOT EXISTS idx_srs_logs_user_reviewed 
ON srs_review_logs (user_id, reviewed_at);

CREATE INDEX IF NOT EXISTS idx_srs_logs_item_id 
ON srs_review_logs (srs_item_id);
