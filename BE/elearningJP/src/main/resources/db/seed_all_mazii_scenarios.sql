-- Seed đầy đủ các kịch bản chuẩn Mazii AI Speaking cho các chủ đề:
-- 1. LIVING_IN_JAPAN (Sống ở Nhật)
-- 2. FRIENDSHIP_CHATTING (Kết bạn & Trò chuyện)
-- 3. TRAVEL_TRANSPORT (Đi lại & Du lịch)
-- 4. DINING_SHOPPING (Đi ăn & Mua sắm)
-- 5. HELP_EMERGENCY (Lúc cần giúp đỡ)

-- Cập nhật lại Scenario ID 8 "Xin phép làm gì đó" với đầy đủ cấu trúc ngữ pháp mẫu như Screenshot 5
UPDATE ai_scenario
SET 
  thumbnail_url = 'https://images.unsplash.com/photo-1516280440614-37939bbacd81?w=500&auto=format&fit=crop&q=80',
  grammar = '[
    {
      "pattern": "Vてもいいでしょうか。",
      "meaning": "Tôi (làm gì) có được không ạ?",
      "example": "相席してもいいでしょうか。",
      "exampleMeaning": "Tôi có thể ngồi ghép bàn cùng anh không ạ?",
      "explanation": "Cấu trúc xin phép lịch sự trang trọng nhất của nhóm Vてもいい, dùng khi nói chuyện với người lạ hoặc cấp trên."
    },
    {
      "pattern": "Vてもいいですか？",
      "meaning": "Tôi (làm gì) có được không?",
      "example": "窓をあけてもいいですか？",
      "exampleMeaning": "Tôi mở cửa sổ được không?",
      "explanation": "Cấu trúc xin phép lịch sự thông thường, rất phổ biến trong giao tiếp hàng ngày."
    },
    {
      "pattern": "Vてもかまいませんか？",
      "meaning": "Tôi (làm gì) cũng không sao chứ ạ?",
      "example": "ここに座ってもかまいませんか？",
      "exampleMeaning": "Tôi ngồi ở đây cũng không sao chứ ạ?",
      "explanation": "Hỏi xem đối phương có cảm thấy phiền hay bất tiện nếu mình làm hành động đó hay không."
    }
  ]'::jsonb
WHERE id = 8;

