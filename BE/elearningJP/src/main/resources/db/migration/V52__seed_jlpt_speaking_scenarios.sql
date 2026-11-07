-- =============================================================================
-- Flyway Database Migration: V51__seed_jlpt_speaking_scenarios.sql
-- Module: AI Speaking Practice - Rich Scenario Bank for All JLPT Levels (N5 - N1)
-- =============================================================================

INSERT INTO ai_scenario (
    topic_id, jlpt_level, title, description, user_role, ai_role,
    tasks, sample_dialogue, vocabulary, grammar, tips, is_premium, thumbnail_url
) VALUES
-- -----------------------------------------------------------------------------
-- 1. [N5] Mua đồ ở cửa hàng tiện lợi Konbini (DINING_SHOPPING)
-- -----------------------------------------------------------------------------
(
    'DINING_SHOPPING',
    'N5',
    'Mua đồ ở cửa hàng tiện lợi Konbini (コンビニでお買い物)',
    'Bạn ghé vào cửa hàng tiện lợi Konbini để mua cơm nắm và nước uống. Hãy chào nhân viên, yêu cầu hâm nóng đồ ăn và chọn túi nilon phù hợp.',
    'Khách mua hàng',
    'Nhân viên Konbini (Maziiko)',
    $$[
        {"id": 0, "text": "Chào nhân viên và đặt các món đồ cần mua lên quầy thanh toán"},
        {"id": 1, "text": "Trả lời khi nhân viên hỏi có cần hâm nóng cơm nắm không (あたためますか)"},
        {"id": 2, "text": "Nói rõ có lấy túi nilon hay không (ふくろはいりません / おねがいします)"}
    ]$$::jsonb,
    $$[
        {"speaker": "AI", "text": "いらっしゃいませ！お弁当あたためますか？", "vi": "Kính chào quý khách! Cơm hộp có cần hâm nóng không ạ?"},
        {"speaker": "USER", "text": "はい、おねがいします。", "vi": "Vâng, xin vui lòng hâm nóng giúp tôi."},
        {"speaker": "AI", "text": "かしこまりました。レジ袋はご利用になりますか？", "vi": "Tôi hiểu rồi. Quý khách có dùng túi nilon không ạ?"},
        {"speaker": "USER", "text": "いいえ、けっこうです。バッグがあります。", "vi": "Không, tôi không cần ạ. Tôi có túi rồi."},
        {"speaker": "AI", "text": "お会計、全部で５５０円になります。", "vi": "Tổng cộng tiền thanh toán là 550 yên ạ."},
        {"speaker": "USER", "text": "カードではらいます。ありがとうございます。", "vi": "Tôi thanh toán bằng thẻ. Cảm ơn bạn."}
    ]$$::jsonb,
    $$[
        {"word": "あたためる", "reading": "温める", "meaning": "Hâm nóng"},
        {"word": "レジぶくろ", "reading": "レジ袋", "meaning": "Túi nilon đựng đồ tại quầy"},
        {"word": "おかいけい", "reading": "お会計", "meaning": "Thanh toán tiền"}
    ]$$::jsonb,
    $$[
        {"pattern": "～をおねがいします", "meaning": "Làm ơn cho tôi...", "example": "これをおねがいします。", "exampleMeaning": "Làm ơn cho tôi cái này."},
        {"pattern": "～はけっこうです", "meaning": "...thì không cần / đủ rồi", "example": "ふくろはけっこうです。", "exampleMeaning": "Túi nilon thì không cần ạ."}
    ]$$::jsonb,
    $$[
        "Tại Konbini Nhật, nhân viên thường hỏi 2 câu quen thuộc: 'あたためますか' (Có hâm nóng không) và 'レジ袋はご利用ですか' (Có lấy túi không)."
    ]$$::jsonb,
    false,
    'https://images.unsplash.com/photo-1569718212165-3a8278d5f624?w=600&auto=format&fit=crop&q=80'
),

