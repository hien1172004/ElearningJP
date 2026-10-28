-- =============================================================================
-- seed_courses_n5_n4.sql — Tạo dataset Course + Lesson cho JLPT N5 và N4
-- =============================================================================
-- Mục đích
--   Sinh dữ liệu mẫu cho module Curriculum (courses → lessons → sections →
--   items) + Quiz, lấy nội dung tự động từ kho từ điển đã có sẵn:
--       vocabulary         (N5 = 626, N4 = 596)
--       writing_characters (char_type = 'KANJI'; N5 = 79, N4 = 166)
--       grammar_points     (N5 = 81, N4 = 98)
--
-- Vì sao KHÔNG đặt file này trong src/main/resources/db/migration
--   Flyway sẽ tính checksum và áp dụng lúc app start. Script này có tính
--   chất phá huỷ (DELETE rồi INSERT) nên không phù hợp với migration bất biến.
--
-- Đặc tính
--   - IDEMPOTENT: chạy lại bao nhiêu lần cũng cho cùng kết quả.
--   - DETERMINISTIC: luôn chọn cùng dữ liệu nhờ ORDER BY id + row_number.
--   - AN TOÀN: bọc trong 1 transaction; bước 13 tự validate rồi mới COMMIT.
--
-- CÁCH CHẠY
--   (a) Qua Docker — copy file vào container rồi chạy psql (khuyến nghị,
--       tránh lỗi encoding tiếng Việt khi pipe qua PowerShell):
--         docker cp scripts/db-seed/seed_courses_n5_n4.sql elearning_postgres:/tmp/seed.sql
--         docker exec elearning_postgres psql -U postgres -d elearning_db \
--           -v ON_ERROR_STOP=1 -f /tmp/seed.sql
--
--   (b) Qua psql cục bộ (xem BE/elearningJP/.env cho URL/credentials):
--         psql "postgresql://postgres:<pwd>@localhost:5433/elearning_db" \
--           -v ON_ERROR_STOP=1 -f scripts/db-seed/seed_courses_n5_n4.sql
--
-- HOÀN TÁC (xoá sạch dataset vừa seed)
--       DELETE FROM courses
--        WHERE slug IN ('jlpt-n5-khoa-hoc-co-ban', 'jlpt-n4-khoa-hoc-co-ban');
--   (FK đều ON DELETE CASCADE nên lesson/section/item/quiz/question/option
--    cùng enrollment/progress của dataset này đều bị xoá theo.)
--
-- LƯU Ý
--   learning_paths.course_id và learning_path_templates.course_id là
--   ON DELETE NO ACTION. Nếu sau này gán course_id trỏ tới 2 slug ở trên
--   cho hai bảng đó, câu DELETE ở bước 3 sẽ bị chặn. Hiện tại cả hai đang rỗng.
-- =============================================================================

\set ON_ERROR_STOP on

SET client_encoding = 'UTF8';

BEGIN;

-- -----------------------------------------------------------------------------
-- THAM SỐ CẤU HÌNH
--   Đổi các biến psql bên dưới để đổi quy mô dataset mà không phải viết lại SQL.
-- -----------------------------------------------------------------------------
\set kanji_per_lesson     10
\set vocab_per_lesson     12
\set grammar_per_lesson    3
\set questions_per_lesson  5
\set passing_score        80

-- Những tham số này cũng được đọc lại từ _seed_param bởi khối kiểm tra ở bước 13
-- (psql không nội suy biến :var bên trong khối dollar-quoted $$ ... $$).
CREATE TEMP TABLE _seed_param ON COMMIT DROP AS
SELECT :kanji_per_lesson::int     AS kanji_per_lesson,
       :vocab_per_lesson::int     AS vocab_per_lesson,
       :grammar_per_lesson::int   AS grammar_per_lesson,
       :questions_per_lesson::int AS questions_per_lesson,
       :passing_score::int        AS passing_score;


-- -----------------------------------------------------------------------------
-- BƯỚC 1 — Bảng kế hoạch cho 2 khóa học
-- -----------------------------------------------------------------------------
CREATE TEMP TABLE _seed_course (
    slug        TEXT PRIMARY KEY,
    title       TEXT NOT NULL,
    jlpt_level  TEXT NOT NULL,
    description TEXT
) ON COMMIT DROP;