-- Thêm các kịch bản còn lại trong "Sống ở Nhật" (LIVING_IN_JAPAN)
INSERT INTO ai_scenario (
    topic_id, jlpt_level, title, description, user_role, ai_role,
    tasks, sample_dialogue, vocabulary, grammar, tips, is_premium, thumbnail_url
) VALUES
(
    'LIVING_IN_JAPAN',
    'N5',
    'Đồng ý khi được xin phép',
    'Yamada hỏi mượn ô tô của tôi để tới hội thao của con gái. Trời có mưa nên tôi muốn cho mượn ô tô.',
    'Chủ xe (người bạn tốt bụng)',
    'Yamada (đồng nghiệp/bạn bè)',
    '[
        {"id": 0, "text": "Hỏi Yamada đi đâu và cần giúp gì."},
        {"id": 1, "text": "Đồng ý cho mượn xe nhiệt tình."},
        {"id": 2, "text": "Dặn dò lái xe cẩn thận vì trời mưa."}
    ]'::jsonb,
    '[
        {"speaker": "AI", "text": "あのう、明日車を貸してもらえませんか？"},
        {"speaker": "USER", "text": "ええ、いいですよ。何か用事ですか？"},
        {"speaker": "AI", "text": "娘の運動会があるんですが、雨が降りそうなんです。"},
        {"speaker": "USER", "text": "そうなんですね！どうぞ使ってください。気をつけて運転してくださいね。"},
        {"speaker": "AI", "text": "ありがとうございます！本当に助かります！"}
    ]'::jsonb,
    '[
        {"word": "車を貸す", "reading": "くるまをかす", "meaning": "Cho mượn xe ô tô"},
        {"word": "運動会", "reading": "うんどうかい", "meaning": "Hội thao thể thao trường học"},
        {"word": "運転する", "reading": "うんてんする", "meaning": "Lái xe, điều khiển phương tiện"},
        {"word": "助かる", "reading": "たすかる", "meaning": "Được cứu giúp, đỡ quá"}
    ]'::jsonb,
    '[
        {"pattern": "どうぞ〜てください", "meaning": "Xin mời cứ làm ~ đi", "example": "どうぞ使ってください。", "exampleMeaning": "Xin mời cứ dùng xe tự nhiên nhé."},
        {"pattern": "〜て運転してください", "meaning": "Hãy lái xe cẩn thận", "example": "気をつけて運転してください。", "exampleMeaning": "Hãy lái xe cẩn thận nhé."}
    ]'::jsonb,
    '["Khi đồng ý cho mượn đồ, người Nhật thường dùng ええ、いいですよ hoặc もちろんどうぞ.", "Kèm theo lời dặn dò chu đáo để tăng thêm sự gắn kết thân thiết."]'::jsonb,
    true,
    'https://images.unsplash.com/photo-1573496359142-b8d87734a5a2?w=500&auto=format&fit=crop&q=80'
),
(
    'LIVING_IN_JAPAN',
    'N5',
    'Từ chối khi được xin phép',
    'Shizuka chưa làm bài tập về nhà nên muốn chép bài của tôi. Bạn ấy nhờ nhưng tôi từ chối vì không muốn bạn ấy lười biếng.',
    'Học sinh chăm chỉ',
    'Shizuka (bạn cùng lớp)',
    '[
        {"id": 0, "text": "Lắng nghe lời nhờ vả của Shizuka."},
        {"id": 1, "text": "Khéo léo từ chối việc cho chép bài."},
        {"id": 2, "text": "Đề nghị hướng dẫn bạn tự làm bài."}
    ]'::jsonb,
    '[
        {"speaker": "AI", "text": "宿題を忘れてしまって...ノートを見せてもらえない？"},
        {"speaker": "USER", "text": "うーん、見せるのはちょっと...自分でやった方がいいよ。"},
        {"speaker": "AI", "text": "えー、お願い！今回だけだから！"},
        {"speaker": "USER", "text": "だめだよ。でも、わからないところを教えるのはできるよ！"},
        {"speaker": "AI", "text": "そっか、ありがとう。じゃあ教えてくれる？"}
    ]'::jsonb,
    '[
        {"word": "宿題", "reading": "しゅくだい", "meaning": "Bài tập về nhà"},
        {"word": "ノート", "reading": "ノート", "meaning": "Vở ghi bài"},
        {"word": "教える", "reading": "おしえる", "meaning": "Chỉ dạy, hướng dẫn"}
    ]'::jsonb,
    '[
        {"pattern": "〜のはちょっと...", "meaning": "Làm ~ thì hơi khó/ngại (từ chối khéo)", "example": "見せるのはちょっと...", "exampleMeaning": "Cho xem bài thì hơi ngại đấy..."},
        {"pattern": "〜た方がいい", "meaning": "Nên làm ~ thì tốt hơn", "example": "自分でやった方がいいよ。", "exampleMeaning": "Cậu nên tự làm thì tốt hơn đấy."}
    ]'::jsonb,
    '["Văn hóa Nhật rất ngại từ chối thẳng thừng だめ (Không được), thay vào đó hay nói ngập ngừng 〜はちょっと... để đối phương tự hiểu."]'::jsonb,
    true,
    'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=500&auto=format&fit=crop&q=80'
),
(
    'LIVING_IN_JAPAN',
    'N4',
    'Chào hàng xóm mới',
    'Tôi đang sang nhà hàng xóm để chào hỏi và tặng quà ra mắt sau khi chuyển đến khu phố mới.',
    'Người mới chuyển đến khu phố',
    'Bác hàng xóm tốt bụng (Yamamoto)',
    '[
        {"id": 0, "text": "Bấm chuông và tự giới thiệu vừa chuyển tới nhà bên cạnh."},
        {"id": 1, "text": "Tặng món quà nhỏ ra mắt (hộp khăn hoặc bánh kẹo)."},
        {"id": 2, "text": "Nói câu chào kết thúc nhờ giúp đỡ từ nay về sau."}
    ]'::jsonb,
    '[
        {"speaker": "USER", "text": "こんにちは！お忙しいところすみません。隣の201号室に引っ越してきたナムと申します。"},
        {"speaker": "AI", "text": "あら、ご丁寧に！はじめまして、山本です。"},
        {"speaker": "USER", "text": "これ、心ばかりのものですが、どうぞ。"},
        {"speaker": "AI", "text": "まあ、ありがとうございます！お気遣いなく。"},
        {"speaker": "USER", "text": "これからお世話になります。よろしくお願いいたします。"},
        {"speaker": "AI", "text": "こちらこそ、何か困ったことがあったら何でも聞いてくださいね。"}
    ]'::jsonb,
    '[
        {"word": "引っ越す", "reading": "ひっこす", "meaning": "Chuyển nhà"},
        {"word": "ご丁寧に", "reading": "ごていねいに", "meaning": "Chu đáo quá, lịch sự quá"},
        {"word": "心ばかり", "reading": "こころばかり", "meaning": "Chút quà mọn (thể hiện sự khiêm tốn)"},
        {"word": "お世話になる", "reading": "おせわになる", "meaning": "Nhận được sự giúp đỡ, chăm sóc"}
    ]'::jsonb,
    '[
        {"pattern": "〜と申します (もうします)", "meaning": "Tôi tên là ~ (khiêm nhường ngữ)", "example": "ナムと申します。", "exampleMeaning": "Tôi tên là Nam ạ."},
        {"pattern": "心ばかりのものですが", "meaning": "Đây chỉ là chút lòng mọn xin gửi tặng", "example": "これ、心ばかりのものですが、どうぞ。", "exampleMeaning": "Đây là chút quà mọn gửi tặng bác, xin mời bác nhận ạ."}
    ]'::jsonb,
    '["Văn hóa 引越し挨拶 (chào hỏi khi chuyển nhà) là nét văn hóa truyền thống quan trọng ở Nhật Bản để xây dựng tình làng nghĩa xóm."]'::jsonb,
    true,
    'https://images.unsplash.com/photo-1576267423445-b2e0074d68a4?w=500&auto=format&fit=crop&q=80'
);

