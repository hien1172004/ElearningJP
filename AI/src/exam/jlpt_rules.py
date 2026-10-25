"""
jlpt_rules.py — Bộ quy chuẩn cấu trúc đề thi JLPT chính thức (N1 - N5)
Dùng để đối chiếu, kiểm tra (audit), phát hiện câu hỏi hoặc Mondai bị sót/thiếu
khi AI trích xuất đề thi từ file PDF / Audio.
"""

from dataclasses import dataclass, field
from typing import Any, Dict, List, Optional, Tuple
import logging
import re

logger = logging.getLogger("kanji_ai")


@dataclass
class MondaiSpec:
    mondai_number: int                 # 1, 2, 3...
    mondai_code: str                   # KANJI_READING, ORTHOGRAPHY, ...
    title_jp: str                      # 問題1 漢字読み, 問題2 表記...
    title_vi: str                      # Cách đọc chữ Hán, Cách viết chữ Hán...
    min_count: int                     # Số câu tối thiểu
    max_count: int                     # Số câu tối đa
    typical_count: int                 # Số câu chuẩn
    typical_range: Tuple[int, int]     # (start_q, end_q) chuẩn trong đề
    description: str                   # Mô tả đặc trưng nhận diện
    is_required: bool = True           # Mondai bắt buộc phải có