INSERT INTO _seed_course (slug, title, jlpt_level, description) VALUES
('jlpt-n5-khoa-hoc-co-ban',
 'JLPT N5 — Khóa học cơ bản',
 'N5',
 'Khóa học dành cho người mới bắt đầu: làm quen chữ Hán số cơ bản, từ vựng và ngữ pháp câu đơn giản. Mỗi bài gồm 10 kanji, 12 từ vựng, 3 điểm ngữ pháp và 5 câu kiểm tra, lấy tự động từ kho từ điển N5 của hệ thống.'),
('jlpt-n4-khoa-hoc-co-ban',
 'JLPT N4 — Khóa học cơ bản',
 'N4',
 'Khóa học trung cấp: mở rộng từ vựng, chữ Hán và ngữ pháp so với N5, luyện cách diễn đạt câu dài hơn. Mỗi bài gồm 10 kanji, 12 từ vựng, 3 điểm ngữ pháp và 5 câu kiểm tra, lấy tự động từ kho từ điển N4 của hệ thống.');


-- -----------------------------------------------------------------------------
-- BƯỚC 2 — Bảng kế hoạch cho 6 bài học (3 bài / khóa)
-- -----------------------------------------------------------------------------
CREATE TEMP TABLE _seed_lesson (
    course_slug TEXT    NOT NULL,
    lesson_no   INTEGER NOT NULL,
    title       TEXT    NOT NULL,
    PRIMARY KEY (course_slug, lesson_no)
) ON COMMIT DROP;

INSERT INTO _seed_lesson (course_slug, lesson_no, title) VALUES
('jlpt-n5-khoa-hoc-co-ban', 1, 'Bài 1 — Kanji, từ vựng và ngữ pháp nền tảng'),
('jlpt-n5-khoa-hoc-co-ban', 2, 'Bài 2 — Mở rộng vốn từ và câu đơn giản'),
('jlpt-n5-khoa-hoc-co-ban', 3, 'Bài 3 — Tổng hợp và luyện tập'),
('jlpt-n4-khoa-hoc-co-ban', 1, 'Bài 1 — Kanji, từ vựng và ngữ pháp trung cấp'),
('jlpt-n4-khoa-hoc-co-ban', 2, 'Bài 2 — Cấu trúc câu và từ vựng mở rộng'),
('jlpt-n4-khoa-hoc-co-ban', 3, 'Bài 3 — Tổng hợp và luyện tập');

-- 3 section cho mỗi lesson; thứ tự hiển thị trên FE
CREATE TEMP TABLE _seed_section_type (
    section_type TEXT PRIMARY KEY,
    title        TEXT    NOT NULL,
    ord          INTEGER NOT NULL
) ON COMMIT DROP;

INSERT INTO _seed_section_type (section_type, title, ord) VALUES
('KANJI',      'Kanji',    1),
('VOCABULARY', 'Từ vựng',  2),
('GRAMMAR',    'Ngữ pháp', 3);


-- -----------------------------------------------------------------------------
-- BƯỚC 3 — Dọn dẹp (idempotent)
--   Xoá 2 course theo slug. Toàn bộ cây con bị CASCADE:
--     courses  → lessons (CASCADE)
--             → lesson_sections, lesson_quizzes, user_lesson_progress (CASCADE)
--             → lesson_kanji_items / lesson_vocabulary_items / lesson_grammar_items
--             → lesson_vocabulary_examples / lesson_grammar_examples
--             → quiz_questions → quiz_options
--             → course_enrollments, lesson_items
-- -----------------------------------------------------------------------------
DELETE FROM courses
 WHERE slug IN (SELECT slug FROM _seed_course);


-- -----------------------------------------------------------------------------
-- BƯỚC 4 — Insert 2 course
--   created_by lấy theo email admin (không hardcode id); không tìm thấy thì NULL.
-- -----------------------------------------------------------------------------
INSERT INTO courses (title, slug, jlpt_level, description, is_published, created_by)
SELECT sc.title,
       sc.slug,
       sc.jlpt_level,
       sc.description,
       TRUE,
       (SELECT u.id FROM users u WHERE u.email = 'admin@gmail.com' LIMIT 1)
  FROM _seed_course sc;


