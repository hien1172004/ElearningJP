INSERT INTO ai_scenario (
    topic_id, jlpt_level, title, description, user_role, ai_role,
    tasks, sample_dialogue, vocabulary, grammar, tips, is_premium, thumbnail_url
) VALUES (
    'LIVING_IN_JAPAN',
    'N5',
    'Xin phép làm gì đó',
    'Quán đã hết chỗ nên tôi muốn ngồi ghép bàn với một vị khách khác. Biết anh ấy là người Hàn Quốc, tôi muốn được kết bạn trên LINE.',
    'Một khách hàng vào quán lúc đông khách, khá thích Hàn Quốc và nói được tiếng Hàn',
    'Một vị khách Hàn Quốc đang ngồi tại một trong tại quán',
    '[
        {"id": 0, "text": "Bắt chuyện và xin phép ngồi ghép bàn."},
        {"id": 1, "text": "Xin phép để ba lô ở ghế bên cạnh."},
        {"id": 2, "text": "Bày tỏ mong muốn kết bạn với vị khách người Hàn Quốc."}
    ]'::jsonb,
    '[
        {"speaker": "USER", "text": "すみません。"},
        {"speaker": "AI", "text": "はい、何でしょうか？"},
        {"speaker": "USER", "text": "お店が満席なんですが、相席してもいいでしょうか。"},
        {"speaker": "AI", "text": "もちろんです。どうぞ。"},
        {"speaker": "USER", "text": "ありがとうございます。隣の席にバッグを置いてもいいですか？"},
        {"speaker": "AI", "text": "ええ、大丈夫ですよ。"},
        {"speaker": "USER", "text": "助かります！もしかして韓国の方ですか？LINEで友達を追加してもいいですか？"},
        {"speaker": "AI", "text": "はい、韓国から来ました！いいですよ、交換しましょう！"}
    ]'::jsonb,
    '[
        {"word": "満席", "reading": "まんせき", "meaning": "Sự hết chỗ ngồi"},
        {"word": "相席する", "reading": "あいせきする", "meaning": "Ngồi ghép bàn"},
        {"word": "バッグ", "reading": "バッグ", "meaning": "Cái túi, cái cặp"},
        {"word": "やっぱり", "reading": "やっぱり", "meaning": "Quả nhiên, quả vậy (tỏ ý sự việc xảy ra đúng như dự đoán của mình)"},
        {"word": "ラインで友達を追加する", "reading": "らいんでともだちをついかする", "meaning": "Kết bạn trên LINE"},
        {"word": "理由", "reading": "りゆう", "meaning": "Lý do"},
        {"word": "遅刻する", "reading": "ちこくする", "meaning": "Trễ giờ, đến muộn"},
        {"word": "駐車する", "reading": "ちゅうしゃする", "meaning": "Đậu xe"}
    ]'::jsonb,
    '[
        {"pattern": "〜てもいいでしょうか / 〜てもいいですか", "meaning": "Tôi làm ~ có được không ạ? (Xin phép lịch sự)"},
        {"pattern": "〜んですが", "meaning": "Mào đầu câu xin phép / nhờ vả tự nhiên"}
    ]'::jsonb,
    '[
        "Khi xin phép ngồi ghép bàn tại quán ăn Nhật đông đúc, luôn dùng cụm 相席してもいいでしょうか để thể hiện sự lịch sự.",
        "Mở đầu bằng すみません và kết thúc bằng ありがとうございます khi được đối phương đồng ý."
    ]'::jsonb,
    false,
    'https://images.unsplash.com/photo-1555396273-367ea4eb4db5?w=500&auto=format&fit=crop&q=60'
);
