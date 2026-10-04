-- =============================================================================
-- Flyway Database Migration: V1__init_schema.sql
-- Project: E-Learning Japan (elearningJP)
-- Database Engine: PostgreSQL 16
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1. USER & SECURITY MODULE
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS roles (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255),
    full_name VARCHAR(100) NOT NULL,
    avatar_url VARCHAR(500),
    auth_provider VARCHAR(20) NOT NULL DEFAULT 'LOCAL',
    provider_id VARCHAR(255),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    email_verified_at TIMESTAMPTZ,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS user_role (
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id BIGINT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE IF NOT EXISTS user_profiles (
    user_id BIGINT PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    current_level VARCHAR(10) NOT NULL DEFAULT 'BEGINNER',
    target_level VARCHAR(2) NOT NULL DEFAULT 'N5',
    target_exam_date DATE,
    daily_study_time_minutes INT NOT NULL DEFAULT 30,
    timezone VARCHAR(50) NOT NULL DEFAULT 'Asia/Ho_Chi_Minh',
    bio TEXT,
    phone_number VARCHAR(20),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS user_settings (
    user_id BIGINT PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    reminder_time TIME NOT NULL DEFAULT '20:00:00',
    push_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    notify_srs_due BOOLEAN NOT NULL DEFAULT TRUE,
    notify_streak_warning BOOLEAN NOT NULL DEFAULT TRUE,
    notify_task_reminder BOOLEAN NOT NULL DEFAULT TRUE,
    notify_exam_result BOOLEAN NOT NULL DEFAULT TRUE,
    notify_community BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

-- Seed Default Roles
INSERT INTO roles (name, description) VALUES 
('USER', 'Default learner role'),
('ADMIN', 'System administrator role')
ON CONFLICT (name) DO NOTHING;


-- -----------------------------------------------------------------------------
-- 2. CONTENT & CURRICULUM MODULE
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS courses (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    slug VARCHAR(255) NOT NULL UNIQUE,
    jlpt_level VARCHAR(2) NOT NULL,
    description TEXT,
    thumbnail_url VARCHAR(500),
    is_published BOOLEAN NOT NULL DEFAULT FALSE,
    created_by BIGINT REFERENCES users(id),
    archived_at TIMESTAMPTZ,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS lessons (
    id BIGSERIAL PRIMARY KEY,
    course_id BIGINT NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    lesson_type VARCHAR(20) NOT NULL,
    order_index INT NOT NULL,
    duration_minutes INT NOT NULL DEFAULT 15,
    content_markdown TEXT,
    archived_at TIMESTAMPTZ,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS vocabulary (
    id BIGSERIAL PRIMARY KEY,
    word VARCHAR(100) NOT NULL,
    hiragana VARCHAR(100) NOT NULL,
    sense_no SMALLINT NOT NULL DEFAULT 1,
    meaning_vi TEXT,
    meaning_en TEXT,
    jlpt_level VARCHAR(2),
    part_of_speech VARCHAR(50),
    audio_url VARCHAR(500),
    is_tts BOOLEAN NOT NULL DEFAULT FALSE,
    example_jp TEXT,
    example_vi TEXT,
    source_type VARCHAR(30) NOT NULL DEFAULT 'CORE',
    visibility VARCHAR(10) NOT NULL DEFAULT 'PUBLIC',
    review_status VARCHAR(20) NOT NULL DEFAULT 'PUBLISHED',
    ai_generated BOOLEAN NOT NULL DEFAULT FALSE,
    created_by_user_id BIGINT REFERENCES users(id),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    data_source VARCHAR(100),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS writing_characters (
    id BIGSERIAL PRIMARY KEY,
    character VARCHAR(4) NOT NULL,
    char_type VARCHAR(20) NOT NULL,
    stroke_count INT NOT NULL,
    onyomi TEXT[],
    kunyomi TEXT[],
    han_viet VARCHAR(100),
    meaning_vi VARCHAR(255),
    meaning_en VARCHAR(255),
    romaji VARCHAR(20),
    jlpt_level VARCHAR(2),
    radicals VARCHAR(50),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    data_source VARCHAR(100),
    review_status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS character_strokes (
    id BIGSERIAL PRIMARY KEY,
    character_id BIGINT NOT NULL REFERENCES writing_characters(id) ON DELETE CASCADE,
    stroke_number INT NOT NULL,
    svg_path TEXT NOT NULL,
    svg_viewbox VARCHAR(50) DEFAULT '0 0 109 109',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS grammar_points (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(150) NOT NULL,
    structure TEXT NOT NULL,
    jlpt_level VARCHAR(2) NOT NULL,
    meaning_vi VARCHAR(255) NOT NULL,
    usage_notes TEXT,
    examples_json JSONB,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS reading_passages (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    jlpt_level VARCHAR(2) NOT NULL,
    content_jp TEXT NOT NULL,
    translation_vi TEXT,
    vocab_notes_json JSONB,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS lesson_items (
    id BIGSERIAL PRIMARY KEY,
    lesson_id BIGINT NOT NULL REFERENCES lessons(id) ON DELETE CASCADE,
    item_type VARCHAR(20) NOT NULL,
    order_index INT NOT NULL,
    vocab_id BIGINT REFERENCES vocabulary(id),
    character_id BIGINT REFERENCES writing_characters(id),
    grammar_id BIGINT REFERENCES grammar_points(id),
    reading_id BIGINT REFERENCES reading_passages(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS user_lesson_progress (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    lesson_id BIGINT NOT NULL REFERENCES lessons(id) ON DELETE CASCADE,
    status VARCHAR(20) NOT NULL DEFAULT 'NOT_STARTED',
    started_at TIMESTAMPTZ,
    last_accessed_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    best_score NUMERIC(5, 2),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);


-- -----------------------------------------------------------------------------
-- 3. LEARNING PATH & DAILY TASKS
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS learning_path_templates (
    id BIGSERIAL PRIMARY KEY,
    jlpt_level VARCHAR(2) NOT NULL,
    course_id BIGINT REFERENCES courses(id),
    title VARCHAR(255) NOT NULL,
    duration_days INT NOT NULL,
    daily_vocab_quota INT NOT NULL,
    daily_kanji_quota INT NOT NULL,
    daily_grammar_quota INT NOT NULL,
    description TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS learning_paths (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    template_id BIGINT REFERENCES learning_path_templates(id),
    course_id BIGINT REFERENCES courses(id),
    target_level VARCHAR(2) NOT NULL,
    start_date DATE NOT NULL,
    target_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    total_vocab_planned INT NOT NULL DEFAULT 0,
    total_kanji_planned INT NOT NULL DEFAULT 0,
    total_grammar_planned INT NOT NULL DEFAULT 0,
    last_recalculated_at TIMESTAMPTZ,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS daily_tasks (
    id BIGSERIAL PRIMARY KEY,
    learning_path_id BIGINT NOT NULL REFERENCES learning_paths(id) ON DELETE CASCADE,
    task_date DATE NOT NULL,
    task_type VARCHAR(30) NOT NULL,
    target_count INT NOT NULL,
    completed_count INT NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    rescheduled_from_date DATE,
    completed_at TIMESTAMPTZ,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS course_enrollments (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    course_id BIGINT NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);


-- -----------------------------------------------------------------------------
-- 4. SRS (SPACED REPETITION SYSTEM) MODULE
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS srs_items (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    card_type VARCHAR(20) NOT NULL DEFAULT 'STANDARD',
    repetition_count INT NOT NULL DEFAULT 0,
    lapse_count INT NOT NULL DEFAULT 0,
    ease_factor NUMERIC(3, 2) NOT NULL DEFAULT 2.50,
    interval_days INT NOT NULL DEFAULT 0,
    next_review_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_reviewed_at TIMESTAMPTZ,
    status VARCHAR(20) NOT NULL DEFAULT 'LEARNING',
    vocab_id BIGINT REFERENCES vocabulary(id),
    character_id BIGINT REFERENCES writing_characters(id),
    grammar_id BIGINT REFERENCES grammar_points(id),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS srs_review_logs (
    id BIGSERIAL PRIMARY KEY,
    srs_item_id BIGINT NOT NULL REFERENCES srs_items(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    rating VARCHAR(10) NOT NULL,
    score_q SMALLINT NOT NULL,
    previous_interval INT NOT NULL,
    new_interval INT NOT NULL,
    previous_ef NUMERIC(3, 2) NOT NULL,
    new_ef NUMERIC(3, 2) NOT NULL,
    new_status VARCHAR(20) NOT NULL,
    response_time_ms INT,
    client_event_id UUID,
    reviewed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);


-- -----------------------------------------------------------------------------
-- 5. EXAM & EXAMINATION MODULE
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS exams (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    exam_type VARCHAR(30) NOT NULL,
    jlpt_level VARCHAR(2) NOT NULL,
    total_duration_minutes INT NOT NULL,
    total_scaled_max_score INT NOT NULL DEFAULT 180,
    overall_passing_score INT,
    total_questions INT DEFAULT 0,
    source_file_name VARCHAR(255),
    is_scanned_ocr BOOLEAN NOT NULL DEFAULT FALSE,
    is_published BOOLEAN NOT NULL DEFAULT FALSE,
    created_by BIGINT REFERENCES users(id),
    created_for_user_id BIGINT REFERENCES users(id),
    archived_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS exam_score_groups (
    id BIGSERIAL PRIMARY KEY,
    exam_id BIGINT NOT NULL REFERENCES exams(id) ON DELETE CASCADE,
    score_group_code VARCHAR(30) NOT NULL,
    title_vi VARCHAR(150) NOT NULL,
    max_scaled_score INT NOT NULL DEFAULT 60,
    passing_score INT NOT NULL DEFAULT 19,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS exam_sections (
    id BIGSERIAL PRIMARY KEY,
    exam_id BIGINT NOT NULL REFERENCES exams(id) ON DELETE CASCADE,
    score_group_id BIGINT REFERENCES exam_score_groups(id),
    session_type VARCHAR(30) NOT NULL,
    section_title VARCHAR(255) NOT NULL,
    audio_url VARCHAR(500),
    order_index INT NOT NULL,
    duration_minutes INT,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS mondai_types (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    skill_type VARCHAR(30) NOT NULL,
    name_vi VARCHAR(150) NOT NULL,
    instruction_jp TEXT,
    instruction_vi TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS questions (
    id BIGSERIAL PRIMARY KEY,
    mondai_type_id BIGINT REFERENCES mondai_types(id),
    question_text TEXT,
    passage_text TEXT,
    audio_url VARCHAR(500),
    image_url VARCHAR(500),
    explanation_vi TEXT,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS exam_section_questions (
    id BIGSERIAL PRIMARY KEY,
    exam_id BIGINT NOT NULL REFERENCES exams(id) ON DELETE CASCADE,
    section_id BIGINT NOT NULL REFERENCES exam_sections(id) ON DELETE CASCADE,
    question_id BIGINT NOT NULL REFERENCES questions(id) ON DELETE CASCADE,
    question_number INT NOT NULL,
    points NUMERIC(5, 2) NOT NULL DEFAULT 1.00,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS question_options (
    id BIGSERIAL PRIMARY KEY,
    question_id BIGINT NOT NULL REFERENCES questions(id) ON DELETE CASCADE,
    option_key SMALLINT NOT NULL,
    option_text TEXT,
    option_image_url VARCHAR(500),
    is_correct BOOLEAN NOT NULL DEFAULT FALSE,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS exam_attempts (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    exam_id BIGINT NOT NULL REFERENCES exams(id) ON DELETE CASCADE,
    started_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    submitted_at TIMESTAMPTZ,
    time_spent_seconds INT,
    total_raw_score NUMERIC(6, 2),
    total_scaled_score INT,
    is_passed BOOLEAN,
    status VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS',
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS student_answers (
    id BIGSERIAL PRIMARY KEY,
    attempt_id BIGINT NOT NULL REFERENCES exam_attempts(id) ON DELETE CASCADE,
    exam_id BIGINT NOT NULL REFERENCES exams(id) ON DELETE CASCADE,
    question_id BIGINT NOT NULL REFERENCES questions(id) ON DELETE CASCADE,
    selected_option_id BIGINT REFERENCES question_options(id),
    is_correct BOOLEAN NOT NULL DEFAULT FALSE,
    points_earned NUMERIC(5, 2) NOT NULL DEFAULT 0.00,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);


-- -----------------------------------------------------------------------------
-- 6. GAMIFICATION & NOTIFICATIONS
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS user_gamification (
    user_id BIGINT PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    total_xp BIGINT NOT NULL DEFAULT 0,
    xp_level INT NOT NULL DEFAULT 1,
    current_streak INT NOT NULL DEFAULT 0,
    longest_streak INT NOT NULL DEFAULT 0,
    last_activity_date DATE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS badges (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    title VARCHAR(100) NOT NULL,
    description VARCHAR(255) NOT NULL,
    icon_url VARCHAR(500) NOT NULL,
    xp_reward INT NOT NULL DEFAULT 50,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS user_badges (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    badge_id BIGINT NOT NULL REFERENCES badges(id) ON DELETE CASCADE,
    unlocked_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS user_daily_stats (
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    stat_date DATE NOT NULL DEFAULT CURRENT_DATE,
    study_seconds INT NOT NULL DEFAULT 0,
    cards_reviewed INT NOT NULL DEFAULT 0,
    new_items_learned INT NOT NULL DEFAULT 0,
    xp_earned INT NOT NULL DEFAULT 0,
    PRIMARY KEY (user_id, stat_date)
);

CREATE TABLE IF NOT EXISTS xp_logs (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    amount INT NOT NULL,
    source VARCHAR(30) NOT NULL,
    reference_id VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS notifications (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    body TEXT NOT NULL,
    type VARCHAR(30) NOT NULL,
    data_json JSONB,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);


-- -----------------------------------------------------------------------------
-- 7. COMMUNITY & FORUM MODULE
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS discussions (
    id BIGSERIAL PRIMARY KEY,
    lesson_id BIGINT NOT NULL REFERENCES lessons(id) ON DELETE CASCADE,
    user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    status VARCHAR(10) NOT NULL DEFAULT 'VISIBLE',
    like_count INT NOT NULL DEFAULT 0,
    reply_count INT NOT NULL DEFAULT 0,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS discussion_likes (
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    discussion_id BIGINT NOT NULL REFERENCES discussions(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, discussion_id)
);

CREATE TABLE IF NOT EXISTS discussion_replies (
    id BIGSERIAL PRIMARY KEY,
    discussion_id BIGINT NOT NULL REFERENCES discussions(id) ON DELETE CASCADE,
    user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    parent_reply_id BIGINT REFERENCES discussion_replies(id) ON DELETE CASCADE,
    content TEXT NOT NULL,
    status VARCHAR(10) NOT NULL DEFAULT 'VISIBLE',
    like_count INT NOT NULL DEFAULT 0,
    is_teacher_endorsed BOOLEAN NOT NULL DEFAULT FALSE,
    endorsed_by BIGINT REFERENCES users(id),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS discussion_reply_likes (
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    reply_id BIGINT NOT NULL REFERENCES discussion_replies(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, reply_id)
);

CREATE TABLE IF NOT EXISTS content_reports (
    id BIGSERIAL PRIMARY KEY,
    reporter_user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    discussion_id BIGINT REFERENCES discussions(id) ON DELETE CASCADE,
    reply_id BIGINT REFERENCES discussion_replies(id) ON DELETE CASCADE,
    reason VARCHAR(30) NOT NULL,
    detail TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    handled_by BIGINT REFERENCES users(id),
    handled_at TIMESTAMPTZ,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);


-- -----------------------------------------------------------------------------
-- 8. AI CHAT & HANDWRITING MODULES
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS chat_sessions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(255),
    mode VARCHAR(30) NOT NULL DEFAULT 'FREE_CONVERSATION',
    scenario_code VARCHAR(50),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    ended_at TIMESTAMPTZ,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS chat_messages (
    id BIGSERIAL PRIMARY KEY,
    session_id BIGINT NOT NULL REFERENCES chat_sessions(id) ON DELETE CASCADE,
    sender VARCHAR(10) NOT NULL,
    content_jp TEXT NOT NULL,
    translation_vi TEXT,
    romaji TEXT,
    audio_url VARCHAR(500),
    grammar_analysis_json JSONB,
    pronunciation_score NUMERIC(5, 2),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS chat_extracted_words (
    id BIGSERIAL PRIMARY KEY,
    session_id BIGINT NOT NULL REFERENCES chat_sessions(id) ON DELETE CASCADE,
    message_id BIGINT REFERENCES chat_messages(id) ON DELETE CASCADE,
    vocab_id BIGINT NOT NULL REFERENCES vocabulary(id) ON DELETE CASCADE,
    context_sentence TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'DETECTED',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS handwriting_practice_logs (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    character_id BIGINT NOT NULL REFERENCES writing_characters(id) ON DELETE CASCADE,
    overall_score NUMERIC(5, 2) NOT NULL,
    accuracy_score NUMERIC(5, 2),
    stroke_order_score NUMERIC(5, 2),
    detailed_metrics_json JSONB,
    practice_image_url VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