# ====================================================================
# MA TRẬN QUY CHUẨN ĐỀ THI JLPT CHÍNH THỨC (N5 -> N1)
# ====================================================================
JLPT_STRUCTURE_SPEC: Dict[str, Dict[str, List[MondaiSpec]]] = {
    # -------------------------------------------------------------
    # N5
    # -------------------------------------------------------------
    "N5": {
        "VOCABULARY": [
            MondaiSpec(1, "KANJI_READING", "問題1 漢字読み", "Cách đọc chữ Hán (Hán tự -> Hiragana)", 10, 14, 12, (1, 12),
                       "Đề bài có chữ Hán gạch chân 【漢字】, 4 lựa chọn là Hiragana."),
            MondaiSpec(2, "ORTHOGRAPHY", "問題2 表記", "Cách viết chữ Hán (Hiragana -> Hán tự)", 6, 9, 8, (13, 20),
                       "Đề bài có Hiragana gạch chân 【ひらがな】, 4 lựa chọn là chữ Hán/Katakana."),
            MondaiSpec(3, "CONTEXTUAL_USE", "問題3 文脈規定", "Điền từ vào câu", 8, 11, 10, (21, 30),
                       "Câu văn đơn có chỗ trống （　　）, 4 lựa chọn là từ vựng phù hợp ngữ cảnh."),
            MondaiSpec(4, "SYNONYMS", "問題4 言い換え類義", "Từ đồng nghĩa / Cách diễn đạt tương đương", 4, 6, 5, (31, 35),
                       "Câu văn có cụm từ gạch chân, chọn câu có nghĩa gần nhất."),
        ],
        "GRAMMAR_READING": [
            MondaiSpec(1, "SENTENTIAL_GRAMMAR", "問題1 文法形式の判断", "Ngữ pháp câu đơn", 14, 17, 16, (1, 16),
                       "Câu văn đơn có chỗ trống （　　）, 4 lựa chọn là trợ từ / ngữ pháp."),
            MondaiSpec(2, "SENTENCE_COMPOSITION", "問題2 文の組み立て", "Sắp xếp trật tự câu (Dấu sao ★)", 4, 6, 5, (17, 21),
                       "Câu có 4 vị trí gạch kèm 1 dấu sao ★: ＿＿ ＿＿ ＿★＿ ＿＿."),
            MondaiSpec(3, "TEXT_GRAMMAR", "問題3 文章の文法", "Ngữ pháp đoạn văn (Bài đục lỗ)", 4, 6, 5, (22, 26),
                       "Một bài văn có các ô số 【 22 】...【 26 】 cần điền từ."),
            MondaiSpec(4, "SHORT_PASSAGE", "問題4 内容理解（短文）", "Đọc hiểu đoạn ngắn", 3, 4, 3, (27, 29),
                       "Mỗi đoạn văn ngắn ~80 chữ đi kèm 1 câu hỏi độc lập."),
            MondaiSpec(5, "MEDIUM_PASSAGE", "問題5 内容理解（中文）", "Đọc hiểu đoạn trung", 2, 3, 2, (30, 31),
                       "Đoạn văn vừa ~250 chữ đi kèm 2 câu hỏi."),
            MondaiSpec(6, "INFORMATION_RETRIEVAL", "問題6 情報検索", "Tìm kiếm thông tin (Tờ rơi/Bảng biểu)", 1, 2, 1, (32, 32),
                       "Tờ thông báo, áp phích hoặc bảng quy định kèm 1 câu hỏi tra cứu."),
        ],
        "LISTENING": [
            MondaiSpec(1, "TASK_BASED", "問題1 課題理解", "Hiểu nhiệm vụ (Thường có tranh)", 6, 8, 7, (1, 7),
                       "Nghe câu hỏi trước, có tranh hoặc text lựa chọn hành động tiếp theo."),
            MondaiSpec(2, "KEY_POINT", "問題2 ポイント理解", "Hiểu điểm chính", 5, 7, 6, (8, 13),
                       "Nghe câu hỏi, có thời gian đọc đề, nghe hội thoại chọn điểm mấu chốt."),
            MondaiSpec(3, "UTTERANCE_EXPRESSIONS", "問題3 発話表現", "Diễn đạt phát ngôn (Nhìn tranh chọn câu)", 4, 6, 5, (14, 18),
                       "Nhìn tranh mũi tên chỉ vào người nói, chọn câu nói phù hợp (3 lựa chọn)."),
            MondaiSpec(4, "QUICK_RESPONSE", "問題4 即時応答", "Phản xạ nhanh", 5, 7, 6, (19, 24),
                       "Nghe câu ngắn, chọn câu đối đáp phù hợp ngay lập tức (3 lựa chọn)."),
        ],
    },

    # -------------------------------------------------------------
    # N4
    # -------------------------------------------------------------
    "N4": {
        "VOCABULARY": [
            MondaiSpec(1, "KANJI_READING", "問題1 漢字読み", "Cách đọc chữ Hán (Hán tự -> Hiragana)", 8, 10, 9, (1, 9),
                       "Đề bài có chữ Hán gạch chân 【漢字】, 4 lựa chọn là Hiragana."),
            MondaiSpec(2, "ORTHOGRAPHY", "問題2 表記", "Cách viết chữ Hán (Hiragana -> Hán tự)", 5, 7, 6, (10, 15),
                       "Đề bài có Hiragana gạch chân 【ひらがな】, 4 lựa chọn là chữ Hán/Katakana."),
            MondaiSpec(3, "CONTEXTUAL_USE", "問題3 文脈規定", "Điền từ vào câu", 8, 11, 10, (16, 25),
                       "Câu văn đơn có chỗ trống （　　）, 4 lựa chọn là từ vựng phù hợp."),
            MondaiSpec(4, "SYNONYMS", "問題4 言い換え類義", "Từ đồng nghĩa", 4, 6, 5, (26, 30),
                       "Câu văn có cụm từ gạch chân, chọn câu có ý nghĩa tương đương."),
            MondaiSpec(5, "PROPER_USAGE", "問題5 用法", "Cách dùng từ trong câu", 4, 6, 5, (31, 35),
                       "Cho 1 từ vựng (ví dụ 【案内】), chọn 1 trong 4 câu dùng từ này chính xác nhất."),
        ],
        "GRAMMAR_READING": [
            MondaiSpec(1, "SENTENTIAL_GRAMMAR", "問題1 文法形式の判断", "Ngữ pháp câu đơn", 13, 16, 15, (1, 15),
                       "Câu văn đơn có chỗ trống （　　）, lựa chọn ngữ pháp."),
            MondaiSpec(2, "SENTENCE_COMPOSITION", "問題2 文の組み立て", "Sắp xếp trật tự câu (Dấu sao ★)", 4, 6, 5, (16, 20),
                       "Câu có 4 vị trí gạch kèm 1 dấu sao ★: ＿＿ ＿＿ ＿★＿ ＿＿."),
            MondaiSpec(3, "TEXT_GRAMMAR", "問題3 文章の文法", "Ngữ pháp đoạn văn (Bài đục lỗ)", 4, 6, 5, (21, 25),
                       "Một bài văn có các ô số 【 21 】...【 25 】 cần điền từ/trợ từ nối."),
            MondaiSpec(4, "SHORT_PASSAGE", "問題4 内容理解（短文）", "Đọc hiểu đoạn ngắn", 3, 5, 4, (26, 29),
                       "Đoạn văn ngắn ~100-150 chữ đi kèm 1 câu hỏi độc lập."),
            MondaiSpec(5, "MEDIUM_PASSAGE", "問題5 内容理解（中文）", "Đọc hiểu đoạn trung", 3, 5, 4, (30, 33),
                       "1-2 bài văn ~450 chữ, mỗi bài kèm 2 câu hỏi."),
            MondaiSpec(6, "INFORMATION_RETRIEVAL", "問題6 情報検索", "Tìm kiếm thông tin (Tờ rơi/Bảng thông báo)", 2, 2, 2, (34, 35),
                       "Tờ rơi quảng cáo / thông báo / thư điện tử kèm 2 câu hỏi tra cứu."),
        ],
        "LISTENING": [
            MondaiSpec(1, "TASK_BASED", "問題1 課題理解", "Hiểu nhiệm vụ", 7, 9, 8, (1, 8),
                       "Nghe câu hỏi trước, có tranh/văn bản chọn hành động tiếp theo."),
            MondaiSpec(2, "KEY_POINT", "問題2 ポイント理解", "Hiểu điểm chính", 6, 8, 7, (9, 15),
                       "Nghe câu hỏi, đọc trước các phương án, nghe tìm điểm cốt lõi."),
            MondaiSpec(3, "UTTERANCE_EXPRESSIONS", "問題3 発話表現", "Diễn đạt phát ngôn (Nhìn tranh)", 4, 6, 5, (16, 20),
                       "Nhìn tranh mũi tên chỉ nhân vật, chọn câu nói đúng (3 phương án)."),
            MondaiSpec(4, "QUICK_RESPONSE", "問題4 即時応答", "Phản xạ nhanh", 7, 9, 8, (21, 28),
                       "Nghe câu thoại ngắn, chọn câu phản hồi tức thì (3 phương án)."),
        ],
    },

    # -------------------------------------------------------------
    # N3
    # -------------------------------------------------------------
    "N3": {
        "VOCABULARY": [
            MondaiSpec(1, "KANJI_READING", "問題1 漢字読み", "Cách đọc chữ Hán", 7, 9, 8, (1, 8),
                       "Đề bài có chữ Hán gạch chân 【漢字】, 4 lựa chọn là Hiragana."),
            MondaiSpec(2, "ORTHOGRAPHY", "問題2 表記", "Cách viết chữ Hán", 5, 7, 6, (9, 14),
                       "Đề bài có Hiragana gạch chân 【ひらがな】, 4 lựa chọn là chữ Hán."),
            MondaiSpec(3, "WORD_FORMATION", "問題3 語形成", "Cấu tạo từ (Tiền tố/Hậu tố)", 4, 6, 5, (15, 19),
                       "Điền tiếp đầu ngữ / tiếp vĩ ngữ tạo thành từ vựng hoàn chỉnh."),
            MondaiSpec(4, "CONTEXTUAL_USE", "問題4 文脈規定", "Điền từ vào câu", 6, 8, 7, (20, 26),
                       "Câu văn đơn có chỗ trống （　　）, 4 phương án từ vựng."),
            MondaiSpec(5, "SYNONYMS", "問題5 言い換え類義", "Từ đồng nghĩa", 4, 6, 5, (27, 31),
                       "Câu văn có từ gạch chân, chọn câu có ý nghĩa tương đương."),
            MondaiSpec(6, "PROPER_USAGE", "問題6 用法", "Cách dùng từ trong câu", 4, 6, 5, (32, 36),
                       "Cho 1 từ vựng, chọn câu văn sử dụng từ đó chính xác nhất."),
        ],
        "GRAMMAR_READING": [
            MondaiSpec(1, "SENTENTIAL_GRAMMAR", "問題1 文法形式の判断", "Ngữ pháp câu đơn", 11, 14, 13, (1, 13),
                       "Câu văn đơn có chỗ trống ngữ pháp."),
            MondaiSpec(2, "SENTENCE_COMPOSITION", "問題2 文の組み立て", "Sắp xếp trật tự câu (Dấu sao ★)", 4, 6, 5, (14, 18),
                       "Sắp xếp 4 từ/cụm từ vào 4 vị trí gạch sao ★."),
            MondaiSpec(3, "TEXT_GRAMMAR", "問題3 文章の文法", "Ngữ pháp đoạn văn (Bài đục lỗ)", 4, 6, 5, (19, 23),
                       "Đoạn văn có 5 chỗ trống ngữ pháp."),
            MondaiSpec(4, "SHORT_PASSAGE", "問題4 内容理解（短文）", "Đọc hiểu đoạn ngắn", 3, 5, 4, (24, 27),
                       "4 bài đọc ngắn ~350 chữ, mỗi bài 1 câu hỏi."),
            MondaiSpec(5, "MEDIUM_PASSAGE", "問題5 内容理解（中文）", "Đọc hiểu đoạn trung", 5, 7, 6, (28, 33),
                       "2 bài đọc vừa ~600 chữ, mỗi bài 3 câu hỏi."),
            MondaiSpec(6, "LONG_PASSAGE", "問題6 内容理解（長文）", "Đọc hiểu đoạn dài", 3, 5, 4, (34, 37),
                       "1 bài đọc dài ~900 chữ kèm 4 câu hỏi."),
            MondaiSpec(7, "INFORMATION_RETRIEVAL", "問題7 情報検索", "Tìm kiếm thông tin (Tờ rơi)", 2, 2, 2, (38, 39),
                       "Tờ thông báo, hướng dẫn kèm 2 câu hỏi."),
        ],
        "LISTENING": [
            MondaiSpec(1, "TASK_BASED", "問題1 課題理解", "Hiểu nhiệm vụ", 5, 7, 6, (1, 6), "Hành động tiếp theo."),
            MondaiSpec(2, "KEY_POINT", "問題2 ポイント理解", "Hiểu điểm chính", 5, 7, 6, (7, 12), "Nghe tìm điểm mấu chốt."),
            MondaiSpec(3, "GENERAL_OUTLINE", "問題3 概要理解", "Hiểu khái quát (Không in sẵn trên đề)", 2, 4, 3, (13, 15), "Chủ đề bao quát."),
            MondaiSpec(4, "UTTERANCE_EXPRESSIONS", "問題4 発話表現", "Diễn đạt phát ngôn (Nhìn tranh)", 3, 5, 4, (16, 19), "Nhìn tranh chọn câu nói."),
            MondaiSpec(5, "QUICK_RESPONSE", "問題5 即時応答", "Phản xạ nhanh", 8, 10, 9, (20, 28), "Câu phản hồi tức thì."),
        ],
    },

    # -------------------------------------------------------------
    # N2
    # -------------------------------------------------------------
    "N2": {
        "VOCABULARY": [
            MondaiSpec(1, "KANJI_READING", "問題1 漢字読み", "Cách đọc chữ Hán", 4, 6, 5, (1, 5), "Đọc chữ Hán N2."),
            MondaiSpec(2, "ORTHOGRAPHY", "問題2 表記", "Cách viết chữ Hán", 4, 6, 5, (6, 10), "Viết chữ Hán N2."),
            MondaiSpec(3, "WORD_FORMATION", "問題3 語形成", "Cấu tạo từ", 4, 6, 5, (11, 15), "Tiền tố/Hậu tố ghép."),
            MondaiSpec(4, "CONTEXTUAL_USE", "問題4 文脈規定", "Điền từ vào câu", 6, 8, 7, (16, 22), "Điền từ vựng."),
            MondaiSpec(5, "SYNONYMS", "問題5 言い換え類義", "Từ đồng nghĩa", 4, 6, 5, (23, 27), "Từ tương đương ý nghĩa."),
            MondaiSpec(6, "PROPER_USAGE", "問題6 用法", "Cách dùng từ", 4, 6, 5, (28, 32), "Cách dùng từ đúng."),
        ],
        "GRAMMAR_READING": [
            MondaiSpec(1, "SENTENTIAL_GRAMMAR", "問題1 文法形式の判断", "Ngữ pháp câu đơn", 10, 13, 12, (1, 12), "Ngữ pháp N2."),
            MondaiSpec(2, "SENTENCE_COMPOSITION", "問題2 文の組み立て", "Sắp xếp câu (Dấu sao ★)", 4, 6, 5, (13, 17), "Dấu sao ★."),
            MondaiSpec(3, "TEXT_GRAMMAR", "問題3 文章の文法", "Ngữ pháp đoạn văn (Đục lỗ)", 4, 6, 5, (18, 22), "Bài đục lỗ."),
            MondaiSpec(4, "SHORT_PASSAGE", "問題4 内容理解（短文）", "Đọc hiểu đoạn ngắn", 4, 6, 5, (23, 27), "5 bài đọc ngắn."),
            MondaiSpec(5, "MEDIUM_PASSAGE", "問題5 内容理解（中文）", "Đọc hiểu đoạn trung", 8, 10, 9, (28, 36), "3 bài đọc vừa."),
            MondaiSpec(6, "INTEGRATED_COMPREHENSION", "問題6 統合理解", "Đọc hiểu so sánh", 2, 3, 2, (37, 38), "So sánh 2 bài đọc."),
            MondaiSpec(7, "THEMATIC_COMPREHENSION", "問題7 主張理解（長文）", "Đọc hiểu chủ đề (Đoạn dài)", 3, 4, 3, (39, 41), "Bài luận dài."),
            MondaiSpec(8, "INFORMATION_RETRIEVAL", "問題8 情報検索", "Tìm kiếm thông tin", 2, 2, 2, (42, 43), "Tờ rơi / Thông báo."),
        ],
        "LISTENING": [
            MondaiSpec(1, "TASK_BASED", "問題1 課題理解", "Hiểu nhiệm vụ", 4, 6, 5, (1, 5), "Hành động tiếp theo."),
            MondaiSpec(2, "KEY_POINT", "問題2 ポイント理解", "Hiểu điểm chính", 5, 7, 6, (6, 11), "Điểm cốt lõi."),
            MondaiSpec(3, "GENERAL_OUTLINE", "問題3 概要理解", "Hiểu khái quát", 4, 6, 5, (12, 16), "Ý chính."),
            MondaiSpec(4, "QUICK_RESPONSE", "問題4 即時応答", "Phản xạ nhanh", 10, 13, 11, (17, 27), "Phản xạ 3 lựa chọn."),
            MondaiSpec(5, "INTEGRATED_LISTENING", "問題5 統合理解", "Nghe hiểu tích hợp", 3, 5, 4, (28, 31), "Hội thoại dài so sánh."),
        ],
    },

    # -------------------------------------------------------------
    # N1
    # -------------------------------------------------------------
    "N1": {
        "VOCABULARY": [
            MondaiSpec(1, "KANJI_READING", "問題1 漢字読み", "Cách đọc chữ Hán", 5, 7, 6, (1, 6), "Đọc Hán tự N1."),
            MondaiSpec(2, "CONTEXTUAL_USE", "問題2 文脈規定", "Điền từ vào câu", 6, 8, 7, (7, 13), "Điền từ khó."),
            MondaiSpec(3, "SYNONYMS", "問題3 言い換え類義", "Từ đồng nghĩa", 5, 7, 6, (14, 19), "Từ tương đương ý nghĩa."),
            MondaiSpec(4, "PROPER_USAGE", "問題4 用法", "Cách dùng từ", 5, 7, 6, (20, 25), "Cách dùng từ N1."),
        ],
        "GRAMMAR_READING": [
            MondaiSpec(1, "SENTENTIAL_GRAMMAR", "問題1 文法形式の判断", "Ngữ pháp câu đơn", 9, 12, 10, (1, 10), "Ngữ pháp N1."),
            MondaiSpec(2, "SENTENCE_COMPOSITION", "問題2 文の組み立て", "Sắp xếp câu (Dấu sao ★)", 4, 6, 5, (11, 15), "Dấu sao ★."),
            MondaiSpec(3, "TEXT_GRAMMAR", "問題3 文章の文法", "Ngữ pháp đoạn văn (Đục lỗ)", 4, 6, 5, (16, 20), "Bài đục lỗ N1."),
            MondaiSpec(4, "SHORT_PASSAGE", "問題4 内容理解（短文）", "Đọc hiểu đoạn ngắn", 3, 5, 4, (21, 24), "4 bài ngắn."),
            MondaiSpec(5, "MEDIUM_PASSAGE", "問題5 内容理解（中文）", "Đọc hiểu đoạn trung", 8, 10, 9, (25, 33), "3 bài vừa."),
            MondaiSpec(6, "INTEGRATED_COMPREHENSION", "問題6 統合理解", "Đọc hiểu so sánh", 2, 4, 3, (34, 36), "So sánh 2 bài đọc."),
            MondaiSpec(7, "THEMATIC_COMPREHENSION", "問題7 主張理解（長文）", "Đọc hiểu chủ đề (Đoạn dài)", 3, 5, 4, (37, 40), "Bài luận dài."),
            MondaiSpec(8, "INFORMATION_RETRIEVAL", "問題8 情報検索", "Tìm kiếm thông tin", 2, 2, 2, (41, 42), "Tờ rơi / Bảng biểu."),
        ],
        "LISTENING": [
            MondaiSpec(1, "TASK_BASED", "問題1 課題理解", "Hiểu nhiệm vụ", 5, 7, 6, (1, 6), "Nhiệm vụ."),
            MondaiSpec(2, "KEY_POINT", "問題2 ポイント理解", "Hiểu điểm chính", 6, 8, 7, (7, 13), "Điểm chính."),
            MondaiSpec(3, "GENERAL_OUTLINE", "問題3 概要理解", "Hiểu khái quát", 5, 7, 6, (14, 19), "Bao quát."),
            MondaiSpec(4, "QUICK_RESPONSE", "問題4 即時応答", "Phản xạ nhanh", 12, 15, 13, (20, 32), "Phản xạ nhanh."),
            MondaiSpec(5, "INTEGRATED_LISTENING", "問題5 統合理解", "Nghe hiểu tích hợp", 3, 5, 4, (33, 36), "Hội thoại dài so sánh."),
        ],
    },
}

