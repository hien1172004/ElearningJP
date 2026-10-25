-- =============================================================================
-- Flyway Database Migration: V30__create_ai_speaking_practice_tables.sql
-- Module: AI Speaking Practice (Luyện nói AI mô phỏng Mazii)
-- Engine: PostgreSQL 15+
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1. AI SCENARIOS (Kịch bản tình huống nhập vai)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ai_scenario (
    id BIGSERIAL PRIMARY KEY,
    topic_id VARCHAR(50) NOT NULL,
    jlpt_level VARCHAR(10) NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    user_role VARCHAR(100) NOT NULL,
    ai_role VARCHAR(100) NOT NULL,
    tasks JSONB NOT NULL DEFAULT '[]'::jsonb,
    sample_dialogue JSONB NOT NULL DEFAULT '[]'::jsonb,
    vocabulary JSONB NOT NULL DEFAULT '[]'::jsonb,
    grammar JSONB NOT NULL DEFAULT '[]'::jsonb,
    tips JSONB NOT NULL DEFAULT '[]'::jsonb,
    is_premium BOOLEAN NOT NULL DEFAULT FALSE,
    thumbnail_url VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

-- Index tra cứu kịch bản theo chủ đề và trình độ JLPT
CREATE INDEX IF NOT EXISTS idx_ai_scenario_topic_level ON ai_scenario (topic_id, jlpt_level);
CREATE INDEX IF NOT EXISTS idx_ai_scenario_premium ON ai_scenario (is_premium);

-- -----------------------------------------------------------------------------
-- 2. AI PRACTICE SESSIONS (Phiên hội thoại luyện nói của người học)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ai_session (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    scenario_id BIGINT REFERENCES ai_scenario(id) ON DELETE SET NULL,
    mode VARCHAR(20) NOT NULL, -- 'SCENARIO' hoặc 'FREE'
    title VARCHAR(255) NOT NULL,
    completed_tasks JSONB NOT NULL DEFAULT '[]'::jsonb,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

-- Index lịch sử luyện tập theo user và thời gian
CREATE INDEX IF NOT EXISTS idx_ai_session_user_created ON ai_session (user_id, created_at DESC) WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_ai_session_scenario ON ai_session (scenario_id);

-- -----------------------------------------------------------------------------
-- 3. AI MESSAGES (Từng lượt thoại trong phiên luyện nói)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ai_message (
    id BIGSERIAL PRIMARY KEY,
    session_id BIGINT NOT NULL REFERENCES ai_session(id) ON DELETE CASCADE,
    role VARCHAR(10) NOT NULL, -- 'USER' hoặc 'AI'
    text_ja TEXT NOT NULL,
    text_vi TEXT,
    audio_url VARCHAR(500),
    correction JSONB, -- {"original": "...", "fixed": "...", "note_vi": "..."}
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

-- Index tin nhắn theo phiên để load ngữ cảnh trò chuyện nhanh
CREATE INDEX IF NOT EXISTS idx_ai_message_session_created ON ai_message (session_id, created_at ASC);

-- -----------------------------------------------------------------------------
-- 4. AI USAGE LIMITS (Giới hạn số token/lượt sử dụng theo ngày của người dùng)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ai_usage (
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    usage_date DATE NOT NULL,
    tokens_used INT NOT NULL DEFAULT 0,
    PRIMARY KEY (user_id, usage_date)
);

CREATE INDEX IF NOT EXISTS idx_ai_usage_date ON ai_usage (usage_date);

-- -----------------------------------------------------------------------------
-- 5. SEED INITIAL SAMPLE SCENARIOS (Dữ liệu mẫu khởi đầu chuẩn JLPT)
-- -----------------------------------------------------------------------------
INSERT INTO ai_scenario (
    topic_id, jlpt_level, title, description, user_role, ai_role,
    tasks, sample_dialogue, vocabulary, grammar, tips, is_premium, thumbnail_url
) VALUES
(
    'DINING_SHOPPING',
    'N5',
    'Gọi món tại quán Ramen (ラーメン屋で注文)',
    'Bạn vừa bước vào một quán mì Ramen ở Tokyo sau một ngày học tập. Hãy chào hỏi, chọn món mì yêu thích và thanh toán tiền khi ăn xong.',
    'Khách hàng',
    'Nhân viên quán Ramen (Maziiko)',
    $$[
        {"id": 0, "text": "Nói số lượng người (1 người) và hỏi chỗ ngồi"},
        {"id": 1, "text": "Gọi 1 bát Shoyu Ramen và đồ uống"},
        {"id": 2, "text": "Hỏi thanh toán tiền khi dùng bữa xong"}
    ]$$::jsonb,
    $$[
        {"speaker": "AI", "text": "いらっしゃいませ！何名様ですか？"},
        {"speaker": "USER", "text": "一人です。カウンター席でいいですか？"},
        {"speaker": "AI", "text": "どうぞこちらへ！ご注文はお決まりですか？"},
        {"speaker": "USER", "text": "醤油ラーメンを一つと、お水をください。"},
        {"speaker": "AI", "text": "かしこまりました。少々お待ちください。"},
        {"speaker": "USER", "text": "ごちそうさまでした。お会計をお願いします。"},
        {"speaker": "AI", "text": "ありがとうございます！800円になります。"}
    ]$$::jsonb,
    $$[
        {"word": "注文", "reading": "ちゅうもん", "meaning": "Gọi món, đặt hàng"},
        {"word": "醤油ラーメン", "reading": "しょうゆラーメン", "meaning": "Mì ramen xì dầu"},
        {"word": "お会計", "reading": "おかいけい", "meaning": "Thanh toán hóa đơn"},
        {"word": "一人", "reading": "ひとり", "meaning": "Một người"},
        {"word": "少々お待ちください", "reading": "しょうしょうおまちください", "meaning": "Xin vui lòng đợi một chút"}
    ]$$::jsonb,
    $$[
        {"pattern": "〜をください / 〜をお願いします", "meaning": "Cho tôi xin ~ / Làm ơn lấy giúp ~"},
        {"pattern": "〜名様 (めいさま)", "meaning": "Kính ngữ chỉ số lượng người"}
    ]$$::jsonb,
    $$[
        "Khi nhân viên chào và hỏi 何名様ですか, hãy tự tin trả lời 一人です (Hitori desu) nếu đi một mình.",
        "Dùng từ お会計をお願いします (Okaikei o onegaishimasu) khi muốn gọi tính tiền lịch sự."
    ]$$::jsonb,
    false,
    'https://images.unsplash.com/photo-1569718212165-3a8278d5f624?w=500&auto=format&fit=crop&q=60'
),
(
    'FRIENDSHIP_CHATTING',
    'N5',
    'Làm quen bạn mới ở lớp học tiếng Nhật (クラスメートと自己紹介)',
    'Hôm nay là buổi học đầu tiên tại trường tiếng Nhật. Hãy làm quen và tự giới thiệu bản thân với bạn cùng bàn người Nhật.',
    'Học viên mới (người Việt Nam)',
    'Bạn cùng lớp thân thiện (Maziiko)',
    $$[
        {"id": 0, "text": "Chào hỏi và giới thiệu tên, quốc tịch"},
        {"id": 1, "text": "Hỏi sở thích hoặc chuyên ngành của bạn"},
        {"id": 2, "text": "Nói lời chào kết thúc hẹn cùng học chung"}
    ]$$::jsonb,
    $$[
        {"speaker": "AI", "text": "こんにちは！隣、座ってもいいですか？"},
        {"speaker": "USER", "text": "こんにちは、どうぞ！初めまして、ナムです。ベトナムから来ました。"},
        {"speaker": "AI", "text": "ナムさん、よろしくね！私はマジーコです。日本のアニメが好きですか？"},
        {"speaker": "USER", "text": "はい、大好きです！特にワンピースが好きです。これからよろしくお願いします。"}
    ]$$::jsonb,
    $$[
        {"word": "初めまして", "reading": "はじめまして", "meaning": "Rất vui được gặp bạn lần đầu"},
        {"word": "自己紹介", "reading": "じこしょうかい", "meaning": "Tự giới thiệu bản thân"},
        {"word": "隣", "reading": "となり", "meaning": "Bên cạnh"},
        {"word": "これから", "reading": "これから", "meaning": "Từ nay về sau"}
    ]$$::jsonb,
    $$[
        {"pattern": "〜から来ました", "meaning": "Tôi đến từ ~"},
        {"pattern": "〜が好きです", "meaning": "Tôi thích ~"}
    ]$$::jsonb,
    $$[
        "Người Nhật rất thích nói về sở thích (anime, ẩm thực, du lịch) trong buổi đầu làm quen.",
        "Đừng quên câu 'よろしくお願いします' (Yoroshiku onegaishimasu) ở cuối cuộc trò chuyện."
    ]$$::jsonb,
    false,
    'https://images.unsplash.com/photo-1522202176988-66273c2fd55f?w=500&auto=format&fit=crop&q=60'
),
(
    'LIVING_IN_JAPAN',
    'N4',
    'Hỏi đường đến ga tàu điện ngầm (駅への道を聞く)',
    'Bạn đang đi dạo ở khu phố Shibuya thì bị lạc đường. Hãy hỏi một người dân địa phương cách đi đến ga gần nhất và thời gian đi bộ.',
    'Người đi đường cần giúp đỡ',
    'Người dân Tokyo nhiệt tình (Maziiko)',
    $$[
        {"id": 0, "text": "Lịch sự gọi và xin lỗi vì làm phiền (すみません)"},
        {"id": 1, "text": "Hỏi đường đi đến ga Shibuya gần nhất"},
        {"id": 2, "text": "Hỏi thời gian đi bộ mất bao lâu và cảm ơn"}
    ]$$::jsonb,
    $$[
        {"speaker": "USER", "text": "すみません、ちょっとお伺いしてもいいですか？"},
        {"speaker": "AI", "text": "はい、何でしょうか？道に迷いましたか？"},
        {"speaker": "USER", "text": "渋谷駅へ行きたいんですが、どちらでしょうか？"},
        {"speaker": "AI", "text": "あそこに見える交差点を右に曲がって、まっすぐ行くと駅ですよ。"},
        {"speaker": "USER", "text": "歩いてどのくらいかかりますか？"},
        {"speaker": "AI", "text": "5分くらいで着きますよ。"},
        {"speaker": "USER", "text": "わかりました、ありがとうございます！"}
    ]$$::jsonb,
    $$[
        {"word": "道に迷う", "reading": "みちにまよう", "meaning": "Lạc đường"},
        {"word": "交差点", "reading": "こうさてん", "meaning": "Ngã tư"},
        {"word": "曲がる", "reading": "まがる", "meaning": "Rẽ, quẹo"},
        {"word": "まっすぐ", "reading": "まっすぐ", "meaning": "Đi thẳng"}
    ]$$::jsonb,
    $$[
        {"pattern": "〜へ行きたいんですが", "meaning": "Tôi muốn đến ~ nhưng không biết đường..."},
        {"pattern": "歩いてどのくらいかかりますか", "meaning": "Đi bộ mất bao lâu?"}
    ]$$::jsonb,
    $$[
        "Dùng '〜んですが' để tạo câu mở đầu nhờ vả mềm mại, tự nhiên hơn thay vì hỏi cộc lốc.",
        "Lưu ý các từ chỉ hướng: 右に曲がる (rẽ phải), 左に曲がる (rẽ trái), まっすぐ行く (đi thẳng)."
    ]$$::jsonb,
    true,
    'https://images.unsplash.com/photo-1503899036084-c55cdd92da26?w=500&auto=format&fit=crop&q=60'
);