-- -----------------------------------------------------------------------------
-- 2. [N5] Hỏi cách mua vé và đi tàu điện (TRAVEL_TRANSPORT)
-- -----------------------------------------------------------------------------
(
    'TRAVEL_TRANSPORT',
    'N5',
    'Hỏi cách mua vé và đi tàu điện (電車の切符の買い方を聞く)',
    'Bạn đang ở ga tàu điện và muốn mua vé đi Asakusa nhưng máy bán vé tự động khó sử dụng. Hãy hỏi nhân viên nhà ga để được hướng dẫn.',
    'Hành khách đi tàu',
    'Nhân viên nhà ga',
    $$[
        {"id": 0, "text": "Chào hỏi và nói bạn muốn mua vé đến ga Asakusa"},
        {"id": 1, "text": "Hỏi giá vé là bao nhiêu tiền và cách bấm chọn trên máy"},
        {"id": 2, "text": "Hỏi tàu đi Asakusa xuất phát ở đường ray số mấy (何番線)"}
    ]$$::jsonb,
    $$[
        {"speaker": "USER", "text": "すみません、浅草までの切符を買いたいんですが。", "vi": "Xin lỗi, tôi muốn mua vé đi đến Asakusa ạ."},
        {"speaker": "AI", "text": "浅草ですね。運賃は２１０円になります。こちらの券売機で「２１０」のボタンを押してください。", "vi": "Asakusa phải không ạ. Giá vé là 210 yên. Hãy bấm nút '210' trên máy bán vé này nhé."},
        {"speaker": "USER", "text": "わかりました。何番線に乗ればいいですか？", "vi": "Tôi hiểu rồi. Tôi nên lên tàu ở đường ray số mấy ạ?"},
        {"speaker": "AI", "text": "２番線の銀座線にお乗りください。", "vi": "Quý khách hãy lên tuyến Ginza ở đường ray số 2 nhé."},
        {"speaker": "USER", "text": "どうもありがとうございます！", "vi": "Cảm ơn bác rất nhiều ạ!"}
    ]$$::jsonb,
    $$[
        {"word": "きっぷ", "reading": "切符", "meaning": "Vé tàu"},
        {"word": "けんばいき", "reading": "券売機", "meaning": "Máy bán vé tự động"},
        {"word": "なんばんせん", "reading": "何番線", "meaning": "Đường ray số mấy"}
    ]$$::jsonb,
    $$[
        {"pattern": "～までの～をかいたいんですが", "meaning": "Tôi muốn mua... đi đến...", "example": "東京までの切符を買いたいんですが。", "exampleMeaning": "Tôi muốn mua vé đi đến Tokyo ạ."}
    ]$$::jsonb,
    $$[
        "Khi cần sự giúp đỡ ở ga, dùng 'すみません' mở đầu và '～んですが' để giải thích hoàn cảnh một cách lịch sự."
    ]$$::jsonb,
    false,
    'https://images.unsplash.com/photo-1542051841857-5f90071e7989?w=600&auto=format&fit=crop&q=80'
),

-- -----------------------------------------------------------------------------
-- 3. [N5] Khám bệnh và miêu tả triệu chứng tại phòng khám (HELP_EMERGENCY)
-- -----------------------------------------------------------------------------
(
    'HELP_EMERGENCY',
    'N5',
    'Khám bệnh và miêu tả triệu chứng tại phòng khám (病院で症状を説明する)',
    'Bạn bị sốt nhẹ và đau họng từ hôm qua nên đến phòng khám gần nhà. Hãy giải thích tình trạng sức khỏe của bạn cho bác sĩ.',
    'Bệnh nhân',
    'Bác sĩ phòng khám',
    $$[
        {"id": 0, "text": "Chào bác sĩ và trình bày bị sốt và đau họng từ khi nào"},
        {"id": 1, "text": "Trả lời câu hỏi của bác sĩ về nhiệt độ sốt và triệu chứng khác"},
        {"id": 2, "text": "Lắng nghe hướng dẫn uống thuốc và cảm ơn bác sĩ"}
    ]$$::jsonb,
    $$[
        {"speaker": "AI", "text": "今日はどうされましたか？", "vi": "Hôm nay em cảm thấy thế nào trong người?"},
        {"speaker": "USER", "text": "きのうから喉が痛くて、熱もあります。", "vi": "Từ hôm qua cổ họng em bị đau và cũng có sốt nữa ạ."},
        {"speaker": "AI", "text": "熱は何度くらいありますか？咳は出ますか？", "vi": "Em sốt khoảng bao nhiêu độ? Có bị ho không?"},
        {"speaker": "USER", "text": "３８度くらいです。咳も少し出ます。", "vi": "Khoảng 38 độ ạ. Em cũng bị ho một chút."},
        {"speaker": "AI", "text": "風邪ですね。３日分の薬を出しておきます。食後に飲んでゆっくり休んでください。", "vi": "Em bị cảm rồi. Bác sĩ kê thuốc 3 ngày nhé. Hãy uống sau bữa ăn và nghỉ ngơi điều độ."},
        {"speaker": "USER", "text": "はい、わかりました。ありがとうございました。", "vi": "Vâng, em hiểu rồi ạ. Em cảm ơn bác sĩ."}
    ]$$::jsonb,
    $$[
        {"word": "のどがいたい", "reading": "喉が痛い", "meaning": "Đau họng"},
        {"word": "ねつ", "reading": "熱", "meaning": "Sốt"},
        {"word": "しょくご", "reading": "食後", "meaning": "Sau bữa ăn"}
    ]$$::jsonb,
    $$[
        {"pattern": "～が痛いです", "meaning": "Bị đau ở...", "example": "頭が痛いです。", "exampleMeaning": "Tôi bị đau đầu."}
    ]$$::jsonb,
    $$[
        "Khi nói về nhiệt độ cơ thể ở Nhật, dùng '度 (ど)' cho số độ chẵn và '分 (ぶ)' cho số lẻ, ví dụ 37度5分 (37 độ 5)."
    ]$$::jsonb,
    false,
    'https://images.unsplash.com/photo-1513407030348-c983a97b98d8?w=600&auto=format&fit=crop&q=80'
),