-- -----------------------------------------------------------------------------
-- BƯỚC 5 — Insert 6 lesson
--   order_index chạy 1..3 trong từng course → thoả uq_lessons_course_order_active.
-- -----------------------------------------------------------------------------
INSERT INTO lessons (course_id, title, order_index)
SELECT c.id, sl.title, sl.lesson_no
  FROM _seed_lesson sl
  JOIN courses c ON c.slug = sl.course_slug
 ORDER BY sl.course_slug, sl.lesson_no;

-- Ghi lại id bài học để các bước sau dùng (không hardcode id).
CREATE TEMP TABLE _seed_lesson_id ON COMMIT DROP AS
SELECT sl.course_slug,
       sl.lesson_no,
       le.id AS lesson_id
  FROM _seed_lesson sl
  JOIN courses c  ON c.slug = sl.course_slug
  JOIN lessons le ON le.course_id = c.id
                AND le.title     = sl.title
                AND le.deleted   = FALSE;


-- -----------------------------------------------------------------------------
-- BƯỚC 6 — Insert section (KANJI / VOCABULARY / GRAMMAR) cho mỗi lesson
--   order_index 1..3 → thoả uq_lesson_sections_lesson_order_active.
-- -----------------------------------------------------------------------------
INSERT INTO lesson_sections (lesson_id, section_type, title, order_index)
SELECT li.lesson_id, st.section_type, st.title, st.ord
  FROM _seed_lesson_id li
 CROSS JOIN _seed_section_type st
 ORDER BY li.course_slug, li.lesson_no, st.ord;

CREATE TEMP TABLE _seed_section_id ON COMMIT DROP AS
SELECT li.course_slug,
       li.lesson_no,
       li.lesson_id,
       s.section_type,
       s.id AS section_id
  FROM _seed_lesson_id li
  JOIN lesson_sections s ON s.lesson_id = li.lesson_id
                        AND s.deleted   = FALSE;


-- -----------------------------------------------------------------------------
-- BƯỚC 7 — Chia đều dictionary thành các "lát cắt" theo row_number
--   Mỗi pool được đánh số 1..total theo id ASC (deterministic); bài học n
--   nhận lát (n-1)*quota + 1 .. n*quota.
-- -----------------------------------------------------------------------------

-- 7.1. Pool chữ Hán
CREATE TEMP TABLE _seed_kanji_pool ON COMMIT DROP AS
SELECT jlpt_level,
       id AS character_id,
       row_number() OVER (PARTITION BY jlpt_level ORDER BY id) AS rn
  FROM writing_characters
 WHERE char_type = 'KANJI'
   AND deleted = FALSE
   AND jlpt_level IN ('N5', 'N4');

-- 7.2. Pool từ vựng (chỉ sense 1, đang active, có nghĩa tiếng Việt)
CREATE TEMP TABLE _seed_vocab_pool ON COMMIT DROP AS
SELECT jlpt_level,
       id AS vocab_id,
       word,
       hiragana,
       romaji,
       meaning_vi,
       row_number() OVER (PARTITION BY jlpt_level ORDER BY id) AS rn,
       count(*)     OVER (PARTITION BY jlpt_level)              AS total
  FROM vocabulary
 WHERE deleted = FALSE
   AND is_active = TRUE
   AND sense_no = 1
   AND jlpt_level IN ('N5', 'N4')
   AND nullif(btrim(meaning_vi), '') IS NOT NULL;

-- 7.3. Pool ngữ pháp
CREATE TEMP TABLE _seed_grammar_pool ON COMMIT DROP AS
SELECT jlpt_level,
       id AS grammar_id,
       row_number() OVER (PARTITION BY jlpt_level ORDER BY id) AS rn
  FROM grammar_points
 WHERE deleted = FALSE
   AND jlpt_level IN ('N5', 'N4');

-- 7.4. Pool NGHĨA duy nhất theo level — dùng làm nguồn sinh đáp án sai.
--      Vì mỗi meaning_vi chỉ xuất hiện đúng 1 lần, lấy 3 dòng ở 3 vị trí khác
--      nhau là 3 đáp án sai chắc chắn khác nhau và khác đáp án đúng.
CREATE TEMP TABLE _seed_meaning_pool ON COMMIT DROP AS
SELECT jlpt_level,
       meaning_vi,
       min(vocab_id) AS vocab_id,
       row_number() OVER (PARTITION BY jlpt_level ORDER BY min(vocab_id)) AS rn,
       count(*)     OVER (PARTITION BY jlpt_level)                           AS total
  FROM _seed_vocab_pool
 GROUP BY jlpt_level, meaning_vi;


