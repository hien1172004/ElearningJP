-- =============================================================================
-- SEED DATA CHO JLPT LEVEL: N5
-- =============================================================================

CREATE UNIQUE INDEX IF NOT EXISTS uq_writing_characters_character ON writing_characters(character);
CREATE UNIQUE INDEX IF NOT EXISTS uq_character_strokes_char_stroke ON character_strokes(character_id, stroke_number);
CREATE UNIQUE INDEX IF NOT EXISTS uq_vocabulary_word_hiragana ON vocabulary(word, hiragana);
CREATE UNIQUE INDEX IF NOT EXISTS uq_vocabulary_senses_vocab_sense ON vocabulary_senses(vocab_id, sense_no);



ALTER TABLE writing_characters ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE writing_characters ALTER COLUMN updated_at SET DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE writing_characters ALTER COLUMN version SET DEFAULT 0;
ALTER TABLE writing_characters ALTER COLUMN deleted SET DEFAULT FALSE;
ALTER TABLE character_strokes ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE character_strokes ALTER COLUMN updated_at SET DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE character_strokes ALTER COLUMN version SET DEFAULT 0;
ALTER TABLE character_strokes ALTER COLUMN deleted SET DEFAULT FALSE;
ALTER TABLE character_strokes ALTER COLUMN sample_points_json SET DEFAULT '[]'::jsonb;

ALTER TABLE writing_characters ALTER COLUMN is_active SET DEFAULT true;
ALTER TABLE writing_characters ALTER COLUMN review_status SET DEFAULT 'PUBLISHED';

ALTER TABLE vocabulary ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE vocabulary ALTER COLUMN updated_at SET DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE vocabulary ALTER COLUMN version SET DEFAULT 0;
ALTER TABLE vocabulary ALTER COLUMN deleted SET DEFAULT FALSE;
ALTER TABLE vocabulary ALTER COLUMN is_active SET DEFAULT true;
ALTER TABLE vocabulary ALTER COLUMN ai_generated SET DEFAULT false;
ALTER TABLE vocabulary ALTER COLUMN is_tts SET DEFAULT false;
ALTER TABLE vocabulary ALTER COLUMN visibility SET DEFAULT 'PUBLIC';
ALTER TABLE vocabulary ALTER COLUMN review_status SET DEFAULT 'PUBLISHED';
ALTER TABLE vocabulary ALTER COLUMN source_type SET DEFAULT 'CORE';

ALTER TABLE vocabulary_senses ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE vocabulary_senses ALTER COLUMN updated_at SET DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE vocabulary_senses ALTER COLUMN version SET DEFAULT 0;
ALTER TABLE vocabulary_senses ALTER COLUMN deleted SET DEFAULT FALSE;

-- -----------------------------------------------------------------------------
-- WRITING CHARACTERS & KANJIVG STROKES (N5)
-- -----------------------------------------------------------------------------
INSERT INTO writing_characters (character, char_type, stroke_count, onyomi, kunyomi, han_viet, meaning_vi, meaning_en, romaji, jlpt_level, radicals, is_active, review_status)
VALUES ('一', 'KANJI', 1, ARRAY['いち', 'いつ'], ARRAY['ひと-', 'ひと.つ'], 'NHẤT', 'một, 1; bộ nhất', 'One, One Radical (no.1)', '', 'N5', ARRAY['Ground'], TRUE, 'PUBLISHED')
ON CONFLICT (character) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 1, 'M11,54.25c3.19,0.62,6.25,0.75,9.73,0.5c20.64-1.5,50.39-5.12,68.58-5.24c3.6-0.02,5.77,0.24,7.57,0.49', '[]'::jsonb
FROM writing_characters WHERE character = '一'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO writing_characters (character, char_type, stroke_count, onyomi, kunyomi, han_viet, meaning_vi, meaning_en, romaji, jlpt_level, radicals, is_active, review_status)
VALUES ('二', 'KANJI', 2, ARRAY['に', 'じ'], ARRAY['ふた', 'ふた.つ', 'ふたたび'], 'NHỊ', 'hai, 2', 'Two, Two Radical (no. 7)', '', 'N5', ARRAY['Two'], TRUE, 'PUBLISHED')
ON CONFLICT (character) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 1, 'M25.25,32.4c1.77,0.37,4.78,0.56,6.55,0.37c10.82-1.15,28.82-3.4,41.24-3.76c2.95-0.09,4.73,0.18,6.21,0.36', '[]'::jsonb
FROM writing_characters WHERE character = '二'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 2, 'M12,80.75c2.37,0.5,6.73,0.67,9.09,0.5c23.79-1.75,45.04-4.12,67.49-4.74c3.95-0.11,6.32,0.24,8.3,0.49', '[]'::jsonb
FROM writing_characters WHERE character = '二'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO writing_characters (character, char_type, stroke_count, onyomi, kunyomi, han_viet, meaning_vi, meaning_en, romaji, jlpt_level, radicals, is_active, review_status)
VALUES ('九', 'KANJI', 2, ARRAY['きゅう', 'く'], ARRAY['ここの', 'ここの.つ'], 'CƯU, CỬU', 'chín, 9', 'Nine', '', 'N5', ARRAY['Nine'], TRUE, 'PUBLISHED')
ON CONFLICT (character) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 1, 'M41.88,14.38c1,1.38,1.5,3.25,1.5,5.12c0,40.13-9.12,57.5-28.5,68.75', '[]'::jsonb
FROM writing_characters WHERE character = '九'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 2, 'M13.5,45.75c2.88,0.85,5.78,0.05,8.58-0.66c8.47-2.14,39.88-9.79,40.92-9.84c2.5-0.12,4.75,0.5,4.25,4.75c-0.5,4.25-5.5,20.75-7,32.5c-2.23,17.46,2,19.37,18.21,19.37c13.79,0,19.01-1.07,19.27-10.12', '[]'::jsonb
FROM writing_characters WHERE character = '九'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO writing_characters (character, char_type, stroke_count, onyomi, kunyomi, han_viet, meaning_vi, meaning_en, romaji, jlpt_level, radicals, is_active, review_status)
VALUES ('七', 'KANJI', 2, ARRAY['しち'], ARRAY['なな', 'なな.つ', 'なの'], 'THẤT', 'bảy, 7', 'Seven', '', 'N5', ARRAY['Seven'], TRUE, 'PUBLISHED')
ON CONFLICT (character) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 1, 'M15.5,51.75c1.82,0.5,4.38,0.88,6.96,0.5c16.91-2.45,50.92-8.12,64.44-8.74c3.02-0.14,4.84,0.24,6.35,0.49', '[]'::jsonb
FROM writing_characters WHERE character = '七'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 2, 'M43,20c1.38,1.38,2.15,3.25,2.15,5.26C45.15,29.5,45,71.84,45,76c0,10.5,2.25,12.25,20.25,12.25c18.75,0,20-3.75,20-2.75', '[]'::jsonb
FROM writing_characters WHERE character = '七'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO writing_characters (character, char_type, stroke_count, onyomi, kunyomi, han_viet, meaning_vi, meaning_en, romaji, jlpt_level, radicals, is_active, review_status)
VALUES ('人', 'KANJI', 2, ARRAY['じん', 'にん'], ARRAY['ひと', '-り', '-と'], 'NHÂN, NHƠN', 'người', 'Person', '', 'N5', ARRAY['Person'], TRUE, 'PUBLISHED')
ON CONFLICT (character) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 1, 'M54.5,20c0.37,2.12,0.23,4.03-0.22,6.27C51.68,39.48,38.25,72.25,16.5,87.25', '[]'::jsonb
FROM writing_characters WHERE character = '人'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 2, 'M46,54.25c6.12,6,25.51,22.24,35.52,29.72c3.66,2.73,6.94,4.64,11.48,5.53', '[]'::jsonb
FROM writing_characters WHERE character = '人'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO writing_characters (character, char_type, stroke_count, onyomi, kunyomi, han_viet, meaning_vi, meaning_en, romaji, jlpt_level, radicals, is_active, review_status)
VALUES ('入', 'KANJI', 2, ARRAY['にゅう', 'じゅ'], ARRAY['い.る', '-い.る', '-い.り', 'い.れる', '-い.れ', 'はい.る'], 'NHẬP', 'vào trong', 'Enter, Insert', '', 'N5', ARRAY['Enter'], TRUE, 'PUBLISHED')
ON CONFLICT (character) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 1, 'M54.75,48.75c-0.5,2-1.1,3.2-2.07,4.62C44.22,65.8,27.98,81.44,14.5,88', '[]'::jsonb
FROM writing_characters WHERE character = '入'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 2, 'M36.5,20c8.25,1.38,15.12,34,48.81,62.08c2.71,2.26,5.56,4.8,9.44,6.42', '[]'::jsonb
FROM writing_characters WHERE character = '入'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO writing_characters (character, char_type, stroke_count, onyomi, kunyomi, han_viet, meaning_vi, meaning_en, romaji, jlpt_level, radicals, is_active, review_status)
VALUES ('八', 'KANJI', 2, ARRAY['はち'], ARRAY['や', 'や.つ', 'やっ.つ', 'よう'], 'BÁT', 'tám, 8', 'Eight, Eight Radical (no. 12)', '', 'N5', ARRAY['Fins'], TRUE, 'PUBLISHED')
ON CONFLICT (character) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 1, 'M37.22,45c0.28,1.5,0.2,3.21-0.86,5.48c-4.23,9.02-11.48,20.4-24.1,32.02', '[]'::jsonb
FROM writing_characters WHERE character = '八'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 2, 'M48,27.25c9.38,0.25,21.12,30,37.27,45.72c3.79,3.69,6.73,5.66,9.98,7.03', '[]'::jsonb
FROM writing_characters WHERE character = '八'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO writing_characters (character, char_type, stroke_count, onyomi, kunyomi, han_viet, meaning_vi, meaning_en, romaji, jlpt_level, radicals, is_active, review_status)
VALUES ('十', 'KANJI', 2, ARRAY['じゅう', 'じっ', 'じゅっ'], ARRAY['とお', 'と'], 'THẬP', 'mười, 10; đủ hết', 'Ten', '', 'N5', ARRAY['Cross'], TRUE, 'PUBLISHED')
ON CONFLICT (character) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 1, 'M11.88,50.98c3.18,0.89,6.62,0.61,9.87,0.35c19.92-1.58,45.23-4.76,63.38-5.82c3.85-0.23,7.23-0.07,11,0.56', '[]'::jsonb
FROM writing_characters WHERE character = '十'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 2, 'M52.22,11.63c1.4,1.4,2.2,3.96,2.2,6.26c0,1.13-0.03,51.22-0.19,73.41c-0.03,3.96-0.06,6.83-0.08,8.08', '[]'::jsonb
FROM writing_characters WHERE character = '十'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO writing_characters (character, char_type, stroke_count, onyomi, kunyomi, han_viet, meaning_vi, meaning_en, romaji, jlpt_level, radicals, is_active, review_status)
VALUES ('三', 'KANJI', 3, ARRAY['さん', 'ぞう'], ARRAY['み', 'み.つ', 'みっ.つ'], 'TAM, TÁM, TẠM', 'ba, 3', 'Three', '', 'N5', ARRAY['Ground', 'Two'], TRUE, 'PUBLISHED')
ON CONFLICT (character) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 1, 'M27.5,23.65c3.09,0.73,6.29,0.36,9.4,0.06c10.2-1,27-2.94,38.97-3.57c3.06-0.16,6.09-0.2,9.14,0.23', '[]'::jsonb
FROM writing_characters WHERE character = '三'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 2, 'M28.75,55.14c3.13,0.76,6.46,0.43,9.64,0.2c10.03-0.72,23.97-2.63,34.73-3.12c2.7-0.12,5.45-0.16,8.13,0.3', '[]'::jsonb
FROM writing_characters WHERE character = '三'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 3, 'M13,87.83c3.94,1.01,7.72,0.96,11.75,0.72c18.41-1.07,41.27-3.39,61.12-4.07c3.63-0.13,7.2-0.1,10.75,0.78', '[]'::jsonb
FROM writing_characters WHERE character = '三'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO writing_characters (character, char_type, stroke_count, onyomi, kunyomi, han_viet, meaning_vi, meaning_en, romaji, jlpt_level, radicals, is_active, review_status)
VALUES ('上', 'KANJI', 3, ARRAY['じょう', 'しょう', 'しゃん'], ARRAY['うえ', '-うえ', 'うわ-', 'かみ', 'あ.げる', '-あ.げる', 'あ.がる', '-あ.がる', 'あ.がり', '-あ.がり', 'のぼ.る', 'のぼ.り', 'のぼ.せる', 'のぼ.す', 'たてまつ.る'], 'THƯỚNG, THƯỢNG', 'đi lên; ở phía trên', 'Above, Up', '', 'N5', ARRAY['Toe', 'Ground'], TRUE, 'PUBLISHED')
ON CONFLICT (character) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 1, 'M52.31,15.88c1.15,1.15,2.01,3.12,2.01,5.12c0,0.82-0.22,63.62-0.25,64.63', '[]'::jsonb
FROM writing_characters WHERE character = '上'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 2, 'M58,44.75c7-0.62,14.25-2.5,17.75-3c1.38-0.2,3.5-0.38,4.75,0', '[]'::jsonb
FROM writing_characters WHERE character = '上'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 3, 'M13.38,88.28c3.6,1.15,7.45,0.62,11.13,0.34c16.23-1.23,41.16-2.66,60.24-2.92c3.65-0.05,7.47-0.32,11,0.82', '[]'::jsonb
FROM writing_characters WHERE character = '上'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO writing_characters (character, char_type, stroke_count, onyomi, kunyomi, han_viet, meaning_vi, meaning_en, romaji, jlpt_level, radicals, is_active, review_status)
VALUES ('下', 'KANJI', 3, ARRAY['か', 'げ'], ARRAY['した', 'しも', 'もと', 'さ.げる', 'さ.がる', 'くだ.る', 'くだ.り', 'くだ.す', '-くだ.す', 'くだ.さる', 'お.ろす', 'お.りる'], 'HÁ, HẠ', 'đi xuống; ở bên dưới', 'Below, Down, Descend, Give, Low, Inferior', '', 'N5', ARRAY['Ground', 'Toe'], TRUE, 'PUBLISHED')
ON CONFLICT (character) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 1, 'M13.25,22.5c0.94,0.23,5.18,0.96,7.74,0.75c17.87-1.5,46.54-4.75,66.38-4.75c2.92,0,6.42,0.75,7.88,1.25', '[]'::jsonb
FROM writing_characters WHERE character = '下'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 2, 'M52.97,23.25c0.93,1.07,1.56,2.75,1.56,5.3c0,8.65-0.2,39.42-0.27,57.2c-0.02,3.86-0.02,5.89-0.02,8.25', '[]'::jsonb
FROM writing_characters WHERE character = '下'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 3, 'M67.83,37.17C72.75,39.5,79.88,47.62,82,52.12', '[]'::jsonb
FROM writing_characters WHERE character = '下'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO writing_characters (character, char_type, stroke_count, onyomi, kunyomi, han_viet, meaning_vi, meaning_en, romaji, jlpt_level, radicals, is_active, review_status)
VALUES ('大', 'KANJI', 3, ARRAY['だい', 'たい'], ARRAY['おお-', 'おお.きい', '-おお.いに'], 'THÁI, ĐẠI', 'to, lớn', 'Large, Big', '', 'N5', ARRAY['Big'], TRUE, 'PUBLISHED')
ON CONFLICT (character) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 1, 'M19.38,48.25c1.49,0.51,5.03,0.89,7.6,0.49C41.12,46.5,63,43,77.19,42.44c2.7-0.11,4.87-0.06,7.31,0.33', '[]'::jsonb
FROM writing_characters WHERE character = '大'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 2, 'M49.5,18c0.88,2.12,1.03,4.16,0.99,6.32C50,57,37.75,81.12,18,91.75', '[]'::jsonb
FROM writing_characters WHERE character = '大'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 3, 'M49.5,46c9,10.5,28.5,36.25,37.49,43.28c3.06,2.39,5.62,3.75,7.01,3.97', '[]'::jsonb
FROM writing_characters WHERE character = '大'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO writing_characters (character, char_type, stroke_count, onyomi, kunyomi, han_viet, meaning_vi, meaning_en, romaji, jlpt_level, radicals, is_active, review_status)
VALUES ('女', 'KANJI', 3, ARRAY['じょ', 'にょ', 'にょう'], ARRAY['おんな', 'め'], 'NHỮ, NỨ, NỮ, NỰ', 'đàn bà, con gái', 'Woman, Female', '', 'N5', ARRAY['Woman'], TRUE, 'PUBLISHED')
ON CONFLICT (character) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 1, 'M53.21,18.37c0.54,2.13,0.26,3.41-0.25,5.25C50.38,33,42.62,52.75,35.75,64c-1.39,2.27-1,3.5,1,3.5c11.63,0,28.46,7.48,38.83,16.41c2.56,2.21,4.68,4.51,6.17,6.84', '[]'::jsonb
FROM writing_characters WHERE character = '女'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 2, 'M69.62,42.18c0.5,1.7,0.63,3.57-0.01,5.93C65.93,61.8,54.61,81.6,27,91.75', '[]'::jsonb
FROM writing_characters WHERE character = '女'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 3, 'M13.88,50.43c3.48,1.39,7.26,0.85,10.88,0.53c19.52-1.7,42.04-4.08,60.61-4.63c3.66-0.11,7.21-0.1,10.62,1.42', '[]'::jsonb
FROM writing_characters WHERE character = '女'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO writing_characters (character, char_type, stroke_count, onyomi, kunyomi, han_viet, meaning_vi, meaning_en, romaji, jlpt_level, radicals, is_active, review_status)
VALUES ('山', 'KANJI', 3, ARRAY['さん', 'せん'], ARRAY['やま'], 'SAN, SƠN', 'núi; mồ mả', 'Mountain', '', 'N5', ARRAY['Mountain'], TRUE, 'PUBLISHED')
ON CONFLICT (character) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 1, 'M52.49,15.5c1.38,1.38,2.26,3.5,2.26,5.75c0,0.75-0.22,58.3-0.25,59.25', '[]'::jsonb
FROM writing_characters WHERE character = '山'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 2, 'M21.49,54.5c0.88,0.88,1.39,2.25,1.26,3.75c-0.58,6.99-1,16-2.5,23c-0.7,3.26,0.11,4,2,3.75c17-2.25,47.12-5.12,65.5-6', '[]'::jsonb
FROM writing_characters WHERE character = '山'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 3, 'M89.24,49c0.94,0.94,1.64,2.38,1.51,4.25c-0.25,3.68-1.83,20.3-2.55,28.77c-0.22,2.64-0.39,4.51-0.45,4.98', '[]'::jsonb
FROM writing_characters WHERE character = '山'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO writing_characters (character, char_type, stroke_count, onyomi, kunyomi, han_viet, meaning_vi, meaning_en, romaji, jlpt_level, radicals, is_active, review_status)
VALUES ('川', 'KANJI', 3, ARRAY['せん'], ARRAY['かわ'], 'XUYÊN', 'dòng nước, sông; cánh đồng', 'Stream, River, River Or Three-stroke River Radical (no. 47)', '', 'N5', ARRAY['River'], TRUE, 'PUBLISHED')
ON CONFLICT (character) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 1, 'M27.22,25.68c0.91,1.57,1.18,3.45,1.19,5.37C28.5,43.5,28.5,69,17.39,84.15', '[]'::jsonb
FROM writing_characters WHERE character = '川'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 2, 'M53.75,23.63c0.94,0.94,1.41,2.37,1.41,3.9c0,0.58-0.01,28.48-0.08,41.71c-0.02,3.31-0.04,5.74-0.06,6.63', '[]'::jsonb
FROM writing_characters WHERE character = '川'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 3, 'M85.56,15.63c1.09,1.09,1.76,2.62,1.76,4.25c0,0.74,0.23,46.86,0.09,66.12c-0.03,4.31-0.06,7.61-0.09,8.63', '[]'::jsonb
FROM writing_characters WHERE character = '川'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO writing_characters (character, char_type, stroke_count, onyomi, kunyomi, han_viet, meaning_vi, meaning_en, romaji, jlpt_level, radicals, is_active, review_status)
VALUES ('土', 'KANJI', 3, ARRAY['ど', 'と'], ARRAY['つち'], 'THỔ, ĐỖ, ĐỘ', 'đất; sao Thổ', 'Soil, Earth, Ground, Turkey', '', 'N5', ARRAY['Dirt'], TRUE, 'PUBLISHED')
ON CONFLICT (character) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 1, 'M26.63,50.89c1.63,0.4,4.64,0.6,6.26,0.4C43.5,50,62.12,48,75.66,46.92c2.71-0.22,4.36,0.19,5.72,0.39', '[]'::jsonb
FROM writing_characters WHERE character = '土'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 2, 'M52.17,17.37c1.17,1.17,2.02,3.13,2.02,4.64c0,10.25,0.14,61.06,0.14,63.36', '[]'::jsonb
FROM writing_characters WHERE character = '土'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 3, 'M15.38,87.73c2.12,0.54,6.01,0.73,8.12,0.54C46,86.25,69,84.62,90.34,83.79c3.53-0.14,5.65,0.26,7.41,0.53', '[]'::jsonb
FROM writing_characters WHERE character = '土'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO writing_characters (character, char_type, stroke_count, onyomi, kunyomi, han_viet, meaning_vi, meaning_en, romaji, jlpt_level, radicals, is_active, review_status)
VALUES ('千', 'KANJI', 3, ARRAY['せん'], ARRAY['ち'], 'THIÊN', 'nghìn, 1000; (xem: thu thiên 鞦韆,秋千)', 'Thousand', '', 'N5', ARRAY['Thousand'], TRUE, 'PUBLISHED')
ON CONFLICT (character) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 1, 'M70.38,10.17c-0.13,1.58-0.83,2.64-2.17,3.67c-5.71,4.41-21.46,11.91-41.57,16.82', '[]'::jsonb
FROM writing_characters WHERE character = '千'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 2, 'M12.13,50.83c3.36,0.94,7.21,0.75,10.63,0.49c17.76-1.34,37.63-4.16,66.24-4.94c3.08-0.08,6.08-0.14,9.13,0.38', '[]'::jsonb
FROM writing_characters WHERE character = '千'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 3, 'M54.56,25.25c1.03,1.03,2.01,3,2.01,5.18c0,0.9-0.07,46.38-0.19,63.58c-0.02,2.93-0.04,5.04-0.06,5.99', '[]'::jsonb
FROM writing_characters WHERE character = '千'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO writing_characters (character, char_type, stroke_count, onyomi, kunyomi, han_viet, meaning_vi, meaning_en, romaji, jlpt_level, radicals, is_active, review_status)
VALUES ('子', 'KANJI', 3, ARRAY['し', 'す', 'つ'], ARRAY['こ', '-こ', 'ね'], 'TÍ, TÝ, TỬ', 'Tý (ngôi thứ nhất hàng Chi); (như: tử 子); con; cái', 'Child, Sign Of The Rat, 11pm-1am, First Sign Of Chinese Zodiac', '', 'N5', ARRAY['Child'], TRUE, 'PUBLISHED')
ON CONFLICT (character) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 1, 'M33.28,19.04c1.84,0.71,3.7,0.86,5.4,0.63c4.95-0.67,27.95-4.58,29.86-4.92c3.46-0.62,4.06,1.36,2.11,3.58c-1.95,2.22-11.41,13.17-16.35,17.19', '[]'::jsonb
FROM writing_characters WHERE character = '子'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 2, 'M52.48,37.74c6.42,2.97,11.75,30.73,5.24,52.57c-2.8,9.38-8.09,2.96-10.47,0.99', '[]'::jsonb
FROM writing_characters WHERE character = '子'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 3, 'M12.25,51.48c3.75,1.14,8.79,1.03,12.48,0.49c16.77-2.47,42.86-5.84,58.53-6.75c4.26-0.25,9.11-0.34,13.11,0.57', '[]'::jsonb
FROM writing_characters WHERE character = '子'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO writing_characters (character, char_type, stroke_count, onyomi, kunyomi, han_viet, meaning_vi, meaning_en, romaji, jlpt_level, radicals, is_active, review_status)
VALUES ('小', 'KANJI', 3, ARRAY['しょう'], ARRAY['ちい.さい', 'こ-', 'お-', 'さ-'], 'TIỂU', 'nhỏ bé', 'Little, Small', '', 'N5', ARRAY['Small'], TRUE, 'PUBLISHED')
ON CONFLICT (character) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 1, 'M54.71,18.37c1.4,1.4,2.26,3.13,2.26,5.77c0,14.56-0.26,54.91-0.26,59.87c0,11.25-7.21,1.5-8.71,0.25', '[]'::jsonb
FROM writing_characters WHERE character = '小'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 2, 'M31.95,47.68c0.17,0.82,0.1,1.72-0.34,2.9C29.5,56.38,24.38,66.25,16.75,73', '[]'::jsonb
FROM writing_characters WHERE character = '小'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 3, 'M80.96,47.12C86.62,52,95.25,64.62,97,72', '[]'::jsonb
FROM writing_characters WHERE character = '小'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO writing_characters (character, char_type, stroke_count, onyomi, kunyomi, han_viet, meaning_vi, meaning_en, romaji, jlpt_level, radicals, is_active, review_status)
VALUES ('中', 'KANJI', 4, ARRAY['ちゅう'], ARRAY['なか', 'うち', 'あた.る'], 'TRUNG, TRÚNG', 'ở giữa; ở bên trong; đúng, trúng, tin; mắc phải, bị', 'In, Inside, Middle, Mean, Center', '', 'N5', ARRAY['Middle'], TRUE, 'PUBLISHED')
ON CONFLICT (character) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 1, 'M19.89,36.87c1,1,1.74,2.25,2.01,3.65c1.13,5.71,2.58,13.06,4.17,22.97c0.27,1.68,0.55,4.43,0.83,6.26', '[]'::jsonb
FROM writing_characters WHERE character = '中'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 2, 'M23.33,39.51C37.12,37.62,70.88,34,84,33.24c4.38-0.25,6,1.14,5.12,4.42c-1.53,5.7-5.61,20.18-6.12,22.09', '[]'::jsonb
FROM writing_characters WHERE character = '中'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 3, 'M27.74,64.84C40.12,63.62,61.86,62.2,79,60.77c2.36-0.2,5.75-0.27,7.25-0.27', '[]'::jsonb
FROM writing_characters WHERE character = '中'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 4, 'M52.5,11.5c1.44,1.44,2.25,3.5,2.25,5.06c0,0.9,0.06,56.6-0.15,76.69c-0.03,3.3-0.07,5.6-0.1,6.5', '[]'::jsonb
FROM writing_characters WHERE character = '中'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO writing_characters (character, char_type, stroke_count, onyomi, kunyomi, han_viet, meaning_vi, meaning_en, romaji, jlpt_level, radicals, is_active, review_status)
VALUES ('五', 'KANJI', 4, ARRAY['ご'], ARRAY['いつ', 'いつ.つ'], 'NGŨ', 'năm, 5', 'Five', '', 'N5', ARRAY['Five'], TRUE, 'PUBLISHED')
ON CONFLICT (character) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 1, 'M31.75,23.15c2.8,0.67,5.54,0.42,8.36,0.12c9.3-0.99,22.18-2.4,34.14-3.21c2.49-0.17,5.04-0.33,7.5,0.2', '[]'::jsonb
FROM writing_characters WHERE character = '五'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 2, 'M55.75,25.25c0.62,1.25,1.02,3.01,0.5,5c-3.12,11.88-14,44.12-19.75,59', '[]'::jsonb
FROM writing_characters WHERE character = '五'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 3, 'M25.5,55.25c2.07,1.24,4.73,1.03,7,0.81c15.49-1.45,29.89-3.03,42.25-4.06c3-0.25,4.25,1.75,3.5,3.75c-2.24,5.96-6,20.75-7.75,31.5', '[]'::jsonb
FROM writing_characters WHERE character = '五'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 4, 'M11.25,90.5c3.04,0.81,6.52,0.63,9.63,0.41c15.71-1.1,43.9-2.8,67.75-3.8c3.41-0.14,6.9-0.4,10.25,0.39', '[]'::jsonb
FROM writing_characters WHERE character = '五'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO writing_characters (character, char_type, stroke_count, onyomi, kunyomi, han_viet, meaning_vi, meaning_en, romaji, jlpt_level, radicals, is_active, review_status)
VALUES ('六', 'KANJI', 4, ARRAY['ろく', 'りく'], ARRAY['む', 'む.つ', 'むっ.つ', 'むい'], 'LỤC', 'sáu, 6', 'Six', '', 'N5', ARRAY['Lid', 'Fins'], TRUE, 'PUBLISHED')
ON CONFLICT (character) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 1, 'M51.87,17.5c1.78,1.78,2.71,3.48,2.71,6.5c0,6.46,0.12,9.16,0.12,14.35', '[]'::jsonb
FROM writing_characters WHERE character = '六'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 2, 'M13.5,42.13c3.27,0.74,7.11,0.89,9.93,0.64c21.56-1.9,41.78-5.02,61.41-5.47c4.8-0.11,7.49,0.31,11.06,1.07', '[]'::jsonb
FROM writing_characters WHERE character = '六'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 3, 'M38.11,58.6c0.51,1.37,0.42,3.67-0.49,5.29C33.38,71.38,24,82.38,15.41,88.75', '[]'::jsonb
FROM writing_characters WHERE character = '六'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 4, 'M70.16,59.92c9.96,8.61,18.18,18.54,23.16,28.99', '[]'::jsonb
FROM writing_characters WHERE character = '六'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO writing_characters (character, char_type, stroke_count, onyomi, kunyomi, han_viet, meaning_vi, meaning_en, romaji, jlpt_level, radicals, is_active, review_status)
VALUES ('円', 'KANJI', 4, ARRAY['えん'], ARRAY['まる.い', 'まる', 'まど', 'まど.か', 'まろ.やか'], 'VIÊN', 'tròn, hình tròn; cầu, hình cầu; tròn (trăng)', 'Circle, Yen, Round', '', 'N5', ARRAY['Lid', 'Head'], TRUE, 'PUBLISHED')
ON CONFLICT (character) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 1, 'M21.75,19.8c0.91,0.91,1.47,3.23,1.5,5.45c0.2,13.9,0.03,47.69,0.03,62.5c0,2-0.03,4.99-0.03,6', '[]'::jsonb
FROM writing_characters WHERE character = '円'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 2, 'M24.06,21.56c15.07-1.68,49.46-5.58,57.92-6.31c2.9-0.25,4.78,1.88,4.78,4.27c0,13.48,0,53.21,0,67.48c0,9.75-4.25,6.5-8.5,1.5', '[]'::jsonb
FROM writing_characters WHERE character = '円'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 3, 'M52.25,20.75c0.88,0.88,1.5,2,1.5,3.71c0,6.76,0,27.54,0,31.04', '[]'::jsonb
FROM writing_characters WHERE character = '円'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 4, 'M24.75,59.75c14.62-1.75,43-4.25,60.5-5.25', '[]'::jsonb
FROM writing_characters WHERE character = '円'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO writing_characters (character, char_type, stroke_count, onyomi, kunyomi, han_viet, meaning_vi, meaning_en, romaji, jlpt_level, radicals, is_active, review_status)
VALUES ('天', 'KANJI', 4, ARRAY['てん'], ARRAY['あまつ', 'あめ', 'あま-'], 'THIÊN', 'trời, bầu trời; tự nhiên; ngày; hình phạt săm chữ vào trán', 'Heavens, Sky, Imperial', '', 'N5', ARRAY['Heaven'], TRUE, 'PUBLISHED')
ON CONFLICT (character) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 1, 'M21.63,24.83c1.81,0.46,5.14,0.4,6.94,0.21c14.55-1.53,35.18-4.16,50.1-5.25c3.01-0.22,4.83,0.22,6.34,0.45', '[]'::jsonb
FROM writing_characters WHERE character = '天'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 2, 'M25.31,51.64c2.09,0.31,3.47,0.4,5.94,0.11c10.62-1.25,35.88-4.38,45.96-4.93c1.74-0.1,3.62,0.03,5.99,0.45', '[]'::jsonb
FROM writing_characters WHERE character = '天'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 3, 'M50.32,26c0.68,1,1.3,3.43,1.29,5.37c-0.24,30.51-14.86,50.88-33.86,60.25', '[]'::jsonb
FROM writing_characters WHERE character = '天'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 4, 'M50.1,51.39C58.71,60,75.07,78.48,86.59,87.05c2.33,1.73,4.41,3.08,7.91,4.2', '[]'::jsonb
FROM writing_characters WHERE character = '天'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO writing_characters (character, char_type, stroke_count, onyomi, kunyomi, han_viet, meaning_vi, meaning_en, romaji, jlpt_level, radicals, is_active, review_status)
VALUES ('日', 'KANJI', 4, ARRAY['にち', 'じつ'], ARRAY['ひ', '-び', '-か'], 'NHẬT, NHỰT', 'Mặt Trời; ngày', 'Day, Sun, Japan, Counter For Days', '', 'N5', ARRAY['Sun'], TRUE, 'PUBLISHED')
ON CONFLICT (character) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 1, 'M31.5,24.5c1.12,1.12,1.74,2.75,1.74,4.75c0,1.6-0.16,38.11-0.09,53.5c0.02,3.82,0.05,6.35,0.09,6.75', '[]'::jsonb
FROM writing_characters WHERE character = '日'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 2, 'M33.48,26c0.8-0.05,37.67-3.01,40.77-3.25c3.19-0.25,5,1.75,5,4.25c0,4-0.22,40.84-0.23,56c0,3.48,0,5.72,0,6', '[]'::jsonb
FROM writing_characters WHERE character = '日'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 3, 'M34.22,55.25c7.78-0.5,35.9-2.5,44.06-2.75', '[]'::jsonb
FROM writing_characters WHERE character = '日'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 4, 'M34.23,86.5c10.52-0.75,34.15-2.12,43.81-2.25', '[]'::jsonb
FROM writing_characters WHERE character = '日'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO writing_characters (character, char_type, stroke_count, onyomi, kunyomi, han_viet, meaning_vi, meaning_en, romaji, jlpt_level, radicals, is_active, review_status)
VALUES ('月', 'KANJI', 4, ARRAY['げつ', 'がつ'], ARRAY['つき'], 'NGUYỆT', 'Mặt Trăng; tháng', 'Month, Moon', '', 'N5', ARRAY['Moon'], TRUE, 'PUBLISHED')
ON CONFLICT (character) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 1, 'M34.25,16.25c1,1,1.48,2.38,1.5,4c0.38,33.62,2.38,59.38-11,73.25', '[]'::jsonb
FROM writing_characters WHERE character = '月'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 2, 'M36.25,19c4.12-0.62,31.49-4.78,33.25-5c4-0.5,5.5,1.12,5.5,4.75c0,2.76-0.5,49.25-0.5,69.5c0,13-6.25,4-8.75,1.75', '[]'::jsonb
FROM writing_characters WHERE character = '月'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 3, 'M37.25,38c10.25-1.5,27.25-3.75,36.25-4.5', '[]'::jsonb
FROM writing_characters WHERE character = '月'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 4, 'M37,58.25c8.75-1.12,27-3.5,36.25-4', '[]'::jsonb
FROM writing_characters WHERE character = '月'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO writing_characters (character, char_type, stroke_count, onyomi, kunyomi, han_viet, meaning_vi, meaning_en, romaji, jlpt_level, radicals, is_active, review_status)
VALUES ('木', 'KANJI', 4, ARRAY['ぼく', 'もく'], ARRAY['き', 'こ-'], 'MỘC', 'cây, gỗ; mộc mạc, chất phác; sao Mộc', 'Tree, Wood', '', 'N5', ARRAY['Tree'], TRUE, 'PUBLISHED')
ON CONFLICT (character) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 1, 'M19.5,39.86c2.45,0.57,5.23,0.8,8.04,0.57C40.75,39.38,63,36.5,79.78,36.15c2.8-0.06,4.54,0.1,7.34,0.5', '[]'::jsonb
FROM writing_characters WHERE character = '木'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 2, 'M51.75,10.5c1.19,1.19,2,3,2,5c0,8.65,0,55.15-0.14,74.75c-0.03,4.19-0.07,7.15-0.11,8.25', '[]'::jsonb
FROM writing_characters WHERE character = '木'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 3, 'M50.75,39.5c0,1.12-0.61,2.44-1.42,3.95C41.75,57.5,26.7,73.93,15.75,80.25', '[]'::jsonb
FROM writing_characters WHERE character = '木'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 4, 'M54.5,39c4.62,6,23,25.75,31.76,34.61c2.27,2.29,4.61,4.39,7.49,5.64', '[]'::jsonb
FROM writing_characters WHERE character = '木'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO writing_characters (character, char_type, stroke_count, onyomi, kunyomi, han_viet, meaning_vi, meaning_en, romaji, jlpt_level, radicals, is_active, review_status)
VALUES ('水', 'KANJI', 4, ARRAY['すい'], ARRAY['みず', 'みず-'], 'THUỶ', 'nước; sao Thuỷ', 'Water', '', 'N5', ARRAY['Water'], TRUE, 'PUBLISHED')
ON CONFLICT (character) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 1, 'M52.77,15.08c1.08,1.08,1.67,2.49,1.76,5.52c0.4,14.55-0.26,62.16-0.26,67.12c0,9.78-7.52,0.03-9.02-1.22', '[]'::jsonb
FROM writing_characters WHERE character = '水'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 2, 'M17.5,45.75c1.75,0.62,3.73,0.43,5.25,0C25.88,44.88,36.09,41,38.59,40s4.47,1.24,3.75,3.5C39,54,28.25,69,19,74.75', '[]'::jsonb
FROM writing_characters WHERE character = '水'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 3, 'M81.22,27.5c-0.22,1.25-0.72,2.25-1.52,2.97c-5.64,5.1-12.45,9.78-22.45,13.78', '[]'::jsonb
FROM writing_characters WHERE character = '水'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 4, 'M57,46c8.82,10.73,19.23,21.46,28.42,27.42c2.16,1.4,4.52,3,7.08,3.58', '[]'::jsonb
FROM writing_characters WHERE character = '水'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO writing_characters (character, char_type, stroke_count, onyomi, kunyomi, han_viet, meaning_vi, meaning_en, romaji, jlpt_level, radicals, is_active, review_status)
VALUES ('火', 'KANJI', 4, ARRAY['か'], ARRAY['ひ', '-び', 'ほ-'], 'HOẢ', 'lửa', 'Fire', '', 'N5', ARRAY['Fire'], TRUE, 'PUBLISHED')
ON CONFLICT (character) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 1, 'M24.25,34c3.27,3.33,8.5,13,9.5,17.75', '[]'::jsonb
FROM writing_characters WHERE character = '火'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 2, 'M83,27.25c0.5,1.38,0.22,2.74-0.5,4.25c-2.38,5-7.5,12.12-12.75,17.25', '[]'::jsonb
FROM writing_characters WHERE character = '火'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 3, 'M52.5,14.25c1,1.25,1.5,3.12,1.5,5C54,69,39.62,80,21,91.5', '[]'::jsonb
FROM writing_characters WHERE character = '火'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 4, 'M52.75,50c12.49,14.06,25.01,28.42,33.62,36.13c2.7,2.42,4.9,4.02,8.38,4.87', '[]'::jsonb
FROM writing_characters WHERE character = '火'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO writing_characters (character, char_type, stroke_count, onyomi, kunyomi, han_viet, meaning_vi, meaning_en, romaji, jlpt_level, radicals, is_active, review_status)
VALUES ('出', 'KANJI', 5, ARRAY['しゅつ', 'すい'], ARRAY['で.る', '-で', 'だ.す', '-だ.す', 'い.でる', 'い.だす'], 'XUÝ, XUẤT, XÍCH', 'ra ngoài, đi ra; một tấn (một đoạn) trong vở tuồng', 'Exit, Leave, Go Out, Come Out, Put Out, Protrude', '', 'N5', ARRAY['Mountain'], TRUE, 'PUBLISHED')
ON CONFLICT (character) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 1, 'M52.76,13.38c1.42,1.42,1.86,2.91,1.86,5.31c0,3.18,0.19,61.81,0.19,68.31', '[]'::jsonb
FROM writing_characters WHERE character = '出'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 2, 'M29.02,36.13c0.98,1.12,1.23,2.87,1.06,4.04c-0.4,2.82-1.02,7.67-2.78,13.4c-0.43,1.41,0.07,2.84,1.55,2.39c10.61-3.24,33.9-5.33,55.97-6.28', '[]'::jsonb
FROM writing_characters WHERE character = '出'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 3, 'M86.31,30.63c0.94,1.37,1.17,3,0.94,4.87c-0.62,5.12-0.86,7.07-1.66,13.48c-0.15,1.19-0.34,1.9-0.51,3.4', '[]'::jsonb
FROM writing_characters WHERE character = '出'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 4, 'M25.27,71.13c1.11,1.11,1.6,2.74,1.31,4.29c-0.53,2.8-1.27,8.92-3.03,14.65c-0.43,1.41,0.57,3.11,2.05,2.64c12.9-4.08,41.9-6.21,59.47-7.03', '[]'::jsonb
FROM writing_characters WHERE character = '出'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 5, 'M85.06,66.88c1.06,1.49,1.56,3.12,1.44,5.37c-0.27,4.8-0.23,9.14-0.45,13.51c-0.08,1.54-0.17,3.17-0.3,4.99', '[]'::jsonb
FROM writing_characters WHERE character = '出'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO writing_characters (character, char_type, stroke_count, onyomi, kunyomi, han_viet, meaning_vi, meaning_en, romaji, jlpt_level, radicals, is_active, review_status)
VALUES ('右', 'KANJI', 5, ARRAY['う', 'ゆう'], ARRAY['みぎ'], 'HỮU', 'bên phải', 'Right', '', 'N5', ARRAY['Narwhal', 'Mouth'], TRUE, 'PUBLISHED')
ON CONFLICT (character) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 1, 'M53.5,21.5c0.62,1.12,0.69,2.23,0.25,4C49.62,42,39.5,61,25.25,74.25', '[]'::jsonb
FROM writing_characters WHERE character = '右'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 2, 'M13,42.15c1.9,0.56,5.9,0.52,7.79,0.34c23.41-2.24,49.76-5.74,67.67-6.3c3.24-0.1,6.45,0.31,9.17,0.81', '[]'::jsonb
FROM writing_characters WHERE character = '右'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 3, 'M41.75,66.5c0.75,0.75,1.35,1.93,1.54,2.95c0.94,5,2.38,16.66,3.07,22.76c0.24,2.15,0.39,2.8,0.39,3.54', '[]'::jsonb
FROM writing_characters WHERE character = '右'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 4, 'M43.25,68c5.25-0.5,29.75-3.25,37-3.75c1.75-0.12,3.24,1.52,3,2.75c-1,5.12-3.38,18-4.5,23.25', '[]'::jsonb
FROM writing_characters WHERE character = '右'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 5, 'M47,93.25c5.79-0.2,19.51-1.58,28.25-2.23c2.21-0.17,4.18-0.27,5.75-0.27', '[]'::jsonb
FROM writing_characters WHERE character = '右'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO writing_characters (character, char_type, stroke_count, onyomi, kunyomi, han_viet, meaning_vi, meaning_en, romaji, jlpt_level, radicals, is_active, review_status)
VALUES ('四', 'KANJI', 5, ARRAY['し'], ARRAY['よ', 'よ.つ', 'よっ.つ', 'よん'], 'TỨ', 'bốn, 4', 'Four', '', 'N5', ARRAY['Mouth', 'Legs'], TRUE, 'PUBLISHED')
ON CONFLICT (character) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 1, 'M14.5,31.48c1.51,1.51,2.25,3.27,2.53,5.2c1.14,7.9,2.61,25.18,4.39,40.83c0.29,2.55,0.34,3.81,0.64,6.24', '[]'::jsonb
FROM writing_characters WHERE character = '四'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 2, 'M17.85,34.04c21.65-1.92,51.52-3.92,67.82-4.3c4.85-0.11,6.31,2.62,6.04,5.38c-0.9,9.02-4.17,28.29-6.41,39.62c-0.49,2.49-0.94,4.6-1.3,6.13', '[]'::jsonb
FROM writing_characters WHERE character = '四'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 3, 'M40.5,36c0.08,0.64,0.12,1.65-0.16,2.57c-2.22,7.3-5.1,14.55-13.35,22.68', '[]'::jsonb
FROM writing_characters WHERE character = '四'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 4, 'M59.75,34.25c0.8,1.05,1.44,2.29,1.49,3.92c0.11,3.62,0.05,7.05,0.05,9.89c0,6.94,0.71,7.54,9.47,7.54c4.99,0,8.86-0.72,10.25-1.72', '[]'::jsonb
FROM writing_characters WHERE character = '四'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 5, 'M22.73,79.32c13.77-0.57,43.64-1.8,61.18-2.08', '[]'::jsonb
FROM writing_characters WHERE character = '四'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO writing_characters (character, char_type, stroke_count, onyomi, kunyomi, han_viet, meaning_vi, meaning_en, romaji, jlpt_level, radicals, is_active, review_status)
VALUES ('左', 'KANJI', 5, ARRAY['さ', 'しゃ'], ARRAY['ひだり'], 'TÁ, TẢ', 'bên trái', 'Left', '', 'N5', ARRAY['Narwhal', 'Construction'], TRUE, 'PUBLISHED')
ON CONFLICT (character) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 1, 'M20.75,40.17c2.95,0.49,5.68,0.29,8.64-0.05c14.5-1.68,29.75-4.47,47.22-5.96c2.83-0.24,5.87-0.58,8.64,0.26', '[]'::jsonb
FROM writing_characters WHERE character = '左'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 2, 'M55.48,12.5c0.27,1.57,0.21,4.18-0.29,5.93C46.59,48.64,32.07,74.14,11.25,91', '[]'::jsonb
FROM writing_characters WHERE character = '左'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 3, 'M43.25,64.59c1.25,0.29,2.38,0.29,3.86,0.17c4.86-0.37,17.91-2.17,26.92-3.77c1.88-0.33,3.97-0.5,5.97-0.11', '[]'::jsonb
FROM writing_characters WHERE character = '左'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 4, 'M58.54,66.75c1.04,1.04,1.91,2.62,1.91,4.03c0,6.84,0.04,13.22,0.04,18.72', '[]'::jsonb
FROM writing_characters WHERE character = '左'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 5, 'M28.5,92.25c2.76,0.84,5.92,0.51,8.75,0.34c14.54-0.9,32.08-2.65,48.13-3.26c3.25-0.12,6.38-0.17,9.49,0.93', '[]'::jsonb
FROM writing_characters WHERE character = '左'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO writing_characters (character, char_type, stroke_count, onyomi, kunyomi, han_viet, meaning_vi, meaning_en, romaji, jlpt_level, radicals, is_active, review_status)
VALUES ('本', 'KANJI', 5, ARRAY['ほん'], ARRAY['もと'], 'BÔN, BẢN, BỔN', 'gốc (cây); vốn có, từ trước, nguồn gốc; mình (từ xưng hô); tập sách, vở', 'Book, Present, Main, Origin, True, Real, Counter For Long Cylindrical Things', '', 'N5', ARRAY['Book'], TRUE, 'PUBLISHED')
ON CONFLICT (character) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 1, 'M20.5,33.5c1.93,0.62,4.91,1.07,8.1,0.75C42.43,32.88,66,30.75,79.64,30c3.2-0.18,7.22,0.25,9.23,0.5', '[]'::jsonb
FROM writing_characters WHERE character = '本'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 2, 'M52.1,11.12c1.25,1.25,2.05,3.23,2.05,4.99c0,0.84,0,57.16-0.02,76.76c-0.01,3.96-0.01,6.42-0.02,6.62', '[]'::jsonb
FROM writing_characters WHERE character = '本'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 3, 'M51.75,33.5c0,1-0.41,2.22-1.29,3.88C43.62,50.25,30.12,65.5,13.25,75.5', '[]'::jsonb
FROM writing_characters WHERE character = '本'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 4, 'M54.75,35.5c4.92,5.74,23.48,23.33,32.85,31.27c2.58,2.18,5.16,4.41,8.52,5.23', '[]'::jsonb
FROM writing_characters WHERE character = '本'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 5, 'M33.88,73.92c1.5,0.46,2.74,0.75,5.3,0.59c9.95-0.63,21.2-2.13,27.96-2.95c1.93-0.23,3.62-0.31,6-0.02', '[]'::jsonb
FROM writing_characters WHERE character = '本'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO writing_characters (character, char_type, stroke_count, onyomi, kunyomi, han_viet, meaning_vi, meaning_en, romaji, jlpt_level, radicals, is_active, review_status)
VALUES ('白', 'KANJI', 5, ARRAY['はく', 'びゃく'], ARRAY['しろ', 'しら-', 'しろ.い'], 'BẠCH', 'trắng, màu trắng; bạc (tóc); sạch sẽ; rõ, sáng, tỏ', 'White', '', 'N5', ARRAY['White'], TRUE, 'PUBLISHED')
ON CONFLICT (character) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 1, 'M55,13c0.38,1.5,0,3.25-0.57,4.29C51.32,22.93,46,31.12,36.81,40.22', '[]'::jsonb
FROM writing_characters WHERE character = '白'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 2, 'M25.5,40.47c1.14,1.14,1.63,2.81,1.63,4.63c0,1.55,0.95,32.47,1.32,47.14c0.06,2.58,0.13,4.66,0.18,6', '[]'::jsonb
FROM writing_characters WHERE character = '白'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 3, 'M28.27,43.98c13.98-1.73,39.08-4.47,49.67-5.48c5.19-0.5,7.37,0.76,7.06,5.38c-0.62,9.12-2.09,30.3-3.29,46.88c-0.17,2.42-0.33,4.65-0.46,6.57', '[]'::jsonb
FROM writing_characters WHERE character = '白'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 4, 'M29.13,67.44C42.25,66,72.38,63.62,82.57,63.4', '[]'::jsonb
FROM writing_characters WHERE character = '白'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 5, 'M29.69,94.49C43,93.75,66.62,92,80.19,91.41', '[]'::jsonb
FROM writing_characters WHERE character = '白'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO writing_characters (character, char_type, stroke_count, onyomi, kunyomi, han_viet, meaning_vi, meaning_en, romaji, jlpt_level, radicals, is_active, review_status)
VALUES ('万', 'KANJI', 3, ARRAY['まん', 'ばん'], ARRAY['よろず'], 'MẶC, VẠN', '(tên riêng); vạn, mười nghìn', 'Ten Thousand, 10,000', '', 'N5', ARRAY['Leaf', 'Sword'], TRUE, 'PUBLISHED')
ON CONFLICT (character) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 1, 'M14.38,24.73c2.3,0.54,6.52,0.78,8.81,0.54c21.57-2.27,44.44-5.64,64.9-5.98c3.83-0.06,6.12,0.26,8.04,0.53', '[]'::jsonb
FROM writing_characters WHERE character = '万'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 2, 'M51,41.5c1.45,0.7,3.19,1.43,5.19,1.74c7.31,1.14,17.05,1.94,22.64,1.5c4.64-0.37,6.38,1.08,5.17,4.73C77.88,68,72.75,78.75,63.87,90.4c-7.6,9.97-10.12,3.22-12.62,0.2', '[]'::jsonb
FROM writing_characters WHERE character = '万'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 3, 'M51.75,25.5c0.5,2,0.22,3.78-0.21,5.89C48.95,43.8,34.75,73.38,13.56,87.97', '[]'::jsonb
FROM writing_characters WHERE character = '万'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO writing_characters (character, char_type, stroke_count, onyomi, kunyomi, han_viet, meaning_vi, meaning_en, romaji, jlpt_level, radicals, is_active, review_status)
VALUES ('今', 'KANJI', 4, ARRAY['こん', 'きん'], ARRAY['いま'], 'KIM', 'nay, bây giờ', 'Now', '', 'N5', ARRAY['Now'], TRUE, 'PUBLISHED')
ON CONFLICT (character) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 1, 'M49.42,14.25c0.1,1.11-0.11,2.93-0.71,4.47C44.5,29.5,32,47.25,11.5,61.75', '[]'::jsonb
FROM writing_characters WHERE character = '今'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 2, 'M50.66,18.99c6.1,7.28,32.37,31.03,39.1,36.36c2.28,1.81,5.21,2.58,7.49,3.09', '[]'::jsonb
FROM writing_characters WHERE character = '今'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 3, 'M39.23,50.26c1.27,0.24,2.64,0.37,4.13,0.18c5.39-0.68,11.02-1.69,15.86-2.31c1.8-0.23,3.66-0.38,4.8-0.08', '[]'::jsonb
FROM writing_characters WHERE character = '今'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 4, 'M33.25,67.75c2.12,0.38,3.57,0.61,6,0.25c6.31-0.93,18.5-3.25,25.24-4.44C68.48,62.85,70,65,68,68.75C63.33,77.5,58.75,85,53,94.5', '[]'::jsonb
FROM writing_characters WHERE character = '今'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO writing_characters (character, char_type, stroke_count, onyomi, kunyomi, han_viet, meaning_vi, meaning_en, romaji, jlpt_level, radicals, is_active, review_status)
VALUES ('午', 'KANJI', 4, ARRAY['ご'], ARRAY['うま'], 'NGỌ', 'buổi trưa; Ngọ (ngôi 7 trong hàng Chi)', 'Noon, Sign Of The Horse, 11am-1pm, Seventh Sign Of Chinese Zodiac', '', 'N5', ARRAY['Slide', 'Dry'], TRUE, 'PUBLISHED')
ON CONFLICT (character) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 1, 'M37.5,9.14c0.06,0.7,0.22,1.85-0.11,2.83C35.25,18.25,27,29.62,17.5,39', '[]'::jsonb
FROM writing_characters WHERE character = '午'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 2, 'M32.13,27.28c2.75,0.09,4.3-0.07,5.82-0.21c12.92-1.2,20.78-2.82,33.1-4.68c2.49-0.38,4.69-0.24,5.95,0.03', '[]'::jsonb
FROM writing_characters WHERE character = '午'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 3, 'M13.88,54.53c3,0.72,7.23,0.71,9.74,0.46c19.64-1.99,42.64-4.99,63-6.16c4.22-0.24,6.77,0.22,8.89,0.45', '[]'::jsonb
FROM writing_characters WHERE character = '午'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 4, 'M53.06,28.13c1.03,1.03,1.79,2.37,1.79,4.33c0,0.88-0.02,44.17-0.13,61.04c-0.02,2.88-0.03,4.96-0.05,5.88', '[]'::jsonb
FROM writing_characters WHERE character = '午'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO writing_characters (character, char_type, stroke_count, onyomi, kunyomi, han_viet, meaning_vi, meaning_en, romaji, jlpt_level, radicals, is_active, review_status)
VALUES ('友', 'KANJI', 4, ARRAY['ゆう'], ARRAY['とも'], 'HỮU', 'bạn bè', 'Friend', '', 'N5', ARRAY['Narwhal', 'Stool'], TRUE, 'PUBLISHED')
ON CONFLICT (character) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 1, 'M17.88,37.23c2.13,0.54,5.78,0.58,7.89,0.29c17.48-2.39,35.98-4.39,54.65-5.48c3.54-0.21,5.68,0.01,7.46,0.28', '[]'::jsonb
FROM writing_characters WHERE character = '友'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 2, 'M48.22,14.14c0.53,2.11,0.53,4.3,0.31,6.73C47,38,33,72.5,15.5,85.25', '[]'::jsonb
FROM writing_characters WHERE character = '友'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 3, 'M42.66,54.86c1.09,0.27,2.51,0.28,3.59,0.14c6.88-0.88,16.62-3.25,22.43-4.88c3.45-0.97,4.6,1.45,3.11,4.55C66.5,65.62,48.12,87.75,24,96.5', '[]'::jsonb
FROM writing_characters WHERE character = '友'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 4, 'M41.25,63.5c5.15,0.45,27.5,18.62,40.93,26.36c3.2,1.84,6.65,3.76,10.32,4.39', '[]'::jsonb
FROM writing_characters WHERE character = '友'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO writing_characters (character, char_type, stroke_count, onyomi, kunyomi, han_viet, meaning_vi, meaning_en, romaji, jlpt_level, radicals, is_active, review_status)
VALUES ('父', 'KANJI', 4, ARRAY['ふ'], ARRAY['ちち'], 'PHỤ, PHỦ', 'cha, bố', 'Father', '', 'N5', ARRAY['Father'], TRUE, 'PUBLISHED')
ON CONFLICT (character) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 1, 'M38.49,18.25c0.39,1.38,0.07,2.89-0.59,4.16C32,33.91,26.32,39.13,18.75,45.62', '[]'::jsonb
FROM writing_characters WHERE character = '父'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 2, 'M69.38,19.5c7.25,4,14.29,9.68,18.88,15.5', '[]'::jsonb
FROM writing_characters WHERE character = '父'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 3, 'M67.7,39.68c0.55,1.57,0.31,3.8-0.42,5.92C60.63,64.87,48,80.25,23,90.25', '[]'::jsonb
FROM writing_characters WHERE character = '父'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 4, 'M34.25,47c4.83,0,29,25.38,45.99,37.02c3.54,2.43,7.39,4.55,11.51,5.77', '[]'::jsonb
FROM writing_characters WHERE character = '父'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO writing_characters (character, char_type, stroke_count, onyomi, kunyomi, han_viet, meaning_vi, meaning_en, romaji, jlpt_level, radicals, is_active, review_status)
VALUES ('北', 'KANJI', 5, ARRAY['ほく'], ARRAY['きた'], 'BẮC, BỐI, BỘI', 'phía bắc, phương bắc; thua trận', 'North', '', 'N5', ARRAY['Fingers', 'Spoon'], TRUE, 'PUBLISHED')
ON CONFLICT (character) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 1, 'M13.25,48.75c2.61,0.5,3.52,0.51,6.12,0.25c3.76-0.38,14.13-3.38,17.63-3.75', '[]'::jsonb
FROM writing_characters WHERE character = '北'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 2, 'M37.25,21.5c1.19,1.19,2,3.25,2,4.75c0,1,0.25,48,0.25,50.75', '[]'::jsonb
FROM writing_characters WHERE character = '北'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 3, 'M12.75,88.75c1.88,1.12,4.29,0.87,6.5-0.25C32.5,81.75,36.5,80,43.5,76.25', '[]'::jsonb
FROM writing_characters WHERE character = '北'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 4, 'M90.25,33c-0.25,1.75-1.13,3.26-2.19,4.07C82.12,41.62,73.75,46.5,65,49.75', '[]'::jsonb
FROM writing_characters WHERE character = '北'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 5, 'M60.37,19.5c1.38,1.5,1.91,3.25,1.91,5.3c0,1.12-0.28,33.71-0.28,47.7c0,14,2,15.75,17.25,15.75c15.5,0,16.5-1.5,16.5-12.49', '[]'::jsonb
FROM writing_characters WHERE character = '北'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO writing_characters (character, char_type, stroke_count, onyomi, kunyomi, han_viet, meaning_vi, meaning_en, romaji, jlpt_level, radicals, is_active, review_status)
VALUES ('半', 'KANJI', 5, ARRAY['はん'], ARRAY['なか.ば'], 'BÁN', 'một nửa; ở giữa, lưng chừng; nhỏ bé; hơi hơi', 'Half, Middle, Odd Number, Semi-, Part-', '', 'N5', ARRAY['Triceratops', 'Dry'], TRUE, 'PUBLISHED')
ON CONFLICT (character) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 1, 'M26.02,22.58c3.9,2.41,10.07,9.89,11.04,13.63', '[]'::jsonb
FROM writing_characters WHERE character = '半'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 2, 'M82.75,18.75c0.16,1.19-0.39,2.25-1.01,3.2c-2.57,3.96-7.16,8.76-12.62,12.92', '[]'::jsonb
FROM writing_characters WHERE character = '半'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 3, 'M28.3,48.57c1.78,0.38,5.07,0.21,6.83,0.03c10.62-1.09,29.12-2.99,38.5-3.67c2.97-0.22,4.75-0.17,6.24,0.02', '[]'::jsonb
FROM writing_characters WHERE character = '半'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 4, 'M13.5,67.22c2.25,0.91,6.85,1.03,9.22,0.78c18.28-1.87,40.4-3.58,62.61-4.38c4-0.14,6.41,0.25,8.42,0.52', '[]'::jsonb
FROM writing_characters WHERE character = '半'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 5, 'M52.67,11.5c1.23,1.23,1.85,3.17,1.85,4.4c0,7.6,0.1,54.35,0.1,75.1c0,3.18-0.07,5.46-0.11,6.5', '[]'::jsonb
FROM writing_characters WHERE character = '半'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO writing_characters (character, char_type, stroke_count, onyomi, kunyomi, han_viet, meaning_vi, meaning_en, romaji, jlpt_level, radicals, is_active, review_status)
VALUES ('外', 'KANJI', 5, ARRAY['がい', 'げ'], ARRAY['そと', 'ほか', 'はず.す', 'はず.れる', 'と-'], 'NGOẠI', 'bên ngoài', 'Outside', '', 'N5', ARRAY['Evening', 'Toe'], TRUE, 'PUBLISHED')
ON CONFLICT (character) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 1, 'M35.44,15.75c0.53,1.77,0.62,3.56,0.13,5.33C33.25,29.5,26.75,44.75,16.5,55', '[]'::jsonb
FROM writing_characters WHERE character = '外'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 2, 'M36.72,28.81c1.47,0.25,3.12,0.16,4.56-0.14c3.52-0.72,5.83-1.04,9.45-2.2c3.61-1.17,4.39,1.7,3.77,3.54c-5.46,16.06-22.18,50.32-39.75,58.5', '[]'::jsonb
FROM writing_characters WHERE character = '外'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 3, 'M27.5,47c3.71,1.68,9.57,6.89,10.5,9.5', '[]'::jsonb
FROM writing_characters WHERE character = '外'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 4, 'M65.56,12.13c1.09,1.09,1.76,2.87,1.76,5.25c0,0.78-0.07,54.62-0.19,73.62c-0.02,3.16-0.04,5.33-0.06,6.13', '[]'::jsonb
FROM writing_characters WHERE character = '外'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 5, 'M71.5,42.5c7.85,3.75,20.29,15.42,22.25,21.25', '[]'::jsonb
FROM writing_characters WHERE character = '外'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO writing_characters (character, char_type, stroke_count, onyomi, kunyomi, han_viet, meaning_vi, meaning_en, romaji, jlpt_level, radicals, is_active, review_status)
VALUES ('母', 'KANJI', 5, ARRAY['ぼ'], ARRAY['はは', 'も'], 'MÔ, MẪU', 'mẹ; con cái, giống cái', 'Mother', '', 'N5', ARRAY['Sun', 'Drop'], TRUE, 'PUBLISHED')
ON CONFLICT (character) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 1, 'M34.82,19.37c1.14,1.02,1.72,3.42,1.41,6.09c-1.46,12.62-8.22,32.3-13.04,44.1c-1.47,3.6-0.44,5.27,3.63,5.2c11.37-0.18,27.52,2.13,40.68,7.24c4.98,1.93,9.54,4.26,13.25,7.01', '[]'::jsonb
FROM writing_characters WHERE character = '母'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 2, 'M37.32,22.07c4.81,0.56,13.81-1.32,39.18-6.07c4-0.75,5.92,0.77,5.5,4.25c-2.25,18.62-7,49-16.06,69.18c-3.36,7.49-9.86,1.68-11.42,0', '[]'::jsonb
FROM writing_characters WHERE character = '母'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 3, 'M49,28.5c3.88,1.98,10.03,8.16,11,11.25', '[]'::jsonb
FROM writing_characters WHERE character = '母'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 4, 'M45.25,57.62c3.79,1.96,9.8,8.07,10.75,11.12', '[]'::jsonb
FROM writing_characters WHERE character = '母'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 5, 'M9.88,51.45c2.21,0.63,6.26,0.87,8.46,0.63C46.77,49,67.12,47,90.65,46.07c3.68-0.15,5.89,0.3,7.72,0.61', '[]'::jsonb
FROM writing_characters WHERE character = '母'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO writing_characters (character, char_type, stroke_count, onyomi, kunyomi, han_viet, meaning_vi, meaning_en, romaji, jlpt_level, radicals, is_active, review_status)
VALUES ('休', 'KANJI', 6, ARRAY['きゅう'], ARRAY['やす.む', 'やす.まる', 'やす.める'], 'HU, HƯU', 'nghỉ ngơi; thôi, dừng; tốt lành', 'Rest, Day Off, Retire, Sleep', '', 'N5', ARRAY['Leader', 'Tree'], TRUE, 'PUBLISHED')
ON CONFLICT (character) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 1, 'M35,16.5c0.25,1.75,0.25,4.25-0.88,6.8C28.91,35.01,22.37,46.02,10.5,60.29', '[]'::jsonb
FROM writing_characters WHERE character = '休'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 2, 'M26.28,42.5c0.72,1.25,1.26,3.48,1.26,4.75c0,12.75-0.07,29.88-0.26,42.25c-0.02,1.54-0.04,2.97-0.04,4.25', '[]'::jsonb
FROM writing_characters WHERE character = '休'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 3, 'M37.65,38.83c2.45,0.97,5.18,0.75,7.73,0.54c11.76-0.97,24.94-3.35,37.49-4.01c2.65-0.14,5.39-0.22,7.99,0.39', '[]'::jsonb
FROM writing_characters WHERE character = '休'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 4, 'M61.43,14c0.82,0.75,1.87,2.12,1.87,3.7c0,8.8,0.05,53.72-0.12,72.05c-0.03,2.88-0.06,4.91-0.08,5.75', '[]'::jsonb
FROM writing_characters WHERE character = '休'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 5, 'M62.43,38.32c0,2.18-1.1,4.31-1.9,6.04C54.57,57.4,44.96,71.84,35,78.75', '[]'::jsonb
FROM writing_characters WHERE character = '休'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 6, 'M64.12,38.08c4.45,8.37,16.21,25.33,24.99,33.19c1.96,1.76,4.35,4.18,6.9,5', '[]'::jsonb
FROM writing_characters WHERE character = '休'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO writing_characters (character, char_type, stroke_count, onyomi, kunyomi, han_viet, meaning_vi, meaning_en, romaji, jlpt_level, radicals, is_active, review_status)
VALUES ('先', 'KANJI', 6, ARRAY['せん'], ARRAY['さき', 'ま.ず'], 'TIÊN, TIẾN', 'trước', 'Before, Ahead, Previous, Future, Precedence', '', 'N5', ARRAY['Slide', 'Dirt', 'Legs'], TRUE, 'PUBLISHED')
ON CONFLICT (character) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 1, 'M37.51,21c0.07,0.62,0.15,1.61-0.14,2.49C35.25,29.88,31.62,37.38,24.5,45', '[]'::jsonb
FROM writing_characters WHERE character = '先'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 2, 'M38.13,32.04c1.5,0.09,3.95-0.16,4.64-0.22c6.48-0.57,20.36-1.82,27.82-2.94c1.65-0.25,3.66-0.13,5.16,0.27', '[]'::jsonb
FROM writing_characters WHERE character = '先'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 3, 'M52.81,12.38c1.28,1.28,2.01,3.12,2.01,4.75c0,0.75-0.05,31.92-0.07,32.87', '[]'::jsonb
FROM writing_characters WHERE character = '先'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 4, 'M15.88,53.26c3.42,0.98,7.15,0.5,10.62,0.22c15.99-1.3,38.99-3.55,59-4.4c2.94-0.13,5.84-0.03,8.75,0.47', '[]'::jsonb
FROM writing_characters WHERE character = '先'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 5, 'M45.18,55.68c0.32,1.45,0.15,2.48-0.15,3.85C43.24,67.65,35,86.62,20,96.38', '[]'::jsonb
FROM writing_characters WHERE character = '先'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 6, 'M60.49,53.62c1.07,1.07,1.38,2.71,1.38,4.98c0,7.78-0.22,14.88-0.22,21.89c0,15.14,1.1,16.04,15.85,16.04c14.62,0,15.64-1.78,15.64-11.29', '[]'::jsonb
FROM writing_characters WHERE character = '先'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO writing_characters (character, char_type, stroke_count, onyomi, kunyomi, han_viet, meaning_vi, meaning_en, romaji, jlpt_level, radicals, is_active, review_status)
VALUES ('名', 'KANJI', 6, ARRAY['めい', 'みょう'], ARRAY['な', '-な'], 'DANH', 'tên, danh; danh tiếng', 'Name, Noted, Distinguished, Reputation', '', 'N5', ARRAY['Evening', 'Mouth'], TRUE, 'PUBLISHED')
ON CONFLICT (character) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 1, 'M54.2,12.64c0.3,1.61-0.07,2.99-0.69,4.24c-3.49,7.13-13.28,19.29-25.96,27.54', '[]'::jsonb
FROM writing_characters WHERE character = '名'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 2, 'M53.25,24.16c0.88,0.47,1.95,0.5,3.28,0.37c4.37-0.43,11.99-2.47,17.81-4.1c4.18-1.17,5.46,1.02,4.41,3.51C72.25,39.38,43.88,69.62,16,77.5', '[]'::jsonb
FROM writing_characters WHERE character = '名'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 3, 'M43.62,40.88c3.62,2,8,6,9.68,9.58', '[]'::jsonb
FROM writing_characters WHERE character = '名'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 4, 'M42,67.81c0.91,0.91,1.62,2.19,1.83,3.33c0.5,2.82,2.15,14.38,3.05,20.86c0.3,2.12,0.52,3.7,0.59,4.25', '[]'::jsonb
FROM writing_characters WHERE character = '名'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 5, 'M44.53,69.52c10.82-1.38,32.39-4.4,38.01-4.53c2.76-0.06,4.08,1.63,3.25,4.64c-1.13,4.06-3.52,14.04-4.64,20.36', '[]'::jsonb
FROM writing_characters WHERE character = '名'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 6, 'M47.99,93.99c7.26-0.61,19.65-1.61,29.54-2.37c2.19-0.17,4.24-0.28,6.04-0.31', '[]'::jsonb
FROM writing_characters WHERE character = '名'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO writing_characters (character, char_type, stroke_count, onyomi, kunyomi, han_viet, meaning_vi, meaning_en, romaji, jlpt_level, radicals, is_active, review_status)
VALUES ('年', 'KANJI', 6, ARRAY['ねん'], ARRAY['とし'], 'NIÊN', 'năm; tuổi; được mùa', 'Year, Counter For Years', '', 'N5', ARRAY['Gun', 'Cow'], TRUE, 'PUBLISHED')
ON CONFLICT (character) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 1, 'M40.01,11.89c0.24,1.61-0.01,2.86-0.84,4.46c-2.53,4.84-6.91,11.4-15.86,19.62', '[]'::jsonb
FROM writing_characters WHERE character = '年'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 2, 'M39.13,23.62c2.25,0.38,4.4,0.18,5.79,0.03c11.7-1.27,21.33-2.9,33.22-4.07c2.3-0.23,4.2,0,5.35,0.26', '[]'::jsonb
FROM writing_characters WHERE character = '年'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 3, 'M30.13,43.59c1.36,0.33,3.87,0.46,5.21,0.33c10.91-1.05,28.53-3.42,40.78-4.26c2.26-0.15,3.63,0.16,4.76,0.32', '[]'::jsonb
FROM writing_characters WHERE character = '年'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 4, 'M33.75,44.5c1,1.25,1,1.97,1.01,3.5C34.8,52.33,35,65.29,35,66.25', '[]'::jsonb
FROM writing_characters WHERE character = '年'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 5, 'M13.88,67.74c1.97,0.47,5.61,0.66,7.57,0.47c20.21-2.03,36.35-4.62,66.65-5.31c3.29-0.08,5.26,0.22,6.91,0.46', '[]'::jsonb
FROM writing_characters WHERE character = '年'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 6, 'M56.56,25.46c1.12,1.12,1.79,3.54,1.79,4.94c0,0.89-0.05,44.26-0.13,61.6c-0.01,3.12-0.03,5.39-0.05,6.38', '[]'::jsonb
FROM writing_characters WHERE character = '年'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO writing_characters (character, char_type, stroke_count, onyomi, kunyomi, han_viet, meaning_vi, meaning_en, romaji, jlpt_level, radicals, is_active, review_status)
VALUES ('気', 'KANJI', 6, ARRAY['き', 'け'], ARRAY['いき'], 'KHÍ', 'khí, hơi', 'Spirit, Mind, Air, Atmosphere, Mood', '', 'N5', ARRAY['Energy', 'Treasure'], TRUE, 'PUBLISHED')
ON CONFLICT (character) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 1, 'M37.75,9.25c0.25,1.62-0.25,2.75-1,4.25C35.63,15.74,28,25.25,24,29', '[]'::jsonb
FROM writing_characters WHERE character = '気'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 2, 'M36.5,21.25c1.33-0.03,3.29-0.05,4.8-0.32c9.2-1.68,18.17-3.46,26.98-5.27c1.63-0.33,3.71-0.64,5.21-0.91', '[]'::jsonb
FROM writing_characters WHERE character = '気'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 3, 'M31.25,32.75c1.5,0.38,3.3,0.26,4.96,0.08c7.67-0.83,19.54-2.58,29.14-4.39c1.94-0.37,3.64-0.41,4.91-0.45', '[]'::jsonb
FROM writing_characters WHERE character = '気'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 4, 'M18.5,47c1.88,0.75,4,0.88,6.25,0.5c15.08-2.51,35-5.62,48.25-8c4.73-0.85,5.6,0.47,4.5,6.25c-4,21,0.71,40.32,11.5,50c7.25,6.5,6.5,0.75,6-5.25', '[]'::jsonb
FROM writing_characters WHERE character = '気'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 5, 'M57,51.75c0.12,1.62-0.17,3.03-1,4.75C49.5,70,40.25,82.75,25.75,93.25', '[]'::jsonb
FROM writing_characters WHERE character = '気'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 6, 'M30,63.75C41.5,68,54.5,78,62.25,90.5', '[]'::jsonb
FROM writing_characters WHERE character = '気'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO writing_characters (character, char_type, stroke_count, onyomi, kunyomi, han_viet, meaning_vi, meaning_en, romaji, jlpt_level, radicals, is_active, review_status)
VALUES ('百', 'KANJI', 6, ARRAY['ひゃく', 'びゃく'], ARRAY['もも'], 'BÁ, BÁCH, MẠCH', 'trăm, 100; rất nhiều', 'Hundred', '', 'N5', ARRAY['Leaf', 'Sun'], TRUE, 'PUBLISHED')
ON CONFLICT (character) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 1, 'M16.13,20.23c2.22,0.54,6.29,0.75,8.51,0.54c21.49-2.02,41.86-4.39,59.22-4.98c3.7-0.12,5.92,0.26,7.77,0.53', '[]'::jsonb
FROM writing_characters WHERE character = '百'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 2, 'M52.31,21.75c0.19,1.38,0.19,2.5-0.38,3.93c-1.65,4.19-4.81,9.19-8.66,14.68', '[]'::jsonb
FROM writing_characters WHERE character = '百'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 3, 'M30.75,42.82c0.96,0.96,1.64,2.45,1.72,4.19c0.41,8.74,0.96,32.92,1.18,43.74c0.05,2.48,0.08,4.12,0.1,4.5', '[]'::jsonb
FROM writing_characters WHERE character = '百'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 4, 'M33.55,44.8c10.35-1.37,35.73-4.38,38.78-4.59c3.15-0.22,4.92,1.17,4.92,4.24c0,4.48-0.68,32-0.92,44.06c-0.06,3.02-0.1,5.05-0.11,5.48', '[]'::jsonb
FROM writing_characters WHERE character = '百'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 5, 'M34.14,66.95c10.24-1.08,32.11-3.2,41.44-3.57', '[]'::jsonb
FROM writing_characters WHERE character = '百'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 6, 'M34.97,92.87c8.78-0.87,30.53-2.12,40.06-2.39', '[]'::jsonb
FROM writing_characters WHERE character = '百'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO writing_characters (character, char_type, stroke_count, onyomi, kunyomi, han_viet, meaning_vi, meaning_en, romaji, jlpt_level, radicals, is_active, review_status)
VALUES ('男', 'KANJI', 7, ARRAY['だん', 'なん'], ARRAY['おとこ', 'お'], 'NAM', 'đàn ông, con trai; tước Nam', 'Male', '', 'N5', ARRAY['Rice Paddy', 'Power'], TRUE, 'PUBLISHED')
ON CONFLICT (character) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 1, 'M26.5,14.25c0.88,0.88,1.56,1.99,1.73,2.98c0.84,4.77,2.47,16.75,3.34,26.04c0.18,1.95,0.37,2.37,0.55,4.23', '[]'::jsonb
FROM writing_characters WHERE character = '男'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 2, 'M29,15.95c11.38-1.45,41.21-4.57,49.56-4.71c3.9-0.07,5.44,1.51,4.91,5.29c-0.45,3.21-3.15,15.19-4.94,22.23c-0.41,1.62-0.79,2.99-1.4,4.19', '[]'::jsonb
FROM writing_characters WHERE character = '男'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 3, 'M54,15.97c0.77,0.77,1,1.91,1,2.79c0.02,6.32,0.2,22,0.2,22.75', '[]'::jsonb
FROM writing_characters WHERE character = '男'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 4, 'M30.98,30.82C45,29.12,57.12,28,80.53,26.34', '[]'::jsonb
FROM writing_characters WHERE character = '男'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 5, 'M32.87,44.74c11.38-1.24,28.38-2.99,44.14-3.7', '[]'::jsonb
FROM writing_characters WHERE character = '男'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 6, 'M19.98,60.98c2.15,0.67,4.58,0.78,6.77,0.53c13.46-1.53,42.24-5.66,51.88-6.86c5.26-0.66,6.86,1.04,5.72,6.27c-1.92,8.83-9,27.39-15.66,33.19c-5.11,4.45-7.44,2.14-9.69-0.86', '[]'::jsonb
FROM writing_characters WHERE character = '男'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 7, 'M53.22,46.43c0.28,1.32,0.29,3.04-0.2,4.57C49.12,63.12,38,81.25,17.14,92.06', '[]'::jsonb
FROM writing_characters WHERE character = '男'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO writing_characters (character, char_type, stroke_count, onyomi, kunyomi, han_viet, meaning_vi, meaning_en, romaji, jlpt_level, radicals, is_active, review_status)
VALUES ('見', 'KANJI', 7, ARRAY['けん'], ARRAY['み.る', 'み.える', 'み.せる'], 'HIỆN, KIẾN', 'tỏ rõ, hiện ra; gặp, thấy', 'See, Hopes, Chances, Idea, Opinion, Look At, Visible', '', 'N5', ARRAY['See'], TRUE, 'PUBLISHED')
ON CONFLICT (character) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 1, 'M32.5,15.46c0.96,0.96,1.18,2.1,1.18,3.52c0,1.12,0.07,27.43-0.02,39.27c-0.02,3.12-0.02,5.21,0.02,5.5', '[]'::jsonb
FROM writing_characters WHERE character = '見'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 2, 'M34.65,17.15c9.23-1.27,22.23-2.65,31.1-3.65c2.99-0.34,4.26,1.01,4.26,3.55c0,2.5-0.1,28.08-0.14,38.96c-0.01,2.91-0.02,4.75-0.02,4.79', '[]'::jsonb
FROM writing_characters WHERE character = '見'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 3, 'M34.84,31.1c7.28-0.6,25.03-2.98,33.9-3.38', '[]'::jsonb
FROM writing_characters WHERE character = '見'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 4, 'M34.86,44.63C43.38,44,59,42.12,68.6,41.51', '[]'::jsonb
FROM writing_characters WHERE character = '見'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 5, 'M34.71,59.66C44.5,59,58.38,57.5,68.45,57.03', '[]'::jsonb
FROM writing_characters WHERE character = '見'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 6, 'M41.99,66.75c0.26,1.5,0.01,2.99-0.41,4.04c-2.7,6.83-13.83,20.83-28.41,27.87', '[]'::jsonb
FROM writing_characters WHERE character = '見'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 7, 'M54.49,61.37c1.07,1.07,1.33,2.59,1.38,4.43c0.2,8.19,0.04,6.2,0.04,18.2c0,10.12,1.23,11.53,18.54,11.53c18.81,0,19.81-1.53,19.81-10.12', '[]'::jsonb
FROM writing_characters WHERE character = '見'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO writing_characters (character, char_type, stroke_count, onyomi, kunyomi, han_viet, meaning_vi, meaning_en, romaji, jlpt_level, radicals, is_active, review_status)
VALUES ('車', 'KANJI', 7, ARRAY['しゃ'], ARRAY['くるま'], 'XA', 'cái xe', 'Car', '', 'N5', ARRAY['Car'], TRUE, 'PUBLISHED')
ON CONFLICT (character) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 1, 'M26,26c2.85,0.69,6.1,0.14,8.98-0.1c11.09-0.93,25.8-2.64,38.89-3.51c2.68-0.18,5.22-0.16,7.88,0.23', '[]'::jsonb
FROM writing_characters WHERE character = '車'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 2, 'M27.5,37.92c0.81,0.5,1.83,2.39,1.98,3.05c0.85,3.73,1.83,11.31,2.95,18.54c0.32,2.09,0.41,3.2,0.75,5.24', '[]'::jsonb
FROM writing_characters WHERE character = '車'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 3, 'M30.36,39.49c14.52-1.61,36.14-4.11,47.63-4.5c3.46-0.12,4.17,1.57,4.03,3.08c-0.42,4.32-2.01,13.81-3.5,20.19c-0.21,0.89-0.51,1.87-0.76,3', '[]'::jsonb
FROM writing_characters WHERE character = '車'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 4, 'M32.5,51.25c13.75-1.5,34.25-3.75,47-4.25', '[]'::jsonb
FROM writing_characters WHERE character = '車'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 5, 'M34.25,63.25C46,61.62,65,59.75,77,59.25', '[]'::jsonb
FROM writing_characters WHERE character = '車'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 6, 'M16,77.86c3.62,0.89,7.38,0.77,10.63,0.39c18.51-2.14,39.85-4.55,57.12-5.45c3.05-0.16,6.5-0.05,9.5,0.63', '[]'::jsonb
FROM writing_characters WHERE character = '車'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 7, 'M52.5,11.51c1.36,1.36,2.06,2.78,2.14,6.02c0.03,1.07-0.07,48.79-0.19,70.7c-0.02,4.27-0.05,7.36-0.07,8.65', '[]'::jsonb
FROM writing_characters WHERE character = '車'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO writing_characters (character, char_type, stroke_count, onyomi, kunyomi, han_viet, meaning_vi, meaning_en, romaji, jlpt_level, radicals, is_active, review_status)
VALUES ('毎', 'KANJI', 6, ARRAY['まい'], ARRAY['ごと', '-ごと.に'], 'MỖI', 'mỗi một', 'Every', '', 'N5', ARRAY['Gun', 'Window'], TRUE, 'PUBLISHED')
ON CONFLICT (character) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 1, 'M44.13,10.62c0.11,1.39-0.04,2.48-0.54,3.78c-2.23,5.81-9.37,16.73-16.84,22.34', '[]'::jsonb
FROM writing_characters WHERE character = '毎'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 2, 'M43.77,23.04c1.73,0.08,3.68,0.03,4.73-0.04c6.88-0.46,19.87-3.02,27.66-4.42c2.11-0.38,3.74-0.39,5.84-0.14', '[]'::jsonb
FROM writing_characters WHERE character = '毎'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 3, 'M41.55,34.87c1.1,0.91,1.96,3.97,1.36,6.3C40.25,51.5,33.25,67,29.45,72.74c-1.98,2.99-0.52,4.58,1.57,4.62c11.98,0.26,25.1,1.88,37.95,5.72c5.92,1.77,11.28,4.6,15.77,7.92', '[]'::jsonb
FROM writing_characters WHERE character = '毎'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 4, 'M44.26,37.32c9.17-1.07,22.11-2.57,28.58-3.32c3.9-0.45,6.36,2.01,6.1,5.12c-1.44,17.51-4.81,42.51-11.32,56.07c-2.99,6.22-6.5-1.15-7.52-2.27', '[]'::jsonb
FROM writing_characters WHERE character = '毎'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 5, 'M58.71,39.5c0.51,1,0.75,2.74,0.53,4c-1.53,8.5-7.11,27-9.92,35', '[]'::jsonb
FROM writing_characters WHERE character = '毎'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 6, 'M10.88,58.99c2.46,0.56,6.98,0.49,9.45,0.31c21.05-1.54,44.8-3.42,69.3-3.88c4.11-0.08,6.57,0.26,8.62,0.54', '[]'::jsonb
FROM writing_characters WHERE character = '毎'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO writing_characters (character, char_type, stroke_count, onyomi, kunyomi, han_viet, meaning_vi, meaning_en, romaji, jlpt_level, radicals, is_active, review_status)
VALUES ('行', 'KANJI', 6, ARRAY['こう', 'ぎょう', 'あん'], ARRAY['い.く', 'ゆ.く', '-ゆ.き', '-ゆき', '-い.き', '-いき', 'おこな.う', 'おこ.なう'], 'HÀNG, HÀNH, HÃNG, HẠNG, HẠNH', 'hàng, dòng; đi; làm; hàng, dãy', 'Going, Journey, Carry Out, Conduct, Act, Line, Row, Bank', '', 'N5', ARRAY['Go'], TRUE, 'PUBLISHED')
ON CONFLICT (character) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 1, 'M32.49,12c-0.12,1-0.45,1.9-1.1,2.62C28.29,18.06,22.2,22.6,12.5,28', '[]'::jsonb
FROM writing_characters WHERE character = '行'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 2, 'M36.5,31.75c0.07,0.73,0.08,2.28-0.39,3.18C32.12,42.5,23.83,52.5,11,62.75', '[]'::jsonb
FROM writing_characters WHERE character = '行'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 3, 'M25.57,51.75c0.9,0.9,1.23,2.25,1.23,3.26c0,0.72,0.04,24.47-0.07,35.49c-0.02,2.19-0.04,3.87-0.07,4.75', '[]'::jsonb
FROM writing_characters WHERE character = '行'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 4, 'M50.5,18.45c1.44,0.35,3.81,0.52,5.23,0.35c7.14-0.8,16.01-2.43,24.49-3.06c2.38-0.18,3.83-0.06,5.02,0.11', '[]'::jsonb
FROM writing_characters WHERE character = '行'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 5, 'M43.13,41.42c1.5,0.38,4.27,0.58,5.76,0.38c12.86-1.67,28.86-4.05,41.85-5.38c2.49-0.26,4.01,0.18,5.26,0.37', '[]'::jsonb
FROM writing_characters WHERE character = '行'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 6, 'M71.52,41.33c1.26,1.26,1.76,2.79,1.76,5.27c0,14.56-0.26,38.66-0.26,43.62c0,8.03-7.21-0.5-8.71-1.75', '[]'::jsonb
FROM writing_characters WHERE character = '行'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO writing_characters (character, char_type, stroke_count, onyomi, kunyomi, han_viet, meaning_vi, meaning_en, romaji, jlpt_level, radicals, is_active, review_status)
VALUES ('西', 'KANJI', 6, ARRAY['せい', 'さい', 'す'], ARRAY['にし'], 'TÂY, TÊ', 'phía tây, phương tây', 'West, Spain', '', 'N5', ARRAY['West'], TRUE, 'PUBLISHED')
ON CONFLICT (character) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 1, 'M20.63,24.22c2.31,0.34,6.05,0.3,8.35,0.09c15.15-1.43,36.18-3.38,49.83-4.02c3.84-0.18,6.66-0.09,8.58,0.08', '[]'::jsonb
FROM writing_characters WHERE character = '西'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 2, 'M18.25,48.63c1.25,1.25,2.14,3.42,2.33,4.49c1.25,6.77,3.24,20.18,5.12,33.35c0.27,1.87,0.53,3.72,0.77,5.53', '[]'::jsonb
FROM writing_characters WHERE character = '西'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 3, 'M20.75,50.26c17.77-1.6,53.73-5.05,63.68-5.27c4.51-0.1,7.27,2.79,6.55,6.54c-1.6,8.35-4.1,21.97-6.49,32.97c-0.36,1.68-0.61,2.5-1.13,4.6', '[]'::jsonb
FROM writing_characters WHERE character = '西'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 4, 'M42.75,26.75c0.5,1.25,0.81,3.99,0.85,5.73C43.98,48.84,42,64.5,30.64,73.77', '[]'::jsonb
FROM writing_characters WHERE character = '西'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 5, 'M60.6,24.46c0.94,1.12,1.74,2.94,1.74,4.67c0,13.33-0.45,21.61-0.45,27.37c0,10.25,0.62,11.14,11.36,11.14c6.38,0,8.94-0.54,10.55-1.53', '[]'::jsonb
FROM writing_characters WHERE character = '西'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 6, 'M27.34,88.55C40.75,88,66.69,86.2,83,85.97', '[]'::jsonb
FROM writing_characters WHERE character = '西'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO writing_characters (character, char_type, stroke_count, onyomi, kunyomi, han_viet, meaning_vi, meaning_en, romaji, jlpt_level, radicals, is_active, review_status)
VALUES ('何', 'KANJI', 7, ARRAY['か'], ARRAY['なに', 'なん', 'なに-', 'なん-'], 'HÀ', 'nào (trong hà nhân, hà xứ, ...)', 'What', '', 'N5', ARRAY['Leader', 'Lip Ring'], TRUE, 'PUBLISHED')
ON CONFLICT (character) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 1, 'M32.5,13.75c0.23,2.1-0.19,3.81-0.8,5.66c-3.95,11.84-9.67,23.37-20.45,37.34', '[]'::jsonb
FROM writing_characters WHERE character = '何'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 2, 'M26.76,36.5c1.24,1.5,1.54,3.04,1.54,5.5c0,9.46-0.13,30.79-0.17,44.62c-0.01,2.6-0.01,4.94-0.01,6.88', '[]'::jsonb
FROM writing_characters WHERE character = '何'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 3, 'M38.88,26.64c1.74,0.5,4.68,0.67,6.41,0.5c13.21-1.27,33.84-4.77,46.26-5.86c2.88-0.25,4.63,0.24,6.08,0.49', '[]'::jsonb
FROM writing_characters WHERE character = '何'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 4, 'M40.87,44c0.75,0.75,1.26,1.62,1.36,2.21c0.67,4.06,1.44,10.16,2.25,16.3c0.27,2.04,0.26,2.01,0.47,3.75', '[]'::jsonb
FROM writing_characters WHERE character = '何'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 5, 'M43.27,45.6c6.28-1.17,15.14-2.97,19.73-3.72c3.13-0.51,4.4,0.31,3.68,3.51c-0.86,3.86-2.49,10.33-3.28,14.14', '[]'::jsonb
FROM writing_characters WHERE character = '何'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 6, 'M45.59,63.17c3.71-0.39,9.45-1.24,14.45-1.85c1.89-0.23,3.82-0.47,5.75-0.69', '[]'::jsonb
FROM writing_characters WHERE character = '何'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 7, 'M83.25,24.75c1,1,1.74,2.18,1.81,4.99c0.33,13.52-0.21,56.44-0.21,61.04c0,10.71-6.35,2.71-9.18-0.77', '[]'::jsonb
FROM writing_characters WHERE character = '何'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO writing_characters (character, char_type, stroke_count, onyomi, kunyomi, han_viet, meaning_vi, meaning_en, romaji, jlpt_level, radicals, is_active, review_status)
VALUES ('来', 'KANJI', 7, ARRAY['らい', 'たい'], ARRAY['く.る', 'きた.る', 'きた.す', 'き.たす', 'き.たる', 'き', 'こ'], 'LAI, LÃI', 'đến nơi', 'Come, Due, Next, Cause, Become', '', 'N5', ARRAY['Ground', 'Rice'], TRUE, 'PUBLISHED')
ON CONFLICT (character) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 1, 'M25.54,28.33c1.61,0.39,4.58,0.53,6.19,0.39c16.32-1.46,27.01-3.46,43.69-3.67c2.69-0.03,4.31,0.18,5.65,0.38', '[]'::jsonb
FROM writing_characters WHERE character = '来'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 2, 'M30.12,37.62c2.85,2.07,7.16,7.91,7.88,11.12', '[]'::jsonb
FROM writing_characters WHERE character = '来'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 3, 'M74.52,33c0.08,0.98-0.11,1.9-0.58,2.77c-1.33,3.04-4.7,7.77-9.06,10.86', '[]'::jsonb
FROM writing_characters WHERE character = '来'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 4, 'M16.62,57c2.28,0.5,4.9,0.74,8.42,0.5c14.81-1,39.08-3.5,58.03-4.25c3.54-0.14,6.33,0.25,8.55,0.5', '[]'::jsonb
FROM writing_characters WHERE character = '来'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 5, 'M51.67,10.75c1.33,1,2.18,2.75,2.18,4.5c0,0.9,0.06,58.96-0.17,78c-0.03,2.77-0.07,4.71-0.1,5.5', '[]'::jsonb
FROM writing_characters WHERE character = '来'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 6, 'M49.75,56.5c0,1.5-0.44,2.48-0.82,3.11C42.37,70.49,29,83.75,15.75,90.5', '[]'::jsonb
FROM writing_characters WHERE character = '来'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 7, 'M55,56.25c4.38,3.88,19.75,19,29.73,26.28c2.82,2.06,6.52,4.5,10.02,5.22', '[]'::jsonb
FROM writing_characters WHERE character = '来'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO writing_characters (character, char_type, stroke_count, onyomi, kunyomi, han_viet, meaning_vi, meaning_en, romaji, jlpt_level, radicals, is_active, review_status)
VALUES ('学', 'KANJI', 8, ARRAY['がく'], ARRAY['まな.ぶ'], 'HỌC', 'học hành', 'Study, Learning, Science', '', 'N5', ARRAY['Viking', 'Child'], TRUE, 'PUBLISHED')
ON CONFLICT (character) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 1, 'M29.5,17.25c3.5,3,6.5,7.25,7.75,9.75', '[]'::jsonb
FROM writing_characters WHERE character = '学'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 2, 'M49,12c1.25,2,4.75,8.25,5.25,11.5', '[]'::jsonb
FROM writing_characters WHERE character = '学'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 3, 'M75,11c0.25,1.75-0.12,2.75-0.75,4.25c-1.29,3.1-4.25,7.38-6.5,9.75', '[]'::jsonb
FROM writing_characters WHERE character = '学'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 4, 'M21.25,33.75c-0.12,4.75-2,12.5-3.75,16.25', '[]'::jsonb
FROM writing_characters WHERE character = '学'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 5, 'M23.5,36.5c17-1.62,42.38-5.5,60-5.75c9.5-0.13,4.12,5.12,0,9', '[]'::jsonb
FROM writing_characters WHERE character = '学'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 6, 'M37.25,46.5c1,0.25,3.75,0.25,5.5-0.25s18.25-4,20-4s2.75,0.75,1,2.25S54.5,53.5,53,54.75', '[]'::jsonb
FROM writing_characters WHERE character = '学'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 7, 'M50.75,55.75c4,8.75,7.18,24.67,1.75,38c-2.75,6.75-7.75,1.25-9.75-2', '[]'::jsonb
FROM writing_characters WHERE character = '学'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 8, 'M15.75,67.75c1.75,1,4.64,1.36,7.5,1c15.88-2,44.43-6.25,61.37-5.5c2.5,0.11,4.72,0.25,6.39,1', '[]'::jsonb
FROM writing_characters WHERE character = '学'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO writing_characters (character, char_type, stroke_count, onyomi, kunyomi, han_viet, meaning_vi, meaning_en, romaji, jlpt_level, radicals, is_active, review_status)
VALUES ('金', 'KANJI', 8, ARRAY['きん', 'こん', 'ごん'], ARRAY['かね', 'かな-', '-がね'], 'KIM', 'vàng, tiền; sao Kim; nước Kim', 'Gold', '', 'N5', ARRAY['Gold'], TRUE, 'PUBLISHED')
ON CONFLICT (character) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 1, 'M51.75,11.88c0.25,1.52-0.22,3.57-0.8,4.84C47.73,23.79,33.13,47.1,14.5,58', '[]'::jsonb
FROM writing_characters WHERE character = '金'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 2, 'M52.25,18.25c9.5,7.5,34.14,30.88,37.21,32.67c3.12,1.82,4.14,2.66,5.54,2.83', '[]'::jsonb
FROM writing_characters WHERE character = '金'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 3, 'M34.02,47.08c1.69,0.65,3.85,0.36,5.6,0.21c6.91-0.6,14.33-1.69,23.99-2.64c2.07-0.2,4.1-0.4,6.15,0.12', '[]'::jsonb
FROM writing_characters WHERE character = '金'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 4, 'M30.18,64.96c1.95,0.67,4.47,0.31,6.47,0.12c9.24-0.87,17.42-1.58,31.35-2.53c2.3-0.16,4.68-0.36,6.96,0.08', '[]'::jsonb
FROM writing_characters WHERE character = '金'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 5, 'M51.47,48.82c0.89,0.85,0.89,3.76,0.89,4.43c0,3.64,0.27,38.71,0.22,39.82', '[]'::jsonb
FROM writing_characters WHERE character = '金'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 6, 'M31,74.75c3.25,3,7.48,9.27,8.5,12', '[]'::jsonb
FROM writing_characters WHERE character = '金'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 7, 'M73.01,72.11c0.24,1.14,0.11,2.46-0.54,3.51C70.38,79,66.44,83.22,63,86', '[]'::jsonb
FROM writing_characters WHERE character = '金'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 8, 'M18.5,94.86c2.88,1.01,6.41,0.4,9.37,0.15c16.55-1.42,32.95-2.12,51.51-3c3.13-0.15,6.32-0.27,9.38,0.59', '[]'::jsonb
FROM writing_characters WHERE character = '金'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO writing_characters (character, char_type, stroke_count, onyomi, kunyomi, han_viet, meaning_vi, meaning_en, romaji, jlpt_level, radicals, is_active, review_status)
VALUES ('雨', 'KANJI', 8, ARRAY['う'], ARRAY['あめ', 'あま-', '-さめ'], 'VÚ, VŨ, VỤ', 'mưa', 'Rain', '', 'N5', ARRAY['Rain'], TRUE, 'PUBLISHED')
ON CONFLICT (character) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 1, 'M25.75,22.37c1.87,0.4,4.47,0.62,6.32,0.4c11.68-1.39,28.28-3.77,41.25-4.64c2.49-0.17,4.37-0.12,7.18,0.28', '[]'::jsonb
FROM writing_characters WHERE character = '雨'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 2, 'M15.5,41.25c1.25,1.5,1.66,3.26,1.89,5.19c1.24,10.69,2.19,26.61,2.66,36.31c0.13,2.7,0.2,5,0.2,6', '[]'::jsonb
FROM writing_characters WHERE character = '雨'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 3, 'M18.25,44.25c1.42-0.09,62.76-5.33,69.5-6c2.5-0.25,4.61,1,4.5,3.75c-0.5,12.75-1.77,28.11-6,44.75c-1.88,7.38-5.38,1.88-8.5-1.25', '[]'::jsonb
FROM writing_characters WHERE character = '雨'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 4, 'M52.25,23.5C53.31,24.56,54,26.25,54,28c0,0.82-0.25,37.8-0.43,53c-0.04,3.43-0.07,5.74-0.07,6.25', '[]'::jsonb
FROM writing_characters WHERE character = '雨'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 5, 'M31,53.5c4.21,1.24,8.95,3.94,11.25,6', '[]'::jsonb
FROM writing_characters WHERE character = '雨'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 6, 'M30.5,68.75c3.8,1.26,9.68,5.89,11.75,8', '[]'::jsonb
FROM writing_characters WHERE character = '雨'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 7, 'M66.88,48.88c4.98,1.99,10.63,5.97,12.62,7.62', '[]'::jsonb
FROM writing_characters WHERE character = '雨'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 8, 'M67.25,66.5c2.75,1,9,5.5,11,7.75', '[]'::jsonb
FROM writing_characters WHERE character = '雨'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO writing_characters (character, char_type, stroke_count, onyomi, kunyomi, han_viet, meaning_vi, meaning_en, romaji, jlpt_level, radicals, is_active, review_status)
VALUES ('国', 'KANJI', 8, ARRAY['こく'], ARRAY['くに'], 'QUỐC', 'đất nước, quốc gia', 'Country', '', 'N5', ARRAY['Mouth', 'King', 'Drop'], TRUE, 'PUBLISHED')
ON CONFLICT (character) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 1, 'M19,16.82c1.09,1.09,1.61,2.51,1.61,4.41c0,14.65-0.22,44.9-0.22,71.53c0,1.95-0.06,3.86-0.09,5.75', '[]'::jsonb
FROM writing_characters WHERE character = '国'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 2, 'M21.52,18.67C41.38,16.75,74.03,13.5,85,13.5c3.38,0,5,1.85,5,5.25c0,15.36-0.04,47.89-0.08,70.62c0,1.68,0,3.31,0,4.88', '[]'::jsonb
FROM writing_characters WHERE character = '国'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 3, 'M35.12,33.88c1.32,0.28,4.2,0.44,5.51,0.28c10.47-1.29,20.62-2.54,28.62-3.13c2.02-0.15,3.88-0.19,5.56,0.04', '[]'::jsonb
FROM writing_characters WHERE character = '国'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 4, 'M52.8,34.89c0.96,0.97,1.47,2.48,1.47,3.81c0,3.99-0.13,24.74-0.09,33.55', '[]'::jsonb
FROM writing_characters WHERE character = '国'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 5, 'M37.58,52.16c1.79,0.22,3.41,0.14,5.36-0.08c7.56-0.83,17.56-1.99,25.38-2.8c1.25-0.13,4.02-0.15,5.89,0.17', '[]'::jsonb
FROM writing_characters WHERE character = '国'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 6, 'M31.08,75.14c1.54,0.36,3.85,0.28,5.19,0.16c9.98-0.92,25.85-2.67,37.03-3.54c2.15-0.17,5.04-0.18,6.12,0.13', '[]'::jsonb
FROM writing_characters WHERE character = '国'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 7, 'M67.75,56.62c3,1.75,6.12,5.12,8,8.38', '[]'::jsonb
FROM writing_characters WHERE character = '国'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 8, 'M21.5,93.01c14.25-0.51,48.38-1.89,67-2.51', '[]'::jsonb
FROM writing_characters WHERE character = '国'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO writing_characters (character, char_type, stroke_count, onyomi, kunyomi, han_viet, meaning_vi, meaning_en, romaji, jlpt_level, radicals, is_active, review_status)
VALUES ('東', 'KANJI', 8, ARRAY['とう'], ARRAY['ひがし'], 'ĐÔNG', 'phía đông, phương đông', 'East', '', 'N5', ARRAY['Tree', 'Sun'], TRUE, 'PUBLISHED')
ON CONFLICT (character) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 1, 'M30.63,25.23c2.36,0.62,4.86,0.47,7.25,0.22c8.24-0.86,22.7-2.7,32.4-3.57c2.38-0.21,4.51-0.14,6.85,0.22', '[]'::jsonb
FROM writing_characters WHERE character = '東'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 2, 'M26.77,37.86c1.03,1.03,1.78,2.05,2.07,3.44c0.86,4.14,3.61,16.02,4.97,21.91c0.43,1.87,0.72,3.14,0.76,3.36', '[]'::jsonb
FROM writing_characters WHERE character = '東'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 3, 'M29.55,39.31c14.7-2.12,34.45-4.37,48.18-5.5c2.89-0.24,4.02,2.01,3.49,4.2c-1.33,5.48-2.84,12.21-5.27,19.87c-0.52,1.65-1.08,3.3-1.7,4.94', '[]'::jsonb
FROM writing_characters WHERE character = '東'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 4, 'M32.25,51.07c8.12-0.88,37.75-4.12,45.57-4.4', '[]'::jsonb
FROM writing_characters WHERE character = '東'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 5, 'M35.76,63.99c8.99-1.05,28.37-2.68,38.3-3.23', '[]'::jsonb
FROM writing_characters WHERE character = '東'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 6, 'M51.25,12.32c1.5,1.5,2.25,3.5,2.25,5.25c0,4.5,0.06,55.21-0.14,75.75c-0.04,3.7-0.07,5.29-0.11,6.25', '[]'::jsonb
FROM writing_characters WHERE character = '東'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 7, 'M51.62,63.94c-0.24,1.91-0.81,2.76-1.27,3.45c-6.59,9.83-20.19,21.12-31.6,26.42', '[]'::jsonb
FROM writing_characters WHERE character = '東'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 8, 'M55,64.44c7.5,7.2,21.77,17.49,29.78,22.16c2.81,1.64,6.1,3.51,9.34,4.09', '[]'::jsonb
FROM writing_characters WHERE character = '東'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO writing_characters (character, char_type, stroke_count, onyomi, kunyomi, han_viet, meaning_vi, meaning_en, romaji, jlpt_level, radicals, is_active, review_status)
VALUES ('長', 'KANJI', 8, ARRAY['ちょう'], ARRAY['なが.い', 'おさ'], 'TRÀNG, TRƯỚNG, TRƯỜNG, TRƯỞNG, TRƯỢNG', 'dài; lâu; to, lớn; đứng đầu', 'Long, Leader, Superior, Senior', '', 'N5', ARRAY['Long'], TRUE, 'PUBLISHED')
ON CONFLICT (character) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 1, 'M34,14.75C35.25,16,36,18,36,19.5s0,33.5,0,35.75', '[]'::jsonb
FROM writing_characters WHERE character = '長'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 2, 'M37.75,16c9.6-0.15,21.73-3.26,26.63-4.2c1.97-0.38,3.9-0.41,5.87,0.06', '[]'::jsonb
FROM writing_characters WHERE character = '長'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 3, 'M37.25,29.5C48.12,28.25,57,27,64,25.72c1.96-0.36,3.76-0.25,5.25,0.06', '[]'::jsonb
FROM writing_characters WHERE character = '長'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 4, 'M37.5,42.5c8.88-0.86,21.62-2.62,26.75-3.53c1.97-0.35,3.88-0.32,5.75,0', '[]'::jsonb
FROM writing_characters WHERE character = '長'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 5, 'M10.88,58.23c3.14,0.86,6.44,0.68,9.62,0.29c19.73-2.35,44.86-6.1,65-7.61c2.97-0.22,5.7-0.08,8.63,0.4', '[]'::jsonb
FROM writing_characters WHERE character = '長'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 6, 'M31.25,60.25c0.94,0.94,1.39,2.38,1.39,4c0,11.82-0.7,28.19-0.7,30.19s1.65,3.14,3.74,1.64c2.09-1.5,17.25-11.09,20.03-12.59', '[]'::jsonb
FROM writing_characters WHERE character = '長'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 7, 'M76.52,55.25c0.23,1.25-0.33,2.45-1.05,3.41C73.5,61.25,69.62,65,64.62,68.75', '[]'::jsonb
FROM writing_characters WHERE character = '長'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 8, 'M46.5,62.25c8.21,0,34.52,25.9,44.28,29.5C93.12,92.61,94.51,93,97,93.5', '[]'::jsonb
FROM writing_characters WHERE character = '長'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO writing_characters (character, char_type, stroke_count, onyomi, kunyomi, han_viet, meaning_vi, meaning_en, romaji, jlpt_level, radicals, is_active, review_status)
VALUES ('前', 'KANJI', 9, ARRAY['ぜん'], ARRAY['まえ', '-まえ'], 'TIỀN, TIỄN', 'trước', 'In Front, Before', '', 'N5', ARRAY['Horns', 'Ground', 'Moon', 'Knife'], TRUE, 'PUBLISHED')
ON CONFLICT (character) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 1, 'M33.5,14.5c3.34,2.07,8.64,8.5,9.48,11.72', '[]'::jsonb
FROM writing_characters WHERE character = '前'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 2, 'M72.67,12c0.26,1.14,0.07,2.23-0.52,3.19c-2.12,3.41-6.02,8.65-8.6,11.31', '[]'::jsonb
FROM writing_characters WHERE character = '前'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 3, 'M13.38,33.24c2.63,0.72,7.46,0.94,10.08,0.72c21.67-1.83,39.8-3.58,62.68-4.61c4.37-0.2,7.01,0.34,9.2,0.7', '[]'::jsonb
FROM writing_characters WHERE character = '前'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 4, 'M24.65,45.46c1.1,1.29,1.63,2.92,1.63,4.17c0,3.05,0.1,28.65,0.08,40.62c0,3.23-0.03,5.46-0.1,6', '[]'::jsonb
FROM writing_characters WHERE character = '前'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 5, 'M27.01,46.8c2.3-0.41,13.37-2.3,17.2-2.9c2.2-0.34,3.51,1.1,3.51,3.24c0,1.02-0.16,30.82-0.16,44.63c0,5.48-3.79,2.98-5.93,0.53', '[]'::jsonb
FROM writing_characters WHERE character = '前'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 6, 'M27.65,60.01c5.72-0.76,13.63-1.81,18.59-2.32', '[]'::jsonb
FROM writing_characters WHERE character = '前'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 7, 'M27.57,73.47c4.41-0.51,13.3-1.54,18.4-1.89', '[]'::jsonb
FROM writing_characters WHERE character = '前'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 8, 'M62.27,47.58c1.2,1.2,1.76,2.67,1.76,4.58c0,8.93-0.01,15.1-0.09,18.59c-0.04,1.49-0.09,2.76-0.16,4.05', '[]'::jsonb
FROM writing_characters WHERE character = '前'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 9, 'M78.85,39.25c1.26,1.26,2.01,2.88,2.01,5.02c0,14.56-0.01,42.91-0.01,47.87c0,8.62-5.96,1-7.46-0.25', '[]'::jsonb
FROM writing_characters WHERE character = '前'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO writing_characters (character, char_type, stroke_count, onyomi, kunyomi, han_viet, meaning_vi, meaning_en, romaji, jlpt_level, radicals, is_active, review_status)
VALUES ('南', 'KANJI', 9, ARRAY['なん', 'な'], ARRAY['みなみ'], 'NA, NAM', 'phía nam, phương nam', 'South', '', 'N5', ARRAY['Cross', 'Head', 'Horns', 'Dry'], TRUE, 'PUBLISHED')
ON CONFLICT (character) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 1, 'M27.38,28.25c1.82,0.38,4.2,0.45,7.22,0.2c11.52-0.95,28.37-3.2,40.58-3.7c3.03-0.12,4.92-0.23,6.82,0', '[]'::jsonb
FROM writing_characters WHERE character = '南'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 2, 'M52.35,10.75c0.95,0.95,1.55,2.86,1.55,3.96c0,7.3,0,19.47,0,28.54', '[]'::jsonb
FROM writing_characters WHERE character = '南'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 3, 'M20.25,45.75c1.25,1.25,1.89,2.74,2,5c0.19,4.06,0.83,27.03,1.12,37.99c0.08,3.23,0.13,5.48,0.13,6.01', '[]'::jsonb
FROM writing_characters WHERE character = '南'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 4, 'M22.78,47.6c16.31-1.76,59.32-6.35,60.97-6.35c5,0,6.25,1.62,6.25,6.75c0,5.25-0.25,37.3-0.25,43.05c0,7.7-3.5,6.45-9.5,0.95', '[]'::jsonb
FROM writing_characters WHERE character = '南'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 5, 'M38.5,49.38c2.58,1.74,6.66,7.14,7.31,9.84', '[]'::jsonb
FROM writing_characters WHERE character = '南'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 6, 'M66.25,45.5c0.05,0.89-0.07,1.75-0.37,2.59c-0.8,2.73-2.75,6.92-5.26,9.66', '[]'::jsonb
FROM writing_characters WHERE character = '南'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 7, 'M34.78,61.67c1.81,0.39,4.55,0.55,6.36,0.39c9.11-0.8,18.63-1.54,27.63-2.39c2.99-0.28,4.83-0.32,6.33-0.12', '[]'::jsonb
FROM writing_characters WHERE character = '南'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 8, 'M33.13,74.19c1.81,0.49,4.55,0.83,6.38,0.74c10.73-0.56,22.66-1.94,31.04-2.66c3-0.26,4.82-0.02,6.33,0.23', '[]'::jsonb
FROM writing_characters WHERE character = '南'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 9, 'M53.5,62.5c0.75,0.75,1,2.14,0.99,3.5c-0.04,6.22-0.15,18.46-0.21,25.26c-0.02,2.19-0.03,3.81-0.03,4.49', '[]'::jsonb
FROM writing_characters WHERE character = '南'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO writing_characters (character, char_type, stroke_count, onyomi, kunyomi, han_viet, meaning_vi, meaning_en, romaji, jlpt_level, radicals, is_active, review_status)
VALUES ('後', 'KANJI', 9, ARRAY['ご', 'こう'], ARRAY['のち', 'うし.ろ', 'うしろ', 'あと', 'おく.れる'], 'HẤU, HẬU', 'sau; phía sau', 'Behind, Back, Later', '', 'N5', ARRAY['Loiter', 'Poop', 'Winter'], TRUE, 'PUBLISHED')
ON CONFLICT (character) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 1, 'M34.25,18.38c0,1.3-0.24,2.26-0.93,3.05c-3.57,4.07-8.94,8.7-15.91,13.39', '[]'::jsonb
FROM writing_characters WHERE character = '後'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 2, 'M38.75,36.62c0.14,1.32-0.42,2.67-1.13,3.79c-3.45,5.4-11.43,14.17-22.37,22.71', '[]'::jsonb
FROM writing_characters WHERE character = '後'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 3, 'M28.4,54.36c0.81,0.81,1.38,2.02,1.38,3.28c0,0.68,0.03,25.57-0.07,35.86c-0.02,1.74-0.04,3.05-0.05,3.75', '[]'::jsonb
FROM writing_characters WHERE character = '後'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 4, 'M61.16,12.62c0.29,1.07,0.21,2.43-0.39,3.54c-2.52,4.6-6.1,9.01-9.88,12.93c-1.01,1.05-1.26,2.17,0,2.68c2.96,1.19,6.3,3.11,8.88,5.07', '[]'::jsonb
FROM writing_characters WHERE character = '後'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 5, 'M76.35,20.12c0.27,1.25-0.3,2.56-1,3.31c-7.6,8.19-16.6,16.57-26.61,25.32c-1.25,1.1-0.74,1.76,0.74,1.41C55.72,48.69,74.5,44.4,82.5,43', '[]'::jsonb
FROM writing_characters WHERE character = '後'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 6, 'M77.88,36.25c3.66,2.21,9.46,9.07,10.38,12.5', '[]'::jsonb
FROM writing_characters WHERE character = '後'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 7, 'M57.75,53c0.09,1.13,0.02,2.27-0.4,3.33c-2.2,5.51-7.08,13.22-15.6,20.92', '[]'::jsonb
FROM writing_characters WHERE character = '後'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 8, 'M59.31,59.82c1.17,0.13,2.31,0.02,3.29-0.09c2.65-0.29,9.84-1.39,13.62-2.29c2.59-0.62,3.24,0.68,2.67,2.4C75.25,70.75,59.25,88.88,43.02,96.5', '[]'::jsonb
FROM writing_characters WHERE character = '後'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 9, 'M54.75,67.5c2.52,0,19.5,15.75,31.17,24.23c2.31,1.68,4.74,3.38,7.58,4.01', '[]'::jsonb
FROM writing_characters WHERE character = '後'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO writing_characters (character, char_type, stroke_count, onyomi, kunyomi, han_viet, meaning_vi, meaning_en, romaji, jlpt_level, radicals, is_active, review_status)
VALUES ('食', 'KANJI', 9, ARRAY['しょく', 'じき'], ARRAY['く.う', 'く.らう', 'た.べる', 'は.む'], 'THỰC, TỰ', 'ăn; đồ ăn; lộc', 'Eat, Food', '', 'N5', ARRAY['Eat'], TRUE, 'PUBLISHED')
ON CONFLICT (character) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 1, 'M52.75,10.5c0.11,0.98-0.19,2.67-0.97,3.93C45,25.34,31.75,41.19,14,51.5', '[]'::jsonb
FROM writing_characters WHERE character = '食'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 2, 'M52.75,16.25c5.09,4.8,25.71,19.61,33.7,24.9c2.68,1.78,5.37,2.79,8.55,3.35', '[]'::jsonb
FROM writing_characters WHERE character = '食'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 3, 'M52.25,29.25c1,1,1.5,2.25,1.5,3.5c0,2,0,3,0,5.5', '[]'::jsonb
FROM writing_characters WHERE character = '食'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 4, 'M38,40c0.83,0.47,2.19,1,3.86,0.83c9.39-0.96,21.95-2.76,23.25-2.84c1.67-0.1,3.14,0.88,3.11,2.53C68.2,41.8,67,53.25,66.34,62.4c-0.07,0.94-0.13,1.36-0.13,1.99', '[]'::jsonb
FROM writing_characters WHERE character = '食'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 5, 'M40.83,51.73C47.25,51.25,59.5,50,66,49.75', '[]'::jsonb
FROM writing_characters WHERE character = '食'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 6, 'M40.69,63.9c7.04-0.52,16.55-1.62,24.6-2.04', '[]'::jsonb
FROM writing_characters WHERE character = '食'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 7, 'M38.25,40.25c1.12,1.12,1.5,2.62,1.5,4c0,9.12,0,43.62,0,47.25c0,4,1,4.88,4.12,2.88c2.93-1.87,6.75-5.25,10.88-8.38', '[]'::jsonb
FROM writing_characters WHERE character = '食'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 8, 'M74,64c0.25,1.25,0.09,2.57-0.75,3.5c-3.5,3.88-4.5,4.88-7.25,7.5', '[]'::jsonb
FROM writing_characters WHERE character = '食'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 9, 'M51.5,71C55.75,71,77,90,81,92.75c2.49,1.71,4.62,2.62,7.5,3.5', '[]'::jsonb
FROM writing_characters WHERE character = '食'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO writing_characters (character, char_type, stroke_count, onyomi, kunyomi, han_viet, meaning_vi, meaning_en, romaji, jlpt_level, radicals, is_active, review_status)
VALUES ('校', 'KANJI', 10, ARRAY['こう', 'きょう'], ARRAY[]::TEXT[], 'GIÁO, HIỆU, HÀO', 'kiểm tra, xét; sửa chữa, đính chính; trường học; họ Hiệu', 'Exam, School, Printing, Proof, Correction', '', 'N5', ARRAY['Tree', 'Lid', 'Father'], TRUE, 'PUBLISHED')
ON CONFLICT (character) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 1, 'M11.53,40.68c1.1,0.32,2.6,0.45,4.53,0.32c5.4-0.35,16.57-3,23.14-4.04c1.25-0.2,2.3-0.18,3.07,0', '[]'::jsonb
FROM writing_characters WHERE character = '校'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 2, 'M28.99,17.25c1.07,1.07,1.76,3.25,1.76,5.25c0,0.77-0.03,48.09-0.18,65.25c-0.03,3.03-0.05,5.16-0.07,6', '[]'::jsonb
FROM writing_characters WHERE character = '校'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 3, 'M30.25,40.75c0,1.25-0.49,2.66-0.96,3.77C25.28,53.91,20.88,62.25,15,70', '[]'::jsonb
FROM writing_characters WHERE character = '校'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 4, 'M33.75,51.25c2.75,1.5,6,5.25,7.25,7.75', '[]'::jsonb
FROM writing_characters WHERE character = '校'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 5, 'M66.39,15.5c0.99,0.99,1.38,1.88,1.38,3.62c0,4.25-0.02,7.62-0.08,10.41', '[]'::jsonb
FROM writing_characters WHERE character = '校'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 6, 'M48.12,31.71c2.3,0.29,3.9,0.44,6.09,0.2c10.28-1.16,20.32-2.66,32.45-3.53c2.35-0.17,4.03-0.01,5.33,0.32', '[]'::jsonb
FROM writing_characters WHERE character = '校'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 7, 'M59.24,38.93c0.2,0.53,0.06,2.27-0.4,3.14C57,45.5,53.75,49.5,50,52.5', '[]'::jsonb
FROM writing_characters WHERE character = '校'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 8, 'M79.27,38.5c4.34,3.07,8.73,8.68,10.9,12.41', '[]'::jsonb
FROM writing_characters WHERE character = '校'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 9, 'M79.15,49.18c0.35,1.32,0.17,2.62-0.54,4.18C72.25,67.25,58.75,83.25,44,91.25', '[]'::jsonb
FROM writing_characters WHERE character = '校'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 10, 'M55.95,55.88c6.3,3.37,21.64,22.12,31.45,30.33c2.64,2.21,5.07,4.15,8.6,4.44', '[]'::jsonb
FROM writing_characters WHERE character = '校'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO writing_characters (character, char_type, stroke_count, onyomi, kunyomi, han_viet, meaning_vi, meaning_en, romaji, jlpt_level, radicals, is_active, review_status)
VALUES ('時', 'KANJI', 10, ARRAY['じ'], ARRAY['とき', '-どき'], 'THÌ, THỜI', 'lúc; thời gian', 'Time, Hour', '', 'N5', ARRAY['Sun', 'Temple'], TRUE, 'PUBLISHED')
ON CONFLICT (character) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 1, 'M16,29.84c0.75,0.66,1.21,1.62,1.21,3.07c0,1.18-0.16,30.08-0.21,40.85c-0.01,2.42-0.02,3.95-0.02,4.08', '[]'::jsonb
FROM writing_characters WHERE character = '時'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 2, 'M17.78,30.74c4.65-0.63,16.12-2.07,17.6-2.25c1.52-0.18,3,1.5,2.88,2.57c-0.24,2.17-0.36,24.9-0.35,40.79c0,1.63-0.12,3.35-0.12,4.43', '[]'::jsonb
FROM writing_characters WHERE character = '時'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 3, 'M18.75,52c4.5-0.75,13.5-2.12,18.22-2.35', '[]'::jsonb
FROM writing_characters WHERE character = '時'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 4, 'M17.8,74.52c6.2-0.92,11.45-1.89,18.94-2.7', '[]'::jsonb
FROM writing_characters WHERE character = '時'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 5, 'M51.44,29.03c1.37,0.44,3.63,0.34,5,0.19c9.79-1.09,16.34-2.34,25.62-3.08c2.27-0.18,3.9-0.04,5.04,0.18', '[]'::jsonb
FROM writing_characters WHERE character = '時'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 6, 'M67.59,11.37c0.89,0.9,1.59,2.24,1.59,3.75c0,8.39,0.03,27.02,0.03,27.6', '[]'::jsonb
FROM writing_characters WHERE character = '時'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 7, 'M45.38,45.35c1.49,0.44,4.21,0.59,5.71,0.44c11.29-1.16,25.66-3.29,39.2-3.99c2.48-0.13,3.97,0.21,5.21,0.43', '[]'::jsonb
FROM writing_characters WHERE character = '時'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 8, 'M46,60.99c1.43,0.46,4.04,0.58,5.49,0.46c12.01-1.07,26.89-3.07,39.07-3.89c2.38-0.16,4.41,0.22,5.6,0.44', '[]'::jsonb
FROM writing_characters WHERE character = '時'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 9, 'M78.07,46.08c1.11,1.11,1.66,2.56,1.71,5.06c0.23,12.03-0.09,34.43-0.09,38.52c0,9.83-5.42,2.19-7.66-0.04', '[]'::jsonb
FROM writing_characters WHERE character = '時'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 10, 'M56.75,70.38c2.87,1.76,6.55,6.38,7.27,9.12', '[]'::jsonb
FROM writing_characters WHERE character = '時'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO writing_characters (character, char_type, stroke_count, onyomi, kunyomi, han_viet, meaning_vi, meaning_en, romaji, jlpt_level, radicals, is_active, review_status)
VALUES ('高', 'KANJI', 10, ARRAY['こう'], ARRAY['たか.い', 'たか', '-だか', 'たか.まる', 'たか.める'], 'CAO', 'cao; kiêu, đắt; cao thượng, thanh cao; nhiều, hơn', 'Tall, High, Expensive', '', 'N5', ARRAY['Lid', 'Mouth', 'Mustache'], TRUE, 'PUBLISHED')
ON CONFLICT (character) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 1, 'M52.47,11.75c1.08,1.08,1.48,2.25,1.48,4.22c0,1.53-0.12,4.28-0.12,5.45', '[]'::jsonb
FROM writing_characters WHERE character = '高'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 2, 'M22.9,25.7c2.68,0.3,4.96,0.26,7.47-0.04c14.76-1.78,35.83-4.16,49.3-5.17c2.89-0.22,4.99-0.12,6.81,0.33', '[]'::jsonb
FROM writing_characters WHERE character = '高'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 3, 'M36.25,34.75c1,0.63,1.5,1.5,1.78,2.89c0.72,3.59,1.36,7.37,2.05,11.85c0.2,1.3,0.17,1.82,0.44,3.01', '[]'::jsonb
FROM writing_characters WHERE character = '高'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 4, 'M39.05,36.33c9.95-1.71,23.99-3.65,29.61-4.1c2.96-0.23,3.83,1.02,3.14,3.31c-0.88,2.93-2.17,7.01-3.32,10.2', '[]'::jsonb
FROM writing_characters WHERE character = '高'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 5, 'M41.28,49.94c6.57-0.42,16.36-1.87,25.72-2.71c1.3-0.12,2.59-0.22,3.85-0.31', '[]'::jsonb
FROM writing_characters WHERE character = '高'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 6, 'M21,60.25c1.31,1.31,1.9,2.76,2.21,5c0.79,5.62,2.21,18.19,3.16,26.99c0.15,1.39,0.28,2.67,0.38,3.76', '[]'::jsonb
FROM writing_characters WHERE character = '高'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 7, 'M24.06,64c16.08-1.51,58.63-6.55,60.19-6.75c3.75-0.5,6,1.5,5.25,6.25c-1.49,9.45-2.62,19.62-5.25,28.25c-2.05,6.75-5.38,2.5-7.8,0', '[]'::jsonb
FROM writing_characters WHERE character = '高'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 8, 'M41.5,71.68c0.66,0.66,1.16,1.63,1.31,2.47c0.89,2.82,1.58,7.24,2.39,11.81c0.21,1.16,0.4,1.78,0.56,2.79', '[]'::jsonb
FROM writing_characters WHERE character = '高'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 9, 'M44.02,73.08c6.79-1.36,17.1-2.86,20.92-3.35c1.81-0.23,3.31,1.02,3.13,2.5c-0.35,2.92-1.96,8.31-3.12,11.75', '[]'::jsonb
FROM writing_characters WHERE character = '高'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 10, 'M46.44,87.05c4.61-0.4,11.01-1.31,17.32-1.94c0.88-0.09,1.77-0.17,2.65-0.26', '[]'::jsonb
FROM writing_characters WHERE character = '高'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO writing_characters (character, char_type, stroke_count, onyomi, kunyomi, han_viet, meaning_vi, meaning_en, romaji, jlpt_level, radicals, is_active, review_status)
VALUES ('間', 'KANJI', 12, ARRAY['かん', 'けん'], ARRAY['あいだ', 'ま', 'あい'], 'GIAN, GIÁN, NHÀN', 'khoảng không gian; kẽ hở, lỗ hổng; chia rẽ', 'Interval, Space', '', 'N5', ARRAY['Gate', 'Sun'], TRUE, 'PUBLISHED')
ON CONFLICT (character) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 1, 'M18.64,15.3c0.71,0.71,1.18,1.82,1.18,3.43c0,3.89-0.05,56.65-0.19,72.77c-0.02,1.92-0.03,4.03-0.05,4.65', '[]'::jsonb
FROM writing_characters WHERE character = '間'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 2, 'M21.01,16.81c5.75-0.6,18.73-2.74,20.5-2.84c1.85-0.1,2.86,0.28,2.9,2.02c0.06,2.75-0.5,16.1-0.85,20.76c-0.12,1.55-0.19,2.57-0.19,2.7', '[]'::jsonb
FROM writing_characters WHERE character = '間'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 3, 'M20.95,27.27c5.99-0.61,14.92-2.02,21.88-2.6', '[]'::jsonb
FROM writing_characters WHERE character = '間'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 4, 'M21.02,39.04c8.11-1.19,14.14-2.1,21.31-2.64', '[]'::jsonb
FROM writing_characters WHERE character = '間'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 5, 'M63.19,13.1c0.98,0.98,1.34,2.15,1.34,2.97c0,5.8-0.08,12.65-0.06,18.93c0.01,2.01,0.02,3.4,0.06,3.58', '[]'::jsonb
FROM writing_characters WHERE character = '間'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 6, 'M65.32,14.77c5.97-0.68,20.69-3.19,22.38-3.28c1.8-0.09,2.81,0.88,2.81,2.82c0,17-0.22,66.12-0.22,78.44c0,10.5-6.35,1.36-7.72,0.23', '[]'::jsonb
FROM writing_characters WHERE character = '間'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 7, 'M65.63,24.79c4.49-0.42,19.73-1.99,23.35-1.99', '[]'::jsonb
FROM writing_characters WHERE character = '間'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 8, 'M65.22,36.07c6.41-0.32,16.53-1.32,23.49-1.81', '[]'::jsonb
FROM writing_characters WHERE character = '間'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 9, 'M40.56,50.95c0.74,0.74,1.04,1.93,1.04,2.99c0,0.83-0.08,20.84-0.05,29.06c0.01,2.25,0.02,2.77,0.05,3', '[]'::jsonb
FROM writing_characters WHERE character = '間'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 10, 'M42.26,52.09c5.56-0.48,19.71-1.98,21.3-2.1c1.68-0.13,2.76,1.46,2.63,2.24c-0.21,1.24-0.41,20.66-0.48,29.02c-0.02,2.29-0.03,3.8-0.03,3.97', '[]'::jsonb
FROM writing_characters WHERE character = '間'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 11, 'M42.64,66.6c5.11-0.48,17.36-1.73,21.88-2.07', '[]'::jsonb
FROM writing_characters WHERE character = '間'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 12, 'M42.25,82.8c5.13-0.3,17-1.55,22.26-2.05', '[]'::jsonb
FROM writing_characters WHERE character = '間'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO writing_characters (character, char_type, stroke_count, onyomi, kunyomi, han_viet, meaning_vi, meaning_en, romaji, jlpt_level, radicals, is_active, review_status)
VALUES ('話', 'KANJI', 13, ARRAY['わ'], ARRAY['はな.す', 'はなし'], 'THOẠI', 'nói', 'Tale, Talk', '', 'N5', ARRAY['Say', 'Tongue'], TRUE, 'PUBLISHED')
ON CONFLICT (character) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 1, 'M24.99,14c2.36,1.5,6.1,6.17,6.7,8.5', '[]'::jsonb
FROM writing_characters WHERE character = '話'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 2, 'M11.37,32.83c1.41,0.42,3.07,0.29,4.51,0.17c8.29-0.7,16.95-1.9,23.59-2.81c1.28-0.17,3.22,0.11,3.87,0.23', '[]'::jsonb
FROM writing_characters WHERE character = '話'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 3, 'M17.78,47.06c1.05,0.32,2.15,0.35,3.23,0.29c4.11-0.23,10.69-1.59,14.43-1.97c1.31-0.13,2.68-0.13,4.04-0.13', '[]'::jsonb
FROM writing_characters WHERE character = '話'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 4, 'M18.58,59.33c1.2,0.36,2.8,0.13,4.04,0.08c3.71-0.16,8.38-0.79,12.92-1.36c1.43-0.18,2.85-0.34,4.3-0.08', '[]'::jsonb
FROM writing_characters WHERE character = '話'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 5, 'M17.4,71.9c0.81,0.68,1.33,1.82,1.49,2.87c0.73,4.61,1.4,8.31,2.2,13.18c0.22,1.33,0.43,2.62,0.63,3.8', '[]'::jsonb
FROM writing_characters WHERE character = '話'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 6, 'M19.64,73.54c6.67-1.14,13.31-2.75,19.46-3.66c1.85-0.27,2.96,1.26,2.7,2.51c-0.89,4.19-2.46,8.16-4.05,14.07', '[]'::jsonb
FROM writing_characters WHERE character = '話'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 7, 'M22.49,89.55c4.76-0.55,8.86-1.17,14.02-1.72c0.93-0.1,1.91,0.04,2.97-0.09', '[]'::jsonb
FROM writing_characters WHERE character = '話'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 8, 'M81.75,13.75c-0.12,1.25-0.79,2.39-1.66,3.06c-4.84,3.69-15.34,9.94-29.34,14.94', '[]'::jsonb
FROM writing_characters WHERE character = '話'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 9, 'M45.39,45.8c1.55,0.34,4.36,0.62,6.73,0.34C62.8,44.9,76.37,43.07,91,42.23c2.28-0.13,4.49-0.01,6.75,0.29', '[]'::jsonb
FROM writing_characters WHERE character = '話'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 10, 'M68.87,27.19c1.4,1.4,1.85,3.06,1.85,4.88c0,1.44-0.21,28.18-0.21,35.16', '[]'::jsonb
FROM writing_characters WHERE character = '話'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 11, 'M52.07,67.83c0.93,1.17,1.22,1.76,1.35,2.34c1.38,6.2,2.13,12.33,3.08,20.33c0.13,1.14,0.27,2.31,0.42,3.53', '[]'::jsonb
FROM writing_characters WHERE character = '話'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 12, 'M54.62,69.29c12.9-1.37,25.41-2.73,32.68-3.31c2.94-0.24,3.79,2.4,3.37,3.8c-1.47,4.87-3.03,11.64-5.04,18.35', '[]'::jsonb
FROM writing_characters WHERE character = '話'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 13, 'M56.64,91.74c7.5-0.57,16.99-1.13,27.14-1.93c1.37-0.11,2.75-0.22,4.13-0.34', '[]'::jsonb
FROM writing_characters WHERE character = '話'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO writing_characters (character, char_type, stroke_count, onyomi, kunyomi, han_viet, meaning_vi, meaning_en, romaji, jlpt_level, radicals, is_active, review_status)
VALUES ('電', 'KANJI', 13, ARRAY['でん'], ARRAY[]::TEXT[], 'ĐIỆN', 'điện; chớp', 'Electricity', '', 'N5', ARRAY['Rain', 'Rice Paddy', 'Umbrella'], TRUE, 'PUBLISHED')
ON CONFLICT (character) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 1, 'M34.66,16.19c2.31,0.7,4.78,0.25,7.11-0.04c6.03-0.75,16.15-1.97,23.49-2.76c2.19-0.23,4.03-0.14,6.05,0.1', '[]'::jsonb
FROM writing_characters WHERE character = '電'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 2, 'M19.51,29.11c-0.2,5.55-1.93,11.7-3.21,17.3', '[]'::jsonb
FROM writing_characters WHERE character = '電'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 3, 'M20.85,32.18C37.62,30.38,70,25.62,86.4,25.2c9.1-0.24,2.35,6.05-0.78,9.53', '[]'::jsonb
FROM writing_characters WHERE character = '電'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 4, 'M50.92,17.26c1.1,1.1,1.83,2.49,1.83,3.76c0,3.98-0.16,17.08-0.23,23.73c-0.02,2.1-0.04,3.56-0.04,3.89', '[]'::jsonb
FROM writing_characters WHERE character = '電'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 5, 'M32.75,36.46c3.42,0.53,8.09,2.12,9.96,3', '[]'::jsonb
FROM writing_characters WHERE character = '電'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 6, 'M31.25,45c3.16,0.57,8.05,2.68,9.77,3.64', '[]'::jsonb
FROM writing_characters WHERE character = '電'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 7, 'M65.5,33.47c3.66,0.79,7.81,2.37,9.28,3.03', '[]'::jsonb
FROM writing_characters WHERE character = '電'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 8, 'M65.54,41.89c2.82,0.77,6.67,3.06,8.21,4.34', '[]'::jsonb
FROM writing_characters WHERE character = '電'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 9, 'M25.86,55.87c0.89,1.04,1.07,1.52,1.21,2.88c0.47,4.62,2.55,21.84,2.55,22.22c0,0.43,0.19,1.93,0.37,2.79', '[]'::jsonb
FROM writing_characters WHERE character = '電'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 10, 'M28.04,57.11c10.79-1,41.49-4.23,43.85-4.37c4.11-0.24,5.36,1.76,4.73,4.82c-0.27,1.31-2.37,9.94-4.05,17.74c-0.2,0.95-0.69,2.39-0.69,3.04', '[]'::jsonb
FROM writing_characters WHERE character = '電'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 11, 'M29.46,68.34c7.42-0.97,36.17-3.47,44.14-3.8', '[]'::jsonb
FROM writing_characters WHERE character = '電'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 12, 'M31.02,80.34C42.25,79,59.5,77.75,71.25,76.75', '[]'::jsonb
FROM writing_characters WHERE character = '電'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 13, 'M48.42,57.62c1.08,1.13,1.45,2.8,1.45,4.78c0,4.85-0.1,12.75-0.1,18.85c0,12.75,1.48,14.5,20.5,14.5c19.23,0,20.18-2.75,20.18-10.82', '[]'::jsonb
FROM writing_characters WHERE character = '電'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO writing_characters (character, char_type, stroke_count, onyomi, kunyomi, han_viet, meaning_vi, meaning_en, romaji, jlpt_level, radicals, is_active, review_status)
VALUES ('聞', 'KANJI', 14, ARRAY['ぶん', 'もん'], ARRAY['き.く', 'き.こえる'], 'VĂN, VẤN, VẶN', 'nghe; tiếng động tới, tiếng truyền tới', 'Hear, Ask, Listen', '', 'N5', ARRAY['Gate', 'Ear'], TRUE, 'PUBLISHED')
ON CONFLICT (character) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 1, 'M16.14,17.97c0.94,0.94,1.51,2.16,1.51,3.25c0,0.77-0.03,48.45-0.18,66.29c-0.03,3.16-0.05,5.37-0.07,6.21', '[]'::jsonb
FROM writing_characters WHERE character = '聞'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 2, 'M19.01,19.6c6.86-1.22,18.49-3.1,21.08-3.39c1.9-0.21,3.03,0.79,3,2.46c-0.04,1.84-0.59,10.46-1.44,20.02c-0.09,1.04-0.15,2-0.15,2.69', '[]'::jsonb
FROM writing_characters WHERE character = '聞'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 3, 'M18.81,30.43c6.94-1.05,15.82-2.55,22.41-2.9', '[]'::jsonb
FROM writing_characters WHERE character = '聞'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 4, 'M17.86,42.57C26.25,41.25,33,40,40.42,39.4', '[]'::jsonb
FROM writing_characters WHERE character = '聞'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 5, 'M66.21,14.22c0.66,0.66,1.17,1.78,1.17,2.93c0,0.56,0.12,13.19,0.17,19.1c0.02,1.71,0.03,2.83,0.03,2.91', '[]'::jsonb
FROM writing_characters WHERE character = '聞'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 6, 'M68.51,16.1c6.89-1.1,19.28-3.26,21.17-3.36c1.96-0.1,3.57,1.38,3.57,2.98c0,18.78-0.26,60.28-0.26,73.89c0,11.13-6.37,2.13-8.21,0.25', '[]'::jsonb
FROM writing_characters WHERE character = '聞'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 7, 'M68.59,25.94c5.16-0.69,18.16-2.44,23-2.71', '[]'::jsonb
FROM writing_characters WHERE character = '聞'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 8, 'M69.13,36.75c6.37-0.75,15.12-2,22.15-2.53', '[]'::jsonb
FROM writing_characters WHERE character = '聞'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 9, 'M33.5,51.24c1.71,0.31,4.02,0.25,5.71,0.06c9.6-1.05,20.21-3.05,30.66-4.05c2.83-0.27,4.57-0.1,6,0.05', '[]'::jsonb
FROM writing_characters WHERE character = '聞'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 10, 'M42.71,53c0.89,0.89,1.3,2.26,1.3,3.51S44,79.31,44,83.84', '[]'::jsonb
FROM writing_characters WHERE character = '聞'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 11, 'M45.13,61.43c4.49-0.43,13.74-2.05,19.78-2.36', '[]'::jsonb
FROM writing_characters WHERE character = '聞'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 12, 'M44.81,71.67c5.72-0.67,12.31-1.92,20.12-2.95', '[]'::jsonb
FROM writing_characters WHERE character = '聞'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 13, 'M32.51,85.14c0.99,0.86,2.2,1.23,3.25,0.92c4.87-1.43,28.2-7.83,34.76-9.39', '[]'::jsonb
FROM writing_characters WHERE character = '聞'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 14, 'M64.84,49.66c0.62,0.63,1.05,1.71,1.05,2.99c0,0.66,0.12,27.55,0.15,39.1c0.01,1.88,0.01,3.36,0.01,4.25', '[]'::jsonb
FROM writing_characters WHERE character = '聞'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO writing_characters (character, char_type, stroke_count, onyomi, kunyomi, han_viet, meaning_vi, meaning_en, romaji, jlpt_level, radicals, is_active, review_status)
VALUES ('語', 'KANJI', 14, ARRAY['ご'], ARRAY['かた.る', 'かた.らう'], 'NGỨ, NGỮ, NGỰ', 'ngôn ngữ; lời lẽ', 'Word, Speech, Language', '', 'N5', ARRAY['Say', 'Five', 'Mouth'], TRUE, 'PUBLISHED')
ON CONFLICT (character) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 1, 'M26,15.25c2.82,1.41,7.29,5.8,8,8', '[]'::jsonb
FROM writing_characters WHERE character = '語'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 2, 'M12.37,32.97c1.25,0.28,2.88,0.66,4.36,0.53c7.02-0.59,17.78-1.75,25.95-3c1.52-0.23,3.57-0.38,5.16,0.03', '[]'::jsonb
FROM writing_characters WHERE character = '語'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 3, 'M18.73,45.76c0.38,0.18,2.71,0.2,3.1,0.18c3.97-0.21,9.79-1.19,14.46-2.31c1.67-0.4,2.71-0.38,3.86-0.08', '[]'::jsonb
FROM writing_characters WHERE character = '語'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 4, 'M18.73,58.89c0.89,0.23,1.89,0.36,3.35,0.15c3.89-0.54,10.71-1.51,14.85-2.29c0.7-0.13,1.82-0.26,2.61-0.1', '[]'::jsonb
FROM writing_characters WHERE character = '語'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 5, 'M17.14,71.9c0.63,0.62,1.12,1.65,1.23,2.57c0.63,5.03,1.51,10.28,2.23,15.59c0.14,1.03,0.27,2.02,0.41,2.93', '[]'::jsonb
FROM writing_characters WHERE character = '語'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 6, 'M19.37,73.6c5.67-0.94,15.47-2.73,20.36-3.48c1.49-0.22,2.39,1.05,2.18,2.08c-0.71,3.44-2.27,9.75-3.23,13.89', '[]'::jsonb
FROM writing_characters WHERE character = '語'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 7, 'M21.47,89.02c3.95-0.45,10.71-1.19,16.28-1.61c1.21-0.09,2.36-0.17,3.41-0.22', '[]'::jsonb
FROM writing_characters WHERE character = '語'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 8, 'M51.79,17.49c1.38,0.26,3.91,0.28,5.27,0.15C63.88,17,72.62,15.62,80,15.32c2.3-0.1,3.67,0.04,4.81,0.15', '[]'::jsonb
FROM writing_characters WHERE character = '語'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 9, 'M67.75,20.25c0.37,1.25,0.5,2.38,0.23,3.75c-0.75,3.78-6.03,23.83-7.96,31.58', '[]'::jsonb
FROM writing_characters WHERE character = '語'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 10, 'M52.18,36.96c1.82,0.66,4.17,0.95,5.84,0.66c8.48-1.5,16.13-3.06,22.74-4.1c2.49-0.39,4.05,1.27,3.71,2.93c-0.6,2.93-2.48,11.43-3.74,17.86', '[]'::jsonb
FROM writing_characters WHERE character = '語'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 11, 'M46.33,58.46c1.13,0.24,3.94,0.2,5.07,0.08c12.34-1.29,19.11-2.39,40.88-4.02c1.88-0.14,3.75-0.02,4.69,0.09', '[]'::jsonb
FROM writing_characters WHERE character = '語'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 12, 'M52.5,69.88c0.93,0.93,1.42,2.28,1.54,3.31c0.71,6.06,1.42,12.65,2.06,19.3c0.15,1.5,0.28,2.44,0.4,3.75', '[]'::jsonb
FROM writing_characters WHERE character = '語'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 13, 'M54.99,71.67c9.47-1.45,23.75-3.41,28.85-3.9c2.14-0.21,3.28,0.98,2.86,2.93c-0.84,3.88-3.08,12.57-4.39,17.58', '[]'::jsonb
FROM writing_characters WHERE character = '語'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 14, 'M57.2,91.49c5.94-0.55,14.67-1.24,23.54-1.76c1.3-0.08,2.63-0.13,3.97-0.2', '[]'::jsonb
FROM writing_characters WHERE character = '語'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO writing_characters (character, char_type, stroke_count, onyomi, kunyomi, han_viet, meaning_vi, meaning_en, romaji, jlpt_level, radicals, is_active, review_status)
VALUES ('読', 'KANJI', 14, ARRAY['どく', 'とく', 'とう'], ARRAY['よ.む', '-よ.み'], 'ĐẬU, ĐỘC', '', 'Read', '', 'N5', ARRAY['Say', 'Sell'], TRUE, 'PUBLISHED')
ON CONFLICT (character) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 1, 'M22.38,14.75c2.25,1.63,5.81,6.71,6.37,9.25', '[]'::jsonb
FROM writing_characters WHERE character = '読'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 2, 'M10.37,33.08c1.61,0.48,3.62,0.35,5.27,0.14c5.96-0.76,13.52-1.42,20.1-2.38c1.5-0.22,3.09-0.43,4.6-0.16', '[]'::jsonb
FROM writing_characters WHERE character = '読'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 3, 'M16.23,46.31c1.17,0.37,2.73,0.18,3.93-0.01c3.99-0.62,8.33-1.2,11.58-1.97c1.35-0.32,3.26-0.58,4.65-0.58', '[]'::jsonb
FROM writing_characters WHERE character = '読'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 4, 'M16.73,58.58c1.02,0.35,2.46,0.15,3.53,0.04c3.8-0.4,9.57-1.17,12.55-1.77c1.45-0.29,2.94-0.48,4.22-0.14', '[]'::jsonb
FROM writing_characters WHERE character = '読'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 5, 'M15.64,70.4c0.71,0.61,1.08,1.37,1.12,2.29c0.79,3.76,1.71,9.85,2.52,15.05c0.16,1.05,0.32,2.06,0.48,3', '[]'::jsonb
FROM writing_characters WHERE character = '読'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 6, 'M17.75,72.05c6.09-0.91,11.59-1.7,17.42-2.67c1.7-0.28,2.73,1.3,2.49,2.58c-0.85,4.46-1.61,6.91-2.88,12.78', '[]'::jsonb
FROM writing_characters WHERE character = '読'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 7, 'M20.47,88.3c4.06-0.46,7.76-1.19,12.79-1.92c0.92-0.13,1.88-0.26,2.9-0.39', '[]'::jsonb
FROM writing_characters WHERE character = '読'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 8, 'M46.37,26.71c2.31,0.59,4.67,0.42,7,0.15c9.97-1.14,21.82-2.23,29.77-3.06c2.36-0.25,4.38-0.21,6.71,0.14', '[]'::jsonb
FROM writing_characters WHERE character = '読'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 9, 'M65.76,13.25c1.06,1.06,1.59,2.08,1.59,3.25c0,8.5-0.07,17.03-0.19,20.46', '[]'::jsonb
FROM writing_characters WHERE character = '読'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 10, 'M52.53,38.58c2.22,0.42,4.15,0.3,5.98,0.08C64,38,71.21,37.1,78,36.47c1.61-0.15,3.63-0.29,5.23,0.04', '[]'::jsonb
FROM writing_characters WHERE character = '読'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 11, 'M46.14,48.1c-0.11,3.93-1.7,12-2.6,14.35', '[]'::jsonb
FROM writing_characters WHERE character = '読'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 12, 'M47,49.96c11.42-1.27,28-3.71,41.35-4.28c9.15-0.39,0.43,7.14-0.74,8.35', '[]'::jsonb
FROM writing_characters WHERE character = '読'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 13, 'M59.49,59.25c0.5,1.52,0.71,3.09,0.29,4.83c-2.76,11.61-6.54,22.94-15.26,30.61', '[]'::jsonb
FROM writing_characters WHERE character = '読'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 14, 'M71.64,56.26c1.11,1.24,1.65,2.67,1.69,4.37c0.11,4.62-0.13,16.99-0.13,24.62c0,9.5,0.8,10.52,11.3,10.52c11,0,11.38-1.02,11.38-7.31', '[]'::jsonb
FROM writing_characters WHERE character = '読'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO writing_characters (character, char_type, stroke_count, onyomi, kunyomi, han_viet, meaning_vi, meaning_en, romaji, jlpt_level, radicals, is_active, review_status)
VALUES ('生', 'KANJI', 5, ARRAY['せい', 'しょう'], ARRAY['い.きる', 'い.かす', 'い.ける', 'う.まれる', 'うま.れる', 'う.まれ', 'うまれ', 'う.む', 'お.う', 'は.える', 'は.やす', 'き', 'なま', 'なま-', 'な.る', 'な.す', 'む.す', '-う'], 'SANH, SINH', 'sinh đẻ; sống', 'Life, Genuine, Birth', '', 'N5', ARRAY['Life'], TRUE, 'PUBLISHED')
ON CONFLICT (character) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 1, 'M31.26,25.89c0.36,1.36,0.35,2.65-0.05,3.79c-2.34,6.69-7.24,17.22-14.96,24.19', '[]'::jsonb
FROM writing_characters WHERE character = '生'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 2, 'M31.13,40.67c2.37,0.33,4.03,0.07,5.64-0.12c9.5-1.1,25.15-4.12,35.35-5.83c2.51-0.42,4.86-0.73,7.38-0.33', '[]'::jsonb
FROM writing_characters WHERE character = '生'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 3, 'M52.31,12.63c1.28,1.28,2.01,3.12,2.01,5.23c0,4.01,0,65.14,0,69.77', '[]'::jsonb
FROM writing_characters WHERE character = '生'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 4, 'M29.38,64.03c2.64,0.67,5.38,0.31,8.04-0.02C49.45,62.51,62.16,61,72.5,59.86c2.38-0.26,4.99-0.76,7.38-0.23', '[]'::jsonb
FROM writing_characters WHERE character = '生'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 5, 'M15.75,90.25c3.04,0.75,6.21,0.94,8.4,0.8C40.62,90,68.12,86.5,83.3,85.75c3.63-0.18,7.68,0,10.07,0.73', '[]'::jsonb
FROM writing_characters WHERE character = '生'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO writing_characters (character, char_type, stroke_count, onyomi, kunyomi, han_viet, meaning_vi, meaning_en, romaji, jlpt_level, radicals, is_active, review_status)
VALUES ('書', 'KANJI', 10, ARRAY['しょ'], ARRAY['か.く', '-が.き', '-がき'], 'THƯ', 'sách; thư tín', 'Write', '', 'N5', ARRAY['Brush', 'Sun'], TRUE, 'PUBLISHED')
ON CONFLICT (character) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 1, 'M30.74,20.63c2.01,0.49,3.84,0.58,5.91,0.39c9.45-0.89,28.54-2.97,37.54-3.64c2.92-0.22,4.18,1.24,3.66,3.55c-0.4,1.76-2.56,9.62-3.88,16.56', '[]'::jsonb
FROM writing_characters WHERE character = '書'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 2, 'M11.89,32.34c3.17,0.66,5.87,0.47,9.58,0.19c20.16-1.52,48.41-3.9,67.91-4.64c4.08-0.16,7.07,0.26,8.91,0.59', '[]'::jsonb
FROM writing_characters WHERE character = '書'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 3, 'M29.86,41.38c1.64,0.49,3.39,0.52,4.84,0.44c9.67-0.57,27.55-2.07,37.43-2.85c1.93-0.15,3.14-0.08,4.59,0.07', '[]'::jsonb
FROM writing_characters WHERE character = '書'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 4, 'M30.04,52.06c1.47,0.26,3.65,0.54,5.12,0.43c12.34-0.98,25.59-2.23,36.78-3.31c2.43-0.23,4.41-0.19,5.63-0.07', '[]'::jsonb
FROM writing_characters WHERE character = '書'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 5, 'M17,63.44c2,0.58,5.67,0.57,7.66,0.41c20.78-1.7,42.92-3.93,61.09-4.48c3.33-0.1,5.33,0.15,6.99,0.42', '[]'::jsonb
FROM writing_characters WHERE character = '書'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 6, 'M52.69,9.52c1.33,1.33,1.95,2.98,1.95,4.71c0,5.67,0.22,33.72,0.31,45.77', '[]'::jsonb
FROM writing_characters WHERE character = '書'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 7, 'M31.25,71.75c0.43,0.46,1.24,1.44,1.43,2.7c1.11,7.42,2.22,14.33,3.18,20.74c0.18,1.2,0.34,2.38,0.49,3.57', '[]'::jsonb
FROM writing_characters WHERE character = '書'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 8, 'M33.75,73.75c14.98-2.05,36.1-3.71,44.41-4.26c3.33-0.22,5.09,1.76,4.6,4.33c-0.99,5.22-2.01,11.55-3.81,19.41c-0.3,1.31-0.74,2.55-1.15,3.72', '[]'::jsonb
FROM writing_characters WHERE character = '書'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 9, 'M35.5,83.75C46.79,83.03,65.75,81.5,80.25,81', '[]'::jsonb
FROM writing_characters WHERE character = '書'
ON CONFLICT (character_id, stroke_number) DO NOTHING;
INSERT INTO character_strokes (character_id, stroke_number, svg_path_data, sample_points_json)
SELECT id, 10, 'M37.25,95.25c10.7-0.68,26.5-1.38,40.5-2', '[]'::jsonb
FROM writing_characters WHERE character = '書'
ON CONFLICT (character_id, stroke_number) DO NOTHING;

-- -----------------------------------------------------------------------------
-- VOCABULARY (N5)
-- -----------------------------------------------------------------------------
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('東', 'ひがし', 1, 'đông; Hướng Đông', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397105/elearningJP/audio/vocab/N5/vocab_n5_1_%E3%81%B2%E3%81%8C%E3%81%97.mp3', TRUE, '', '', 'higashi', 'ĐÔNG', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('私', 'わたくし', 1, 'tôi (trang trọng, khiêm nhường)', '', 'N5', 'ADJECTIVE', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397108/elearningJP/audio/vocab/N5/vocab_n5_2_%E3%82%8F%E3%81%9F%E3%81%8F%E3%81%97.mp3', TRUE, '', '', 'watakushi', 'TƯ', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('一日', 'いちにち', 1, 'một ngày', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397111/elearningJP/audio/vocab/N5/vocab_n5_3_%E3%81%84%E3%81%A1%E3%81%AB%E3%81%A1.mp3', TRUE, '', '', 'ichinichi', 'NHẤT NHẬT', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('一人', 'ひとり', 1, 'một người', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397112/elearningJP/audio/vocab/N5/vocab_n5_4_%E3%81%B2%E3%81%A8%E3%82%8A.mp3', TRUE, '', '', 'hitori', 'NHẤT NHÂN', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('暇', 'ひま', 1, 'thời gian rảnh rỗi; thì giờ nhàn hạ; sự nghỉ ngơi; sự cáo từ; sự từ giã', '', 'N5', 'ADJECTIVE', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397114/elearningJP/audio/vocab/N5/vocab_n5_5_%E3%81%B2%E3%81%BE.mp3', TRUE, '', '', 'hima', 'HẠ', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('入口', 'いりぐち', 1, 'cổng vào; cửa vào; lối vào; sự bắt đầu', '', 'N5', 'ADJECTIVE', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397207/elearningJP/audio/vocab/N5/vocab_n5_6_%E3%81%84%E3%82%8A%E3%81%90%E3%81%A1.mp3', TRUE, '', '', 'iriguchi', 'NHẬP KHẨU', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('入る', 'はいる', 1, 'đi vào; vào', '', 'N5', 'VERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397209/elearningJP/audio/vocab/N5/vocab_n5_7_%E3%81%AF%E3%81%84%E3%82%8B.mp3', TRUE, '', '', 'hairu', 'NHẬP', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('甘い', 'あまい', 1, 'ngon ngọt; ngọt; ngọt bùi', '', 'N5', 'ADJECTIVE', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397211/elearningJP/audio/vocab/N5/vocab_n5_8_%E3%81%82%E3%81%BE%E3%81%84.mp3', TRUE, '', '', 'amai', 'CAM', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('上手', 'じょうず', 1, 'lời tâng bốc; lời nịnh nọt; giỏi; cừ; túm lấy khố của đối thủ từ vị trí tay đè trên tay đối thủ', '', 'N5', 'ADJECTIVE', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397212/elearningJP/audio/vocab/N5/vocab_n5_9_%E3%81%98%E3%82%87%E3%81%86%E3%81%9A.mp3', TRUE, '', '', 'jouzu', 'THƯỢNG THỦ', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('塩', 'しお', 1, 'muối', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397214/elearningJP/audio/vocab/N5/vocab_n5_10_%E3%81%97%E3%81%8A.mp3', TRUE, '', '', 'shio', 'DIÊM', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('黄色', 'きいろ', 1, 'màu vàng; vàng', '', 'N5', 'ADJECTIVE', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397215/elearningJP/audio/vocab/N5/vocab_n5_11_%E3%81%8D%E3%81%84%E3%82%8D.mp3', TRUE, '', '', 'kiiro', 'HOÀNG SẮC', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('弟', 'おとうと', 1, 'bào đệ; em; em trai', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397217/elearningJP/audio/vocab/N5/vocab_n5_12_%E3%81%8A%E3%81%A8%E3%81%86%E3%81%A8.mp3', TRUE, '', '', 'otouto', 'ĐỆ', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('門', 'もん', 1, 'cổng; một trong những giai đoạn phân loại sinh học', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397218/elearningJP/audio/vocab/N5/vocab_n5_13_%E3%82%82%E3%82%93.mp3', TRUE, '', '', 'mon', 'MÔN', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('眼鏡', 'めがね', 1, 'kính (đeo mắt)', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397220/elearningJP/audio/vocab/N5/vocab_n5_14_%E3%82%81%E3%81%8C%E3%81%AD.mp3', TRUE, '', '', 'megane', 'NHÃN KÍNH', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('来る', 'くる', 1, 'đến', '', 'N5', 'VERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397221/elearningJP/audio/vocab/N5/vocab_n5_15_%E3%81%8F%E3%82%8B.mp3', TRUE, '', '', 'kuru', 'LAI', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('戸', 'と', 1, 'cánh cửa; cửa', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397223/elearningJP/audio/vocab/N5/vocab_n5_16_%E3%81%A8.mp3', TRUE, '', '', 'to', 'HỘ', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('次', 'つぎ', 1, 'lần sau; sau đây; tiếp đến', '', 'N5', 'ADJECTIVE', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397224/elearningJP/audio/vocab/N5/vocab_n5_17_%E3%81%A4%E3%81%8E.mp3', TRUE, '', '', 'tsugi', 'THỨ', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('丈夫', 'じょうぶ', 1, 'sự bền; sự vững chắc; sức bền; sự dai sức; chắc; khoẻ; cứng; bền; độ bền', '', 'N5', 'ADJECTIVE', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397225/elearningJP/audio/vocab/N5/vocab_n5_18_%E3%81%98%E3%82%87%E3%81%86%E3%81%B6.mp3', TRUE, '', '', 'joubu', 'TRƯỢNG PHU', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('人', 'ひと', 1, 'người', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397227/elearningJP/audio/vocab/N5/vocab_n5_19_%E3%81%B2%E3%81%A8.mp3', TRUE, '', '', 'hito', 'NHÂN', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('前', 'まえ', 1, 'tiền; trước; kém; trước đây; cũ; người hay việc cũ đã nói ở trên; trước khi; 前年:năm trước', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397228/elearningJP/audio/vocab/N5/vocab_n5_20_%E3%81%BE%E3%81%88.mp3', TRUE, '', '', 'mae', 'TIỀN', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('誰', 'だれ', 1, 'ai', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397229/elearningJP/audio/vocab/N5/vocab_n5_21_%E3%81%A0%E3%82%8C.mp3', TRUE, '', '', 'dare', 'THÙY', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('体', 'からだ', 1, 'cơ thể; sức khoẻ; thân thể; khối', '', 'N5', 'ADJECTIVE', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397231/elearningJP/audio/vocab/N5/vocab_n5_22_%E3%81%8B%E3%82%89%E3%81%A0.mp3', TRUE, '', '', 'karada', 'THỂ', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('店', 'みせ', 1, 'cửa hàng; cửa hiệu; sự thành lập', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397232/elearningJP/audio/vocab/N5/vocab_n5_23_%E3%81%BF%E3%81%9B.mp3', TRUE, '', '', 'mise', 'ĐIẾM', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('止まる', 'とまる', 1, 'dừng lại; giữ lại; ở lại', '', 'N5', 'VERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397234/elearningJP/audio/vocab/N5/vocab_n5_24_%E3%81%A8%E3%81%BE%E3%82%8B.mp3', TRUE, '', '', 'tomaru', 'CHỈ', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('七日', 'なのか', 1, '7 ngày; ngày thứ 7 của tháng', '', 'N5', 'ADVERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397235/elearningJP/audio/vocab/N5/vocab_n5_25_%E3%81%AA%E3%81%AE%E3%81%8B.mp3', TRUE, '', '', 'nanoka', 'THẤT NHẬT', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('南', 'みなみ', 1, 'nam; phía Nam; phương Nam', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397236/elearningJP/audio/vocab/N5/vocab_n5_26_%E3%81%BF%E3%81%AA%E3%81%BF.mp3', TRUE, '', '', 'minami', 'NAM', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('悪い', 'わるい', 1, 'còm; xấu; không tốt; ngu ngốc', '', 'N5', 'ADJECTIVE', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397238/elearningJP/audio/vocab/N5/vocab_n5_27_%E3%82%8F%E3%82%8B%E3%81%84.mp3', TRUE, '', '', 'warui', 'ÁC', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('二人', 'ふたり', 1, 'Hai người', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397239/elearningJP/audio/vocab/N5/vocab_n5_28_%E3%81%B5%E3%81%9F%E3%82%8A.mp3', TRUE, '', '', 'futari', 'NHỊ NHÂN', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('灰皿', 'はいざら', 1, 'gạt tàn', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397240/elearningJP/audio/vocab/N5/vocab_n5_29_%E3%81%AF%E3%81%84%E3%81%96%E3%82%89.mp3', TRUE, '', '', 'haizara', 'HÔI MÃNH', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('弾く', 'ひく', 1, 'gảy, búng; không thấm nước, chống nước; không chấp nhận những thứ không phù hợp với điều kiện', '', 'N5', 'VERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397242/elearningJP/audio/vocab/N5/vocab_n5_30_%E3%81%B2%E3%81%8F.mp3', TRUE, '', '', 'hiku', 'ĐÀN', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('始め', 'はじめ', 1, 'lúc đầu; đầu tiên; 開始', '', 'N5', 'ADVERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397243/elearningJP/audio/vocab/N5/vocab_n5_31_%E3%81%AF%E3%81%98%E3%82%81.mp3', TRUE, '', '', 'hajime', 'THỦY', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('万', 'まん', 1, 'mười nghìn; 1 vạn', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397245/elearningJP/audio/vocab/N5/vocab_n5_32_%E3%81%BE%E3%82%93.mp3', TRUE, '', '', 'man', 'VẠN', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('三', 'さん', 1, 'ba', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397246/elearningJP/audio/vocab/N5/vocab_n5_33_%E3%81%95%E3%82%93.mp3', TRUE, '', '', 'san', 'TAM', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('六', 'ろく', 1, 'số sáu', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397247/elearningJP/audio/vocab/N5/vocab_n5_34_%E3%82%8D%E3%81%8F.mp3', TRUE, '', '', 'roku', 'LỤC', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('一昨日', 'おととい', 1, 'hôm kia', '', 'N5', 'ADVERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397249/elearningJP/audio/vocab/N5/vocab_n5_35_%E3%81%8A%E3%81%A8%E3%81%A8%E3%81%84.mp3', TRUE, '', '', 'ototoi', 'NHẤT TẠC NHẬT', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('一昨年', 'おととし', 1, 'năm kia', '', 'N5', 'ADVERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397250/elearningJP/audio/vocab/N5/vocab_n5_36_%E3%81%8A%E3%81%A8%E3%81%A8%E3%81%97.mp3', TRUE, '', '', 'ototoshi', 'NHẤT TẠC NIÊN', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('伯母さん', 'おばさん', 1, 'bác; cô', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397252/elearningJP/audio/vocab/N5/vocab_n5_37_%E3%81%8A%E3%81%B0%E3%81%95%E3%82%93.mp3', TRUE, '', '', 'obasan', 'BÁ MẪU', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('叔母さん', 'おばさん', 1, 'cô; dì; người đàn bà trung niên; phụ nữ trung niên', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397253/elearningJP/audio/vocab/N5/vocab_n5_38_%E3%81%8A%E3%81%B0%E3%81%95%E3%82%93.mp3', TRUE, '', '', 'obasan', 'THÚC MẪU', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('そうして', 'そうして', 1, 'làm như thế; và', '', 'N5', 'CONJUNCTION', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397255/elearningJP/audio/vocab/N5/vocab_n5_39_%E3%81%9D%E3%81%86%E3%81%97%E3%81%A6.mp3', TRUE, '', '', 'soushite', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('作る', 'つくる', 1, 'chế biến; làm; tạo; sáng tác; xây dựng; nấu', '', 'N5', 'VERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397256/elearningJP/audio/vocab/N5/vocab_n5_40_%E3%81%A4%E3%81%8F%E3%82%8B.mp3', TRUE, '', '', 'tsukuru', 'TÁC', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('円い', 'まるい', 1, 'tròn', '', 'N5', 'ADJECTIVE', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397257/elearningJP/audio/vocab/N5/vocab_n5_41_%E3%81%BE%E3%82%8B%E3%81%84.mp3', TRUE, '', '', 'marui', 'VIÊN', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('会う', 'あう', 1, 'gặp; hội ngộ', '', 'N5', 'VERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397259/elearningJP/audio/vocab/N5/vocab_n5_42_%E3%81%82%E3%81%86.mp3', TRUE, '', '', 'au', 'HỘI', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('青', 'あお', 1, 'màu xanh da trời; màu xanh nước biển', '', 'N5', 'ADJECTIVE', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397260/elearningJP/audio/vocab/N5/vocab_n5_43_%E3%81%82%E3%81%8A.mp3', TRUE, '', '', 'ao', 'THANH', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('青い', 'あおい', 1, 'xanh da trời; xanh lục; còn xanh; thiếu kinh nghiệm', '', 'N5', 'ADJECTIVE', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397262/elearningJP/audio/vocab/N5/vocab_n5_44_%E3%81%82%E3%81%8A%E3%81%84.mp3', TRUE, '', '', 'aoi', 'THANH', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('赤', 'あか', 1, 'màu đỏ', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397263/elearningJP/audio/vocab/N5/vocab_n5_45_%E3%81%82%E3%81%8B.mp3', TRUE, '', '', 'aka', 'XÍCH', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('赤い', 'あかい', 1, 'đỏ', '', 'N5', 'ADJECTIVE', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397264/elearningJP/audio/vocab/N5/vocab_n5_46_%E3%81%82%E3%81%8B%E3%81%84.mp3', TRUE, '', '', 'akai', 'XÍCH', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('明い', 'あかるい', 1, 'sáng sủa; vui vẻ', '', 'N5', 'ADJECTIVE', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397266/elearningJP/audio/vocab/N5/vocab_n5_47_%E3%81%82%E3%81%8B%E3%82%8B%E3%81%84.mp3', TRUE, '', '', 'akarui', 'MINH', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('秋', 'あき', 1, 'mùa thu; thu', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397268/elearningJP/audio/vocab/N5/vocab_n5_48_%E3%81%82%E3%81%8D.mp3', TRUE, '', '', 'aki', 'THU', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('開ける', 'あける', 1, 'đào; đục; khoan; há; mở', '', 'N5', 'VERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397270/elearningJP/audio/vocab/N5/vocab_n5_49_%E3%81%82%E3%81%91%E3%82%8B.mp3', TRUE, '', '', 'akeru', 'KHAI', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('上げる', 'あげる', 1, 'tăng, nâng lên; di chuyển lên cao; cải thiện, thúc đẩy', '', 'N5', 'VERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397271/elearningJP/audio/vocab/N5/vocab_n5_50_%E3%81%82%E3%81%92%E3%82%8B.mp3', TRUE, '', '', 'ageru', 'THƯỢNG', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('朝', 'あさ', 1, 'ban sáng; buổi sáng; sáng', '', 'N5', 'ADVERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397272/elearningJP/audio/vocab/N5/vocab_n5_51_%E3%81%82%E3%81%95.mp3', TRUE, '', '', 'asa', 'TRIÊU', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('朝御飯', 'あさごはん', 1, 'bữa sáng; cơm sáng (nói chung); cơm sáng', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397274/elearningJP/audio/vocab/N5/vocab_n5_52_%E3%81%82%E3%81%95%E3%81%94%E3%81%AF%E3%82%93.mp3', TRUE, '', '', 'asagohan', 'TRIÊU NGỰ PHẠN', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('あさって', 'あさって', 1, 'bữa mốt; mốt; ngày kia; hai ngày sau', '', 'N5', 'ADVERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397275/elearningJP/audio/vocab/N5/vocab_n5_53_%E3%81%82%E3%81%95%E3%81%A3%E3%81%A6.mp3', TRUE, '', '', 'asatte', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('足', 'あし', 1, 'cẳng; chân', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397276/elearningJP/audio/vocab/N5/vocab_n5_54_%E3%81%82%E3%81%97.mp3', TRUE, '', '', 'ashi', 'TÚC', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('あそこ', 'あそこ', 1, 'mức độ ấy; mức ấy; ở đó; ở chỗ đó', '', 'N5', 'ADJECTIVE', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397278/elearningJP/audio/vocab/N5/vocab_n5_55_%E3%81%82%E3%81%9D%E3%81%93.mp3', TRUE, '', '', 'asoko', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('遊ぶ', 'あそぶ', 1, 'nô đùa; vui đùa; chơi (bóng chày)', '', 'N5', 'VERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397279/elearningJP/audio/vocab/N5/vocab_n5_56_%E3%81%82%E3%81%9D%E3%81%B6.mp3', TRUE, '', '', 'asobu', 'DU', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('暖かい', 'あたたかい', 1, 'đầm ấm; êm ấm; nóng; nồng hậu; ấm áp', '', 'N5', 'ADJECTIVE', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397281/elearningJP/audio/vocab/N5/vocab_n5_57_%E3%81%82%E3%81%9F%E3%81%9F%E3%81%8B%E3%81%84.mp3', TRUE, '', '', 'atatakai', 'NOÃN', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('頭', 'あたま', 1, 'đầu; người cầm đầu; kẻ cầm đầu; ông chủ', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397282/elearningJP/audio/vocab/N5/vocab_n5_58_%E3%81%82%E3%81%9F%E3%81%BE.mp3', TRUE, '', '', 'atama', 'ĐẦU', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('新しい', 'あたらしい', 1, 'mới; mới mẻ', '', 'N5', 'ADJECTIVE', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397283/elearningJP/audio/vocab/N5/vocab_n5_59_%E3%81%82%E3%81%9F%E3%82%89%E3%81%97%E3%81%84.mp3', TRUE, '', '', 'atarashii', 'TÂN', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('あちら', 'あちら', 1, 'chỗ đó; ở đó', '', 'N5', 'ADJECTIVE', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397285/elearningJP/audio/vocab/N5/vocab_n5_60_%E3%81%82%E3%81%A1%E3%82%89.mp3', TRUE, '', '', 'achira', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('暑い', 'あつい', 1, 'nóng; nóng nực; nực', '', 'N5', 'ADJECTIVE', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397286/elearningJP/audio/vocab/N5/vocab_n5_61_%E3%81%82%E3%81%A4%E3%81%84.mp3', TRUE, '', '', 'atsui', 'THỬ', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('熱い', 'あつい', 1, 'nóng; nóng bỏng; oi bức; thân thiện; nhiệt tình', '', 'N5', 'ADJECTIVE', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397288/elearningJP/audio/vocab/N5/vocab_n5_62_%E3%81%82%E3%81%A4%E3%81%84.mp3', TRUE, '', '', 'atsui', 'NHIỆT', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('厚い', 'あつい', 1, 'dày; dầy', '', 'N5', 'ADJECTIVE', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397289/elearningJP/audio/vocab/N5/vocab_n5_63_%E3%81%82%E3%81%A4%E3%81%84.mp3', TRUE, '', '', 'atsui', 'HẬU', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('あっち', 'あっち', 1, 'ấy; đó; kia; đằng kia; chỗ kia', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397291/elearningJP/audio/vocab/N5/vocab_n5_64_%E3%81%82%E3%81%A3%E3%81%A1.mp3', TRUE, '', '', 'acchi', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('あなた', 'あなた', 1, 'anh; chị', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397292/elearningJP/audio/vocab/N5/vocab_n5_65_%E3%81%82%E3%81%AA%E3%81%9F.mp3', TRUE, '', '', 'anata', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('兄', 'あに', 1, 'anh trai', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397294/elearningJP/audio/vocab/N5/vocab_n5_66_%E3%81%82%E3%81%AB.mp3', TRUE, '', '', 'ani', 'HUYNH', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('姉', 'あね', 1, 'chị; chị của mình; tỷ', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397295/elearningJP/audio/vocab/N5/vocab_n5_67_%E3%81%82%E3%81%AD.mp3', TRUE, '', '', 'ane', 'TỈ', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('あの', 'あの', 1, 'cái đó; chỗ đó', '', 'N5', 'ADJECTIVE', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397297/elearningJP/audio/vocab/N5/vocab_n5_68_%E3%81%82%E3%81%AE.mp3', TRUE, '', '', 'ano', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('アパート', 'アパート', 1, 'khu nhà tập thể; nhà chung cư; căn hộ; nhà khối', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397298/elearningJP/audio/vocab/N5/vocab_n5_69_%E3%82%A2%E3%83%91%E3%83%BC%E3%83%88.mp3', TRUE, '', '', 'アパート', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('あびる', 'あびる', 1, 'rơi vào; ngập chìm; tắm; thu hút', '', 'N5', 'VERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397300/elearningJP/audio/vocab/N5/vocab_n5_70_%E3%81%82%E3%81%B3%E3%82%8B.mp3', TRUE, '', '', 'abiru', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('危ない', 'あぶない', 1, 'nghi ngờ; không rõ; không đáng tin; nguy; nguy hiểm; nguy kịch', '', 'N5', 'ADJECTIVE', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397302/elearningJP/audio/vocab/N5/vocab_n5_71_%E3%81%82%E3%81%B6%E3%81%AA%E3%81%84.mp3', TRUE, '', '', 'abunai', 'NGUY', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('あまり', 'あまり', 1, 'không mấy; ít; thừa; phần còn lại; phần dư; phần thừa; phần dư thừa; rất; lắm', '', 'N5', 'ADJECTIVE', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397303/elearningJP/audio/vocab/N5/vocab_n5_72_%E3%81%82%E3%81%BE%E3%82%8A.mp3', TRUE, '', '', 'amari', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('雨', 'あめ', 1, 'cơn mưa; mưa; trận mưa', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397304/elearningJP/audio/vocab/N5/vocab_n5_73_%E3%81%82%E3%82%81.mp3', TRUE, '', '', 'ame', 'VŨ', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('飴', 'あめ', 1, 'kẹo; kẹo ngậm', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397306/elearningJP/audio/vocab/N5/vocab_n5_74_%E3%81%82%E3%82%81.mp3', TRUE, '', '', 'ame', 'DI', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('洗う', 'あらう', 1, 'giặt; rửa; tắm gội; tẩy; tẩy rửa', '', 'N5', 'VERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397308/elearningJP/audio/vocab/N5/vocab_n5_75_%E3%81%82%E3%82%89%E3%81%86.mp3', TRUE, '', '', 'arau', 'TẨY', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('ある', 'ある', 1, 'một certain...; some..', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397309/elearningJP/audio/vocab/N5/vocab_n5_76_%E3%81%82%E3%82%8B.mp3', TRUE, '', '', 'aru', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('歩く', 'あるく', 1, 'đi bộ; đi; bước', '', 'N5', 'VERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397310/elearningJP/audio/vocab/N5/vocab_n5_77_%E3%81%82%E3%82%8B%E3%81%8F.mp3', TRUE, '', '', 'aruku', 'BỘ', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('あれ', 'あれ', 1, 'giông tố', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397312/elearningJP/audio/vocab/N5/vocab_n5_78_%E3%81%82%E3%82%8C.mp3', TRUE, '', '', 'are', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('いいえ', 'いいえ', 1, 'không; không có gì', '', 'N5', 'INTERJECTION', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397313/elearningJP/audio/vocab/N5/vocab_n5_79_%E3%81%84%E3%81%84%E3%81%88.mp3', TRUE, '', '', 'iie', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('家', 'いえ', 1, 'gia đình; nhà', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397315/elearningJP/audio/vocab/N5/vocab_n5_80_%E3%81%84%E3%81%88.mp3', TRUE, '', '', 'ie', 'GIA', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('いかが', 'いかが', 1, 'như thế nào; thế nào', '', 'N5', 'ADVERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397316/elearningJP/audio/vocab/N5/vocab_n5_81_%E3%81%84%E3%81%8B%E3%81%8C.mp3', TRUE, '', '', 'ikaga', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('行く', 'いく', 1, 'đi', '', 'N5', 'VERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397318/elearningJP/audio/vocab/N5/vocab_n5_82_%E3%81%84%E3%81%8F.mp3', TRUE, '', '', 'iku', 'HÀNH', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('いくつ', 'いくつ', 1, 'bao nhiêu; bao nhiêu tuổi', '', 'N5', 'ADVERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397319/elearningJP/audio/vocab/N5/vocab_n5_83_%E3%81%84%E3%81%8F%E3%81%A4.mp3', TRUE, '', '', 'ikutsu', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('いくら', 'いくら', 1, 'ý nghĩa', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397320/elearningJP/audio/vocab/N5/vocab_n5_84_%E3%81%84%E3%81%8F%E3%82%89.mp3', TRUE, '', '', 'ikura', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('池', 'いけ', 1, 'bàu; cái ao; ao; hồ', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397322/elearningJP/audio/vocab/N5/vocab_n5_85_%E3%81%84%E3%81%91.mp3', TRUE, '', '', 'ike', 'TRÌ', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('医者', 'いしゃ', 1, 'bác sĩ; đại phu; thầy lang', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397323/elearningJP/audio/vocab/N5/vocab_n5_86_%E3%81%84%E3%81%97%E3%82%83.mp3', TRUE, '', '', 'isha', 'Y GIẢ', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('いす', 'いす', 1, 'ghế; cái ghế', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397325/elearningJP/audio/vocab/N5/vocab_n5_87_%E3%81%84%E3%81%99.mp3', TRUE, '', '', 'isu', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('忙しい', 'いそがしい', 1, 'bận; bận rộn; bề bộn', '', 'N5', 'ADJECTIVE', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397326/elearningJP/audio/vocab/N5/vocab_n5_88_%E3%81%84%E3%81%9D%E3%81%8C%E3%81%97%E3%81%84.mp3', TRUE, '', '', 'isogashii', 'MANG', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('痛い', 'いたい', 1, 'đau; đau đớn; nhức', '', 'N5', 'ADJECTIVE', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397327/elearningJP/audio/vocab/N5/vocab_n5_89_%E3%81%84%E3%81%9F%E3%81%84.mp3', TRUE, '', '', 'itai', 'THỐNG', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('一', 'いち', 1, 'một', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397329/elearningJP/audio/vocab/N5/vocab_n5_90_%E3%81%84%E3%81%A1.mp3', TRUE, '', '', 'ichi', 'NHẤT', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('いちばん', 'いちばん', 1, 'nhất; tốt nhất; số một; đầu tiên; number one', '', 'N5', 'ADJECTIVE', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397330/elearningJP/audio/vocab/N5/vocab_n5_91_%E3%81%84%E3%81%A1%E3%81%B0%E3%82%93.mp3', TRUE, '', '', 'ichiban', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('いつ', 'いつ', 1, 'khi nào; bao giờ; いつまでも:Lúc nào cũng; いつまで:đến bao giờ', '', 'N5', 'ADVERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397332/elearningJP/audio/vocab/N5/vocab_n5_92_%E3%81%84%E3%81%A4.mp3', TRUE, '', '', 'itsu', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('五日', 'いつか', 1, '5 ngày; năm ngày; ngày mồng 5', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397333/elearningJP/audio/vocab/N5/vocab_n5_93_%E3%81%84%E3%81%A4%E3%81%8B.mp3', TRUE, '', '', 'itsuka', 'NGŨ NHẬT', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('一緒', 'いっしょ', 1, 'cùng; cùng nhau; sự giống như vậy', '', 'N5', 'ADJECTIVE', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397335/elearningJP/audio/vocab/N5/vocab_n5_94_%E3%81%84%E3%81%A3%E3%81%97%E3%82%87.mp3', TRUE, '', '', 'issho', 'NHẤT TỰ', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('五つ', 'いつつ', 1, 'năm cái; năm chiếc', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397336/elearningJP/audio/vocab/N5/vocab_n5_95_%E3%81%84%E3%81%A4%E3%81%A4.mp3', TRUE, '', '', 'itsutsu', 'NGŨ', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('犬', 'いぬ', 1, 'cẩu; chó; khuyển', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397338/elearningJP/audio/vocab/N5/vocab_n5_96_%E3%81%84%E3%81%AC.mp3', TRUE, '', '', 'inu', 'KHUYỂN', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('今', 'いま', 1, 'bây giờ', '', 'N5', 'ADVERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397339/elearningJP/audio/vocab/N5/vocab_n5_97_%E3%81%84%E3%81%BE.mp3', TRUE, '', '', 'ima', 'KIM', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('意味', 'いみ', 1, 'ý nghĩa; nghĩa', '', 'N5', 'VERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397340/elearningJP/audio/vocab/N5/vocab_n5_98_%E3%81%84%E3%81%BF.mp3', TRUE, '', '', 'imi', 'Ý VỊ', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('妹', 'いもうと', 1, 'em; em gái', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397342/elearningJP/audio/vocab/N5/vocab_n5_99_%E3%81%84%E3%82%82%E3%81%86%E3%81%A8.mp3', TRUE, '', '', 'imouto', 'MUỘI', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('嫌', 'いや', 1, 'khó chịu; ghét; không vừa ý; sự khó chịu; sự ghét; điều chán ghét; khó chịu; không thích', '', 'N5', 'ADJECTIVE', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397343/elearningJP/audio/vocab/N5/vocab_n5_100_%E3%81%84%E3%82%84.mp3', TRUE, '', '', 'iya', 'HIỀM', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('要る', 'いる', 1, 'cần', '', 'N5', 'VERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397345/elearningJP/audio/vocab/N5/vocab_n5_101_%E3%81%84%E3%82%8B.mp3', TRUE, '', '', 'iru', 'YẾU', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('入れる', 'いれる', 1, 'cho vào; bỏ vào; đút; kéo vào', '', 'N5', 'VERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397346/elearningJP/audio/vocab/N5/vocab_n5_102_%E3%81%84%E3%82%8C%E3%82%8B.mp3', TRUE, '', '', 'ireru', 'NHẬP', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('色', 'いろ', 1, 'màu; mầu; màu sắc', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397348/elearningJP/audio/vocab/N5/vocab_n5_103_%E3%81%84%E3%82%8D.mp3', TRUE, '', '', 'iro', 'SẮC', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('いろいろ', 'いろいろ', 1, 'nhiều; phong phú', '', 'N5', 'ADJECTIVE', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397349/elearningJP/audio/vocab/N5/vocab_n5_104_%E3%81%84%E3%82%8D%E3%81%84%E3%82%8D.mp3', TRUE, '', '', 'iroiro', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('後ろ', 'うしろ', 1, 'sau; đằng sau; phía sau', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397350/elearningJP/audio/vocab/N5/vocab_n5_105_%E3%81%86%E3%81%97%E3%82%8D.mp3', TRUE, '', '', 'ushiro', 'HẬU', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('薄い', 'うすい', 1, 'lạt; lỏng; lợt', '', 'N5', 'ADJECTIVE', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397351/elearningJP/audio/vocab/N5/vocab_n5_106_%E3%81%86%E3%81%99%E3%81%84.mp3', TRUE, '', '', 'usui', 'BẠC', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('歌', 'うた', 1, 'bài hát', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397353/elearningJP/audio/vocab/N5/vocab_n5_107_%E3%81%86%E3%81%9F.mp3', TRUE, '', '', 'uta', 'CA', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('歌う', 'うたう', 1, 'ca; ca hát; hát', '', 'N5', 'VERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397354/elearningJP/audio/vocab/N5/vocab_n5_108_%E3%81%86%E3%81%9F%E3%81%86.mp3', TRUE, '', '', 'utau', 'CA', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('生まれる', 'うまれる', 1, 'đản sinh; được sinh ra; sinh ra; lọt lòng', '', 'N5', 'VERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397355/elearningJP/audio/vocab/N5/vocab_n5_109_%E3%81%86%E3%81%BE%E3%82%8C%E3%82%8B.mp3', TRUE, '', '', 'umareru', 'SANH', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('海', 'うみ', 1, 'bể; bể khơi; biển; bờ biển', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397357/elearningJP/audio/vocab/N5/vocab_n5_110_%E3%81%86%E3%81%BF.mp3', TRUE, '', '', 'umi', 'HẢI', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('売る', 'うる', 1, 'bán; bán hàng', '', 'N5', 'VERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397358/elearningJP/audio/vocab/N5/vocab_n5_111_%E3%81%86%E3%82%8B.mp3', TRUE, '', '', 'uru', 'MẠI', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('煩い', 'うるさい', 1, 'chán ghét; đáng ghét; ồn ào; phiền phức; lắm điều', '', 'N5', 'ADJECTIVE', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397360/elearningJP/audio/vocab/N5/vocab_n5_112_%E3%81%86%E3%82%8B%E3%81%95%E3%81%84.mp3', TRUE, '', '', 'urusai', 'PHIỀN', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('上着', 'うわぎ', 1, 'áo vét; áo khoác', '', 'N5', 'ADJECTIVE', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397361/elearningJP/audio/vocab/N5/vocab_n5_113_%E3%81%86%E3%82%8F%E3%81%8E.mp3', TRUE, '', '', 'uwagi', 'THƯỢNG TRỨ', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('絵', 'え', 1, 'bức tranh; tranh', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397362/elearningJP/audio/vocab/N5/vocab_n5_114_%E3%81%88.mp3', TRUE, '', '', 'e', 'HỘI', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('映画', 'えいが', 1, 'điện ảnh; phim', '', 'N5', 'ADJECTIVE', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397364/elearningJP/audio/vocab/N5/vocab_n5_115_%E3%81%88%E3%81%84%E3%81%8C.mp3', TRUE, '', '', 'eiga', 'ÁNH HỌA', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('映画館', 'えいがかん', 1, 'rạp chiếu phim; rạp; rạp chiếu bóng; trung tâm chiếu phim', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397365/elearningJP/audio/vocab/N5/vocab_n5_116_%E3%81%88%E3%81%84%E3%81%8C%E3%81%8B%E3%82%93.mp3', TRUE, '', '', 'eigakan', 'ÁNH HỌA QUÁN', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('英語', 'えいご', 1, 'tiếng Anh', '', 'N5', 'ADJECTIVE', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397366/elearningJP/audio/vocab/N5/vocab_n5_117_%E3%81%88%E3%81%84%E3%81%94.mp3', TRUE, '', '', 'eigo', 'ANH NGỮ', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('ええ', 'ええ', 1, 'vâng; vâng; dạ; ừ', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397368/elearningJP/audio/vocab/N5/vocab_n5_118_%E3%81%88%E3%81%88.mp3', TRUE, '', '', 'ee', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('駅', 'えき', 1, 'ga; nhà ga', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397369/elearningJP/audio/vocab/N5/vocab_n5_119_%E3%81%88%E3%81%8D.mp3', TRUE, '', '', 'eki', 'DỊCH', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('エレベーター', 'エレベーター', 1, 'thang máy', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397371/elearningJP/audio/vocab/N5/vocab_n5_120_%E3%82%A8%E3%83%AC%E3%83%99%E3%83%BC%E3%82%BF%E3%83%BC.mp3', TRUE, '', '', 'エレベーター', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('鉛筆', 'えんぴつ', 1, 'bút chì; viết chì', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397372/elearningJP/audio/vocab/N5/vocab_n5_121_%E3%81%88%E3%82%93%E3%81%B4%E3%81%A4.mp3', TRUE, '', '', 'enpitsu', 'DUYÊN BÚT', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('おいしい', 'おいしい', 1, 'Ngon', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397374/elearningJP/audio/vocab/N5/vocab_n5_122_%E3%81%8A%E3%81%84%E3%81%97%E3%81%84.mp3', TRUE, '', '', 'oishii', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('多い', 'おおい', 1, 'bộn; nhiều', '', 'N5', 'ADJECTIVE', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397375/elearningJP/audio/vocab/N5/vocab_n5_123_%E3%81%8A%E3%81%8A%E3%81%84.mp3', TRUE, '', '', 'ooi', 'ĐA', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('大きい', 'おおきい', 1, 'bự; to lớn; to; lớn', '', 'N5', 'ADJECTIVE', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397376/elearningJP/audio/vocab/N5/vocab_n5_124_%E3%81%8A%E3%81%8A%E3%81%8D%E3%81%84.mp3', TRUE, '', '', 'ookii', 'ĐẠI', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('大きな', 'おおきな', 1, 'bự; lớn; to', '', 'N5', 'ADJECTIVE', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397378/elearningJP/audio/vocab/N5/vocab_n5_125_%E3%81%8A%E3%81%8A%E3%81%8D%E3%81%AA.mp3', TRUE, '', '', 'ookina', 'ĐẠI', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('大勢', 'おおぜい', 1, 'đại chúng; phần lớn mọi người; đám đông; nhiều người; nhiều; rất nhiều', '', 'N5', 'ADJECTIVE', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397379/elearningJP/audio/vocab/N5/vocab_n5_126_%E3%81%8A%E3%81%8A%E3%81%9C%E3%81%84.mp3', TRUE, '', '', 'oozei', 'ĐẠI THẾ', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('お母さん', 'おかあさん', 1, 'má; mẹ; mẹ ơi; thân mẫu', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397380/elearningJP/audio/vocab/N5/vocab_n5_127_%E3%81%8A%E3%81%8B%E3%81%82%E3%81%95%E3%82%93.mp3', TRUE, '', '', 'okaasan', 'MẪU', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('お菓子', 'おかし', 1, 'bánh kẹo; kẹo; bánh ngọt', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397381/elearningJP/audio/vocab/N5/vocab_n5_128_%E3%81%8A%E3%81%8B%E3%81%97.mp3', TRUE, '', '', 'okashi', 'QUẢ TỬ', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('お金', 'おかね', 1, 'tiền; của cải', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397383/elearningJP/audio/vocab/N5/vocab_n5_129_%E3%81%8A%E3%81%8B%E3%81%AD.mp3', TRUE, '', '', 'okane', 'KIM', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('起きる', 'おきる', 1, 'dấy; đứng dậy; ngồi dậy; bình phục; nhen nhúm', '', 'N5', 'VERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397384/elearningJP/audio/vocab/N5/vocab_n5_130_%E3%81%8A%E3%81%8D%E3%82%8B.mp3', TRUE, '', '', 'okiru', 'KHỞI', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('置く', 'おく', 1, 'bố trí (người); cho thuê chỗ ở; chừa ra; để chừa ra', '', 'N5', 'VERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397385/elearningJP/audio/vocab/N5/vocab_n5_131_%E3%81%8A%E3%81%8F.mp3', TRUE, '', '', 'oku', 'TRÍ', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('奥さん', 'おくさん', 1, 'bà; vợ; bà nhà; chị nhà', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397387/elearningJP/audio/vocab/N5/vocab_n5_132_%E3%81%8A%E3%81%8F%E3%81%95%E3%82%93.mp3', TRUE, '', '', 'okusan', 'ÁO', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('お酒', 'おさけ', 1, 'rượu; rượu sakê', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397388/elearningJP/audio/vocab/N5/vocab_n5_133_%E3%81%8A%E3%81%95%E3%81%91.mp3', TRUE, '', '', 'osake', 'TỬU', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('お皿', 'おさら', 1, 'đĩa', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397390/elearningJP/audio/vocab/N5/vocab_n5_134_%E3%81%8A%E3%81%95%E3%82%89.mp3', TRUE, '', '', 'osara', 'MÃNH', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('伯父', 'おじいさん', 1, 'bác; chú; chú bác; dì', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397391/elearningJP/audio/vocab/N5/vocab_n5_135_%E3%81%8A%E3%81%98%E3%81%84%E3%81%95%E3%82%93.mp3', TRUE, '', '', 'ojiisan', 'BÁ PHỤ', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('叔父', 'おじいさん', 1, 'cậu; chú; chú bác', '', 'N5', 'ADJECTIVE', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397392/elearningJP/audio/vocab/N5/vocab_n5_136_%E3%81%8A%E3%81%98%E3%81%84%E3%81%95%E3%82%93.mp3', TRUE, '', '', 'ojiisan', 'THÚC PHỤ', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('教える', 'おしえる', 1, 'chỉ dẫn; chỉ dạy; dạy dỗ; chỉ bảo; dạy', '', 'N5', 'VERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397393/elearningJP/audio/vocab/N5/vocab_n5_137_%E3%81%8A%E3%81%97%E3%81%88%E3%82%8B.mp3', TRUE, '', '', 'oshieru', 'GIÁO', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('押す', 'おす', 1, 'ẩn; đẩy; ấn; nhấn; bấm; dí', '', 'N5', 'VERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397395/elearningJP/audio/vocab/N5/vocab_n5_138_%E3%81%8A%E3%81%99.mp3', TRUE, '', '', 'osu', 'ÁP', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('遅い', 'おそい', 1, 'muộn màng; muộn; chậm; trễ', '', 'N5', 'ADJECTIVE', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397396/elearningJP/audio/vocab/N5/vocab_n5_139_%E3%81%8A%E3%81%9D%E3%81%84.mp3', TRUE, '', '', 'osoi', 'TRÌ', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('お茶', 'おちゃ', 1, 'chè; nước chè; trà; chè xanh', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397397/elearningJP/audio/vocab/N5/vocab_n5_140_%E3%81%8A%E3%81%A1%E3%82%83.mp3', TRUE, '', '', 'ocha', 'TRÀ', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('お手洗い', 'おてあらい', 1, 'toa-lét; nhà vệ sinh', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397399/elearningJP/audio/vocab/N5/vocab_n5_141_%E3%81%8A%E3%81%A6%E3%81%82%E3%82%89%E3%81%84.mp3', TRUE, '', '', 'otearai', 'THỦ TẨY', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('お父さん', 'おとうさん', 1, 'bố; bố ơi (khi con gọi bố; cha; thân phụ', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397400/elearningJP/audio/vocab/N5/vocab_n5_142_%E3%81%8A%E3%81%A8%E3%81%86%E3%81%95%E3%82%93.mp3', TRUE, '', '', 'otousan', 'PHỤ', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('男', 'おとこ', 1, 'đàn ông; người đàn ông; nam; trai', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397401/elearningJP/audio/vocab/N5/vocab_n5_143_%E3%81%8A%E3%81%A8%E3%81%93.mp3', TRUE, '', '', 'otoko', 'NAM', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('男の子', 'おとこのこ', 1, 'cậu bé; con đực (động vật)', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397403/elearningJP/audio/vocab/N5/vocab_n5_144_%E3%81%8A%E3%81%A8%E3%81%93%E3%81%AE%E3%81%93.mp3', TRUE, '', '', 'otokonoko', 'NAM TỬ', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('大人', 'おとな', 1, 'người lớn; người trưởng thành', '', 'N5', 'ADJECTIVE', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397404/elearningJP/audio/vocab/N5/vocab_n5_145_%E3%81%8A%E3%81%A8%E3%81%AA.mp3', TRUE, '', '', 'otona', 'ĐẠI NHÂN', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('おなか', 'おなか', 1, 'bụng', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397405/elearningJP/audio/vocab/N5/vocab_n5_146_%E3%81%8A%E3%81%AA%E3%81%8B.mp3', TRUE, '', '', 'onaka', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('同じ', 'おなじ', 1, 'bằng nhau; sự giống nhau; sự giống; giống nhau; cùng; giống', '', 'N5', 'ADJECTIVE', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397407/elearningJP/audio/vocab/N5/vocab_n5_147_%E3%81%8A%E3%81%AA%E3%81%98.mp3', TRUE, '', '', 'onaji', 'ĐỒNG', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('お兄さん', 'おにいさん', 1, 'anh trai; thưa anh; anh ơi; anh trai (...bạn)', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397408/elearningJP/audio/vocab/N5/vocab_n5_148_%E3%81%8A%E3%81%AB%E3%81%84%E3%81%95%E3%82%93.mp3', TRUE, '', '', 'oniisan', 'HUYNH', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('お姉さん', 'おねえさん', 1, 'chị; chị gái (bạn...); thưa chị; chị ơi', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397409/elearningJP/audio/vocab/N5/vocab_n5_149_%E3%81%8A%E3%81%AD%E3%81%88%E3%81%95%E3%82%93.mp3', TRUE, '', '', 'oneesan', 'TỈ', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('おばあさん', 'おばあさん', 1, 'bà; bà già; người già; bà cụ', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397411/elearningJP/audio/vocab/N5/vocab_n5_150_%E3%81%8A%E3%81%B0%E3%81%82%E3%81%95%E3%82%93.mp3', TRUE, '', '', 'obaasan', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('お風呂', 'おふろ', 1, 'bồn', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397412/elearningJP/audio/vocab/N5/vocab_n5_151_%E3%81%8A%E3%81%B5%E3%82%8D.mp3', TRUE, '', '', 'ofuro', 'PHONG LỮ', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('お弁当', 'おべんとう', 1, 'cơm hộp; cơm trưa', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397413/elearningJP/audio/vocab/N5/vocab_n5_152_%E3%81%8A%E3%81%B9%E3%82%93%E3%81%A8%E3%81%86.mp3', TRUE, '', '', 'obentou', 'BIỆN ĐƯƠNG', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('覚える', 'おぼえる', 1, 'cảm thấy; học; học thuộc; nhớ', '', 'N5', 'VERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397415/elearningJP/audio/vocab/N5/vocab_n5_153_%E3%81%8A%E3%81%BC%E3%81%88%E3%82%8B.mp3', TRUE, '', '', 'oboeru', 'GIÁC', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('おまわりさん', 'おまわりさん', 1, 'cảnh sát giao thông; tuần cảnh; tuần du', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397416/elearningJP/audio/vocab/N5/vocab_n5_154_%E3%81%8A%E3%81%BE%E3%82%8F%E3%82%8A%E3%81%95%E3%82%93.mp3', TRUE, '', '', 'omawarisan', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('重い', 'おもい', 1, 'nặng; nặng nề; trầm trọng', '', 'N5', 'ADJECTIVE', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397418/elearningJP/audio/vocab/N5/vocab_n5_155_%E3%81%8A%E3%82%82%E3%81%84.mp3', TRUE, '', '', 'omoi', 'TRỌNG', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('おもしろい', 'おもしろい', 1, 'dí dỏm; thú vị; hay; vui tính', '', 'N5', 'ADJECTIVE', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397419/elearningJP/audio/vocab/N5/vocab_n5_156_%E3%81%8A%E3%82%82%E3%81%97%E3%82%8D%E3%81%84.mp3', TRUE, '', '', 'omoshiroi', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('泳ぐ', 'およぐ', 1, 'bơi; bơi lội; lội', '', 'N5', 'VERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397421/elearningJP/audio/vocab/N5/vocab_n5_157_%E3%81%8A%E3%82%88%E3%81%90.mp3', TRUE, '', '', 'oyogu', 'VỊNH', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('降りる', 'おりる', 1, 'bước xuống; hạ; rủ', '', 'N5', 'VERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397422/elearningJP/audio/vocab/N5/vocab_n5_158_%E3%81%8A%E3%82%8A%E3%82%8B.mp3', TRUE, '', '', 'oriru', 'HÀNG', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('終る', 'おわる', 1, 'hoàn thành, kết thúc', '', 'N5', 'VERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397423/elearningJP/audio/vocab/N5/vocab_n5_159_%E3%81%8A%E3%82%8F%E3%82%8B.mp3', TRUE, '', '', 'owaru', 'CHUNG', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('音楽', 'おんがく', 1, 'âm nhạc; nhạc; ca nhạc; âm nhạc', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397425/elearningJP/audio/vocab/N5/vocab_n5_160_%E3%81%8A%E3%82%93%E3%81%8C%E3%81%8F.mp3', TRUE, '', '', 'ongaku', 'ÂM LẠC', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('女', 'おんな', 1, 'phụ nữ; con gái; cô gái; đàn bà; nữ; con gái, nữ giới', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397426/elearningJP/audio/vocab/N5/vocab_n5_161_%E3%81%8A%E3%82%93%E3%81%AA.mp3', TRUE, '', '', 'onna', 'NỮ', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('女の子', 'おんなのこ', 1, 'cô gái; cô bé', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397429/elearningJP/audio/vocab/N5/vocab_n5_162_%E3%81%8A%E3%82%93%E3%81%AA%E3%81%AE%E3%81%93.mp3', TRUE, '', '', 'onnanoko', 'NỮ TỬ', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('外国', 'がいこく', 1, 'đất khách; ngoại bang; ngoại quốc', '', 'N5', 'ADJECTIVE', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397430/elearningJP/audio/vocab/N5/vocab_n5_163_%E3%81%8C%E3%81%84%E3%81%93%E3%81%8F.mp3', TRUE, '', '', 'gaikoku', 'NGOẠI QUỐC', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('外国人', 'がいこくじん', 1, 'ngoại nhân; người nước ngoài; người ngoại quốc', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397431/elearningJP/audio/vocab/N5/vocab_n5_164_%E3%81%8C%E3%81%84%E3%81%93%E3%81%8F%E3%81%98%E3%82%93.mp3', TRUE, '', '', 'gaikokujin', 'NGOẠI QUỐC NHÂN', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('会社', 'かいしゃ', 1, 'công ty; hãng', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397433/elearningJP/audio/vocab/N5/vocab_n5_165_%E3%81%8B%E3%81%84%E3%81%97%E3%82%83.mp3', TRUE, '', '', 'kaisha', 'HỘI XÃ', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('階段', 'かいだん', 1, 'cầu thang; thang gác; thang lầu', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397434/elearningJP/audio/vocab/N5/vocab_n5_166_%E3%81%8B%E3%81%84%E3%81%A0%E3%82%93.mp3', TRUE, '', '', 'kaidan', 'GIAI ĐOẠN', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('買い物', 'かいもの', 1, 'món hàng mua được; sự mua hàng; thứ cần mua', '', 'N5', 'ADJECTIVE', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397435/elearningJP/audio/vocab/N5/vocab_n5_167_%E3%81%8B%E3%81%84%E3%82%82%E3%81%AE.mp3', TRUE, '', '', 'kaimono', 'MÃI VẬT', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('買う', 'かう', 1, 'đánh giá cao; tán dương thưởng thức; gây ra; chuốc lấy; làm cho; mua', '', 'N5', 'VERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397437/elearningJP/audio/vocab/N5/vocab_n5_168_%E3%81%8B%E3%81%86.mp3', TRUE, '', '', 'kau', 'MÃI', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('返す', 'かえす', 1, 'trả; trả lại; chuyển lại', '', 'N5', 'VERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397438/elearningJP/audio/vocab/N5/vocab_n5_169_%E3%81%8B%E3%81%88%E3%81%99.mp3', TRUE, '', '', 'kaesu', 'PHẢN', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('帰る', 'かえる', 1, 'về; chạy về', '', 'N5', 'VERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397440/elearningJP/audio/vocab/N5/vocab_n5_170_%E3%81%8B%E3%81%88%E3%82%8B.mp3', TRUE, '', '', 'kaeru', 'QUY', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('かかる', 'かかる', 1, 'liên quan; liên lụy; về', '', 'N5', 'VERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397441/elearningJP/audio/vocab/N5/vocab_n5_171_%E3%81%8B%E3%81%8B%E3%82%8B.mp3', TRUE, '', '', 'kakaru', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('かぎ', 'かぎ', 1, 'woman who earns her living by entertaining with song, dance and playing the shamisen, geisha who sings at parties', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397443/elearningJP/audio/vocab/N5/vocab_n5_172_%E3%81%8B%E3%81%8E.mp3', TRUE, '', '', 'kagi', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('書く', 'かく', 1, 'vẽ; viết; viết lách', '', 'N5', 'VERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397444/elearningJP/audio/vocab/N5/vocab_n5_173_%E3%81%8B%E3%81%8F.mp3', TRUE, '', '', 'kaku', 'THƯ', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('学生', 'がくせい', 1, 'sinh viên; học sinh', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397445/elearningJP/audio/vocab/N5/vocab_n5_174_%E3%81%8C%E3%81%8F%E3%81%9B%E3%81%84.mp3', TRUE, '', '', 'gakusei', 'HỌC SANH', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('かける', 'かける', 1, 'treo lên; treo; dựng', '', 'N5', 'VERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397447/elearningJP/audio/vocab/N5/vocab_n5_175_%E3%81%8B%E3%81%91%E3%82%8B.mp3', TRUE, '', '', 'kakeru', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('傘', 'かさ', 1, 'cái ô; dù; ô; cái ô', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397448/elearningJP/audio/vocab/N5/vocab_n5_176_%E3%81%8B%E3%81%95.mp3', TRUE, '', '', 'kasa', 'TÁN', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('貸す', 'かす', 1, 'bán đợ; cho vay; cho mượn', '', 'N5', 'VERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397450/elearningJP/audio/vocab/N5/vocab_n5_177_%E3%81%8B%E3%81%99.mp3', TRUE, '', '', 'kasu', 'THẢI', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('風', 'かぜ', 1, 'gió', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397451/elearningJP/audio/vocab/N5/vocab_n5_178_%E3%81%8B%E3%81%9C.mp3', TRUE, '', '', 'kaze', 'PHONG', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('風邪', 'かぜ', 1, 'cảm lạnh; cảm; cảm cúm; sổ mũi', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397452/elearningJP/audio/vocab/N5/vocab_n5_179_%E3%81%8B%E3%81%9C.mp3', TRUE, '', '', 'kaze', 'PHONG TÀ', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('家族', 'かぞく', 1, 'gia đình; gia quyến; gia tộc', '', 'N5', 'ADJECTIVE', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397454/elearningJP/audio/vocab/N5/vocab_n5_180_%E3%81%8B%E3%81%9E%E3%81%8F.mp3', TRUE, '', '', 'kazoku', 'GIA TỘC', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('学校', 'がっこう', 1, 'học đường; học hiệu; nhà trường', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397456/elearningJP/audio/vocab/N5/vocab_n5_181_%E3%81%8C%E3%81%A3%E3%81%93%E3%81%86.mp3', TRUE, '', '', 'gakkou', 'HỌC GIÁO', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('カップ', 'カップ', 1, 'cốc; chén; bát; cúp', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397457/elearningJP/audio/vocab/N5/vocab_n5_182_%E3%82%AB%E3%83%83%E3%83%97.mp3', TRUE, '', '', 'カップ', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('家庭', 'かてい', 1, 'gia đình, hộ gia đình (nơi chốn)', '', 'N5', 'ADJECTIVE', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397458/elearningJP/audio/vocab/N5/vocab_n5_183_%E3%81%8B%E3%81%A6%E3%81%84.mp3', TRUE, '', '', 'katei', 'GIA ĐÌNH', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('かばん', 'かばん', 1, 'việc đóng dấu', '', 'N5', 'VERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397460/elearningJP/audio/vocab/N5/vocab_n5_184_%E3%81%8B%E3%81%B0%E3%82%93.mp3', TRUE, '', '', 'kaban', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('花瓶', 'かびん', 1, 'bình hoa; lọ hoa; bình dùng để đựng hoa cúng (thường làm bằng đồng mạ vàng)', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397461/elearningJP/audio/vocab/N5/vocab_n5_185_%E3%81%8B%E3%81%B3%E3%82%93.mp3', TRUE, '', '', 'kabin', 'HOA BÌNH', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('紙', 'かみ', 1, 'giấy', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397462/elearningJP/audio/vocab/N5/vocab_n5_186_%E3%81%8B%E3%81%BF.mp3', TRUE, '', '', 'kami', 'CHỈ', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('カメラ', 'カメラ', 1, 'máy ảnh; máy ảnh; máy quay phim; máy chụp ảnh', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397464/elearningJP/audio/vocab/N5/vocab_n5_187_%E3%82%AB%E3%83%A1%E3%83%A9.mp3', TRUE, '', '', 'カメラ', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('火曜日', 'かようび', 1, 'thứ ba; ngày thứ ba', '', 'N5', 'ADVERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397465/elearningJP/audio/vocab/N5/vocab_n5_188_%E3%81%8B%E3%82%88%E3%81%86%E3%81%B3.mp3', TRUE, '', '', 'kayoubi', 'HỎA DIỆU NHẬT', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('借りる', 'かりる', 1, 'mướn; tô; thuê; mượn; vay', '', 'N5', 'VERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397466/elearningJP/audio/vocab/N5/vocab_n5_189_%E3%81%8B%E3%82%8A%E3%82%8B.mp3', TRUE, '', '', 'kariru', 'TÁ', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('軽い', 'かるい', 1, 'nhẹ', '', 'N5', 'ADJECTIVE', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397467/elearningJP/audio/vocab/N5/vocab_n5_190_%E3%81%8B%E3%82%8B%E3%81%84.mp3', TRUE, '', '', 'karui', 'KHINH', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('カレー', 'カレー', 1, 'cà ri; món cari; cà-ri', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397469/elearningJP/audio/vocab/N5/vocab_n5_191_%E3%82%AB%E3%83%AC%E3%83%BC.mp3', TRUE, '', '', 'カレー', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('カレンダー', 'カレンダー', 1, 'lịch; máy ca-len-da; máy định hình vải; niên lịch', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397471/elearningJP/audio/vocab/N5/vocab_n5_192_%E3%82%AB%E3%83%AC%E3%83%B3%E3%83%80%E3%83%BC.mp3', TRUE, '', '', 'カレンダー', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('かわいい', 'かわいい', 1, 'duyên dáng; đáng yêu; xinh xắn; dễ thương; khả ái; êm ái; ngộ nghĩnh', '', 'N5', 'ADJECTIVE', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397472/elearningJP/audio/vocab/N5/vocab_n5_193_%E3%81%8B%E3%82%8F%E3%81%84%E3%81%84.mp3', TRUE, '', '', 'kawaii', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('漢字', 'かんじ', 1, 'chữ Hán; hán tự', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397473/elearningJP/audio/vocab/N5/vocab_n5_194_%E3%81%8B%E3%82%93%E3%81%98.mp3', TRUE, '', '', 'kanji', 'HÁN TỰ', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('木', 'き', 1, 'cây cối; cây; gỗ; mộc', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397475/elearningJP/audio/vocab/N5/vocab_n5_195_%E3%81%8D.mp3', TRUE, '', '', 'ki', 'MỘC', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('黄色い', 'きいろい', 1, 'vàng', '', 'N5', 'ADJECTIVE', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397476/elearningJP/audio/vocab/N5/vocab_n5_196_%E3%81%8D%E3%81%84%E3%82%8D%E3%81%84.mp3', TRUE, '', '', 'kiiroi', 'HOÀNG SẮC', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('消える', 'きえる', 1, 'biến mất; tan đi; tắt', '', 'N5', 'VERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397478/elearningJP/audio/vocab/N5/vocab_n5_197_%E3%81%8D%E3%81%88%E3%82%8B.mp3', TRUE, '', '', 'kieru', 'TIÊU', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('聞く', 'きく', 1, 'nghe; hỏi', '', 'N5', 'VERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397479/elearningJP/audio/vocab/N5/vocab_n5_198_%E3%81%8D%E3%81%8F.mp3', TRUE, '', '', 'kiku', 'VĂN', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('北', 'きた', 1, 'phía Bắc; miền Bắc', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397481/elearningJP/audio/vocab/N5/vocab_n5_199_%E3%81%8D%E3%81%9F.mp3', TRUE, '', '', 'kita', 'BẮC', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('ギター', 'ギター', 1, 'đàn ghita; ghita', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397483/elearningJP/audio/vocab/N5/vocab_n5_200_%E3%82%AE%E3%82%BF%E3%83%BC.mp3', TRUE, '', '', 'ギター', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('汚い', 'きたない', 1, 'bẩn; ô uế; bẩn thỉu; bê bết; bệ rạc', '', 'N5', 'ADJECTIVE', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397485/elearningJP/audio/vocab/N5/vocab_n5_201_%E3%81%8D%E3%81%9F%E3%81%AA%E3%81%84.mp3', TRUE, '', '', 'kitanai', 'Ô', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('喫茶店', 'きっさてん', 1, 'quán cà phê; quán trà; quán nước; tiệm giải khát; quán giải khát', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397487/elearningJP/audio/vocab/N5/vocab_n5_202_%E3%81%8D%E3%81%A3%E3%81%95%E3%81%A6%E3%82%93.mp3', TRUE, '', '', 'kissaten', 'KHIẾT TRÀ ĐIẾM', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('切手', 'きって', 1, 'tem; tem hàng', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397489/elearningJP/audio/vocab/N5/vocab_n5_203_%E3%81%8D%E3%81%A3%E3%81%A6.mp3', TRUE, '', '', 'kitte', 'THIẾT THỦ', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('切符', 'きっぷ', 1, 'vé', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397490/elearningJP/audio/vocab/N5/vocab_n5_204_%E3%81%8D%E3%81%A3%E3%81%B7.mp3', TRUE, '', '', 'kippu', 'THIẾT PHÙ', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('昨日', 'きのう', 1, 'bữa hôm trước; bữa qua; ngày hôm qua', '', 'N5', 'ADVERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397491/elearningJP/audio/vocab/N5/vocab_n5_205_%E3%81%8D%E3%81%AE%E3%81%86.mp3', TRUE, '', '', 'kinou', 'TẠC NHẬT', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('牛肉', 'ぎゅうにく', 1, 'thịt bò', '', 'N5', 'ADJECTIVE', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397493/elearningJP/audio/vocab/N5/vocab_n5_206_%E3%81%8E%E3%82%85%E3%81%86%E3%81%AB%E3%81%8F.mp3', TRUE, '', '', 'gyuuniku', 'NGƯU NHỤC', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('牛乳', 'ぎゅうにゅう', 1, 'sữa; sữa bò', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397494/elearningJP/audio/vocab/N5/vocab_n5_207_%E3%81%8E%E3%82%85%E3%81%86%E3%81%AB%E3%82%85%E3%81%86.mp3', TRUE, '', '', 'gyuunyuu', 'NGƯU NHŨ', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('教室', 'きょうしつ', 1, 'Lớp học, phòng học', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397496/elearningJP/audio/vocab/N5/vocab_n5_208_%E3%81%8D%E3%82%87%E3%81%86%E3%81%97%E3%81%A4.mp3', TRUE, '', '', 'kyoushitsu', 'GIÁO THẤT', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('兄弟', 'きょうだい', 1, 'anh em; huynh đệ', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397497/elearningJP/audio/vocab/N5/vocab_n5_209_%E3%81%8D%E3%82%87%E3%81%86%E3%81%A0%E3%81%84.mp3', TRUE, '', '', 'kyoudai', 'HUYNH ĐỆ', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('去年', 'きょねん', 1, 'năm ngoái; năm trước; năm qua', '', 'N5', 'ADVERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397501/elearningJP/audio/vocab/N5/vocab_n5_210_%E3%81%8D%E3%82%87%E3%81%AD%E3%82%93.mp3', TRUE, '', '', 'kyonen', 'KHỨ NIÊN', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('嫌い', 'きらい', 1, 'đáng ghét; không ưa; không thích; ghét; phân biệt; khu biệt; sự đáng ghét; sự không ưa; đáng ghét; không ưa; không thích; ghét', '', 'N5', 'ADJECTIVE', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397502/elearningJP/audio/vocab/N5/vocab_n5_211_%E3%81%8D%E3%82%89%E3%81%84.mp3', TRUE, '', '', 'kirai', 'HIỀM', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('切る', 'きる', 1, 'cắt; chặt; thái; băm; ngắt; đốn; hạ; bấm; cúp; thái; xé; bẻ; lật; ấn định; cắt đứt; chọc tiết; cưa', '', 'N5', 'VERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397503/elearningJP/audio/vocab/N5/vocab_n5_212_%E3%81%8D%E3%82%8B.mp3', TRUE, '', '', 'kiru', 'THIẾT', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('着る', 'きる', 1, 'bận; khoác; mặc', '', 'N5', 'VERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397505/elearningJP/audio/vocab/N5/vocab_n5_213_%E3%81%8D%E3%82%8B.mp3', TRUE, '', '', 'kiru', 'TRỨ', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('きれい', 'きれい', 1, 'đẹp; sạch; đẹp; giỏ rác; đẹp; hội chợ', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397506/elearningJP/audio/vocab/N5/vocab_n5_214_%E3%81%8D%E3%82%8C%E3%81%84.mp3', TRUE, '', '', 'kirei', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('キログラム', 'キロ', 1, 'cân; kilô; kilôgam; ký; kilogram', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397508/elearningJP/audio/vocab/N5/vocab_n5_215_%E3%82%AD%E3%83%AD.mp3', TRUE, '', '', 'キロ', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('キロメートル', 'キロ', 1, 'kilômét; cây số', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397509/elearningJP/audio/vocab/N5/vocab_n5_216_%E3%82%AD%E3%83%AD.mp3', TRUE, '', '', 'キロ', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('銀行', 'ぎんこう', 1, 'ngân hàng; nhà băng', '', 'N5', 'ADJECTIVE', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397510/elearningJP/audio/vocab/N5/vocab_n5_217_%E3%81%8E%E3%82%93%E3%81%93%E3%81%86.mp3', TRUE, '', '', 'ginkou', 'NGÂN HÀNH', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('金曜日', 'きんようび', 1, 'ngày thứ sáu; thứ sáu', '', 'N5', 'ADVERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397512/elearningJP/audio/vocab/N5/vocab_n5_218_%E3%81%8D%E3%82%93%E3%82%88%E3%81%86%E3%81%B3.mp3', TRUE, '', '', 'kinyoubi', 'KIM DIỆU NHẬT', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('薬', 'くすり', 1, 'dược; thuốc', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397513/elearningJP/audio/vocab/N5/vocab_n5_219_%E3%81%8F%E3%81%99%E3%82%8A.mp3', TRUE, '', '', 'kusuri', 'DƯỢC', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('ください', 'ください', 1, 'please (kanonly); (with te-form verb) please do for me', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397514/elearningJP/audio/vocab/N5/vocab_n5_220_%E3%81%8F%E3%81%A0%E3%81%95%E3%81%84.mp3', TRUE, '', '', 'kudasai', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('果物', 'くだもの', 1, 'hoa quả; trái cây', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397517/elearningJP/audio/vocab/N5/vocab_n5_221_%E3%81%8F%E3%81%A0%E3%82%82%E3%81%AE.mp3', TRUE, '', '', 'kudamono', 'QUẢ VẬT', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('口', 'くち', 1, 'cửa; miệng; chỗ cho vào; chỗ ra vào (đồ vật); mồm; miệng; mỏ; miệng', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397518/elearningJP/audio/vocab/N5/vocab_n5_222_%E3%81%8F%E3%81%A1.mp3', TRUE, '', '', 'kuchi', 'KHẨU', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('靴', 'くつ', 1, 'giày; dép; guốc', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397520/elearningJP/audio/vocab/N5/vocab_n5_223_%E3%81%8F%E3%81%A4.mp3', TRUE, '', '', 'kutsu', 'NGOA', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('靴下', 'くつした', 1, 'bít tất; tất; tất chân; vớ', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397521/elearningJP/audio/vocab/N5/vocab_n5_224_%E3%81%8F%E3%81%A4%E3%81%97%E3%81%9F.mp3', TRUE, '', '', 'kutsushita', 'NGOA HẠ', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('国', 'くに', 1, 'đất nước; quốc gia; quê nhà', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397523/elearningJP/audio/vocab/N5/vocab_n5_225_%E3%81%8F%E3%81%AB.mp3', TRUE, '', '', 'kuni', 'QUỐC', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('曇り', 'くもり', 1, 'mờ; không rõ; nhiều mây; sự không chính trực; trời âm u; trời đầy mây', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397524/elearningJP/audio/vocab/N5/vocab_n5_226_%E3%81%8F%E3%82%82%E3%82%8A.mp3', TRUE, '', '', 'kumori', 'ĐÀM', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('曇る', 'くもる', 1, 'đầy ..; nỗi lòng buồn chán; ủ ê; râm', '', 'N5', 'VERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397526/elearningJP/audio/vocab/N5/vocab_n5_227_%E3%81%8F%E3%82%82%E3%82%8B.mp3', TRUE, '', '', 'kumoru', 'ĐÀM', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('暗い', 'くらい', 1, 'tối tăm; ảm đạm; âm u (bầu trời, không khí); tối; tối màu; u sầu, u ám, trầm (tính cách, tâm trạng)', '', 'N5', 'ADJECTIVE', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397527/elearningJP/audio/vocab/N5/vocab_n5_228_%E3%81%8F%E3%82%89%E3%81%84.mp3', TRUE, '', '', 'kurai', 'ÁM', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('クラス', 'クラス', 1, 'lớp; lớp học; lớp; lớp học', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397529/elearningJP/audio/vocab/N5/vocab_n5_229_%E3%82%AF%E3%83%A9%E3%82%B9.mp3', TRUE, '', '', 'クラス', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('グラム', 'グラム', 1, 'gam (gr, đơn vị đo lường); gam', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397530/elearningJP/audio/vocab/N5/vocab_n5_230_%E3%82%B0%E3%83%A9%E3%83%A0.mp3', TRUE, '', '', 'グラム', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('車', 'くるま', 1, 'bánh xe; mô tô; ô tô', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397531/elearningJP/audio/vocab/N5/vocab_n5_231_%E3%81%8F%E3%82%8B%E3%81%BE.mp3', TRUE, '', '', 'kuruma', 'XA', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('黒', 'くろ', 1, 'màu đen; sự có tội', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397533/elearningJP/audio/vocab/N5/vocab_n5_232_%E3%81%8F%E3%82%8D.mp3', TRUE, '', '', 'kuro', 'HẮC', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('黒い', 'くろい', 1, 'đen; u ám; đen tối', '', 'N5', 'ADJECTIVE', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397534/elearningJP/audio/vocab/N5/vocab_n5_233_%E3%81%8F%E3%82%8D%E3%81%84.mp3', TRUE, '', '', 'kuroi', 'HẮC', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('警官', 'けいかん', 1, 'cánh sát; cảnh sát; cánh sát viên', '', 'N5', 'ADJECTIVE', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397537/elearningJP/audio/vocab/N5/vocab_n5_234_%E3%81%91%E3%81%84%E3%81%8B%E3%82%93.mp3', TRUE, '', '', 'keikan', 'CẢNH QUAN', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('今朝', 'けさ', 1, 'hồi sáng; sáng hôm nay; sáng nay', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397539/elearningJP/audio/vocab/N5/vocab_n5_235_%E3%81%91%E3%81%95.mp3', TRUE, '', '', 'kesa', 'KIM TRIÊU', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('消す', 'けす', 1, 'bôi; dụi; tắt', '', 'N5', 'VERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397542/elearningJP/audio/vocab/N5/vocab_n5_236_%E3%81%91%E3%81%99.mp3', TRUE, '', '', 'kesu', 'TIÊU', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('結構', 'けっこう', 1, 'kết cấu; cấu trúc; tạm được; tương đối; kha khá; đủ; được; cũng được', '', 'N5', 'ADJECTIVE', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397543/elearningJP/audio/vocab/N5/vocab_n5_237_%E3%81%91%E3%81%A3%E3%81%93%E3%81%86.mp3', TRUE, '', '', 'kekkou', 'KẾT CẤU', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('結婚', 'けっこん', 1, 'cưới xin; đã lập gia đình; đã có chồng; đã có vợ; đã kết hôn; hôn nhân', '', 'N5', 'VERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397544/elearningJP/audio/vocab/N5/vocab_n5_238_%E3%81%91%E3%81%A3%E3%81%93%E3%82%93.mp3', TRUE, '', '', 'kekkon', 'KẾT HÔN', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('月曜日', 'げつようび', 1, 'ngày thứ hai; thứ Hai', '', 'N5', 'ADVERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397546/elearningJP/audio/vocab/N5/vocab_n5_239_%E3%81%92%E3%81%A4%E3%82%88%E3%81%86%E3%81%B3.mp3', TRUE, '', '', 'getsuyoubi', 'NGUYỆT DIỆU NHẬT', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('玄関', 'げんかん', 1, 'phòng ngoài; lối đi vào; sảnh trong nhà', '', 'N5', 'ADJECTIVE', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397547/elearningJP/audio/vocab/N5/vocab_n5_240_%E3%81%92%E3%82%93%E3%81%8B%E3%82%93.mp3', TRUE, '', '', 'genkan', 'HUYỀN QUAN', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('元気', 'げんき', 1, 'khoẻ; khoẻ mạnh; khoẻ khoắn; sức khoẻ; sự khoẻ mạnh', '', 'N5', 'ADJECTIVE', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397549/elearningJP/audio/vocab/N5/vocab_n5_241_%E3%81%92%E3%82%93%E3%81%8D.mp3', TRUE, '', '', 'genki', 'NGUYÊN KHÍ', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('五', 'ご', 1, 'năm; số 5', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397550/elearningJP/audio/vocab/N5/vocab_n5_242_%E3%81%94.mp3', TRUE, '', '', 'go', 'NGŨ', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('公園', 'こうえん', 1, 'công viên; uyển; vườn', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397551/elearningJP/audio/vocab/N5/vocab_n5_243_%E3%81%93%E3%81%86%E3%81%88%E3%82%93.mp3', TRUE, '', '', 'kouen', 'CÔNG VIÊN', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('交差点', 'こうさてん', 1, 'bùng binh; ngã tư; điểm giao nhau; giao điểm', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397553/elearningJP/audio/vocab/N5/vocab_n5_244_%E3%81%93%E3%81%86%E3%81%95%E3%81%A6%E3%82%93.mp3', TRUE, '', '', 'kousaten', 'GIAO SOA ĐIỂM', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('紅茶', 'こうちゃ', 1, 'chè đen; trà đen; hồng trà', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397554/elearningJP/audio/vocab/N5/vocab_n5_245_%E3%81%93%E3%81%86%E3%81%A1%E3%82%83.mp3', TRUE, '', '', 'koucha', 'HỒNG TRÀ', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('交番', 'こうばん', 1, 'đồn cảnh sát', '', 'N5', 'VERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397556/elearningJP/audio/vocab/N5/vocab_n5_246_%E3%81%93%E3%81%86%E3%81%B0%E3%82%93.mp3', TRUE, '', '', 'kouban', 'GIAO PHIÊN', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('声', 'こえ', 1, 'tiếng; giọng nói; giọng nói', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397557/elearningJP/audio/vocab/N5/vocab_n5_247_%E3%81%93%E3%81%88.mp3', TRUE, '', '', 'koe', 'THANH', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('コート', 'コート', 1, 'áo khoác; áo bành tô; áo choàng', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397558/elearningJP/audio/vocab/N5/vocab_n5_248_%E3%82%B3%E3%83%BC%E3%83%88.mp3', TRUE, '', '', 'コート', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('コーヒー', 'コーヒー', 1, 'cà phê; cà-phê', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397560/elearningJP/audio/vocab/N5/vocab_n5_249_%E3%82%B3%E3%83%BC%E3%83%92%E3%83%BC.mp3', TRUE, '', '', 'コーヒー', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('午後', 'ごご', 1, 'vào buổi chiều; sau 12 giờ trưa; buổi chiều; chiều', '', 'N5', 'ADVERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397561/elearningJP/audio/vocab/N5/vocab_n5_250_%E3%81%94%E3%81%94.mp3', TRUE, '', '', 'gogo', 'NGỌ HẬU', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('九日', 'ここのか', 1, 'mồng 9; ngày 9; ngày mồng 9; 9 ngày', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397563/elearningJP/audio/vocab/N5/vocab_n5_251_%E3%81%93%E3%81%93%E3%81%AE%E3%81%8B.mp3', TRUE, '', '', 'kokonoka', 'CỬU NHẬT', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('九つ', 'ここのつ', 1, '9 cái; 9 chiếc', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397564/elearningJP/audio/vocab/N5/vocab_n5_252_%E3%81%93%E3%81%93%E3%81%AE%E3%81%A4.mp3', TRUE, '', '', 'kokonotsu', 'CỬU', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('午前', 'ごぜん', 1, 'buổi sáng; vào buổi sáng; sáng', '', 'N5', 'ADVERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397565/elearningJP/audio/vocab/N5/vocab_n5_253_%E3%81%94%E3%81%9C%E3%82%93.mp3', TRUE, '', '', 'gozen', 'NGỌ TIỀN', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('答える', 'こたえる', 1, 'trả lời', '', 'N5', 'VERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397567/elearningJP/audio/vocab/N5/vocab_n5_254_%E3%81%93%E3%81%9F%E3%81%88%E3%82%8B.mp3', TRUE, '', '', 'kotaeru', 'ĐÁP', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('こちら', 'こちら', 1, 'phía này; bên này; hướng này; tôi; chúng tôi', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397568/elearningJP/audio/vocab/N5/vocab_n5_255_%E3%81%93%E3%81%A1%E3%82%89.mp3', TRUE, '', '', 'kochira', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('こっち', 'こっち', 1, 'hướng này; phía này; ở đây; đây; này', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397569/elearningJP/audio/vocab/N5/vocab_n5_256_%E3%81%93%E3%81%A3%E3%81%A1.mp3', TRUE, '', '', 'kocchi', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('コップ', 'コップ', 1, 'cái cốc; cái cốc; cúp; cốc; ca; cái ly', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397571/elearningJP/audio/vocab/N5/vocab_n5_257_%E3%82%B3%E3%83%83%E3%83%97.mp3', TRUE, '', '', 'コップ', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('今年', 'ことし', 1, 'năm nay', '', 'N5', 'ADVERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397572/elearningJP/audio/vocab/N5/vocab_n5_258_%E3%81%93%E3%81%A8%E3%81%97.mp3', TRUE, '', '', 'kotoshi', 'KIM NIÊN', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('言葉', 'ことば', 1, 'câu nói; ngôn ngữ; tiếng nói; lời ăn tiếng nói; từ ngữ; lời nói; lời', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397573/elearningJP/audio/vocab/N5/vocab_n5_259_%E3%81%93%E3%81%A8%E3%81%B0.mp3', TRUE, '', '', 'kotoba', 'NGÔN DIỆP', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('子供', 'こども', 1, 'bé con; bé thơ; con', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397575/elearningJP/audio/vocab/N5/vocab_n5_260_%E3%81%93%E3%81%A9%E3%82%82.mp3', TRUE, '', '', 'kodomo', 'TỬ CUNG', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('御飯', 'ごはん', 1, 'cơm; ăn cơm', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397576/elearningJP/audio/vocab/N5/vocab_n5_261_%E3%81%94%E3%81%AF%E3%82%93.mp3', TRUE, '', '', 'gohan', 'NGỰ PHẠN', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('コピーする', 'コピーする', 1, 'chép; chép lại; sao chép', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397577/elearningJP/audio/vocab/N5/vocab_n5_262_%E3%82%B3%E3%83%94%E3%83%BC%E3%81%99%E3%82%8B.mp3', TRUE, '', '', 'コピーsuru', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('困る', 'こまる', 1, 'bối rối; khó khăn (về tiền bạc, cuộc sống.v.v...); lúng túng', '', 'N5', 'VERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397579/elearningJP/audio/vocab/N5/vocab_n5_263_%E3%81%93%E3%81%BE%E3%82%8B.mp3', TRUE, '', '', 'komaru', 'KHỐN', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('これ', 'これ', 1, '(used to get the attention of one''s equals or inferiors) hey, oi, yo', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397580/elearningJP/audio/vocab/N5/vocab_n5_264_%E3%81%93%E3%82%8C.mp3', TRUE, '', '', 'kore', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('今月', 'こんげつ', 1, 'tháng này', '', 'N5', 'ADVERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397581/elearningJP/audio/vocab/N5/vocab_n5_265_%E3%81%93%E3%82%93%E3%81%92%E3%81%A4.mp3', TRUE, '', '', 'kongetsu', 'KIM NGUYỆT', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('今週', 'こんしゅう', 1, 'tuần lễ này; tuần này', '', 'N5', 'ADVERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397583/elearningJP/audio/vocab/N5/vocab_n5_266_%E3%81%93%E3%82%93%E3%81%97%E3%82%85%E3%81%86.mp3', TRUE, '', '', 'konshuu', 'KIM CHU', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('こんな', 'こんな', 1, 'như thế này (mức độ, số lượng, trạng thái, tính chất...)', '', 'N5', 'ADJECTIVE', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397584/elearningJP/audio/vocab/N5/vocab_n5_267_%E3%81%93%E3%82%93%E3%81%AA.mp3', TRUE, '', '', 'konna', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('今晩', 'こんばん', 1, 'đêm nay; tối nay', '', 'N5', 'ADVERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397586/elearningJP/audio/vocab/N5/vocab_n5_268_%E3%81%93%E3%82%93%E3%81%B0%E3%82%93.mp3', TRUE, '', '', 'konban', 'KIM VÃN', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('さあ', 'さあ', 1, 'nào; thôi nào; tiếp đi', '', 'N5', 'CONJUNCTION', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397587/elearningJP/audio/vocab/N5/vocab_n5_269_%E3%81%95%E3%81%82.mp3', TRUE, '', '', 'saa', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('財布', 'さいふ', 1, 'bao tượng; bóp; đãy tiền', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397589/elearningJP/audio/vocab/N5/vocab_n5_270_%E3%81%95%E3%81%84%E3%81%B5.mp3', TRUE, '', '', 'saifu', 'TÀI BỐ', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('先', 'さき', 1, 'đầu mút; điểm đầu; tương lai; trước đây', '', 'N5', 'ADJECTIVE', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397590/elearningJP/audio/vocab/N5/vocab_n5_271_%E3%81%95%E3%81%8D.mp3', TRUE, '', '', 'saki', 'TIÊN', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('咲く', 'さく', 1, 'nở', '', 'N5', 'VERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397592/elearningJP/audio/vocab/N5/vocab_n5_272_%E3%81%95%E3%81%8F.mp3', TRUE, '', '', 'saku', 'TIẾU', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('作文', 'さくぶん', 1, 'sự đặt câu; sự viết văn; sự làm văn; đoạn văn', '', 'N5', 'VERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397593/elearningJP/audio/vocab/N5/vocab_n5_273_%E3%81%95%E3%81%8F%E3%81%B6%E3%82%93.mp3', TRUE, '', '', 'sakubun', 'TÁC VĂN', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('差す', 'さす', 1, 'đặt cánh tay của bạn dưới nách người khác', '', 'N5', 'VERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397594/elearningJP/audio/vocab/N5/vocab_n5_274_%E3%81%95%E3%81%99.mp3', TRUE, '', '', 'sasu', 'SOA', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('雑誌', 'ざっし', 1, 'tạp chí; tạp san; tập san', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397596/elearningJP/audio/vocab/N5/vocab_n5_275_%E3%81%96%E3%81%A3%E3%81%97.mp3', TRUE, '', '', 'zasshi', 'TẠP CHÍ', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('砂糖', 'さとう', 1, 'đường; đường (ăn)', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397597/elearningJP/audio/vocab/N5/vocab_n5_276_%E3%81%95%E3%81%A8%E3%81%86.mp3', TRUE, '', '', 'satou', 'SA ĐƯỜNG', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('寒い', 'さむい', 1, 'cóng; hàn; lành lạnh', '', 'N5', 'ADJECTIVE', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397599/elearningJP/audio/vocab/N5/vocab_n5_277_%E3%81%95%E3%82%80%E3%81%84.mp3', TRUE, '', '', 'samui', 'HÀN', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('さ来年', 'さらいねん', 1, 'năm sau nữa; hai năm nữa', '', 'N5', 'ADVERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397600/elearningJP/audio/vocab/N5/vocab_n5_278_%E3%81%95%E3%82%89%E3%81%84%E3%81%AD%E3%82%93.mp3', TRUE, '', '', 'sarainen', 'LAI NIÊN', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('しかし', 'しかし', 1, 'tuy nhiên; nhưng', '', 'N5', 'CONJUNCTION', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397601/elearningJP/audio/vocab/N5/vocab_n5_279_%E3%81%97%E3%81%8B%E3%81%97.mp3', TRUE, '', '', 'shikashi', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('時間', 'じかん', 1, 'giờ; giờ đồng hồ; giờ giấc', '', 'N5', 'ADVERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397603/elearningJP/audio/vocab/N5/vocab_n5_280_%E3%81%98%E3%81%8B%E3%82%93.mp3', TRUE, '', '', 'jikan', 'THỜI GIAN', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('仕事', 'しごと', 1, 'công việc; công của lực', '', 'N5', 'VERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397604/elearningJP/audio/vocab/N5/vocab_n5_281_%E3%81%97%E3%81%94%E3%81%A8.mp3', TRUE, '', '', 'shigoto', 'SĨ SỰ', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('辞書', 'じしょ', 1, 'từ điển; tự điển', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397605/elearningJP/audio/vocab/N5/vocab_n5_282_%E3%81%98%E3%81%97%E3%82%87.mp3', TRUE, '', '', 'jisho', 'TỪ THƯ', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('静か', 'しずか', 1, 'yên tĩnh, yên lặng; điềm tĩnh', '', 'N5', 'ADJECTIVE', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397607/elearningJP/audio/vocab/N5/vocab_n5_283_%E3%81%97%E3%81%9A%E3%81%8B.mp3', TRUE, '', '', 'shizuka', 'TĨNH', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('質問', 'しつもん', 1, 'câu hỏi; chất vấn', '', 'N5', 'VERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397608/elearningJP/audio/vocab/N5/vocab_n5_284_%E3%81%97%E3%81%A4%E3%82%82%E3%82%93.mp3', TRUE, '', '', 'shitsumon', 'CHẤT VẤN', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('自転車', 'じてんしゃ', 1, 'xe đạp', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397609/elearningJP/audio/vocab/N5/vocab_n5_285_%E3%81%98%E3%81%A6%E3%82%93%E3%81%97%E3%82%83.mp3', TRUE, '', '', 'jitensha', 'TỰ CHUYỂN XA', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('自動車', 'じどうしゃ', 1, 'xe con; xe hơi; xe ô tô', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397611/elearningJP/audio/vocab/N5/vocab_n5_286_%E3%81%98%E3%81%A9%E3%81%86%E3%81%97%E3%82%83.mp3', TRUE, '', '', 'jidousha', 'TỰ ĐỘNG XA', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('死ぬ', 'しぬ', 1, 'chết; đi đời; lâm chung', '', 'N5', 'VERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397612/elearningJP/audio/vocab/N5/vocab_n5_287_%E3%81%97%E3%81%AC.mp3', TRUE, '', '', 'shinu', 'TỬ', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('字引', 'じびき', 1, 'từ điển; tự điển', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397613/elearningJP/audio/vocab/N5/vocab_n5_288_%E3%81%98%E3%81%B3%E3%81%8D.mp3', TRUE, '', '', 'jibiki', 'TỰ DẪN', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('自分', 'じぶん', 1, 'bản thân mình; tự mình', '', 'N5', 'ADJECTIVE', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397615/elearningJP/audio/vocab/N5/vocab_n5_289_%E3%81%98%E3%81%B6%E3%82%93.mp3', TRUE, '', '', 'jibun', 'TỰ PHÂN', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('閉まる', 'しまる', 1, 'đóng; bị đóng chặt; buộc chặt', '', 'N5', 'VERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397616/elearningJP/audio/vocab/N5/vocab_n5_290_%E3%81%97%E3%81%BE%E3%82%8B.mp3', TRUE, '', '', 'shimaru', 'BẾ', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('閉める', 'しめる', 1, 'đóng; gài', '', 'N5', 'VERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397617/elearningJP/audio/vocab/N5/vocab_n5_291_%E3%81%97%E3%82%81%E3%82%8B.mp3', TRUE, '', '', 'shimeru', 'BẾ', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('締める', 'しめる', 1, 'buộc; buộc chặt; vặn chặt; kín', '', 'N5', 'VERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397619/elearningJP/audio/vocab/N5/vocab_n5_292_%E3%81%97%E3%82%81%E3%82%8B.mp3', TRUE, '', '', 'shimeru', 'ĐẾ', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('じゃ', 'じゃ', 1, 'thế thì; vậy thì', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397620/elearningJP/audio/vocab/N5/vocab_n5_293_%E3%81%98%E3%82%83.mp3', TRUE, '', '', 'ja', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('写真', 'しゃしん', 1, 'ảnh; bóng; hình ảnh', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397621/elearningJP/audio/vocab/N5/vocab_n5_294_%E3%81%97%E3%82%83%E3%81%97%E3%82%93.mp3', TRUE, '', '', 'shashin', 'TẢ CHÂN', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('シャツ', 'シャツ', 1, 'áo sơ mi; áo cánh; sơ mi', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397623/elearningJP/audio/vocab/N5/vocab_n5_295_%E3%82%B7%E3%83%A3%E3%83%84.mp3', TRUE, '', '', 'シャツ', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('シャワー', 'シャワー', 1, 'buồng tắm vòi hoa sen; vòi hoa sen', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397624/elearningJP/audio/vocab/N5/vocab_n5_296_%E3%82%B7%E3%83%A3%E3%83%AF%E3%83%BC.mp3', TRUE, '', '', 'シャワー', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('授業', 'じゅぎょう', 1, 'buổi học, giờ học; sự giảng bài; sự lên lớp', '', 'N5', 'VERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397626/elearningJP/audio/vocab/N5/vocab_n5_297_%E3%81%98%E3%82%85%E3%81%8E%E3%82%87%E3%81%86.mp3', TRUE, '', '', 'jugyou', 'THỤ NGHIỆP', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('宿題', 'しゅくだい', 1, 'bài tập về nhà', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397628/elearningJP/audio/vocab/N5/vocab_n5_298_%E3%81%97%E3%82%85%E3%81%8F%E3%81%A0%E3%81%84.mp3', TRUE, '', '', 'shukudai', 'TÚC ĐỀ', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('しょうゆ', 'しょうゆ', 1, 'xì dầu', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397629/elearningJP/audio/vocab/N5/vocab_n5_299_%E3%81%97%E3%82%87%E3%81%86%E3%82%86.mp3', TRUE, '', '', 'shouyu', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('食堂', 'しょくどう', 1, 'buồng ăn; nhà ăn; bếp ăn; phòng ăn (tại một ngôi chùa)', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397631/elearningJP/audio/vocab/N5/vocab_n5_300_%E3%81%97%E3%82%87%E3%81%8F%E3%81%A9%E3%81%86.mp3', TRUE, '', '', 'shokudou', 'THỰC ĐƯỜNG', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('知る', 'しる', 1, 'biết; biết (có kinh nghiệm); biết (mặt)', '', 'N5', 'VERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397632/elearningJP/audio/vocab/N5/vocab_n5_301_%E3%81%97%E3%82%8B.mp3', TRUE, '', '', 'shiru', 'TRI', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('白', 'しろ', 1, 'bên trắng; màu trắng; người da trắng', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397633/elearningJP/audio/vocab/N5/vocab_n5_302_%E3%81%97%E3%82%8D.mp3', TRUE, '', '', 'shiro', 'BẠCH', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('白い', 'しろい', 1, 'màu trắng; sạch sẽ; trắng muốt; trắng', '', 'N5', 'ADJECTIVE', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397635/elearningJP/audio/vocab/N5/vocab_n5_303_%E3%81%97%E3%82%8D%E3%81%84.mp3', TRUE, '', '', 'shiroi', 'BẠCH', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('新聞', 'しんぶん', 1, 'báo; tờ báo; nhật báo; tờ báo', '', 'N5', 'ADJECTIVE', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397637/elearningJP/audio/vocab/N5/vocab_n5_304_%E3%81%97%E3%82%93%E3%81%B6%E3%82%93.mp3', TRUE, '', '', 'shinbun', 'TÂN VĂN', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('水曜日', 'すいようび', 1, 'ngày thứ tư; thứ tư', '', 'N5', 'ADVERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397638/elearningJP/audio/vocab/N5/vocab_n5_305_%E3%81%99%E3%81%84%E3%82%88%E3%81%86%E3%81%B3.mp3', TRUE, '', '', 'suiyoubi', 'THỦY DIỆU NHẬT', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('吸う', 'すう', 1, 'bú; hấp; hít; hít vào; hút', '', 'N5', 'VERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397639/elearningJP/audio/vocab/N5/vocab_n5_306_%E3%81%99%E3%81%86.mp3', TRUE, '', '', 'suu', 'HẤP', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('スカート', 'スカート', 1, 'váy; váy; juýp; cáp scart', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397641/elearningJP/audio/vocab/N5/vocab_n5_307_%E3%82%B9%E3%82%AB%E3%83%BC%E3%83%88.mp3', TRUE, '', '', 'スカート', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('好き', 'すき', 1, 'sự thích; yêu; quý; mến', '', 'N5', 'ADJECTIVE', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397642/elearningJP/audio/vocab/N5/vocab_n5_308_%E3%81%99%E3%81%8D.mp3', TRUE, '', '', 'suki', 'HẢO', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('少ない', 'すくない', 1, 'ít; hiếm; thiểu', '', 'N5', 'ADJECTIVE', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397643/elearningJP/audio/vocab/N5/vocab_n5_309_%E3%81%99%E3%81%8F%E3%81%AA%E3%81%84.mp3', TRUE, '', '', 'sukunai', 'THIỂU', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('すぐに', 'すぐに', 1, 'ngay khi; ngay lập tức, tức thì, trực tiếp', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397645/elearningJP/audio/vocab/N5/vocab_n5_310_%E3%81%99%E3%81%90%E3%81%AB.mp3', TRUE, '', '', 'suguni', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('少し', 'すこし', 1, 'chút đỉnh; chút ít; hơi', '', 'N5', 'ADVERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397646/elearningJP/audio/vocab/N5/vocab_n5_311_%E3%81%99%E3%81%93%E3%81%97.mp3', TRUE, '', '', 'sukoshi', 'THIỂU', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('涼しい', 'すずしい', 1, 'bình tĩnh; mát; mát mẻ', '', 'N5', 'ADJECTIVE', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397647/elearningJP/audio/vocab/N5/vocab_n5_312_%E3%81%99%E3%81%9A%E3%81%97%E3%81%84.mp3', TRUE, '', '', 'suzushii', 'LƯƠNG', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('ストーブ', 'ストーブ', 1, 'lò; lò sưởi', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397649/elearningJP/audio/vocab/N5/vocab_n5_313_%E3%82%B9%E3%83%88%E3%83%BC%E3%83%96.mp3', TRUE, '', '', 'ストーブ', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('スプーン', 'スプーン', 1, 'cái muỗng; cái thìa; muỗng', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397650/elearningJP/audio/vocab/N5/vocab_n5_314_%E3%82%B9%E3%83%97%E3%83%BC%E3%83%B3.mp3', TRUE, '', '', 'スプーン', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('スポーツ', 'スポーツ', 1, 'thể thao', '', 'N5', 'ADJECTIVE', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397651/elearningJP/audio/vocab/N5/vocab_n5_315_%E3%82%B9%E3%83%9D%E3%83%BC%E3%83%84.mp3', TRUE, '', '', 'スポーツ', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('ズボン', 'ズボン', 1, 'quần; quần; quần dài', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397653/elearningJP/audio/vocab/N5/vocab_n5_316_%E3%82%BA%E3%83%9C%E3%83%B3.mp3', TRUE, '', '', 'ズボン', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('住む', 'すむ', 1, 'có thể giải quyết; có thể đối phó được; cư trú; ở; trả nợ; trả xong', '', 'N5', 'VERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397654/elearningJP/audio/vocab/N5/vocab_n5_317_%E3%81%99%E3%82%80.mp3', TRUE, '', '', 'sumu', 'TRỤ', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('スリッパ', 'スリッパ', 1, 'dép đi trong nhà; hài', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397655/elearningJP/audio/vocab/N5/vocab_n5_318_%E3%82%B9%E3%83%AA%E3%83%83%E3%83%91.mp3', TRUE, '', '', 'スリッパ', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('する', 'する', 1, 'làm; thực hiện', '', 'N5', 'VERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397657/elearningJP/audio/vocab/N5/vocab_n5_319_%E3%81%99%E3%82%8B.mp3', TRUE, '', '', 'suru', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('座る', 'すわる', 1, 'ngồi; ngồi xuống', '', 'N5', 'VERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397658/elearningJP/audio/vocab/N5/vocab_n5_320_%E3%81%99%E3%82%8F%E3%82%8B.mp3', TRUE, '', '', 'suwaru', 'TỌA', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('生徒', 'せいと', 1, 'học sinh; học trò', '', 'N5', 'ADJECTIVE', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397659/elearningJP/audio/vocab/N5/vocab_n5_321_%E3%81%9B%E3%81%84%E3%81%A8.mp3', TRUE, '', '', 'seito', 'SANH ĐỒ', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('セーター', 'セーター', 1, 'áo len chui đầu; áo len dài tay', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397661/elearningJP/audio/vocab/N5/vocab_n5_322_%E3%82%BB%E3%83%BC%E3%82%BF%E3%83%BC.mp3', TRUE, '', '', 'セーター', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('せっけん', 'せっけん', 1, 'bánh xà phòng; xà bông; xà phòng', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397663/elearningJP/audio/vocab/N5/vocab_n5_323_%E3%81%9B%E3%81%A3%E3%81%91%E3%82%93.mp3', TRUE, '', '', 'sekken', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('背広', 'せびろ', 1, 'bộ com lê', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397664/elearningJP/audio/vocab/N5/vocab_n5_324_%E3%81%9B%E3%81%B3%E3%82%8D.mp3', TRUE, '', '', 'sebiro', 'BỐI QUẢNG', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('狭い', 'せまい', 1, 'bé; chật; chật chội', '', 'N5', 'ADJECTIVE', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397665/elearningJP/audio/vocab/N5/vocab_n5_325_%E3%81%9B%E3%81%BE%E3%81%84.mp3', TRUE, '', '', 'semai', 'HIỆP', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('ゼロ', 'ゼロ', 1, 'số không; số không; sự không có gì', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397666/elearningJP/audio/vocab/N5/vocab_n5_326_%E3%82%BC%E3%83%AD.mp3', TRUE, '', '', 'ゼロ', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('千', 'せん', 1, 'một nghìn; ngàn; nghìn', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397668/elearningJP/audio/vocab/N5/vocab_n5_327_%E3%81%9B%E3%82%93.mp3', TRUE, '', '', 'sen', 'THIÊN', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('先月', 'せんげつ', 1, 'tháng trước', '', 'N5', 'ADVERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397669/elearningJP/audio/vocab/N5/vocab_n5_328_%E3%81%9B%E3%82%93%E3%81%92%E3%81%A4.mp3', TRUE, '', '', 'sengetsu', 'TIÊN NGUYỆT', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('先週', 'せんしゅう', 1, 'tuần lễ trước; tuần trước', '', 'N5', 'ADVERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397670/elearningJP/audio/vocab/N5/vocab_n5_329_%E3%81%9B%E3%82%93%E3%81%97%E3%82%85%E3%81%86.mp3', TRUE, '', '', 'senshuu', 'TIÊN CHU', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('先生', 'せんせい', 1, 'giáo viên; giảng viên; thầy; ông giáo; ông thầy', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397672/elearningJP/audio/vocab/N5/vocab_n5_330_%E3%81%9B%E3%82%93%E3%81%9B%E3%81%84.mp3', TRUE, '', '', 'sensei', 'TIÊN SANH', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('洗濯', 'せんたく', 1, 'sự giặt giũ; quần áo được giặt giũ', '', 'N5', 'VERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397673/elearningJP/audio/vocab/N5/vocab_n5_331_%E3%81%9B%E3%82%93%E3%81%9F%E3%81%8F.mp3', TRUE, '', '', 'sentaku', 'TẨY TRẠC', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('全部', 'ぜんぶ', 1, 'cả thảy; hết cả; hết thảy', '', 'N5', 'ADJECTIVE', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397674/elearningJP/audio/vocab/N5/vocab_n5_332_%E3%81%9C%E3%82%93%E3%81%B6.mp3', TRUE, '', '', 'zenbu', 'TOÀN BỘ', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('そちら', 'そちら', 1, 'nơi đó', '', 'N5', 'ADJECTIVE', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397676/elearningJP/audio/vocab/N5/vocab_n5_333_%E3%81%9D%E3%81%A1%E3%82%89.mp3', TRUE, '', '', 'sochira', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('そっち', 'そっち', 1, 'nơi đó', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397677/elearningJP/audio/vocab/N5/vocab_n5_334_%E3%81%9D%E3%81%A3%E3%81%A1.mp3', TRUE, '', '', 'socchi', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('そば', 'そば', 1, 'phía', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397678/elearningJP/audio/vocab/N5/vocab_n5_335_%E3%81%9D%E3%81%B0.mp3', TRUE, '', '', 'soba', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('それから', 'それから', 1, 'sau đó; từ sau đó', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397680/elearningJP/audio/vocab/N5/vocab_n5_336_%E3%81%9D%E3%82%8C%E3%81%8B%E3%82%89.mp3', TRUE, '', '', 'sorekara', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('それでは', 'それでは', 1, 'trong trường hợp đó; sau đó; vậy thì', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397681/elearningJP/audio/vocab/N5/vocab_n5_337_%E3%81%9D%E3%82%8C%E3%81%A7%E3%81%AF.mp3', TRUE, '', '', 'soredeha', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('大学', 'だいがく', 1, 'đại học; trường đại học', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397682/elearningJP/audio/vocab/N5/vocab_n5_338_%E3%81%A0%E3%81%84%E3%81%8C%E3%81%8F.mp3', TRUE, '', '', 'daigaku', 'ĐẠI HỌC', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('大使館', 'たいしかん', 1, 'đại sứ quán; tòa đại sứ', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397684/elearningJP/audio/vocab/N5/vocab_n5_339_%E3%81%9F%E3%81%84%E3%81%97%E3%81%8B%E3%82%93.mp3', TRUE, '', '', 'taishikan', 'ĐẠI SỬ QUÁN', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('大丈夫', 'だいじょうぶ', 1, 'an toàn; chắc chắn; được; ổn; ok', '', 'N5', 'ADJECTIVE', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397685/elearningJP/audio/vocab/N5/vocab_n5_340_%E3%81%A0%E3%81%84%E3%81%98%E3%82%87%E3%81%86%E3%81%B6.mp3', TRUE, '', '', 'daijoubu', 'ĐẠI TRƯỢNG PHU', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('大好き', 'だいすき', 1, 'rất thích', '', 'N5', 'ADJECTIVE', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397686/elearningJP/audio/vocab/N5/vocab_n5_341_%E3%81%A0%E3%81%84%E3%81%99%E3%81%8D.mp3', TRUE, '', '', 'daisuki', 'ĐẠI HẢO', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('大切', 'たいせつ', 1, 'quan trọng; sự quan trọng', '', 'N5', 'ADJECTIVE', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397689/elearningJP/audio/vocab/N5/vocab_n5_342_%E3%81%9F%E3%81%84%E3%81%9B%E3%81%A4.mp3', TRUE, '', '', 'taisetsu', 'ĐẠI THIẾT', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('台所', 'だいどころ', 1, 'bếp; bếp núc; bếp nước', '', 'N5', 'ADJECTIVE', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397691/elearningJP/audio/vocab/N5/vocab_n5_343_%E3%81%A0%E3%81%84%E3%81%A9%E3%81%93%E3%82%8D.mp3', TRUE, '', '', 'daidokoro', 'THAI SỞ', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('たいへん', 'たいへん', 1, 'opposite side', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397692/elearningJP/audio/vocab/N5/vocab_n5_344_%E3%81%9F%E3%81%84%E3%81%B8%E3%82%93.mp3', TRUE, '', '', 'taihen', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('高い', 'たかい', 1, 'cao; đắt; đắt tiền', '', 'N5', 'ADJECTIVE', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397693/elearningJP/audio/vocab/N5/vocab_n5_345_%E3%81%9F%E3%81%8B%E3%81%84.mp3', TRUE, '', '', 'takai', 'CAO', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('たくさん', 'たくさん', 1, 'đủ; nhiều', '', 'N5', 'ADJECTIVE', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397695/elearningJP/audio/vocab/N5/vocab_n5_346_%E3%81%9F%E3%81%8F%E3%81%95%E3%82%93.mp3', TRUE, '', '', 'takusan', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('タクシー', 'タクシー', 1, 'tắc xi; tắc-xi; xe tắc xi', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397696/elearningJP/audio/vocab/N5/vocab_n5_347_%E3%82%BF%E3%82%AF%E3%82%B7%E3%83%BC.mp3', TRUE, '', '', 'タクシー', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('出す', 'だす', 1, 'gửi đi; cho ra khỏi; xuất bản', '', 'N5', 'VERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397697/elearningJP/audio/vocab/N5/vocab_n5_348_%E3%81%A0%E3%81%99.mp3', TRUE, '', '', 'dasu', 'XUẤT', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('立つ', 'たつ', 1, 'đứng; đứng lên; đứng dậy', '', 'N5', 'VERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397699/elearningJP/audio/vocab/N5/vocab_n5_349_%E3%81%9F%E3%81%A4.mp3', TRUE, '', '', 'tatsu', 'LẬP', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('たて', 'たて', 1, 'làm tươi; chỉ cần làm', '', 'N5', 'ADJECTIVE', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397701/elearningJP/audio/vocab/N5/vocab_n5_350_%E3%81%9F%E3%81%A6.mp3', TRUE, '', '', 'tate', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('建物', 'たてもの', 1, 'tòa nhà; ngôi nhà; công trình kiến trúc', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397702/elearningJP/audio/vocab/N5/vocab_n5_351_%E3%81%9F%E3%81%A6%E3%82%82%E3%81%AE.mp3', TRUE, '', '', 'tatemono', 'KIẾN VẬT', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('楽しい', 'たのしい', 1, 'dí dỏm; khoái ý; sướng', '', 'N5', 'ADJECTIVE', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397703/elearningJP/audio/vocab/N5/vocab_n5_352_%E3%81%9F%E3%81%AE%E3%81%97%E3%81%84.mp3', TRUE, '', '', 'tanoshii', 'LẠC', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('頼む', 'たのむ', 1, 'Nhờ', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397705/elearningJP/audio/vocab/N5/vocab_n5_353_%E3%81%9F%E3%81%AE%E3%82%80.mp3', TRUE, '', '', 'tanomu', 'LẠI', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('たばこ', 'たばこ', 1, 'điếu thuốc; thuốc; thuốc lá', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397706/elearningJP/audio/vocab/N5/vocab_n5_354_%E3%81%9F%E3%81%B0%E3%81%93.mp3', TRUE, '', '', 'tabako', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('たぶん', 'たぶん', 1, 'đa phần; rất nhiều; rất lớn', '', 'N5', 'ADVERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397707/elearningJP/audio/vocab/N5/vocab_n5_355_%E3%81%9F%E3%81%B6%E3%82%93.mp3', TRUE, '', '', 'tabun', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('食べ物', 'たべもの', 1, 'đồ ăn; món ăn; thức', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397709/elearningJP/audio/vocab/N5/vocab_n5_356_%E3%81%9F%E3%81%B9%E3%82%82%E3%81%AE.mp3', TRUE, '', '', 'tabemono', 'THỰC VẬT', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('食べる', 'たべる', 1, 'ăn', '', 'N5', 'VERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397710/elearningJP/audio/vocab/N5/vocab_n5_357_%E3%81%9F%E3%81%B9%E3%82%8B.mp3', TRUE, '', '', 'taberu', 'THỰC', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('卵', 'たまご', 1, 'trứng; quả trứng; noãn; tế bào trứng', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397711/elearningJP/audio/vocab/N5/vocab_n5_358_%E3%81%9F%E3%81%BE%E3%81%94.mp3', TRUE, '', '', 'tamago', 'NOÃN', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('誕生日', 'たんじょうび', 1, 'ngày sinh; ngày sinh nhật', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397713/elearningJP/audio/vocab/N5/vocab_n5_359_%E3%81%9F%E3%82%93%E3%81%98%E3%82%87%E3%81%86%E3%81%B3.mp3', TRUE, '', '', 'tanjoubi', 'ĐẢN SANH NHẬT', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('だんだん', 'だんだん', 1, 'thank you (dialect from the Izumo region of Shimane Prefecture)', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397714/elearningJP/audio/vocab/N5/vocab_n5_360_%E3%81%A0%E3%82%93%E3%81%A0%E3%82%93.mp3', TRUE, '', '', 'dandan', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('小さい', 'ちいさい', 1, 'bé; bé bỏng; bé nhỏ', '', 'N5', 'ADJECTIVE', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397716/elearningJP/audio/vocab/N5/vocab_n5_361_%E3%81%A1%E3%81%84%E3%81%95%E3%81%84.mp3', TRUE, '', '', 'chiisai', 'TIỂU', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('小さな', 'ちいさな', 1, 'small, little, tiny', '', 'N5', 'ADJECTIVE', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397717/elearningJP/audio/vocab/N5/vocab_n5_362_%E3%81%A1%E3%81%84%E3%81%95%E3%81%AA.mp3', TRUE, '', '', 'chiisana', 'TIỂU', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('近い', 'ちかい', 1, 'cận; gần; cạnh; kề sát; ngay cạnh; ngay sát; giống như; gần như; tương tự', '', 'N5', 'ADJECTIVE', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397718/elearningJP/audio/vocab/N5/vocab_n5_363_%E3%81%A1%E3%81%8B%E3%81%84.mp3', TRUE, '', '', 'chikai', 'CẬN', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('違う', 'ちがう', 1, 'khác; khác nhau; không giống; trái ngược; không phù hợp; lầm lẫn; sai', '', 'N5', 'VERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397720/elearningJP/audio/vocab/N5/vocab_n5_364_%E3%81%A1%E3%81%8C%E3%81%86.mp3', TRUE, '', '', 'chigau', 'VI', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('近く', 'ちかく', 1, 'cận; gần; ở gần; cạnh; kề; kề bên; ngay cạnh; ngay sát; hàng xóm', '', 'N5', 'ADVERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397721/elearningJP/audio/vocab/N5/vocab_n5_365_%E3%81%A1%E3%81%8B%E3%81%8F.mp3', TRUE, '', '', 'chikaku', 'CẬN', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('地下鉄', 'ちかてつ', 1, 'tàu điện ngầm; xe điện ngầm', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397722/elearningJP/audio/vocab/N5/vocab_n5_366_%E3%81%A1%E3%81%8B%E3%81%A6%E3%81%A4.mp3', TRUE, '', '', 'chikatetsu', 'ĐỊA HẠ THIẾT', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('地図', 'ちず', 1, 'bản đồ; địa đồ', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397724/elearningJP/audio/vocab/N5/vocab_n5_367_%E3%81%A1%E3%81%9A.mp3', TRUE, '', '', 'chizu', 'ĐỊA ĐỒ', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('茶色', 'ちゃいろ', 1, 'màu nâu nhạt', '', 'N5', 'ADJECTIVE', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397725/elearningJP/audio/vocab/N5/vocab_n5_368_%E3%81%A1%E3%82%83%E3%81%84%E3%82%8D.mp3', TRUE, '', '', 'chairo', 'TRÀ SẮC', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('ちゃわん', 'ちゃわん', 1, 'bát cơm; chén uống chè', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397726/elearningJP/audio/vocab/N5/vocab_n5_369_%E3%81%A1%E3%82%83%E3%82%8F%E3%82%93.mp3', TRUE, '', '', 'chawan', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('ちょうど', 'ちょうど', 1, 'vừa đúng; vừa chuẩn', '', 'N5', 'ADJECTIVE', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397728/elearningJP/audio/vocab/N5/vocab_n5_370_%E3%81%A1%E3%82%87%E3%81%86%E3%81%A9.mp3', TRUE, '', '', 'choudo', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('ちょっと', 'ちょっと', 1, 'một chút; một lát; một lúc; hơi hơi', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397729/elearningJP/audio/vocab/N5/vocab_n5_371_%E3%81%A1%E3%82%87%E3%81%A3%E3%81%A8.mp3', TRUE, '', '', 'chotto', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('使う', 'つかう', 1, 'dụng; sử dụng; dùng; xài', '', 'N5', 'VERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397730/elearningJP/audio/vocab/N5/vocab_n5_372_%E3%81%A4%E3%81%8B%E3%81%86.mp3', TRUE, '', '', 'tsukau', 'SỬ', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('疲れる', 'つかれる', 1, 'cũ rồi; mệt; mệt mỏi', '', 'N5', 'VERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397732/elearningJP/audio/vocab/N5/vocab_n5_373_%E3%81%A4%E3%81%8B%E3%82%8C%E3%82%8B.mp3', TRUE, '', '', 'tsukareru', 'BÌ', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('着く', 'つく', 1, 'đến (một địa điểm); tới; vào (vị trí)', '', 'N5', 'VERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397733/elearningJP/audio/vocab/N5/vocab_n5_374_%E3%81%A4%E3%81%8F.mp3', TRUE, '', '', 'tsuku', 'TRỨ', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('机', 'つくえ', 1, 'bàn; bàn viết, bàn (dùng để học bài, làm việc công sở...)', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397735/elearningJP/audio/vocab/N5/vocab_n5_375_%E3%81%A4%E3%81%8F%E3%81%88.mp3', TRUE, '', '', 'tsukue', 'KY', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('つける', 'つける', 1, 'ngâm (quần áo,...); tẩm, ướp (gia vị...)', '', 'N5', 'VERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397736/elearningJP/audio/vocab/N5/vocab_n5_376_%E3%81%A4%E3%81%91%E3%82%8B.mp3', TRUE, '', '', 'tsukeru', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('勤める', 'つとめる', 1, 'làm việc', '', 'N5', 'VERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397737/elearningJP/audio/vocab/N5/vocab_n5_377_%E3%81%A4%E3%81%A8%E3%82%81%E3%82%8B.mp3', TRUE, '', '', 'tsutomeru', 'CẦN', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('つまらない', 'つまらない', 1, 'chán; không ra cái gì; không đáng gì', '', 'N5', 'ADJECTIVE', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397739/elearningJP/audio/vocab/N5/vocab_n5_378_%E3%81%A4%E3%81%BE%E3%82%89%E3%81%AA%E3%81%84.mp3', TRUE, '', '', 'tsumaranai', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('冷たい', 'つめたい', 1, 'lành lạnh; lạnh nhạt; lạnh lùng; lạnh; lạnh buốt; lạnh giá; lạnh lẽo', '', 'N5', 'ADJECTIVE', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397740/elearningJP/audio/vocab/N5/vocab_n5_379_%E3%81%A4%E3%82%81%E3%81%9F%E3%81%84.mp3', TRUE, '', '', 'tsumetai', 'LÃNH', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('強い', 'つよい', 1, 'khỏe; mạnh; khoẻ; bền; tốt', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397741/elearningJP/audio/vocab/N5/vocab_n5_380_%E3%81%A4%E3%82%88%E3%81%84.mp3', TRUE, '', '', 'tsuyoi', 'CƯỜNG', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('手', 'て', 1, 'bàn tay; tay', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397743/elearningJP/audio/vocab/N5/vocab_n5_381_%E3%81%A6.mp3', TRUE, '', '', 'te', 'THỦ', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('テープ', 'テープ', 1, 'băng cát sét; video; băng; dải dây; băng dính', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397744/elearningJP/audio/vocab/N5/vocab_n5_382_%E3%83%86%E3%83%BC%E3%83%97.mp3', TRUE, '', '', 'テープ', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('テーブル', 'テーブル', 1, 'bàn; bàn; cái bàn', '', 'N5', 'ADJECTIVE', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397746/elearningJP/audio/vocab/N5/vocab_n5_383_%E3%83%86%E3%83%BC%E3%83%96%E3%83%AB.mp3', TRUE, '', '', 'テーブル', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('テープレコーダー', 'テープレコーダー', 1, 'máy ghi âm; máy hát; máy thu băng', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397748/elearningJP/audio/vocab/N5/vocab_n5_384_%E3%83%86%E3%83%BC%E3%83%97%E3%83%AC%E3%82%B3%E3%83%BC%E3%83%80%E3%83%BC.mp3', TRUE, '', '', 'テープレコーダー', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('出かける', 'でかける', 1, 'đăng trình; ra; rời khỏi', '', 'N5', 'VERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397749/elearningJP/audio/vocab/N5/vocab_n5_385_%E3%81%A7%E3%81%8B%E3%81%91%E3%82%8B.mp3', TRUE, '', '', 'dekakeru', 'XUẤT', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('手紙', 'てがみ', 1, 'bức thơ; bức thư; phong thơ', '', 'N5', 'ADJECTIVE', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397750/elearningJP/audio/vocab/N5/vocab_n5_386_%E3%81%A6%E3%81%8C%E3%81%BF.mp3', TRUE, '', '', 'tegami', 'THỦ CHỈ', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('出口', 'でぐち', 1, 'cổng ra; cửa ra; lối ra', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397752/elearningJP/audio/vocab/N5/vocab_n5_387_%E3%81%A7%E3%81%90%E3%81%A1.mp3', TRUE, '', '', 'deguchi', 'XUẤT KHẨU', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('テスト', 'テスト', 1, 'bài kiểm tra; cuộc thí nghiệm; sự kiểm tra; thử; thí nghiệm', '', 'N5', 'VERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397753/elearningJP/audio/vocab/N5/vocab_n5_388_%E3%83%86%E3%82%B9%E3%83%88.mp3', TRUE, '', '', 'テスト', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('デパート', 'デパート', 1, 'trung tâm thương mại', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397754/elearningJP/audio/vocab/N5/vocab_n5_389_%E3%83%87%E3%83%91%E3%83%BC%E3%83%88.mp3', TRUE, '', '', 'デパート', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('出る', 'でる', 1, 'đi ra; ngoi; xuất hiện; đi ra khỏi', '', 'N5', 'VERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397756/elearningJP/audio/vocab/N5/vocab_n5_390_%E3%81%A7%E3%82%8B.mp3', TRUE, '', '', 'deru', 'XUẤT', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('テレビ', 'テレビ', 1, 'máy tuốc bin; máy vô tuyến truyền hình; ti vi; vô tuyến', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397757/elearningJP/audio/vocab/N5/vocab_n5_391_%E3%83%86%E3%83%AC%E3%83%93.mp3', TRUE, '', '', 'テレビ', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('天気', 'てんき', 1, 'thời tiết', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397759/elearningJP/audio/vocab/N5/vocab_n5_392_%E3%81%A6%E3%82%93%E3%81%8D.mp3', TRUE, '', '', 'tenki', 'THIÊN KHÍ', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('電気', 'でんき', 1, 'điện khí; điện; đèn điện; điện', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397760/elearningJP/audio/vocab/N5/vocab_n5_393_%E3%81%A7%E3%82%93%E3%81%8D.mp3', TRUE, '', '', 'denki', 'ĐIỆN KHÍ', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('電車', 'でんしゃ', 1, 'tàu điện; tàu lửa; xe điện', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397761/elearningJP/audio/vocab/N5/vocab_n5_394_%E3%81%A7%E3%82%93%E3%81%97%E3%82%83.mp3', TRUE, '', '', 'densha', 'ĐIỆN XA', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('電話', 'でんわ', 1, 'điện thoại; máy điện thoại', '', 'N5', 'VERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397766/elearningJP/audio/vocab/N5/vocab_n5_395_%E3%81%A7%E3%82%93%E3%82%8F.mp3', TRUE, '', '', 'denwa', 'ĐIỆN THOẠI', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('ドア', 'ドア', 1, 'cửa; cửa; cửa ra vào; cánh cửa ra vào', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397768/elearningJP/audio/vocab/N5/vocab_n5_396_%E3%83%89%E3%82%A2.mp3', TRUE, '', '', 'ドア', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('トイレ', 'トイレ', 1, 'cầu tiên; toa-lét; nhà vệ sinh', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397769/elearningJP/audio/vocab/N5/vocab_n5_397_%E3%83%88%E3%82%A4%E3%83%AC.mp3', TRUE, '', '', 'トイレ', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('どうして', 'どうして', 1, 'ái chà; kì thực; như thế nào', '', 'N5', 'ADVERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397770/elearningJP/audio/vocab/N5/vocab_n5_398_%E3%81%A9%E3%81%86%E3%81%97%E3%81%A6.mp3', TRUE, '', '', 'doushite', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('どうぞ', 'どうぞ', 1, 'xin mời', '', 'N5', 'ADVERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397772/elearningJP/audio/vocab/N5/vocab_n5_399_%E3%81%A9%E3%81%86%E3%81%9E.mp3', TRUE, '', '', 'douzo', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('動物', 'どうぶつ', 1, 'động vật; muông thú; súc vật', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397773/elearningJP/audio/vocab/N5/vocab_n5_400_%E3%81%A9%E3%81%86%E3%81%B6%E3%81%A4.mp3', TRUE, '', '', 'doubutsu', 'ĐỘNG VẬT', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('どうも', 'どうも', 1, 'Xin chào; Cảm ơn; hơi hơi; có vẻ; mập mờ', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397775/elearningJP/audio/vocab/N5/vocab_n5_401_%E3%81%A9%E3%81%86%E3%82%82.mp3', TRUE, '', '', 'doumo', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('遠い', 'とおい', 1, 'hẻo; viễn; xa lắc', '', 'N5', 'ADJECTIVE', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397776/elearningJP/audio/vocab/N5/vocab_n5_402_%E3%81%A8%E3%81%8A%E3%81%84.mp3', TRUE, '', '', 'tooi', 'VIỄN', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('十日', 'とおか', 1, 'mười ngày; ngày mùng mười; ngày mười', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397778/elearningJP/audio/vocab/N5/vocab_n5_403_%E3%81%A8%E3%81%8A%E3%81%8B.mp3', TRUE, '', '', 'tooka', 'THẬP NHẬT', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('時々', 'ときどき', 1, 'có lúc; thỉnh thoảng; đôi khi; lắm khi', '', 'N5', 'ADJECTIVE', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397779/elearningJP/audio/vocab/N5/vocab_n5_404_%E3%81%A8%E3%81%8D%E3%81%A9%E3%81%8D.mp3', TRUE, '', '', 'tokidoki', 'THÌ', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('時計', 'とけい', 1, 'đồng hồ', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397780/elearningJP/audio/vocab/N5/vocab_n5_405_%E3%81%A8%E3%81%91%E3%81%84.mp3', TRUE, '', '', 'tokei', 'THÌ KẾ', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('どこ', 'どこ', 1, 'ở đâu; ở chỗ nào', '', 'N5', 'ADJECTIVE', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397782/elearningJP/audio/vocab/N5/vocab_n5_406_%E3%81%A9%E3%81%93.mp3', TRUE, '', '', 'doko', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('所', 'ところ', 1, 'nơi; chỗ', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397783/elearningJP/audio/vocab/N5/vocab_n5_407_%E3%81%A8%E3%81%93%E3%82%8D.mp3', TRUE, '', '', 'tokoro', 'SỞ', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('年', 'とし', 1, 'năm; năm tháng; tuổi', '', 'N5', 'ADVERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397784/elearningJP/audio/vocab/N5/vocab_n5_408_%E3%81%A8%E3%81%97.mp3', TRUE, '', '', 'toshi', 'NIÊN', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('図書館', 'としょかん', 1, 'thư quán; thư viện', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397786/elearningJP/audio/vocab/N5/vocab_n5_409_%E3%81%A8%E3%81%97%E3%82%87%E3%81%8B%E3%82%93.mp3', TRUE, '', '', 'toshokan', 'ĐỒ THƯ QUÁN', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('どちら', 'どちら', 1, 'phía nào; cái nào; người nào', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397787/elearningJP/audio/vocab/N5/vocab_n5_410_%E3%81%A9%E3%81%A1%E3%82%89.mp3', TRUE, '', '', 'dochira', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('どっち', 'どっち', 1, 'phía nào', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397788/elearningJP/audio/vocab/N5/vocab_n5_411_%E3%81%A9%E3%81%A3%E3%81%A1.mp3', TRUE, '', '', 'docchi', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('とても', 'とても', 1, 'rất; cực kỳ', '', 'N5', 'ADVERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397790/elearningJP/audio/vocab/N5/vocab_n5_412_%E3%81%A8%E3%81%A6%E3%82%82.mp3', TRUE, '', '', 'totemo', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('どなた', 'どなた', 1, 'vị nào', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397791/elearningJP/audio/vocab/N5/vocab_n5_413_%E3%81%A9%E3%81%AA%E3%81%9F.mp3', TRUE, '', '', 'donata', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('隣', 'となり', 1, 'bên cạnh; cạnh; sự giáp bên; sự ngay bên cạnh', '', 'N5', 'ADJECTIVE', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397792/elearningJP/audio/vocab/N5/vocab_n5_414_%E3%81%A8%E3%81%AA%E3%82%8A.mp3', TRUE, '', '', 'tonari', 'LÂN', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('どの', 'どの', 1, 'cung điện; lâu đài', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397794/elearningJP/audio/vocab/N5/vocab_n5_415_%E3%81%A9%E3%81%AE.mp3', TRUE, '', '', 'dono', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('飛ぶ', 'とぶ', 1, 'bay nhảy; bay tán loạn; bay lả tả; bay; cất cánh bay; bay liệng', '', 'N5', 'VERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397795/elearningJP/audio/vocab/N5/vocab_n5_416_%E3%81%A8%E3%81%B6.mp3', TRUE, '', '', 'tobu', 'PHI', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('友達', 'ともだち', 1, 'bạn; bạn bè; bè bạn', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397796/elearningJP/audio/vocab/N5/vocab_n5_417_%E3%81%A8%E3%82%82%E3%81%A0%E3%81%A1.mp3', TRUE, '', '', 'tomodachi', 'HỮU ĐẠT', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('土曜日', 'どようび', 1, 'bảy; ngày thứ bẩy; Thứ bảy', '', 'N5', 'ADVERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397798/elearningJP/audio/vocab/N5/vocab_n5_418_%E3%81%A9%E3%82%88%E3%81%86%E3%81%B3.mp3', TRUE, '', '', 'doyoubi', 'THỔ DIỆU NHẬT', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('鳥', 'とり', 1, 'chim chóc; chim; gia cầm; điểu', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397799/elearningJP/audio/vocab/N5/vocab_n5_419_%E3%81%A8%E3%82%8A.mp3', TRUE, '', '', 'tori', 'ĐIỂU', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('とり肉', 'とりにく', 1, 'Thịt gà', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397801/elearningJP/audio/vocab/N5/vocab_n5_420_%E3%81%A8%E3%82%8A%E3%81%AB%E3%81%8F.mp3', TRUE, '', '', 'toriniku', 'NHỤ', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('取る', 'とる', 1, 'bắt giữ; biểu thị; biểu quyết; cầm lấy', '', 'N5', 'VERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397802/elearningJP/audio/vocab/N5/vocab_n5_421_%E3%81%A8%E3%82%8B.mp3', TRUE, '', '', 'toru', 'THỦ', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('撮る', 'とる', 1, 'chụp (ảnh); làm (phim)', '', 'N5', 'VERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397803/elearningJP/audio/vocab/N5/vocab_n5_422_%E3%81%A8%E3%82%8B.mp3', TRUE, '', '', 'toru', 'TOÁT', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('ナイフ', 'ナイフ', 1, 'con dao; dao; dao; dao nhíp', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397805/elearningJP/audio/vocab/N5/vocab_n5_423_%E3%83%8A%E3%82%A4%E3%83%95.mp3', TRUE, '', '', 'ナイフ', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('長い', 'ながい', 1, 'bao lâu; dài; lâu', '', 'N5', 'ADJECTIVE', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397806/elearningJP/audio/vocab/N5/vocab_n5_424_%E3%81%AA%E3%81%8C%E3%81%84.mp3', TRUE, '', '', 'nagai', 'TRƯỜNG', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('鳴く', 'なく', 1, 'kêu; hót; hú; rống', '', 'N5', 'VERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397808/elearningJP/audio/vocab/N5/vocab_n5_425_%E3%81%AA%E3%81%8F.mp3', TRUE, '', '', 'naku', 'MINH', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('無くす', 'なくす', 1, 'đánh mất, làm mất; loại bỏ, bỏ ra, loại ra', '', 'N5', 'VERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397810/elearningJP/audio/vocab/N5/vocab_n5_426_%E3%81%AA%E3%81%8F%E3%81%99.mp3', TRUE, '', '', 'nakusu', 'VÔ', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('なぜ', 'なぜ', 1, 'vì sao', '', 'N5', 'ADVERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397811/elearningJP/audio/vocab/N5/vocab_n5_427_%E3%81%AA%E3%81%9C.mp3', TRUE, '', '', 'naze', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('夏', 'なつ', 1, 'hạ; mùa hè; mùa hạ; mùa hè (theo lịch âm dương: ngày 16 của tháng thứ 4 đến ngày 15 của tháng thứ 7)', '', 'N5', 'ADVERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397812/elearningJP/audio/vocab/N5/vocab_n5_428_%E3%81%AA%E3%81%A4.mp3', TRUE, '', '', 'natsu', 'HẠ', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('夏休み', 'なつやすみ', 1, 'nghỉ hè', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397814/elearningJP/audio/vocab/N5/vocab_n5_429_%E3%81%AA%E3%81%A4%E3%82%84%E3%81%99%E3%81%BF.mp3', TRUE, '', '', 'natsuyasumi', 'HẠ HƯU', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('など', 'など', 1, 'Vân vân', '', 'N5', 'PARTICLE', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397815/elearningJP/audio/vocab/N5/vocab_n5_430_%E3%81%AA%E3%81%A9.mp3', TRUE, '', '', 'nado', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('七つ', 'ななつ', 1, 'bảy cái', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397816/elearningJP/audio/vocab/N5/vocab_n5_431_%E3%81%AA%E3%81%AA%E3%81%A4.mp3', TRUE, '', '', 'nanatsu', 'THẤT', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('名前', 'なまえ', 1, 'tên; họ tên', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397818/elearningJP/audio/vocab/N5/vocab_n5_432_%E3%81%AA%E3%81%BE%E3%81%88.mp3', TRUE, '', '', 'namae', 'DANH TIỀN', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('習う', 'ならう', 1, 'học tập; luyện tập; học', '', 'N5', 'VERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397819/elearningJP/audio/vocab/N5/vocab_n5_433_%E3%81%AA%E3%82%89%E3%81%86.mp3', TRUE, '', '', 'narau', 'TẬP', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('並ぶ', 'ならぶ', 1, 'được xếp; được bài trí', '', 'N5', 'VERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397820/elearningJP/audio/vocab/N5/vocab_n5_434_%E3%81%AA%E3%82%89%E3%81%B6.mp3', TRUE, '', '', 'narabu', 'TỊNH', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('並べる', 'ならべる', 1, 'bày; sắp hàng; sắp; bày; bày đặt; bài trí', '', 'N5', 'VERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397822/elearningJP/audio/vocab/N5/vocab_n5_435_%E3%81%AA%E3%82%89%E3%81%B9%E3%82%8B.mp3', TRUE, '', '', 'naraberu', 'TỊNH', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('なる', 'なる', 1, 'that is in; who is called, that is called; that is', '', 'N5', 'ADJECTIVE', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397823/elearningJP/audio/vocab/N5/vocab_n5_436_%E3%81%AA%E3%82%8B.mp3', TRUE, '', '', 'naru', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('二', 'に', 1, 'hai; số hai', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397824/elearningJP/audio/vocab/N5/vocab_n5_437_%E3%81%AB.mp3', TRUE, '', '', 'ni', 'NHỊ', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('賑やか', 'にぎやか', 1, 'sôi nổi; náo nhiệt; sống động; huyên náo', '', 'N5', 'ADJECTIVE', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397825/elearningJP/audio/vocab/N5/vocab_n5_438_%E3%81%AB%E3%81%8E%E3%82%84%E3%81%8B.mp3', TRUE, '', '', 'nigiyaka', 'CHẨN', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('肉', 'にく', 1, 'thịt', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397827/elearningJP/audio/vocab/N5/vocab_n5_439_%E3%81%AB%E3%81%8F.mp3', TRUE, '', '', 'niku', 'NHỤC', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('西', 'にし', 1, 'hướng tây; phía tây', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397828/elearningJP/audio/vocab/N5/vocab_n5_440_%E3%81%AB%E3%81%97.mp3', TRUE, '', '', 'nishi', 'TÂY', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('日曜日', 'にちようび', 1, 'Chủ Nhật; ngày Chủ Nhật; chúa nhật', '', 'N5', 'ADVERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397831/elearningJP/audio/vocab/N5/vocab_n5_441_%E3%81%AB%E3%81%A1%E3%82%88%E3%81%86%E3%81%B3.mp3', TRUE, '', '', 'nichiyoubi', 'NHẬT DIỆU NHẬT', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('荷物', 'にもつ', 1, 'hành lý', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397832/elearningJP/audio/vocab/N5/vocab_n5_442_%E3%81%AB%E3%82%82%E3%81%A4.mp3', TRUE, '', '', 'nimotsu', 'HÀ VẬT', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('ニュース', 'ニュース', 1, 'bản tin; thời sự; thông tin', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397834/elearningJP/audio/vocab/N5/vocab_n5_443_%E3%83%8B%E3%83%A5%E3%83%BC%E3%82%B9.mp3', TRUE, '', '', 'ニュース', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('庭', 'にわ', 1, 'vườn', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397836/elearningJP/audio/vocab/N5/vocab_n5_444_%E3%81%AB%E3%82%8F.mp3', TRUE, '', '', 'niwa', 'ĐÌNH', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('脱ぐ', 'ぬぐ', 1, 'cởi (quần áo, giày); bỏ (mũ); lột', '', 'N5', 'VERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397837/elearningJP/audio/vocab/N5/vocab_n5_445_%E3%81%AC%E3%81%90.mp3', TRUE, '', '', 'nugu', 'THOÁT', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('温い', 'ぬるい', 1, 'nguội; âm ấm', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397838/elearningJP/audio/vocab/N5/vocab_n5_446_%E3%81%AC%E3%82%8B%E3%81%84.mp3', TRUE, '', '', 'nurui', 'ÔN', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('ネクタイ', 'ネクタイ', 1, 'ca vát; cavát; caravát; cà vạt', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397840/elearningJP/audio/vocab/N5/vocab_n5_447_%E3%83%8D%E3%82%AF%E3%82%BF%E3%82%A4.mp3', TRUE, '', '', 'ネクタイ', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('猫', 'ねこ', 1, 'mèo', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397841/elearningJP/audio/vocab/N5/vocab_n5_448_%E3%81%AD%E3%81%93.mp3', TRUE, '', '', 'neko', 'MIÊU', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('寝る', 'ねる', 1, 'đặt lưng; đặt mình; nằm', '', 'N5', 'VERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397843/elearningJP/audio/vocab/N5/vocab_n5_449_%E3%81%AD%E3%82%8B.mp3', TRUE, '', '', 'neru', 'TẨM', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('ノート', 'ノート', 1, 'máy vi tính xách tay; sổ; vở', '', 'N5', 'VERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397844/elearningJP/audio/vocab/N5/vocab_n5_450_%E3%83%8E%E3%83%BC%E3%83%88.mp3', TRUE, '', '', 'ノート', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('登る', 'のぼる', 1, 'được đưa ra; được đặt ra (trong chương trình); được thăng chức; giương buồm', '', 'N5', 'VERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397845/elearningJP/audio/vocab/N5/vocab_n5_451_%E3%81%AE%E3%81%BC%E3%82%8B.mp3', TRUE, '', '', 'noboru', 'ĐĂNG', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('飲み物', 'のみもの', 1, 'đồ uống; thức uống', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397847/elearningJP/audio/vocab/N5/vocab_n5_452_%E3%81%AE%E3%81%BF%E3%82%82%E3%81%AE.mp3', TRUE, '', '', 'nomimono', 'ẨM VẬT', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('飲む', 'のむ', 1, 'húp; uống', '', 'N5', 'VERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397848/elearningJP/audio/vocab/N5/vocab_n5_453_%E3%81%AE%E3%82%80.mp3', TRUE, '', '', 'nomu', 'ẨM', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('乗る', 'のる', 1, 'cưỡi; lên xe; lên tàu; đi (tàu, xe); vào (nhịp); có hứng', '', 'N5', 'VERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397850/elearningJP/audio/vocab/N5/vocab_n5_454_%E3%81%AE%E3%82%8B.mp3', TRUE, '', '', 'noru', 'THỪA', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('歯', 'は', 1, 'răng', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397852/elearningJP/audio/vocab/N5/vocab_n5_455_%E3%81%AF.mp3', TRUE, '', '', 'ha', 'XỈ', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('パーティー', 'パーティー', 1, 'bữa tiệc; buổi tiệc; liên hoan', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397853/elearningJP/audio/vocab/N5/vocab_n5_456_%E3%83%91%E3%83%BC%E3%83%86%E3%82%A3%E3%83%BC.mp3', TRUE, '', '', 'パーティー', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('葉書', 'はがき', 1, 'bưu thiếp', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397855/elearningJP/audio/vocab/N5/vocab_n5_457_%E3%81%AF%E3%81%8C%E3%81%8D.mp3', TRUE, '', '', 'hagaki', 'DIỆP THƯ', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('はく', 'はく', 1, 'măc, mang, đeo', '', 'N5', 'VERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397872/elearningJP/audio/vocab/N5/vocab_n5_458_%E3%81%AF%E3%81%8F.mp3', TRUE, '', '', 'haku', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('箱', 'はこ', 1, 'hòm; hộp; kiện hàng', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397874/elearningJP/audio/vocab/N5/vocab_n5_459_%E3%81%AF%E3%81%93.mp3', TRUE, '', '', 'hako', 'TƯƠNG', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('橋', 'はし', 1, 'cầu; cầu não', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397876/elearningJP/audio/vocab/N5/vocab_n5_460_%E3%81%AF%E3%81%97.mp3', TRUE, '', '', 'hashi', 'KIỀU', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('はし', 'はし', 1, 'bờ; cạnh, lề; chót', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397877/elearningJP/audio/vocab/N5/vocab_n5_461_%E3%81%AF%E3%81%97.mp3', TRUE, '', '', 'hashi', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('始まる', 'はじまる', 1, 'bắt đầu; khởi đầu; 開始する; 遡る', '', 'N5', 'VERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397880/elearningJP/audio/vocab/N5/vocab_n5_462_%E3%81%AF%E3%81%98%E3%81%BE%E3%82%8B.mp3', TRUE, '', '', 'hajimaru', 'THỦY', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('初め', 'はじめ', 1, 'ban đầu; lần đầu; khởi đầu', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397881/elearningJP/audio/vocab/N5/vocab_n5_463_%E3%81%AF%E3%81%98%E3%82%81.mp3', TRUE, '', '', 'hajime', 'SƠ', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('初めて', 'はじめて', 1, 'lần đầu tiên; mới', '', 'N5', 'ADJECTIVE', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397883/elearningJP/audio/vocab/N5/vocab_n5_464_%E3%81%AF%E3%81%98%E3%82%81%E3%81%A6.mp3', TRUE, '', '', 'hajimete', 'SƠ', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('走る', 'はしる', 1, 'chạy; tẩu', '', 'N5', 'VERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397886/elearningJP/audio/vocab/N5/vocab_n5_465_%E3%81%AF%E3%81%97%E3%82%8B.mp3', TRUE, '', '', 'hashiru', 'TẨU', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('バス', 'バス', 1, 'đàn công-trơ-bas; giọng trầm; giọng nam trầm; sự tắm rửa; sự tắm bồn', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397888/elearningJP/audio/vocab/N5/vocab_n5_466_%E3%83%90%E3%82%B9.mp3', TRUE, '', '', 'バス', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('バター', 'バター', 1, 'bơ', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397890/elearningJP/audio/vocab/N5/vocab_n5_467_%E3%83%90%E3%82%BF%E3%83%BC.mp3', TRUE, '', '', 'バター', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('二十歳', 'はたち', 1, 'đôi mươi; hai mươi tuổi', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397892/elearningJP/audio/vocab/N5/vocab_n5_468_%E3%81%AF%E3%81%9F%E3%81%A1.mp3', TRUE, '', '', 'hatachi', 'NHỊ THẬP TUẾ', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('働く', 'はたらく', 1, 'làm lụng; lao động; hoạt động; phạm (tội); làm việc; làm', '', 'N5', 'VERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397893/elearningJP/audio/vocab/N5/vocab_n5_469_%E3%81%AF%E3%81%9F%E3%82%89%E3%81%8F.mp3', TRUE, '', '', 'hataraku', 'ĐỘNG', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('八', 'はち', 1, 'bát; bát quác; tám', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397895/elearningJP/audio/vocab/N5/vocab_n5_470_%E3%81%AF%E3%81%A1.mp3', TRUE, '', '', 'hachi', 'BÁT', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('二十日', 'はつか', 1, 'ngày hai mươi; hai mươi ngày', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397896/elearningJP/audio/vocab/N5/vocab_n5_471_%E3%81%AF%E3%81%A4%E3%81%8B.mp3', TRUE, '', '', 'hatsuka', 'NHỊ THẬP NHẬT', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('花', 'はな', 1, 'bông hoa; đóa hoa; hoa', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397897/elearningJP/audio/vocab/N5/vocab_n5_472_%E3%81%AF%E3%81%AA.mp3', TRUE, '', '', 'hana', 'HOA', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('鼻', 'はな', 1, 'mũi', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397898/elearningJP/audio/vocab/N5/vocab_n5_473_%E3%81%AF%E3%81%AA.mp3', TRUE, '', '', 'hana', 'TỊ', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('話', 'はなし', 1, 'câu chuyện; sự nói chuyện; sự hội thoại', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397900/elearningJP/audio/vocab/N5/vocab_n5_474_%E3%81%AF%E3%81%AA%E3%81%97.mp3', TRUE, '', '', 'hanashi', 'THOẠI', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('話す', 'はなす', 1, 'bàn tán; chuyện; chuyện trò', '', 'N5', 'VERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397901/elearningJP/audio/vocab/N5/vocab_n5_475_%E3%81%AF%E3%81%AA%E3%81%99.mp3', TRUE, '', '', 'hanasu', 'THOẠI', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('早い', 'はやい', 1, 'sớm; nhanh chóng ( thời gian)', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397903/elearningJP/audio/vocab/N5/vocab_n5_476_%E3%81%AF%E3%82%84%E3%81%84.mp3', TRUE, '', '', 'hayai', 'TẢO', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('速い', 'はやい', 1, 'chóng; lẹ; mau lẹ', '', 'N5', 'ADJECTIVE', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397904/elearningJP/audio/vocab/N5/vocab_n5_477_%E3%81%AF%E3%82%84%E3%81%84.mp3', TRUE, '', '', 'hayai', 'TỐC', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('春', 'はる', 1, 'mùa xuân; xuân', '', 'N5', 'ADVERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397906/elearningJP/audio/vocab/N5/vocab_n5_478_%E3%81%AF%E3%82%8B.mp3', TRUE, '', '', 'haru', 'XUÂN', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('貼る', 'はる', 1, 'dán; gắn cho; gắn', '', 'N5', 'VERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397907/elearningJP/audio/vocab/N5/vocab_n5_479_%E3%81%AF%E3%82%8B.mp3', TRUE, '', '', 'haru', 'THIẾP', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('晴れ', 'はれ', 1, 'trời nắng', '', 'N5', 'ADJECTIVE', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397909/elearningJP/audio/vocab/N5/vocab_n5_480_%E3%81%AF%E3%82%8C.mp3', TRUE, '', '', 'hare', 'TÌNH', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('晴れる', 'はれる', 1, 'nắng; tạnh', '', 'N5', 'VERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397910/elearningJP/audio/vocab/N5/vocab_n5_481_%E3%81%AF%E3%82%8C%E3%82%8B.mp3', TRUE, '', '', 'hareru', 'TÌNH', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('半', 'はん', 1, 'bán; một nửa; nửa', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397912/elearningJP/audio/vocab/N5/vocab_n5_482_%E3%81%AF%E3%82%93.mp3', TRUE, '', '', 'han', 'BÁN', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('晩', 'ばん', 1, 'buổi tối; đêm; muộn', '', 'N5', 'ADVERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397913/elearningJP/audio/vocab/N5/vocab_n5_483_%E3%81%B0%E3%82%93.mp3', TRUE, '', '', 'ban', 'VÃN', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('パン', 'パン', 1, 'bánh mì; bánh mỳ; chảo; cái chảo', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397917/elearningJP/audio/vocab/N5/vocab_n5_484_%E3%83%91%E3%83%B3.mp3', TRUE, '', '', 'パン', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('ハンカチ', 'ハンカチ', 1, 'khăn mùi xoa; khăn tay; mùi xoa', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397918/elearningJP/audio/vocab/N5/vocab_n5_485_%E3%83%8F%E3%83%B3%E3%82%AB%E3%83%81.mp3', TRUE, '', '', 'ハンカチ', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('番号', 'ばんごう', 1, 'số hiệu; số liệu', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397919/elearningJP/audio/vocab/N5/vocab_n5_486_%E3%81%B0%E3%82%93%E3%81%94%E3%81%86.mp3', TRUE, '', '', 'bangou', 'PHIÊN HÀO', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('晩御飯', 'ばんごはん', 1, 'bữa tối; cơm chiều; cơm tối', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397921/elearningJP/audio/vocab/N5/vocab_n5_487_%E3%81%B0%E3%82%93%E3%81%94%E3%81%AF%E3%82%93.mp3', TRUE, '', '', 'bangohan', 'VÃN NGỰ PHẠN', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('半分', 'はんぶん', 1, 'một nửa', '', 'N5', 'ADVERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397923/elearningJP/audio/vocab/N5/vocab_n5_488_%E3%81%AF%E3%82%93%E3%81%B6%E3%82%93.mp3', TRUE, '', '', 'hanbun', 'BÁN PHÂN', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('引く', 'ひく', 1, 'chăng; dẫn; kéo; rút; bị (cảm); tra', '', 'N5', 'VERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397925/elearningJP/audio/vocab/N5/vocab_n5_489_%E3%81%B2%E3%81%8F.mp3', TRUE, '', '', 'hiku', 'DẪN', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('低い', 'ひくい', 1, 'lè tè; thấp', '', 'N5', 'ADJECTIVE', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397926/elearningJP/audio/vocab/N5/vocab_n5_490_%E3%81%B2%E3%81%8F%E3%81%84.mp3', TRUE, '', '', 'hikui', 'ĐÊ', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('飛行機', 'ひこうき', 1, 'máy bay; phi cơ; tàu bay', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397927/elearningJP/audio/vocab/N5/vocab_n5_491_%E3%81%B2%E3%81%93%E3%81%86%E3%81%8D.mp3', TRUE, '', '', 'hikouki', 'PHI HÀNH KI', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('左', 'ひだり', 1, 'bên trái; tả; trái', '', 'N5', 'ADJECTIVE', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397930/elearningJP/audio/vocab/N5/vocab_n5_492_%E3%81%B2%E3%81%A0%E3%82%8A.mp3', TRUE, '', '', 'hidari', 'TẢ', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('一つ', 'ひとつ', 1, 'một', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397933/elearningJP/audio/vocab/N5/vocab_n5_493_%E3%81%B2%E3%81%A8%E3%81%A4.mp3', TRUE, '', '', 'hitotsu', 'NHẤT', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('一月', 'ひとつき', 1, 'tháng giêng; tháng Một', '', 'N5', 'ADVERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397935/elearningJP/audio/vocab/N5/vocab_n5_494_%E3%81%B2%E3%81%A8%E3%81%A4%E3%81%8D.mp3', TRUE, '', '', 'hitotsuki', 'NHẤT NGUYỆT', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('百', 'ひゃく', 1, 'một trăm; trăm', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397937/elearningJP/audio/vocab/N5/vocab_n5_495_%E3%81%B2%E3%82%83%E3%81%8F.mp3', TRUE, '', '', 'hyaku', 'BÁCH', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('病院', 'びょういん', 1, 'bệnh viện; nhà thương; sinh bệnh', '', 'N5', 'ADJECTIVE', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397938/elearningJP/audio/vocab/N5/vocab_n5_496_%E3%81%B3%E3%82%87%E3%81%86%E3%81%84%E3%82%93.mp3', TRUE, '', '', 'byouin', 'BỆNH VIỆN', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('病気', 'びょうき', 1, 'bệnh tật; bệnh; sự ốm; bịnh; đau ốm', '', 'N5', 'VERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397939/elearningJP/audio/vocab/N5/vocab_n5_497_%E3%81%B3%E3%82%87%E3%81%86%E3%81%8D.mp3', TRUE, '', '', 'byouki', 'BỆNH KHÍ', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('昼', 'ひる', 1, 'ban trưa; buổi trưa; ban ngày; trưa', '', 'N5', 'ADVERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397941/elearningJP/audio/vocab/N5/vocab_n5_498_%E3%81%B2%E3%82%8B.mp3', TRUE, '', '', 'hiru', 'TRÚ', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('昼御飯', 'ひるごはん', 1, 'bữa trưa; cơm trưa', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397942/elearningJP/audio/vocab/N5/vocab_n5_499_%E3%81%B2%E3%82%8B%E3%81%94%E3%81%AF%E3%82%93.mp3', TRUE, '', '', 'hirugohan', 'TRÚ NGỰ PHẠN', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('広い', 'ひろい', 1, 'rộng; rộng rãi; rộng lớn', '', 'N5', 'ADJECTIVE', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397943/elearningJP/audio/vocab/N5/vocab_n5_500_%E3%81%B2%E3%82%8D%E3%81%84.mp3', TRUE, '', '', 'hiroi', 'QUẢNG', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('フィルム', 'フィルム', 1, 'phim; cuộn phim (ảnh...)', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397946/elearningJP/audio/vocab/N5/vocab_n5_501_%E3%83%95%E3%82%A3%E3%83%AB%E3%83%A0.mp3', TRUE, '', '', 'フィルム', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('封筒', 'ふうとう', 1, 'bao thư; phong bì; phong thơ', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397948/elearningJP/audio/vocab/N5/vocab_n5_502_%E3%81%B5%E3%81%86%E3%81%A8%E3%81%86.mp3', TRUE, '', '', 'fuutou', 'PHONG ĐỒNG', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('プール', 'プール', 1, 'bể; bể bơi; vùng chứa, vùng trữ', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397950/elearningJP/audio/vocab/N5/vocab_n5_503_%E3%83%97%E3%83%BC%E3%83%AB.mp3', TRUE, '', '', 'プール', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('フォーク', 'フォーク', 1, 'cái nĩa; dân ca; dân gian; dĩa; nĩa', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397951/elearningJP/audio/vocab/N5/vocab_n5_504_%E3%83%95%E3%82%A9%E3%83%BC%E3%82%AF.mp3', TRUE, '', '', 'フォーク', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('吹く', 'ふく', 1, 'dậy mùi; hắt hiu; phát ra; bốc ra; tỏa ra', '', 'N5', 'VERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397953/elearningJP/audio/vocab/N5/vocab_n5_505_%E3%81%B5%E3%81%8F.mp3', TRUE, '', '', 'fuku', 'XUY', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('服', 'ふく', 1, 'quần áo; bộ', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397956/elearningJP/audio/vocab/N5/vocab_n5_506_%E3%81%B5%E3%81%8F.mp3', TRUE, '', '', 'fuku', 'PHỤC', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('二つ', 'ふたつ', 1, 'hai', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397957/elearningJP/audio/vocab/N5/vocab_n5_507_%E3%81%B5%E3%81%9F%E3%81%A4.mp3', TRUE, '', '', 'futatsu', 'NHỊ', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('豚肉', 'ぶたにく', 1, 'thịt heo; thịt lợn', '', 'N5', 'ADJECTIVE', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397959/elearningJP/audio/vocab/N5/vocab_n5_508_%E3%81%B6%E3%81%9F%E3%81%AB%E3%81%8F.mp3', TRUE, '', '', 'butaniku', 'ĐỒN NHỤC', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('二日', 'ふつか', 1, 'ngày mùng hai', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397961/elearningJP/audio/vocab/N5/vocab_n5_509_%E3%81%B5%E3%81%A4%E3%81%8B.mp3', TRUE, '', '', 'futsuka', 'NHỊ NHẬT', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('太い', 'ふとい', 1, 'béo; dày; to; mập', '', 'N5', 'ADJECTIVE', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397962/elearningJP/audio/vocab/N5/vocab_n5_510_%E3%81%B5%E3%81%A8%E3%81%84.mp3', TRUE, '', '', 'futoi', 'THÁI', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('冬', 'ふゆ', 1, 'đông; mùa đông', '', 'N5', 'ADVERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397964/elearningJP/audio/vocab/N5/vocab_n5_511_%E3%81%B5%E3%82%86.mp3', TRUE, '', '', 'fuyu', 'ĐÔNG', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('降る', 'ふる', 1, 'rơi (mưa); đổ (mưa)', '', 'N5', 'VERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397965/elearningJP/audio/vocab/N5/vocab_n5_512_%E3%81%B5%E3%82%8B.mp3', TRUE, '', '', 'furu', 'HÀNG', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('古い', 'ふるい', 1, 'cũ; cổ; già', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397966/elearningJP/audio/vocab/N5/vocab_n5_513_%E3%81%B5%E3%82%8B%E3%81%84.mp3', TRUE, '', '', 'furui', 'CỔ', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('ふろ', 'ふろ', 1, 'bể tắm; bồn tắm', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397968/elearningJP/audio/vocab/N5/vocab_n5_514_%E3%81%B5%E3%82%8D.mp3', TRUE, '', '', 'furo', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('文章', 'ぶんしょう', 1, 'đoạn văn (đôi khi chỉ là một câu văn); văn chương', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397969/elearningJP/audio/vocab/N5/vocab_n5_515_%E3%81%B6%E3%82%93%E3%81%97%E3%82%87%E3%81%86.mp3', TRUE, '', '', 'bunshou', 'VĂN CHƯƠNG', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('ページ', 'ページ', 1, 'trang; trang (sách vở)', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397971/elearningJP/audio/vocab/N5/vocab_n5_516_%E3%83%9A%E3%83%BC%E3%82%B8.mp3', TRUE, '', '', 'ページ', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('下手', 'へた', 1, 'phần phía dưới; vị trí thấp kém; thứ hạng thấp', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397972/elearningJP/audio/vocab/N5/vocab_n5_517_%E3%81%B8%E3%81%9F.mp3', TRUE, '', '', 'heta', 'HẠ THỦ', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('ベッド', 'ベッド', 1, 'giường; giường ngủ; giường', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397973/elearningJP/audio/vocab/N5/vocab_n5_518_%E3%83%99%E3%83%83%E3%83%89.mp3', TRUE, '', '', 'ベッド', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('ペット', 'ペット', 1, 'động vật cảnh; người được cưng chiều', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397975/elearningJP/audio/vocab/N5/vocab_n5_519_%E3%83%9A%E3%83%83%E3%83%88.mp3', TRUE, '', '', 'ペット', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('部屋', 'へや', 1, 'buồng; căn buồng; phòng', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397976/elearningJP/audio/vocab/N5/vocab_n5_520_%E3%81%B8%E3%82%84.mp3', TRUE, '', '', 'heya', 'BỘ ỐC', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('辺', 'へん', 1, 'cạnh (hình học); nơi xa; nơi hẻo lánh; trình độ; mức độ', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397978/elearningJP/audio/vocab/N5/vocab_n5_521_%E3%81%B8%E3%82%93.mp3', TRUE, '', '', 'hen', 'BIÊN', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('ペン', 'ペン', 1, 'bút; bút máy; bút; bút máy', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397979/elearningJP/audio/vocab/N5/vocab_n5_522_%E3%83%9A%E3%83%B3.mp3', TRUE, '', '', 'ペン', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('便利', 'べんり', 1, 'thuận tiện; tiện lợi', '', 'N5', 'ADJECTIVE', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397980/elearningJP/audio/vocab/N5/vocab_n5_523_%E3%81%B9%E3%82%93%E3%82%8A.mp3', TRUE, '', '', 'benri', 'TIỆN LỢI', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('帽子', 'ぼうし', 1, 'mũ; nón', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397982/elearningJP/audio/vocab/N5/vocab_n5_524_%E3%81%BC%E3%81%86%E3%81%97.mp3', TRUE, '', '', 'boushi', 'MẠO TỬ', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('ボールペン', 'ボールペン', 1, 'bút bi; bút nguyên tử', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397983/elearningJP/audio/vocab/N5/vocab_n5_525_%E3%83%9C%E3%83%BC%E3%83%AB%E3%83%9A%E3%83%B3.mp3', TRUE, '', '', 'ボールペン', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('ほか', 'ほか', 1, 'ngoài', '', 'N5', 'ADJECTIVE', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397984/elearningJP/audio/vocab/N5/vocab_n5_526_%E3%81%BB%E3%81%8B.mp3', TRUE, '', '', 'hoka', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('ポケット', 'ポケット', 1, 'túi; túi áo; túi quần; áo', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397986/elearningJP/audio/vocab/N5/vocab_n5_527_%E3%83%9D%E3%82%B1%E3%83%83%E3%83%88.mp3', TRUE, '', '', 'ポケット', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('欲しい', 'ほしい', 1, 'muốn; mong muốn', '', 'N5', 'ADJECTIVE', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397987/elearningJP/audio/vocab/N5/vocab_n5_528_%E3%81%BB%E3%81%97%E3%81%84.mp3', TRUE, '', '', 'hoshii', 'DỤC', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('ポスト', 'ポスト', 1, 'thùng thư; hòm thư; hộp thư; quá trình post máy tính (power on self test); tự kiểm tra khi nguồn bật (post)', '', 'N5', 'VERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397988/elearningJP/audio/vocab/N5/vocab_n5_529_%E3%83%9D%E3%82%B9%E3%83%88.mp3', TRUE, '', '', 'ポスト', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('細い', 'ほそい', 1, 'thon dài; mảnh mai , mỏng; cảm giác không đủ về số lượng', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397989/elearningJP/audio/vocab/N5/vocab_n5_530_%E3%81%BB%E3%81%9D%E3%81%84.mp3', TRUE, '', '', 'hosoi', 'TẾ', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('ボタン', 'ボタン', 1, 'cúc; khuy; khuy áo', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397991/elearningJP/audio/vocab/N5/vocab_n5_531_%E3%83%9C%E3%82%BF%E3%83%B3.mp3', TRUE, '', '', 'ボタン', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('ホテル', 'ホテル', 1, 'khách sạn', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397992/elearningJP/audio/vocab/N5/vocab_n5_532_%E3%83%9B%E3%83%86%E3%83%AB.mp3', TRUE, '', '', 'ホテル', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('本', 'ほん', 1, 'cái; chiếc; điếu; bông; sách; này; nay', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397993/elearningJP/audio/vocab/N5/vocab_n5_533_%E3%81%BB%E3%82%93.mp3', TRUE, '', '', 'hon', 'BỔN', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('本棚', 'ほんだな', 1, 'giá sách; kệ sách; tủ sách', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397995/elearningJP/audio/vocab/N5/vocab_n5_534_%E3%81%BB%E3%82%93%E3%81%A0%E3%81%AA.mp3', TRUE, '', '', 'hondana', 'BỔN BẰNG', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('ほんとう', 'ほんとう', 1, 'sự tăng vọt (giá cả); sự bùng nổ giá cả', '', 'N5', 'VERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397997/elearningJP/audio/vocab/N5/vocab_n5_535_%E3%81%BB%E3%82%93%E3%81%A8%E3%81%86.mp3', TRUE, '', '', 'hontou', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('毎朝', 'まいあさ', 1, 'hàng sáng; mỗi sáng', '', 'N5', 'ADVERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791397998/elearningJP/audio/vocab/N5/vocab_n5_536_%E3%81%BE%E3%81%84%E3%81%82%E3%81%95.mp3', TRUE, '', '', 'maiasa', 'MỖI TRIÊU', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('毎月', 'まいげつ', 1, 'hàng tháng; mỗi tháng', '', 'N5', 'ADVERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791398000/elearningJP/audio/vocab/N5/vocab_n5_537_%E3%81%BE%E3%81%84%E3%81%92%E3%81%A4.mp3', TRUE, '', '', 'maigetsu', 'MỖI NGUYỆT', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('毎週', 'まいしゅう', 1, 'hàng tuần; mỗi tuần', '', 'N5', 'ADVERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791398001/elearningJP/audio/vocab/N5/vocab_n5_538_%E3%81%BE%E3%81%84%E3%81%97%E3%82%85%E3%81%86.mp3', TRUE, '', '', 'maishuu', 'MỖI CHU', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('毎日', 'まいにち', 1, 'hàng ngày; mỗi ngày; mọi ngày', '', 'N5', 'ADVERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791398003/elearningJP/audio/vocab/N5/vocab_n5_539_%E3%81%BE%E3%81%84%E3%81%AB%E3%81%A1.mp3', TRUE, '', '', 'mainichi', 'MỖI NHẬT', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('毎年', 'まいねん', 1, 'hàng năm; mỗi năm; mọi năm; thường niên', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791398004/elearningJP/audio/vocab/N5/vocab_n5_540_%E3%81%BE%E3%81%84%E3%81%AD%E3%82%93.mp3', TRUE, '', '', 'mainen', 'MỖI NIÊN', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('毎晩', 'まいばん', 1, 'đêm đêm; hàng tối; tối tối', '', 'N5', 'ADVERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791398005/elearningJP/audio/vocab/N5/vocab_n5_541_%E3%81%BE%E3%81%84%E3%81%B0%E3%82%93.mp3', TRUE, '', '', 'maiban', 'MỖI VÃN', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('曲る', 'まがる', 1, 'cong; cúi; ẹo', '', 'N5', 'VERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791398007/elearningJP/audio/vocab/N5/vocab_n5_542_%E3%81%BE%E3%81%8C%E3%82%8B.mp3', TRUE, '', '', 'magaru', 'KHÚC', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('まずい', 'まずい', 1, 'dại dột; không thận trọng; dở; vụng; chán (món ăn); không ngon; khó chịu; xấu', '', 'N5', 'ADJECTIVE', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791398008/elearningJP/audio/vocab/N5/vocab_n5_543_%E3%81%BE%E3%81%9A%E3%81%84.mp3', TRUE, '', '', 'mazui', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('また', 'また', 1, 'cũng; lần nữa', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791398009/elearningJP/audio/vocab/N5/vocab_n5_544_%E3%81%BE%E3%81%9F.mp3', TRUE, '', '', 'mata', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('まだ', 'まだ', 1, 'chưa; vẫn; hơn nữa; bên cạnh đó; vẫn', '', 'N5', 'ADVERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791398012/elearningJP/audio/vocab/N5/vocab_n5_545_%E3%81%BE%E3%81%A0.mp3', TRUE, '', '', 'mada', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('町', 'まち', 1, 'thị trấn; con phố', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791398013/elearningJP/audio/vocab/N5/vocab_n5_546_%E3%81%BE%E3%81%A1.mp3', TRUE, '', '', 'machi', 'ĐINH', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('待つ', 'まつ', 1, 'chờ; chờ đợi; đợi', '', 'N5', 'VERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791398015/elearningJP/audio/vocab/N5/vocab_n5_547_%E3%81%BE%E3%81%A4.mp3', TRUE, '', '', 'matsu', 'ĐÃI', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('まっすぐ', 'まっすぐ', 1, 'thẳng (phía trước); trực tiếp; trụ đứng; đứng thẳng; trung thực; thành thật', '', 'N5', 'ADJECTIVE', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791398016/elearningJP/audio/vocab/N5/vocab_n5_548_%E3%81%BE%E3%81%A3%E3%81%99%E3%81%90.mp3', TRUE, '', '', 'massugu', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('マッチ', 'マッチ', 1, 'phù hợp, hợp; diêm; ngòi cháy; quẹt', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791398017/elearningJP/audio/vocab/N5/vocab_n5_549_%E3%83%9E%E3%83%83%E3%83%81.mp3', TRUE, '', '', 'マッチ', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('窓', 'まど', 1, 'cửa sổ; khoảng trống giá; khoảng trống', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791398018/elearningJP/audio/vocab/N5/vocab_n5_550_%E3%81%BE%E3%81%A9.mp3', TRUE, '', '', 'mado', 'SONG', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('万年筆', 'まんねんひつ', 1, 'bút máy; viết máy', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791398020/elearningJP/audio/vocab/N5/vocab_n5_551_%E3%81%BE%E3%82%93%E3%81%AD%E3%82%93%E3%81%B2%E3%81%A4.mp3', TRUE, '', '', 'mannenhitsu', 'VẠN NIÊN BÚT', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('磨く', 'みがく', 1, 'đánh bóng; làm sáng bóng; mài bóng; mài; đánh; chải; trau chuốt; cải thiện; gọt giũa', '', 'N5', 'VERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791398021/elearningJP/audio/vocab/N5/vocab_n5_552_%E3%81%BF%E3%81%8C%E3%81%8F.mp3', TRUE, '', '', 'migaku', 'MA', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('右', 'みぎ', 1, 'bên phải; phía bên phải; hữu', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791398022/elearningJP/audio/vocab/N5/vocab_n5_553_%E3%81%BF%E3%81%8E.mp3', TRUE, '', '', 'migi', 'HỮU', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('短い', 'みじかい', 1, 'cụt; hụt; ngắn', '', 'N5', 'ADJECTIVE', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791398024/elearningJP/audio/vocab/N5/vocab_n5_554_%E3%81%BF%E3%81%98%E3%81%8B%E3%81%84.mp3', TRUE, '', '', 'mijikai', 'ĐOẢN', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('水', 'みず', 1, 'nước', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791398025/elearningJP/audio/vocab/N5/vocab_n5_555_%E3%81%BF%E3%81%9A.mp3', TRUE, '', '', 'mizu', 'THỦY', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('見せる', 'みせる', 1, 'cho xem; cho thấy; chứng tỏ; bày tỏ', '', 'N5', 'VERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791398027/elearningJP/audio/vocab/N5/vocab_n5_556_%E3%81%BF%E3%81%9B%E3%82%8B.mp3', TRUE, '', '', 'miseru', 'KIẾN', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('道', 'みち', 1, 'con đường; con phố; đạo; đường', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791398028/elearningJP/audio/vocab/N5/vocab_n5_557_%E3%81%BF%E3%81%A1.mp3', TRUE, '', '', 'michi', 'ĐẠO', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('三日', 'みっか', 1, 'ba ngày; ngày mùng ba', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791398031/elearningJP/audio/vocab/N5/vocab_n5_558_%E3%81%BF%E3%81%A3%E3%81%8B.mp3', TRUE, '', '', 'mikka', 'TAM NHẬT', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('三つ', 'みっつ', 1, 'ba', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791398032/elearningJP/audio/vocab/N5/vocab_n5_559_%E3%81%BF%E3%81%A3%E3%81%A4.mp3', TRUE, '', '', 'mittsu', 'TAM', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('緑', 'みどり', 1, 'màu xanh lá cây; xanh', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791398034/elearningJP/audio/vocab/N5/vocab_n5_560_%E3%81%BF%E3%81%A9%E3%82%8A.mp3', TRUE, '', '', 'midori', 'LỤC', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('皆さん', 'みなさん', 1, 'các anh; các vị; tất cả mọi người', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791398035/elearningJP/audio/vocab/N5/vocab_n5_561_%E3%81%BF%E3%81%AA%E3%81%95%E3%82%93.mp3', TRUE, '', '', 'minasan', 'GIAI', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('耳', 'みみ', 1, 'cái tai; tai', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791398036/elearningJP/audio/vocab/N5/vocab_n5_562_%E3%81%BF%E3%81%BF.mp3', TRUE, '', '', 'mimi', 'NHĨ', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('見る 観る', 'みる', 1, 'xem; kiểm tra đánh giá; trông coi; chăm sóc', '', 'N5', 'VERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791398038/elearningJP/audio/vocab/N5/vocab_n5_563_%E3%81%BF%E3%82%8B.mp3', TRUE, '', '', 'miru', 'HIỆN QUAN', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('みんな', 'みんな', 1, 'mọi người; tất cả mọi người', '', 'N5', 'ADVERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791398039/elearningJP/audio/vocab/N5/vocab_n5_564_%E3%81%BF%E3%82%93%E3%81%AA.mp3', TRUE, '', '', 'minna', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('六日', 'むいか', 1, 'ngày thứ sáu', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791398041/elearningJP/audio/vocab/N5/vocab_n5_565_%E3%82%80%E3%81%84%E3%81%8B.mp3', TRUE, '', '', 'muika', 'LỤC NHẬT', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('向こう', 'むこう', 1, 'phía bên kia; mặt bên kia; cạnh bên kia; phía trước; phía đối diện', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791398042/elearningJP/audio/vocab/N5/vocab_n5_566_%E3%82%80%E3%81%93%E3%81%86.mp3', TRUE, '', '', 'mukou', 'HƯỚNG', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('難しい', 'むずかしい', 1, 'khó; khó khăn', '', 'N5', 'ADJECTIVE', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791398043/elearningJP/audio/vocab/N5/vocab_n5_567_%E3%82%80%E3%81%9A%E3%81%8B%E3%81%97%E3%81%84.mp3', TRUE, '', '', 'muzukashii', 'NAN', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('六つ', 'むっつ', 1, 'sáu', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791398045/elearningJP/audio/vocab/N5/vocab_n5_568_%E3%82%80%E3%81%A3%E3%81%A4.mp3', TRUE, '', '', 'muttsu', 'LỤC', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('村', 'むら', 1, 'làng; làng mạc; thôn xã', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791398046/elearningJP/audio/vocab/N5/vocab_n5_569_%E3%82%80%E3%82%89.mp3', TRUE, '', '', 'mura', 'THÔN', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('目', 'め', 1, 'con mắt; mắt', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791398049/elearningJP/audio/vocab/N5/vocab_n5_570_%E3%82%81.mp3', TRUE, '', '', 'me', 'MỤC', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('メートル', 'メートル', 1, 'mét; thuộc về mét; mét (m)', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791398050/elearningJP/audio/vocab/N5/vocab_n5_571_%E3%83%A1%E3%83%BC%E3%83%88%E3%83%AB.mp3', TRUE, '', '', 'メートル', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('もう', 'もう', 1, 'đã, rồi', '', 'N5', 'ADVERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791398064/elearningJP/audio/vocab/N5/vocab_n5_572_%E3%82%82%E3%81%86.mp3', TRUE, '', '', 'mou', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('もう一度', 'もういちど', 1, 'lại; lần nữa; thêm một lần nữa', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791398067/elearningJP/audio/vocab/N5/vocab_n5_573_%E3%82%82%E3%81%86%E3%81%84%E3%81%A1%E3%81%A9.mp3', TRUE, '', '', 'mouichido', 'NHẤT ĐỘ', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('木曜日', 'もくようび', 1, 'ngày thứ năm; thứ năm', '', 'N5', 'ADVERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791398069/elearningJP/audio/vocab/N5/vocab_n5_574_%E3%82%82%E3%81%8F%E3%82%88%E3%81%86%E3%81%B3.mp3', TRUE, '', '', 'mokuyoubi', 'MỘC DIỆU NHẬT', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('持つ', 'もつ', 1, 'cầm; nắm; mang; chịu (phí tổn); đảm nhiệm; có', '', 'N5', 'VERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791398070/elearningJP/audio/vocab/N5/vocab_n5_575_%E3%82%82%E3%81%A4.mp3', TRUE, '', '', 'motsu', 'TRÌ', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('もっと', 'もっと', 1, 'nữa; hơn nữa; thêm', '', 'N5', 'ADVERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791398071/elearningJP/audio/vocab/N5/vocab_n5_576_%E3%82%82%E3%81%A3%E3%81%A8.mp3', TRUE, '', '', 'motto', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('物', 'もの', 1, 'đồ vật; vật', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791398073/elearningJP/audio/vocab/N5/vocab_n5_577_%E3%82%82%E3%81%AE.mp3', TRUE, '', '', 'mono', 'VẬT', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('問題', 'もんだい', 1, 'vấn đề', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791398074/elearningJP/audio/vocab/N5/vocab_n5_578_%E3%82%82%E3%82%93%E3%81%A0%E3%81%84.mp3', TRUE, '', '', 'mondai', 'VẤN ĐỀ', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('八百屋', 'やおや', 1, 'hàng rau; người bán rau quả', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791398076/elearningJP/audio/vocab/N5/vocab_n5_579_%E3%82%84%E3%81%8A%E3%82%84.mp3', TRUE, '', '', 'yaoya', 'BÁT BÁCH ỐC', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('野菜', 'やさい', 1, 'rau; rau củ', '', 'N5', 'ADJECTIVE', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791398077/elearningJP/audio/vocab/N5/vocab_n5_580_%E3%82%84%E3%81%95%E3%81%84.mp3', TRUE, '', '', 'yasai', 'DÃ THÁI', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('易しい', 'やさしい', 1, 'dễ tánh; dễ tính; dễ; dễ dàng', '', 'N5', 'ADJECTIVE', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791398079/elearningJP/audio/vocab/N5/vocab_n5_581_%E3%82%84%E3%81%95%E3%81%97%E3%81%84.mp3', TRUE, '', '', 'yasashii', 'DỊCH', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('安い', 'やすい', 1, 'điềm tĩnh; yên tâm; rẻ; rẻ tiền', '', 'N5', 'ADJECTIVE', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791398080/elearningJP/audio/vocab/N5/vocab_n5_582_%E3%82%84%E3%81%99%E3%81%84.mp3', TRUE, '', '', 'yasui', 'AN', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('休み', 'やすみ', 1, 'nghỉ; vắng mặt', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791398081/elearningJP/audio/vocab/N5/vocab_n5_583_%E3%82%84%E3%81%99%E3%81%BF.mp3', TRUE, '', '', 'yasumi', 'HƯU', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('休む', 'やすむ', 1, 'nghỉ ngơi; nghỉ; vắng mặt; ngủ', '', 'N5', 'VERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791398082/elearningJP/audio/vocab/N5/vocab_n5_584_%E3%82%84%E3%81%99%E3%82%80.mp3', TRUE, '', '', 'yasumu', 'HƯU', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('八つ', 'やっつ', 1, 'tám; thứ tám', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791398084/elearningJP/audio/vocab/N5/vocab_n5_585_%E3%82%84%E3%81%A3%E3%81%A4.mp3', TRUE, '', '', 'yattsu', 'BÁT', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('山', 'やま', 1, 'núi; sơn; ngọn núi', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791398086/elearningJP/audio/vocab/N5/vocab_n5_586_%E3%82%84%E3%81%BE.mp3', TRUE, '', '', 'yama', 'SAN', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('やる', 'やる', 1, 'tưới', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791398087/elearningJP/audio/vocab/N5/vocab_n5_587_%E3%82%84%E3%82%8B.mp3', TRUE, '', '', 'yaru', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('夕方', 'ゆうがた', 1, 'Chiều tà', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791398088/elearningJP/audio/vocab/N5/vocab_n5_588_%E3%82%86%E3%81%86%E3%81%8C%E3%81%9F.mp3', TRUE, '', '', 'yuugata', 'TỊCH PHƯƠNG', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('夕飯', 'ゆうはん', 1, 'bữa ăn tối', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791398090/elearningJP/audio/vocab/N5/vocab_n5_589_%E3%82%86%E3%81%86%E3%81%AF%E3%82%93.mp3', TRUE, '', '', 'yuuhan', 'TỊCH PHẠN', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('郵便局', 'ゆうびんきょく', 1, 'bưu điện', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791398091/elearningJP/audio/vocab/N5/vocab_n5_590_%E3%82%86%E3%81%86%E3%81%B3%E3%82%93%E3%81%8D%E3%82%87%E3%81%8F.mp3', TRUE, '', '', 'yuubinkyoku', 'BƯU TIỆN CỤC', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('昨夜', 'ゆうべ', 1, 'đêm hôm qua; đêm qua; hồi khuya', '', 'N5', 'ADVERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791398092/elearningJP/audio/vocab/N5/vocab_n5_591_%E3%82%86%E3%81%86%E3%81%B9.mp3', TRUE, '', '', 'yuube', 'TẠC DẠ', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('有名', 'ゆうめい', 1, 'hữu danh; sự nổi tiếng; nổi tiếng; có danh', '', 'N5', 'ADJECTIVE', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791398094/elearningJP/audio/vocab/N5/vocab_n5_592_%E3%82%86%E3%81%86%E3%82%81%E3%81%84.mp3', TRUE, '', '', 'yuumei', 'HỮU DANH', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('雪', 'ゆき', 1, 'tuyết', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791398096/elearningJP/audio/vocab/N5/vocab_n5_593_%E3%82%86%E3%81%8D.mp3', TRUE, '', '', 'yuki', 'TUYẾT', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('ゆっくりと', 'ゆっくりと', 1, 'thong thả; từ từ; chậm rãi', '', 'N5', 'ADVERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791398098/elearningJP/audio/vocab/N5/vocab_n5_594_%E3%82%86%E3%81%A3%E3%81%8F%E3%82%8A%E3%81%A8.mp3', TRUE, '', '', 'yukkurito', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('八日', 'ようか', 1, 'ngày tám; mồng tám; tám ngày', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791398101/elearningJP/audio/vocab/N5/vocab_n5_595_%E3%82%88%E3%81%86%E3%81%8B.mp3', TRUE, '', '', 'youka', 'BÁT NHẬT', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('洋服', 'ようふく', 1, 'âu phục; quần áo; quần áo tây', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791398103/elearningJP/audio/vocab/N5/vocab_n5_596_%E3%82%88%E3%81%86%E3%81%B5%E3%81%8F.mp3', TRUE, '', '', 'youfuku', 'DƯƠNG PHỤC', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('よく', 'よく', 1, 'sự mong muốn; sự tham lam; 欲が深く:tham lam', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791398104/elearningJP/audio/vocab/N5/vocab_n5_597_%E3%82%88%E3%81%8F.mp3', TRUE, '', '', 'yoku', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('横', 'よこ', 1, 'bề ngang; bên cạnh; chiều ngang', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791398106/elearningJP/audio/vocab/N5/vocab_n5_598_%E3%82%88%E3%81%93.mp3', TRUE, '', '', 'yoko', 'HOÀNH', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('四日', 'よっか', 1, 'bốn ngày; ngày mùng bốn', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791398107/elearningJP/audio/vocab/N5/vocab_n5_599_%E3%82%88%E3%81%A3%E3%81%8B.mp3', TRUE, '', '', 'yokka', 'TỨ NHẬT', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('四つ', 'よっつ', 1, 'bốn', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791398108/elearningJP/audio/vocab/N5/vocab_n5_600_%E3%82%88%E3%81%A3%E3%81%A4.mp3', TRUE, '', '', 'yottsu', 'TỨ', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('呼ぶ', 'よぶ', 1, 'gào; gọi; mời; kêu tên; hô hào', '', 'N5', 'VERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791398110/elearningJP/audio/vocab/N5/vocab_n5_601_%E3%82%88%E3%81%B6.mp3', TRUE, '', '', 'yobu', 'HÔ', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('読む', 'よむ', 1, 'đọc', '', 'N5', 'VERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791398111/elearningJP/audio/vocab/N5/vocab_n5_602_%E3%82%88%E3%82%80.mp3', TRUE, '', '', 'yomu', 'ĐỘC', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('弱い', 'よわい', 1, 'hèn yếu; kém cỏi; không chắc; không bền', '', 'N5', 'ADJECTIVE', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791398112/elearningJP/audio/vocab/N5/vocab_n5_603_%E3%82%88%E3%82%8F%E3%81%84.mp3', TRUE, '', '', 'yowai', 'NHƯỢC', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('来月', 'らいげつ', 1, 'tháng sau', '', 'N5', 'ADVERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791398114/elearningJP/audio/vocab/N5/vocab_n5_604_%E3%82%89%E3%81%84%E3%81%92%E3%81%A4.mp3', TRUE, '', '', 'raigetsu', 'LAI NGUYỆT', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('来週', 'らいしゅう', 1, 'tuần lễ sau; tuần sau', '', 'N5', 'ADVERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791398116/elearningJP/audio/vocab/N5/vocab_n5_605_%E3%82%89%E3%81%84%E3%81%97%E3%82%85%E3%81%86.mp3', TRUE, '', '', 'raishuu', 'LAI CHU', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('来年', 'らいねん', 1, 'năm sau', '', 'N5', 'ADVERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791398118/elearningJP/audio/vocab/N5/vocab_n5_606_%E3%82%89%E3%81%84%E3%81%AD%E3%82%93.mp3', TRUE, '', '', 'rainen', 'LAI NIÊN', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('ラジオ', 'ラジオ', 1, 'cái đài; cái radio; máy thu thanh; máy vô tuyến truyền thanh', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791398120/elearningJP/audio/vocab/N5/vocab_n5_607_%E3%83%A9%E3%82%B8%E3%82%AA.mp3', TRUE, '', '', 'ラジオ', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('ラジカセ', 'ラジカセ', 1, 'đài radio cassette', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791398121/elearningJP/audio/vocab/N5/vocab_n5_608_%E3%83%A9%E3%82%B8%E3%82%AB%E3%82%BB.mp3', TRUE, '', '', 'ラジカセ', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('ラジオカセット', 'ラジオカセット', 1, 'radio-cassette, tape recorder', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791398124/elearningJP/audio/vocab/N5/vocab_n5_609_%E3%83%A9%E3%82%B8%E3%82%AA%E3%82%AB%E3%82%BB%E3%83%83%E3%83%88.mp3', TRUE, '', '', 'ラジオカセット', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('りっぱ', 'りっぱ', 1, 'nhánh; sự tuyệt vời; sự tuyệt hảo; tuyệt vời; tuyệt hảo', '', 'N5', 'ADJECTIVE', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791398125/elearningJP/audio/vocab/N5/vocab_n5_610_%E3%82%8A%E3%81%A3%E3%81%B1.mp3', TRUE, '', '', 'rippa', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('留学生', 'りゅうがくせい', 1, 'du học sinh; lưu học sinh; học sinh du học', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791398126/elearningJP/audio/vocab/N5/vocab_n5_611_%E3%82%8A%E3%82%85%E3%81%86%E3%81%8C%E3%81%8F%E3%81%9B%E3%81%84.mp3', TRUE, '', '', 'ryuugakusei', 'LƯU HỌC SANH', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('両親', 'りょうしん', 1, 'Cha mẹ; bố mẹ', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791398128/elearningJP/audio/vocab/N5/vocab_n5_612_%E3%82%8A%E3%82%87%E3%81%86%E3%81%97%E3%82%93.mp3', TRUE, '', '', 'ryoushin', 'LƯỠNG THÂN', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('料理', 'りょうり', 1, 'bữa ăn; sự nấu ăn; món ăn; bữa ăn', '', 'N5', 'VERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791398129/elearningJP/audio/vocab/N5/vocab_n5_613_%E3%82%8A%E3%82%87%E3%81%86%E3%82%8A.mp3', TRUE, '', '', 'ryouri', 'LIỆU LÍ', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('旅行', 'りょこう', 1, 'lữ hành; sự đi lại; sự du lịch; du lịch', '', 'N5', 'VERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791398131/elearningJP/audio/vocab/N5/vocab_n5_614_%E3%82%8A%E3%82%87%E3%81%93%E3%81%86.mp3', TRUE, '', '', 'ryokou', 'LỮ HÀNH', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('零', 'れい', 1, 'số không; số 0', '', 'N5', 'ADJECTIVE', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791398132/elearningJP/audio/vocab/N5/vocab_n5_615_%E3%82%8C%E3%81%84.mp3', TRUE, '', '', 'rei', 'LINH', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('冷蔵庫', 'れいぞうこ', 1, 'kho ướp lạnh; tủ lạnh', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791398133/elearningJP/audio/vocab/N5/vocab_n5_616_%E3%82%8C%E3%81%84%E3%81%9E%E3%81%86%E3%81%93.mp3', TRUE, '', '', 'reizouko', 'LÃNH TÀNG KHỐ', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('レコード', 'レコード', 1, 'đĩa nhựa; kỷ lục; sự ghi âm; sự thu thanh', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791398135/elearningJP/audio/vocab/N5/vocab_n5_617_%E3%83%AC%E3%82%B3%E3%83%BC%E3%83%89.mp3', TRUE, '', '', 'レコード', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('レストラン', 'レストラン', 1, 'cao lâu; hiệu ăn; nhà hàng', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791398136/elearningJP/audio/vocab/N5/vocab_n5_618_%E3%83%AC%E3%82%B9%E3%83%88%E3%83%A9%E3%83%B3.mp3', TRUE, '', '', 'レストラン', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('廊下', 'ろうか', 1, 'gác; hành lang; thềm', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791398138/elearningJP/audio/vocab/N5/vocab_n5_619_%E3%82%8D%E3%81%86%E3%81%8B.mp3', TRUE, '', '', 'rouka', 'LANG HẠ', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('ワイシャツ', 'ワイシャツ', 1, 'áo sơ mi dài tay', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791398139/elearningJP/audio/vocab/N5/vocab_n5_620_%E3%83%AF%E3%82%A4%E3%82%B7%E3%83%A3%E3%83%84.mp3', TRUE, '', '', 'ワイシャツ', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('若い', 'わかい', 1, 'bé; bé bỏng; choai choai', '', 'N5', 'ADJECTIVE', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791398142/elearningJP/audio/vocab/N5/vocab_n5_621_%E3%82%8F%E3%81%8B%E3%81%84.mp3', TRUE, '', '', 'wakai', 'NHƯỢC', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('分かる', 'わかる', 1, 'hay tin; hiểu biết; hiểu; lý giải; biết', '', 'N5', 'VERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791398143/elearningJP/audio/vocab/N5/vocab_n5_622_%E3%82%8F%E3%81%8B%E3%82%8B.mp3', TRUE, '', '', 'wakaru', 'PHÂN', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('忘れる', 'わすれる', 1, 'bỏ lại; đãng; quên bẵng', '', 'N5', 'VERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791398144/elearningJP/audio/vocab/N5/vocab_n5_623_%E3%82%8F%E3%81%99%E3%82%8C%E3%82%8B.mp3', TRUE, '', '', 'wasureru', 'VONG', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('渡す', 'わたす', 1, 'trao, đưa', '', 'N5', 'VERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791398146/elearningJP/audio/vocab/N5/vocab_n5_624_%E3%82%8F%E3%81%9F%E3%81%99.mp3', TRUE, '', '', 'watasu', 'ĐỘ', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('渡る', 'わたる', 1, 'băng qua; đi qua; độ', '', 'N5', 'VERB', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791398148/elearningJP/audio/vocab/N5/vocab_n5_625_%E3%82%8F%E3%81%9F%E3%82%8B.mp3', TRUE, '', '', 'wataru', 'ĐỘ', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;
INSERT INTO vocabulary (word, hiragana, sense_no, meaning_vi, meaning_en, jlpt_level, part_of_speech, audio_url, is_tts, example_jp, example_vi, romaji, han_viet, source_type, visibility, review_status, is_active)
VALUES ('ほう', 'より', 1, 'oh, ho, exclamation of surprise, admiration, etc; hoo (owl call), toot (sound of a flute)', '', 'N5', 'NOUN', 'https://res.cloudinary.com/dw9krx7ac/video/upload/v1791398150/elearningJP/audio/vocab/N5/vocab_n5_626_%E3%82%88%E3%82%8A.mp3', TRUE, '', '', 'yori', '', 'CORE', 'PUBLIC', 'PUBLISHED', TRUE)
ON CONFLICT (word, hiragana) DO NOTHING;

-- -----------------------------------------------------------------------------
-- VOCABULARY SENSES (N5)
-- -----------------------------------------------------------------------------
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'đông; Hướng Đông', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '東' AND v.hiragana = 'ひがし'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'tôi (trang trọng, khiêm nhường)', '', 'ADJECTIVE'
FROM vocabulary v
WHERE v.word = '私' AND v.hiragana = 'わたくし'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'một ngày', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '一日' AND v.hiragana = 'いちにち'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'một người', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '一人' AND v.hiragana = 'ひとり'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'thời gian rảnh rỗi; thì giờ nhàn hạ; sự nghỉ ngơi; sự cáo từ; sự từ giã', '', 'ADJECTIVE'
FROM vocabulary v
WHERE v.word = '暇' AND v.hiragana = 'ひま'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'cổng vào; cửa vào; lối vào; sự bắt đầu', '', 'ADJECTIVE'
FROM vocabulary v
WHERE v.word = '入口' AND v.hiragana = 'いりぐち'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'đi vào; vào', '', 'VERB'
FROM vocabulary v
WHERE v.word = '入る' AND v.hiragana = 'はいる'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'ngon ngọt; ngọt; ngọt bùi', '', 'ADJECTIVE'
FROM vocabulary v
WHERE v.word = '甘い' AND v.hiragana = 'あまい'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'lời tâng bốc; lời nịnh nọt; giỏi; cừ; túm lấy khố của đối thủ từ vị trí tay đè trên tay đối thủ', '', 'ADJECTIVE'
FROM vocabulary v
WHERE v.word = '上手' AND v.hiragana = 'じょうず'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'muối', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '塩' AND v.hiragana = 'しお'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'màu vàng; vàng', '', 'ADJECTIVE'
FROM vocabulary v
WHERE v.word = '黄色' AND v.hiragana = 'きいろ'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'bào đệ; em; em trai', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '弟' AND v.hiragana = 'おとうと'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'cổng; một trong những giai đoạn phân loại sinh học', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '門' AND v.hiragana = 'もん'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'kính (đeo mắt)', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '眼鏡' AND v.hiragana = 'めがね'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'đến', '', 'VERB'
FROM vocabulary v
WHERE v.word = '来る' AND v.hiragana = 'くる'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'cánh cửa; cửa', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '戸' AND v.hiragana = 'と'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'lần sau; sau đây; tiếp đến', '', 'ADJECTIVE'
FROM vocabulary v
WHERE v.word = '次' AND v.hiragana = 'つぎ'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'sự bền; sự vững chắc; sức bền; sự dai sức; chắc; khoẻ; cứng; bền; độ bền', '', 'ADJECTIVE'
FROM vocabulary v
WHERE v.word = '丈夫' AND v.hiragana = 'じょうぶ'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'người', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '人' AND v.hiragana = 'ひと'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'tiền; trước; kém; trước đây; cũ; người hay việc cũ đã nói ở trên; trước khi; 前年:năm trước', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '前' AND v.hiragana = 'まえ'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'ai', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '誰' AND v.hiragana = 'だれ'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'cơ thể; sức khoẻ; thân thể; khối', '', 'ADJECTIVE'
FROM vocabulary v
WHERE v.word = '体' AND v.hiragana = 'からだ'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'cửa hàng; cửa hiệu; sự thành lập', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '店' AND v.hiragana = 'みせ'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'dừng lại; giữ lại; ở lại', '', 'VERB'
FROM vocabulary v
WHERE v.word = '止まる' AND v.hiragana = 'とまる'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, '7 ngày; ngày thứ 7 của tháng', '', 'ADVERB'
FROM vocabulary v
WHERE v.word = '七日' AND v.hiragana = 'なのか'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'nam; phía Nam; phương Nam', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '南' AND v.hiragana = 'みなみ'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'còm; xấu; không tốt; ngu ngốc', '', 'ADJECTIVE'
FROM vocabulary v
WHERE v.word = '悪い' AND v.hiragana = 'わるい'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'Hai người', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '二人' AND v.hiragana = 'ふたり'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'gạt tàn', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '灰皿' AND v.hiragana = 'はいざら'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'gảy, búng; không thấm nước, chống nước; không chấp nhận những thứ không phù hợp với điều kiện', '', 'VERB'
FROM vocabulary v
WHERE v.word = '弾く' AND v.hiragana = 'ひく'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'lúc đầu; đầu tiên; 開始', '', 'ADVERB'
FROM vocabulary v
WHERE v.word = '始め' AND v.hiragana = 'はじめ'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'mười nghìn; 1 vạn', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '万' AND v.hiragana = 'まん'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'ba', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '三' AND v.hiragana = 'さん'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'số sáu', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '六' AND v.hiragana = 'ろく'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'hôm kia', '', 'ADVERB'
FROM vocabulary v
WHERE v.word = '一昨日' AND v.hiragana = 'おととい'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'năm kia', '', 'ADVERB'
FROM vocabulary v
WHERE v.word = '一昨年' AND v.hiragana = 'おととし'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'bác; cô', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '伯母さん' AND v.hiragana = 'おばさん'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'cô; dì; người đàn bà trung niên; phụ nữ trung niên', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '叔母さん' AND v.hiragana = 'おばさん'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'làm như thế; và', '', 'CONJUNCTION'
FROM vocabulary v
WHERE v.word = 'そうして' AND v.hiragana = 'そうして'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'chế biến; làm; tạo; sáng tác; xây dựng; nấu', '', 'VERB'
FROM vocabulary v
WHERE v.word = '作る' AND v.hiragana = 'つくる'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'tròn', '', 'ADJECTIVE'
FROM vocabulary v
WHERE v.word = '円い' AND v.hiragana = 'まるい'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'gặp; hội ngộ', '', 'VERB'
FROM vocabulary v
WHERE v.word = '会う' AND v.hiragana = 'あう'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'màu xanh da trời; màu xanh nước biển', '', 'ADJECTIVE'
FROM vocabulary v
WHERE v.word = '青' AND v.hiragana = 'あお'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'xanh da trời; xanh lục; còn xanh; thiếu kinh nghiệm', '', 'ADJECTIVE'
FROM vocabulary v
WHERE v.word = '青い' AND v.hiragana = 'あおい'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'màu đỏ', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '赤' AND v.hiragana = 'あか'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'đỏ', '', 'ADJECTIVE'
FROM vocabulary v
WHERE v.word = '赤い' AND v.hiragana = 'あかい'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'sáng sủa; vui vẻ', '', 'ADJECTIVE'
FROM vocabulary v
WHERE v.word = '明い' AND v.hiragana = 'あかるい'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'mùa thu; thu', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '秋' AND v.hiragana = 'あき'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'đào; đục; khoan; há; mở', '', 'VERB'
FROM vocabulary v
WHERE v.word = '開ける' AND v.hiragana = 'あける'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'tăng, nâng lên; di chuyển lên cao; cải thiện, thúc đẩy', '', 'VERB'
FROM vocabulary v
WHERE v.word = '上げる' AND v.hiragana = 'あげる'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'ban sáng; buổi sáng; sáng', '', 'ADVERB'
FROM vocabulary v
WHERE v.word = '朝' AND v.hiragana = 'あさ'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'bữa sáng; cơm sáng (nói chung); cơm sáng', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '朝御飯' AND v.hiragana = 'あさごはん'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'bữa mốt; mốt; ngày kia; hai ngày sau', '', 'ADVERB'
FROM vocabulary v
WHERE v.word = 'あさって' AND v.hiragana = 'あさって'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'cẳng; chân', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '足' AND v.hiragana = 'あし'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'mức độ ấy; mức ấy; ở đó; ở chỗ đó', '', 'ADJECTIVE'
FROM vocabulary v
WHERE v.word = 'あそこ' AND v.hiragana = 'あそこ'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'nô đùa; vui đùa; chơi (bóng chày)', '', 'VERB'
FROM vocabulary v
WHERE v.word = '遊ぶ' AND v.hiragana = 'あそぶ'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'đầm ấm; êm ấm; nóng; nồng hậu; ấm áp', '', 'ADJECTIVE'
FROM vocabulary v
WHERE v.word = '暖かい' AND v.hiragana = 'あたたかい'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'đầu; người cầm đầu; kẻ cầm đầu; ông chủ', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '頭' AND v.hiragana = 'あたま'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'mới; mới mẻ', '', 'ADJECTIVE'
FROM vocabulary v
WHERE v.word = '新しい' AND v.hiragana = 'あたらしい'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'chỗ đó; ở đó', '', 'ADJECTIVE'
FROM vocabulary v
WHERE v.word = 'あちら' AND v.hiragana = 'あちら'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'nóng; nóng nực; nực', '', 'ADJECTIVE'
FROM vocabulary v
WHERE v.word = '暑い' AND v.hiragana = 'あつい'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'nóng; nóng bỏng; oi bức; thân thiện; nhiệt tình', '', 'ADJECTIVE'
FROM vocabulary v
WHERE v.word = '熱い' AND v.hiragana = 'あつい'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'dày; dầy', '', 'ADJECTIVE'
FROM vocabulary v
WHERE v.word = '厚い' AND v.hiragana = 'あつい'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'ấy; đó; kia; đằng kia; chỗ kia', '', 'NOUN'
FROM vocabulary v
WHERE v.word = 'あっち' AND v.hiragana = 'あっち'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'anh; chị', '', 'NOUN'
FROM vocabulary v
WHERE v.word = 'あなた' AND v.hiragana = 'あなた'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'anh trai', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '兄' AND v.hiragana = 'あに'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'chị; chị của mình; tỷ', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '姉' AND v.hiragana = 'あね'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'cái đó; chỗ đó', '', 'ADJECTIVE'
FROM vocabulary v
WHERE v.word = 'あの' AND v.hiragana = 'あの'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'khu nhà tập thể; nhà chung cư; căn hộ; nhà khối', '', 'NOUN'
FROM vocabulary v
WHERE v.word = 'アパート' AND v.hiragana = 'アパート'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'rơi vào; ngập chìm; tắm; thu hút', '', 'VERB'
FROM vocabulary v
WHERE v.word = 'あびる' AND v.hiragana = 'あびる'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'nghi ngờ; không rõ; không đáng tin; nguy; nguy hiểm; nguy kịch', '', 'ADJECTIVE'
FROM vocabulary v
WHERE v.word = '危ない' AND v.hiragana = 'あぶない'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'không mấy; ít; thừa; phần còn lại; phần dư; phần thừa; phần dư thừa; rất; lắm', '', 'ADJECTIVE'
FROM vocabulary v
WHERE v.word = 'あまり' AND v.hiragana = 'あまり'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'cơn mưa; mưa; trận mưa', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '雨' AND v.hiragana = 'あめ'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'kẹo; kẹo ngậm', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '飴' AND v.hiragana = 'あめ'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'giặt; rửa; tắm gội; tẩy; tẩy rửa', '', 'VERB'
FROM vocabulary v
WHERE v.word = '洗う' AND v.hiragana = 'あらう'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'một certain...; some..', '', 'NOUN'
FROM vocabulary v
WHERE v.word = 'ある' AND v.hiragana = 'ある'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'đi bộ; đi; bước', '', 'VERB'
FROM vocabulary v
WHERE v.word = '歩く' AND v.hiragana = 'あるく'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'giông tố', '', 'NOUN'
FROM vocabulary v
WHERE v.word = 'あれ' AND v.hiragana = 'あれ'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'không; không có gì', '', 'INTERJECTION'
FROM vocabulary v
WHERE v.word = 'いいえ' AND v.hiragana = 'いいえ'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'gia đình; nhà', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '家' AND v.hiragana = 'いえ'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'như thế nào; thế nào', '', 'ADVERB'
FROM vocabulary v
WHERE v.word = 'いかが' AND v.hiragana = 'いかが'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'đi', '', 'VERB'
FROM vocabulary v
WHERE v.word = '行く' AND v.hiragana = 'いく'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'bao nhiêu; bao nhiêu tuổi', '', 'ADVERB'
FROM vocabulary v
WHERE v.word = 'いくつ' AND v.hiragana = 'いくつ'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'ý nghĩa', '', 'NOUN'
FROM vocabulary v
WHERE v.word = 'いくら' AND v.hiragana = 'いくら'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'bàu; cái ao; ao; hồ', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '池' AND v.hiragana = 'いけ'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'bác sĩ; đại phu; thầy lang', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '医者' AND v.hiragana = 'いしゃ'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'ghế; cái ghế', '', 'NOUN'
FROM vocabulary v
WHERE v.word = 'いす' AND v.hiragana = 'いす'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'bận; bận rộn; bề bộn', '', 'ADJECTIVE'
FROM vocabulary v
WHERE v.word = '忙しい' AND v.hiragana = 'いそがしい'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'đau; đau đớn; nhức', '', 'ADJECTIVE'
FROM vocabulary v
WHERE v.word = '痛い' AND v.hiragana = 'いたい'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'một', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '一' AND v.hiragana = 'いち'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'nhất; tốt nhất; số một; đầu tiên; number one', '', 'ADJECTIVE'
FROM vocabulary v
WHERE v.word = 'いちばん' AND v.hiragana = 'いちばん'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'khi nào; bao giờ; いつまでも:Lúc nào cũng; いつまで:đến bao giờ', '', 'ADVERB'
FROM vocabulary v
WHERE v.word = 'いつ' AND v.hiragana = 'いつ'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, '5 ngày; năm ngày; ngày mồng 5', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '五日' AND v.hiragana = 'いつか'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'cùng; cùng nhau; sự giống như vậy', '', 'ADJECTIVE'
FROM vocabulary v
WHERE v.word = '一緒' AND v.hiragana = 'いっしょ'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'năm cái; năm chiếc', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '五つ' AND v.hiragana = 'いつつ'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'cẩu; chó; khuyển', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '犬' AND v.hiragana = 'いぬ'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'bây giờ', '', 'ADVERB'
FROM vocabulary v
WHERE v.word = '今' AND v.hiragana = 'いま'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'ý nghĩa; nghĩa', '', 'VERB'
FROM vocabulary v
WHERE v.word = '意味' AND v.hiragana = 'いみ'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'em; em gái', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '妹' AND v.hiragana = 'いもうと'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'khó chịu; ghét; không vừa ý; sự khó chịu; sự ghét; điều chán ghét; khó chịu; không thích', '', 'ADJECTIVE'
FROM vocabulary v
WHERE v.word = '嫌' AND v.hiragana = 'いや'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'cần', '', 'VERB'
FROM vocabulary v
WHERE v.word = '要る' AND v.hiragana = 'いる'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'cho vào; bỏ vào; đút; kéo vào', '', 'VERB'
FROM vocabulary v
WHERE v.word = '入れる' AND v.hiragana = 'いれる'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'màu; mầu; màu sắc', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '色' AND v.hiragana = 'いろ'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'nhiều; phong phú', '', 'ADJECTIVE'
FROM vocabulary v
WHERE v.word = 'いろいろ' AND v.hiragana = 'いろいろ'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'sau; đằng sau; phía sau', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '後ろ' AND v.hiragana = 'うしろ'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'lạt; lỏng; lợt', '', 'ADJECTIVE'
FROM vocabulary v
WHERE v.word = '薄い' AND v.hiragana = 'うすい'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'bài hát', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '歌' AND v.hiragana = 'うた'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'ca; ca hát; hát', '', 'VERB'
FROM vocabulary v
WHERE v.word = '歌う' AND v.hiragana = 'うたう'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'đản sinh; được sinh ra; sinh ra; lọt lòng', '', 'VERB'
FROM vocabulary v
WHERE v.word = '生まれる' AND v.hiragana = 'うまれる'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'bể; bể khơi; biển; bờ biển', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '海' AND v.hiragana = 'うみ'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'bán; bán hàng', '', 'VERB'
FROM vocabulary v
WHERE v.word = '売る' AND v.hiragana = 'うる'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'chán ghét; đáng ghét; ồn ào; phiền phức; lắm điều', '', 'ADJECTIVE'
FROM vocabulary v
WHERE v.word = '煩い' AND v.hiragana = 'うるさい'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'áo vét; áo khoác', '', 'ADJECTIVE'
FROM vocabulary v
WHERE v.word = '上着' AND v.hiragana = 'うわぎ'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'bức tranh; tranh', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '絵' AND v.hiragana = 'え'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'điện ảnh; phim', '', 'ADJECTIVE'
FROM vocabulary v
WHERE v.word = '映画' AND v.hiragana = 'えいが'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'rạp chiếu phim; rạp; rạp chiếu bóng; trung tâm chiếu phim', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '映画館' AND v.hiragana = 'えいがかん'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'tiếng Anh', '', 'ADJECTIVE'
FROM vocabulary v
WHERE v.word = '英語' AND v.hiragana = 'えいご'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'vâng; vâng; dạ; ừ', '', 'NOUN'
FROM vocabulary v
WHERE v.word = 'ええ' AND v.hiragana = 'ええ'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'ga; nhà ga', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '駅' AND v.hiragana = 'えき'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'thang máy', '', 'NOUN'
FROM vocabulary v
WHERE v.word = 'エレベーター' AND v.hiragana = 'エレベーター'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'bút chì; viết chì', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '鉛筆' AND v.hiragana = 'えんぴつ'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'Ngon', '', 'NOUN'
FROM vocabulary v
WHERE v.word = 'おいしい' AND v.hiragana = 'おいしい'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'bộn; nhiều', '', 'ADJECTIVE'
FROM vocabulary v
WHERE v.word = '多い' AND v.hiragana = 'おおい'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'bự; to lớn; to; lớn', '', 'ADJECTIVE'
FROM vocabulary v
WHERE v.word = '大きい' AND v.hiragana = 'おおきい'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'bự; lớn; to', '', 'ADJECTIVE'
FROM vocabulary v
WHERE v.word = '大きな' AND v.hiragana = 'おおきな'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'đại chúng; phần lớn mọi người; đám đông; nhiều người; nhiều; rất nhiều', '', 'ADJECTIVE'
FROM vocabulary v
WHERE v.word = '大勢' AND v.hiragana = 'おおぜい'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'má; mẹ; mẹ ơi; thân mẫu', '', 'NOUN'
FROM vocabulary v
WHERE v.word = 'お母さん' AND v.hiragana = 'おかあさん'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'bánh kẹo; kẹo; bánh ngọt', '', 'NOUN'
FROM vocabulary v
WHERE v.word = 'お菓子' AND v.hiragana = 'おかし'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'tiền; của cải', '', 'NOUN'
FROM vocabulary v
WHERE v.word = 'お金' AND v.hiragana = 'おかね'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'dấy; đứng dậy; ngồi dậy; bình phục; nhen nhúm', '', 'VERB'
FROM vocabulary v
WHERE v.word = '起きる' AND v.hiragana = 'おきる'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'bố trí (người); cho thuê chỗ ở; chừa ra; để chừa ra', '', 'VERB'
FROM vocabulary v
WHERE v.word = '置く' AND v.hiragana = 'おく'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'bà; vợ; bà nhà; chị nhà', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '奥さん' AND v.hiragana = 'おくさん'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'rượu; rượu sakê', '', 'NOUN'
FROM vocabulary v
WHERE v.word = 'お酒' AND v.hiragana = 'おさけ'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'đĩa', '', 'NOUN'
FROM vocabulary v
WHERE v.word = 'お皿' AND v.hiragana = 'おさら'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'bác; chú; chú bác; dì', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '伯父' AND v.hiragana = 'おじいさん'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'cậu; chú; chú bác', '', 'ADJECTIVE'
FROM vocabulary v
WHERE v.word = '叔父' AND v.hiragana = 'おじいさん'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'chỉ dẫn; chỉ dạy; dạy dỗ; chỉ bảo; dạy', '', 'VERB'
FROM vocabulary v
WHERE v.word = '教える' AND v.hiragana = 'おしえる'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'ẩn; đẩy; ấn; nhấn; bấm; dí', '', 'VERB'
FROM vocabulary v
WHERE v.word = '押す' AND v.hiragana = 'おす'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'muộn màng; muộn; chậm; trễ', '', 'ADJECTIVE'
FROM vocabulary v
WHERE v.word = '遅い' AND v.hiragana = 'おそい'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'chè; nước chè; trà; chè xanh', '', 'NOUN'
FROM vocabulary v
WHERE v.word = 'お茶' AND v.hiragana = 'おちゃ'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'toa-lét; nhà vệ sinh', '', 'NOUN'
FROM vocabulary v
WHERE v.word = 'お手洗い' AND v.hiragana = 'おてあらい'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'bố; bố ơi (khi con gọi bố; cha; thân phụ', '', 'NOUN'
FROM vocabulary v
WHERE v.word = 'お父さん' AND v.hiragana = 'おとうさん'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'đàn ông; người đàn ông; nam; trai', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '男' AND v.hiragana = 'おとこ'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'cậu bé; con đực (động vật)', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '男の子' AND v.hiragana = 'おとこのこ'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'người lớn; người trưởng thành', '', 'ADJECTIVE'
FROM vocabulary v
WHERE v.word = '大人' AND v.hiragana = 'おとな'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'bụng', '', 'NOUN'
FROM vocabulary v
WHERE v.word = 'おなか' AND v.hiragana = 'おなか'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'bằng nhau; sự giống nhau; sự giống; giống nhau; cùng; giống', '', 'ADJECTIVE'
FROM vocabulary v
WHERE v.word = '同じ' AND v.hiragana = 'おなじ'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'anh trai; thưa anh; anh ơi; anh trai (...bạn)', '', 'NOUN'
FROM vocabulary v
WHERE v.word = 'お兄さん' AND v.hiragana = 'おにいさん'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'chị; chị gái (bạn...); thưa chị; chị ơi', '', 'NOUN'
FROM vocabulary v
WHERE v.word = 'お姉さん' AND v.hiragana = 'おねえさん'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'bà; bà già; người già; bà cụ', '', 'NOUN'
FROM vocabulary v
WHERE v.word = 'おばあさん' AND v.hiragana = 'おばあさん'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'bồn', '', 'NOUN'
FROM vocabulary v
WHERE v.word = 'お風呂' AND v.hiragana = 'おふろ'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'cơm hộp; cơm trưa', '', 'NOUN'
FROM vocabulary v
WHERE v.word = 'お弁当' AND v.hiragana = 'おべんとう'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'cảm thấy; học; học thuộc; nhớ', '', 'VERB'
FROM vocabulary v
WHERE v.word = '覚える' AND v.hiragana = 'おぼえる'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'cảnh sát giao thông; tuần cảnh; tuần du', '', 'NOUN'
FROM vocabulary v
WHERE v.word = 'おまわりさん' AND v.hiragana = 'おまわりさん'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'nặng; nặng nề; trầm trọng', '', 'ADJECTIVE'
FROM vocabulary v
WHERE v.word = '重い' AND v.hiragana = 'おもい'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'dí dỏm; thú vị; hay; vui tính', '', 'ADJECTIVE'
FROM vocabulary v
WHERE v.word = 'おもしろい' AND v.hiragana = 'おもしろい'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'bơi; bơi lội; lội', '', 'VERB'
FROM vocabulary v
WHERE v.word = '泳ぐ' AND v.hiragana = 'およぐ'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'bước xuống; hạ; rủ', '', 'VERB'
FROM vocabulary v
WHERE v.word = '降りる' AND v.hiragana = 'おりる'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'hoàn thành, kết thúc', '', 'VERB'
FROM vocabulary v
WHERE v.word = '終る' AND v.hiragana = 'おわる'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'âm nhạc; nhạc; ca nhạc; âm nhạc', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '音楽' AND v.hiragana = 'おんがく'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'phụ nữ; con gái; cô gái; đàn bà; nữ; con gái, nữ giới', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '女' AND v.hiragana = 'おんな'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'cô gái; cô bé', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '女の子' AND v.hiragana = 'おんなのこ'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'đất khách; ngoại bang; ngoại quốc', '', 'ADJECTIVE'
FROM vocabulary v
WHERE v.word = '外国' AND v.hiragana = 'がいこく'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'ngoại nhân; người nước ngoài; người ngoại quốc', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '外国人' AND v.hiragana = 'がいこくじん'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'công ty; hãng', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '会社' AND v.hiragana = 'かいしゃ'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'cầu thang; thang gác; thang lầu', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '階段' AND v.hiragana = 'かいだん'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'món hàng mua được; sự mua hàng; thứ cần mua', '', 'ADJECTIVE'
FROM vocabulary v
WHERE v.word = '買い物' AND v.hiragana = 'かいもの'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'đánh giá cao; tán dương thưởng thức; gây ra; chuốc lấy; làm cho; mua', '', 'VERB'
FROM vocabulary v
WHERE v.word = '買う' AND v.hiragana = 'かう'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'trả; trả lại; chuyển lại', '', 'VERB'
FROM vocabulary v
WHERE v.word = '返す' AND v.hiragana = 'かえす'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'về; chạy về', '', 'VERB'
FROM vocabulary v
WHERE v.word = '帰る' AND v.hiragana = 'かえる'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'liên quan; liên lụy; về', '', 'VERB'
FROM vocabulary v
WHERE v.word = 'かかる' AND v.hiragana = 'かかる'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'woman who earns her living by entertaining with song, dance and playing the shamisen, geisha who sings at parties', '', 'NOUN'
FROM vocabulary v
WHERE v.word = 'かぎ' AND v.hiragana = 'かぎ'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'vẽ; viết; viết lách', '', 'VERB'
FROM vocabulary v
WHERE v.word = '書く' AND v.hiragana = 'かく'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'sinh viên; học sinh', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '学生' AND v.hiragana = 'がくせい'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'treo lên; treo; dựng', '', 'VERB'
FROM vocabulary v
WHERE v.word = 'かける' AND v.hiragana = 'かける'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'cái ô; dù; ô; cái ô', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '傘' AND v.hiragana = 'かさ'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'bán đợ; cho vay; cho mượn', '', 'VERB'
FROM vocabulary v
WHERE v.word = '貸す' AND v.hiragana = 'かす'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'gió', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '風' AND v.hiragana = 'かぜ'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'cảm lạnh; cảm; cảm cúm; sổ mũi', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '風邪' AND v.hiragana = 'かぜ'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'gia đình; gia quyến; gia tộc', '', 'ADJECTIVE'
FROM vocabulary v
WHERE v.word = '家族' AND v.hiragana = 'かぞく'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'học đường; học hiệu; nhà trường', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '学校' AND v.hiragana = 'がっこう'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'cốc; chén; bát; cúp', '', 'NOUN'
FROM vocabulary v
WHERE v.word = 'カップ' AND v.hiragana = 'カップ'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'gia đình, hộ gia đình (nơi chốn)', '', 'ADJECTIVE'
FROM vocabulary v
WHERE v.word = '家庭' AND v.hiragana = 'かてい'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'việc đóng dấu', '', 'VERB'
FROM vocabulary v
WHERE v.word = 'かばん' AND v.hiragana = 'かばん'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'bình hoa; lọ hoa; bình dùng để đựng hoa cúng (thường làm bằng đồng mạ vàng)', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '花瓶' AND v.hiragana = 'かびん'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'giấy', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '紙' AND v.hiragana = 'かみ'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'máy ảnh; máy ảnh; máy quay phim; máy chụp ảnh', '', 'NOUN'
FROM vocabulary v
WHERE v.word = 'カメラ' AND v.hiragana = 'カメラ'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'thứ ba; ngày thứ ba', '', 'ADVERB'
FROM vocabulary v
WHERE v.word = '火曜日' AND v.hiragana = 'かようび'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'mướn; tô; thuê; mượn; vay', '', 'VERB'
FROM vocabulary v
WHERE v.word = '借りる' AND v.hiragana = 'かりる'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'nhẹ', '', 'ADJECTIVE'
FROM vocabulary v
WHERE v.word = '軽い' AND v.hiragana = 'かるい'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'cà ri; món cari; cà-ri', '', 'NOUN'
FROM vocabulary v
WHERE v.word = 'カレー' AND v.hiragana = 'カレー'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'lịch; máy ca-len-da; máy định hình vải; niên lịch', '', 'NOUN'
FROM vocabulary v
WHERE v.word = 'カレンダー' AND v.hiragana = 'カレンダー'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'duyên dáng; đáng yêu; xinh xắn; dễ thương; khả ái; êm ái; ngộ nghĩnh', '', 'ADJECTIVE'
FROM vocabulary v
WHERE v.word = 'かわいい' AND v.hiragana = 'かわいい'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'chữ Hán; hán tự', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '漢字' AND v.hiragana = 'かんじ'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'cây cối; cây; gỗ; mộc', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '木' AND v.hiragana = 'き'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'vàng', '', 'ADJECTIVE'
FROM vocabulary v
WHERE v.word = '黄色い' AND v.hiragana = 'きいろい'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'biến mất; tan đi; tắt', '', 'VERB'
FROM vocabulary v
WHERE v.word = '消える' AND v.hiragana = 'きえる'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'nghe; hỏi', '', 'VERB'
FROM vocabulary v
WHERE v.word = '聞く' AND v.hiragana = 'きく'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'phía Bắc; miền Bắc', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '北' AND v.hiragana = 'きた'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'đàn ghita; ghita', '', 'NOUN'
FROM vocabulary v
WHERE v.word = 'ギター' AND v.hiragana = 'ギター'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'bẩn; ô uế; bẩn thỉu; bê bết; bệ rạc', '', 'ADJECTIVE'
FROM vocabulary v
WHERE v.word = '汚い' AND v.hiragana = 'きたない'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'quán cà phê; quán trà; quán nước; tiệm giải khát; quán giải khát', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '喫茶店' AND v.hiragana = 'きっさてん'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'tem; tem hàng', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '切手' AND v.hiragana = 'きって'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'vé', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '切符' AND v.hiragana = 'きっぷ'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'bữa hôm trước; bữa qua; ngày hôm qua', '', 'ADVERB'
FROM vocabulary v
WHERE v.word = '昨日' AND v.hiragana = 'きのう'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'thịt bò', '', 'ADJECTIVE'
FROM vocabulary v
WHERE v.word = '牛肉' AND v.hiragana = 'ぎゅうにく'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'sữa; sữa bò', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '牛乳' AND v.hiragana = 'ぎゅうにゅう'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'Lớp học, phòng học', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '教室' AND v.hiragana = 'きょうしつ'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'anh em; huynh đệ', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '兄弟' AND v.hiragana = 'きょうだい'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'năm ngoái; năm trước; năm qua', '', 'ADVERB'
FROM vocabulary v
WHERE v.word = '去年' AND v.hiragana = 'きょねん'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'đáng ghét; không ưa; không thích; ghét; phân biệt; khu biệt; sự đáng ghét; sự không ưa; đáng ghét; không ưa; không thích; ghét', '', 'ADJECTIVE'
FROM vocabulary v
WHERE v.word = '嫌い' AND v.hiragana = 'きらい'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'cắt; chặt; thái; băm; ngắt; đốn; hạ; bấm; cúp; thái; xé; bẻ; lật; ấn định; cắt đứt; chọc tiết; cưa', '', 'VERB'
FROM vocabulary v
WHERE v.word = '切る' AND v.hiragana = 'きる'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'bận; khoác; mặc', '', 'VERB'
FROM vocabulary v
WHERE v.word = '着る' AND v.hiragana = 'きる'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'đẹp; sạch; đẹp; giỏ rác; đẹp; hội chợ', '', 'NOUN'
FROM vocabulary v
WHERE v.word = 'きれい' AND v.hiragana = 'きれい'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'cân; kilô; kilôgam; ký; kilogram', '', 'NOUN'
FROM vocabulary v
WHERE v.word = 'キログラム' AND v.hiragana = 'キロ'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'kilômét; cây số', '', 'NOUN'
FROM vocabulary v
WHERE v.word = 'キロメートル' AND v.hiragana = 'キロ'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'ngân hàng; nhà băng', '', 'ADJECTIVE'
FROM vocabulary v
WHERE v.word = '銀行' AND v.hiragana = 'ぎんこう'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'ngày thứ sáu; thứ sáu', '', 'ADVERB'
FROM vocabulary v
WHERE v.word = '金曜日' AND v.hiragana = 'きんようび'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'dược; thuốc', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '薬' AND v.hiragana = 'くすり'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'please (kanonly); (with te-form verb) please do for me', '', 'NOUN'
FROM vocabulary v
WHERE v.word = 'ください' AND v.hiragana = 'ください'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'hoa quả; trái cây', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '果物' AND v.hiragana = 'くだもの'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'cửa; miệng; chỗ cho vào; chỗ ra vào (đồ vật); mồm; miệng; mỏ; miệng', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '口' AND v.hiragana = 'くち'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'giày; dép; guốc', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '靴' AND v.hiragana = 'くつ'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'bít tất; tất; tất chân; vớ', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '靴下' AND v.hiragana = 'くつした'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'đất nước; quốc gia; quê nhà', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '国' AND v.hiragana = 'くに'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'mờ; không rõ; nhiều mây; sự không chính trực; trời âm u; trời đầy mây', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '曇り' AND v.hiragana = 'くもり'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'đầy ..; nỗi lòng buồn chán; ủ ê; râm', '', 'VERB'
FROM vocabulary v
WHERE v.word = '曇る' AND v.hiragana = 'くもる'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'tối tăm; ảm đạm; âm u (bầu trời, không khí); tối; tối màu; u sầu, u ám, trầm (tính cách, tâm trạng)', '', 'ADJECTIVE'
FROM vocabulary v
WHERE v.word = '暗い' AND v.hiragana = 'くらい'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'lớp; lớp học; lớp; lớp học', '', 'NOUN'
FROM vocabulary v
WHERE v.word = 'クラス' AND v.hiragana = 'クラス'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'gam (gr, đơn vị đo lường); gam', '', 'NOUN'
FROM vocabulary v
WHERE v.word = 'グラム' AND v.hiragana = 'グラム'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'bánh xe; mô tô; ô tô', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '車' AND v.hiragana = 'くるま'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'màu đen; sự có tội', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '黒' AND v.hiragana = 'くろ'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'đen; u ám; đen tối', '', 'ADJECTIVE'
FROM vocabulary v
WHERE v.word = '黒い' AND v.hiragana = 'くろい'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'cánh sát; cảnh sát; cánh sát viên', '', 'ADJECTIVE'
FROM vocabulary v
WHERE v.word = '警官' AND v.hiragana = 'けいかん'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'hồi sáng; sáng hôm nay; sáng nay', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '今朝' AND v.hiragana = 'けさ'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'bôi; dụi; tắt', '', 'VERB'
FROM vocabulary v
WHERE v.word = '消す' AND v.hiragana = 'けす'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'kết cấu; cấu trúc; tạm được; tương đối; kha khá; đủ; được; cũng được', '', 'ADJECTIVE'
FROM vocabulary v
WHERE v.word = '結構' AND v.hiragana = 'けっこう'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'cưới xin; đã lập gia đình; đã có chồng; đã có vợ; đã kết hôn; hôn nhân', '', 'VERB'
FROM vocabulary v
WHERE v.word = '結婚' AND v.hiragana = 'けっこん'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'ngày thứ hai; thứ Hai', '', 'ADVERB'
FROM vocabulary v
WHERE v.word = '月曜日' AND v.hiragana = 'げつようび'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'phòng ngoài; lối đi vào; sảnh trong nhà', '', 'ADJECTIVE'
FROM vocabulary v
WHERE v.word = '玄関' AND v.hiragana = 'げんかん'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'khoẻ; khoẻ mạnh; khoẻ khoắn; sức khoẻ; sự khoẻ mạnh', '', 'ADJECTIVE'
FROM vocabulary v
WHERE v.word = '元気' AND v.hiragana = 'げんき'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'năm; số 5', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '五' AND v.hiragana = 'ご'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'công viên; uyển; vườn', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '公園' AND v.hiragana = 'こうえん'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'bùng binh; ngã tư; điểm giao nhau; giao điểm', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '交差点' AND v.hiragana = 'こうさてん'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'chè đen; trà đen; hồng trà', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '紅茶' AND v.hiragana = 'こうちゃ'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'đồn cảnh sát', '', 'VERB'
FROM vocabulary v
WHERE v.word = '交番' AND v.hiragana = 'こうばん'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'tiếng; giọng nói; giọng nói', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '声' AND v.hiragana = 'こえ'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'áo khoác; áo bành tô; áo choàng', '', 'NOUN'
FROM vocabulary v
WHERE v.word = 'コート' AND v.hiragana = 'コート'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'cà phê; cà-phê', '', 'NOUN'
FROM vocabulary v
WHERE v.word = 'コーヒー' AND v.hiragana = 'コーヒー'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'vào buổi chiều; sau 12 giờ trưa; buổi chiều; chiều', '', 'ADVERB'
FROM vocabulary v
WHERE v.word = '午後' AND v.hiragana = 'ごご'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'mồng 9; ngày 9; ngày mồng 9; 9 ngày', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '九日' AND v.hiragana = 'ここのか'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, '9 cái; 9 chiếc', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '九つ' AND v.hiragana = 'ここのつ'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'buổi sáng; vào buổi sáng; sáng', '', 'ADVERB'
FROM vocabulary v
WHERE v.word = '午前' AND v.hiragana = 'ごぜん'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'trả lời', '', 'VERB'
FROM vocabulary v
WHERE v.word = '答える' AND v.hiragana = 'こたえる'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'phía này; bên này; hướng này; tôi; chúng tôi', '', 'NOUN'
FROM vocabulary v
WHERE v.word = 'こちら' AND v.hiragana = 'こちら'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'hướng này; phía này; ở đây; đây; này', '', 'NOUN'
FROM vocabulary v
WHERE v.word = 'こっち' AND v.hiragana = 'こっち'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'cái cốc; cái cốc; cúp; cốc; ca; cái ly', '', 'NOUN'
FROM vocabulary v
WHERE v.word = 'コップ' AND v.hiragana = 'コップ'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'năm nay', '', 'ADVERB'
FROM vocabulary v
WHERE v.word = '今年' AND v.hiragana = 'ことし'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'câu nói; ngôn ngữ; tiếng nói; lời ăn tiếng nói; từ ngữ; lời nói; lời', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '言葉' AND v.hiragana = 'ことば'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'bé con; bé thơ; con', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '子供' AND v.hiragana = 'こども'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'cơm; ăn cơm', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '御飯' AND v.hiragana = 'ごはん'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'chép; chép lại; sao chép', '', 'NOUN'
FROM vocabulary v
WHERE v.word = 'コピーする' AND v.hiragana = 'コピーする'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'bối rối; khó khăn (về tiền bạc, cuộc sống.v.v...); lúng túng', '', 'VERB'
FROM vocabulary v
WHERE v.word = '困る' AND v.hiragana = 'こまる'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, '(used to get the attention of one''s equals or inferiors) hey, oi, yo', '', 'NOUN'
FROM vocabulary v
WHERE v.word = 'これ' AND v.hiragana = 'これ'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'tháng này', '', 'ADVERB'
FROM vocabulary v
WHERE v.word = '今月' AND v.hiragana = 'こんげつ'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'tuần lễ này; tuần này', '', 'ADVERB'
FROM vocabulary v
WHERE v.word = '今週' AND v.hiragana = 'こんしゅう'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'như thế này (mức độ, số lượng, trạng thái, tính chất...)', '', 'ADJECTIVE'
FROM vocabulary v
WHERE v.word = 'こんな' AND v.hiragana = 'こんな'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'đêm nay; tối nay', '', 'ADVERB'
FROM vocabulary v
WHERE v.word = '今晩' AND v.hiragana = 'こんばん'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'nào; thôi nào; tiếp đi', '', 'CONJUNCTION'
FROM vocabulary v
WHERE v.word = 'さあ' AND v.hiragana = 'さあ'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'bao tượng; bóp; đãy tiền', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '財布' AND v.hiragana = 'さいふ'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'đầu mút; điểm đầu; tương lai; trước đây', '', 'ADJECTIVE'
FROM vocabulary v
WHERE v.word = '先' AND v.hiragana = 'さき'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'nở', '', 'VERB'
FROM vocabulary v
WHERE v.word = '咲く' AND v.hiragana = 'さく'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'sự đặt câu; sự viết văn; sự làm văn; đoạn văn', '', 'VERB'
FROM vocabulary v
WHERE v.word = '作文' AND v.hiragana = 'さくぶん'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'đặt cánh tay của bạn dưới nách người khác', '', 'VERB'
FROM vocabulary v
WHERE v.word = '差す' AND v.hiragana = 'さす'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'tạp chí; tạp san; tập san', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '雑誌' AND v.hiragana = 'ざっし'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'đường; đường (ăn)', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '砂糖' AND v.hiragana = 'さとう'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'cóng; hàn; lành lạnh', '', 'ADJECTIVE'
FROM vocabulary v
WHERE v.word = '寒い' AND v.hiragana = 'さむい'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'năm sau nữa; hai năm nữa', '', 'ADVERB'
FROM vocabulary v
WHERE v.word = 'さ来年' AND v.hiragana = 'さらいねん'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'tuy nhiên; nhưng', '', 'CONJUNCTION'
FROM vocabulary v
WHERE v.word = 'しかし' AND v.hiragana = 'しかし'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'giờ; giờ đồng hồ; giờ giấc', '', 'ADVERB'
FROM vocabulary v
WHERE v.word = '時間' AND v.hiragana = 'じかん'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'công việc; công của lực', '', 'VERB'
FROM vocabulary v
WHERE v.word = '仕事' AND v.hiragana = 'しごと'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'từ điển; tự điển', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '辞書' AND v.hiragana = 'じしょ'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'yên tĩnh, yên lặng; điềm tĩnh', '', 'ADJECTIVE'
FROM vocabulary v
WHERE v.word = '静か' AND v.hiragana = 'しずか'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'câu hỏi; chất vấn', '', 'VERB'
FROM vocabulary v
WHERE v.word = '質問' AND v.hiragana = 'しつもん'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'xe đạp', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '自転車' AND v.hiragana = 'じてんしゃ'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'xe con; xe hơi; xe ô tô', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '自動車' AND v.hiragana = 'じどうしゃ'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'chết; đi đời; lâm chung', '', 'VERB'
FROM vocabulary v
WHERE v.word = '死ぬ' AND v.hiragana = 'しぬ'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'từ điển; tự điển', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '字引' AND v.hiragana = 'じびき'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'bản thân mình; tự mình', '', 'ADJECTIVE'
FROM vocabulary v
WHERE v.word = '自分' AND v.hiragana = 'じぶん'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'đóng; bị đóng chặt; buộc chặt', '', 'VERB'
FROM vocabulary v
WHERE v.word = '閉まる' AND v.hiragana = 'しまる'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'đóng; gài', '', 'VERB'
FROM vocabulary v
WHERE v.word = '閉める' AND v.hiragana = 'しめる'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'buộc; buộc chặt; vặn chặt; kín', '', 'VERB'
FROM vocabulary v
WHERE v.word = '締める' AND v.hiragana = 'しめる'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'thế thì; vậy thì', '', 'NOUN'
FROM vocabulary v
WHERE v.word = 'じゃ' AND v.hiragana = 'じゃ'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'ảnh; bóng; hình ảnh', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '写真' AND v.hiragana = 'しゃしん'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'áo sơ mi; áo cánh; sơ mi', '', 'NOUN'
FROM vocabulary v
WHERE v.word = 'シャツ' AND v.hiragana = 'シャツ'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'buồng tắm vòi hoa sen; vòi hoa sen', '', 'NOUN'
FROM vocabulary v
WHERE v.word = 'シャワー' AND v.hiragana = 'シャワー'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'buổi học, giờ học; sự giảng bài; sự lên lớp', '', 'VERB'
FROM vocabulary v
WHERE v.word = '授業' AND v.hiragana = 'じゅぎょう'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'bài tập về nhà', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '宿題' AND v.hiragana = 'しゅくだい'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'xì dầu', '', 'NOUN'
FROM vocabulary v
WHERE v.word = 'しょうゆ' AND v.hiragana = 'しょうゆ'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'buồng ăn; nhà ăn; bếp ăn; phòng ăn (tại một ngôi chùa)', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '食堂' AND v.hiragana = 'しょくどう'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'biết; biết (có kinh nghiệm); biết (mặt)', '', 'VERB'
FROM vocabulary v
WHERE v.word = '知る' AND v.hiragana = 'しる'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'bên trắng; màu trắng; người da trắng', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '白' AND v.hiragana = 'しろ'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'màu trắng; sạch sẽ; trắng muốt; trắng', '', 'ADJECTIVE'
FROM vocabulary v
WHERE v.word = '白い' AND v.hiragana = 'しろい'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'báo; tờ báo; nhật báo; tờ báo', '', 'ADJECTIVE'
FROM vocabulary v
WHERE v.word = '新聞' AND v.hiragana = 'しんぶん'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'ngày thứ tư; thứ tư', '', 'ADVERB'
FROM vocabulary v
WHERE v.word = '水曜日' AND v.hiragana = 'すいようび'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'bú; hấp; hít; hít vào; hút', '', 'VERB'
FROM vocabulary v
WHERE v.word = '吸う' AND v.hiragana = 'すう'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'váy; váy; juýp; cáp scart', '', 'NOUN'
FROM vocabulary v
WHERE v.word = 'スカート' AND v.hiragana = 'スカート'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'sự thích; yêu; quý; mến', '', 'ADJECTIVE'
FROM vocabulary v
WHERE v.word = '好き' AND v.hiragana = 'すき'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'ít; hiếm; thiểu', '', 'ADJECTIVE'
FROM vocabulary v
WHERE v.word = '少ない' AND v.hiragana = 'すくない'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'ngay khi; ngay lập tức, tức thì, trực tiếp', '', 'NOUN'
FROM vocabulary v
WHERE v.word = 'すぐに' AND v.hiragana = 'すぐに'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'chút đỉnh; chút ít; hơi', '', 'ADVERB'
FROM vocabulary v
WHERE v.word = '少し' AND v.hiragana = 'すこし'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'bình tĩnh; mát; mát mẻ', '', 'ADJECTIVE'
FROM vocabulary v
WHERE v.word = '涼しい' AND v.hiragana = 'すずしい'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'lò; lò sưởi', '', 'NOUN'
FROM vocabulary v
WHERE v.word = 'ストーブ' AND v.hiragana = 'ストーブ'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'cái muỗng; cái thìa; muỗng', '', 'NOUN'
FROM vocabulary v
WHERE v.word = 'スプーン' AND v.hiragana = 'スプーン'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'thể thao', '', 'ADJECTIVE'
FROM vocabulary v
WHERE v.word = 'スポーツ' AND v.hiragana = 'スポーツ'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'quần; quần; quần dài', '', 'NOUN'
FROM vocabulary v
WHERE v.word = 'ズボン' AND v.hiragana = 'ズボン'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'có thể giải quyết; có thể đối phó được; cư trú; ở; trả nợ; trả xong', '', 'VERB'
FROM vocabulary v
WHERE v.word = '住む' AND v.hiragana = 'すむ'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'dép đi trong nhà; hài', '', 'NOUN'
FROM vocabulary v
WHERE v.word = 'スリッパ' AND v.hiragana = 'スリッパ'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'làm; thực hiện', '', 'VERB'
FROM vocabulary v
WHERE v.word = 'する' AND v.hiragana = 'する'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'ngồi; ngồi xuống', '', 'VERB'
FROM vocabulary v
WHERE v.word = '座る' AND v.hiragana = 'すわる'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'học sinh; học trò', '', 'ADJECTIVE'
FROM vocabulary v
WHERE v.word = '生徒' AND v.hiragana = 'せいと'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'áo len chui đầu; áo len dài tay', '', 'NOUN'
FROM vocabulary v
WHERE v.word = 'セーター' AND v.hiragana = 'セーター'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'bánh xà phòng; xà bông; xà phòng', '', 'NOUN'
FROM vocabulary v
WHERE v.word = 'せっけん' AND v.hiragana = 'せっけん'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'bộ com lê', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '背広' AND v.hiragana = 'せびろ'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'bé; chật; chật chội', '', 'ADJECTIVE'
FROM vocabulary v
WHERE v.word = '狭い' AND v.hiragana = 'せまい'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'số không; số không; sự không có gì', '', 'NOUN'
FROM vocabulary v
WHERE v.word = 'ゼロ' AND v.hiragana = 'ゼロ'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'một nghìn; ngàn; nghìn', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '千' AND v.hiragana = 'せん'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'tháng trước', '', 'ADVERB'
FROM vocabulary v
WHERE v.word = '先月' AND v.hiragana = 'せんげつ'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'tuần lễ trước; tuần trước', '', 'ADVERB'
FROM vocabulary v
WHERE v.word = '先週' AND v.hiragana = 'せんしゅう'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'giáo viên; giảng viên; thầy; ông giáo; ông thầy', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '先生' AND v.hiragana = 'せんせい'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'sự giặt giũ; quần áo được giặt giũ', '', 'VERB'
FROM vocabulary v
WHERE v.word = '洗濯' AND v.hiragana = 'せんたく'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'cả thảy; hết cả; hết thảy', '', 'ADJECTIVE'
FROM vocabulary v
WHERE v.word = '全部' AND v.hiragana = 'ぜんぶ'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'nơi đó', '', 'ADJECTIVE'
FROM vocabulary v
WHERE v.word = 'そちら' AND v.hiragana = 'そちら'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'nơi đó', '', 'NOUN'
FROM vocabulary v
WHERE v.word = 'そっち' AND v.hiragana = 'そっち'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'phía', '', 'NOUN'
FROM vocabulary v
WHERE v.word = 'そば' AND v.hiragana = 'そば'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'sau đó; từ sau đó', '', 'NOUN'
FROM vocabulary v
WHERE v.word = 'それから' AND v.hiragana = 'それから'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'trong trường hợp đó; sau đó; vậy thì', '', 'NOUN'
FROM vocabulary v
WHERE v.word = 'それでは' AND v.hiragana = 'それでは'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'đại học; trường đại học', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '大学' AND v.hiragana = 'だいがく'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'đại sứ quán; tòa đại sứ', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '大使館' AND v.hiragana = 'たいしかん'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'an toàn; chắc chắn; được; ổn; ok', '', 'ADJECTIVE'
FROM vocabulary v
WHERE v.word = '大丈夫' AND v.hiragana = 'だいじょうぶ'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'rất thích', '', 'ADJECTIVE'
FROM vocabulary v
WHERE v.word = '大好き' AND v.hiragana = 'だいすき'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'quan trọng; sự quan trọng', '', 'ADJECTIVE'
FROM vocabulary v
WHERE v.word = '大切' AND v.hiragana = 'たいせつ'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'bếp; bếp núc; bếp nước', '', 'ADJECTIVE'
FROM vocabulary v
WHERE v.word = '台所' AND v.hiragana = 'だいどころ'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'opposite side', '', 'NOUN'
FROM vocabulary v
WHERE v.word = 'たいへん' AND v.hiragana = 'たいへん'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'cao; đắt; đắt tiền', '', 'ADJECTIVE'
FROM vocabulary v
WHERE v.word = '高い' AND v.hiragana = 'たかい'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'đủ; nhiều', '', 'ADJECTIVE'
FROM vocabulary v
WHERE v.word = 'たくさん' AND v.hiragana = 'たくさん'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'tắc xi; tắc-xi; xe tắc xi', '', 'NOUN'
FROM vocabulary v
WHERE v.word = 'タクシー' AND v.hiragana = 'タクシー'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'gửi đi; cho ra khỏi; xuất bản', '', 'VERB'
FROM vocabulary v
WHERE v.word = '出す' AND v.hiragana = 'だす'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'đứng; đứng lên; đứng dậy', '', 'VERB'
FROM vocabulary v
WHERE v.word = '立つ' AND v.hiragana = 'たつ'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'làm tươi; chỉ cần làm', '', 'ADJECTIVE'
FROM vocabulary v
WHERE v.word = 'たて' AND v.hiragana = 'たて'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'tòa nhà; ngôi nhà; công trình kiến trúc', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '建物' AND v.hiragana = 'たてもの'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'dí dỏm; khoái ý; sướng', '', 'ADJECTIVE'
FROM vocabulary v
WHERE v.word = '楽しい' AND v.hiragana = 'たのしい'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'Nhờ', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '頼む' AND v.hiragana = 'たのむ'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'điếu thuốc; thuốc; thuốc lá', '', 'NOUN'
FROM vocabulary v
WHERE v.word = 'たばこ' AND v.hiragana = 'たばこ'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'đa phần; rất nhiều; rất lớn', '', 'ADVERB'
FROM vocabulary v
WHERE v.word = 'たぶん' AND v.hiragana = 'たぶん'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'đồ ăn; món ăn; thức', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '食べ物' AND v.hiragana = 'たべもの'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'ăn', '', 'VERB'
FROM vocabulary v
WHERE v.word = '食べる' AND v.hiragana = 'たべる'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'trứng; quả trứng; noãn; tế bào trứng', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '卵' AND v.hiragana = 'たまご'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'ngày sinh; ngày sinh nhật', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '誕生日' AND v.hiragana = 'たんじょうび'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'thank you (dialect from the Izumo region of Shimane Prefecture)', '', 'NOUN'
FROM vocabulary v
WHERE v.word = 'だんだん' AND v.hiragana = 'だんだん'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'bé; bé bỏng; bé nhỏ', '', 'ADJECTIVE'
FROM vocabulary v
WHERE v.word = '小さい' AND v.hiragana = 'ちいさい'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'small, little, tiny', '', 'ADJECTIVE'
FROM vocabulary v
WHERE v.word = '小さな' AND v.hiragana = 'ちいさな'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'cận; gần; cạnh; kề sát; ngay cạnh; ngay sát; giống như; gần như; tương tự', '', 'ADJECTIVE'
FROM vocabulary v
WHERE v.word = '近い' AND v.hiragana = 'ちかい'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'khác; khác nhau; không giống; trái ngược; không phù hợp; lầm lẫn; sai', '', 'VERB'
FROM vocabulary v
WHERE v.word = '違う' AND v.hiragana = 'ちがう'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'cận; gần; ở gần; cạnh; kề; kề bên; ngay cạnh; ngay sát; hàng xóm', '', 'ADVERB'
FROM vocabulary v
WHERE v.word = '近く' AND v.hiragana = 'ちかく'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'tàu điện ngầm; xe điện ngầm', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '地下鉄' AND v.hiragana = 'ちかてつ'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'bản đồ; địa đồ', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '地図' AND v.hiragana = 'ちず'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'màu nâu nhạt', '', 'ADJECTIVE'
FROM vocabulary v
WHERE v.word = '茶色' AND v.hiragana = 'ちゃいろ'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'bát cơm; chén uống chè', '', 'NOUN'
FROM vocabulary v
WHERE v.word = 'ちゃわん' AND v.hiragana = 'ちゃわん'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'vừa đúng; vừa chuẩn', '', 'ADJECTIVE'
FROM vocabulary v
WHERE v.word = 'ちょうど' AND v.hiragana = 'ちょうど'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'một chút; một lát; một lúc; hơi hơi', '', 'NOUN'
FROM vocabulary v
WHERE v.word = 'ちょっと' AND v.hiragana = 'ちょっと'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'dụng; sử dụng; dùng; xài', '', 'VERB'
FROM vocabulary v
WHERE v.word = '使う' AND v.hiragana = 'つかう'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'cũ rồi; mệt; mệt mỏi', '', 'VERB'
FROM vocabulary v
WHERE v.word = '疲れる' AND v.hiragana = 'つかれる'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'đến (một địa điểm); tới; vào (vị trí)', '', 'VERB'
FROM vocabulary v
WHERE v.word = '着く' AND v.hiragana = 'つく'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'bàn; bàn viết, bàn (dùng để học bài, làm việc công sở...)', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '机' AND v.hiragana = 'つくえ'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'ngâm (quần áo,...); tẩm, ướp (gia vị...)', '', 'VERB'
FROM vocabulary v
WHERE v.word = 'つける' AND v.hiragana = 'つける'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'làm việc', '', 'VERB'
FROM vocabulary v
WHERE v.word = '勤める' AND v.hiragana = 'つとめる'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'chán; không ra cái gì; không đáng gì', '', 'ADJECTIVE'
FROM vocabulary v
WHERE v.word = 'つまらない' AND v.hiragana = 'つまらない'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'lành lạnh; lạnh nhạt; lạnh lùng; lạnh; lạnh buốt; lạnh giá; lạnh lẽo', '', 'ADJECTIVE'
FROM vocabulary v
WHERE v.word = '冷たい' AND v.hiragana = 'つめたい'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'khỏe; mạnh; khoẻ; bền; tốt', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '強い' AND v.hiragana = 'つよい'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'bàn tay; tay', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '手' AND v.hiragana = 'て'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'băng cát sét; video; băng; dải dây; băng dính', '', 'NOUN'
FROM vocabulary v
WHERE v.word = 'テープ' AND v.hiragana = 'テープ'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'bàn; bàn; cái bàn', '', 'ADJECTIVE'
FROM vocabulary v
WHERE v.word = 'テーブル' AND v.hiragana = 'テーブル'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'máy ghi âm; máy hát; máy thu băng', '', 'NOUN'
FROM vocabulary v
WHERE v.word = 'テープレコーダー' AND v.hiragana = 'テープレコーダー'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'đăng trình; ra; rời khỏi', '', 'VERB'
FROM vocabulary v
WHERE v.word = '出かける' AND v.hiragana = 'でかける'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'bức thơ; bức thư; phong thơ', '', 'ADJECTIVE'
FROM vocabulary v
WHERE v.word = '手紙' AND v.hiragana = 'てがみ'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'cổng ra; cửa ra; lối ra', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '出口' AND v.hiragana = 'でぐち'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'bài kiểm tra; cuộc thí nghiệm; sự kiểm tra; thử; thí nghiệm', '', 'VERB'
FROM vocabulary v
WHERE v.word = 'テスト' AND v.hiragana = 'テスト'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'trung tâm thương mại', '', 'NOUN'
FROM vocabulary v
WHERE v.word = 'デパート' AND v.hiragana = 'デパート'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'đi ra; ngoi; xuất hiện; đi ra khỏi', '', 'VERB'
FROM vocabulary v
WHERE v.word = '出る' AND v.hiragana = 'でる'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'máy tuốc bin; máy vô tuyến truyền hình; ti vi; vô tuyến', '', 'NOUN'
FROM vocabulary v
WHERE v.word = 'テレビ' AND v.hiragana = 'テレビ'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'thời tiết', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '天気' AND v.hiragana = 'てんき'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'điện khí; điện; đèn điện; điện', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '電気' AND v.hiragana = 'でんき'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'tàu điện; tàu lửa; xe điện', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '電車' AND v.hiragana = 'でんしゃ'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'điện thoại; máy điện thoại', '', 'VERB'
FROM vocabulary v
WHERE v.word = '電話' AND v.hiragana = 'でんわ'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'cửa; cửa; cửa ra vào; cánh cửa ra vào', '', 'NOUN'
FROM vocabulary v
WHERE v.word = 'ドア' AND v.hiragana = 'ドア'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'cầu tiên; toa-lét; nhà vệ sinh', '', 'NOUN'
FROM vocabulary v
WHERE v.word = 'トイレ' AND v.hiragana = 'トイレ'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'ái chà; kì thực; như thế nào', '', 'ADVERB'
FROM vocabulary v
WHERE v.word = 'どうして' AND v.hiragana = 'どうして'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'xin mời', '', 'ADVERB'
FROM vocabulary v
WHERE v.word = 'どうぞ' AND v.hiragana = 'どうぞ'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'động vật; muông thú; súc vật', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '動物' AND v.hiragana = 'どうぶつ'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'Xin chào; Cảm ơn; hơi hơi; có vẻ; mập mờ', '', 'NOUN'
FROM vocabulary v
WHERE v.word = 'どうも' AND v.hiragana = 'どうも'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'hẻo; viễn; xa lắc', '', 'ADJECTIVE'
FROM vocabulary v
WHERE v.word = '遠い' AND v.hiragana = 'とおい'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'mười ngày; ngày mùng mười; ngày mười', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '十日' AND v.hiragana = 'とおか'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'có lúc; thỉnh thoảng; đôi khi; lắm khi', '', 'ADJECTIVE'
FROM vocabulary v
WHERE v.word = '時々' AND v.hiragana = 'ときどき'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'đồng hồ', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '時計' AND v.hiragana = 'とけい'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'ở đâu; ở chỗ nào', '', 'ADJECTIVE'
FROM vocabulary v
WHERE v.word = 'どこ' AND v.hiragana = 'どこ'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'nơi; chỗ', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '所' AND v.hiragana = 'ところ'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'năm; năm tháng; tuổi', '', 'ADVERB'
FROM vocabulary v
WHERE v.word = '年' AND v.hiragana = 'とし'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'thư quán; thư viện', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '図書館' AND v.hiragana = 'としょかん'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'phía nào; cái nào; người nào', '', 'NOUN'
FROM vocabulary v
WHERE v.word = 'どちら' AND v.hiragana = 'どちら'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'phía nào', '', 'NOUN'
FROM vocabulary v
WHERE v.word = 'どっち' AND v.hiragana = 'どっち'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'rất; cực kỳ', '', 'ADVERB'
FROM vocabulary v
WHERE v.word = 'とても' AND v.hiragana = 'とても'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'vị nào', '', 'NOUN'
FROM vocabulary v
WHERE v.word = 'どなた' AND v.hiragana = 'どなた'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'bên cạnh; cạnh; sự giáp bên; sự ngay bên cạnh', '', 'ADJECTIVE'
FROM vocabulary v
WHERE v.word = '隣' AND v.hiragana = 'となり'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'cung điện; lâu đài', '', 'NOUN'
FROM vocabulary v
WHERE v.word = 'どの' AND v.hiragana = 'どの'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'bay nhảy; bay tán loạn; bay lả tả; bay; cất cánh bay; bay liệng', '', 'VERB'
FROM vocabulary v
WHERE v.word = '飛ぶ' AND v.hiragana = 'とぶ'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'bạn; bạn bè; bè bạn', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '友達' AND v.hiragana = 'ともだち'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'bảy; ngày thứ bẩy; Thứ bảy', '', 'ADVERB'
FROM vocabulary v
WHERE v.word = '土曜日' AND v.hiragana = 'どようび'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'chim chóc; chim; gia cầm; điểu', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '鳥' AND v.hiragana = 'とり'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'Thịt gà', '', 'NOUN'
FROM vocabulary v
WHERE v.word = 'とり肉' AND v.hiragana = 'とりにく'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'bắt giữ; biểu thị; biểu quyết; cầm lấy', '', 'VERB'
FROM vocabulary v
WHERE v.word = '取る' AND v.hiragana = 'とる'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'chụp (ảnh); làm (phim)', '', 'VERB'
FROM vocabulary v
WHERE v.word = '撮る' AND v.hiragana = 'とる'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'con dao; dao; dao; dao nhíp', '', 'NOUN'
FROM vocabulary v
WHERE v.word = 'ナイフ' AND v.hiragana = 'ナイフ'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'bao lâu; dài; lâu', '', 'ADJECTIVE'
FROM vocabulary v
WHERE v.word = '長い' AND v.hiragana = 'ながい'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'kêu; hót; hú; rống', '', 'VERB'
FROM vocabulary v
WHERE v.word = '鳴く' AND v.hiragana = 'なく'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'đánh mất, làm mất; loại bỏ, bỏ ra, loại ra', '', 'VERB'
FROM vocabulary v
WHERE v.word = '無くす' AND v.hiragana = 'なくす'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'vì sao', '', 'ADVERB'
FROM vocabulary v
WHERE v.word = 'なぜ' AND v.hiragana = 'なぜ'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'hạ; mùa hè; mùa hạ; mùa hè (theo lịch âm dương: ngày 16 của tháng thứ 4 đến ngày 15 của tháng thứ 7)', '', 'ADVERB'
FROM vocabulary v
WHERE v.word = '夏' AND v.hiragana = 'なつ'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'nghỉ hè', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '夏休み' AND v.hiragana = 'なつやすみ'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'Vân vân', '', 'PARTICLE'
FROM vocabulary v
WHERE v.word = 'など' AND v.hiragana = 'など'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'bảy cái', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '七つ' AND v.hiragana = 'ななつ'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'tên; họ tên', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '名前' AND v.hiragana = 'なまえ'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'học tập; luyện tập; học', '', 'VERB'
FROM vocabulary v
WHERE v.word = '習う' AND v.hiragana = 'ならう'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'được xếp; được bài trí', '', 'VERB'
FROM vocabulary v
WHERE v.word = '並ぶ' AND v.hiragana = 'ならぶ'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'bày; sắp hàng; sắp; bày; bày đặt; bài trí', '', 'VERB'
FROM vocabulary v
WHERE v.word = '並べる' AND v.hiragana = 'ならべる'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'that is in; who is called, that is called; that is', '', 'ADJECTIVE'
FROM vocabulary v
WHERE v.word = 'なる' AND v.hiragana = 'なる'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'hai; số hai', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '二' AND v.hiragana = 'に'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'sôi nổi; náo nhiệt; sống động; huyên náo', '', 'ADJECTIVE'
FROM vocabulary v
WHERE v.word = '賑やか' AND v.hiragana = 'にぎやか'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'thịt', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '肉' AND v.hiragana = 'にく'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'hướng tây; phía tây', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '西' AND v.hiragana = 'にし'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'Chủ Nhật; ngày Chủ Nhật; chúa nhật', '', 'ADVERB'
FROM vocabulary v
WHERE v.word = '日曜日' AND v.hiragana = 'にちようび'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'hành lý', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '荷物' AND v.hiragana = 'にもつ'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'bản tin; thời sự; thông tin', '', 'NOUN'
FROM vocabulary v
WHERE v.word = 'ニュース' AND v.hiragana = 'ニュース'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'vườn', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '庭' AND v.hiragana = 'にわ'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'cởi (quần áo, giày); bỏ (mũ); lột', '', 'VERB'
FROM vocabulary v
WHERE v.word = '脱ぐ' AND v.hiragana = 'ぬぐ'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'nguội; âm ấm', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '温い' AND v.hiragana = 'ぬるい'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'ca vát; cavát; caravát; cà vạt', '', 'NOUN'
FROM vocabulary v
WHERE v.word = 'ネクタイ' AND v.hiragana = 'ネクタイ'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'mèo', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '猫' AND v.hiragana = 'ねこ'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'đặt lưng; đặt mình; nằm', '', 'VERB'
FROM vocabulary v
WHERE v.word = '寝る' AND v.hiragana = 'ねる'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'máy vi tính xách tay; sổ; vở', '', 'VERB'
FROM vocabulary v
WHERE v.word = 'ノート' AND v.hiragana = 'ノート'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'được đưa ra; được đặt ra (trong chương trình); được thăng chức; giương buồm', '', 'VERB'
FROM vocabulary v
WHERE v.word = '登る' AND v.hiragana = 'のぼる'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'đồ uống; thức uống', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '飲み物' AND v.hiragana = 'のみもの'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'húp; uống', '', 'VERB'
FROM vocabulary v
WHERE v.word = '飲む' AND v.hiragana = 'のむ'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'cưỡi; lên xe; lên tàu; đi (tàu, xe); vào (nhịp); có hứng', '', 'VERB'
FROM vocabulary v
WHERE v.word = '乗る' AND v.hiragana = 'のる'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'răng', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '歯' AND v.hiragana = 'は'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'bữa tiệc; buổi tiệc; liên hoan', '', 'NOUN'
FROM vocabulary v
WHERE v.word = 'パーティー' AND v.hiragana = 'パーティー'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'bưu thiếp', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '葉書' AND v.hiragana = 'はがき'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'măc, mang, đeo', '', 'VERB'
FROM vocabulary v
WHERE v.word = 'はく' AND v.hiragana = 'はく'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'hòm; hộp; kiện hàng', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '箱' AND v.hiragana = 'はこ'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'cầu; cầu não', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '橋' AND v.hiragana = 'はし'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'bờ; cạnh, lề; chót', '', 'NOUN'
FROM vocabulary v
WHERE v.word = 'はし' AND v.hiragana = 'はし'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'bắt đầu; khởi đầu; 開始する; 遡る', '', 'VERB'
FROM vocabulary v
WHERE v.word = '始まる' AND v.hiragana = 'はじまる'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'ban đầu; lần đầu; khởi đầu', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '初め' AND v.hiragana = 'はじめ'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'lần đầu tiên; mới', '', 'ADJECTIVE'
FROM vocabulary v
WHERE v.word = '初めて' AND v.hiragana = 'はじめて'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'chạy; tẩu', '', 'VERB'
FROM vocabulary v
WHERE v.word = '走る' AND v.hiragana = 'はしる'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'đàn công-trơ-bas; giọng trầm; giọng nam trầm; sự tắm rửa; sự tắm bồn', '', 'NOUN'
FROM vocabulary v
WHERE v.word = 'バス' AND v.hiragana = 'バス'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'bơ', '', 'NOUN'
FROM vocabulary v
WHERE v.word = 'バター' AND v.hiragana = 'バター'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'đôi mươi; hai mươi tuổi', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '二十歳' AND v.hiragana = 'はたち'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'làm lụng; lao động; hoạt động; phạm (tội); làm việc; làm', '', 'VERB'
FROM vocabulary v
WHERE v.word = '働く' AND v.hiragana = 'はたらく'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'bát; bát quác; tám', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '八' AND v.hiragana = 'はち'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'ngày hai mươi; hai mươi ngày', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '二十日' AND v.hiragana = 'はつか'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'bông hoa; đóa hoa; hoa', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '花' AND v.hiragana = 'はな'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'mũi', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '鼻' AND v.hiragana = 'はな'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'câu chuyện; sự nói chuyện; sự hội thoại', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '話' AND v.hiragana = 'はなし'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'bàn tán; chuyện; chuyện trò', '', 'VERB'
FROM vocabulary v
WHERE v.word = '話す' AND v.hiragana = 'はなす'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'sớm; nhanh chóng ( thời gian)', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '早い' AND v.hiragana = 'はやい'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'chóng; lẹ; mau lẹ', '', 'ADJECTIVE'
FROM vocabulary v
WHERE v.word = '速い' AND v.hiragana = 'はやい'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'mùa xuân; xuân', '', 'ADVERB'
FROM vocabulary v
WHERE v.word = '春' AND v.hiragana = 'はる'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'dán; gắn cho; gắn', '', 'VERB'
FROM vocabulary v
WHERE v.word = '貼る' AND v.hiragana = 'はる'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'trời nắng', '', 'ADJECTIVE'
FROM vocabulary v
WHERE v.word = '晴れ' AND v.hiragana = 'はれ'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'nắng; tạnh', '', 'VERB'
FROM vocabulary v
WHERE v.word = '晴れる' AND v.hiragana = 'はれる'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'bán; một nửa; nửa', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '半' AND v.hiragana = 'はん'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'buổi tối; đêm; muộn', '', 'ADVERB'
FROM vocabulary v
WHERE v.word = '晩' AND v.hiragana = 'ばん'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'bánh mì; bánh mỳ; chảo; cái chảo', '', 'NOUN'
FROM vocabulary v
WHERE v.word = 'パン' AND v.hiragana = 'パン'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'khăn mùi xoa; khăn tay; mùi xoa', '', 'NOUN'
FROM vocabulary v
WHERE v.word = 'ハンカチ' AND v.hiragana = 'ハンカチ'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'số hiệu; số liệu', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '番号' AND v.hiragana = 'ばんごう'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'bữa tối; cơm chiều; cơm tối', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '晩御飯' AND v.hiragana = 'ばんごはん'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'một nửa', '', 'ADVERB'
FROM vocabulary v
WHERE v.word = '半分' AND v.hiragana = 'はんぶん'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'chăng; dẫn; kéo; rút; bị (cảm); tra', '', 'VERB'
FROM vocabulary v
WHERE v.word = '引く' AND v.hiragana = 'ひく'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'lè tè; thấp', '', 'ADJECTIVE'
FROM vocabulary v
WHERE v.word = '低い' AND v.hiragana = 'ひくい'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'máy bay; phi cơ; tàu bay', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '飛行機' AND v.hiragana = 'ひこうき'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'bên trái; tả; trái', '', 'ADJECTIVE'
FROM vocabulary v
WHERE v.word = '左' AND v.hiragana = 'ひだり'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'một', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '一つ' AND v.hiragana = 'ひとつ'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'tháng giêng; tháng Một', '', 'ADVERB'
FROM vocabulary v
WHERE v.word = '一月' AND v.hiragana = 'ひとつき'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'một trăm; trăm', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '百' AND v.hiragana = 'ひゃく'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'bệnh viện; nhà thương; sinh bệnh', '', 'ADJECTIVE'
FROM vocabulary v
WHERE v.word = '病院' AND v.hiragana = 'びょういん'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'bệnh tật; bệnh; sự ốm; bịnh; đau ốm', '', 'VERB'
FROM vocabulary v
WHERE v.word = '病気' AND v.hiragana = 'びょうき'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'ban trưa; buổi trưa; ban ngày; trưa', '', 'ADVERB'
FROM vocabulary v
WHERE v.word = '昼' AND v.hiragana = 'ひる'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'bữa trưa; cơm trưa', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '昼御飯' AND v.hiragana = 'ひるごはん'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'rộng; rộng rãi; rộng lớn', '', 'ADJECTIVE'
FROM vocabulary v
WHERE v.word = '広い' AND v.hiragana = 'ひろい'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'phim; cuộn phim (ảnh...)', '', 'NOUN'
FROM vocabulary v
WHERE v.word = 'フィルム' AND v.hiragana = 'フィルム'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'bao thư; phong bì; phong thơ', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '封筒' AND v.hiragana = 'ふうとう'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'bể; bể bơi; vùng chứa, vùng trữ', '', 'NOUN'
FROM vocabulary v
WHERE v.word = 'プール' AND v.hiragana = 'プール'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'cái nĩa; dân ca; dân gian; dĩa; nĩa', '', 'NOUN'
FROM vocabulary v
WHERE v.word = 'フォーク' AND v.hiragana = 'フォーク'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'dậy mùi; hắt hiu; phát ra; bốc ra; tỏa ra', '', 'VERB'
FROM vocabulary v
WHERE v.word = '吹く' AND v.hiragana = 'ふく'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'quần áo; bộ', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '服' AND v.hiragana = 'ふく'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'hai', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '二つ' AND v.hiragana = 'ふたつ'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'thịt heo; thịt lợn', '', 'ADJECTIVE'
FROM vocabulary v
WHERE v.word = '豚肉' AND v.hiragana = 'ぶたにく'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'ngày mùng hai', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '二日' AND v.hiragana = 'ふつか'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'béo; dày; to; mập', '', 'ADJECTIVE'
FROM vocabulary v
WHERE v.word = '太い' AND v.hiragana = 'ふとい'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'đông; mùa đông', '', 'ADVERB'
FROM vocabulary v
WHERE v.word = '冬' AND v.hiragana = 'ふゆ'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'rơi (mưa); đổ (mưa)', '', 'VERB'
FROM vocabulary v
WHERE v.word = '降る' AND v.hiragana = 'ふる'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'cũ; cổ; già', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '古い' AND v.hiragana = 'ふるい'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'bể tắm; bồn tắm', '', 'NOUN'
FROM vocabulary v
WHERE v.word = 'ふろ' AND v.hiragana = 'ふろ'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'đoạn văn (đôi khi chỉ là một câu văn); văn chương', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '文章' AND v.hiragana = 'ぶんしょう'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'trang; trang (sách vở)', '', 'NOUN'
FROM vocabulary v
WHERE v.word = 'ページ' AND v.hiragana = 'ページ'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'phần phía dưới; vị trí thấp kém; thứ hạng thấp', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '下手' AND v.hiragana = 'へた'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'giường; giường ngủ; giường', '', 'NOUN'
FROM vocabulary v
WHERE v.word = 'ベッド' AND v.hiragana = 'ベッド'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'động vật cảnh; người được cưng chiều', '', 'NOUN'
FROM vocabulary v
WHERE v.word = 'ペット' AND v.hiragana = 'ペット'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'buồng; căn buồng; phòng', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '部屋' AND v.hiragana = 'へや'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'cạnh (hình học); nơi xa; nơi hẻo lánh; trình độ; mức độ', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '辺' AND v.hiragana = 'へん'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'bút; bút máy; bút; bút máy', '', 'NOUN'
FROM vocabulary v
WHERE v.word = 'ペン' AND v.hiragana = 'ペン'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'thuận tiện; tiện lợi', '', 'ADJECTIVE'
FROM vocabulary v
WHERE v.word = '便利' AND v.hiragana = 'べんり'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'mũ; nón', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '帽子' AND v.hiragana = 'ぼうし'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'bút bi; bút nguyên tử', '', 'NOUN'
FROM vocabulary v
WHERE v.word = 'ボールペン' AND v.hiragana = 'ボールペン'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'ngoài', '', 'ADJECTIVE'
FROM vocabulary v
WHERE v.word = 'ほか' AND v.hiragana = 'ほか'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'túi; túi áo; túi quần; áo', '', 'NOUN'
FROM vocabulary v
WHERE v.word = 'ポケット' AND v.hiragana = 'ポケット'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'muốn; mong muốn', '', 'ADJECTIVE'
FROM vocabulary v
WHERE v.word = '欲しい' AND v.hiragana = 'ほしい'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'thùng thư; hòm thư; hộp thư; quá trình post máy tính (power on self test); tự kiểm tra khi nguồn bật (post)', '', 'VERB'
FROM vocabulary v
WHERE v.word = 'ポスト' AND v.hiragana = 'ポスト'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'thon dài; mảnh mai , mỏng; cảm giác không đủ về số lượng', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '細い' AND v.hiragana = 'ほそい'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'cúc; khuy; khuy áo', '', 'NOUN'
FROM vocabulary v
WHERE v.word = 'ボタン' AND v.hiragana = 'ボタン'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'khách sạn', '', 'NOUN'
FROM vocabulary v
WHERE v.word = 'ホテル' AND v.hiragana = 'ホテル'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'cái; chiếc; điếu; bông; sách; này; nay', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '本' AND v.hiragana = 'ほん'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'giá sách; kệ sách; tủ sách', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '本棚' AND v.hiragana = 'ほんだな'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'sự tăng vọt (giá cả); sự bùng nổ giá cả', '', 'VERB'
FROM vocabulary v
WHERE v.word = 'ほんとう' AND v.hiragana = 'ほんとう'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'hàng sáng; mỗi sáng', '', 'ADVERB'
FROM vocabulary v
WHERE v.word = '毎朝' AND v.hiragana = 'まいあさ'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'hàng tháng; mỗi tháng', '', 'ADVERB'
FROM vocabulary v
WHERE v.word = '毎月' AND v.hiragana = 'まいげつ'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'hàng tuần; mỗi tuần', '', 'ADVERB'
FROM vocabulary v
WHERE v.word = '毎週' AND v.hiragana = 'まいしゅう'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'hàng ngày; mỗi ngày; mọi ngày', '', 'ADVERB'
FROM vocabulary v
WHERE v.word = '毎日' AND v.hiragana = 'まいにち'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'hàng năm; mỗi năm; mọi năm; thường niên', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '毎年' AND v.hiragana = 'まいねん'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'đêm đêm; hàng tối; tối tối', '', 'ADVERB'
FROM vocabulary v
WHERE v.word = '毎晩' AND v.hiragana = 'まいばん'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'cong; cúi; ẹo', '', 'VERB'
FROM vocabulary v
WHERE v.word = '曲る' AND v.hiragana = 'まがる'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'dại dột; không thận trọng; dở; vụng; chán (món ăn); không ngon; khó chịu; xấu', '', 'ADJECTIVE'
FROM vocabulary v
WHERE v.word = 'まずい' AND v.hiragana = 'まずい'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'cũng; lần nữa', '', 'NOUN'
FROM vocabulary v
WHERE v.word = 'また' AND v.hiragana = 'また'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'chưa; vẫn; hơn nữa; bên cạnh đó; vẫn', '', 'ADVERB'
FROM vocabulary v
WHERE v.word = 'まだ' AND v.hiragana = 'まだ'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'thị trấn; con phố', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '町' AND v.hiragana = 'まち'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'chờ; chờ đợi; đợi', '', 'VERB'
FROM vocabulary v
WHERE v.word = '待つ' AND v.hiragana = 'まつ'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'thẳng (phía trước); trực tiếp; trụ đứng; đứng thẳng; trung thực; thành thật', '', 'ADJECTIVE'
FROM vocabulary v
WHERE v.word = 'まっすぐ' AND v.hiragana = 'まっすぐ'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'phù hợp, hợp; diêm; ngòi cháy; quẹt', '', 'NOUN'
FROM vocabulary v
WHERE v.word = 'マッチ' AND v.hiragana = 'マッチ'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'cửa sổ; khoảng trống giá; khoảng trống', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '窓' AND v.hiragana = 'まど'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'bút máy; viết máy', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '万年筆' AND v.hiragana = 'まんねんひつ'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'đánh bóng; làm sáng bóng; mài bóng; mài; đánh; chải; trau chuốt; cải thiện; gọt giũa', '', 'VERB'
FROM vocabulary v
WHERE v.word = '磨く' AND v.hiragana = 'みがく'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'bên phải; phía bên phải; hữu', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '右' AND v.hiragana = 'みぎ'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'cụt; hụt; ngắn', '', 'ADJECTIVE'
FROM vocabulary v
WHERE v.word = '短い' AND v.hiragana = 'みじかい'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'nước', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '水' AND v.hiragana = 'みず'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'cho xem; cho thấy; chứng tỏ; bày tỏ', '', 'VERB'
FROM vocabulary v
WHERE v.word = '見せる' AND v.hiragana = 'みせる'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'con đường; con phố; đạo; đường', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '道' AND v.hiragana = 'みち'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'ba ngày; ngày mùng ba', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '三日' AND v.hiragana = 'みっか'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'ba', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '三つ' AND v.hiragana = 'みっつ'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'màu xanh lá cây; xanh', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '緑' AND v.hiragana = 'みどり'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'các anh; các vị; tất cả mọi người', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '皆さん' AND v.hiragana = 'みなさん'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'cái tai; tai', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '耳' AND v.hiragana = 'みみ'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'xem; kiểm tra đánh giá; trông coi; chăm sóc', '', 'VERB'
FROM vocabulary v
WHERE v.word = '見る 観る' AND v.hiragana = 'みる'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'mọi người; tất cả mọi người', '', 'ADVERB'
FROM vocabulary v
WHERE v.word = 'みんな' AND v.hiragana = 'みんな'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'ngày thứ sáu', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '六日' AND v.hiragana = 'むいか'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'phía bên kia; mặt bên kia; cạnh bên kia; phía trước; phía đối diện', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '向こう' AND v.hiragana = 'むこう'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'khó; khó khăn', '', 'ADJECTIVE'
FROM vocabulary v
WHERE v.word = '難しい' AND v.hiragana = 'むずかしい'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'sáu', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '六つ' AND v.hiragana = 'むっつ'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'làng; làng mạc; thôn xã', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '村' AND v.hiragana = 'むら'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'con mắt; mắt', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '目' AND v.hiragana = 'め'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'mét; thuộc về mét; mét (m)', '', 'NOUN'
FROM vocabulary v
WHERE v.word = 'メートル' AND v.hiragana = 'メートル'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'đã, rồi', '', 'ADVERB'
FROM vocabulary v
WHERE v.word = 'もう' AND v.hiragana = 'もう'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'lại; lần nữa; thêm một lần nữa', '', 'NOUN'
FROM vocabulary v
WHERE v.word = 'もう一度' AND v.hiragana = 'もういちど'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'ngày thứ năm; thứ năm', '', 'ADVERB'
FROM vocabulary v
WHERE v.word = '木曜日' AND v.hiragana = 'もくようび'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'cầm; nắm; mang; chịu (phí tổn); đảm nhiệm; có', '', 'VERB'
FROM vocabulary v
WHERE v.word = '持つ' AND v.hiragana = 'もつ'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'nữa; hơn nữa; thêm', '', 'ADVERB'
FROM vocabulary v
WHERE v.word = 'もっと' AND v.hiragana = 'もっと'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'đồ vật; vật', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '物' AND v.hiragana = 'もの'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'vấn đề', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '問題' AND v.hiragana = 'もんだい'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'hàng rau; người bán rau quả', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '八百屋' AND v.hiragana = 'やおや'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'rau; rau củ', '', 'ADJECTIVE'
FROM vocabulary v
WHERE v.word = '野菜' AND v.hiragana = 'やさい'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'dễ tánh; dễ tính; dễ; dễ dàng', '', 'ADJECTIVE'
FROM vocabulary v
WHERE v.word = '易しい' AND v.hiragana = 'やさしい'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'điềm tĩnh; yên tâm; rẻ; rẻ tiền', '', 'ADJECTIVE'
FROM vocabulary v
WHERE v.word = '安い' AND v.hiragana = 'やすい'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'nghỉ; vắng mặt', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '休み' AND v.hiragana = 'やすみ'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'nghỉ ngơi; nghỉ; vắng mặt; ngủ', '', 'VERB'
FROM vocabulary v
WHERE v.word = '休む' AND v.hiragana = 'やすむ'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'tám; thứ tám', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '八つ' AND v.hiragana = 'やっつ'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'núi; sơn; ngọn núi', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '山' AND v.hiragana = 'やま'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'tưới', '', 'NOUN'
FROM vocabulary v
WHERE v.word = 'やる' AND v.hiragana = 'やる'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'Chiều tà', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '夕方' AND v.hiragana = 'ゆうがた'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'bữa ăn tối', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '夕飯' AND v.hiragana = 'ゆうはん'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'bưu điện', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '郵便局' AND v.hiragana = 'ゆうびんきょく'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'đêm hôm qua; đêm qua; hồi khuya', '', 'ADVERB'
FROM vocabulary v
WHERE v.word = '昨夜' AND v.hiragana = 'ゆうべ'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'hữu danh; sự nổi tiếng; nổi tiếng; có danh', '', 'ADJECTIVE'
FROM vocabulary v
WHERE v.word = '有名' AND v.hiragana = 'ゆうめい'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'tuyết', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '雪' AND v.hiragana = 'ゆき'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'thong thả; từ từ; chậm rãi', '', 'ADVERB'
FROM vocabulary v
WHERE v.word = 'ゆっくりと' AND v.hiragana = 'ゆっくりと'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'ngày tám; mồng tám; tám ngày', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '八日' AND v.hiragana = 'ようか'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'âu phục; quần áo; quần áo tây', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '洋服' AND v.hiragana = 'ようふく'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'sự mong muốn; sự tham lam; 欲が深く:tham lam', '', 'NOUN'
FROM vocabulary v
WHERE v.word = 'よく' AND v.hiragana = 'よく'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'bề ngang; bên cạnh; chiều ngang', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '横' AND v.hiragana = 'よこ'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'bốn ngày; ngày mùng bốn', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '四日' AND v.hiragana = 'よっか'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'bốn', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '四つ' AND v.hiragana = 'よっつ'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'gào; gọi; mời; kêu tên; hô hào', '', 'VERB'
FROM vocabulary v
WHERE v.word = '呼ぶ' AND v.hiragana = 'よぶ'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'đọc', '', 'VERB'
FROM vocabulary v
WHERE v.word = '読む' AND v.hiragana = 'よむ'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'hèn yếu; kém cỏi; không chắc; không bền', '', 'ADJECTIVE'
FROM vocabulary v
WHERE v.word = '弱い' AND v.hiragana = 'よわい'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'tháng sau', '', 'ADVERB'
FROM vocabulary v
WHERE v.word = '来月' AND v.hiragana = 'らいげつ'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'tuần lễ sau; tuần sau', '', 'ADVERB'
FROM vocabulary v
WHERE v.word = '来週' AND v.hiragana = 'らいしゅう'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'năm sau', '', 'ADVERB'
FROM vocabulary v
WHERE v.word = '来年' AND v.hiragana = 'らいねん'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'cái đài; cái radio; máy thu thanh; máy vô tuyến truyền thanh', '', 'NOUN'
FROM vocabulary v
WHERE v.word = 'ラジオ' AND v.hiragana = 'ラジオ'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'đài radio cassette', '', 'NOUN'
FROM vocabulary v
WHERE v.word = 'ラジカセ' AND v.hiragana = 'ラジカセ'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'radio-cassette, tape recorder', '', 'NOUN'
FROM vocabulary v
WHERE v.word = 'ラジオカセット' AND v.hiragana = 'ラジオカセット'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'nhánh; sự tuyệt vời; sự tuyệt hảo; tuyệt vời; tuyệt hảo', '', 'ADJECTIVE'
FROM vocabulary v
WHERE v.word = 'りっぱ' AND v.hiragana = 'りっぱ'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'du học sinh; lưu học sinh; học sinh du học', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '留学生' AND v.hiragana = 'りゅうがくせい'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'Cha mẹ; bố mẹ', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '両親' AND v.hiragana = 'りょうしん'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'bữa ăn; sự nấu ăn; món ăn; bữa ăn', '', 'VERB'
FROM vocabulary v
WHERE v.word = '料理' AND v.hiragana = 'りょうり'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'lữ hành; sự đi lại; sự du lịch; du lịch', '', 'VERB'
FROM vocabulary v
WHERE v.word = '旅行' AND v.hiragana = 'りょこう'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'số không; số 0', '', 'ADJECTIVE'
FROM vocabulary v
WHERE v.word = '零' AND v.hiragana = 'れい'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'kho ướp lạnh; tủ lạnh', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '冷蔵庫' AND v.hiragana = 'れいぞうこ'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'đĩa nhựa; kỷ lục; sự ghi âm; sự thu thanh', '', 'NOUN'
FROM vocabulary v
WHERE v.word = 'レコード' AND v.hiragana = 'レコード'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'cao lâu; hiệu ăn; nhà hàng', '', 'NOUN'
FROM vocabulary v
WHERE v.word = 'レストラン' AND v.hiragana = 'レストラン'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'gác; hành lang; thềm', '', 'NOUN'
FROM vocabulary v
WHERE v.word = '廊下' AND v.hiragana = 'ろうか'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'áo sơ mi dài tay', '', 'NOUN'
FROM vocabulary v
WHERE v.word = 'ワイシャツ' AND v.hiragana = 'ワイシャツ'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'bé; bé bỏng; choai choai', '', 'ADJECTIVE'
FROM vocabulary v
WHERE v.word = '若い' AND v.hiragana = 'わかい'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'hay tin; hiểu biết; hiểu; lý giải; biết', '', 'VERB'
FROM vocabulary v
WHERE v.word = '分かる' AND v.hiragana = 'わかる'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'bỏ lại; đãng; quên bẵng', '', 'VERB'
FROM vocabulary v
WHERE v.word = '忘れる' AND v.hiragana = 'わすれる'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'trao, đưa', '', 'VERB'
FROM vocabulary v
WHERE v.word = '渡す' AND v.hiragana = 'わたす'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'băng qua; đi qua; độ', '', 'VERB'
FROM vocabulary v
WHERE v.word = '渡る' AND v.hiragana = 'わたる'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;
INSERT INTO vocabulary_senses (vocab_id, sense_no, meaning_vi, meaning_en, part_of_speech)
SELECT v.id, 1, 'oh, ho, exclamation of surprise, admiration, etc; hoo (owl call), toot (sound of a flute)', '', 'NOUN'
FROM vocabulary v
WHERE v.word = 'ほう' AND v.hiragana = 'より'
LIMIT 1
ON CONFLICT (vocab_id, sense_no) DO NOTHING;

-- -----------------------------------------------------------------------------
-- MAP VOCABULARY TO CHARACTERS (N5)
-- -----------------------------------------------------------------------------
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', 'ひがし'
FROM vocabulary v, writing_characters c
WHERE v.word = '東' AND v.hiragana = 'ひがし' AND c.character = '東'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '私' AND v.hiragana = 'わたくし' AND c.character = '私'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', 'いち'
FROM vocabulary v, writing_characters c
WHERE v.word = '一日' AND v.hiragana = 'いちにち' AND c.character = '一'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', 'にち'
FROM vocabulary v, writing_characters c
WHERE v.word = '一日' AND v.hiragana = 'いちにち' AND c.character = '日'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', 'ひと'
FROM vocabulary v, writing_characters c
WHERE v.word = '一人' AND v.hiragana = 'ひとり' AND c.character = '一'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'KUNYOMI', 'ひと'
FROM vocabulary v, writing_characters c
WHERE v.word = '一人' AND v.hiragana = 'ひとり' AND c.character = '人'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '暇' AND v.hiragana = 'ひま' AND c.character = '暇'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', 'い'
FROM vocabulary v, writing_characters c
WHERE v.word = '入口' AND v.hiragana = 'いりぐち' AND c.character = '入'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '入口' AND v.hiragana = 'いりぐち' AND c.character = '口'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', 'い'
FROM vocabulary v, writing_characters c
WHERE v.word = '入る' AND v.hiragana = 'はいる' AND c.character = '入'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '甘い' AND v.hiragana = 'あまい' AND c.character = '甘'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', 'じょう'
FROM vocabulary v, writing_characters c
WHERE v.word = '上手' AND v.hiragana = 'じょうず' AND c.character = '上'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '上手' AND v.hiragana = 'じょうず' AND c.character = '手'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '塩' AND v.hiragana = 'しお' AND c.character = '塩'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '黄色' AND v.hiragana = 'きいろ' AND c.character = '黄'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '黄色' AND v.hiragana = 'きいろ' AND c.character = '色'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '弟' AND v.hiragana = 'おとうと' AND c.character = '弟'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '門' AND v.hiragana = 'もん' AND c.character = '門'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '眼鏡' AND v.hiragana = 'めがね' AND c.character = '眼'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '眼鏡' AND v.hiragana = 'めがね' AND c.character = '鏡'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', 'く'
FROM vocabulary v, writing_characters c
WHERE v.word = '来る' AND v.hiragana = 'くる' AND c.character = '来'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '戸' AND v.hiragana = 'と' AND c.character = '戸'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '次' AND v.hiragana = 'つぎ' AND c.character = '次'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '丈夫' AND v.hiragana = 'じょうぶ' AND c.character = '丈'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '丈夫' AND v.hiragana = 'じょうぶ' AND c.character = '夫'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', 'ひと'
FROM vocabulary v, writing_characters c
WHERE v.word = '人' AND v.hiragana = 'ひと' AND c.character = '人'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', 'まえ'
FROM vocabulary v, writing_characters c
WHERE v.word = '前' AND v.hiragana = 'まえ' AND c.character = '前'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '誰' AND v.hiragana = 'だれ' AND c.character = '誰'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '体' AND v.hiragana = 'からだ' AND c.character = '体'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '店' AND v.hiragana = 'みせ' AND c.character = '店'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '止まる' AND v.hiragana = 'とまる' AND c.character = '止'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', 'なの'
FROM vocabulary v, writing_characters c
WHERE v.word = '七日' AND v.hiragana = 'なのか' AND c.character = '七'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'KUNYOMI', 'か'
FROM vocabulary v, writing_characters c
WHERE v.word = '七日' AND v.hiragana = 'なのか' AND c.character = '日'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', 'な'
FROM vocabulary v, writing_characters c
WHERE v.word = '南' AND v.hiragana = 'みなみ' AND c.character = '南'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '悪い' AND v.hiragana = 'わるい' AND c.character = '悪'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', 'ふた'
FROM vocabulary v, writing_characters c
WHERE v.word = '二人' AND v.hiragana = 'ふたり' AND c.character = '二'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'KUNYOMI', 'り'
FROM vocabulary v, writing_characters c
WHERE v.word = '二人' AND v.hiragana = 'ふたり' AND c.character = '人'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '灰皿' AND v.hiragana = 'はいざら' AND c.character = '灰'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '灰皿' AND v.hiragana = 'はいざら' AND c.character = '皿'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '弾く' AND v.hiragana = 'ひく' AND c.character = '弾'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '始め' AND v.hiragana = 'はじめ' AND c.character = '始'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', 'まん'
FROM vocabulary v, writing_characters c
WHERE v.word = '万' AND v.hiragana = 'まん' AND c.character = '万'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', 'さん'
FROM vocabulary v, writing_characters c
WHERE v.word = '三' AND v.hiragana = 'さん' AND c.character = '三'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', 'ろく'
FROM vocabulary v, writing_characters c
WHERE v.word = '六' AND v.hiragana = 'ろく' AND c.character = '六'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', 'いち'
FROM vocabulary v, writing_characters c
WHERE v.word = '一昨日' AND v.hiragana = 'おととい' AND c.character = '一'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '一昨日' AND v.hiragana = 'おととい' AND c.character = '昨'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 3, FALSE, 'ONYOMI', 'にち'
FROM vocabulary v, writing_characters c
WHERE v.word = '一昨日' AND v.hiragana = 'おととい' AND c.character = '日'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', 'いち'
FROM vocabulary v, writing_characters c
WHERE v.word = '一昨年' AND v.hiragana = 'おととし' AND c.character = '一'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '一昨年' AND v.hiragana = 'おととし' AND c.character = '昨'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 3, FALSE, 'KUNYOMI', 'とし'
FROM vocabulary v, writing_characters c
WHERE v.word = '一昨年' AND v.hiragana = 'おととし' AND c.character = '年'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '伯母さん' AND v.hiragana = 'おばさん' AND c.character = '伯'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', 'ぼ'
FROM vocabulary v, writing_characters c
WHERE v.word = '伯母さん' AND v.hiragana = 'おばさん' AND c.character = '母'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '叔母さん' AND v.hiragana = 'おばさん' AND c.character = '叔'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', 'ぼ'
FROM vocabulary v, writing_characters c
WHERE v.word = '叔母さん' AND v.hiragana = 'おばさん' AND c.character = '母'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '作る' AND v.hiragana = 'つくる' AND c.character = '作'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', 'まる'
FROM vocabulary v, writing_characters c
WHERE v.word = '円い' AND v.hiragana = 'まるい' AND c.character = '円'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '会う' AND v.hiragana = 'あう' AND c.character = '会'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '青' AND v.hiragana = 'あお' AND c.character = '青'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '青い' AND v.hiragana = 'あおい' AND c.character = '青'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '赤' AND v.hiragana = 'あか' AND c.character = '赤'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '赤い' AND v.hiragana = 'あかい' AND c.character = '赤'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '明い' AND v.hiragana = 'あかるい' AND c.character = '明'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '秋' AND v.hiragana = 'あき' AND c.character = '秋'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '開ける' AND v.hiragana = 'あける' AND c.character = '開'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', 'あ'
FROM vocabulary v, writing_characters c
WHERE v.word = '上げる' AND v.hiragana = 'あげる' AND c.character = '上'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '朝' AND v.hiragana = 'あさ' AND c.character = '朝'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '朝御飯' AND v.hiragana = 'あさごはん' AND c.character = '朝'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '朝御飯' AND v.hiragana = 'あさごはん' AND c.character = '御'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 3, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '朝御飯' AND v.hiragana = 'あさごはん' AND c.character = '飯'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '足' AND v.hiragana = 'あし' AND c.character = '足'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '遊ぶ' AND v.hiragana = 'あそぶ' AND c.character = '遊'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '暖かい' AND v.hiragana = 'あたたかい' AND c.character = '暖'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '頭' AND v.hiragana = 'あたま' AND c.character = '頭'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '新しい' AND v.hiragana = 'あたらしい' AND c.character = '新'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '暑い' AND v.hiragana = 'あつい' AND c.character = '暑'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '熱い' AND v.hiragana = 'あつい' AND c.character = '熱'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '厚い' AND v.hiragana = 'あつい' AND c.character = '厚'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '兄' AND v.hiragana = 'あに' AND c.character = '兄'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '姉' AND v.hiragana = 'あね' AND c.character = '姉'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '危ない' AND v.hiragana = 'あぶない' AND c.character = '危'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', 'あめ'
FROM vocabulary v, writing_characters c
WHERE v.word = '雨' AND v.hiragana = 'あめ' AND c.character = '雨'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '飴' AND v.hiragana = 'あめ' AND c.character = '飴'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '洗う' AND v.hiragana = 'あらう' AND c.character = '洗'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '歩く' AND v.hiragana = 'あるく' AND c.character = '歩'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '家' AND v.hiragana = 'いえ' AND c.character = '家'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', 'い'
FROM vocabulary v, writing_characters c
WHERE v.word = '行く' AND v.hiragana = 'いく' AND c.character = '行'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '池' AND v.hiragana = 'いけ' AND c.character = '池'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '医者' AND v.hiragana = 'いしゃ' AND c.character = '医'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '医者' AND v.hiragana = 'いしゃ' AND c.character = '者'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '忙しい' AND v.hiragana = 'いそがしい' AND c.character = '忙'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '痛い' AND v.hiragana = 'いたい' AND c.character = '痛'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', 'いち'
FROM vocabulary v, writing_characters c
WHERE v.word = '一' AND v.hiragana = 'いち' AND c.character = '一'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', 'いつ'
FROM vocabulary v, writing_characters c
WHERE v.word = '五日' AND v.hiragana = 'いつか' AND c.character = '五'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'KUNYOMI', 'か'
FROM vocabulary v, writing_characters c
WHERE v.word = '五日' AND v.hiragana = 'いつか' AND c.character = '日'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', 'いち'
FROM vocabulary v, writing_characters c
WHERE v.word = '一緒' AND v.hiragana = 'いっしょ' AND c.character = '一'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '一緒' AND v.hiragana = 'いっしょ' AND c.character = '緒'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', 'いつ'
FROM vocabulary v, writing_characters c
WHERE v.word = '五つ' AND v.hiragana = 'いつつ' AND c.character = '五'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '犬' AND v.hiragana = 'いぬ' AND c.character = '犬'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', 'いま'
FROM vocabulary v, writing_characters c
WHERE v.word = '今' AND v.hiragana = 'いま' AND c.character = '今'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '意味' AND v.hiragana = 'いみ' AND c.character = '意'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '意味' AND v.hiragana = 'いみ' AND c.character = '味'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '妹' AND v.hiragana = 'いもうと' AND c.character = '妹'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '嫌' AND v.hiragana = 'いや' AND c.character = '嫌'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '要る' AND v.hiragana = 'いる' AND c.character = '要'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', 'い'
FROM vocabulary v, writing_characters c
WHERE v.word = '入れる' AND v.hiragana = 'いれる' AND c.character = '入'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '色' AND v.hiragana = 'いろ' AND c.character = '色'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', 'うし'
FROM vocabulary v, writing_characters c
WHERE v.word = '後ろ' AND v.hiragana = 'うしろ' AND c.character = '後'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '薄い' AND v.hiragana = 'うすい' AND c.character = '薄'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '歌' AND v.hiragana = 'うた' AND c.character = '歌'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '歌う' AND v.hiragana = 'うたう' AND c.character = '歌'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', 'う'
FROM vocabulary v, writing_characters c
WHERE v.word = '生まれる' AND v.hiragana = 'うまれる' AND c.character = '生'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '海' AND v.hiragana = 'うみ' AND c.character = '海'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '売る' AND v.hiragana = 'うる' AND c.character = '売'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '煩い' AND v.hiragana = 'うるさい' AND c.character = '煩'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', 'うわ'
FROM vocabulary v, writing_characters c
WHERE v.word = '上着' AND v.hiragana = 'うわぎ' AND c.character = '上'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '上着' AND v.hiragana = 'うわぎ' AND c.character = '着'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '絵' AND v.hiragana = 'え' AND c.character = '絵'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '映画' AND v.hiragana = 'えいが' AND c.character = '映'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '映画' AND v.hiragana = 'えいが' AND c.character = '画'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '映画館' AND v.hiragana = 'えいがかん' AND c.character = '映'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '映画館' AND v.hiragana = 'えいがかん' AND c.character = '画'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 3, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '映画館' AND v.hiragana = 'えいがかん' AND c.character = '館'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '英語' AND v.hiragana = 'えいご' AND c.character = '英'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', 'ご'
FROM vocabulary v, writing_characters c
WHERE v.word = '英語' AND v.hiragana = 'えいご' AND c.character = '語'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '駅' AND v.hiragana = 'えき' AND c.character = '駅'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '鉛筆' AND v.hiragana = 'えんぴつ' AND c.character = '鉛'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '鉛筆' AND v.hiragana = 'えんぴつ' AND c.character = '筆'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '多い' AND v.hiragana = 'おおい' AND c.character = '多'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', 'おお'
FROM vocabulary v, writing_characters c
WHERE v.word = '大きい' AND v.hiragana = 'おおきい' AND c.character = '大'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', 'おお'
FROM vocabulary v, writing_characters c
WHERE v.word = '大きな' AND v.hiragana = 'おおきな' AND c.character = '大'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', 'おお'
FROM vocabulary v, writing_characters c
WHERE v.word = '大勢' AND v.hiragana = 'おおぜい' AND c.character = '大'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '大勢' AND v.hiragana = 'おおぜい' AND c.character = '勢'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'KUNYOMI', 'ぼ'
FROM vocabulary v, writing_characters c
WHERE v.word = 'お母さん' AND v.hiragana = 'おかあさん' AND c.character = '母'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = 'お菓子' AND v.hiragana = 'おかし' AND c.character = '菓'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 3, FALSE, 'ONYOMI', 'し'
FROM vocabulary v, writing_characters c
WHERE v.word = 'お菓子' AND v.hiragana = 'おかし' AND c.character = '子'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'KUNYOMI', 'かね'
FROM vocabulary v, writing_characters c
WHERE v.word = 'お金' AND v.hiragana = 'おかね' AND c.character = '金'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '起きる' AND v.hiragana = 'おきる' AND c.character = '起'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '置く' AND v.hiragana = 'おく' AND c.character = '置'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '奥さん' AND v.hiragana = 'おくさん' AND c.character = '奥'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = 'お酒' AND v.hiragana = 'おさけ' AND c.character = '酒'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = 'お皿' AND v.hiragana = 'おさら' AND c.character = '皿'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '伯父' AND v.hiragana = 'おじいさん' AND c.character = '伯'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', 'ふ'
FROM vocabulary v, writing_characters c
WHERE v.word = '伯父' AND v.hiragana = 'おじいさん' AND c.character = '父'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '叔父' AND v.hiragana = 'おじいさん' AND c.character = '叔'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', 'ふ'
FROM vocabulary v, writing_characters c
WHERE v.word = '叔父' AND v.hiragana = 'おじいさん' AND c.character = '父'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '教える' AND v.hiragana = 'おしえる' AND c.character = '教'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '押す' AND v.hiragana = 'おす' AND c.character = '押'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '遅い' AND v.hiragana = 'おそい' AND c.character = '遅'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = 'お茶' AND v.hiragana = 'おちゃ' AND c.character = '茶'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = 'お手洗い' AND v.hiragana = 'おてあらい' AND c.character = '手'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 3, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = 'お手洗い' AND v.hiragana = 'おてあらい' AND c.character = '洗'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'KUNYOMI', 'ふ'
FROM vocabulary v, writing_characters c
WHERE v.word = 'お父さん' AND v.hiragana = 'おとうさん' AND c.character = '父'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', 'おとこ'
FROM vocabulary v, writing_characters c
WHERE v.word = '男' AND v.hiragana = 'おとこ' AND c.character = '男'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', 'おとこ'
FROM vocabulary v, writing_characters c
WHERE v.word = '男の子' AND v.hiragana = 'おとこのこ' AND c.character = '男'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 3, FALSE, 'KUNYOMI', 'こ'
FROM vocabulary v, writing_characters c
WHERE v.word = '男の子' AND v.hiragana = 'おとこのこ' AND c.character = '子'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', 'だい'
FROM vocabulary v, writing_characters c
WHERE v.word = '大人' AND v.hiragana = 'おとな' AND c.character = '大'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'KUNYOMI', 'と'
FROM vocabulary v, writing_characters c
WHERE v.word = '大人' AND v.hiragana = 'おとな' AND c.character = '人'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '同じ' AND v.hiragana = 'おなじ' AND c.character = '同'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = 'お兄さん' AND v.hiragana = 'おにいさん' AND c.character = '兄'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = 'お姉さん' AND v.hiragana = 'おねえさん' AND c.character = '姉'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = 'お風呂' AND v.hiragana = 'おふろ' AND c.character = '風'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 3, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = 'お風呂' AND v.hiragana = 'おふろ' AND c.character = '呂'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = 'お弁当' AND v.hiragana = 'おべんとう' AND c.character = '弁'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 3, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = 'お弁当' AND v.hiragana = 'おべんとう' AND c.character = '当'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '覚える' AND v.hiragana = 'おぼえる' AND c.character = '覚'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '重い' AND v.hiragana = 'おもい' AND c.character = '重'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '泳ぐ' AND v.hiragana = 'およぐ' AND c.character = '泳'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '降りる' AND v.hiragana = 'おりる' AND c.character = '降'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '終る' AND v.hiragana = 'おわる' AND c.character = '終'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '音楽' AND v.hiragana = 'おんがく' AND c.character = '音'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '音楽' AND v.hiragana = 'おんがく' AND c.character = '楽'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', 'おんな'
FROM vocabulary v, writing_characters c
WHERE v.word = '女' AND v.hiragana = 'おんな' AND c.character = '女'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', 'おんな'
FROM vocabulary v, writing_characters c
WHERE v.word = '女の子' AND v.hiragana = 'おんなのこ' AND c.character = '女'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 3, FALSE, 'KUNYOMI', 'こ'
FROM vocabulary v, writing_characters c
WHERE v.word = '女の子' AND v.hiragana = 'おんなのこ' AND c.character = '子'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', 'がい'
FROM vocabulary v, writing_characters c
WHERE v.word = '外国' AND v.hiragana = 'がいこく' AND c.character = '外'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', 'こく'
FROM vocabulary v, writing_characters c
WHERE v.word = '外国' AND v.hiragana = 'がいこく' AND c.character = '国'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', 'がい'
FROM vocabulary v, writing_characters c
WHERE v.word = '外国人' AND v.hiragana = 'がいこくじん' AND c.character = '外'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', 'こく'
FROM vocabulary v, writing_characters c
WHERE v.word = '外国人' AND v.hiragana = 'がいこくじん' AND c.character = '国'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 3, FALSE, 'ONYOMI', 'じん'
FROM vocabulary v, writing_characters c
WHERE v.word = '外国人' AND v.hiragana = 'がいこくじん' AND c.character = '人'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '会社' AND v.hiragana = 'かいしゃ' AND c.character = '会'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '会社' AND v.hiragana = 'かいしゃ' AND c.character = '社'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '階段' AND v.hiragana = 'かいだん' AND c.character = '階'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '階段' AND v.hiragana = 'かいだん' AND c.character = '段'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '買い物' AND v.hiragana = 'かいもの' AND c.character = '買'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 3, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '買い物' AND v.hiragana = 'かいもの' AND c.character = '物'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '買う' AND v.hiragana = 'かう' AND c.character = '買'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '返す' AND v.hiragana = 'かえす' AND c.character = '返'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '帰る' AND v.hiragana = 'かえる' AND c.character = '帰'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', 'か'
FROM vocabulary v, writing_characters c
WHERE v.word = '書く' AND v.hiragana = 'かく' AND c.character = '書'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', 'がく'
FROM vocabulary v, writing_characters c
WHERE v.word = '学生' AND v.hiragana = 'がくせい' AND c.character = '学'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', 'せい'
FROM vocabulary v, writing_characters c
WHERE v.word = '学生' AND v.hiragana = 'がくせい' AND c.character = '生'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '傘' AND v.hiragana = 'かさ' AND c.character = '傘'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '貸す' AND v.hiragana = 'かす' AND c.character = '貸'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '風' AND v.hiragana = 'かぜ' AND c.character = '風'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '風邪' AND v.hiragana = 'かぜ' AND c.character = '風'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '風邪' AND v.hiragana = 'かぜ' AND c.character = '邪'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '家族' AND v.hiragana = 'かぞく' AND c.character = '家'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '家族' AND v.hiragana = 'かぞく' AND c.character = '族'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', 'がく'
FROM vocabulary v, writing_characters c
WHERE v.word = '学校' AND v.hiragana = 'がっこう' AND c.character = '学'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', 'こう'
FROM vocabulary v, writing_characters c
WHERE v.word = '学校' AND v.hiragana = 'がっこう' AND c.character = '校'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '家庭' AND v.hiragana = 'かてい' AND c.character = '家'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '家庭' AND v.hiragana = 'かてい' AND c.character = '庭'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '花瓶' AND v.hiragana = 'かびん' AND c.character = '花'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '花瓶' AND v.hiragana = 'かびん' AND c.character = '瓶'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '紙' AND v.hiragana = 'かみ' AND c.character = '紙'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', 'か'
FROM vocabulary v, writing_characters c
WHERE v.word = '火曜日' AND v.hiragana = 'かようび' AND c.character = '火'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '火曜日' AND v.hiragana = 'かようび' AND c.character = '曜'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 3, FALSE, 'KUNYOMI', 'び'
FROM vocabulary v, writing_characters c
WHERE v.word = '火曜日' AND v.hiragana = 'かようび' AND c.character = '日'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '借りる' AND v.hiragana = 'かりる' AND c.character = '借'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '軽い' AND v.hiragana = 'かるい' AND c.character = '軽'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '漢字' AND v.hiragana = 'かんじ' AND c.character = '漢'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '漢字' AND v.hiragana = 'かんじ' AND c.character = '字'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', 'き'
FROM vocabulary v, writing_characters c
WHERE v.word = '木' AND v.hiragana = 'き' AND c.character = '木'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '黄色い' AND v.hiragana = 'きいろい' AND c.character = '黄'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '黄色い' AND v.hiragana = 'きいろい' AND c.character = '色'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '消える' AND v.hiragana = 'きえる' AND c.character = '消'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', 'き'
FROM vocabulary v, writing_characters c
WHERE v.word = '聞く' AND v.hiragana = 'きく' AND c.character = '聞'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', 'きた'
FROM vocabulary v, writing_characters c
WHERE v.word = '北' AND v.hiragana = 'きた' AND c.character = '北'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '汚い' AND v.hiragana = 'きたない' AND c.character = '汚'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '喫茶店' AND v.hiragana = 'きっさてん' AND c.character = '喫'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '喫茶店' AND v.hiragana = 'きっさてん' AND c.character = '茶'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 3, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '喫茶店' AND v.hiragana = 'きっさてん' AND c.character = '店'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '切手' AND v.hiragana = 'きって' AND c.character = '切'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '切手' AND v.hiragana = 'きって' AND c.character = '手'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '切符' AND v.hiragana = 'きっぷ' AND c.character = '切'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '切符' AND v.hiragana = 'きっぷ' AND c.character = '符'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '昨日' AND v.hiragana = 'きのう' AND c.character = '昨'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', 'にち'
FROM vocabulary v, writing_characters c
WHERE v.word = '昨日' AND v.hiragana = 'きのう' AND c.character = '日'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '牛肉' AND v.hiragana = 'ぎゅうにく' AND c.character = '牛'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '牛肉' AND v.hiragana = 'ぎゅうにく' AND c.character = '肉'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '牛乳' AND v.hiragana = 'ぎゅうにゅう' AND c.character = '牛'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '牛乳' AND v.hiragana = 'ぎゅうにゅう' AND c.character = '乳'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '教室' AND v.hiragana = 'きょうしつ' AND c.character = '教'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '教室' AND v.hiragana = 'きょうしつ' AND c.character = '室'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '兄弟' AND v.hiragana = 'きょうだい' AND c.character = '兄'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '兄弟' AND v.hiragana = 'きょうだい' AND c.character = '弟'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '去年' AND v.hiragana = 'きょねん' AND c.character = '去'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', 'ねん'
FROM vocabulary v, writing_characters c
WHERE v.word = '去年' AND v.hiragana = 'きょねん' AND c.character = '年'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '嫌い' AND v.hiragana = 'きらい' AND c.character = '嫌'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '切る' AND v.hiragana = 'きる' AND c.character = '切'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '着る' AND v.hiragana = 'きる' AND c.character = '着'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '銀行' AND v.hiragana = 'ぎんこう' AND c.character = '銀'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', 'こう'
FROM vocabulary v, writing_characters c
WHERE v.word = '銀行' AND v.hiragana = 'ぎんこう' AND c.character = '行'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', 'きん'
FROM vocabulary v, writing_characters c
WHERE v.word = '金曜日' AND v.hiragana = 'きんようび' AND c.character = '金'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '金曜日' AND v.hiragana = 'きんようび' AND c.character = '曜'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 3, FALSE, 'KUNYOMI', 'び'
FROM vocabulary v, writing_characters c
WHERE v.word = '金曜日' AND v.hiragana = 'きんようび' AND c.character = '日'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '薬' AND v.hiragana = 'くすり' AND c.character = '薬'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '果物' AND v.hiragana = 'くだもの' AND c.character = '果'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '果物' AND v.hiragana = 'くだもの' AND c.character = '物'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '口' AND v.hiragana = 'くち' AND c.character = '口'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '靴' AND v.hiragana = 'くつ' AND c.character = '靴'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '靴下' AND v.hiragana = 'くつした' AND c.character = '靴'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'KUNYOMI', 'した'
FROM vocabulary v, writing_characters c
WHERE v.word = '靴下' AND v.hiragana = 'くつした' AND c.character = '下'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', 'くに'
FROM vocabulary v, writing_characters c
WHERE v.word = '国' AND v.hiragana = 'くに' AND c.character = '国'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '曇り' AND v.hiragana = 'くもり' AND c.character = '曇'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '曇る' AND v.hiragana = 'くもる' AND c.character = '曇'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '暗い' AND v.hiragana = 'くらい' AND c.character = '暗'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', 'くるま'
FROM vocabulary v, writing_characters c
WHERE v.word = '車' AND v.hiragana = 'くるま' AND c.character = '車'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '黒' AND v.hiragana = 'くろ' AND c.character = '黒'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '黒い' AND v.hiragana = 'くろい' AND c.character = '黒'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '警官' AND v.hiragana = 'けいかん' AND c.character = '警'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '警官' AND v.hiragana = 'けいかん' AND c.character = '官'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', 'こん'
FROM vocabulary v, writing_characters c
WHERE v.word = '今朝' AND v.hiragana = 'けさ' AND c.character = '今'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '今朝' AND v.hiragana = 'けさ' AND c.character = '朝'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '消す' AND v.hiragana = 'けす' AND c.character = '消'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '結構' AND v.hiragana = 'けっこう' AND c.character = '結'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '結構' AND v.hiragana = 'けっこう' AND c.character = '構'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '結婚' AND v.hiragana = 'けっこん' AND c.character = '結'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '結婚' AND v.hiragana = 'けっこん' AND c.character = '婚'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', 'げつ'
FROM vocabulary v, writing_characters c
WHERE v.word = '月曜日' AND v.hiragana = 'げつようび' AND c.character = '月'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '月曜日' AND v.hiragana = 'げつようび' AND c.character = '曜'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 3, FALSE, 'KUNYOMI', 'び'
FROM vocabulary v, writing_characters c
WHERE v.word = '月曜日' AND v.hiragana = 'げつようび' AND c.character = '日'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '玄関' AND v.hiragana = 'げんかん' AND c.character = '玄'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '玄関' AND v.hiragana = 'げんかん' AND c.character = '関'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '元気' AND v.hiragana = 'げんき' AND c.character = '元'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', 'き'
FROM vocabulary v, writing_characters c
WHERE v.word = '元気' AND v.hiragana = 'げんき' AND c.character = '気'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', 'ご'
FROM vocabulary v, writing_characters c
WHERE v.word = '五' AND v.hiragana = 'ご' AND c.character = '五'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '公園' AND v.hiragana = 'こうえん' AND c.character = '公'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '公園' AND v.hiragana = 'こうえん' AND c.character = '園'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '交差点' AND v.hiragana = 'こうさてん' AND c.character = '交'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '交差点' AND v.hiragana = 'こうさてん' AND c.character = '差'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 3, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '交差点' AND v.hiragana = 'こうさてん' AND c.character = '点'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '紅茶' AND v.hiragana = 'こうちゃ' AND c.character = '紅'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '紅茶' AND v.hiragana = 'こうちゃ' AND c.character = '茶'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '交番' AND v.hiragana = 'こうばん' AND c.character = '交'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '交番' AND v.hiragana = 'こうばん' AND c.character = '番'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '声' AND v.hiragana = 'こえ' AND c.character = '声'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', 'ご'
FROM vocabulary v, writing_characters c
WHERE v.word = '午後' AND v.hiragana = 'ごご' AND c.character = '午'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', 'ご'
FROM vocabulary v, writing_characters c
WHERE v.word = '午後' AND v.hiragana = 'ごご' AND c.character = '後'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', 'ここの'
FROM vocabulary v, writing_characters c
WHERE v.word = '九日' AND v.hiragana = 'ここのか' AND c.character = '九'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'KUNYOMI', 'か'
FROM vocabulary v, writing_characters c
WHERE v.word = '九日' AND v.hiragana = 'ここのか' AND c.character = '日'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', 'ここの'
FROM vocabulary v, writing_characters c
WHERE v.word = '九つ' AND v.hiragana = 'ここのつ' AND c.character = '九'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', 'ご'
FROM vocabulary v, writing_characters c
WHERE v.word = '午前' AND v.hiragana = 'ごぜん' AND c.character = '午'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', 'ぜん'
FROM vocabulary v, writing_characters c
WHERE v.word = '午前' AND v.hiragana = 'ごぜん' AND c.character = '前'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '答える' AND v.hiragana = 'こたえる' AND c.character = '答'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', 'こん'
FROM vocabulary v, writing_characters c
WHERE v.word = '今年' AND v.hiragana = 'ことし' AND c.character = '今'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'KUNYOMI', 'とし'
FROM vocabulary v, writing_characters c
WHERE v.word = '今年' AND v.hiragana = 'ことし' AND c.character = '年'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '言葉' AND v.hiragana = 'ことば' AND c.character = '言'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '言葉' AND v.hiragana = 'ことば' AND c.character = '葉'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', 'こ'
FROM vocabulary v, writing_characters c
WHERE v.word = '子供' AND v.hiragana = 'こども' AND c.character = '子'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '子供' AND v.hiragana = 'こども' AND c.character = '供'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '御飯' AND v.hiragana = 'ごはん' AND c.character = '御'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '御飯' AND v.hiragana = 'ごはん' AND c.character = '飯'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '困る' AND v.hiragana = 'こまる' AND c.character = '困'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', 'こん'
FROM vocabulary v, writing_characters c
WHERE v.word = '今月' AND v.hiragana = 'こんげつ' AND c.character = '今'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', 'げつ'
FROM vocabulary v, writing_characters c
WHERE v.word = '今月' AND v.hiragana = 'こんげつ' AND c.character = '月'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', 'こん'
FROM vocabulary v, writing_characters c
WHERE v.word = '今週' AND v.hiragana = 'こんしゅう' AND c.character = '今'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '今週' AND v.hiragana = 'こんしゅう' AND c.character = '週'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', 'こん'
FROM vocabulary v, writing_characters c
WHERE v.word = '今晩' AND v.hiragana = 'こんばん' AND c.character = '今'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '今晩' AND v.hiragana = 'こんばん' AND c.character = '晩'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '財布' AND v.hiragana = 'さいふ' AND c.character = '財'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '財布' AND v.hiragana = 'さいふ' AND c.character = '布'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', 'さき'
FROM vocabulary v, writing_characters c
WHERE v.word = '先' AND v.hiragana = 'さき' AND c.character = '先'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '咲く' AND v.hiragana = 'さく' AND c.character = '咲'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '作文' AND v.hiragana = 'さくぶん' AND c.character = '作'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '作文' AND v.hiragana = 'さくぶん' AND c.character = '文'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '差す' AND v.hiragana = 'さす' AND c.character = '差'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '雑誌' AND v.hiragana = 'ざっし' AND c.character = '雑'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '雑誌' AND v.hiragana = 'ざっし' AND c.character = '誌'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '砂糖' AND v.hiragana = 'さとう' AND c.character = '砂'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '砂糖' AND v.hiragana = 'さとう' AND c.character = '糖'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '寒い' AND v.hiragana = 'さむい' AND c.character = '寒'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', 'らい'
FROM vocabulary v, writing_characters c
WHERE v.word = 'さ来年' AND v.hiragana = 'さらいねん' AND c.character = '来'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 3, FALSE, 'ONYOMI', 'ねん'
FROM vocabulary v, writing_characters c
WHERE v.word = 'さ来年' AND v.hiragana = 'さらいねん' AND c.character = '年'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', 'じ'
FROM vocabulary v, writing_characters c
WHERE v.word = '時間' AND v.hiragana = 'じかん' AND c.character = '時'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', 'かん'
FROM vocabulary v, writing_characters c
WHERE v.word = '時間' AND v.hiragana = 'じかん' AND c.character = '間'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '仕事' AND v.hiragana = 'しごと' AND c.character = '仕'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '仕事' AND v.hiragana = 'しごと' AND c.character = '事'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '辞書' AND v.hiragana = 'じしょ' AND c.character = '辞'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', 'しょ'
FROM vocabulary v, writing_characters c
WHERE v.word = '辞書' AND v.hiragana = 'じしょ' AND c.character = '書'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '静か' AND v.hiragana = 'しずか' AND c.character = '静'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '質問' AND v.hiragana = 'しつもん' AND c.character = '質'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '質問' AND v.hiragana = 'しつもん' AND c.character = '問'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '自転車' AND v.hiragana = 'じてんしゃ' AND c.character = '自'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '自転車' AND v.hiragana = 'じてんしゃ' AND c.character = '転'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 3, FALSE, 'ONYOMI', 'しゃ'
FROM vocabulary v, writing_characters c
WHERE v.word = '自転車' AND v.hiragana = 'じてんしゃ' AND c.character = '車'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '自動車' AND v.hiragana = 'じどうしゃ' AND c.character = '自'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '自動車' AND v.hiragana = 'じどうしゃ' AND c.character = '動'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 3, FALSE, 'ONYOMI', 'しゃ'
FROM vocabulary v, writing_characters c
WHERE v.word = '自動車' AND v.hiragana = 'じどうしゃ' AND c.character = '車'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '死ぬ' AND v.hiragana = 'しぬ' AND c.character = '死'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '字引' AND v.hiragana = 'じびき' AND c.character = '字'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '字引' AND v.hiragana = 'じびき' AND c.character = '引'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '自分' AND v.hiragana = 'じぶん' AND c.character = '自'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '自分' AND v.hiragana = 'じぶん' AND c.character = '分'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '閉まる' AND v.hiragana = 'しまる' AND c.character = '閉'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '閉める' AND v.hiragana = 'しめる' AND c.character = '閉'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '締める' AND v.hiragana = 'しめる' AND c.character = '締'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '写真' AND v.hiragana = 'しゃしん' AND c.character = '写'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '写真' AND v.hiragana = 'しゃしん' AND c.character = '真'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '授業' AND v.hiragana = 'じゅぎょう' AND c.character = '授'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '授業' AND v.hiragana = 'じゅぎょう' AND c.character = '業'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '宿題' AND v.hiragana = 'しゅくだい' AND c.character = '宿'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '宿題' AND v.hiragana = 'しゅくだい' AND c.character = '題'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', 'しょく'
FROM vocabulary v, writing_characters c
WHERE v.word = '食堂' AND v.hiragana = 'しょくどう' AND c.character = '食'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '食堂' AND v.hiragana = 'しょくどう' AND c.character = '堂'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '知る' AND v.hiragana = 'しる' AND c.character = '知'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', 'しろ'
FROM vocabulary v, writing_characters c
WHERE v.word = '白' AND v.hiragana = 'しろ' AND c.character = '白'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', 'しろ'
FROM vocabulary v, writing_characters c
WHERE v.word = '白い' AND v.hiragana = 'しろい' AND c.character = '白'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '新聞' AND v.hiragana = 'しんぶん' AND c.character = '新'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', 'ぶん'
FROM vocabulary v, writing_characters c
WHERE v.word = '新聞' AND v.hiragana = 'しんぶん' AND c.character = '聞'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', 'すい'
FROM vocabulary v, writing_characters c
WHERE v.word = '水曜日' AND v.hiragana = 'すいようび' AND c.character = '水'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '水曜日' AND v.hiragana = 'すいようび' AND c.character = '曜'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 3, FALSE, 'KUNYOMI', 'び'
FROM vocabulary v, writing_characters c
WHERE v.word = '水曜日' AND v.hiragana = 'すいようび' AND c.character = '日'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '吸う' AND v.hiragana = 'すう' AND c.character = '吸'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '好き' AND v.hiragana = 'すき' AND c.character = '好'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '少ない' AND v.hiragana = 'すくない' AND c.character = '少'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '少し' AND v.hiragana = 'すこし' AND c.character = '少'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '涼しい' AND v.hiragana = 'すずしい' AND c.character = '涼'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '住む' AND v.hiragana = 'すむ' AND c.character = '住'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '座る' AND v.hiragana = 'すわる' AND c.character = '座'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', 'せい'
FROM vocabulary v, writing_characters c
WHERE v.word = '生徒' AND v.hiragana = 'せいと' AND c.character = '生'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '生徒' AND v.hiragana = 'せいと' AND c.character = '徒'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '背広' AND v.hiragana = 'せびろ' AND c.character = '背'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '背広' AND v.hiragana = 'せびろ' AND c.character = '広'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '狭い' AND v.hiragana = 'せまい' AND c.character = '狭'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', 'せん'
FROM vocabulary v, writing_characters c
WHERE v.word = '千' AND v.hiragana = 'せん' AND c.character = '千'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', 'せん'
FROM vocabulary v, writing_characters c
WHERE v.word = '先月' AND v.hiragana = 'せんげつ' AND c.character = '先'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', 'げつ'
FROM vocabulary v, writing_characters c
WHERE v.word = '先月' AND v.hiragana = 'せんげつ' AND c.character = '月'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', 'せん'
FROM vocabulary v, writing_characters c
WHERE v.word = '先週' AND v.hiragana = 'せんしゅう' AND c.character = '先'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '先週' AND v.hiragana = 'せんしゅう' AND c.character = '週'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', 'せん'
FROM vocabulary v, writing_characters c
WHERE v.word = '先生' AND v.hiragana = 'せんせい' AND c.character = '先'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', 'せい'
FROM vocabulary v, writing_characters c
WHERE v.word = '先生' AND v.hiragana = 'せんせい' AND c.character = '生'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '洗濯' AND v.hiragana = 'せんたく' AND c.character = '洗'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '洗濯' AND v.hiragana = 'せんたく' AND c.character = '濯'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '全部' AND v.hiragana = 'ぜんぶ' AND c.character = '全'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '全部' AND v.hiragana = 'ぜんぶ' AND c.character = '部'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', 'だい'
FROM vocabulary v, writing_characters c
WHERE v.word = '大学' AND v.hiragana = 'だいがく' AND c.character = '大'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', 'がく'
FROM vocabulary v, writing_characters c
WHERE v.word = '大学' AND v.hiragana = 'だいがく' AND c.character = '学'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', 'たい'
FROM vocabulary v, writing_characters c
WHERE v.word = '大使館' AND v.hiragana = 'たいしかん' AND c.character = '大'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '大使館' AND v.hiragana = 'たいしかん' AND c.character = '使'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 3, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '大使館' AND v.hiragana = 'たいしかん' AND c.character = '館'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', 'だい'
FROM vocabulary v, writing_characters c
WHERE v.word = '大丈夫' AND v.hiragana = 'だいじょうぶ' AND c.character = '大'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '大丈夫' AND v.hiragana = 'だいじょうぶ' AND c.character = '丈'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 3, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '大丈夫' AND v.hiragana = 'だいじょうぶ' AND c.character = '夫'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', 'だい'
FROM vocabulary v, writing_characters c
WHERE v.word = '大好き' AND v.hiragana = 'だいすき' AND c.character = '大'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '大好き' AND v.hiragana = 'だいすき' AND c.character = '好'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', 'たい'
FROM vocabulary v, writing_characters c
WHERE v.word = '大切' AND v.hiragana = 'たいせつ' AND c.character = '大'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '大切' AND v.hiragana = 'たいせつ' AND c.character = '切'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '台所' AND v.hiragana = 'だいどころ' AND c.character = '台'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '台所' AND v.hiragana = 'だいどころ' AND c.character = '所'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', 'たか'
FROM vocabulary v, writing_characters c
WHERE v.word = '高い' AND v.hiragana = 'たかい' AND c.character = '高'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', 'だ'
FROM vocabulary v, writing_characters c
WHERE v.word = '出す' AND v.hiragana = 'だす' AND c.character = '出'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '立つ' AND v.hiragana = 'たつ' AND c.character = '立'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '建物' AND v.hiragana = 'たてもの' AND c.character = '建'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '建物' AND v.hiragana = 'たてもの' AND c.character = '物'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '楽しい' AND v.hiragana = 'たのしい' AND c.character = '楽'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '頼む' AND v.hiragana = 'たのむ' AND c.character = '頼'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', 'た'
FROM vocabulary v, writing_characters c
WHERE v.word = '食べ物' AND v.hiragana = 'たべもの' AND c.character = '食'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 3, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '食べ物' AND v.hiragana = 'たべもの' AND c.character = '物'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', 'た'
FROM vocabulary v, writing_characters c
WHERE v.word = '食べる' AND v.hiragana = 'たべる' AND c.character = '食'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '卵' AND v.hiragana = 'たまご' AND c.character = '卵'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '誕生日' AND v.hiragana = 'たんじょうび' AND c.character = '誕'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'KUNYOMI', 'う'
FROM vocabulary v, writing_characters c
WHERE v.word = '誕生日' AND v.hiragana = 'たんじょうび' AND c.character = '生'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 3, FALSE, 'KUNYOMI', 'び'
FROM vocabulary v, writing_characters c
WHERE v.word = '誕生日' AND v.hiragana = 'たんじょうび' AND c.character = '日'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', 'ちい'
FROM vocabulary v, writing_characters c
WHERE v.word = '小さい' AND v.hiragana = 'ちいさい' AND c.character = '小'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', 'ちい'
FROM vocabulary v, writing_characters c
WHERE v.word = '小さな' AND v.hiragana = 'ちいさな' AND c.character = '小'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '近い' AND v.hiragana = 'ちかい' AND c.character = '近'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '違う' AND v.hiragana = 'ちがう' AND c.character = '違'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '近く' AND v.hiragana = 'ちかく' AND c.character = '近'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '地下鉄' AND v.hiragana = 'ちかてつ' AND c.character = '地'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', 'か'
FROM vocabulary v, writing_characters c
WHERE v.word = '地下鉄' AND v.hiragana = 'ちかてつ' AND c.character = '下'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 3, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '地下鉄' AND v.hiragana = 'ちかてつ' AND c.character = '鉄'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '地図' AND v.hiragana = 'ちず' AND c.character = '地'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '地図' AND v.hiragana = 'ちず' AND c.character = '図'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '茶色' AND v.hiragana = 'ちゃいろ' AND c.character = '茶'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '茶色' AND v.hiragana = 'ちゃいろ' AND c.character = '色'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '使う' AND v.hiragana = 'つかう' AND c.character = '使'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '疲れる' AND v.hiragana = 'つかれる' AND c.character = '疲'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '着く' AND v.hiragana = 'つく' AND c.character = '着'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '机' AND v.hiragana = 'つくえ' AND c.character = '机'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '勤める' AND v.hiragana = 'つとめる' AND c.character = '勤'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '冷たい' AND v.hiragana = 'つめたい' AND c.character = '冷'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '強い' AND v.hiragana = 'つよい' AND c.character = '強'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '手' AND v.hiragana = 'て' AND c.character = '手'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', 'で'
FROM vocabulary v, writing_characters c
WHERE v.word = '出かける' AND v.hiragana = 'でかける' AND c.character = '出'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '手紙' AND v.hiragana = 'てがみ' AND c.character = '手'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '手紙' AND v.hiragana = 'てがみ' AND c.character = '紙'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', 'で'
FROM vocabulary v, writing_characters c
WHERE v.word = '出口' AND v.hiragana = 'でぐち' AND c.character = '出'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '出口' AND v.hiragana = 'でぐち' AND c.character = '口'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', 'で'
FROM vocabulary v, writing_characters c
WHERE v.word = '出る' AND v.hiragana = 'でる' AND c.character = '出'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', 'てん'
FROM vocabulary v, writing_characters c
WHERE v.word = '天気' AND v.hiragana = 'てんき' AND c.character = '天'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', 'き'
FROM vocabulary v, writing_characters c
WHERE v.word = '天気' AND v.hiragana = 'てんき' AND c.character = '気'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', 'でん'
FROM vocabulary v, writing_characters c
WHERE v.word = '電気' AND v.hiragana = 'でんき' AND c.character = '電'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', 'き'
FROM vocabulary v, writing_characters c
WHERE v.word = '電気' AND v.hiragana = 'でんき' AND c.character = '気'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', 'でん'
FROM vocabulary v, writing_characters c
WHERE v.word = '電車' AND v.hiragana = 'でんしゃ' AND c.character = '電'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', 'しゃ'
FROM vocabulary v, writing_characters c
WHERE v.word = '電車' AND v.hiragana = 'でんしゃ' AND c.character = '車'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', 'でん'
FROM vocabulary v, writing_characters c
WHERE v.word = '電話' AND v.hiragana = 'でんわ' AND c.character = '電'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', 'わ'
FROM vocabulary v, writing_characters c
WHERE v.word = '電話' AND v.hiragana = 'でんわ' AND c.character = '話'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '動物' AND v.hiragana = 'どうぶつ' AND c.character = '動'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '動物' AND v.hiragana = 'どうぶつ' AND c.character = '物'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '遠い' AND v.hiragana = 'とおい' AND c.character = '遠'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', 'とお'
FROM vocabulary v, writing_characters c
WHERE v.word = '十日' AND v.hiragana = 'とおか' AND c.character = '十'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'KUNYOMI', 'か'
FROM vocabulary v, writing_characters c
WHERE v.word = '十日' AND v.hiragana = 'とおか' AND c.character = '日'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', 'とき'
FROM vocabulary v, writing_characters c
WHERE v.word = '時々' AND v.hiragana = 'ときどき' AND c.character = '時'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', 'じ'
FROM vocabulary v, writing_characters c
WHERE v.word = '時計' AND v.hiragana = 'とけい' AND c.character = '時'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '時計' AND v.hiragana = 'とけい' AND c.character = '計'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '所' AND v.hiragana = 'ところ' AND c.character = '所'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', 'とし'
FROM vocabulary v, writing_characters c
WHERE v.word = '年' AND v.hiragana = 'とし' AND c.character = '年'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '図書館' AND v.hiragana = 'としょかん' AND c.character = '図'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', 'しょ'
FROM vocabulary v, writing_characters c
WHERE v.word = '図書館' AND v.hiragana = 'としょかん' AND c.character = '書'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 3, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '図書館' AND v.hiragana = 'としょかん' AND c.character = '館'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '隣' AND v.hiragana = 'となり' AND c.character = '隣'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '飛ぶ' AND v.hiragana = 'とぶ' AND c.character = '飛'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', 'とも'
FROM vocabulary v, writing_characters c
WHERE v.word = '友達' AND v.hiragana = 'ともだち' AND c.character = '友'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '友達' AND v.hiragana = 'ともだち' AND c.character = '達'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', 'ど'
FROM vocabulary v, writing_characters c
WHERE v.word = '土曜日' AND v.hiragana = 'どようび' AND c.character = '土'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '土曜日' AND v.hiragana = 'どようび' AND c.character = '曜'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 3, FALSE, 'KUNYOMI', 'び'
FROM vocabulary v, writing_characters c
WHERE v.word = '土曜日' AND v.hiragana = 'どようび' AND c.character = '日'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '鳥' AND v.hiragana = 'とり' AND c.character = '鳥'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 3, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = 'とり肉' AND v.hiragana = 'とりにく' AND c.character = '肉'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '取る' AND v.hiragana = 'とる' AND c.character = '取'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '撮る' AND v.hiragana = 'とる' AND c.character = '撮'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', 'なが'
FROM vocabulary v, writing_characters c
WHERE v.word = '長い' AND v.hiragana = 'ながい' AND c.character = '長'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '鳴く' AND v.hiragana = 'なく' AND c.character = '鳴'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '無くす' AND v.hiragana = 'なくす' AND c.character = '無'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '夏' AND v.hiragana = 'なつ' AND c.character = '夏'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '夏休み' AND v.hiragana = 'なつやすみ' AND c.character = '夏'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'KUNYOMI', 'やす'
FROM vocabulary v, writing_characters c
WHERE v.word = '夏休み' AND v.hiragana = 'なつやすみ' AND c.character = '休'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', 'なな'
FROM vocabulary v, writing_characters c
WHERE v.word = '七つ' AND v.hiragana = 'ななつ' AND c.character = '七'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', 'な'
FROM vocabulary v, writing_characters c
WHERE v.word = '名前' AND v.hiragana = 'なまえ' AND c.character = '名'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'KUNYOMI', 'まえ'
FROM vocabulary v, writing_characters c
WHERE v.word = '名前' AND v.hiragana = 'なまえ' AND c.character = '前'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '習う' AND v.hiragana = 'ならう' AND c.character = '習'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '並ぶ' AND v.hiragana = 'ならぶ' AND c.character = '並'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '並べる' AND v.hiragana = 'ならべる' AND c.character = '並'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', 'に'
FROM vocabulary v, writing_characters c
WHERE v.word = '二' AND v.hiragana = 'に' AND c.character = '二'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '賑やか' AND v.hiragana = 'にぎやか' AND c.character = '賑'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '肉' AND v.hiragana = 'にく' AND c.character = '肉'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', 'にし'
FROM vocabulary v, writing_characters c
WHERE v.word = '西' AND v.hiragana = 'にし' AND c.character = '西'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', 'にち'
FROM vocabulary v, writing_characters c
WHERE v.word = '日曜日' AND v.hiragana = 'にちようび' AND c.character = '日'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '日曜日' AND v.hiragana = 'にちようび' AND c.character = '曜'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 3, FALSE, 'ONYOMI', 'にち'
FROM vocabulary v, writing_characters c
WHERE v.word = '日曜日' AND v.hiragana = 'にちようび' AND c.character = '日'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '荷物' AND v.hiragana = 'にもつ' AND c.character = '荷'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '荷物' AND v.hiragana = 'にもつ' AND c.character = '物'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '庭' AND v.hiragana = 'にわ' AND c.character = '庭'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '脱ぐ' AND v.hiragana = 'ぬぐ' AND c.character = '脱'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '温い' AND v.hiragana = 'ぬるい' AND c.character = '温'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '猫' AND v.hiragana = 'ねこ' AND c.character = '猫'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '寝る' AND v.hiragana = 'ねる' AND c.character = '寝'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '登る' AND v.hiragana = 'のぼる' AND c.character = '登'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '飲み物' AND v.hiragana = 'のみもの' AND c.character = '飲'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 3, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '飲み物' AND v.hiragana = 'のみもの' AND c.character = '物'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '飲む' AND v.hiragana = 'のむ' AND c.character = '飲'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '乗る' AND v.hiragana = 'のる' AND c.character = '乗'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '歯' AND v.hiragana = 'は' AND c.character = '歯'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '葉書' AND v.hiragana = 'はがき' AND c.character = '葉'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'KUNYOMI', 'が'
FROM vocabulary v, writing_characters c
WHERE v.word = '葉書' AND v.hiragana = 'はがき' AND c.character = '書'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '箱' AND v.hiragana = 'はこ' AND c.character = '箱'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '橋' AND v.hiragana = 'はし' AND c.character = '橋'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '始まる' AND v.hiragana = 'はじまる' AND c.character = '始'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '初め' AND v.hiragana = 'はじめ' AND c.character = '初'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '初めて' AND v.hiragana = 'はじめて' AND c.character = '初'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '走る' AND v.hiragana = 'はしる' AND c.character = '走'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', 'に'
FROM vocabulary v, writing_characters c
WHERE v.word = '二十歳' AND v.hiragana = 'はたち' AND c.character = '二'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', 'じゅう'
FROM vocabulary v, writing_characters c
WHERE v.word = '二十歳' AND v.hiragana = 'はたち' AND c.character = '十'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 3, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '二十歳' AND v.hiragana = 'はたち' AND c.character = '歳'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '働く' AND v.hiragana = 'はたらく' AND c.character = '働'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', 'はち'
FROM vocabulary v, writing_characters c
WHERE v.word = '八' AND v.hiragana = 'はち' AND c.character = '八'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', 'に'
FROM vocabulary v, writing_characters c
WHERE v.word = '二十日' AND v.hiragana = 'はつか' AND c.character = '二'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', 'じゅう'
FROM vocabulary v, writing_characters c
WHERE v.word = '二十日' AND v.hiragana = 'はつか' AND c.character = '十'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 3, FALSE, 'KUNYOMI', 'か'
FROM vocabulary v, writing_characters c
WHERE v.word = '二十日' AND v.hiragana = 'はつか' AND c.character = '日'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '花' AND v.hiragana = 'はな' AND c.character = '花'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '鼻' AND v.hiragana = 'はな' AND c.character = '鼻'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', 'はな'
FROM vocabulary v, writing_characters c
WHERE v.word = '話' AND v.hiragana = 'はなし' AND c.character = '話'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', 'はな'
FROM vocabulary v, writing_characters c
WHERE v.word = '話す' AND v.hiragana = 'はなす' AND c.character = '話'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '早い' AND v.hiragana = 'はやい' AND c.character = '早'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '速い' AND v.hiragana = 'はやい' AND c.character = '速'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '春' AND v.hiragana = 'はる' AND c.character = '春'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '貼る' AND v.hiragana = 'はる' AND c.character = '貼'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '晴れ' AND v.hiragana = 'はれ' AND c.character = '晴'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '晴れる' AND v.hiragana = 'はれる' AND c.character = '晴'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', 'はん'
FROM vocabulary v, writing_characters c
WHERE v.word = '半' AND v.hiragana = 'はん' AND c.character = '半'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '晩' AND v.hiragana = 'ばん' AND c.character = '晩'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '番号' AND v.hiragana = 'ばんごう' AND c.character = '番'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '番号' AND v.hiragana = 'ばんごう' AND c.character = '号'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '晩御飯' AND v.hiragana = 'ばんごはん' AND c.character = '晩'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '晩御飯' AND v.hiragana = 'ばんごはん' AND c.character = '御'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 3, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '晩御飯' AND v.hiragana = 'ばんごはん' AND c.character = '飯'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', 'はん'
FROM vocabulary v, writing_characters c
WHERE v.word = '半分' AND v.hiragana = 'はんぶん' AND c.character = '半'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '半分' AND v.hiragana = 'はんぶん' AND c.character = '分'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '引く' AND v.hiragana = 'ひく' AND c.character = '引'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '低い' AND v.hiragana = 'ひくい' AND c.character = '低'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '飛行機' AND v.hiragana = 'ひこうき' AND c.character = '飛'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', 'こう'
FROM vocabulary v, writing_characters c
WHERE v.word = '飛行機' AND v.hiragana = 'ひこうき' AND c.character = '行'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 3, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '飛行機' AND v.hiragana = 'ひこうき' AND c.character = '機'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', 'ひだり'
FROM vocabulary v, writing_characters c
WHERE v.word = '左' AND v.hiragana = 'ひだり' AND c.character = '左'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', 'ひと'
FROM vocabulary v, writing_characters c
WHERE v.word = '一つ' AND v.hiragana = 'ひとつ' AND c.character = '一'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', 'ひと'
FROM vocabulary v, writing_characters c
WHERE v.word = '一月' AND v.hiragana = 'ひとつき' AND c.character = '一'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'KUNYOMI', 'つき'
FROM vocabulary v, writing_characters c
WHERE v.word = '一月' AND v.hiragana = 'ひとつき' AND c.character = '月'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', 'ひゃく'
FROM vocabulary v, writing_characters c
WHERE v.word = '百' AND v.hiragana = 'ひゃく' AND c.character = '百'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '病院' AND v.hiragana = 'びょういん' AND c.character = '病'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '病院' AND v.hiragana = 'びょういん' AND c.character = '院'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '病気' AND v.hiragana = 'びょうき' AND c.character = '病'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', 'き'
FROM vocabulary v, writing_characters c
WHERE v.word = '病気' AND v.hiragana = 'びょうき' AND c.character = '気'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '昼' AND v.hiragana = 'ひる' AND c.character = '昼'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '昼御飯' AND v.hiragana = 'ひるごはん' AND c.character = '昼'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '昼御飯' AND v.hiragana = 'ひるごはん' AND c.character = '御'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 3, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '昼御飯' AND v.hiragana = 'ひるごはん' AND c.character = '飯'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '広い' AND v.hiragana = 'ひろい' AND c.character = '広'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '封筒' AND v.hiragana = 'ふうとう' AND c.character = '封'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '封筒' AND v.hiragana = 'ふうとう' AND c.character = '筒'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '吹く' AND v.hiragana = 'ふく' AND c.character = '吹'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '服' AND v.hiragana = 'ふく' AND c.character = '服'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', 'ふた'
FROM vocabulary v, writing_characters c
WHERE v.word = '二つ' AND v.hiragana = 'ふたつ' AND c.character = '二'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '豚肉' AND v.hiragana = 'ぶたにく' AND c.character = '豚'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '豚肉' AND v.hiragana = 'ぶたにく' AND c.character = '肉'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', 'に'
FROM vocabulary v, writing_characters c
WHERE v.word = '二日' AND v.hiragana = 'ふつか' AND c.character = '二'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'KUNYOMI', 'か'
FROM vocabulary v, writing_characters c
WHERE v.word = '二日' AND v.hiragana = 'ふつか' AND c.character = '日'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '太い' AND v.hiragana = 'ふとい' AND c.character = '太'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '冬' AND v.hiragana = 'ふゆ' AND c.character = '冬'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '降る' AND v.hiragana = 'ふる' AND c.character = '降'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '古い' AND v.hiragana = 'ふるい' AND c.character = '古'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '文章' AND v.hiragana = 'ぶんしょう' AND c.character = '文'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '文章' AND v.hiragana = 'ぶんしょう' AND c.character = '章'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', 'か'
FROM vocabulary v, writing_characters c
WHERE v.word = '下手' AND v.hiragana = 'へた' AND c.character = '下'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '下手' AND v.hiragana = 'へた' AND c.character = '手'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '部屋' AND v.hiragana = 'へや' AND c.character = '部'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '部屋' AND v.hiragana = 'へや' AND c.character = '屋'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '辺' AND v.hiragana = 'へん' AND c.character = '辺'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '便利' AND v.hiragana = 'べんり' AND c.character = '便'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '便利' AND v.hiragana = 'べんり' AND c.character = '利'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '帽子' AND v.hiragana = 'ぼうし' AND c.character = '帽'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', 'し'
FROM vocabulary v, writing_characters c
WHERE v.word = '帽子' AND v.hiragana = 'ぼうし' AND c.character = '子'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '欲しい' AND v.hiragana = 'ほしい' AND c.character = '欲'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '細い' AND v.hiragana = 'ほそい' AND c.character = '細'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', 'ほん'
FROM vocabulary v, writing_characters c
WHERE v.word = '本' AND v.hiragana = 'ほん' AND c.character = '本'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', 'ほん'
FROM vocabulary v, writing_characters c
WHERE v.word = '本棚' AND v.hiragana = 'ほんだな' AND c.character = '本'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '本棚' AND v.hiragana = 'ほんだな' AND c.character = '棚'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', 'まい'
FROM vocabulary v, writing_characters c
WHERE v.word = '毎朝' AND v.hiragana = 'まいあさ' AND c.character = '毎'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '毎朝' AND v.hiragana = 'まいあさ' AND c.character = '朝'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', 'まい'
FROM vocabulary v, writing_characters c
WHERE v.word = '毎月' AND v.hiragana = 'まいげつ' AND c.character = '毎'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', 'げつ'
FROM vocabulary v, writing_characters c
WHERE v.word = '毎月' AND v.hiragana = 'まいげつ' AND c.character = '月'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', 'まい'
FROM vocabulary v, writing_characters c
WHERE v.word = '毎週' AND v.hiragana = 'まいしゅう' AND c.character = '毎'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '毎週' AND v.hiragana = 'まいしゅう' AND c.character = '週'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', 'まい'
FROM vocabulary v, writing_characters c
WHERE v.word = '毎日' AND v.hiragana = 'まいにち' AND c.character = '毎'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', 'にち'
FROM vocabulary v, writing_characters c
WHERE v.word = '毎日' AND v.hiragana = 'まいにち' AND c.character = '日'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', 'まい'
FROM vocabulary v, writing_characters c
WHERE v.word = '毎年' AND v.hiragana = 'まいねん' AND c.character = '毎'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', 'ねん'
FROM vocabulary v, writing_characters c
WHERE v.word = '毎年' AND v.hiragana = 'まいねん' AND c.character = '年'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', 'まい'
FROM vocabulary v, writing_characters c
WHERE v.word = '毎晩' AND v.hiragana = 'まいばん' AND c.character = '毎'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '毎晩' AND v.hiragana = 'まいばん' AND c.character = '晩'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '曲る' AND v.hiragana = 'まがる' AND c.character = '曲'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '町' AND v.hiragana = 'まち' AND c.character = '町'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '待つ' AND v.hiragana = 'まつ' AND c.character = '待'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '窓' AND v.hiragana = 'まど' AND c.character = '窓'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', 'まん'
FROM vocabulary v, writing_characters c
WHERE v.word = '万年筆' AND v.hiragana = 'まんねんひつ' AND c.character = '万'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', 'ねん'
FROM vocabulary v, writing_characters c
WHERE v.word = '万年筆' AND v.hiragana = 'まんねんひつ' AND c.character = '年'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 3, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '万年筆' AND v.hiragana = 'まんねんひつ' AND c.character = '筆'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '磨く' AND v.hiragana = 'みがく' AND c.character = '磨'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', 'みぎ'
FROM vocabulary v, writing_characters c
WHERE v.word = '右' AND v.hiragana = 'みぎ' AND c.character = '右'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '短い' AND v.hiragana = 'みじかい' AND c.character = '短'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', 'みず'
FROM vocabulary v, writing_characters c
WHERE v.word = '水' AND v.hiragana = 'みず' AND c.character = '水'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', 'み'
FROM vocabulary v, writing_characters c
WHERE v.word = '見せる' AND v.hiragana = 'みせる' AND c.character = '見'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '道' AND v.hiragana = 'みち' AND c.character = '道'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', 'み'
FROM vocabulary v, writing_characters c
WHERE v.word = '三日' AND v.hiragana = 'みっか' AND c.character = '三'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'KUNYOMI', 'か'
FROM vocabulary v, writing_characters c
WHERE v.word = '三日' AND v.hiragana = 'みっか' AND c.character = '日'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', 'み'
FROM vocabulary v, writing_characters c
WHERE v.word = '三つ' AND v.hiragana = 'みっつ' AND c.character = '三'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '緑' AND v.hiragana = 'みどり' AND c.character = '緑'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '皆さん' AND v.hiragana = 'みなさん' AND c.character = '皆'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '耳' AND v.hiragana = 'みみ' AND c.character = '耳'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', 'み'
FROM vocabulary v, writing_characters c
WHERE v.word = '見る 観る' AND v.hiragana = 'みる' AND c.character = '見'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 4, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '見る 観る' AND v.hiragana = 'みる' AND c.character = '観'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', 'む'
FROM vocabulary v, writing_characters c
WHERE v.word = '六日' AND v.hiragana = 'むいか' AND c.character = '六'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'KUNYOMI', 'か'
FROM vocabulary v, writing_characters c
WHERE v.word = '六日' AND v.hiragana = 'むいか' AND c.character = '日'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '向こう' AND v.hiragana = 'むこう' AND c.character = '向'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '難しい' AND v.hiragana = 'むずかしい' AND c.character = '難'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', 'む'
FROM vocabulary v, writing_characters c
WHERE v.word = '六つ' AND v.hiragana = 'むっつ' AND c.character = '六'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '村' AND v.hiragana = 'むら' AND c.character = '村'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '目' AND v.hiragana = 'め' AND c.character = '目'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 3, FALSE, 'ONYOMI', 'いち'
FROM vocabulary v, writing_characters c
WHERE v.word = 'もう一度' AND v.hiragana = 'もういちど' AND c.character = '一'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 4, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = 'もう一度' AND v.hiragana = 'もういちど' AND c.character = '度'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', 'もく'
FROM vocabulary v, writing_characters c
WHERE v.word = '木曜日' AND v.hiragana = 'もくようび' AND c.character = '木'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '木曜日' AND v.hiragana = 'もくようび' AND c.character = '曜'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 3, FALSE, 'KUNYOMI', 'び'
FROM vocabulary v, writing_characters c
WHERE v.word = '木曜日' AND v.hiragana = 'もくようび' AND c.character = '日'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '持つ' AND v.hiragana = 'もつ' AND c.character = '持'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '物' AND v.hiragana = 'もの' AND c.character = '物'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '問題' AND v.hiragana = 'もんだい' AND c.character = '問'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '問題' AND v.hiragana = 'もんだい' AND c.character = '題'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', 'や'
FROM vocabulary v, writing_characters c
WHERE v.word = '八百屋' AND v.hiragana = 'やおや' AND c.character = '八'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', 'ひゃく'
FROM vocabulary v, writing_characters c
WHERE v.word = '八百屋' AND v.hiragana = 'やおや' AND c.character = '百'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 3, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '八百屋' AND v.hiragana = 'やおや' AND c.character = '屋'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '野菜' AND v.hiragana = 'やさい' AND c.character = '野'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '野菜' AND v.hiragana = 'やさい' AND c.character = '菜'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '易しい' AND v.hiragana = 'やさしい' AND c.character = '易'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '安い' AND v.hiragana = 'やすい' AND c.character = '安'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', 'やす'
FROM vocabulary v, writing_characters c
WHERE v.word = '休み' AND v.hiragana = 'やすみ' AND c.character = '休'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', 'やす'
FROM vocabulary v, writing_characters c
WHERE v.word = '休む' AND v.hiragana = 'やすむ' AND c.character = '休'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', 'や'
FROM vocabulary v, writing_characters c
WHERE v.word = '八つ' AND v.hiragana = 'やっつ' AND c.character = '八'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', 'やま'
FROM vocabulary v, writing_characters c
WHERE v.word = '山' AND v.hiragana = 'やま' AND c.character = '山'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '夕方' AND v.hiragana = 'ゆうがた' AND c.character = '夕'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '夕方' AND v.hiragana = 'ゆうがた' AND c.character = '方'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '夕飯' AND v.hiragana = 'ゆうはん' AND c.character = '夕'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '夕飯' AND v.hiragana = 'ゆうはん' AND c.character = '飯'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '郵便局' AND v.hiragana = 'ゆうびんきょく' AND c.character = '郵'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '郵便局' AND v.hiragana = 'ゆうびんきょく' AND c.character = '便'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 3, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '郵便局' AND v.hiragana = 'ゆうびんきょく' AND c.character = '局'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '昨夜' AND v.hiragana = 'ゆうべ' AND c.character = '昨'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '昨夜' AND v.hiragana = 'ゆうべ' AND c.character = '夜'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '有名' AND v.hiragana = 'ゆうめい' AND c.character = '有'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', 'めい'
FROM vocabulary v, writing_characters c
WHERE v.word = '有名' AND v.hiragana = 'ゆうめい' AND c.character = '名'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '雪' AND v.hiragana = 'ゆき' AND c.character = '雪'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', 'よう'
FROM vocabulary v, writing_characters c
WHERE v.word = '八日' AND v.hiragana = 'ようか' AND c.character = '八'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'KUNYOMI', 'か'
FROM vocabulary v, writing_characters c
WHERE v.word = '八日' AND v.hiragana = 'ようか' AND c.character = '日'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '洋服' AND v.hiragana = 'ようふく' AND c.character = '洋'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '洋服' AND v.hiragana = 'ようふく' AND c.character = '服'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '横' AND v.hiragana = 'よこ' AND c.character = '横'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', 'よ'
FROM vocabulary v, writing_characters c
WHERE v.word = '四日' AND v.hiragana = 'よっか' AND c.character = '四'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'KUNYOMI', 'か'
FROM vocabulary v, writing_characters c
WHERE v.word = '四日' AND v.hiragana = 'よっか' AND c.character = '日'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', 'よ'
FROM vocabulary v, writing_characters c
WHERE v.word = '四つ' AND v.hiragana = 'よっつ' AND c.character = '四'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '呼ぶ' AND v.hiragana = 'よぶ' AND c.character = '呼'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', 'よ'
FROM vocabulary v, writing_characters c
WHERE v.word = '読む' AND v.hiragana = 'よむ' AND c.character = '読'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '弱い' AND v.hiragana = 'よわい' AND c.character = '弱'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', 'らい'
FROM vocabulary v, writing_characters c
WHERE v.word = '来月' AND v.hiragana = 'らいげつ' AND c.character = '来'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', 'げつ'
FROM vocabulary v, writing_characters c
WHERE v.word = '来月' AND v.hiragana = 'らいげつ' AND c.character = '月'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', 'らい'
FROM vocabulary v, writing_characters c
WHERE v.word = '来週' AND v.hiragana = 'らいしゅう' AND c.character = '来'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '来週' AND v.hiragana = 'らいしゅう' AND c.character = '週'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', 'らい'
FROM vocabulary v, writing_characters c
WHERE v.word = '来年' AND v.hiragana = 'らいねん' AND c.character = '来'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', 'ねん'
FROM vocabulary v, writing_characters c
WHERE v.word = '来年' AND v.hiragana = 'らいねん' AND c.character = '年'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '留学生' AND v.hiragana = 'りゅうがくせい' AND c.character = '留'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', 'がく'
FROM vocabulary v, writing_characters c
WHERE v.word = '留学生' AND v.hiragana = 'りゅうがくせい' AND c.character = '学'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 3, FALSE, 'ONYOMI', 'せい'
FROM vocabulary v, writing_characters c
WHERE v.word = '留学生' AND v.hiragana = 'りゅうがくせい' AND c.character = '生'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '両親' AND v.hiragana = 'りょうしん' AND c.character = '両'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '両親' AND v.hiragana = 'りょうしん' AND c.character = '親'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '料理' AND v.hiragana = 'りょうり' AND c.character = '料'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '料理' AND v.hiragana = 'りょうり' AND c.character = '理'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '旅行' AND v.hiragana = 'りょこう' AND c.character = '旅'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', 'こう'
FROM vocabulary v, writing_characters c
WHERE v.word = '旅行' AND v.hiragana = 'りょこう' AND c.character = '行'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '零' AND v.hiragana = 'れい' AND c.character = '零'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '冷蔵庫' AND v.hiragana = 'れいぞうこ' AND c.character = '冷'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '冷蔵庫' AND v.hiragana = 'れいぞうこ' AND c.character = '蔵'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 3, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '冷蔵庫' AND v.hiragana = 'れいぞうこ' AND c.character = '庫'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'ONYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '廊下' AND v.hiragana = 'ろうか' AND c.character = '廊'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 2, FALSE, 'ONYOMI', 'か'
FROM vocabulary v, writing_characters c
WHERE v.word = '廊下' AND v.hiragana = 'ろうか' AND c.character = '下'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '若い' AND v.hiragana = 'わかい' AND c.character = '若'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '分かる' AND v.hiragana = 'わかる' AND c.character = '分'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '忘れる' AND v.hiragana = 'わすれる' AND c.character = '忘'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '渡す' AND v.hiragana = 'わたす' AND c.character = '渡'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;
INSERT INTO vocabulary_characters (vocab_id, character_id, position, is_ateji, reading_type, reading)
SELECT v.id, c.id, 1, FALSE, 'KUNYOMI', ''
FROM vocabulary v, writing_characters c
WHERE v.word = '渡る' AND v.hiragana = 'わたる' AND c.character = '渡'
LIMIT 1
ON CONFLICT (vocab_id, character_id, position) DO NOTHING;