-- -----------------------------------------------------------------------------
-- 4. [N4] Mua sắm quần áo và thử đồ (DINING_SHOPPING)
-- -----------------------------------------------------------------------------
(
    'DINING_SHOPPING',
    'N4',
    'Mua sắm quần áo và thử đồ (服屋で試着と買い物)',
    'Bạn tìm thấy một chiếc áo len ưng ý tại cửa hàng thời trang nhưng muốn thử size khác và hỏi về màu sắc.',
    'Khách mua hàng',
    'Nhân viên bán hàng',
    $$[
        {"id": 0, "text": "Xin phép nhân viên cho thử áo vào phòng thay đồ (試着してもいいですか)"},
        {"id": 1, "text": "Hỏi xem có cỡ áo lớn hơn hoặc màu sắc khác không"},
        {"id": 2, "text": "Quyết định lấy chiếc áo vừa vặn và thanh toán"}
    ]$$::jsonb,
    $$[
        {"speaker": "USER", "text": "すみません、このセーターを試着してもいいですか？", "vi": "Xin lỗi, tôi có thể mặc thử chiếc áo len này được không ạ?"},
        {"speaker": "AI", "text": "もちろんです！試着室はこちらへどうぞ。", "vi": "Tất nhiên rồi ạ! Xin mời quý khách sang phòng thử đồ bên này."},
        {"speaker": "USER", "text": "ちょっと小さいんですが、Ｍサイズはありますか？", "vi": "Hơi chật một chút, có cỡ M không bạn?"},
        {"speaker": "AI", "text": "はい、少々お待ちください。...こちらがＭサイズでございます。", "vi": "Vâng, xin quý khách đợi một chút. ...Đây là cỡ M ạ."},
        {"speaker": "USER", "text": "ちょうどいいです。これにします。", "vi": "Vừa vặn rồi ạ. Tôi lấy chiếc này."}
    ]$$::jsonb,
    $$[
        {"word": "しちゃく", "reading": "試着", "meaning": "Thử đồ, mặc thử"},
        {"word": "ちょうどいい", "reading": "丁度いい", "meaning": "Vừa vặn, vừa khít"},
        {"word": "セーター", "reading": "セーター", "meaning": "Áo len"}
    ]$$::jsonb,
    $$[
        {"pattern": "～てもいいですか", "meaning": "Tôi làm... có được không?", "example": "写真を撮ってもいいですか。", "exampleMeaning": "Tôi chụp ảnh có được không?"},
        {"pattern": "～にします", "meaning": "Tôi quyết định chọn...", "example": "これにします。", "exampleMeaning": "Tôi lấy cái này."}
    ]$$::jsonb,
    $$[
        "Tại các phòng thay đồ ở Nhật, nữ giới thường được đưa cho một chiếc túi che mặt (face cover) để tránh làm dính phấn son lên áo."
    ]$$::jsonb,
    false,
    'https://images.unsplash.com/photo-1441986300917-64674bd600d8?w=600&auto=format&fit=crop&q=80'
),

