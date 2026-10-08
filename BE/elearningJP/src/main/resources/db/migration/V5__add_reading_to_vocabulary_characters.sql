-- =============================================================================
-- Flyway Database Migration: V3__add_reading_to_vocabulary_characters.sql
-- Module: Mazii Expansion & System-Wide Constraints & Index Optimization
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1. MAZII DICTIONARY EXPANSION (READING FOR VOCABULARY CHARACTERS)
-- -----------------------------------------------------------------------------
ALTER TABLE vocabulary_characters
    ADD COLUMN IF NOT EXISTS reading_type VARCHAR(20),
    ADD COLUMN IF NOT EXISTS reading VARCHAR(50);

CREATE INDEX IF NOT EXISTS idx_vocab_chars_char_reading 
    ON vocabulary_characters (character_id, reading_type, reading);

-- -----------------------------------------------------------------------------
-- 2. EXTENSION CHO TÌM KIẾM TỪ ĐIỂN TIẾNG NHẬT & TIẾNG VIỆT (TRIGRAM)
-- -----------------------------------------------------------------------------
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- -----------------------------------------------------------------------------
-- 3. PARTIAL UNIQUE INDEXES (CHỐNG TRÙNG LẶP & AN TOÀN VỚI SOFT DELETE)
-- -----------------------------------------------------------------------------
-- Mỗi học viên chỉ có 1 bản ghi ghi danh cho 1 khóa học (active)
CREATE UNIQUE INDEX IF NOT EXISTS uq_course_enrollments_active 
    ON course_enrollments(user_id, course_id) 
    WHERE deleted = FALSE;

-- Mỗi học viên chỉ có 1 tiến độ cho 1 bài học (active)
CREATE UNIQUE INDEX IF NOT EXISTS uq_user_lesson_progress_active 
    ON user_lesson_progress(user_id, lesson_id) 
    WHERE deleted = FALSE;

-- Học viên không nhận trùng 1 huy hiệu nhiều lần
CREATE UNIQUE INDEX IF NOT EXISTS uq_user_badges_active 
    ON user_badges(user_id, badge_id) 
    WHERE deleted = FALSE;

-- Trong 1 lần thi, mỗi câu hỏi chỉ có 1 câu trả lời của thí sinh
CREATE UNIQUE INDEX IF NOT EXISTS uq_student_answers_active 
    ON student_answers(attempt_id, question_id) 
    WHERE deleted = FALSE;

-- Thứ tự nét vẽ KanjiVG của 1 chữ Hán không được trùng số nét
CREATE UNIQUE INDEX IF NOT EXISTS uq_char_strokes_number 
    ON character_strokes(character_id, stroke_number);

-- Chống tạo trùng flashcard SRS cho cùng 1 từ vựng / kanji / ngữ pháp
CREATE UNIQUE INDEX IF NOT EXISTS uq_srs_items_user_card_vocab 
    ON srs_items(user_id, card_type, vocab_id) 
    WHERE vocab_id IS NOT NULL AND deleted = FALSE;

CREATE UNIQUE INDEX IF NOT EXISTS uq_srs_items_user_card_char 
    ON srs_items(user_id, card_type, character_id) 
    WHERE character_id IS NOT NULL AND deleted = FALSE;

CREATE UNIQUE INDEX IF NOT EXISTS uq_srs_items_user_card_grammar 
    ON srs_items(user_id, card_type, grammar_id) 
    WHERE grammar_id IS NOT NULL AND deleted = FALSE;

-- Ràng buộc không trùng câu hỏi trong cùng 1 section đề thi
CREATE UNIQUE INDEX IF NOT EXISTS uq_section_question_active 
    ON exam_section_questions(section_id, question_id) 
    WHERE deleted = FALSE;

-- Ràng buộc không trùng mã đáp án (1, 2, 3, 4) trong cùng 1 câu hỏi
CREATE UNIQUE INDEX IF NOT EXISTS uq_question_option_key_active 
    ON question_options(question_id, option_key) 
    WHERE deleted = FALSE;

-- -----------------------------------------------------------------------------
-- 4. TỐI ƯU TRUY VẤN SRS (SPACED REPETITION SYSTEM HOTSPOT)
-- -----------------------------------------------------------------------------
-- Truy vấn các thẻ cần ôn tập hôm nay của người dùng
CREATE INDEX IF NOT EXISTS idx_srs_items_user_due 
    ON srs_items(user_id, next_review_at, status) 
    WHERE deleted = FALSE;

-- Truy vấn lịch sử ôn tập & thống kê học tập
CREATE INDEX IF NOT EXISTS idx_srs_logs_user_reviewed 
    ON srs_review_logs(user_id, reviewed_at DESC);

CREATE INDEX IF NOT EXISTS idx_srs_logs_item 
    ON srs_review_logs(srs_item_id);

-- -----------------------------------------------------------------------------
-- 5. INDEX CÁC FOREIGN KEYS & CỘT TRUY VẤN TẦN SUẤT CAO
-- -----------------------------------------------------------------------------
CREATE INDEX IF NOT EXISTS idx_exam_attempts_user_exam 
    ON exam_attempts(user_id, exam_id);

CREATE INDEX IF NOT EXISTS idx_student_answers_attempt 
    ON student_answers(attempt_id);

CREATE INDEX IF NOT EXISTS idx_chat_messages_session 
    ON chat_messages(session_id, created_at ASC);

CREATE INDEX IF NOT EXISTS idx_notifications_user_unread 
    ON notifications(user_id, is_read) 
    WHERE deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_lessons_course_order 
    ON lessons(course_id, order_index) 
    WHERE deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_lesson_items_lesson 
    ON lesson_items(lesson_id, order_index);

CREATE INDEX IF NOT EXISTS idx_daily_tasks_path_date 
    ON daily_tasks(learning_path_id, task_date);

-- -----------------------------------------------------------------------------
-- 6. GIN INDEX CHO TÌM KIẾM TỪ ĐIỂN MAZII (MẢNG ON/KUN & TEXT SEARCH)
-- -----------------------------------------------------------------------------
-- Hỗ trợ tra Kanji theo âm On / âm Kun trong mảng TEXT[]
CREATE INDEX IF NOT EXISTS idx_char_onyomi_gin 
    ON writing_characters USING gin (onyomi);

CREATE INDEX IF NOT EXISTS idx_char_kunyomi_gin 
    ON writing_characters USING gin (kunyomi);

-- Hỗ trợ tìm kiếm từ vựng và nghĩa tiếng Việt bằng Trigram (LIKE '%...%')
CREATE INDEX IF NOT EXISTS idx_vocab_word_trgm 
    ON vocabulary USING gin (word gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_vocab_meaning_vi_trgm 
    ON vocabulary USING gin (meaning_vi gin_trgm_ops);
