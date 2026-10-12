-- =============================================================================
-- Flyway Database Migration: V13__add_quiz_question_types.sql
-- Module: Multi-format Lesson Quiz (Multiple choice, Matching, Fill-in-the-blank)
-- =============================================================================

-- 1. Bổ sung các cột vào bảng quiz_questions để hỗ trợ dạng Matching và Tự điền đáp án
ALTER TABLE quiz_questions 
    ADD COLUMN IF NOT EXISTS question_type VARCHAR(30) NOT NULL DEFAULT 'MULTIPLE_CHOICE',
    ADD COLUMN IF NOT EXISTS correct_text_answer TEXT,
    ADD COLUMN IF NOT EXISTS matching_data JSONB;

-- 2. Thêm cột match_target vào quiz_options (dùng cho dạng MATCHING nếu lưu dạng options)
ALTER TABLE quiz_options 
    ADD COLUMN IF NOT EXISTS match_key VARCHAR(100);

-- 3. Đánh index hỗ trợ truy vấn lọc theo dạng câu hỏi
CREATE INDEX IF NOT EXISTS idx_quiz_questions_type ON quiz_questions(question_type);