-- -----------------------------------------------------------------------------
-- 5. [N4] Đặt phòng khách sạn và nhận phòng check-in (TRAVEL_TRANSPORT)
-- -----------------------------------------------------------------------------
(
    'TRAVEL_TRANSPORT',
    'N4',
    'Đặt phòng khách sạn và nhận phòng check-in (ホテルのチェックイン)',
    'Bạn vừa đến khách sạn tại Kyoto sau chuyến đi dài. Hãy hoàn tất thủ tục nhận phòng (check-in) và hỏi về giờ ăn sáng cùng mật khẩu Wi-Fi.',
    'Khách lưu trú',
    'Lễ tân khách sạn',
    $$[
        {"id": 0, "text": "Nói tên và báo rằng bạn đã đặt phòng trước qua mạng"},
        {"id": 1, "text": "Điền thông tin vào phiếu đăng ký lưu trú theo hướng dẫn"},
        {"id": 2, "text": "Hỏi giờ phục vụ bữa sáng và xin mật khẩu Wi-Fi của phòng"}
    ]$$::jsonb,
    $$[
        {"speaker": "USER", "text": "こんばんは。チェックインをお願いします。ネットで予約したグエンです。", "vi": "Chào buổi tối. Tôi muốn làm thủ tục nhận phòng. Tôi là Nguyen đã đặt qua mạng."},
        {"speaker": "AI", "text": "グエン様ですね。ご予約を確認いたしました。こちらの宿泊カードにご記入をお願いできますか？", "vi": "Quý khách Nguyen phải không ạ. Chúng tôi đã xác nhận thông tin đặt phòng. Quý khách vui lòng điền vào phiếu lưu trú này giúp tôi nhé?"},
        {"speaker": "USER", "text": "はい、書けました。朝食は何時からですか？", "vi": "Vâng, tôi viết xong rồi. Bữa sáng phục vụ từ mấy giờ ạ?"},
        {"speaker": "AI", "text": "朝食は１階のレストランで朝７時から９時半まででございます。お部屋の鍵とWi-Fiの案内はこちらです。", "vi": "Bữa sáng tại nhà hàng tầng 1 từ 7:00 đến 9:30 sáng ạ. Đây là chìa khóa phòng và hướng dẫn Wi-Fi ạ."},
        {"speaker": "USER", "text": "ありがとうございます。お世話になります。", "vi": "Cảm ơn bạn rất nhiều. Nhờ bạn giúp đỡ nhé."}
    ]$$::jsonb,
    $$[
        {"word": "チェックイン", "reading": "チェックイン", "meaning": "Thủ tục nhận phòng khách sạn"},
        {"word": "ちょうしょく", "reading": "朝食", "meaning": "Bữa ăn sáng"},
        {"word": "よやく", "reading": "予約", "meaning": "Đặt trước, đặt phòng"}
    ]$$::jsonb,
    $$[
        {"pattern": "～をお願いします", "meaning": "Xin vui lòng làm giúp tôi...", "example": "チェックインをお願いします。", "exampleMeaning": "Làm ơn cho tôi làm thủ tục nhận phòng."}
    ]$$::jsonb,
    $$[
        "Câu chào 'お世話になります' rất lịch sự khi bắt đầu lưu trú hoặc nhận dịch vụ tại các khách sạn hay lữ quán Ryokan Nhật Bản."
    ]$$::jsonb,
    false,
    'https://images.unsplash.com/photo-1542314831-068cd1dbfeeb?w=600&auto=format&fit=crop&q=80'
),