# Hỗ trợ gộp KNOWLEDGE_READING (N1 / N2 kỳ thi gộp)
JLPT_STRUCTURE_SPEC["N1"]["KNOWLEDGE_READING"] = (
    JLPT_STRUCTURE_SPEC["N1"]["VOCABULARY"] + JLPT_STRUCTURE_SPEC["N1"]["GRAMMAR_READING"]
)
JLPT_STRUCTURE_SPEC["N2"]["KNOWLEDGE_READING"] = (
    JLPT_STRUCTURE_SPEC["N2"]["VOCABULARY"] + JLPT_STRUCTURE_SPEC["N2"]["GRAMMAR_READING"]
)


def get_expected_mondais(jlpt_level: str, section_type: str) -> List[MondaiSpec]:
    """Lấy danh sách các Mondai chuẩn theo level và section_type."""
    lvl = (jlpt_level or "N5").upper()
    sec = (section_type or "VOCABULARY").upper()
    level_dict = JLPT_STRUCTURE_SPEC.get(lvl, JLPT_STRUCTURE_SPEC["N5"])
    return level_dict.get(sec, [])


def audit_exam_section(
    questions: List[Dict[str, Any]],
    jlpt_level: str,
    section_type: str,
    total_pages: int = 0,
    parsed_pages: int = 0,
) -> Dict[str, Any]:
    """
    Rà soát và đối chiếu toàn diện danh sách câu hỏi AI vừa trích xuất
    với quy chuẩn chính thức của JLPT:
    - Kiểm tra xem có Mondai nào bị thiếu hoàn toàn không (ví dụ: mất Mondai 2 ORTHOGRAPHY).
    - Kiểm tra các khoảng câu hỏi bị đứt gãy (gap ranges).
    - Đối chiếu số lượng câu hỏi thực tế với quy chuẩn min-max.
    - Đưa ra cảnh báo chi tiết và đề xuất hành động khắc phục.
    """
    expected_mondais = get_expected_mondais(jlpt_level, section_type)
    if not expected_mondais:
        return {
            "status": "UNKNOWN",
            "is_complete": True,
            "total_questions": len(questions),
            "warnings": [],
            "mondai_audit": [],
            "missing_mondais": [],
            "missing_question_ranges": [],
        }

    # 1. Thống kê câu hỏi hiện có theo mondai_code và số thứ tự gốc
    actual_by_mondai: Dict[str, List[Dict[str, Any]]] = {}
    found_raw_numbers: List[int] = []

    for q in questions:
        m = q.get("mondai_code", "")
        actual_by_mondai.setdefault(m, []).append(q)

        raw_num = q.get("raw_question_number") or q.get("question_number")
        if raw_num is not None:
            try:
                found_raw_numbers.append(int(raw_num))
            except (ValueError, TypeError):
                pass

    found_raw_numbers = sorted(set(found_raw_numbers))

    # 2. Rà soát từng Mondai chuẩn
    audit_details = []
    missing_mondais = []
    warnings = []
    is_complete = True

    total_expected_typical = sum(m.typical_count for m in expected_mondais)
    total_actual = len(questions)

    for spec in expected_mondais:
        actual_qs = actual_by_mondai.get(spec.mondai_code, [])
        actual_count = len(actual_qs)

        status = "OK"
        note = ""

        if actual_count == 0:
            status = "MISSING"
            is_complete = False
            missing_mondais.append({
                "mondai_number": spec.mondai_number,
                "mondai_code": spec.mondai_code,
                "title_jp": spec.title_jp,
                "title_vi": spec.title_vi,
                "expected_range": f"Câu {spec.typical_range[0]} - {spec.typical_range[1]}",
                "typical_count": spec.typical_count,
            })
            warn_msg = (
                f"❌ THIẾU HOÀN TOÀN: {spec.title_jp} ({spec.title_vi})! "
                f"Dự kiến có {spec.typical_count} câu (khoảng câu {spec.typical_range[0]} - {spec.typical_range[1]})."
            )
            warnings.append(warn_msg)
            logger.warning(f"  [JLPT AUDIT] {warn_msg}")
        elif actual_count < spec.min_count:
            status = "INCOMPLETE"
            is_complete = False
            note = f"Thiếu câu (Có {actual_count}/{spec.typical_count} câu)"
            warn_msg = (
                f"⚠️ CHƯA ĐỦ: {spec.title_jp} chỉ trích xuất được {actual_count}/{spec.typical_count} câu "
                f"(Tối thiểu {spec.min_count} câu)."
            )
            warnings.append(warn_msg)
            logger.warning(f"  [JLPT AUDIT] {warn_msg}")
        elif actual_count > spec.max_count + 3:
            status = "EXCESSIVE"
            note = f"Có thể bị trùng lặp ({actual_count}/{spec.typical_count} câu)"
            warnings.append(f"ℹ️ {spec.title_jp} có số lượng câu ({actual_count}) nhiều hơn thông thường ({spec.typical_count}).")
        else:
            note = f"Đầy đủ ({actual_count}/{spec.typical_count} câu)"

        audit_details.append({
            "mondai_number": spec.mondai_number,
            "mondai_code": spec.mondai_code,
            "title_jp": spec.title_jp,
            "title_vi": spec.title_vi,
            "expected": f"{spec.min_count} - {spec.max_count}",
            "typical": spec.typical_count,
            "actual": actual_count,
            "status": status,
            "note": note,
            "typical_range": f"{spec.typical_range[0]} - {spec.typical_range[1]}",
        })

    # 3. Phát hiện khoảng số thứ tự câu hỏi bị đứt quãng (Gaps)
    missing_ranges = []
    if found_raw_numbers:
        min_n = min(found_raw_numbers)
        max_n = max(found_raw_numbers)
        full_set = set(range(min_n, max_n + 1))
        missing_set = sorted(full_set - set(found_raw_numbers))

        if missing_set:
            # Gom missing_set thành các khoảng [start, end]
            range_start = missing_set[0]
            prev = missing_set[0]
            for num in missing_set[1:]:
                if num == prev + 1:
                    prev = num
                else:
                    missing_ranges.append(f"{range_start}-{prev}" if range_start != prev else str(range_start))
                    range_start = num
                    prev = num
            missing_ranges.append(f"{range_start}-{prev}" if range_start != prev else str(range_start))

            gap_str = ", ".join(missing_ranges)
            warnings.append(f"⚠️ Phát hiện khoảng câu hỏi bị sót trên đề: câu {gap_str}.")
            logger.warning(f"  [JLPT AUDIT] Bị sót câu hỏi các số: {gap_str}")

    overall_status = "PERFECT" if (is_complete and not missing_ranges) else ("WARNING" if questions else "FAILED")

    audit_result = {
        "status": overall_status,
        "is_complete": is_complete and (len(missing_ranges) == 0),
        "jlpt_level": jlpt_level,
        "section_type": section_type,
        "total_extracted": total_actual,
        "expected_typical_total": total_expected_typical,
        "missing_mondais": missing_mondais,
        "missing_question_ranges": missing_ranges,
        "warnings": warnings,
        "mondai_audit": audit_details,
    }

    logger.info(
        f"📊 [JLPT AUDIT] Kết quả soát đề {jlpt_level} - {section_type}: "
        f"{total_actual}/{total_expected_typical} câu. "
        f"Trạng thái: {overall_status} (Thiếu {len(missing_mondais)} mondai, {len(missing_ranges)} dải câu)"
    )

    return audit_result