-- -----------------------------------------------------------------------------
-- BƯỚC 8 — Insert item cho từng section
--   order_index = rn - (lesson_no - 1) * quota → luôn chạy 1..quota.
-- -----------------------------------------------------------------------------

-- 8.1. KANJI
INSERT INTO lesson_kanji_items (section_id, character_id, order_index)
SELECT si.section_id, kp.character_id, kp.rn - (si.lesson_no - 1) * :kanji_per_lesson
  FROM _seed_section_id si
  JOIN courses          c  ON c.slug = si.course_slug
  JOIN _seed_kanji_pool kp ON kp.jlpt_level = c.jlpt_level
 WHERE si.section_type = 'KANJI'
   AND kp.rn BETWEEN (si.lesson_no - 1) * :kanji_per_lesson + 1
                 AND si.lesson_no * :kanji_per_lesson
 ORDER BY si.course_slug, si.lesson_no, kp.rn;

-- 8.2. VOCABULARY
INSERT INTO lesson_vocabulary_items (section_id, vocab_id, order_index)
SELECT si.section_id, vp.vocab_id, vp.rn - (si.lesson_no - 1) * :vocab_per_lesson
  FROM _seed_section_id si
  JOIN courses          c  ON c.slug = si.course_slug
  JOIN _seed_vocab_pool vp ON vp.jlpt_level = c.jlpt_level
 WHERE si.section_type = 'VOCABULARY'
   AND vp.rn BETWEEN (si.lesson_no - 1) * :vocab_per_lesson + 1
                 AND si.lesson_no * :vocab_per_lesson
 ORDER BY si.course_slug, si.lesson_no, vp.rn;

-- 8.3. GRAMMAR
INSERT INTO lesson_grammar_items (section_id, grammar_id, order_index)
SELECT si.section_id, gp.grammar_id, gp.rn - (si.lesson_no - 1) * :grammar_per_lesson
  FROM _seed_section_id si
  JOIN courses           c  ON c.slug = si.course_slug
  JOIN _seed_grammar_pool gp ON gp.jlpt_level = c.jlpt_level
 WHERE si.section_type = 'GRAMMAR'
   AND gp.rn BETWEEN (si.lesson_no - 1) * :grammar_per_lesson + 1
                 AND si.lesson_no * :grammar_per_lesson
 ORDER BY si.course_slug, si.lesson_no, gp.rn;


-- -----------------------------------------------------------------------------
-- BƯỚC 9 — Sinh ví dụ cho item từ dữ liệu gốc trong từ điển
-- -----------------------------------------------------------------------------

-- 9.1. lesson_vocabulary_examples — lấy example_jp / example_vi của vocabulary
INSERT INTO lesson_vocabulary_examples
       (vocab_item_id, example_jp, example_vi, example_romaji, audio_url, order_index, is_primary)
SELECT lvi.id, v.example_jp, v.example_vi, v.romaji, v.audio_url, 1, TRUE
  FROM lesson_vocabulary_items lvi
  JOIN _seed_section_id si ON si.section_id = lvi.section_id
  JOIN vocabulary        v  ON v.id = lvi.vocab_id
 WHERE si.section_type = 'VOCABULARY'
   AND nullif(btrim(v.example_jp), '') IS NOT NULL
   AND nullif(btrim(v.example_vi), '') IS NOT NULL
 ORDER BY si.course_slug, si.lesson_no, lvi.order_index;

-- 9.2. lesson_grammar_examples — lấy phần tử đầu của examples_json
--      (mảng JSON [{"jp": ..., "vi": ...}, ...]).
INSERT INTO lesson_grammar_examples (grammar_item_id, example_jp, example_vi, order_index)
SELECT lgi.id, gp.examples_json -> 0 ->> 'jp', gp.examples_json -> 0 ->> 'vi', 1
  FROM lesson_grammar_items lgi
  JOIN _seed_section_id si ON si.section_id = lgi.section_id
  JOIN grammar_points     gp ON gp.id = lgi.grammar_id
 WHERE si.section_type = 'GRAMMAR'
   AND jsonb_typeof(gp.examples_json) = 'array'
   AND jsonb_array_length(gp.examples_json) > 0
   AND nullif(btrim(gp.examples_json -> 0 ->> 'jp'), '') IS NOT NULL
   AND nullif(btrim(gp.examples_json -> 0 ->> 'vi'), '') IS NOT NULL
 ORDER BY si.course_slug, si.lesson_no, lgi.order_index;