-- -----------------------------------------------------------------------------
-- 6. [N4] Báo rơi mất ví tại đồn cảnh sát Koban (HELP_EMERGENCY)
-- -----------------------------------------------------------------------------
(
    'HELP_EMERGENCY',
    'N4',
    'Báo rơi mất ví tại đồn cảnh sát Koban (交番で落とし物の届出)',
    'Bạn để quên ví trên tàu điện hoặc gần ga. Hãy vào đồn cảnh sát Koban gần nhất để báo mất và làm thủ tục tìm kiếm đồ thất lạc.',
    'Người báo mất đồ',
    'Cảnh sát trực ban (お巡りさん)',
    $$[
        {"id": 0, "text": "Thông báo bị mất ví và thời gian, địa điểm ước chừng"},
        {"id": 1, "text": "Mô tả đặc điểm chiếc ví (màu sắc, giấy tờ và số tiền bên trong)"},
        {"id": 2, "text": "Để lại họ tên và số điện thoại liên lạc khi tìm thấy"}
    ]$$::jsonb,
    $$[
        {"speaker": "USER", "text": "すみません、財布を落としてしまったんですが...", "vi": "Xin lỗi bác cảnh sát, cháu lỡ làm đánh rơi mất ví tiền rồi ạ..."},
        {"speaker": "AI", "text": "それは大変ですね。いつ、どのあたりで落としましたか？", "vi": "Thế thì phiền phức rồi. Cháu đánh rơi lúc nào và ở khu vực nào vậy?"},
        {"speaker": "USER", "text": "さっき１時間前、新宿駅のホームだと思います。黒い革の財布で、中に在留カードが入っています。", "vi": "Cháu nghĩ khoảng 1 tiếng trước ở sân ga Shinjuku ạ. Chiếc ví da màu đen, bên trong có thẻ lưu trú của cháu."},
        {"speaker": "AI", "text": "わかりました。見つかったら連絡しますので、こちらの用紙に名前と電話番号を書いてください。", "vi": "Bác hiểu rồi. Khi nào tìm thấy bác sẽ liên hệ ngay, cháu ghi tên và số điện thoại vào tờ này nhé."},
        {"speaker": "USER", "text": "よろしくお願いします。見つかるといいのですが...", "vi": "Cháu nhờ bác ạ. Mong là sẽ tìm thấy ví..."}
    ]$$::jsonb,
    $$[
        {"word": "おとす", "reading": "落とす", "meaning": "Làm rơi, đánh rơi"},
        {"word": "さいふ", "reading": "財布", "meaning": "Ví tiền"},
        {"word": "ざいりゅうカード", "reading": "在留カード", "meaning": "Thẻ lưu trú cho người nước ngoài tại Nhật"}
    ]$$::jsonb,
    $$[
        {"pattern": "～てしまいました", "meaning": "Lỡ làm... (thể hiện sự tiếc nuối)", "example": "鍵をなくしてしまいました。", "exampleMeaning": "Tôi lỡ làm mất chìa khóa mất rồi."}
    ]$$::jsonb,
    $$[
        "Hơn 80% đồ thất lạc tại Nhật Bản được trả lại người mất qua hệ thống đồn cảnh sát Koban. Hãy nhớ báo càng chi tiết càng tốt."
    ]$$::jsonb,
    false,
    'https://images.unsplash.com/photo-1513407030348-c983a97b98d8?w=600&auto=format&fit=crop&q=80'
),

-- -----------------------------------------------------------------------------
-- 7. [N3] Mở tài khoản ngân hàng và làm thẻ ghi nợ (LIVING_IN_JAPAN)
-- -----------------------------------------------------------------------------
(
    'LIVING_IN_JAPAN',
    'N3',
    'Mở tài khoản ngân hàng và làm thẻ ghi nợ (銀行口座の開設)',
    'Bạn đến chi nhánh ngân hàng để mở tài khoản nhận lương và đăng ký dịch vụ ngân hàng trực tuyến (Internet Banking).',
    'Khách hàng mở tài khoản',
    'Giao dịch viên ngân hàng',
    $$[
        {"id": 0, "text": "Trình bày mục đích mở tài khoản ngân hàng (để nhận lương đi làm)"},
        {"id": 1, "text": "Xuất trình thẻ lưu trú và hộ chiếu để nhân viên đối chiếu thông tin"},
        {"id": 2, "text": "Đăng ký ứng dụng ngân hàng số và thiết lập mã PIN thẻ ATM"}
    ]$$::jsonb,
    $$[
        {"speaker": "USER", "text": "すみません、アルバイトの給料を受け取るために新しく口座を作りたいんですが。", "vi": "Xin chào, tôi muốn mở tài khoản mới để nhận lương làm thêm baito ạ."},
        {"speaker": "AI", "text": "口座開設ですね。ご本人様確認のため、在留カードとパスポートをお持ちですか？", "vi": "Mở tài khoản phải không ạ. Để xác minh danh tính, quý khách có mang theo thẻ lưu trú và hộ chiếu không ạ?"},
        {"speaker": "USER", "text": "はい、両方持ってきました。こちらです。", "vi": "Vâng, tôi mang cả hai đây ạ. Xin gửi bạn."},
        {"speaker": "AI", "text": "ありがとうございます。では、キャッシュカードの暗証番号４桁を決めていただけますか？", "vi": "Cảm ơn quý khách. Tiếp theo xin quý khách vui lòng chọn mã PIN 4 số cho thẻ ATM ạ?"},
        {"speaker": "USER", "text": "はい、入力しました。通帳はいつもらえますか？", "vi": "Vâng, tôi đã nhập rồi. Sổ ngân hàng khi nào tôi có thể nhận được ạ?"},
        {"speaker": "AI", "text": "通帳は本日お渡しできます。カードは約１週間後にご自宅へ郵送いたします。", "vi": "Sổ tài khoản chúng tôi giao ngay hôm nay ạ. Thẻ ATM sẽ gửi qua bưu điện về nhà sau khoảng 1 tuần."}
    ]$$::jsonb,
    $$[
        {"word": "こうざ", "reading": "口座", "meaning": "Tài khoản ngân hàng"},
        {"word": "あんしょうばんごう", "reading": "暗証番号", "meaning": "Mã PIN, mật khẩu thẻ"},
        {"word": "つうちょう", "reading": "通帳", "meaning": "Sổ ngân hàng"}
    ]$$::jsonb,
    $$[
        {"pattern": "～ために", "meaning": "Để nhằm mục đích...", "example": "勉強するために日本へ来ました。", "exampleMeaning": "Tôi đến Nhật để học tập."}
    ]$$::jsonb,
    $$[
        "Nhiều ngân hàng Nhật yêu cầu người nước ngoài ở Nhật đủ 6 tháng mới cấp tài khoản đầy đủ; bưu điện Yucho Bank thường dễ mở nhất cho người mới sang."
    ]$$::jsonb,
    false,
    'https://images.unsplash.com/photo-1503899036084-c55cdd92da26?w=600&auto=format&fit=crop&q=80'
),