-- Thêm kịch bản chuẩn trong "Kết bạn & Trò chuyện" (FRIENDSHIP_CHATTING)
INSERT INTO ai_scenario (
    topic_id, jlpt_level, title, description, user_role, ai_role,
    tasks, sample_dialogue, vocabulary, grammar, tips, is_premium, thumbnail_url
) VALUES
(
    'FRIENDSHIP_CHATTING',
    'N5',
    'Lần đầu làm quen',
    'Bạn tham gia câu lạc bộ giao lưu văn hóa và gặp một người bạn Nhật Bản lần đầu tiên.',
    'Học viên mới',
    'Kenji (thành viên câu lạc bộ)',
    '[
        {"id": 0, "text": "Chào hỏi và giới thiệu tên tuổi, quê quán."},
        {"id": 1, "text": "Hỏi thăm Kenji về sở thích âm nhạc và phim ảnh."},
        {"id": 2, "text": "Trao đổi tài khoản mạng xã hội để giữ liên lạc."}
    ]'::jsonb,
    '[
        {"speaker": "AI", "text": "こんにちは！初めまして、健二です。"},
        {"speaker": "USER", "text": "初めまして！ミンです。ベトナムから来ました。よろしくお願いします！"},
        {"speaker": "AI", "text": "ミンさん、よろしくね！日本はどうですか？もう慣れましたか？"},
        {"speaker": "USER", "text": "はい、とても楽しいです！健二さんは休みの日は何をしていますか？"},
        {"speaker": "AI", "text": "アニメを見たり、音楽を聴いたりしているよ。ミンさんは？"},
        {"speaker": "USER", "text": "私もアニメが大好きです！もしよかったらLINEを交換しませんか？"},
        {"speaker": "AI", "text": "いいね！交換しよう！"}
    ]'::jsonb,
    '[
        {"word": "慣れる", "reading": "なれる", "meaning": "Quen với, thích nghi"},
        {"word": "交換する", "reading": "こうかんする", "meaning": "Trao đổi (thông tin, liên lạc)"},
        {"word": "休みの日", "reading": "やすみのひ", "meaning": "Ngày nghỉ"}
    ]'::jsonb,
    '[
        {"pattern": "〜たり〜たりする", "meaning": "Làm những việc như là ~ và ~", "example": "アニメを見たり、音楽を聴いたりしているよ。", "exampleMeaning": "Tớ hay xem anime và nghe nhạc."},
        {"pattern": "〜たらどうですか / 〜ませんか", "meaning": "Cùng làm ~ nhé?", "example": "LINEを交換しませんか？", "exampleMeaning": "Chúng mình kết bạn LINE nhé?"}
    ]'::jsonb,
    '["Dùng câu もしよかったら trước khi xin thông tin liên lạc để thể hiện sự lịch sự và không ép buộc đối phương."]'::jsonb,
    false,
    'https://images.unsplash.com/photo-1529156069898-49953e39b3ac?w=500&auto=format&fit=crop&q=80'
),
(
    'FRIENDSHIP_CHATTING',
    'N4',
    'Rủ bạn đi xem phim cuối tuần',
    'Bộ phim hoạt hình mới của Ghibli vừa ra rạp. Hãy rủ người bạn thân cùng đi xem vào chiều thứ Bảy.',
    'Bạn rủ đi xem phim',
    'Aoi (bạn thân)',
    '[
        {"id": 0, "text": "Hỏi thăm kế hoạch cuối tuần này của Aoi."},
        {"id": 1, "text": "Đề xuất cùng đi xem phim hoạt hình mới."},
        {"id": 2, "text": "Hẹn giờ và địa điểm gặp mặt tại rạp chiếu phim."}
    ]'::jsonb,
    '[
        {"speaker": "USER", "text": "あおいちゃん、今週の土曜日って何か予定ある？"},
        {"speaker": "AI", "text": "ううん、特に何もないよ。どうしたの？"},
        {"speaker": "USER", "text": "ジブリの新作映画が公開されたんだけど、一緒に見に行かない？"},
        {"speaker": "AI", "text": "本当？見に行きたい！何時から？"},
        {"speaker": "USER", "text": "午後2時の回はどう？渋谷の映画館で1時半に待ち合わせしよう！"},
        {"speaker": "AI", "text": "オッケー！楽しみにしているね！"}
    ]'::jsonb,
    '[
        {"word": "予定", "reading": "よてい", "meaning": "Kế hoạch, dự định"},
        {"word": "公開される", "reading": "こうかいされる", "meaning": "Được công chiếu, ra rạp"},
        {"word": "待ち合わせ", "reading": "まちあわせ", "meaning": "Hẹn gặp"}
    ]'::jsonb,
    '[
        {"pattern": "〜に行かない？", "meaning": "Cùng đi ~ không nào?", "example": "一緒に見に行かない？", "exampleMeaning": "Cậu có muốn cùng đi xem không?"},
        {"pattern": "〜はどう？", "meaning": "Thế còn ~ thì sao?", "example": "午後2時の回はどう？", "exampleMeaning": "Suất chiếu 2 giờ chiều thì sao?"}
    ]'::jsonb,
    '["Dùng thể thông thường (thể ngắn - 辞書形/ない形) khi nói chuyện với bạn bè thân thiết để tạo cảm giác gần gũi."]'::jsonb,
    true,
    'https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=500&auto=format&fit=crop&q=80'
);

