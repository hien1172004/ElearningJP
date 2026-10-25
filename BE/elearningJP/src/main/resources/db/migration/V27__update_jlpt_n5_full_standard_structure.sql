-- =============================================================================
-- V27: UPDATE JLPT N5 EXAM STRUCTURE WITH COMPLETE MONDAI FORMATS
-- Ca 1: Chữ Hán đọc, Chữ Hán viết, Từ vựng ngữ cảnh, Từ gần nghĩa/Cách dùng từ (用法)
-- Ca 2: Ngữ pháp câu, Dấu sao ★, Đục lỗ đoạn văn, Đoản văn đọc hiểu, Tìm kiếm thông tin tờ rơi
-- Ca 3: Nghe hiểu nhiệm vụ, Tranh 2x2, Phản xạ nhanh
-- =============================================================================

DO $$
DECLARE
    v_exam_id BIGINT;
    v_sec_vocab_id BIGINT;
    v_sec_grammar_reading_id BIGINT;
    v_sec_listening_id BIGINT;
    v_passage_short_id BIGINT;
    v_passage_cloze_id BIGINT;
    v_q_id BIGINT;
    v_mondai_kanji_reading BIGINT;
    v_mondai_orthography BIGINT;
    v_mondai_synonyms BIGINT;
    v_mondai_usage BIGINT;
    v_mondai_grammar BIGINT;
    v_mondai_star BIGINT;
    v_mondai_text_grammar BIGINT;
    v_mondai_short_reading BIGINT;
    v_mondai_info BIGINT;
