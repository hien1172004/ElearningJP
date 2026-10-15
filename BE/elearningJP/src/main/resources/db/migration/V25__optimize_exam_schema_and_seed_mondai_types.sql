-- =============================================================================
-- V25: OPTIMIZE EXAM SCHEMA, CREATE ATTEMPT GROUP SCORES & SEED MONDAI TYPES
-- =============================================================================

-- 1. Sync exam_score_groups column names with Java Entity & drop redundant title_vi
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'exam_score_groups' AND column_name = 'score_group_code'
    ) THEN
        ALTER TABLE exam_score_groups RENAME COLUMN score_group_code TO group_code;
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'exam_score_groups' AND column_name = 'passing_score'
    ) THEN
        ALTER TABLE exam_score_groups RENAME COLUMN passing_score TO min_required_score;
    END IF;
END $$;

ALTER TABLE exam_score_groups 
    DROP COLUMN IF EXISTS title_vi;

-- 2. Update exams table: add soft-delete columns & drop redundant created_for_user_id
ALTER TABLE exams
    ADD COLUMN IF NOT EXISTS deleted BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ;

ALTER TABLE exams
    DROP COLUMN IF EXISTS created_for_user_id;

-- 3. Update reading_passages table: add passage_type, image_url & drop unused vocab_notes_json
ALTER TABLE reading_passages
    ADD COLUMN IF NOT EXISTS passage_type VARCHAR(30) NOT NULL DEFAULT 'TEXT',
    ADD COLUMN IF NOT EXISTS image_url VARCHAR(500);

ALTER TABLE reading_passages
    DROP COLUMN IF EXISTS vocab_notes_json;

-- 4. Update questions table: add audio seek & visual fields; drop legacy redundant columns
ALTER TABLE questions
    ADD COLUMN IF NOT EXISTS jlpt_level VARCHAR(2),
    ADD COLUMN IF NOT EXISTS skill_type VARCHAR(30),
    ADD COLUMN IF NOT EXISTS passage_id BIGINT REFERENCES reading_passages(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS audio_play_limit INT NOT NULL DEFAULT 1,
    ADD COLUMN IF NOT EXISTS audio_start_time INT,
    ADD COLUMN IF NOT EXISTS audio_end_time INT,
    ADD COLUMN IF NOT EXISTS transcript_jp TEXT,
    ADD COLUMN IF NOT EXISTS transcript_vi TEXT,
    ADD COLUMN IF NOT EXISTS star_position SMALLINT,
    ADD COLUMN IF NOT EXISTS has_visual_options BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS is_active BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS created_by BIGINT REFERENCES users(id) ON DELETE SET NULL;

-- Ensure explanation_vi is the single canonical column for question explanation
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'questions' AND column_name = 'explanation_vi'
    ) THEN
        ALTER TABLE questions ADD COLUMN explanation_vi TEXT;
    END IF;
END $$;

-- Drop obsolete & redundant columns in questions
ALTER TABLE questions
    DROP COLUMN IF EXISTS passage_text,
    DROP COLUMN IF EXISTS ai_solved,
    DROP COLUMN IF EXISTS difficulty,
    DROP COLUMN IF EXISTS review_status,
    DROP COLUMN IF EXISTS tags,
    DROP COLUMN IF EXISTS explanation;

CREATE INDEX IF NOT EXISTS idx_questions_mondai ON questions(mondai_type_id) WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_questions_passage ON questions(passage_id) WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_questions_jlpt_skill ON questions(jlpt_level, skill_type) WHERE deleted = FALSE;