-- -----------------------------------------------------------------------------
-- 8. [N3] Phỏng vấn xin việc làm thêm baito tại quán ăn (FRIENDSHIP_CHATTING)
-- -----------------------------------------------------------------------------
(
    'FRIENDSHIP_CHATTING',
    'N3',
    'Phỏng vấn xin việc làm thêm baito tại quán ăn (アルバイトの面接を受ける)',
    'Bạn nộp đơn xin làm thêm (baito) tại một nhà hàng Nhật Bản và đang có buổi phỏng vấn trực tiếp với bác chủ quán.',
    'Ứng viên xin việc',
    'Chủ quán phỏng vấn (店長)',
    $$[
        {"id": 0, "text": "Giới thiệu bản thân, trường đang theo học và tư cách lưu trú"},
        {"id": 1, "text": "Trình bày số buổi và các khung giờ bạn có thể đi làm mỗi tuần"},
        {"id": 2, "text": "Thể hiện sự nhiệt tình, sẵn sàng học hỏi phong cách phục vụ của quán"}
    ]$$::jsonb,
    $$[
        {"speaker": "AI", "text": "本日は面接にお越しいただきありがとうございます。まず簡単に自己紹介をお願いできますか？", "vi": "Cảm ơn cháu hôm nay đã đến phỏng vấn. Trước hết cháu giới thiệu sơ qua về bản thân được không?"},
        {"speaker": "USER", "text": "はい！ベトナムから参りましたナムと申します。現在日本語学校で勉強しており、日本語能力試験Ｎ３を持っています。", "vi": "Dạ vâng! Cháu là Nam đến từ Việt Nam. Hiện cháu đang học trường tiếng Nhật và có chứng chỉ JLPT N3 ạ."},
        {"speaker": "AI", "text": "日本語がとても上手ですね。週に何日くらいシフトに入れますか？", "vi": "Tiếng Nhật của cháu tốt quá. Mỗi tuần cháu có thể đi làm được khoảng mấy ngày?"},
        {"speaker": "USER", "text": "週に３日、平日夕方５時からと土曜日に入ることができます。", "vi": "Dạ tuần 3 ngày, các buổi chiều trong tuần từ 5 giờ và cả ngày thứ 7 cháu đều làm được ạ."},
        {"speaker": "AI", "text": "元気があって素晴らしいですね！ぜひ来週から一緒にお願いしたいです。", "vi": "Rất hoạt bát và nhiệt huyết đấy! Rất mong từ tuần sau cháu có thể cùng làm việc với quán nhé."}
    ]$$::jsonb,
    $$[
        {"word": "シフト", "reading": "シフト", "meaning": "Ca làm việc"},
        {"word": "めんせつ", "reading": "面接", "meaning": "Phỏng vấn"},
        {"word": "へいじつ", "reading": "平日", "meaning": "Ngày thường trong tuần (từ thứ 2 đến thứ 6)"}
    ]$$::jsonb,
    $$[
        {"pattern": "～と申します", "meaning": "Tôi tên là... (khiêm nhường ngữ)", "example": "田中と申します。", "exampleMeaning": "Tôi là Tanaka."}
    ]$$::jsonb,
    $$[
        "Khi phỏng vấn tại Nhật, hãy nhớ gõ cửa 3 tiếng, nói '失礼します' khi bước vào phòng và chỉ ngồi xuống sau khi được mời."
    ]$$::jsonb,
    false,
    'https://images.unsplash.com/photo-1517486808906-6ca8b3f04846?w=600&auto=format&fit=crop&q=80'
),