def infer_mondai_from_spec(
    raw_question_number: Optional[int],
    question_text: str,
    jlpt_level: str,
    section_type: str,
    options: Optional[List[Dict[str, Any]]] = None,
) -> str:
    """
    Suy luận mondai_code chuẩn xác nhất:
    1. Ưu tiên số thứ tự in trên đề gốc (raw_question_number) đối chiếu với dải câu chuẩn của JLPT.
    2. Phân tích đặc trưng ngôn ngữ của câu hỏi và 4 lựa chọn.
    """
    q_text = (question_text or "").strip()
    opts = options or []

    # 1. NẾU CÓ SỐ THỨ TỰ IN TRÊN ĐỀ: Đây là căn cứ chuẩn xác 100% của đề thi JLPT
    if raw_question_number is not None:
        try:
            rn = int(raw_question_number)
            specs = get_expected_mondais(jlpt_level, section_type)
            for spec in specs:
                start, end = spec.typical_range
                if start <= rn <= end:
                    return spec.mondai_code
        except (ValueError, TypeError):
            pass

    # 2. PHÂN TÍCH ĐẶC TRƯNG NGÔN NGỮ NẾU KHÔNG CÓ SỐ THỨ TỰ:
    # Đặc trưng A: Câu sắp xếp có dấu sao ★
    if "★" in q_text or re.search(r'[_＿]{2,}.*?★', q_text):
        return "SENTENCE_COMPOSITION"

    # Đặc trưng B: Bài đục lỗ (TEXT_GRAMMAR)
    if re.match(r'^[【\[\(（]?\s*\d+\s*[】\]\)）]?$', q_text):
        return "TEXT_GRAMMAR"

    # Đặc trưng C: Dạng cách dùng từ (PROPER_USAGE - Mondai 5)
    # Câu hỏi CHỈ LÀ 1 TỪ DUY NHẤT trong ngoặc vuông (như 【案内】, 【るす】, 【しんせつ】)
    # và 4 options là 4 câu văn dài
    is_isolated_word = bool(re.match(r'^[【\[「]([^\s】」]+)[】\]」]$', q_text)) or (
        len(q_text) <= 8 and not any(p in q_text for p in ["は", "が", "を", "に", "で", "。", "、"])
    )
    has_sentence_options = opts and any(len(opt.get("option_text", "")) >= 8 for opt in opts)
    if is_isolated_word and has_sentence_options and section_type == "VOCABULARY":
        return "PROPER_USAGE"

    # Đặc trưng D: Từ vựng (VOCABULARY)
    if section_type == "VOCABULARY":
        kanji_in_bracket = bool(re.search(r'【[\u4e00-\u9faf]+】', q_text))
        kana_in_bracket = bool(re.search(r'【[\u3040-\u309f]+】', q_text))
        opt_has_kanji = any(re.search(r'[\u4e00-\u9faf]', opt.get("option_text", "")) for opt in opts)
        opt_all_kana = all(re.match(r'^[\u3040-\u309f\u30a0-\u30ff\s]+$', opt.get("option_text", "").strip())
                           for opt in opts if opt.get("option_text"))

        if kana_in_bracket and opt_has_kanji:
            return "ORTHOGRAPHY"
        if kanji_in_bracket and opt_all_kana:
            return "KANJI_READING"
        if kana_in_bracket:
            return "ORTHOGRAPHY"
        if kanji_in_bracket:
            return "KANJI_READING"

    # Đặc trưng E: Điền từ vào chỗ trống
    if "（" in q_text or "(" in q_text or "＿＿" in q_text:
        return "CONTEXTUAL_USE" if section_type == "VOCABULARY" else "SENTENTIAL_GRAMMAR"

    # Đặc trưng F: Từ đồng nghĩa (SYNONYMS)
    if "同じ" in q_text or "おなじ" in q_text or "最も近い" in q_text:
        return "SYNONYMS"

    return "CONTEXTUAL_USE" if section_type == "VOCABULARY" else "SENTENTIAL_GRAMMAR"
