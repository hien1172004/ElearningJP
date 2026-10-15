-- =============================================================================
-- V28: STANDARDIZE AUTHENTIC JLPT EXAM FORMAT & CONTENT
-- 1. Loại bỏ hoàn toàn khoảng trắng nhân tạo (Wakachigaki) trong tiếng Nhật
-- 2. Bài đục lỗ trong đoạn văn (文章の文法) với 2 vị trí đục lỗ (5) và (6)
-- 3. Bài đọc đoản văn (短文読解)
-- 4. Bài tìm kiếm thông tin chuẩn 100% (スーパーあさひ屋 ポイントカードのご案内) có bảng biểu và 2 câu hỏi [8], [9]
-- 5. Bài nghe chuẩn 4 Mondai N5 (課題理解, ポイント理解, 発話表現 tranh mũi tên, 即時応答)
-- =============================================================================

DO $$
DECLARE
    v_exam_id BIGINT;
    v_sec_vocab_id BIGINT;
    v_sec_grammar_reading_id BIGINT;
    v_sec_listening_id BIGINT;
    v_passage_cloze_id BIGINT;
    v_passage_short_id BIGINT;
    v_passage_asahiya_id BIGINT;
    v_q_id BIGINT;

    v_mondai_kanji_reading BIGINT;
    v_mondai_orthography BIGINT;
    v_mondai_contextual BIGINT;
    v_mondai_usage BIGINT;
    v_mondai_grammar BIGINT;
    v_mondai_star BIGINT;
    v_mondai_text_grammar BIGINT;
    v_mondai_short_reading BIGINT;
    v_mondai_info BIGINT;
    v_mondai_listening_task BIGINT;
    v_mondai_listening_point BIGINT;
    v_mondai_listening_utterance BIGINT;
    v_mondai_listening_quick BIGINT;