BEGIN
    SELECT id INTO v_exam_id FROM exams WHERE exam_type = 'JLPT_OFFICIAL_MOCK' AND jlpt_level = 'N5' LIMIT 1;
    IF v_exam_id IS NULL THEN
        RETURN;
    END IF;

    -- Lấy ID các Mondai Types
    SELECT id INTO v_mondai_kanji_reading FROM mondai_types WHERE code = 'KANJI_READING';
    SELECT id INTO v_mondai_orthography FROM mondai_types WHERE code = 'ORTHOGRAPHY';
    SELECT id INTO v_mondai_synonyms FROM mondai_types WHERE code = 'SYNONYMS';
    SELECT id INTO v_mondai_usage FROM mondai_types WHERE code = 'PROPER_USAGE';
    SELECT id INTO v_mondai_grammar FROM mondai_types WHERE code = 'SENTENTIAL_GRAMMAR';
    SELECT id INTO v_mondai_star FROM mondai_types WHERE code = 'SENTENCE_COMPOSITION';
    SELECT id INTO v_mondai_text_grammar FROM mondai_types WHERE code = 'TEXT_GRAMMAR';
    SELECT id INTO v_mondai_short_reading FROM mondai_types WHERE code = 'SHORT_PASSAGE';
    SELECT id INTO v_mondai_info FROM mondai_types WHERE code = 'INFORMATION_RETRIEVAL';

    -- Lấy ID các Section
    SELECT id INTO v_sec_vocab_id FROM exam_sections WHERE exam_id = v_exam_id AND session_type = 'VOCABULARY';
    SELECT id INTO v_sec_grammar_reading_id FROM exam_sections WHERE exam_id = v_exam_id AND session_type = 'GRAMMAR_READING';
    SELECT id INTO v_sec_listening_id FROM exam_sections WHERE exam_id = v_exam_id AND session_type = 'LISTENING';

    -- 1. Thêm Câu 4 cho Ca 1: Dạng bài Cách dùng từ (用法 - PROPER_USAGE)
    INSERT INTO questions (mondai_type_id, question_text, explanation_vi, jlpt_level, skill_type, is_active)
    VALUES (
        v_mondai_usage,
        'つぎの ことばの つかいかたで いちばん いい ものを ひとつ えらんで ください。<br><b>【どうぞ】</b>',
        '「どうぞ」được dùng để mời ai đó làm gì hoặc đưa đồ cho ai đó một cách lịch sự ("Xin mời..."). Câu 1 "この ほんを どうぞ" là cách dùng chính xác.',
        'N5', 'VOCABULARY', TRUE
    ) RETURNING id INTO v_q_id;
    INSERT INTO exam_section_questions (exam_id, section_id, question_id, question_number, points)
    VALUES (v_exam_id, v_sec_vocab_id, v_q_id, 4, 1.00);
    INSERT INTO question_options (question_id, option_key, option_text, is_correct)
    VALUES 
        (v_q_id, 1, 'この　本を　どうぞ。', TRUE),
        (v_q_id, 2, 'きょうは　どうぞ　あついです。', FALSE),
        (v_q_id, 3, 'どうぞ　ありがとう　ございます。', FALSE),
        (v_q_id, 4, 'あした　どうぞ　いきましょう。', FALSE);

    -- 2. Thêm Passage cho Bài đục lỗ trong đoạn văn (文章の文法 - TEXT_GRAMMAR)
    INSERT INTO reading_passages (title, jlpt_level, content_jp, translation_vi, passage_type, is_active)
    VALUES (
        'わたしの一日 (Một ngày của tôi)', 'N5',
        'わたしは　毎朝　6時に　起きます。\n朝ごはんを　食べて、新聞を　読みます。[ 7 ]、7時半に　家を　出て、学校へ　行きます。学校は　8時半から　始まります。',
        'Tôi thức dậy lúc 6 giờ mỗi sáng.\nSau khi ăn sáng, tôi đọc báo. Sau đó, 7 rưỡi tôi rời nhà và đến trường. Trường học bắt đầu từ 8 rưỡi.',
        'TEXT', TRUE
    ) RETURNING id INTO v_passage_cloze_id;

    -- Thêm Câu 7 cho Ca 2: Ngữ pháp đục lỗ
    INSERT INTO questions (mondai_type_id, passage_id, question_text, explanation_vi, jlpt_level, skill_type, is_active)
    VALUES (
        v_mondai_text_grammar, v_passage_cloze_id,
        '[ 7 ] に　入る　もっとも　よい　ものを　ひとつ　えらんで　ください。',
        'Mẫu liên từ chỉ hành động diễn ra tiếp theo theo trình tự thời gian là「それから」(Sau đó).',
        'N5', 'GRAMMAR', TRUE
    ) RETURNING id INTO v_q_id;
    INSERT INTO exam_section_questions (exam_id, section_id, question_id, question_number, points)
    VALUES (v_exam_id, v_sec_grammar_reading_id, v_q_id, 7, 1.00);
    INSERT INTO question_options (question_id, option_key, option_text, is_correct)
    VALUES 
        (v_q_id, 1, 'それから', TRUE),
        (v_q_id, 2, 'だから', FALSE),
        (v_q_id, 3, 'しかし', FALSE),
        (v_q_id, 4, 'では', FALSE);

    -- 3. Thêm Đoản văn đọc hiểu (短文読解 - SHORT_PASSAGE)
    INSERT INTO reading_passages (title, jlpt_level, content_jp, translation_vi, passage_type, is_active)
    VALUES (
        '山田さんの週末 (Cuối tuần của anh Yamada)', 'N5',
        '山田さんの　へやには　本が　たくさん　あります。山田さんは　毎晩　寝る　前に　本を　1時間　読みます。休みの　日も　よく　図書館へ　行って、新しい　本を　借ります。',
        'Trong phòng anh Yamada có rất nhiều sách. Mỗi tối trước khi ngủ anh đọc sách 1 tiếng. Vào ngày nghỉ anh cũng thường đến thư viện mượn sách mới.',
        'TEXT', TRUE
    ) RETURNING id INTO v_passage_short_id;

    -- Thêm Câu 8 cho Ca 2: Đoản văn đọc hiểu
    INSERT INTO questions (mondai_type_id, passage_id, question_text, explanation_vi, jlpt_level, skill_type, is_active)
    VALUES (
        v_mondai_short_reading, v_passage_short_id,
        '山田さんは　どんな　人ですか。',
        'Đoạn văn miêu tả trong phòng anh Yamada có nhiều sách, tối nào cũng đọc sách và ngày nghỉ hay đi thư viện mượn sách => Là người thích đọc sách (本が　すきな　人).',
        'N5', 'READING', TRUE
    ) RETURNING id INTO v_q_id;
    INSERT INTO exam_section_questions (exam_id, section_id, question_id, question_number, points)
    VALUES (v_exam_id, v_sec_grammar_reading_id, v_q_id, 8, 1.00);
    INSERT INTO question_options (question_id, option_key, option_text, is_correct)
    VALUES 
        (v_q_id, 1, '本が　すきな　人', TRUE),
        (v_q_id, 2, 'えいがが　すきな　人', FALSE),
        (v_q_id, 3, 'おんがくが　すきな　人', FALSE),
        (v_q_id, 4, 'りょこうが　すきな　人', FALSE);

    -- Cập nhật lại số thứ tự câu cho các câu phía sau để thứ tự chuẩn xác:
    -- Câu 6 trước đó (bài đọc tờ rơi) đổi thành Câu 9
    UPDATE exam_section_questions SET question_number = 9 WHERE exam_id = v_exam_id AND question_id IN (
        SELECT id FROM questions WHERE mondai_type_id = v_mondai_info
    );

    -- Các câu nghe (trước là 7, 8, 9, 10) chuyển thành 10, 11, 12, 13
    UPDATE exam_section_questions SET question_number = 10 WHERE exam_id = v_exam_id AND section_id = v_sec_listening_id AND question_number = 7;
    UPDATE exam_section_questions SET question_number = 11 WHERE exam_id = v_exam_id AND section_id = v_sec_listening_id AND question_number = 8;
    UPDATE exam_section_questions SET question_number = 12 WHERE exam_id = v_exam_id AND section_id = v_sec_listening_id AND question_number = 9;
    UPDATE exam_section_questions SET question_number = 13 WHERE exam_id = v_exam_id AND section_id = v_sec_listening_id AND question_number = 10;

    -- Cập nhật tổng số câu của đề thi N5 lên 13 câu
    UPDATE exams SET total_questions = 13 WHERE id = v_exam_id;

END $$;