-- -----------------------------------------------------------------------------
-- BƯỚC 10 — Tạo quiz cho mỗi lesson (1 quiz / lesson theo UNIQUE lesson_id)
-- -----------------------------------------------------------------------------
INSERT INTO lesson_quizzes (lesson_id, title, description, passing_score_percent)
SELECT li.lesson_id,
       'Kiểm tra — ' || le.title,
       'Bài trắc nghiệm ôn lại các từ vựng đã học trong bài. Đạt '
       || :passing_score || '% trở lên để hoàn thành bài học.',
       :passing_score
  FROM _seed_lesson_id li
  JOIN lessons le ON le.id = li.lesson_id
 ORDER BY li.course_slug, li.lesson_no;

CREATE TEMP TABLE _seed_quiz_id ON COMMIT DROP AS
SELECT li.lesson_id, lq.id AS quiz_id
  FROM _seed_lesson_id li
  JOIN lesson_quizzes lq ON lq.lesson_id = li.lesson_id
                         AND lq.deleted   = FALSE;


-- -----------------------------------------------------------------------------
-- BƯỚC 11 — Sinh câu hỏi MCQ
--   Nguồn: :questions_per_lesson từ vựng đầu tiên của section VOCABULARY.
-- -----------------------------------------------------------------------------
CREATE TEMP TABLE _seed_quiz_q ON COMMIT DROP AS
SELECT si.lesson_id,
       si.lesson_no,
       vp.jlpt_level,
       vp.rn        AS word_rn,
       vp.total,
       lvi.order_index AS qno,
       vp.vocab_id,
       vp.word,
       vp.hiragana,
       vp.meaning_vi
  FROM _seed_section_id         si
  JOIN lesson_vocabulary_items lvi ON lvi.section_id = si.section_id
  JOIN _seed_vocab_pool         vp ON vp.vocab_id = lvi.vocab_id
 WHERE si.section_type = 'VOCABULARY'
   AND lvi.order_index <= :questions_per_lesson;

INSERT INTO quiz_questions
       (quiz_id, question_text, explanation, order_index, vocab_id, question_type)
SELECT qi.quiz_id,
       'Nghĩa của từ 「' || q.word || '」(' || q.hiragana || ') là gì?',
       '「' || q.word || '」đọc là ' || q.hiragana
       || ', nghĩa là ' || q.meaning_vi || '.',
       q.qno,
       q.vocab_id,
       'MULTIPLE_CHOICE'
  FROM _seed_quiz_q   q
  JOIN _seed_quiz_id qi ON qi.lesson_id = q.lesson_id
 ORDER BY q.lesson_id, q.qno;


-- -----------------------------------------------------------------------------
-- BƯỚC 12 — Sinh 4 phương án cho mỗi câu
--   - 1 đáp án đúng: meaning_vi của từ trong câu.
--   - 3 đáp án sai: lấy từ _seed_meaning_pool — mỗi nghĩa trong pool xuất
--     hiện đúng 1 lần nên 3 dòng đầu là 3 nghĩa chắc chắn khác nhau và khác
--     đáp án đúng. Cách chọn: bắt đầu từ vị trí CÁCH đáp án đúng một đoạn rồi
--     quanh vòng tròn — nhờ vậy đáp án sai không rơi vào đúng các câu lân
--     cận (tránh trùng nghĩa với nhau ở góc nhìn người học).
--   - Vị trí đáp án đúng xoay theo qno: 1,2,3,4,1,2,3,4... (tránh học thuộc
--     vị trí). 3 đáp án sai lấy 3 slot trống còn lại theo thứ tự tăng dần.
-- -----------------------------------------------------------------------------
CREATE TEMP TABLE _seed_quiz_opt ON COMMIT DROP AS
WITH slots AS (
    SELECT q.lesson_id,
           q.qno,
           (((q.qno - 1) % 4) + 1) AS correct_slot
      FROM _seed_quiz_q q
),
dist AS (
    SELECT q.lesson_id,
           q.qno,
           t.meaning_vi,
           FALSE AS is_correct,
           -- slot thứ (k) trong 1..4, loại trừ correct_slot
           (SELECT g
              FROM generate_series(1, 4) AS g
             WHERE g <> s.correct_slot
             ORDER BY g
             OFFSET (t.k - 1) LIMIT 1) AS slot
      FROM _seed_quiz_q q
      JOIN slots s ON s.lesson_id = q.lesson_id AND s.qno = q.qno
      CROSS JOIN LATERAL (
            SELECT ranked.meaning_vi, ranked.k
              FROM (
                    SELECT mp.meaning_vi,
                           row_number() OVER (
                               ORDER BY ((mp.rn - q.word_rn - 1 - 37 * 3) % mp.total
                                         + mp.total) % mp.total
                           ) AS k
                      FROM _seed_meaning_pool mp
                     WHERE mp.jlpt_level  = q.jlpt_level
                       AND mp.meaning_vi <> q.meaning_vi
                   ) ranked
             WHERE ranked.k <= 3
           ) t
)
SELECT q.lesson_id,
       q.qno,
       q.vocab_id,
       q.meaning_vi,
       TRUE  AS is_correct,
       s.correct_slot AS slot
  FROM _seed_quiz_q q
  JOIN slots s ON s.lesson_id = q.lesson_id AND s.qno = q.qno