-- Thêm kịch bản trong "Đi lại & Du lịch" (TRAVEL_TRANSPORT)
INSERT INTO ai_scenario (
    topic_id, jlpt_level, title, description, user_role, ai_role,
    tasks, sample_dialogue, vocabulary, grammar, tips, is_premium, thumbnail_url
) VALUES
(
    'TRAVEL_TRANSPORT',
    'N4',
    'Mua vé tàu Shinkansen tại quầy',
    'Bạn muốn đi từ Tokyo đến Kyoto bằng tàu Shinkansen. Hãy tới quầy vé Midori no Madoguchi để mua vé và chọn ghế.',
    'Hành khách mua vé',
    'Nhân viên bán vé nhà ga',
    '[
        {"id": 0, "text": "Nói điểm đến (Kyoto) và thời gian muốn đi (sáng mai)."},
        {"id": 1, "text": "Chọn loại ghế (toa chỉ định ghế 指定席 và ghế cạnh cửa sổ 窓側)."},
        {"id": 2, "text": "Xác nhận giá tiền và thanh toán bằng thẻ tín dụng."}
    ]'::jsonb,
    '[
        {"speaker": "AI", "text": "いらっしゃいませ。どちらまででしょうか？"},
        {"speaker": "USER", "text": "京都までの新幹線の切符を1枚お願いします。"},
        {"speaker": "AI", "text": "指定席と自由席のどちらがよろしいですか？"},
        {"speaker": "USER", "text": "指定席でお願いします。できれば窓側の席がいいです。"},
        {"speaker": "AI", "text": "かしこまりました。明日の午前9時発ののぞみ号でお取りできました。14,000円になります。"},
        {"speaker": "USER", "text": "クレジットカードで支払えますか？"},
        {"speaker": "AI", "text": "はい、こちらにカードをタッチしてください。"}
    ]'::jsonb,
    '[
        {"word": "指定席", "reading": "していせき", "meaning": "Ghế đã đặt trước, ghế chỉ định"},
        {"word": "自由席", "reading": "じゆうせき", "meaning": "Ghế tự do, không giữ chỗ"},
        {"word": "窓側", "reading": "まどがわ", "meaning": "Phía cạnh cửa sổ"},
        {"word": "通路側", "reading": "つうろがわ", "meaning": "Phía lối đi giữa"}
    ]'::jsonb,
    '[
        {"pattern": "〜までのお願いします", "meaning": "Làm ơn cho tôi vé đến ~", "example": "京都までの新幹線の切符を1枚お願いします。", "exampleMeaning": "Cho tôi 1 vé Shinkansen đến Kyoto."},
        {"pattern": "〜で支払えますか", "meaning": "Tôi có thể thanh toán bằng ~ được không?", "example": "クレジットカードで支払えますか？", "exampleMeaning": "Tôi có thể thanh toán bằng thẻ tín dụng không?"}
    ]'::jsonb,
    '["Học thuộc hai từ cực kỳ quan trọng khi đi tàu Nhật: 窓側 (Madogawa - cạnh cửa sổ ngắm cảnh) và 通路側 (Tsuurogawa - cạnh lối đi dễ đứng lên)."]'::jsonb,
    false,
    'https://images.unsplash.com/photo-1542051841857-5f90071e7989?w=500&auto=format&fit=crop&q=80'
);

