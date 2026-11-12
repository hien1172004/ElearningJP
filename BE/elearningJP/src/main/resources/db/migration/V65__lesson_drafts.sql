-- =============================================================================
-- V65: Tách bản nháp theo phía — nháp GIÁO VIÊN và nháp ADMIN (docs/lesson-review-design.md mục 8)
-- -----------------------------------------------------------------------------
-- Mục đích: ADMIN không thấy phần giáo viên chưa gửi duyệt (và ngược lại). Mỗi bài luôn có đúng
-- 1 nháp TEACHER (chung cho mọi giáo viên được giao) và 1 nháp ADMIN (chung cho mọi ADMIN); hai phía
-- sửa độc lập, ai xuất bản trước thì phía kia (nếu đã sửa) phải "cập nhật bản mới".
--
-- lesson_drafts:
--   kind               TEACHER | ADMIN.
--   status             EDITING | SUBMITTED (giáo viên đã gửi duyệt — chỉ đọc) | ARCHIVED (nháp cũ chỉ đọc
--                      giữ lại khi "cập nhật bản mới", để người dùng tự chép phần còn thiếu).
--   base_live_revision lessons.live_revision lúc nháp được tạo từ live; lệch với live hiện tại → nháp đã cũ,
--                      gửi duyệt / lưu bị chặn (LESSON_DRAFT_OUTDATED).
--   has_changes        nháp khác bản live mà nó dựa trên (đã ghi ít nhất 1 lần).
--   draft_revision     tăng mỗi lần ghi nội dung; gửi duyệt phải khớp số này (thay lessons.draft_revision).
--   needs_import       nháp chưa có nội dung, cần dựng từ bản live (LessonDraftBackfill khi khởi động).
--
-- lessons.live_revision: tăng mỗi lần nội dung live của bài đổi (duyệt / ADMIN lưu / lưu cả khóa).
--
-- lesson_sections / lesson_quizzes / lesson_speaking_scenarios: thêm draft_id (cha mới của cây nội dung);
-- lesson_id giữ lại (danh tính bài, truy vấn theo khóa). Unique theo bài chuyển thành unique theo nháp.
--
-- origin_id trên mọi bảng nội dung: id ổn định của phần tử trong bản live. Bản chụp ghi
-- COALESCE(origin_id, id) → id học viên thấy không đổi dù phía nào xuất bản. Dòng cũ: NULL (= id của chính nó).
--
-- lesson_review_requests: draft_id (nháp được gửi), content (bản chụp lúc gửi — duyệt xuất bản đúng bản này),
-- base_live_revision (bản live lúc gửi). Yêu cầu cũ chưa có content → duyệt chụp từ nháp (đang bị khóa).
--
-- Dữ liệu cũ (cách A): nháp chung hiện tại → nháp TEACHER; nháp ADMIN dựng lại từ bản live.
-- Các cột duyệt theo bài trên lessons (V62) không còn được ghi; giữ lại tới khi bỏ hẳn.
-- =============================================================================