UNION ALL
SELECT d.lesson_id,
       d.qno,
       NULL::BIGINT AS vocab_id,
       d.meaning_vi,
       d.is_correct,
       d.slot
  FROM dist d;

INSERT INTO quiz_options (question_id, option_key, option_text, is_correct)
SELECT qq.id, o.slot, o.meaning_vi, o.is_correct
  FROM _seed_quiz_opt o
  JOIN _seed_quiz_id  qi ON qi.lesson_id = o.lesson_id
  JOIN quiz_questions qq ON qq.quiz_id    = qi.quiz_id
                        AND qq.order_index = o.qno
 ORDER BY o.lesson_id, o.qno, o.slot;


-- -----------------------------------------------------------------------------
-- BƯỚC 13 — KIỂM TRA TRƯỚC KHI COMMIT
--   RAISE EXCEPTION nếu dữ liệu không đúng kỳ vọng → transaction rollback.
-- -----------------------------------------------------------------------------
DO $check$
DECLARE
    p              RECORD;
    v_courses   INT;
    v_lessons   INT;
    v_sections  INT;
    v_kanji     INT;
    v_vocab     INT;
    v_grammar   INT;
    v_vexamples INT;
    v_gexamples INT;
    v_quizzes   INT;
    v_questions INT;
    v_options   INT;
    v_bad_q     INT;  -- câu không có đúng 4 option / đúng 1 đáp án đúng / trùng nghĩa
