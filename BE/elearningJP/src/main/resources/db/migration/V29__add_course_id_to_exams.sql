-- =============================================================================
-- V29: ADD COURSE_ID TO EXAMS (SUPPORT MIDTERM & FINAL COURSE EXAMS)
-- =============================================================================

ALTER TABLE exams
    ADD COLUMN IF NOT EXISTS course_id BIGINT REFERENCES courses(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_exams_course_id ON exams(course_id);