CREATE TABLE IF NOT EXISTS lesson_drafts (
    id BIGSERIAL PRIMARY KEY,
    lesson_id BIGINT NOT NULL REFERENCES lessons (id) ON DELETE CASCADE,
    kind VARCHAR(10) NOT NULL CHECK (kind IN ('TEACHER', 'ADMIN')),
    status VARCHAR(20) NOT NULL DEFAULT 'EDITING' CHECK (status IN ('EDITING', 'SUBMITTED', 'ARCHIVED')),
    base_live_revision BIGINT NOT NULL DEFAULT 0,
    has_changes BOOLEAN NOT NULL DEFAULT FALSE,
    draft_revision BIGINT NOT NULL DEFAULT 0,
    needs_import BOOLEAN NOT NULL DEFAULT FALSE,
    updated_by BIGINT REFERENCES users (id) ON DELETE SET NULL,
    updated_by_name VARCHAR(255),
    content_updated_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

-- Mỗi bài, mỗi phía: tối đa 1 nháp đang dùng (EDITING / SUBMITTED) và 1 nháp cũ chỉ đọc
CREATE UNIQUE INDEX IF NOT EXISTS uq_lesson_drafts_active
    ON lesson_drafts (lesson_id, kind) WHERE status <> 'ARCHIVED';
CREATE UNIQUE INDEX IF NOT EXISTS uq_lesson_drafts_archived
    ON lesson_drafts (lesson_id, kind) WHERE status = 'ARCHIVED';
CREATE INDEX IF NOT EXISTS idx_lesson_drafts_needs_import ON lesson_drafts (id) WHERE needs_import = TRUE;

ALTER TABLE lessons ADD COLUMN IF NOT EXISTS live_revision BIGINT NOT NULL DEFAULT 0;

-- ---------------------------------------------------------------------------
-- 1. Nháp TEACHER = nháp chung hiện tại (mọi bài, kể cả bài đã xóa mềm — dòng nội dung cần cha)
-- ---------------------------------------------------------------------------
INSERT INTO lesson_drafts (lesson_id, kind, status, base_live_revision, has_changes, draft_revision,
                           updated_by, updated_by_name, content_updated_at)
SELECT l.id, 'TEACHER',
       CASE WHEN l.review_status = 'PENDING_REVIEW' THEN 'SUBMITTED' ELSE 'EDITING' END,
       0, l.has_unpublished_changes, l.draft_revision,
       l.draft_updated_by, l.draft_updated_by_name, l.draft_updated_at
FROM lessons l
WHERE NOT EXISTS (SELECT 1 FROM lesson_drafts d WHERE d.lesson_id = l.id AND d.kind = 'TEACHER');

-- 2. Nháp ADMIN: dựng từ bản live khi khởi động (bài chưa từng duyệt → nháp trống)
INSERT INTO lesson_drafts (lesson_id, kind, status, base_live_revision, has_changes, needs_import)
SELECT l.id, 'ADMIN', 'EDITING', 0, FALSE, l.in_published_version
FROM lessons l
WHERE NOT EXISTS (SELECT 1 FROM lesson_drafts d WHERE d.lesson_id = l.id AND d.kind = 'ADMIN');

-- ---------------------------------------------------------------------------
-- 3. draft_id trên các bảng cha của cây nội dung → trỏ về nháp TEACHER
-- ---------------------------------------------------------------------------
ALTER TABLE lesson_sections ADD COLUMN IF NOT EXISTS draft_id BIGINT REFERENCES lesson_drafts (id) ON DELETE CASCADE;
ALTER TABLE lesson_quizzes ADD COLUMN IF NOT EXISTS draft_id BIGINT REFERENCES lesson_drafts (id) ON DELETE CASCADE;
ALTER TABLE lesson_speaking_scenarios ADD COLUMN IF NOT EXISTS draft_id BIGINT REFERENCES lesson_drafts (id) ON DELETE CASCADE;

UPDATE lesson_sections s SET draft_id = d.id
FROM lesson_drafts d WHERE d.lesson_id = s.lesson_id AND d.kind = 'TEACHER' AND s.draft_id IS NULL;
UPDATE lesson_quizzes q SET draft_id = d.id
FROM lesson_drafts d WHERE d.lesson_id = q.lesson_id AND d.kind = 'TEACHER' AND q.draft_id IS NULL;
UPDATE lesson_speaking_scenarios sp SET draft_id = d.id
FROM lesson_drafts d WHERE d.lesson_id = sp.lesson_id AND d.kind = 'TEACHER' AND sp.draft_id IS NULL;

ALTER TABLE lesson_sections ALTER COLUMN draft_id SET NOT NULL;
ALTER TABLE lesson_quizzes ALTER COLUMN draft_id SET NOT NULL;
ALTER TABLE lesson_speaking_scenarios ALTER COLUMN draft_id SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_lesson_sections_draft ON lesson_sections (draft_id);
CREATE INDEX IF NOT EXISTS idx_lesson_quizzes_draft ON lesson_quizzes (draft_id);
CREATE INDEX IF NOT EXISTS idx_lesson_speaking_scenarios_draft ON lesson_speaking_scenarios (draft_id, order_index);

-- Unique theo bài → theo nháp (2 nháp của cùng bài có thứ tự / quiz / kịch bản riêng)
DROP INDEX IF EXISTS uq_lesson_sections_lesson_order_active;
CREATE UNIQUE INDEX IF NOT EXISTS uq_lesson_sections_draft_order_active
    ON lesson_sections (draft_id, order_index) WHERE deleted = FALSE;

ALTER TABLE lesson_quizzes DROP CONSTRAINT IF EXISTS lesson_quizzes_lesson_id_key;
ALTER TABLE lesson_quizzes DROP CONSTRAINT IF EXISTS uq_lesson_quizzes_draft;
ALTER TABLE lesson_quizzes ADD CONSTRAINT uq_lesson_quizzes_draft UNIQUE (draft_id);
CREATE INDEX IF NOT EXISTS idx_lesson_quizzes_lesson ON lesson_quizzes (lesson_id);

ALTER TABLE lesson_speaking_scenarios DROP CONSTRAINT IF EXISTS uq_lesson_speaking_scenario_item;
ALTER TABLE lesson_speaking_scenarios DROP CONSTRAINT IF EXISTS uq_lesson_speaking_scenario_draft_item;
ALTER TABLE lesson_speaking_scenarios
    ADD CONSTRAINT uq_lesson_speaking_scenario_draft_item UNIQUE (draft_id, scenario_id);

-- ---------------------------------------------------------------------------
-- 4. origin_id — id ổn định trong bản live
-- ---------------------------------------------------------------------------
ALTER TABLE lesson_sections ADD COLUMN IF NOT EXISTS origin_id BIGINT;
ALTER TABLE lesson_kanji_items ADD COLUMN IF NOT EXISTS origin_id BIGINT;
ALTER TABLE lesson_kanji_related_vocabulary ADD COLUMN IF NOT EXISTS origin_id BIGINT;
ALTER TABLE lesson_vocabulary_items ADD COLUMN IF NOT EXISTS origin_id BIGINT;
ALTER TABLE lesson_vocabulary_examples ADD COLUMN IF NOT EXISTS origin_id BIGINT;
ALTER TABLE lesson_grammar_items ADD COLUMN IF NOT EXISTS origin_id BIGINT;
ALTER TABLE lesson_grammar_examples ADD COLUMN IF NOT EXISTS origin_id BIGINT;
ALTER TABLE lesson_quizzes ADD COLUMN IF NOT EXISTS origin_id BIGINT;
ALTER TABLE quiz_questions ADD COLUMN IF NOT EXISTS origin_id BIGINT;
ALTER TABLE quiz_options ADD COLUMN IF NOT EXISTS origin_id BIGINT;
ALTER TABLE lesson_speaking_scenarios ADD COLUMN IF NOT EXISTS origin_id BIGINT;

-- ---------------------------------------------------------------------------
-- 5. Yêu cầu duyệt theo bài: nháp được gửi + bản chụp lúc gửi
-- ---------------------------------------------------------------------------
ALTER TABLE lesson_review_requests
    ADD COLUMN IF NOT EXISTS draft_id BIGINT REFERENCES lesson_drafts (id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS content JSONB,
    ADD COLUMN IF NOT EXISTS base_live_revision BIGINT;

UPDATE lesson_review_requests r SET draft_id = d.id, base_live_revision = 0
FROM lesson_drafts d WHERE d.lesson_id = r.lesson_id AND d.kind = 'TEACHER' AND r.draft_id IS NULL;