BEGIN
    SELECT count(*) INTO v_courses FROM courses
     WHERE slug IN ('jlpt-n5-khoa-hoc-co-ban', 'jlpt-n4-khoa-hoc-co-ban');

    SELECT count(*) INTO v_lessons FROM lessons l
      JOIN courses c ON c.id = l.course_id
     WHERE c.slug IN ('jlpt-n5-khoa-hoc-co-ban', 'jlpt-n4-khoa-hoc-co-ban')
       AND l.deleted = FALSE;

    SELECT count(*) INTO v_sections FROM lesson_sections s
      JOIN lessons l ON l.id = s.lesson_id
      JOIN courses c ON c.id = l.course_id
     WHERE c.slug IN ('jlpt-n5-khoa-hoc-co-ban', 'jlpt-n4-khoa-hoc-co-ban')
       AND s.deleted = FALSE;

    SELECT count(*) INTO v_kanji FROM lesson_kanji_items ki
      JOIN lesson_sections s ON s.id = ki.section_id
      JOIN lessons l ON l.id = s.lesson_id
      JOIN courses c ON c.id = l.course_id
     WHERE c.slug IN ('jlpt-n5-khoa-hoc-co-ban', 'jlpt-n4-khoa-hoc-co-ban')
       AND ki.deleted = FALSE;

    SELECT count(*) INTO v_vocab FROM lesson_vocabulary_items vi
      JOIN lesson_sections s ON s.id = vi.section_id
      JOIN lessons l ON l.id = s.lesson_id
      JOIN courses c ON c.id = l.course_id
     WHERE c.slug IN ('jlpt-n5-khoa-hoc-co-ban', 'jlpt-n4-khoa-hoc-co-ban')
       AND vi.deleted = FALSE;

    SELECT count(*) INTO v_grammar FROM lesson_grammar_items gi
      JOIN lesson_sections s ON s.id = gi.section_id
      JOIN lessons l ON l.id = s.lesson_id
      JOIN courses c ON c.id = l.course_id
     WHERE c.slug IN ('jlpt-n5-khoa-hoc-co-ban', 'jlpt-n4-khoa-hoc-co-ban')
       AND gi.deleted = FALSE;

    SELECT count(*) INTO v_vexamples FROM lesson_vocabulary_examples e
      JOIN lesson_vocabulary_items vi ON vi.id = e.vocab_item_id
      JOIN lesson_sections s ON s.id = vi.section_id
      JOIN lessons l ON l.id = s.lesson_id
      JOIN courses c ON c.id = l.course_id
     WHERE c.slug IN ('jlpt-n5-khoa-hoc-co-ban', 'jlpt-n4-khoa-hoc-co-ban')
       AND e.deleted = FALSE;

    SELECT count(*) INTO v_gexamples FROM lesson_grammar_examples e
      JOIN lesson_grammar_items gi ON gi.id = e.grammar_item_id
      JOIN lesson_sections s ON s.id = gi.section_id
      JOIN lessons l ON l.id = s.lesson_id
      JOIN courses c ON c.id = l.course_id
     WHERE c.slug IN ('jlpt-n5-khoa-hoc-co-ban', 'jlpt-n4-khoa-hoc-co-ban')
       AND e.deleted = FALSE;

    SELECT count(*) INTO v_quizzes FROM lesson_quizzes lq
      JOIN lessons l ON l.id = lq.lesson_id
      JOIN courses c ON c.id = l.course_id
     WHERE c.slug IN ('jlpt-n5-khoa-hoc-co-ban', 'jlpt-n4-khoa-hoc-co-ban')
       AND lq.deleted = FALSE;

    SELECT count(*) INTO v_questions FROM quiz_questions qq
      JOIN lesson_quizzes lq ON lq.id = qq.quiz_id
      JOIN lessons l ON l.id = lq.lesson_id
      JOIN courses c ON c.id = l.course_id
     WHERE c.slug IN ('jlpt-n5-khoa-hoc-co-ban', 'jlpt-n4-khoa-hoc-co-ban')
       AND qq.deleted = FALSE;

    SELECT count(*) INTO v_options FROM quiz_options qo
      JOIN quiz_questions qq ON qq.id = qo.question_id
      JOIN lesson_quizzes lq ON lq.id = qq.quiz_id
      JOIN lessons l ON l.id = lq.lesson_id
      JOIN courses c ON c.id = l.course_id
     WHERE c.slug IN ('jlpt-n5-khoa-hoc-co-ban', 'jlpt-n4-khoa-hoc-co-ban')
       AND qo.deleted = FALSE;

    -- Câu hỏi lỗi: sai số option, sai số đáp án đúng, hoặc có 2 option cùng nghĩa
    SELECT count(*) INTO v_bad_q FROM (
        SELECT qq.id
          FROM quiz_questions qq
          JOIN lesson_quizzes lq ON lq.id  = qq.quiz_id
          JOIN lessons l           ON l.id  = lq.lesson_id
          JOIN courses c           ON c.id  = l.course_id
          LEFT JOIN quiz_options qo ON qo.question_id = qq.id AND qo.deleted = FALSE
         WHERE c.slug IN ('jlpt-n5-khoa-hoc-co-ban', 'jlpt-n4-khoa-hoc-co-ban')
           AND qq.deleted = FALSE
         GROUP BY qq.id
        HAVING count(qo.id) <> 4
            OR count(qo.id) FILTER (WHERE qo.is_correct) <> 1
            OR count(DISTINCT btrim(qo.option_text)) <> count(qo.id)
    ) t;

    RAISE NOTICE '--- Ket qua seed ---';
    RAISE NOTICE 'courses              = %', v_courses;
    RAISE NOTICE 'lessons              = %', v_lessons;
    RAISE NOTICE 'lesson_sections      = %', v_sections;
    RAISE NOTICE 'lesson_kanji_items   = %', v_kanji;
    RAISE NOTICE 'lesson_vocab_items   = %', v_vocab;
    RAISE NOTICE 'lesson_grammar_items = %', v_grammar;
    RAISE NOTICE 'lesson_vocab_examples= %', v_vexamples;
    RAISE NOTICE 'lesson_grammar_exampl= %', v_gexamples;
    RAISE NOTICE 'lesson_quizzes       = %', v_quizzes;
    RAISE NOTICE 'quiz_questions       = %', v_questions;
    RAISE NOTICE 'quiz_options         = %', v_options;

    -- psql KHÔNG nội suy biến :var bên trong khối dollar-quoted, nên đọc
    -- tham số cấu hình từ bảng _seed_param.
    SELECT * INTO STRICT p FROM _seed_param;

    IF v_courses <> 2 THEN
        RAISE EXCEPTION 'Can dung 2 course, thuc te %', v_courses;
    END IF;
    IF v_lessons <> 6 THEN
        RAISE EXCEPTION 'Can dung 6 lesson, thuc te %', v_lessons;
    END IF;
    IF v_sections <> 18 THEN
        RAISE EXCEPTION 'Can dung 18 section, thuc te %', v_sections;
    END IF;
    IF v_kanji <> 6 * p.kanji_per_lesson THEN
        RAISE EXCEPTION 'Can % kanji item, thuc te %', 6 * p.kanji_per_lesson, v_kanji;
    END IF;
    IF v_vocab <> 6 * p.vocab_per_lesson THEN
        RAISE EXCEPTION 'Can % vocab item, thuc te %', 6 * p.vocab_per_lesson, v_vocab;
    END IF;
    IF v_grammar <> 6 * p.grammar_per_lesson THEN
        RAISE EXCEPTION 'Can % grammar item, thuc te %', 6 * p.grammar_per_lesson, v_grammar;
    END IF;
    IF v_quizzes <> 6 THEN
        RAISE EXCEPTION 'Can dung 6 quiz, thuc te %', v_quizzes;
    END IF;
    IF v_questions <> 6 * p.questions_per_lesson THEN
        RAISE EXCEPTION 'Can % cau hoi, thuc te %', 6 * p.questions_per_lesson, v_questions;
    END IF;
    IF v_options <> 4 * v_questions THEN
        RAISE EXCEPTION 'Can % option, thuc te %', 4 * v_questions, v_options;
    END IF;
    IF v_bad_q > 0 THEN
        RAISE EXCEPTION 'Co % cau hoi khong hop le (so option / dap an / trung nghia)', v_bad_q;
    END IF;