-- -----------------------------------------------------------------------------
-- 9. [N2] Báo cáo tiến độ công việc và xin ý kiến cấp trên (LIVING_IN_JAPAN)
-- -----------------------------------------------------------------------------
(
    'LIVING_IN_JAPAN',
    'N2',
    'Báo cáo tiến độ công việc và xin ý kiến cấp trên (上司への業務進捗報告と相談)',
    'Dự án bạn phụ trách gặp chút khó khăn do đối tác phản hồi chậm. Hãy báo cáo trung thực theo nguyên tắc Hou-Ren-So và xin chỉ đạo từ Trưởng phòng.',
    'Nhân viên dự án',
    'Trưởng phòng (Tanaka Bucho)',
    $$[
        {"id": 0, "text": "Xin phép cấp trên một ít thời gian để báo cáo tiến độ dự án"},
        {"id": 1, "text": "Trình bày rõ tình trạng bàn giao tài liệu của đối tác và đề xuất giải pháp"},
        {"id": 2, "text": "Lắng nghe chỉ đạo điều chỉnh từ trưởng phòng và ghi nhận kế hoạch"}
    ]$$::jsonb,
    $$[
        {"speaker": "USER", "text": "部長、ただいまお時間よろしいでしょうか。Ａ社向けシステム開発の件でご報告とご相談がございます。", "vi": "Thưa Trưởng phòng, hiện tại anh có rảnh chút không ạ? Em có chút việc muốn báo cáo và xin ý kiến về dự án phát triển hệ thống cho công ty A ạ."},
        {"speaker": "AI", "text": "ええ、大丈夫ですよ。進捗はどうなっていますか？", "vi": "Được chứ. Tiến độ dự án hiện đang thế nào rồi em?"},
        {"speaker": "USER", "text": "基本設計はおおむね順調ですが、先方からの仕様確認の回答が遅れており、このままだと納期に２日ほど影響が出る恐れがございます。", "vi": "Phần thiết kế cơ bản nhìn chung suôn sẻ, tuy nhiên phản hồi xác nhận đặc tả từ phía đối tác đang bị trễ, nếu cứ giữ đà này e là sẽ ảnh hưởng khoảng 2 ngày tới hạn bàn giao ạ."},
        {"speaker": "AI", "text": "なるほど。では私の方から先方の担当役員に直接連絡を入れてフォローしてみましょう。", "vi": "Tôi hiểu rồi. Vậy để tôi trực tiếp liên lạc với giám đốc phụ trách bên đó để đốc thúc xem sao."},
        {"speaker": "USER", "text": "ご対応ありがとうございます！お手数をおかけいたしますが、よろしくお願いいたします。", "vi": "Em cảm ơn Trưởng phòng nhiều ạ! Làm phiền anh hỗ trợ, em nhờ anh ạ."}
    ]$$::jsonb,
    $$[
        {"word": "しんちょく", "reading": "進捗", "meaning": "Tiến độ công việc"},
        {"word": "のうき", "reading": "納期", "meaning": "Thời hạn bàn giao sản phẩm"},
        {"word": "おそれがある", "reading": "恐れがある", "meaning": "E rằng, có nguy cơ/khả năng xảy ra rủi ro"}
    ]$$::jsonb,
    $$[
        {"pattern": "～の恐れがある", "meaning": "Có nguy cơ / e rằng sẽ dẫn đến...", "example": "台風の影響で遅れる恐れがあります。", "exampleMeaning": "Có nguy cơ trễ do ảnh hưởng của bão."}
    ]$$::jsonb,
    $$[
        "Nguyên tắc Hou-Ren-So (Báo cáo - Liên lạc - Thảo luận) là cốt lõi trong văn hóa doanh nghiệp Nhật Bản. Khi có nguy cơ trễ hẹn, phải báo ngay trước khi hạn chót đến."
    ]$$::jsonb,
    false,
    'https://images.unsplash.com/photo-1522202176988-66273c2fd55f?w=600&auto=format&fit=crop&q=80'
),

