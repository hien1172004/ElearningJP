-- =============================================================================
-- V26: SEED REALISTIC JLPT N5 MOCK EXAM (VOCABULARY, STAR QUESTION, FLYER, AUDIO TIMESTAMPS)
-- =============================================================================

DO $$
DECLARE
    v_exam_id BIGINT;
    v_group_vocab_reading_id BIGINT;
    v_group_listening_id BIGINT;
    v_sec_vocab_id BIGINT;
    v_sec_grammar_reading_id BIGINT;
    v_sec_listening_id BIGINT;
    v_passage_flyer_id BIGINT;
    v_q_id BIGINT;
    v_mondai_kanji_reading BIGINT;
    v_mondai_orthography BIGINT;
    v_mondai_contextual BIGINT;
    v_mondai_grammar BIGINT;
    v_mondai_star BIGINT;
    v_mondai_info BIGINT;
    v_mondai_listening_task BIGINT;
    v_mondai_listening_quick BIGINT;
BEGIN
    -- 1. Get Mondai Type IDs
    SELECT id INTO v_mondai_kanji_reading FROM mondai_types WHERE code = 'KANJI_READING';
    SELECT id INTO v_mondai_orthography FROM mondai_types WHERE code = 'ORTHOGRAPHY';
    SELECT id INTO v_mondai_contextual FROM mondai_types WHERE code = 'CONTEXTUAL_USE';
    SELECT id INTO v_mondai_grammar FROM mondai_types WHERE code = 'SENTENTIAL_GRAMMAR';
    SELECT id INTO v_mondai_star FROM mondai_types WHERE code = 'SENTENCE_COMPOSITION';
    SELECT id INTO v_mondai_info FROM mondai_types WHERE code = 'INFORMATION_RETRIEVAL';
    SELECT id INTO v_mondai_listening_task FROM mondai_types WHERE code = 'TASK_BASED';
    SELECT id INTO v_mondai_listening_quick FROM mondai_types WHERE code = 'QUICK_RESPONSE';

    -- 2. Insert JLPT N5 Mock Exam
    INSERT INTO exams (
        title, exam_type, jlpt_level, total_duration_minutes, 
        total_scaled_max_score, overall_passing_score, total_questions, 
        is_published, created_at, updated_at
    ) VALUES (
        'Đề thi thử JLPT N5 Chuẩn Format Quốc Tế - Số 01', 
        'JLPT_OFFICIAL_MOCK', 'N5', 90, 
        180, 80, 10, 
        TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    ) RETURNING id INTO v_exam_id;

    -- 3. Insert 2 Score Groups for N5 (Ngôn ngữ + Đọc hiểu: 120, Liệt 38 | Nghe hiểu: 60, Liệt 19)
    INSERT INTO exam_score_groups (exam_id, group_code, max_scaled_score, min_required_score)
    VALUES (v_exam_id, 'LANGUAGE_KNOWLEDGE_READING', 120, 38)
    RETURNING id INTO v_group_vocab_reading_id;

    INSERT INTO exam_score_groups (exam_id, group_code, max_scaled_score, min_required_score)
    VALUES (v_exam_id, 'LISTENING', 60, 19)
    RETURNING id INTO v_group_listening_id;

    -- 4. Insert 3 Exam Sections
    -- Ca 1: Từ vựng
    INSERT INTO exam_sections (exam_id, score_group_id, session_type, section_title, order_index, duration_minutes)
    VALUES (v_exam_id, v_group_vocab_reading_id, 'VOCABULARY', 'Ca 1: Từ vựng (文字・語彙)', 1, 20)
    RETURNING id INTO v_sec_vocab_id;

    -- Ca 2: Ngữ pháp & Đọc hiểu
    INSERT INTO exam_sections (exam_id, score_group_id, session_type, section_title, order_index, duration_minutes)
    VALUES (v_exam_id, v_group_vocab_reading_id, 'GRAMMAR_READING', 'Ca 2: Ngữ pháp & Đọc hiểu (文法・読解)', 2, 40)
    RETURNING id INTO v_sec_grammar_reading_id;

    -- Ca 3: Nghe hiểu
    INSERT INTO exam_sections (exam_id, score_group_id, session_type, section_title, audio_url, order_index, duration_minutes)
    VALUES (v_exam_id, v_group_listening_id, 'LISTENING', 'Ca 3: Nghe hiểu (聴解)', 'https://actions.google.com/sounds/v1/ambiences/coffee_shop.ogg', 3, 30)
    RETURNING id INTO v_sec_listening_id;

    -- 5. Bài đọc dạng Tờ rơi (IMAGE_POSTER - Thông tin tìm kiếm)
    INSERT INTO reading_passages (title, jlpt_level, content_jp, translation_vi, passage_type, image_url, is_active)
    VALUES (
        'Thông báo Giờ mở cửa & Bảng giá Thư viện Sakura', 'N5',
        '【さくら図書館の お知らせ】\n月曜日〜金曜日：午前9時〜午後8時\n土曜日・日曜日：午前10時〜午後5時\n※ 祝日は 休みです。\n※ 本は 1人 5さつまで、2しゅうかん かりられます。',
        '【Thông báo của Thư viện Sakura】\nThứ 2 đến Thứ 6: 9:00 sáng - 8:00 tối\nThứ 7, Chủ nhật: 10:00 sáng - 5:00 chiều\n※ Ngày lễ nghỉ.\n※ Mỗi người được mượn tối đa 5 quyển sách trong 2 tuần.',
        'IMAGE_POSTER',
        'https://images.unsplash.com/photo-1521587760476-6c12a4b040da?auto=format&fit=crop&w=800&q=80',
        TRUE
    ) RETURNING id INTO v_passage_flyer_id;

    -- 6. Insert Questions for Section 1 (Từ vựng)
    -- Câu 1: Cách đọc Kanji
    INSERT INTO questions (mondai_type_id, question_text, explanation_vi, jlpt_level, skill_type, is_active)
    VALUES (v_mondai_kanji_reading, '公園の　池で　めずらしい　<u>鳥</u>を　見ました。', '鳥 (とり) có nghĩa là con chim.', 'N5', 'VOCABULARY', TRUE)
    RETURNING id INTO v_q_id;
    INSERT INTO exam_section_questions (exam_id, section_id, question_id, question_number, points) VALUES (v_exam_id, v_sec_vocab_id, v_q_id, 1, 1.00);
    INSERT INTO question_options (question_id, option_key, option_text, is_correct) VALUES (v_q_id, 1, 'とり', TRUE), (v_q_id, 2, 'いぬ', FALSE), (v_q_id, 3, 'さかな', FALSE), (v_q_id, 4, 'ねこ', FALSE);

    -- Câu 2: Cách viết Kanji
    INSERT INTO questions (mondai_type_id, question_text, explanation_vi, jlpt_level, skill_type, is_active)
    VALUES (v_mondai_orthography, 'まいにち　<u>くるま</u>で　かいしゃへ　いきます。', 'くるま viết chữ Hán là 車.', 'N5', 'VOCABULARY', TRUE)
    RETURNING id INTO v_q_id;
    INSERT INTO exam_section_questions (exam_id, section_id, question_id, question_number, points) VALUES (v_exam_id, v_sec_vocab_id, v_q_id, 2, 1.00);
    INSERT INTO question_options (question_id, option_key, option_text, is_correct) VALUES (v_q_id, 1, '車', TRUE), (v_q_id, 2, '東', FALSE), (v_q_id, 3, '電', FALSE), (v_q_id, 4, '校', FALSE);

    -- Câu 3: Ngữ cảnh
    INSERT INTO questions (mondai_type_id, question_text, explanation_vi, jlpt_level, skill_type, is_active)
    VALUES (v_mondai_contextual, 'あついですね。まどを　(　　)ましょうか。', 'Nóng quá nhỉ. Tôi mở cửa sổ nhé? Mở cửa sổ dùng あけます.', 'N5', 'VOCABULARY', TRUE)
    RETURNING id INTO v_q_id;
    INSERT INTO exam_section_questions (exam_id, section_id, question_id, question_number, points) VALUES (v_exam_id, v_sec_vocab_id, v_q_id, 3, 1.00);
    INSERT INTO question_options (question_id, option_key, option_text, is_correct) VALUES (v_q_id, 1, 'しめ', FALSE), (v_q_id, 2, 'あけ', TRUE), (v_q_id, 3, 'つけ', FALSE), (v_q_id, 4, 'けし', FALSE);

    -- 7. Insert Questions for Section 2 (Ngữ pháp & Đọc hiểu)
    -- Câu 4: Ngữ pháp
    INSERT INTO questions (mondai_type_id, question_text, explanation_vi, jlpt_level, skill_type, is_active)
    VALUES (v_mondai_grammar, 'わたしは　日本へ　日本語を　ならい　(　　)　きました。', 'Mẫu câu chỉ mục đích đi đến đâu để làm gì: V(bỏ ます) + に + 行く/来る.', 'N5', 'GRAMMAR', TRUE)
    RETURNING id INTO v_q_id;
    INSERT INTO exam_section_questions (exam_id, section_id, question_id, question_number, points) VALUES (v_exam_id, v_sec_grammar_reading_id, v_q_id, 4, 1.00);
    INSERT INTO question_options (question_id, option_key, option_text, is_correct) VALUES (v_q_id, 1, 'で', FALSE), (v_q_id, 2, 'を', FALSE), (v_q_id, 3, 'に', TRUE), (v_q_id, 4, 'へ', FALSE);

    -- Câu 5: CÂU HỎI SẮP XẾP DẤU SAO (★) - star_position = 3
    INSERT INTO questions (mondai_type_id, question_text, star_position, explanation_vi, jlpt_level, skill_type, is_active)
    VALUES (v_mondai_star, 'さとうさんは　____　____　★　____　と　言っていました。', 3, 'Thứ tự ghép đúng: [2: 遅れると] [1: 電話が] [4: ありました] [3: 会議に]. Vị trí dấu sao số 3 là [4: ありました].', 'N5', 'GRAMMAR', TRUE)
    RETURNING id INTO v_q_id;
    INSERT INTO exam_section_questions (exam_id, section_id, question_id, question_number, points) VALUES (v_exam_id, v_sec_grammar_reading_id, v_q_id, 5, 1.00);
    INSERT INTO question_options (question_id, option_key, option_text, is_correct) VALUES (v_q_id, 1, '電話が', FALSE), (v_q_id, 2, '遅れると', FALSE), (v_q_id, 3, '会議に', FALSE), (v_q_id, 4, 'ありました', TRUE);

    -- Câu 6: Đọc hiểu Tờ rơi (Liên kết với v_passage_flyer_id)
    INSERT INTO questions (mondai_type_id, passage_id, question_text, explanation_vi, jlpt_level, skill_type, is_active)
    VALUES (v_mondai_info, v_passage_flyer_id, '日曜日に　図書館で　本を　借りたいです。何時まで　開いていますか。', 'Theo thông báo, Chủ nhật mở cửa từ 10:00 sáng đến 5:00 chiều (午後5時).', 'N5', 'READING', TRUE)
    RETURNING id INTO v_q_id;
    INSERT INTO exam_section_questions (exam_id, section_id, question_id, question_number, points) VALUES (v_exam_id, v_sec_grammar_reading_id, v_q_id, 6, 1.00);
    INSERT INTO question_options (question_id, option_key, option_text, is_correct) VALUES (v_q_id, 1, '午後5時', TRUE), (v_q_id, 2, '午後8時', FALSE), (v_q_id, 3, '午前9時', FALSE), (v_q_id, 4, '休みです', FALSE);

    -- 8. Insert Questions for Section 3 (Nghe hiểu)
    -- Câu 7: Nghe có mốc audio và tranh tình huống
    INSERT INTO questions (
        mondai_type_id, question_text, image_url, audio_start_time, audio_end_time,
        transcript_jp, transcript_vi, explanation_vi, jlpt_level, skill_type, is_active
    ) VALUES (
        v_mondai_listening_task,
        '男の人と　女の人が　話しています。男の人は　このあと　どこへ　行きますか。',
        'https://images.unsplash.com/photo-1544717305-2782549b5136?auto=format&fit=crop&w=800&q=80',
        10, 45,
        '女：すみません、田中さん。この手紙を　出してきて　くれませんか。\n男：はい、いいですよ。郵便局ですね。\n女：ええ、おねがいします。',
        'Nữ: Xin lỗi Tanaka-san. Anh gửi giúp tôi bức thư này được không?\nNam: Vâng, được chứ. Đến bưu điện phải không ạ.\nNữ: Vâng, nhờ anh nhé.',
        'Người nam đồng ý đi gửi thư ở bưu điện (郵便局).',
        'N5', 'LISTENING', TRUE
    ) RETURNING id INTO v_q_id;
    INSERT INTO exam_section_questions (exam_id, section_id, question_id, question_number, points) VALUES (v_exam_id, v_sec_listening_id, v_q_id, 7, 1.00);
    INSERT INTO question_options (question_id, option_key, option_text, is_correct) VALUES (v_q_id, 1, '郵便局', TRUE), (v_q_id, 2, '銀行', FALSE), (v_q_id, 3, '駅', FALSE), (v_q_id, 4, '図書館', FALSE);

    -- Câu 8: Nghe có 4 lựa chọn tranh ảnh (has_visual_options = TRUE)
    INSERT INTO questions (
        mondai_type_id, question_text, has_visual_options, audio_start_time, audio_end_time,
        transcript_jp, transcript_vi, explanation_vi, jlpt_level, skill_type, is_active
    ) VALUES (
        v_mondai_listening_task,
        '女の子が　何を　買いましたか。',
        TRUE, 48, 85,
        '男：かわいい　帽子ですね。買いましたか。\n女：いいえ、帽子は　買いませんでした。かわいい　かばんを　買いました。',
        'Nam: Chiếc mũ dễ thương nhỉ. Bạn đã mua nó à?\nNữ: Không, tôi không mua mũ. Tôi đã mua một chiếc túi xách dễ thương.',
        'Bé gái nói không mua mũ mà mua chiếc túi xách (かばん).',
        'N5', 'LISTENING', TRUE
    ) RETURNING id INTO v_q_id;
    INSERT INTO exam_section_questions (exam_id, section_id, question_id, question_number, points) VALUES (v_exam_id, v_sec_listening_id, v_q_id, 8, 1.00);
    INSERT INTO question_options (question_id, option_key, option_text, option_image_url, is_correct) VALUES 
        (v_q_id, 1, 'Tranh 1: Túi xách', 'https://images.unsplash.com/photo-1548036328-c9fa89d128fa?auto=format&fit=crop&w=300&q=80', TRUE),
        (v_q_id, 2, 'Tranh 2: Mũ', 'https://images.unsplash.com/photo-1521369909029-2afed882baee?auto=format&fit=crop&w=300&q=80', FALSE),
        (v_q_id, 3, 'Tranh 3: Giày', 'https://images.unsplash.com/photo-1549298916-b41d501d3772?auto=format&fit=crop&w=300&q=80', FALSE),
        (v_q_id, 4, 'Tranh 4: Đồng hồ', 'https://images.unsplash.com/photo-1524805444758-089113d48a6d?auto=format&fit=crop&w=300&q=80', FALSE);

    -- Câu 9: Nghe phản xạ nhanh (QUICK_RESPONSE - 3 LỰA CHỌN)
    INSERT INTO questions (
        mondai_type_id, question_text, audio_start_time, audio_end_time,
        transcript_jp, transcript_vi, explanation_vi, jlpt_level, skill_type, is_active
    ) VALUES (
        v_mondai_listening_quick,
        '（問題用紙には　何も　印刷されていません）',
        88, 115,
        '男：いっしょに　お昼ご飯を　食べませんか。\n１：はい、食べませんでした。\n２：ええ、行きましょう。\n３：いいえ、お昼です。',
        'Nam: Bạn có muốn đi ăn trưa cùng tôi không?\n1: Vâng, tôi đã không ăn.\n2: Vâng, chúng ta cùng đi nhé.\n3: Không, là buổi trưa.',
        'Câu mời rủ "食べませんか", câu đáp phù hợp là "ええ、行きましょう" (Vâng, cùng đi nào).',
        'N5', 'LISTENING', TRUE
    ) RETURNING id INTO v_q_id;
    INSERT INTO exam_section_questions (exam_id, section_id, question_id, question_number, points) VALUES (v_exam_id, v_sec_listening_id, v_q_id, 9, 1.00);
    INSERT INTO question_options (question_id, option_key, option_text, is_correct) VALUES (v_q_id, 1, '1', FALSE), (v_q_id, 2, '2', TRUE), (v_q_id, 3, '3', FALSE);

    -- Câu 10: Nghe phản xạ nhanh tiếp theo (3 LỰA CHỌN)
    INSERT INTO questions (
        mondai_type_id, question_text, audio_start_time, audio_end_time,
        transcript_jp, transcript_vi, explanation_vi, jlpt_level, skill_type, is_active
    ) VALUES (
        v_mondai_listening_quick,
        '（問題用紙には　何も　印刷されていません）',
        118, 145,
        '女：きのうは　どうも　ありがとうございました。\n１：いいえ、どういたしまして。\n２：はい、ありがとう。\n３：どうも　すみません。',
        'Nữ: Hôm qua thực sự cảm ơn anh rất nhiều.\n1: Không có gì đâu ạ.\n2: Vâng, cảm ơn.\n3: Rất xin lỗi.',
        'Khi ai đó cảm ơn "ありがとうございました", câu đáp lịch sự chuẩn xác là "いいえ、どういたしまして" (Không có chi).',
        'N5', 'LISTENING', TRUE
    ) RETURNING id INTO v_q_id;
    INSERT INTO exam_section_questions (exam_id, section_id, question_id, question_number, points) VALUES (v_exam_id, v_sec_listening_id, v_q_id, 10, 1.00);
    INSERT INTO question_options (question_id, option_key, option_text, is_correct) VALUES (v_q_id, 1, '1', TRUE), (v_q_id, 2, '2', FALSE), (v_q_id, 3, '3', FALSE);

END $$;