-- 5. Update exam_attempts table: support section-based practice & duration
ALTER TABLE exam_attempts
    ADD COLUMN IF NOT EXISTS attempt_type VARCHAR(30) NOT NULL DEFAULT 'FULL_EXAM',
    ADD COLUMN IF NOT EXISTS section_id BIGINT REFERENCES exam_sections(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS target_duration_minutes INT;

CREATE INDEX IF NOT EXISTS idx_exam_attempts_user ON exam_attempts(user_id) WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_exam_attempts_exam ON exam_attempts(exam_id) WHERE deleted = FALSE;

-- 6. Update student_answers table: add ordered_options_json & audio_play_count
ALTER TABLE student_answers
    ADD COLUMN IF NOT EXISTS ordered_options_json JSONB,
    ADD COLUMN IF NOT EXISTS audio_play_count INT NOT NULL DEFAULT 0;

CREATE INDEX IF NOT EXISTS idx_student_answers_attempt ON student_answers(attempt_id) WHERE deleted = FALSE;

-- 7. Create attempt_group_scores table (chấm điểm & điểm liệt từng nhóm)
CREATE TABLE IF NOT EXISTS attempt_group_scores (
    id BIGSERIAL PRIMARY KEY,
    attempt_id BIGINT NOT NULL REFERENCES exam_attempts(id) ON DELETE CASCADE,
    exam_id BIGINT NOT NULL REFERENCES exams(id) ON DELETE CASCADE,
    score_group_id BIGINT NOT NULL REFERENCES exam_score_groups(id) ON DELETE CASCADE,
    raw_correct_count INT NOT NULL DEFAULT 0,
    raw_total_count INT NOT NULL DEFAULT 0,
    scaled_score INT NOT NULL DEFAULT 0,
    is_group_passed BOOLEAN NOT NULL DEFAULT FALSE,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_attempt_group_scores_attempt ON attempt_group_scores(attempt_id) WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_attempt_group_scores_exam ON attempt_group_scores(exam_id) WHERE deleted = FALSE;

-- 8. Seed standard JLPT Mondai Types
INSERT INTO mondai_types (code, skill_type, name_vi, instruction_jp, instruction_vi, is_active)
VALUES
    -- Vocabulary (文字・語彙)
    ('KANJI_READING', 'VOCABULARY', 'Cách đọc Kanji', '____の ことばは どう かきますか。', 'Từ gạch chân đọc như thế nào? Chọn đáp án đúng nhất trong 1, 2, 3, 4.', TRUE),
    ('ORTHOGRAPHY', 'VOCABULARY', 'Cách viết Kanji / Hiragana', '____の ことばの 漢字(ひらがな)は どう かきますか。', 'Chữ gạch chân viết Kanji (hoặc Hiragana) như thế nào?', TRUE),
    ('WORD_FORMATION', 'VOCABULARY', 'Cấu tạo từ (Tiền tố / Hậu tố)', '____に 入る もっとも よい ものを 1つ えらびなさい。', 'Chọn từ/tiền tố/hậu tố thích hợp nhất điền vào chỗ trống.', TRUE),
    ('CONTEXTUAL_USE', 'VOCABULARY', 'Ngữ cảnh phù hợp', '( )に なにを 入れますか。いちばん いい ものを ひとつ えらんで ください。', 'Điền từ thích hợp nhất vào ngữ cảnh câu văn.', TRUE),
    ('SYNONYMS', 'VOCABULARY', 'Từ gần nghĩa', '____の ぶんと だいたい おなじ いみの ぶんが あります。', 'Tìm câu có ý nghĩa tương đương với câu chứa từ gạch chân.', TRUE),
    ('PROPER_USAGE', 'VOCABULARY', 'Cách dùng từ đúng', 'つぎの ことばの つかいかたで いちばん いい ものを ひとつ えらんで ください。', 'Chọn câu sử dụng từ đề bài tự nhiên và chính xác nhất.', TRUE),

    -- Grammar (文法)
    ('SENTENTIAL_GRAMMAR', 'GRAMMAR', 'Ngữ pháp câu', 'つぎの ぶんの ( )に なにを 入れますか。', 'Chọn dạng ngữ pháp thích hợp điền vào chỗ trống.', TRUE),
    ('SENTENCE_COMPOSITION', 'GRAMMAR', 'Sắp xếp câu (Dấu sao ★)', 'つぎの ぶんの ★に 入る ものは どれですか。', 'Sắp xếp 4 phần vào câu. Chọn đáp án ở vị trí dấu ★.', TRUE),
    ('TEXT_GRAMMAR', 'GRAMMAR', 'Ngữ pháp đoạn văn', 'つぎの 文章を 読んで、文章全体の 内容を 考えて ( )に 入る ものを えらびなさい。', 'Đọc đoạn văn và chọn dạng liên từ/ngữ pháp phù hợp cho các chỗ trống.', TRUE),

    -- Reading (読解)
    ('SHORT_PASSAGE', 'READING', 'Đoạn văn ngắn', 'つぎの 文章を 読んで、質問に こたえて ください。', 'Đọc bài văn ngắn và trả lời câu hỏi.', TRUE),
    ('MEDIUM_PASSAGE', 'READING', 'Đoạn văn trung', 'つぎの 文章を 読んで、質問に こたえて ください。', 'Đọc bài văn vừa và trả lời các câu hỏi liên quan.', TRUE),
    ('LONG_PASSAGE', 'READING', 'Đoạn văn dài', 'つぎの 文章を 読んで、質問に こたえて ください。', 'Đọc bài văn dài và trả lời các câu hỏi.', TRUE),
    ('COMPARISON_INTEGRATED', 'READING', 'So sánh đối chiếu (N1/N2)', 'AとBの 文章を 読んで、あとの 問いに こたえなさい。', 'Đọc hai văn bản A và B và so sánh quan điểm của hai tác giả.', TRUE),
    ('THEMATIC_LONG', 'READING', 'Nắm bắt chủ kiến (Trường văn)', 'つぎの 文章を 読んで、筆者の 主張を とらえなさい。', 'Đọc bài luận dài và xác định quan điểm/chủ kiến của tác giả.', TRUE),
    ('INFORMATION_RETRIEVAL', 'READING', 'Tìm kiếm thông tin (Tờ rơi/Quảng cáo)', '右の（下の）案内を 見て、質問に こたえて ください。', 'Quan sát tờ rơi, bảng thông báo hoặc biển quảng cáo để tìm thông tin theo yêu cầu.', TRUE),

    -- Listening (聴解)
    ('TASK_BASED', 'LISTENING', 'Hiểu nhiệm vụ', 'まず しつもんを きいて ください。それから はなしを きいて、問題用紙の 1から4の なかから、いちばん いい ものを ひとつ えらんで ください。', 'Nghe câu hỏi trước, sau đó nghe hội thoại và chọn việc nhân vật sẽ làm tiếp theo.', TRUE),
    ('KEY_POINT', 'LISTENING', 'Hiểu điểm mấu chốt', 'まず しつもんを きいて ください。そのあと 問題用紙を みて ください。読む 時間が あります。それから はなしを きいて...', 'Nghe câu hỏi, đọc trước các đáp án trên đề giấy, sau đó nghe và chọn đáp án then chốt.', TRUE),
    ('GENERAL_OUTLINE', 'LISTENING', 'Hiểu khái quát', '問題用紙に 何も 印刷されて いません。まず はなしを きいて ください。それから、しつもんを きいて...', 'Đề giấy không in nội dung. Nghe toàn bộ chủ đề/nội dung khái quát của đoạn văn và chọn đáp án.', TRUE),
    ('UTTERANCE_EXPRESSIONS', 'LISTENING', 'Diễn đạt lời nói (Tranh mũi tên)', 'えを 見ながら、しつもんを きいて ください。やじるし（➡）の ひとは なんと いいますか。', 'Nhìn tranh tình huống. Người có mũi tên chỉ vào sẽ nói câu gì trong tình huống đó?', TRUE),
    ('QUICK_RESPONSE', 'LISTENING', 'Phản xạ nhanh (3 lựa chọn)', '問題用紙に 何も 印刷されて いません。短い 文を きいて、返事を ひとつ えらんで ください。', 'Nghe câu thoại ngắn và chọn câu đáp lại phù hợp nhất trong 3 lựa chọn (1, 2, 3).', TRUE),
    ('INTEGRATED_COMPREHENSION', 'LISTENING', 'Nghe tích hợp (N1/N2)', 'これから 長い はなしを ききます。メモを とっても かまいません。', 'Nghe đoạn hội thoại dài của nhiều người bàn luận và trả lời 2-3 câu hỏi liên quan.', TRUE)
ON CONFLICT (code) DO UPDATE SET
    name_vi = EXCLUDED.name_vi,
    instruction_jp = EXCLUDED.instruction_jp,
    instruction_vi = EXCLUDED.instruction_vi,
    skill_type = EXCLUDED.skill_type,
    is_active = EXCLUDED.is_active;