-- -----------------------------------------------------------------------------
-- 10. [N1] Thuyết trình ý tưởng dự án và thảo luận chiến lược (COMMUNITY)
-- -----------------------------------------------------------------------------
(
    'COMMUNITY',
    'N1',
    'Thuyết trình ý tưởng dự án và thảo luận chiến lược (新規事業計画のプレゼンと討議)',
    'Trong cuộc họp ban điều hành, bạn đại diện nhóm thuyết trình đề án số hóa dịch vụ sang thị trường Đông Nam Á và phản biện các quan ngại chiến lược.',
    'Trưởng nhóm phát triển chiến lược',
    'Tổng giám đốc điều hành (Shachou)',
    $$[
        {"id": 0, "text": "Tóm tắt cơ hội thị trường và các luận điểm mấu chốt của đề án"},
        {"id": 1, "text": "Phản biện chặt chẽ trước các câu hỏi về rủi ro biến động tỷ giá và đối thủ cạnh tranh"},
        {"id": 2, "text": "Đề xuất lộ trình chạy thử nghiệm phiên bản PoC trong vòng 3 tháng tới"}
    ]$$::jsonb,
    $$[
        {"speaker": "USER", "text": "それでは、東南アジアにおける越境ＥＣ展開の新規事業提案についてご説明申し上げます。現地若年層のスマートフォン普及率と日本製品への需要の高まりを鑑み、今期中の参入が極めて戦略的意義を持つと確信しております。", "vi": "Sau đây tôi xin phép trình bày đề xuất dự án kinh doanh thương mại điện tử xuyên biên giới tại Đông Nam Á. Xét đến độ phủ smartphone của giới trẻ bản địa và nhu cầu đối với hàng Nhật tăng cao, chúng tôi tin rằng việc thâm nhập ngay trong quý này mang ý nghĩa chiến lược vô cùng to lớn."},
        {"speaker": "AI", "text": "着眼点は非常に興味深いが、現地競合他社との差別化および初期投資の回収リスクについてはどう検証していますか？", "vi": "Góc tiếp cận rất thú vị đấy, nhưng các bạn đã thẩm định thế nào về sự khác biệt hóa so với đối thủ bản địa cũng như rủi ro thu hồi vốn đầu tư ban đầu?"},
        {"speaker": "USER", "text": "ご指摘の通りです。競合との差別化として、真贋判定付き物流網の構築を軸に据えております。リスクを最小限に抑えるため、まずは３ヶ月のＰｏＣ検証を実施し、費用対効果を見極めた上で本展開へ移行する所存でございます。", "vi": "Đúng như Giám đốc đã chỉ ra ạ. Về điểm khác biệt cốt lõi, chúng tôi tập trung xây dựng mạng lưới logistics tích hợp kiểm định hàng thật. Để tối thiểu hóa rủi ro, trước mắt nhóm dự kiến triển khai thử nghiệm PoC 3 tháng nhằm đánh giá tính hiệu quả chi phí trước khi chuyển sang giai đoạn nhân rộng."},
        {"speaker": "AI", "text": "説得力のある説明だ。その方針で役員会に諮り、パイロットテストの予算を承認することにしよう。", "vi": "Giải trình rất thuyết phục. Hãy theo hướng đó trình Hội đồng quản trị và phê duyệt ngân sách cho giai đoạn chạy thử."}
    ]$$::jsonb,
    $$[
        {"word": "かんがみる", "reading": "鑑みる", "meaning": "Xem xét cẩn trọng đến, dựa trên bối cảnh"},
        {"word": "さべつか", "reading": "差別化", "meaning": "Tạo ra sự khác biệt hóa cạnh tranh"},
        {"word": "ひようたいこうか", "reading": "費用対効果", "meaning": "Hiệu quả tương quan chi phí (Cost-Performance / ROI)"}
    ]$$::jsonb,
    $$[
        {"pattern": "～を鑑みて", "meaning": "Xét đến / căn cứ vào thực tế...", "example": "市場動向を鑑みて計画を見直す。", "exampleMeaning": "Xét đến xu hướng thị trường để xem xét lại kế hoạch."},
        {"pattern": "～所存でございます", "meaning": "Chúng tôi dự định / tha thiết muốn...", "example": "全力を尽くす所存でございます。", "exampleMeaning": "Chúng tôi nguyện sẽ nỗ lực hết sức mình."}
    ]$$::jsonb,
    $$[
        "Tại các buổi thuyết trình cấp cao (N1), việc sử dụng kính ngữ đỉnh cao (Keigo) cùng từ Hán tự chuyên ngành kinh tế giúp tăng độ tin cậy và sức thuyết phục."
    ]$$::jsonb,
    false,
    'https://images.unsplash.com/photo-1557804506-669a67965ba0?w=600&auto=format&fit=crop&q=80'
);