-- Thêm kịch bản trong "Đi ăn & Mua sắm" (DINING_SHOPPING)
INSERT INTO ai_scenario (
    topic_id, jlpt_level, title, description, user_role, ai_role,
    tasks, sample_dialogue, vocabulary, grammar, tips, is_premium, thumbnail_url
) VALUES
(
    'DINING_SHOPPING',
    'N5',
    'Mua sắm tại cửa hàng tiện lợi Konbini',
    'Bạn vào 7-Eleven mua cơm nắm Onigiri và nước ngọt. Hãy trả lời các câu hỏi quen thuộc của thu ngân.',
    'Khách mua hàng',
    'Nhân viên thu ngân Konbini',
    '[
        {"id": 0, "text": "Mang đồ ra quầy tính tiền."},
        {"id": 1, "text": "Từ chối túi nilon hoặc xin hâm nóng đồ ăn."},
        {"id": 2, "text": "Thanh toán bằng tiền mặt hoặc thẻ IC (Suica/Pasmo)."}
    ]'::jsonb,
    '[
        {"speaker": "AI", "text": "いらっしゃいませ。お弁当温めますか？"},
        {"speaker": "USER", "text": "はい、お願いします。"},
        {"speaker": "AI", "text": "レジ袋はご利用になりますか？"},
        {"speaker": "USER", "text": "いえ、大丈夫です。袋はいりません。"},
        {"speaker": "AI", "text": "お会計650円になります。"},
        {"speaker": "USER", "text": "Suicaで支払います。"},
        {"speaker": "AI", "text": "端末にタッチしてください。ありがとうございました！"}
    ]'::jsonb,
    '[
        {"word": "温める", "reading": "あたためる", "meaning": "Hâm nóng (bằng lò vi sóng)"},
        {"word": "レジ袋", "reading": "レジぶくろ", "meaning": "Túi nilon đựng hàng"},
        {"word": "端末", "reading": "たんまつ", "meaning": "Thiết bị quẹt thẻ, máy POS"}
    ]'::jsonb,
    '[
        {"pattern": "〜はいりません / 大丈夫です", "meaning": "Không cần ~ đâu ạ (từ chối lịch sự)", "example": "袋はいりません。大丈夫です。", "exampleMeaning": "Tôi không cần túi đâu ạ, tôi có túi rồi."},
        {"pattern": "〜で支払います", "meaning": "Tôi thanh toán bằng ~", "example": "Suicaで支払います。", "exampleMeaning": "Tôi thanh toán bằng thẻ Suica."}
    ]'::jsonb,
    '["Túi nilon ở các cửa hàng tiện lợi Nhật Bản có tính phí (3-5 yên). Nếu mang túi riêng hãy nói 袋は大丈夫です (Fukuro wa daijoubu desu)."]'::jsonb,
    true,
    'https://images.unsplash.com/photo-1542838132-92c53300491e?w=500&auto=format&fit=crop&q=80'
);