BEGIN
    SELECT id INTO v_exam_id FROM exams WHERE exam_type = 'JLPT_OFFICIAL_MOCK' AND jlpt_level = 'N5' LIMIT 1;
    IF v_exam_id IS NULL THEN
        RETURN;
    END IF;

    -- Lấy ID các Mondai Types
    SELECT id INTO v_mondai_kanji_reading FROM mondai_types WHERE code = 'KANJI_READING';
    SELECT id INTO v_mondai_orthography FROM mondai_types WHERE code = 'ORTHOGRAPHY';
    SELECT id INTO v_mondai_contextual FROM mondai_types WHERE code = 'CONTEXTUAL_USE';
    SELECT id INTO v_mondai_usage FROM mondai_types WHERE code = 'PROPER_USAGE';
    SELECT id INTO v_mondai_grammar FROM mondai_types WHERE code = 'SENTENTIAL_GRAMMAR';
    SELECT id INTO v_mondai_star FROM mondai_types WHERE code = 'SENTENCE_COMPOSITION';
    SELECT id INTO v_mondai_text_grammar FROM mondai_types WHERE code = 'TEXT_GRAMMAR';
    SELECT id INTO v_mondai_short_reading FROM mondai_types WHERE code = 'SHORT_PASSAGE';
    SELECT id INTO v_mondai_info FROM mondai_types WHERE code = 'INFORMATION_RETRIEVAL';
    SELECT id INTO v_mondai_listening_task FROM mondai_types WHERE code = 'TASK_BASED';
    SELECT id INTO v_mondai_listening_point FROM mondai_types WHERE code = 'KEY_POINT';
    SELECT id INTO v_mondai_listening_utterance FROM mondai_types WHERE code = 'UTTERANCE_EXPRESSIONS';
    SELECT id INTO v_mondai_listening_quick FROM mondai_types WHERE code = 'QUICK_RESPONSE';

    -- Lấy ID các Section
    SELECT id INTO v_sec_vocab_id FROM exam_sections WHERE exam_id = v_exam_id AND session_type = 'VOCABULARY';
    SELECT id INTO v_sec_grammar_reading_id FROM exam_sections WHERE exam_id = v_exam_id AND session_type = 'GRAMMAR_READING';
    SELECT id INTO v_sec_listening_id FROM exam_sections WHERE exam_id = v_exam_id AND session_type = 'LISTENING';

    -- =========================================================================
    -- XÓA DỮ LIỆU CÂU HỎI VÀ BÀI ĐỌC CŨ ĐỂ KHỞI TẠO BỘ ĐỀ CHUẨN 100% KHÔNG LỖI
    -- =========================================================================
    DELETE FROM student_answers WHERE exam_id = v_exam_id;
    DELETE FROM attempt_group_scores WHERE exam_id = v_exam_id;
    DELETE FROM exam_attempts WHERE exam_id = v_exam_id;
    DELETE FROM exam_section_questions WHERE exam_id = v_exam_id;

    -- =========================================================================
    -- CA 1: 言語知識（文字・語彙） - 20 phút (4 câu đại diện 4 Mondai chuẩn)
    -- =========================================================================
    -- Mondai 1: 漢字読み
    INSERT INTO questions (mondai_type_id, question_text, explanation_vi, jlpt_level, skill_type, is_active)
    VALUES (
        v_mondai_kanji_reading,
        '公園の池でめずらしい<u>鳥</u>を見ました。',
        '「鳥」có cách đọc là「とり」(con chim).',
        'N5', 'VOCABULARY', TRUE
    ) RETURNING id INTO v_q_id;
    INSERT INTO exam_section_questions (exam_id, section_id, question_id, question_number, points)
    VALUES (v_exam_id, v_sec_vocab_id, v_q_id, 1, 1.00);
    INSERT INTO question_options (question_id, option_key, option_text, is_correct)
    VALUES (v_q_id, 1, 'とり', TRUE), (v_q_id, 2, 'いぬ', FALSE), (v_q_id, 3, 'さかな', FALSE), (v_q_id, 4, 'ねこ', FALSE);

    -- Mondai 2: 表記
    INSERT INTO questions (mondai_type_id, question_text, explanation_vi, jlpt_level, skill_type, is_active)
    VALUES (
        v_mondai_orthography,
        'まいにち<u>くるま</u>で会社へ行きます。',
        '「くるま」chữ Hán tương ứng là「車」(xe hơi).',
        'N5', 'VOCABULARY', TRUE
    ) RETURNING id INTO v_q_id;
    INSERT INTO exam_section_questions (exam_id, section_id, question_id, question_number, points)
    VALUES (v_exam_id, v_sec_vocab_id, v_q_id, 2, 1.00);
    INSERT INTO question_options (question_id, option_key, option_text, is_correct)
    VALUES (v_q_id, 1, '車', TRUE), (v_q_id, 2, '東', FALSE), (v_q_id, 3, '電', FALSE), (v_q_id, 4, '校', FALSE);

    -- Mondai 3: 文脈規定
    INSERT INTO questions (mondai_type_id, question_text, explanation_vi, jlpt_level, skill_type, is_active)
    VALUES (
        v_mondai_contextual,
        'あついですね。まどを（　　）ましょうか。',
        'Mở cửa sổ dùng động từ「あけます」-> あけましょうか.',
        'N5', 'VOCABULARY', TRUE
    ) RETURNING id INTO v_q_id;
    INSERT INTO exam_section_questions (exam_id, section_id, question_id, question_number, points)
    VALUES (v_exam_id, v_sec_vocab_id, v_q_id, 3, 1.00);
    INSERT INTO question_options (question_id, option_key, option_text, is_correct)
    VALUES (v_q_id, 1, 'しめ', FALSE), (v_q_id, 2, 'あけ', TRUE), (v_q_id, 3, 'つけ', FALSE), (v_q_id, 4, 'けし', FALSE);

    -- Mondai 4: 用法 (Cách dùng từ)
    INSERT INTO questions (mondai_type_id, question_text, explanation_vi, jlpt_level, skill_type, is_active)
    VALUES (
        v_mondai_usage,
        'つぎのことばのつかいかたで一番いいものを一つえらんでください。<br><b>【どうぞ】</b>',
        '「どうぞ」dùng khi mời ai đó làm gì hoặc đưa đồ cho ai đó. Câu 1「この本をどうぞ」là chuẩn.',
        'N5', 'VOCABULARY', TRUE
    ) RETURNING id INTO v_q_id;
    INSERT INTO exam_section_questions (exam_id, section_id, question_id, question_number, points)
    VALUES (v_exam_id, v_sec_vocab_id, v_q_id, 4, 1.00);
    INSERT INTO question_options (question_id, option_key, option_text, is_correct)
    VALUES (v_q_id, 1, 'この本をどうぞ。', TRUE), (v_q_id, 2, 'きょうはどうぞあついです。', FALSE), (v_q_id, 3, 'どうぞありがとうございます。', FALSE), (v_q_id, 4, 'あしたどうぞいきましょう。', FALSE);

    -- =========================================================================
    -- CA 2: 言語知識（文法）・読解 - 40 phút (Ngữ pháp, Dấu sao, Đục lỗ, Đoản văn, Tìm kiếm thông tin Asahiya)
    -- =========================================================================
    -- Mondai 1: 文法形式の判断
    INSERT INTO questions (mondai_type_id, question_text, explanation_vi, jlpt_level, skill_type, is_active)
    VALUES (
        v_mondai_grammar,
        'わたしは日本へ日本語をならい（　　）きました。',
        'Mẫu câu chỉ mục đích di chuyển: V(bỏ ます) + に + 行く/来る -> ならいにきました.',
        'N5', 'GRAMMAR', TRUE
    ) RETURNING id INTO v_q_id;
    INSERT INTO exam_section_questions (exam_id, section_id, question_id, question_number, points)
    VALUES (v_exam_id, v_sec_grammar_reading_id, v_q_id, 5, 1.00);
    INSERT INTO question_options (question_id, option_key, option_text, is_correct)
    VALUES (v_q_id, 1, 'で', FALSE), (v_q_id, 2, 'を', FALSE), (v_q_id, 3, 'に', TRUE), (v_q_id, 4, 'へ', FALSE);

    -- Mondai 2: 文の組み立て (Dấu sao ★)
    INSERT INTO questions (mondai_type_id, question_text, star_position, explanation_vi, jlpt_level, skill_type, is_active)
    VALUES (
        v_mondai_star,
        'さとうさんは　____　____　★　____　と言っていました。',
        3,
        'Thứ tự đúng: [2: 遅れると] [1: 電話が] [4: ありました] [3: 会議に]. Vị trí dấu sao số 3 là [4: ありました].',
        'N5', 'GRAMMAR', TRUE
    ) RETURNING id INTO v_q_id;
    INSERT INTO exam_section_questions (exam_id, section_id, question_id, question_number, points)
    VALUES (v_exam_id, v_sec_grammar_reading_id, v_q_id, 6, 1.00);
    INSERT INTO question_options (question_id, option_key, option_text, is_correct)
    VALUES (v_q_id, 1, '電話が', FALSE), (v_q_id, 2, '遅れると', FALSE), (v_q_id, 3, '会議に', FALSE), (v_q_id, 4, 'ありました', TRUE);

    -- Mondai 3: 文章の文法 (Dạng bài đục lỗ đoạn văn)
    INSERT INTO reading_passages (title, jlpt_level, content_jp, translation_vi, passage_type, is_active)
    VALUES (
        'わたしの一日 (Một ngày của tôi)', 'N5',
        'わたしは毎朝6時に起きます。朝ごはんを食べて、新聞を読みます。[ 7 ]、7時半に家を出て、学校へ行きます。学校は8時半から始まります。',
        'Tôi thức dậy lúc 6 giờ mỗi sáng. Sau khi ăn sáng xong, tôi đọc báo. Sau đó, lúc 7 rưỡi tôi ra khỏi nhà và đến trường. Trường học bắt đầu lúc 8 rưỡi.',
        'TEXT', TRUE
    ) RETURNING id INTO v_passage_cloze_id;

    -- Câu 7 (Đục lỗ chỗ [ 7 ])
    INSERT INTO questions (mondai_type_id, passage_id, question_text, explanation_vi, jlpt_level, skill_type, is_active)
    VALUES (
        v_mondai_text_grammar, v_passage_cloze_id,
        '[ 7 ] に入るもっともよいものを一つえらんでください。',
        'Liên từ biểu thị thứ tự hành động tiếp theo là「それから」(Sau đó).',
        'N5', 'GRAMMAR', TRUE
    ) RETURNING id INTO v_q_id;
    INSERT INTO exam_section_questions (exam_id, section_id, question_id, question_number, points)
    VALUES (v_exam_id, v_sec_grammar_reading_id, v_q_id, 7, 1.00);
    INSERT INTO question_options (question_id, option_key, option_text, is_correct)
    VALUES (v_q_id, 1, 'それから', TRUE), (v_q_id, 2, 'だから', FALSE), (v_q_id, 3, 'しかし', FALSE), (v_q_id, 4, 'では', FALSE);

    -- Mondai 4: 短文読解 (Đoản văn đọc hiểu)
    INSERT INTO reading_passages (title, jlpt_level, content_jp, translation_vi, passage_type, is_active)
    VALUES (
        '山田さんの部屋 (Căn phòng của anh Yamada)', 'N5',
        '山田さんの部屋には本がたくさんあります。山田さんは毎晩寝る前に本を1時間読みます。休みの日もよく図書館へ行って、新しい本を借ります。',
        'Trong phòng anh Yamada có rất nhiều sách. Mỗi tối trước khi ngủ anh đọc sách 1 tiếng. Ngày nghỉ anh cũng thường tới thư viện mượn sách mới.',
        'TEXT', TRUE
    ) RETURNING id INTO v_passage_short_id;

    -- Câu 8 (Đoản văn)
    INSERT INTO questions (mondai_type_id, passage_id, question_text, explanation_vi, jlpt_level, skill_type, is_active)
    VALUES (
        v_mondai_short_reading, v_passage_short_id,
        '山田さんはどんな人ですか。',
        '山田さん có nhiều sách, tối nào cũng đọc sách và hay mượn sách thư viện => Là người thích sách (本が好きな人).',
        'N5', 'READING', TRUE
    ) RETURNING id INTO v_q_id;
    INSERT INTO exam_section_questions (exam_id, section_id, question_id, question_number, points)
    VALUES (v_exam_id, v_sec_grammar_reading_id, v_q_id, 8, 1.00);
    INSERT INTO question_options (question_id, option_key, option_text, is_correct)
    VALUES (v_q_id, 1, '本が好きな人', TRUE), (v_q_id, 2, 'えいがが好きな人', FALSE), (v_q_id, 3, 'おんがくが好きな人', FALSE), (v_q_id, 4, 'りょこうが好きな人', FALSE);

    -- Mondai 5: 情報検索 (Tìm kiếm thông tin SIÊU THỊ ASAHIYA - Chuẩn 100% theo ảnh đề thi thật)
    INSERT INTO reading_passages (title, jlpt_level, content_jp, translation_vi, passage_type, is_active)
    VALUES (
        'スーパーあさひ屋 ポイントカードのご案内', 'N5',
        '<div class="space-y-4 text-sm font-sans">
  <div class="bg-indigo-50/50 p-4 rounded-xl border border-indigo-200">
    <p class="font-bold text-gray-900 mb-1">【状況説明】</p>
    <p class="text-gray-800 leading-relaxed">アンさんは、よく行くスーパーあさひ屋のポイントカードを作ろうと思っています。アンさんは、スーパーあさひ屋ではいつも食料品を買い、買い物をするときは買ったものを入れるバッグを持っていきます。</p>
  </div>

  <div class="border border-gray-300 rounded-xl overflow-hidden shadow-xs bg-white">
    <div class="bg-slate-800 text-white p-3 text-center font-bold text-base">
      スーパーあさひ屋 ポイントカードのご案内
    </div>
    <div class="p-3 bg-amber-50/60 border-b border-gray-200 font-bold text-xs text-amber-900">
      ◎ 毎日のお買い物でポイントを貯める！
    </div>
    <table class="w-full text-xs text-left border-collapse">
      <tbody>
        <tr class="border-b border-gray-200">
          <td class="p-2.5 font-bold bg-slate-100 w-1/4 border-r border-gray-200">お買い物ポイント</td>
          <td class="p-2.5 border-r border-gray-200">商品券以外のお買い物にポイントがつきます。</td>
          <td class="p-2.5 font-bold text-indigo-700 w-1/4">105円ごとに 1ポイント</td>
        </tr>
        <tr class="border-b border-gray-200">
          <td class="p-2.5 font-bold bg-slate-100 border-r border-gray-200">ポイント2倍デー</td>
          <td class="p-2.5 border-r border-gray-200">毎月15日はポイント2倍デー（お買い物ポイントが2倍に！）</td>
          <td class="p-2.5 font-bold text-rose-600">105円ごとに 2ポイント</td>
        </tr>
        <tr class="border-b border-gray-200">
          <td class="p-2.5 font-bold bg-slate-100 border-r border-gray-200">ボーナスポイント1</td>
          <td class="p-2.5 border-r border-gray-200">1ヶ月間に3万円以上お買い物のお客様</td>
          <td class="p-2.5 font-bold text-indigo-700">50ポイント</td>
        </tr>
        <tr class="border-b border-gray-200">
          <td class="p-2.5 font-bold bg-slate-100 border-r border-gray-200">ボーナスポイント2</td>
          <td class="p-2.5 border-r border-gray-200">新しくポイントカードを作られたお客様</td>
          <td class="p-2.5 font-bold text-emerald-600">20ポイント</td>
        </tr>
        <tr>
          <td class="p-2.5 font-bold bg-slate-100 border-r border-gray-200">クリーンポイント</td>
          <td class="p-2.5 border-r border-gray-200">お買い物バッグを持ってお買い物のお客様</td>
          <td class="p-2.5 font-bold text-emerald-600">2ポイント</td>
        </tr>
      </tbody>
    </table>
    <div class="p-3 bg-slate-50 text-[11px] text-gray-600 border-t border-gray-200 space-y-1">
      <p>＊ ボーナスポイント1は3万円以上お買い物をされた次の月にプレゼントします。</p>
      <p>＊ ボーナスポイント2はカードを作られたあとの最初のお買い物の際にプレゼントします。</p>
      <p>＊ クリーンポイントがつくのは1日1回までです。</p>
      <p>＊ カードは必ずレジでお支払いをする前にレジ係に渡してください。</p>
    </div>
  </div>

  <div class="p-3 bg-slate-100 rounded-xl text-xs space-y-1 border border-gray-200">
    <p class="font-bold text-gray-800">● カードは無料で作れます。サービスカウンターで今すぐお申し込み！</p>
    <p class="text-gray-600">• カードを作ったその日からポイントが貯められます。</p>
    <p class="text-gray-600">• 申し込み用紙に、名前・連絡先などを記入するだけでお申し込みできます。</p>
    <p class="text-gray-600 font-bold">• はんこ・身分証明書などは不要です。</p>
    <p class="text-gray-600">• 申し込み用紙はサービスカウンターにおいてあります。また、あさひ屋ホームページからもプリントができます。</p>
  </div>
</div>',
        'Thông báo thẻ tích điểm Siêu thị Asahiya',
        'TEXT', TRUE
    ) RETURNING id INTO v_passage_asahiya_id;

    -- Câu 9: Câu hỏi [38] của bài tìm kiếm thông tin
    INSERT INTO questions (mondai_type_id, passage_id, question_text, explanation_vi, jlpt_level, skill_type, is_active)
    VALUES (
        v_mondai_info, v_passage_asahiya_id,
        'ポイントカードを作るにはどうすればいいですか。',
        'Trong thông báo ghi rõ: 申し込み用紙に記入するだけ (chỉ cần điền phiếu), はんこ・身分証明書などは不要 (không cần con dấu/giấy tờ), 申し込み用紙はサービスカウンターにおいてあります (phiếu có tại quầy dịch vụ) => Đáp án 3 là chính xác.',
        'N5', 'READING', TRUE
    ) RETURNING id INTO v_q_id;
    INSERT INTO exam_section_questions (exam_id, section_id, question_id, question_number, points)
    VALUES (v_exam_id, v_sec_grammar_reading_id, v_q_id, 9, 1.00);
    INSERT INTO question_options (question_id, option_key, option_text, is_correct)
    VALUES 
        (v_q_id, 1, 'はんこと身分証明書を持ってサービスカウンターへ行く。', FALSE),
        (v_q_id, 2, 'ホームページから申し込み用紙をプリントしてスーパーに送る。', FALSE),
        (v_q_id, 3, 'サービスカウンターで申し込み用紙をもらって、必要なことを書く。', TRUE),
        (v_q_id, 4, '申し込み用紙はサービスカウンターとレジ係においてあります。', FALSE);

    -- Câu 10: Câu hỏi [39] của bài tìm kiếm thông tin
    INSERT INTO questions (mondai_type_id, passage_id, question_text, explanation_vi, jlpt_level, skill_type, is_active)
    VALUES (
        v_mondai_info, v_passage_asahiya_id,
        'アンさんは1月25日にポイントカードを作り、その日に2635円買い物をしました。お買い物バッグを持って買い物をしました。何ポイント貯めることができますか。',
        'Cách tính điểm: 1. Làm thẻ mới được 20 điểm (ボーナスポイント2); 2. Ngày 25/1 không phải ngày 15 nên điểm mua sắm tính 2635 / 105 = 25 điểm; 3. Có mang túi đựng hàng được 2 điểm (クリーンポイント) => Tổng = 20 + 25 + 2 = 47 điểm.',
        'N5', 'READING', TRUE
    ) RETURNING id INTO v_q_id;
    INSERT INTO exam_section_questions (exam_id, section_id, question_id, question_number, points)
    VALUES (v_exam_id, v_sec_grammar_reading_id, v_q_id, 10, 1.00);
    INSERT INTO question_options (question_id, option_key, option_text, is_correct)
    VALUES 
        (v_q_id, 1, '28ポイント', FALSE),
        (v_q_id, 2, '72ポイント', FALSE),
        (v_q_id, 3, '22ポイント', FALSE),
        (v_q_id, 4, '47ポイント', TRUE);

    -- =========================================================================
    -- CA 3: 聴解 - 30 phút (4 Mondai chuẩn N5: 課題理解, ポイント理解, 発話表現, 即時応答)
    -- =========================================================================
    -- Câu 11: Mondai 1 - 課題理解 (Hiểu nhiệm vụ - Người nam đi đâu?)
    INSERT INTO questions (
        mondai_type_id, question_text, image_url, audio_start_time, audio_end_time,
        transcript_jp, transcript_vi, explanation_vi, jlpt_level, skill_type, is_active
    ) VALUES (
        v_mondai_listening_task,
        '男の人と女の人が話しています。男の人はこのあとどこへ行きますか。',
        'https://images.unsplash.com/photo-1544717305-2782549b5136?auto=format&fit=crop&w=800&q=80',
        10, 45,
        '女：すみません、田中さん。この手紙を出してきてくれませんか。\n男：はい、いいですよ。郵便局ですね。\n女：ええ、おねがいします。',
        'Nữ: Xin lỗi Tanaka-san. Anh gửi giúp tôi bức thư này được không?\nNam: Vâng, được chứ. Đến bưu điện phải không ạ.\nNữ: Vâng, nhờ anh nhé.',
        'Người nam đồng ý đi gửi thư ở bưu điện (郵便局).',
        'N5', 'LISTENING', TRUE
    ) RETURNING id INTO v_q_id;
    INSERT INTO exam_section_questions (exam_id, section_id, question_id, question_number, points)
    VALUES (v_exam_id, v_sec_listening_id, v_q_id, 11, 1.00);
    INSERT INTO question_options (question_id, option_key, option_text, is_correct)
    VALUES (v_q_id, 1, '郵便局', TRUE), (v_q_id, 2, '銀行', FALSE), (v_q_id, 3, '駅', FALSE), (v_q_id, 4, '図書館', FALSE);

    -- Câu 12: Mondai 2 - ポイント理解 (Tranh 2x2 chọn mua đồ)
    INSERT INTO questions (
        mondai_type_id, question_text, has_visual_options, audio_start_time, audio_end_time,
        transcript_jp, transcript_vi, explanation_vi, jlpt_level, skill_type, is_active
    ) VALUES (
        v_mondai_listening_point,
        '女の子は何を買いましたか。',
        TRUE, 48, 85,
        '男：かわいい帽子ですね。買いましたか。\n女：いいえ、帽子は買いませんでした。かわいいかばんを買いました。',
        'Nam: Chiếc mũ dễ thương nhỉ. Bạn đã mua nó à?\nNữ: Không, tôi không mua mũ. Tôi đã mua một chiếc túi xách dễ thương.',
        'Bé gái nói không mua mũ mà mua chiếc túi xách (かばん).',
        'N5', 'LISTENING', TRUE
    ) RETURNING id INTO v_q_id;
    INSERT INTO exam_section_questions (exam_id, section_id, question_id, question_number, points)
    VALUES (v_exam_id, v_sec_listening_id, v_q_id, 12, 1.00);
    INSERT INTO question_options (question_id, option_key, option_text, option_image_url, is_correct)
    VALUES 
        (v_q_id, 1, 'Tranh 1: かばん (Túi xách)', 'https://images.unsplash.com/photo-1548036328-c9fa89d128fa?auto=format&fit=crop&w=300&q=80', TRUE),
        (v_q_id, 2, 'Tranh 2: ぼうし (Mũ)', 'https://images.unsplash.com/photo-1521369909029-2afed882baee?auto=format&fit=crop&w=300&q=80', FALSE),
        (v_q_id, 3, 'Tranh 3: くつ (Giày)', 'https://images.unsplash.com/photo-1549298916-b41d501d3772?auto=format&fit=crop&w=300&q=80', FALSE),
        (v_q_id, 4, 'Tranh 4: とけい (Đồng hồ)', 'https://images.unsplash.com/photo-1524805444758-089113d48a6d?auto=format&fit=crop&w=300&q=80', FALSE);

    -- Câu 13: Mondai 3 - 発話表現 (Diễn đạt lời nói - Nhìn tranh mũi tên ➡, 3 lựa chọn)
    INSERT INTO questions (
        mondai_type_id, question_text, image_url, audio_start_time, audio_end_time,
        transcript_jp, transcript_vi, explanation_vi, jlpt_level, skill_type, is_active
    ) VALUES (
        v_mondai_listening_utterance,
        'えを見ながら、しつもんをきいてください。やじるし（➡）のひとはなんと言いますか。',
        'https://images.unsplash.com/photo-1577563908411-5077b6dc7624?auto=format&fit=crop&w=600&q=80',
        88, 115,
        '質問：これからご飯を食べます。やじるしの人は何と言いますか。\n１：いただきます。\n２：ごちそうさまでした。\n３：いってきます。',
        'Câu hỏi: Bây giờ chuẩn bị ăn cơm. Người có mũi tên chỉ vào sẽ nói gì?\n1: Itadakimasu (Xin mời / Mời ăn cơm).\n2: Gochisousama deshita (Cảm ơn vì bữa ăn - nói sau khi ăn xong).\n3: Ittekimasu (Tôi đi đây - nói khi ra khỏi nhà).',
        'Trước khi ăn cơm người Nhật nói「いただきます」.',
        'N5', 'LISTENING', TRUE
    ) RETURNING id INTO v_q_id;
    INSERT INTO exam_section_questions (exam_id, section_id, question_id, question_number, points)
    VALUES (v_exam_id, v_sec_listening_id, v_q_id, 13, 1.00);
    INSERT INTO question_options (question_id, option_key, option_text, is_correct)
    VALUES 
        (v_q_id, 1, '1: いただきます', TRUE),
        (v_q_id, 2, '2: ごちそうさまでした', FALSE),
        (v_q_id, 3, '3: いってきます', FALSE);

    -- Câu 14: Mondai 4 - 即時応答 (Phản xạ câu trả lời nhanh - 3 lựa chọn, không in trên giấy)
    INSERT INTO questions (
        mondai_type_id, question_text, audio_start_time, audio_end_time,
        transcript_jp, transcript_vi, explanation_vi, jlpt_level, skill_type, is_active
    ) VALUES (
        v_mondai_listening_quick,
        '（問題用紙には何も印刷されていません）',
        118, 145,
        '女：きのうはどうもありがとうございました。\n１：いいえ、どういたしまして。\n２：はい、ありがとう。\n３：どうもすみません。',
        'Nữ: Hôm qua thực sự cảm ơn anh rất nhiều.\n1: Không có chi đâu ạ.\n2: Vâng, cảm ơn.\n3: Rất xin lỗi.',
        'Khi ai đó cảm ơn「ありがとうございました」, câu đáp lịch sự chuẩn xác là「いいえ、どういたしまして」.',
        'N5', 'LISTENING', TRUE
    ) RETURNING id INTO v_q_id;
    INSERT INTO exam_section_questions (exam_id, section_id, question_id, question_number, points)
    VALUES (v_exam_id, v_sec_listening_id, v_q_id, 14, 1.00);
    INSERT INTO question_options (question_id, option_key, option_text, is_correct)
    VALUES 
        (v_q_id, 1, '1', TRUE),
        (v_q_id, 2, '2', FALSE),
        (v_q_id, 3, '3', FALSE);

    -- Cập nhật tổng số câu của đề thi lên 14 câu
    UPDATE exams SET total_questions = 14 WHERE id = v_exam_id;

END $$;