END
$check$;


-- -----------------------------------------------------------------------------
-- BƯỚC 14 — Báo cáo tổng hợp trước COMMIT
-- -----------------------------------------------------------------------------
SELECT c.jlpt_level,
       c.slug,
       c.is_published,
       l.order_index                                 AS lesson_no,
       l.title                                        AS lesson_title,
       lq.passing_score_percent,
       count(DISTINCT s.id)                          AS sections,
       count(DISTINCT ki.id)                         AS kanji_items,
       count(DISTINCT vi.id)                         AS vocab_items,
       count(DISTINCT gi.id)                         AS grammar_items,
       count(DISTINCT qq.id)                         AS questions,
       count(DISTINCT qo.id)                         AS options
  FROM courses c
  JOIN lessons l ON l.course_id = c.id AND l.deleted = FALSE
  LEFT JOIN lesson_quizzes       lq ON lq.lesson_id  = l.id AND lq.deleted = FALSE
  LEFT JOIN lesson_sections       s  ON s.lesson_id   = l.id AND s.deleted  = FALSE
  LEFT JOIN lesson_kanji_items    ki ON ki.section_id = s.id AND ki.deleted = FALSE
  LEFT JOIN lesson_vocabulary_items vi ON vi.section_id = s.id AND vi.deleted = FALSE
  LEFT JOIN lesson_grammar_items    gi ON gi.section_id = s.id AND gi.deleted = FALSE
  LEFT JOIN quiz_questions qq ON qq.quiz_id = lq.id AND qq.deleted = FALSE
  LEFT JOIN quiz_options   qo ON qo.question_id = qq.id AND qo.deleted = FALSE
 WHERE c.slug IN ('jlpt-n5-khoa-hoc-co-ban', 'jlpt-n4-khoa-hoc-co-ban')
 GROUP BY c.jlpt_level, c.slug, c.is_published,
          l.order_index, l.title, lq.passing_score_percent
 ORDER BY c.jlpt_level, l.order_index;

COMMIT;