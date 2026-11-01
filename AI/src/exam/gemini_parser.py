"""
GeminiExamParser — Trích xuất đề thi JLPT bằng Gemini Vision + Audio API.

Pipeline:
  PDF  → PyMuPDF render từng trang → ảnh PNG
       → Gửi từng trang lên Gemini Vision kèm structured prompt
       → Gemini trả JSON chuẩn (câu hỏi, đáp án, passage, hình ảnh)
       → Merge + validate → Trả về danh sách questions

  Audio → Upload lên Gemini File API
        → Gemini multimodal transcribe + parse mốc giây câu nghe
        → Trả về danh sách questions với audio_start_time, audio_end_time, transcript
"""

import hashlib
import io
import json
import logging
import os
import re
import tempfile
from pathlib import Path
from typing import Any, Dict, List, Optional, Tuple
from dotenv import load_dotenv
import numpy as np
from PIL import Image

load_dotenv()

from .jlpt_rules import audit_exam_section, infer_mondai_from_spec, get_expected_mondais

logger = logging.getLogger("kanji_ai")

# Thư mục cache kết quả trích xuất đề thi PDF
CACHE_DIR = Path(__file__).resolve().parent.parent.parent / "cache" / "exam_extractions"


# ====================================================================
# ĐỊNH NGHĨA CÁC MONDAI CODE CHUẨN JLPT (đồng bộ với FE)
# ====================================================================
VALID_MONDAI_CODES = {
    # Từ vựng (文字・語彙)
    "KANJI_READING", "ORTHOGRAPHY", "CONTEXTUAL_USE", "SYNONYMS", "PROPER_USAGE",
    # Ngữ pháp (文法)
    "SENTENTIAL_GRAMMAR", "SENTENCE_COMPOSITION", "TEXT_GRAMMAR",
    # Đọc hiểu (読解)
    "SHORT_PASSAGE", "MEDIUM_PASSAGE", "LONG_PASSAGE", "INFORMATION_RETRIEVAL",
    # Nghe hiểu (聴解)
    "TASK_BASED", "KEY_POINT", "GENERAL_OUTLINE", "UTTERANCE_EXPRESSIONS", "QUICK_RESPONSE",
}

# ====================================================================
# PROMPT TEMPLATES CHO GEMINI
# ====================================================================

PDF_EXTRACTION_PROMPT = """Bạn là chuyên gia phân tích đề thi năng lực tiếng Nhật JLPT cấp {jlpt_level}.

Hãy nhìn vào hình ảnh trang đề thi này và trích xuất TẤT CẢ các câu hỏi có trong trang.

## Quy tắc quan trọng:
1. QUY TẮC VỀ ĐÁP ÁN VÀ NỘI DUNG (TUYỆT ĐỐI KHÔNG ĐOÁN MÒ ĐÁP ÁN HOẶC BỊA GIẢI THÍCH):
   - BẠN TUYỆT ĐỐI KHÔNG TỰ Ý ĐOÁN MÒ HOẶC PHÁN ĐOÁN ĐÁP ÁN:
     + Mặc định toàn bộ các phương án 1, 2, 3, 4 BẮT BUỘC ĐỂ `is_correct: false` (trừ khi trên trang đề thi in sẵn đáp án chính thức).
     + Trường `explanation_vi` ĐỂ CHUỖI RỖNG `""`.
   - NHIỆM VỤ TRỌNG TÂM DUY NHẤT: Tập trung 100% vào việc đọc và trích xuất nguyên văn, chính xác nội dung câu hỏi, bài đọc, chỗ trống và 4 phương án lựa chọn từ trang PDF.
   - Mỗi câu hỏi BẮT BUỘC PHẢI CÓ ĐẦY ĐỦ CẢ 4 PHƯƠNG ÁN LỰA CHỌN (options 1, 2, 3, 4).
   - TUYỆT ĐỐI KHÔNG ĐƯỢC ĐỂ TRỐNG `option_text` (không được để chuỗi rỗng `""` hoặc bỏ sót). Phải đọc kỹ trên đề thi và trích xuất đúng nội dung từng phương án 1, 2, 3, 4.

2. QUY TẮC BẮT BUỘC VỀ FURIGANA (PHIÊN ÂM CHỮ HÁN TRÊN ĐẦU CHỮ):
   - Trong toàn bộ đề thi (cả Ngữ pháp, Đọc hiểu và các phương án trả lời):
     BẤT KỲ chữ Hán nào có chữ Hiragana nhỏ in phiên âm ở trên đầu (Furigana / ルビ), BẮT BUỘC TRÍCH XUẤT BẰNG THẺ CHUẨN HTML:
     `<ruby>漢字<rt>ふりがな</rt></ruby>` (ví dụ: `<ruby>米<rt>こめ</rt></ruby>`, `<ruby>作<rt>つく</rt></ruby>った`, `<ruby>部屋<rt>へや</rt></ruby>`, `<ruby>案内<rt>あんない</rt></ruby>`).
   - TUYỆT ĐỐI KHÔNG BỎ SÓT FURIGANA khi trên trang đề thi có in phiên âm!
   - ĐẶC BIỆT CHÚ Ý Ở PHẦN NGỮ PHÁP (Mondai 1: SENTENTIAL_GRAMMAR):
     * TUYỆT ĐỐI KHÔNG ĐƯỢC đặt chữ Hán trong ngoặc vuông 【 】 và TUYỆT ĐỐI KHÔNG gạch chân <u> bất kỳ chữ Hán nào!
     * Trong Mondai 1 ngữ pháp CHỈ CÓ DUY NHẤT CHỖ TRỐNG `（　）` là cần điền trợ từ/ngữ pháp.
     * Chữ Hán có phiên âm nhỏ trên đầu thì biểu diễn bằng `<ruby>漢字<rt>furigana</rt></ruby>`, TUYỆT ĐỐI KHÔNG viết `【漢字】` hay `<u>漢字</u>`.
   - NGOẶC VUÔNG 【 】 CHỈ ĐƯỢC DÙNG DUY NHẤT CHO CÁC TRƯỜNG HỢP SAU:
     * Từ gạch chân trong KANJI_READING (ví dụ `きのう【夕方】`)
     * Từ gạch chân trong ORTHOGRAPHY (ví dụ `【あおい】`)
     * Từ gạch chân trong SYNONYMS (ví dụ `【会社を 休みました】`)
     * Từ vựng hỏi trong PROPER_USAGE (ví dụ `【案内】`)
     * Ô số đục lỗ trong TEXT_GRAMMAR (ví dụ `【 21 】`)
     TUYỆT ĐỐI KHÔNG DÙNG 【 】 CHO BẤT KỲ TỪ NÀO KHÁC!

3. QUY TẮC BẮT BUỘC THEO TỪNG DẠNG CÂU HỎI (MONDAI):
   - PHẦN TỪ VỰNG (VOCABULARY - 文字・語彙) - QUY TẮC BẮT BUỘC TỪNG MONDAI:
     + TUYỆT ĐỐI KHÔNG CÓ BÀI ĐỌC (PASSAGE)! Tất cả các câu hỏi phần Từ vựng (Mondai 1, 2, 3, 4, 5) đều là câu hỏi câu đơn độc lập.
     + passage của TẤT CẢ câu hỏi từ vựng BẮT BUỘC là null. TUYỆT ĐỐI KHÔNG gom các câu hỏi điền từ vào trường passage!
     
     + MONDAI 1: KANJI_READING (漢字読み - ĐỌC CHỮ HÁN):
       * Đề bài gạch chân chữ Hán đặt trong ngoặc vuông: ví dụ `きのう【夕方】、雨が ふりました。`
       * CỰC KỲ QUAN TRỌNG: Chữ Hán được gạch chân / hỏi cách đọc TUYỆT ĐỐI KHÔNG ĐƯỢC CÓ FURIGANA / THẺ <ruby>! (Vì đây là câu hỏi kiểm tra cách đọc, nếu ghi furigana sẽ làm lộ đáp án!). Ví dụ: `きのう【夕方】` hoặc `<u>夕方</u>`, TUYỆT ĐỐI KHÔNG viết `<ruby>夕方<rt>ゆうがた</rt></ruby>`.
       * 4 phương án lựa chọn (options 1, 2, 3, 4) là HIRAGANA chỉ cách đọc.
       * mondai_code BẮT BUỘC là "KANJI_READING".

     + MONDAI 2: ORTHOGRAPHY (表記 - CÁCH VIẾT CHỮ HÁN / HIRAGANA -> KANJI):
       * ĐÂY LÀ DẠNG BÀI BẮT BUỘC CỦA ĐỀ THI JLPT (thường từ câu 10 đến 15 ở N4/N3, câu 13 đến 20 ở N5)!
       * TUYỆT ĐỐI KHÔNG BỎ QUA HOẶC ĐỂ SÓT MONDAI 2 DÙ IN Ở TRANG NÀO!
       * Đề bài gạch chân từ HIRAGANA/KATAKANA đặt trong ngoặc vuông: ví dụ `すずきさんは 【あおい】 シャツを きて います。`
       * 4 phương án lựa chọn (options 1, 2, 3, 4) là CHỮ HÁN (hoặc Katakana) chỉ cách viết: 1. 青い 2. 黒い 3. 赤い 4. 白い.
       * mondai_code BẮT BUỘC là "ORTHOGRAPHY". TUYỆT ĐỐI KHÔNG gán nhầm thành KANJI_READING hay CONTEXTUAL_USE!

     + MONDAI 3: DẠNG ĐIỀN TỪ VÀO CÂU (CONTEXTUAL_USE - 文脈規定, thường từ câu 16-25 ở N4):
       * Mỗi câu hỏi là 1 câu văn độc lập có chỗ trống in trên đề, ví dụ: "けさ せんたくした ふくが まだ （　　） いません。"
       * question_text BẮT BUỘC PHẢI LÀ NGUYÊN CÂU ĐẦY ĐỦ CÓ CHỖ TRỐNG `（　　）`! TUYỆT ĐỐI KHÔNG CHỈ GHI MỖI SỐ `【 16 】` hay `(16)`!
       * mondai_code BẮT BUỘC là "CONTEXTUAL_USE". TUYỆT ĐỐI KHÔNG gán "TEXT_GRAMMAR" hay "KANJI_READING"!

     + MONDAI 4: SYNONYMS (言い換え類義 - TỪ ĐỒNG NGHĨA, thường từ câu 26-30 ở N4/N3, câu 31-35 ở N5):
       * Đề bài có từ hoặc cụm từ gạch chân đặt trong ngoặc vuông: ví dụ `きのうは 【会社を 休みました】。`
       * 4 phương án lựa chọn là 4 câu văn, chọn câu có ý nghĩa tương đương nhất.
       * mondai_code BẮT BUỘC là "SYNONYMS".

     + MONDAI 5: PROPER_USAGE (用法 - CÁCH DÙNG TỪ CHÍNH XÁC, thường từ câu 31-35 ở N4/N3):
       * `question_text` là từ vựng được hỏi trong ngoặc vuông, ví dụ: `【案内】`, `【ちこく】`, `【るす】`, `【しんせつ】`.
       * TUYỆT ĐỐI KHÔNG đưa câu chỉ dẫn dài ("もんだい５... つぎの ことばの つかいかたで...") vào `question_text`!
       * 4 phương án lựa chọn là 4 câu văn sử dụng từ đó, và từ vựng đó XUẤT HIỆN trong câu và BẮT BUỘC PHẢI ĐƯỢC GẠCH CHÂN BẰNG THẺ `<u>...</u>` (ví dụ: `友だちを 駅まで<u>案内</u>しました。`).
       * mondai_code BẮT BUỘC là "PROPER_USAGE".
     + DẠNG BÀI ĐỤC LỖ (TEXT_GRAMMAR) CHỈ XUẤT HIỆN Ở PHẦN NGỮ PHÁP (Mondai 3 Ngữ pháp), TUYỆT ĐỐI KHÔNG CÓ Ở PHẦN TỪ VỰNG!

   - DẠNG ĐIỀN TỪ / NGỮ PHÁP CÂU ĐƠN (SENTENTIAL_GRAMMAR):
     + BẮT BUỘC PHẢI CÓ DẤU CHỖ TRỐNG `（　　）` trong `question_text`.
     + Ví dụ: `これは わたしが <ruby>米<rt>こめ</rt></ruby>（　　）<ruby>作<rt>つく</rt></ruby>った パンです。`
     + TUYỆT ĐỐI KHÔNG tự ý điền sẵn từ/đáp án vào câu hỏi.
     + TUYỆT ĐỐI KHÔNG đặt ngoặc vuông 【 】 quanh bất kỳ chữ Hán nào.
     + ĐẶC BIỆT CHÚ Ý DẠNG HỘI THOẠI (ví dụ: A và B, hoặc 山田 và 田中):
       * BẮT BUỘC ngăn cách lượt nói của từng nhân vật bằng ký tự xuống dòng `\n` để xếp thành 2 hàng riêng biệt:
         Ví dụ: `A「あした いっしょに ドライブに 行きませんか。」\nB「（　　）、ごめんなさい、あしたは アルバイトが あるんです。」`
         Ví dụ: `山田「田中さん、これから 食事でも どうですか。」\n田中「すみません。ちょうど ＿＿＿　＿＿＿　＿★＿　＿＿＿ です。」`

   - DẠNG SẮP XẾP CÂU CÓ DẤU SAO ★ (SENTENCE_COMPOSITION):
     + Biểu diễn đủ 4 vị trí gạch kèm ngôi sao: `＿＿＿　＿＿＿　＿★＿　＿＿＿`.
     + BẮT BUỘC gán `star_position`: 1, 2, 3 hoặc 4 (chỉ vị trí của dấu sao ★ trong 4 vị trí gạch).

   - DẠNG ĐỤC LỖ / NGỮ PHÁP ĐOẠN VĂN (TEXT_GRAMMAR - Mondai 3 Ngữ pháp):
     + Đề thi chứa bài văn đục lỗ có các chỗ trống đánh số trong khung ô vuông ví dụ [ 21 ], [ 22 ], [ 23 ], [ 24 ], [ 25 ], [ 26 ]...
     + ĐẶC BIỆT CHÚ Ý CẤU TRÚC 3 THÀNH PHẦN BẮT BUỘC CỦA ĐỀ THI MONDAI 3:
       1. CÂU MỞ ĐẦU / DẪN TÌNH HUỐNG BỐI CẢNH NẰM NGOÀI KHUNG:
          In ngay dưới câu chỉ dẫn đề bài Mondai 3 và TRƯỚC các khung kẻ ô bài văn (ví dụ: `日本で べんきょうして いる 学生が 「すきな 店」の ぶんしょうを 書いて、クラスの みんなの 前で 読みました。`).
          -> BẮT BUỘC TRÍCH XUẤT 100% CÂU MỞ ĐẦU NÀY VÀO ĐẦU CỦA `passage.content_jp`! TUYỆT ĐỐI KHÔNG ĐƯỢC BỎ SÓT!
       2. KHUNG BÀI VĂN SỐ 1 (ví dụ: `(1) ケンさんの ぶんしょう` chứa các chỗ trống `【 22 】`, `【 23 】`):
          BẮT BUỘC TRÍCH XUẤT ĐẦY ĐỦ CẢ TIÊU ĐỀ `(1) ケンさんの ぶんしょう` VÀ TOÀN BỘ NỘI DUNG BÀI VĂN (1)!
          LƯU Ý QUAN TRỌNG: `(1) ケンさんの ぶんしょう` LÀ PHẦN THI CHÍNH THỨC, TUYỆT ĐỐI KHÔNG PHẢI LÀ VÍ DỤ (例)!
       3. KHUNG BÀI VĂN SỐ 2 (ví dụ: `(2) ミンジさんの ぶんしょう` chứa các chỗ trống `【 24 】`, `【 25 】`, `【 26 】`):
          BẮT BUỘC TRÍCH XUẤT ĐẦY ĐỦ CẢ TIÊU ĐỀ `(2) ミンジさんの ぶんしょう` VÀ TOÀN BỘ NỘI DUNG BÀI VĂN (2)!
       -> CẢ 3 THÀNH PHẦN TRÊN PHẢI ĐƯỢC GHÉP NỐI THEO THỨ TỰ THÀNH MỘT NỘI DUNG DUY NHẤT TRONG `passage.content_jp` CỦA TẤT CẢ CÁC CÂU HỎI TỪ 22 ĐẾN 26!
       -> CẢNH BÁO NGHIÊM NGẶT: TUYỆT ĐỐI KHÔNG ĐƯỢC CHỈ TRÍCH XUẤT MỖI KHUNG THỨ 2 (bài Minji) MÀ BỎ RƠI KHUNG THỨ 1 (bài Ken) VÀ CÂU DẪN MỞ ĐẦU!
     + `passage.title`: BẮT BUỘC ĐẶT TIÊU ĐỀ CHUNG LÀ "文章の文法" (hoặc "Ngữ pháp đoạn văn"). TUYỆT ĐỐI KHÔNG LẤY TÊN RIÊNG CỦA MỘT NGƯỜI (NHƯ "ミンジさんのぶんしょう") LÀM TIÊU ĐỀ CHUNG!
     + BẮT BUỘC TRÍCH XUẤT ĐỦ 100% CÁC CHỖ TRỐNG ĐÁNH SỐ TRÊN ĐỀ VÀO TRONG `passage.content_jp`: các chỗ trống này PHẢI ĐƯỢC ĐẶT TRONG NGOẶC VUÔNG RÕ RÀNG: `【 21 】`, `【 22 】`, `【 23 】`, `【 24 】`, `【 25 】`, `【 26 】`... TUYỆT ĐỐI KHÔNG để số trần không ngoặc (như "21。", "22 から", "23。", "24、", "名詞 25")!
     + TUYỆT ĐỐI KHÔNG TỰ ĐIỀN TỪ, TRỢ TỪ (như は, が, を, に, で...) HOẶC SỬA VĂN LÀM MẤT BẤT KỲ CHỖ TRỐNG NÀO (ĐẶC BIỆT CHỖ TRỐNG CUỐI CÙNG NHƯ 【 25 】, 【 26 】)!
     + Vì dạng bài đục lỗ trên đề thi KHÔNG CÓ CÂU HỎI RIÊNG cho từng số, TUYỆT ĐỐI KHÔNG đưa câu chỉ dẫn đề bài ("もんだい３... 何を入れますか...") hay "( )" vào question_text!
     + `question_text` của từng câu đục lỗ BẮT BUỘC chỉ ghi số chỗ trống: "【 21 】", "【 22 】", "【 23 】", "【 24 】", "【 25 】", "【 26 】"...
     + Mondai code của TẤT CẢ các câu hỏi trong bài đục lỗ BẮT BUỘC là "TEXT_GRAMMAR".
     + Mỗi ô trống chỉ tạo DUY NHẤT 1 câu hỏi tương ứng, TUYỆT ĐỐI KHÔNG lặp lại câu hỏi.

   - DẠNG ĐỌC HIỂU ĐOẠN VĂN (SHORT_PASSAGE, MEDIUM_PASSAGE, LONG_PASSAGE):
     + BẮT BUỘC trích xuất đầy đủ 100% nội dung đoạn văn tiếng Nhật vào `passage.content_jp`. Bất kỳ chữ Hán nào có furigana thì dùng `<ruby>漢字<rt>furigana</rt></ruby>`.
     + `passage.title`: Đặt tên ngắn gọn cho bài đọc (ví dụ: "Bài đọc (1)", "Bài đọc hiểu", hoặc tiêu đề tờ rơi như "公園で遊ぶときの注意").
       * CỰC KỲ QUAN TRỌNG: TUYỆT ĐỐI KHÔNG copy câu chỉ dẫn đề thi của Mondai (như "もんだい５ つぎの ぶんしょうを 読んで、しつもんに こたえて ください...") vào `passage.title`!
       * TUYỆT ĐỐI KHÔNG dùng thẻ `<ruby>` hay `<rt>` trong `passage.title` (chỉ dùng furigana trong `passage.content_jp`).
       * Nếu đề thi có câu dẫn tình huống/bối cảnh trước bài đọc (ví dụ: `(3) 森さんの机の上に、山口先生のメモと本があります。`), BẮT BUỘC đưa câu này vào ĐẦU của `passage.content_jp` (kèm `<ruby>`), và để `passage.title` là "Bài đọc (3)".
     + Gán đối tượng `passage` này vào câu hỏi tương ứng trong trang.

   - QUY TẮC CÂU HỎI BÀI ĐỌC NẰM Ở TRANG TIẾP THEO:
     + Trong phần Đọc hiểu (MEDIUM_PASSAGE, LONG_PASSAGE), một bài đọc có thể kèm nhiều câu hỏi (ví dụ câu 30, 31).
     + Nếu bài đọc in ở trang trước, còn câu hỏi tiếp theo cùng bài đọc in ở đầu trang này mà KHÔNG in lại bài đọc:
       * BẮT BUỘC để `passage: null` cho các câu hỏi tiếp theo này.
       * BẮT BUỘC gán đúng `mondai_code` (ví dụ "MEDIUM_PASSAGE" hoặc "LONG_PASSAGE").
       * TUYỆT ĐỐI KHÔNG tự bịa một bài đọc giả, tóm tắt hoặc copy đề câu hỏi vào `passage`! Hệ thống sẽ tự động gán bài đọc đầy đủ từ câu trước sang.
     + LƯU Ý ĐẶC BIỆT VỚI N5/N4: Câu hỏi cuối cùng (ví dụ Câu 32 của N5, Câu 34-35 của N4) là Mondai 6 (INFORMATION_RETRIEVAL - Tìm kiếm thông tin tờ rơi/thông báo). ĐÂY LÀ MONDAI RIÊNG BIỆT ĐỘC LẬP KÈM TỜ RƠI MỚI, TUYỆT ĐỐI KHÔNG gán nhầm thành MEDIUM_PASSAGE và TUYỆT ĐỐI KHÔNG để passage: null!

   - DẠNG TÌM KIẾM THÔNG TIN TỪ TỜ RƠI / BẢNG THÔNG BÁO / BIỂU MẪU (INFORMATION_RETRIEVAL - Mondai cuối phần Đọc hiểu):
     + ĐÂY LÀ DẠNG BÀI ĐỌC TỜ RƠI QUẢNG CÁO, ÁP PHÍCH, BẢNG THÔNG BÁO HOẶC BIỂU MẪU QUY ĐỊNH CỦA ĐỀ THI JLPT:
     + TUYỆT ĐỐI KHÔNG CẮT ẢNH VÀO ĐỀ! Hệ thống hiển thị bài đọc trực tiếp bằng văn bản số hóa định dạng đẹp.
     + CẢNH BÁO QUAN TRỌNG: TỜ RƠI THƯỜNG GỒM NHIỀU KHỐI HỘP, NHIỀU BẢNG BIỂU, VÀ NHIỀU MỤC KHÁC NHAU NẰM RẢI RÁC TRÊN TOÀN BỘ TRANG!
       TUYỆT ĐỐI KHÔNG ĐƯỢC CHỈ LẤY 1 KHỐI RỒI DỪNG LẠI! Bạn BẮT BUỘC phải đọc toàn bộ từ đỉnh đến đáy của tờ poster và trích xuất ĐẦY ĐỦ 100% TẤT CẢ các thành phần sau vào `passage.content_jp`:
       1. Tiêu đề chính tờ rơi / thông báo: Đặt trong ngoặc vuông to: ví dụ `【公園で遊ぶときの注意】` hoặc `【図書館利用案内】`.
       2. Mọi đoạn văn giới thiệu, đối tượng áp dụng.
       3. TẤT CẢ CÁC BẢNG BIỂU (bảng giờ mở cửa, giá vé, học phí, lịch trình, đối tượng...): BẮT BUỘC TRÌNH BÀY BẰNG BẢNG MARKDOWN CHUẨN (`| 項目 | 内容 |` kèm `|---|---|`). Trích xuất đầy đủ tất cả các ô, hàng, cột không được bỏ sót!
       4. TẤT CẢ các khung hộp thông tin con (ví dụ: khung "利用案内", khung "申し込み", khung "料金", khung "注意事項").
       5. TẤT CẢ các điều khoản, quy định, gạch đầu dòng (・), số thứ tự (1., 2.).
       6. TẤT CẢ các chú thích đặc biệt, điều kiện ngoại lệ, lưu ý quan trọng bắt đầu bằng `※ ` hoặc `【注意】` hoặc `（注）` ở cuối hoặc bên lề tờ rơi.
     + Nếu chữ Hán có phiên âm Furigana ở trên đầu, BẮT BUỘC dùng thẻ `<ruby>漢字<rt>furigana</rt></ruby>`.
     + Khoảng cách: Dùng 2 dấu xuống dòng `\n\n` giữa các khối/phần để form trình bày thoáng đãng, dễ đọc.
     + `passage.title`: Ghi tiêu đề tờ rơi (ví dụ: "公園で遊ぶときの注意").
     + `passage.passage_type`: "TEXT".
     + `has_visual`: false.

   - QUY TẮC BẮT BUỘC VỀ has_visual VÀ HÌNH ẢNH:
     + BÀI THI NGHE (LISTENING): BẮT BUỘC PHẢI GIỮ NGUYÊN TRANH Ở CÂU HỎI VÀ ĐÁP ÁN KHI CÓ TRANH MINH HỌA VẼ:
       * Nếu câu hỏi nghe có tranh tình huống / tranh nhân vật / khung tranh 4 lựa chọn (Mondai 1, Mondai 3/4...):
         - Gán `has_visual: true`.
         - BẮT BUỘC trả về trường `image_box`: [ymin, xmin, ymax, xmax] (tọa độ chuẩn hóa từ 0 đến 1000 của khung hình chữ nhật chứa tranh vẽ của câu hỏi đó trên trang). Ví dụ: `[120, 150, 480, 850]`.
       * Nếu 4 phương án lựa chọn là 4 tranh vẽ (ví dụ: hình xe đạp, tàu điện, xe buýt, ô tô):
         - Gán `has_visual_options: true` và `options[i].has_image: true`.
         - Cung cấp `image_box` là khung chữ nhật bao toàn bộ tranh các lựa chọn này.

       * QUY TẮC TUYỆT ĐỐI CHO CÂU HỎI NGHE CHỈ CÓ CHỮ (TEXT-ONLY QUESTIONS):
         - Những câu hỏi nghe có mỗi chữ, không có tranh vẽ minh họa tình huống (ví dụ: toàn bộ Mondai 2 - KEY_POINT, và các câu hỏi cuối của Mondai 1 - TASK_BASED in phương án bằng chữ như câu 7, 8):
           + BẮT BUỘC gán has_visual: false, has_visual_options: false, image_box: null, image_url: null.
           + TUYỆT ĐỐI KHÔNG CẮT ẢNH VÀ KHÔNG TRẢ VỀ image_box CHO CÂU HỎI CHỈ CÓ CHỮ!
           + Trích xuất 100% nội dung chữ của các phương án vào options[i].option_text.
         - Mondai 4 (QUICK_RESPONSE): 100% không có tranh, BẮT BUỘC has_visual: false, image_box: null.
+ PHẦN ĐỌC HIỂU (SHORT_PASSAGE, MEDIUM_PASSAGE, LONG_PASSAGE, INFORMATION_RETRIEVAL, TEXT_GRAMMAR) VÀ TỪ VỰNG, NGỮ PHÁP:
       * VỀ BÀI ĐỌC (PASSAGE): Toàn bộ chữ, cấu trúc bảng biểu, chú thích phải số hóa 100% vào `passage.content_jp`. TUYỆT ĐỐI KHÔNG CẮT ẢNH VÀO BÀI ĐỌC (`passage.image_url: null`).
       * VỀ TRANH ẢNH PHƯƠNG ÁN LỰA CHỌN (OPTIONS CÓ TRANH/SƠ ĐỒ):
         - Nếu câu hỏi Đọc hiểu có 4 phương án lựa chọn là TRANH VẼ / SƠ ĐỒ / BẢN ĐỒ / HÌNH MINH HỌA (ví dụ: câu hỏi "今のへやはどれですか" với 4 hình căn phòng 1, 2, 3, 4):
           + BẮT BUỘC gán `has_visual_options: true` và `has_visual: true`.
           + Trả về `image_box`: [ymin, xmin, ymax, xmax] là khung hình chữ nhật bao trọn 4 bức tranh lựa chọn này trên trang.
           + Trong `options`: gán `option_text: "1"`, `"2"`, `"3"`, `"4"` và `has_image: true`.
         - Đối với các câu hỏi đọc hiểu thông thường (phương án lựa chọn hoàn toàn bằng chữ): BẮT BUỘC gán `has_visual: false`, `has_visual_options: false`, `image_box: null`, `image_url: null`.
     + Các câu hỏi ngữ pháp đơn (SENTENTIAL_GRAMMAR) và sắp xếp câu sao (SENTENCE_COMPOSITION) BẮT BUỘC phải có `passage: null`. TUYỆT ĐỐI KHÔNG gán passage vào câu ngữ pháp!
     + Nếu cấp độ là N5 hoặc N4: phần Đọc hiểu chỉ gồm SHORT_PASSAGE (1 câu hỏi/bài), MEDIUM_PASSAGE (2 câu hỏi/bài) và INFORMATION_RETRIEVAL (tờ rơi). N5 và N4 KHÔNG CÓ LONG_PASSAGE.

3. Xác định mondai_code chuẩn:
   - KANJI_READING, ORTHOGRAPHY, CONTEXTUAL_USE, SYNONYMS, PROPER_USAGE
   - SENTENTIAL_GRAMMAR, SENTENCE_COMPOSITION, TEXT_GRAMMAR
   - SHORT_PASSAGE, MEDIUM_PASSAGE, LONG_PASSAGE, INFORMATION_RETRIEVAL
   - TASK_BASED, KEY_POINT, GENERAL_OUTLINE, UTTERANCE_EXPRESSIONS, QUICK_RESPONSE

4. Giữ nguyên 100% nội dung tiếng Nhật của bài đọc và câu hỏi, KHÔNG tóm tắt, KHÔNG lược bớt.
5. TUYỆT ĐỐI để "is_correct": false cho TẤT CẢ các options của MỌI câu hỏi khi trích xuất từ đề thi PDF (vì đề thi chưa kèm đáp án; bảng đáp án 正答表 sẽ được nạp sau). TUYỆT ĐỐI KHÔNG tự ý gán is_correct: true!

## Trả về CHÍNH XÁC JSON theo format sau (không kèm markdown fences):
{{
  "page_number": {page_num},
  "questions": [
    {{
      "question_number": 1,
      "mondai_code": "TASK_BASED",
      "question_text": "1ばん",
      "star_position": null,
      "has_visual": true,
      "has_visual_options": true,
      "image_box": [120, 160, 520, 850],
      "explanation_vi": "",
      "passage": null,
      "options": [
        {{"option_key": 1, "option_text": "1", "is_correct": false}},
        {{"option_key": 2, "option_text": "2", "is_correct": false}},
        {{"option_key": 3, "option_text": "3", "is_correct": false}},
        {{"option_key": 4, "option_text": "4", "is_correct": false}}
      ]
    }},
    {{
      "question_number": 1,
      "mondai_code": "KANJI_READING",
      "question_text": "きのう、あたらしい【本】をかいました。",
      "star_position": null,
      "has_visual": false,
      "image_box": null,
      "explanation_vi": "",
      "passage": null,
      "options": [
        {{"option_key": 1, "option_text": "ほん", "is_correct": false}},
        {{"option_key": 2, "option_text": "ぼん", "is_correct": false}},
        {{"option_key": 3, "option_text": "ぽん", "is_correct": false}},
        {{"option_key": 4, "option_text": "ばん", "is_correct": false}}
      ]
    }},
    {{
      "question_number": 19,
      "mondai_code": "TEXT_GRAMMAR",
      "question_text": "【 21 】",
      "star_position": null,
      "has_visual": false,
      "image_box": null,
      "explanation_vi": "Giải thích",
      "passage": {{
        "title": "文章の文法",
        "passage_type": "TEXT",
        "content_jp": "わたしの 家には 犬が います。... わたしたちは 犬を 家の 中で 【 21 】 。... 父の けいたいでんわを 【 22 】 からです。...",
        "translation_vi": ""
      }},
      "options": [
        {{"option_key": 1, "option_text": "について", "is_correct": false}},
        {{"option_key": 2, "option_text": "によって", "is_correct": false}},
        {{"option_key": 3, "option_text": "に対して", "is_correct": false}},
        {{"option_key": 4, "option_text": "にとって", "is_correct": false}}
      ]
    }}
  ],
  "passages": [
    {{
      "title": "Bài đọc hiểu",
      "passage_type": "TEXT",
      "content_jp": "Nội dung đoạn văn tiếng Nhật nếu có trên trang...",
      "question_indices": [0]
    }}
  ]
}}

Nếu trang không chứa câu hỏi nào (ví dụ: trang bìa, trang hướng dẫn), trả về:
{{"page_number": {page_num}, "questions": [], "passages": []}}
"""

PDF_BATCH_EXTRACTION_PROMPT = """Bạn là chuyên gia phân tích đề thi năng lực tiếng Nhật JLPT cấp {jlpt_level}.

Dưới đây là {num_pages} trang đề thi liên tiếp (gồm các trang {pages_str}) được gửi kèm theo thứ tự.
Hãy đọc kỹ toàn bộ các trang và trích xuất TẤT CẢ các câu hỏi có trong những trang này.

ĐẶC BIỆT CHÚ Ý CÂU HỎI VẮT QUA RANH GIỚI TRANG:
- Nếu một câu hỏi hoặc bài đọc bắt đầu ở cuối trang trước và tiếp diễn ở đầu trang sau: bạn nhìn thấy cả hai trang nên HÃY GHÉP NGUYÊN VẸN NỘI DUNG câu hỏi và các phương án lựa chọn thành một câu hỏi hoàn chỉnh duy nhất.
- Trường `source_page`: ghi số trang mà câu hỏi đó bắt đầu.
- Trường `raw_question_number`: ghi chính xác số thứ tự câu hỏi in trên đề thi (ví dụ 1, 2, ..., 35).

## Quy tắc quan trọng:
1. QUY TẮC VỀ ĐÁP ÁN VÀ NỘI DUNG (TUYỆT ĐỐI KHÔNG ĐOÁN MÒ ĐÁP ÁN):
   - Mặc định toàn bộ các phương án 1, 2, 3, 4 BẮT BUỘC ĐỂ `is_correct: false` (trừ khi trên trang đề thi in sẵn đáp án chính thức).
   - Trường `explanation_vi` ĐỂ CHUỖI RỖNG `""`.
   - Mỗi câu hỏi BẮT BUỘC PHẢI CÓ ĐẦY ĐỦ CẢ 4 PHƯƠNG ÁN LỰA CHỌN (options 1, 2, 3, 4).
   - TUYỆT ĐỐI KHÔNG ĐƯỢC ĐỂ TRỐNG `option_text`. Phải đọc kỹ trên đề thi và trích xuất đúng nội dung từng phương án 1, 2, 3, 4.

2. QUY TẮC BẮT BUỘC VỀ FURIGANA (PHIÊN ÂM CHỮ HÁN TRÊN ĐẦU CHỮ):
   - BẤT KỲ chữ Hán nào có chữ Hiragana nhỏ in phiên âm ở trên đầu (Furigana / ルビ), BẮT BUỘC TRÍCH XUẤT BẰNG THẺ CHUẨN HTML: `<ruby>漢字<rt>ふりがな</rt></ruby>`.
   - ĐẶC BIỆT: Trong Mondai 1 (KANJI_READING), chữ Hán được gạch chân / hỏi cách đọc TUYỆT ĐỐI KHÔNG ĐƯỢC CÓ FURIGANA / THẺ <ruby>! Hãy biểu diễn bằng `【漢字】` hoặc `<u>漢字</u>`.

3. QUY TẮC ĐỊNH DẠNG THEO TỪNG MONDAI:
   - KANJI_READING: mondai_code="KANJI_READING", chữ Hán hỏi đặt trong `【 】` không có furigana, 4 options là Hiragana.
   - ORTHOGRAPHY: mondai_code="ORTHOGRAPHY", từ Hiragana/Katakana gạch chân đặt trong `【 】`, 4 options là Kanji.
   - CONTEXTUAL_USE: mondai_code="CONTEXTUAL_USE", câu hỏi có chỗ trống `（　　）`.
   - SYNONYMS: mondai_code="SYNONYMS", câu hỏi có từ gạch chân `【...】`, 4 options là 4 câu có nghĩa tương đương.
   - PROPER_USAGE: mondai_code="PROPER_USAGE", câu hỏi là `【Từ_vựng】`, 4 options là 4 câu có gạch chân `<u>Từ_vựng</u>`.
   - SENTENTIAL_GRAMMAR: câu hỏi đơn có chỗ trống `（　　）`.
   - SENTENCE_COMPOSITION: câu hỏi sắp xếp có dấu sao `＿＿＿　＿＿＿　＿★＿　＿＿＿`, có `star_position`.
   - TEXT_GRAMMAR: bài đọc đục lỗ có `【 21 】`, `【 22 】`... CỰC KỲ QUAN TRỌNG: Đề thi thường gồm 3 phần:
     (1) Câu dẫn bối cảnh mở đầu ngoài khung (ví dụ: `日本で べんきょうして いる 学生が 「すきな 店」の ぶんしょうを 書いて、クラスの みんなの 前で 読みました。`),
     (2) Khung bài văn 1 `(1) ケンさんの ぶんしょう` (chứa các câu 22, 23 - ĐÂY LÀ PHẦN THI CHÍNH THỨC, KHÔNG PHẢI VÍ DỤ!), và
     (3) Khung bài văn 2 `(2) ミンジさんの ぶんしょう` (chứa các câu 24, 25, 26).
     BẮT BUỘC TRÍCH XUẤT 100% CẢ 3 PHẦN NÀY GHÉP NỐI NGUYÊN VẸN THEO THỨ TỰ VÀO `passage.content_jp` CỦA TẤT CẢ CÁC CÂU HỎI 22 ĐẾN 26!
     TUYỆT ĐỐI KHÔNG ĐƯỢC CHỈ TRÍCH XUẤT MỖI KHUNG THỨ 2 MÀ BỎ SÓT CÂU DẪN NGOÀI KHUNG HAY BÀI VĂN SỐ 1!
     `passage.title` BẮT BUỘC ĐẶT LÀ "文章の文法".
   - Với câu cuối phần Đọc hiểu (ví dụ Câu 32 của N5, Câu 34-35 của N4): ĐÂY LÀ MONDAI 6 (INFORMATION_RETRIEVAL - Đọc tờ rơi/áp phích), TUYỆT ĐỐI KHÔNG gán nhầm thành MEDIUM_PASSAGE!

4. BÀI ĐỌC HIỂU (SHORT_PASSAGE, MEDIUM_PASSAGE, LONG_PASSAGE, INFORMATION_RETRIEVAL):
   - Trích xuất 100% nội dung văn bản tiếng Nhật vào `passage.content_jp`.
   - `passage.title`: Đặt tên ngắn gọn (ví dụ "Bài đọc (1)", "Bài đọc hiểu"). TUYỆT ĐỐI KHÔNG chứa thẻ `<ruby>` và KHÔNG copy câu lệnh Mondai ("もんだい５..."). Nếu có câu dẫn tình huống (ví dụ "(3) 森さんの机の上に..."), hãy đưa vào ĐẦU của `passage.content_jp` và đặt tiêu đề là "Bài đọc (3)".
   - Với tờ rơi (INFORMATION_RETRIEVAL): chuyển đổi toàn bộ bảng biểu sang dạng bảng Markdown (`| 項目 | 内容 |`), trích xuất đầy đủ tiêu đề, các khung chú ý và điều kiện lưu ý.
   - Nếu câu hỏi Đọc hiểu có 4 phương án lựa chọn là TRANH VẼ / SƠ ĐỒ (ví dụ tranh các căn phòng "今のへやはどれですか"):
     gán `has_visual_options: true`, `has_visual: true`, `image_box`: [ymin, xmin, ymax, xmax], options: 1, 2, 3, 4 kèm `has_image: true`.

## Trả về CHÍNH XÁC JSON theo format sau (không kèm markdown fences):
{{
  "questions": [
    {{
      "raw_question_number": 1,
      "question_number": 1,
      "source_page": 3,
      "mondai_code": "KANJI_READING",
      "question_text": "きのう【夕方】、雨が ふりました。",
      "star_position": null,
      "has_visual": false,
      "image_box": null,
      "explanation_vi": "",
      "passage": null,
      "options": [
        {{"option_key": 1, "option_text": "ゆうがた", "is_correct": false}},
        {{"option_key": 2, "option_text": "あさ", "is_correct": false}},
        {{"option_key": 3, "option_text": "ひる", "is_correct": false}},
        {{"option_key": 4, "option_text": "よる", "is_correct": false}}
      ]
    }}
  ],
  "passages": []
}}
"""

ANSWER_SHEET_PROMPT = """Bạn là chuyên gia số hóa đề thi JLPT.
Trước mặt bạn là hình ảnh/trang PDF bảng đáp án chính thức (正答表 - Seitouhyou) của kỳ thi năng lực tiếng Nhật JLPT cấp {jlpt_level}.

Hãy đọc kỹ từng bảng đáp án và trích xuất TOÀN BỘ đáp án cho từng câu hỏi theo định dạng JSON chuẩn xác 100%.

## Quy tắc đọc bảng đáp án (正答表):
1. Phân loại theo các phần thi chính theo tiêu đề in trên bảng:
   - "VOCABULARY": 言語知識（文字・語彙）
   - "GRAMMAR_READING": 言語知識（文法）・読解 (hoặc "KNOWLEDGE_READING" đối với N1/N2)
   - "LISTENING": 聴解

2. CỰC KỲ QUAN TRỌNG VỀ PHẦN THI NGHE (聴解):
   - BẮT BUỘC BỎ QUA CỘT VÍ DỤ "例" (Rei): Ở mỗi bảng 問題 1, 問題 2, 問題 3, 問題 4, cột đầu tiên luôn là câu ví dụ mẫu "例" kèm đáp án (thường là 3 hoặc 2).
     TUYỆT ĐỐI BỎ QUA cột "例" và đáp án của cột "例"! Chỉ đọc từ cột số 1 trở đi!
   - Vì mỗi bảng 問題 trong phần Nghe đều bắt đầu đánh số lại từ 1, hãy trả về nhóm theo từng Mondai:
     "mondai_1": {{"1": ..., "2": ...}},
     "mondai_2": {{"1": ..., "2": ...}},
     "mondai_3": {{"1": ..., "2": ...}},
     "mondai_4": {{"1": ..., "2": ...}}

3. Về phần Từ vựng và Ngữ pháp & Đọc hiểu:
   - Các câu hỏi được đánh số liên tục (ví dụ 1..33 cho Từ vựng N5, 1..32 cho Ngữ pháp/Đọc hiểu N5).
   - Trả về mapping: "số câu": đáp_án (1, 2, 3 hoặc 4).

## Trả về DUY NHẤT mã JSON theo định dạng sau (không kèm markdown fences):
{{
  "sections": {{
    "VOCABULARY": {{
      "1": 2, "2": 4, "3": 1
    }},
    "GRAMMAR_READING": {{
      "1": 2, "2": 3, "3": 2
    }},
    "LISTENING": {{
      "mondai_1": {{"1": 2, "2": 2, "3": 3, "4": 3, "5": 1, "6": 4, "7": 4}},
      "mondai_2": {{"1": 4, "2": 3, "3": 2, "4": 4, "5": 1, "6": 3}},
      "mondai_3": {{"1": 1, "2": 3, "3": 2, "4": 3, "5": 2}},
      "mondai_4": {{"1": 1, "2": 1, "3": 2, "4": 3, "5": 3, "6": 1}}
    }}
  }}
}}
"""

def _build_audio_extraction_prompt(jlpt_level: str) -> str:
    level = (jlpt_level or "N5").upper()
    if level == "N4":
        mondai_rules = """3. CẤU TRÚC ĐẶC THÙ BẮT BUỘC CỦA ĐỀ THI NGHE JLPT N4 (TỔNG CỘNG ĐÚNG 28 CÂU HỎI CHÍNH THỨC):
   - Đề thi N4 BẮT BUỘC gồm 4 Mondai theo đúng thứ tự, TỔNG CỘNG 28 CÂU HỎI:

   ★ MONDAI 1 (TASK_BASED - 課題理解): ĐÚNG 8 CÂU HỎI (1番 đến 8番)
     - Câu 8 là câu CUỐI CÙNG của Mondai 1.
     - KHI HẾT CÂU 8, BẮT BUỘC ĐÓNG CÂU 8 LẠI!
     - TUYỆT ĐỐI KHÔNG ĐƯỢC GHÉP nội dung của Mondai 2 hay bất kỳ lời thoại nào sau câu 8 vào transcript của câu 8!

   ★ MONDAI 2 (KEY_POINT - ポイント理解): ĐÚNG 7 CÂU HỎI (1番 đến 7番)
     - Bắt đầu khi nghe xướng "問題2" (もんだいに).
     - Bỏ qua lời dẫn và câu ví dụ "例" (Rei).
     - BẮT BUỘC trích xuất ĐẦY ĐỦ 7 câu hỏi riêng biệt:
       + Câu 1 (1番): bắt đầu bằng câu hỏi (ví dụ câu hỏi đọc trước: "男の人と女の人が話しています..."), sau đó có khoảng 15-20 GIÂY YÊN LẶNG để thí sinh đọc đề trên giấy, rồi đến đoạn hội thoại. ĐÂY LÀ 1 CÂU RIÊNG BIỆT!
       + Câu 2 (2番)
       + Câu 3 (3番)
       + Câu 4 (4番)
       + Câu 5 (5番)
       + Câu 6 (6番)
       + Câu 7 (7番)
     - TUYỆT ĐỐI KHÔNG ĐƯỢC BỎ QUA HOẶC NUỐT CÁC CÂU CỦA MONDAI 2 VÀO CÂU KHÁC!

   ★ MONDAI 3 (UTTERANCE_EXPRESSIONS - 発話表現): ĐÚNG 5 CÂU HỎI (1番 đến 5番)
     - Bắt đầu khi nghe xướng "問題3" (もんだいさん).
     - Bỏ qua câu ví dụ "例".
     - Trích xuất đúng 5 câu: từ 1番 đến 5番 (mỗi câu có 3 đáp án 1, 2, 3).

   ★ MONDAI 4 (QUICK_RESPONSE - 即時応答): ĐÚNG 8 CÂU HỎI (1番 đến 8番)
     - Bắt đầu khi nghe xướng "問題4" (もんだいよん).
     - CẢNH BÁO QUAN TRỌNG: Đầu Mondai 4 LUÔN CÓ 1 CÂU VÍ DỤ MẪU ("例" / れい) kéo dài khoảng 20 giây (người nói: "...一番いいものは2番です... では始めます").
     - BẠN BẮT BUỘC PHẢI BỎ QUA HOÀN TOÀN CÂU VÍ DỤ NÀY!
     - CHỈ BẮT ĐẦU CÂU 1 khi nghe người đọc xướng "1番" (いちばん) SAU chữ "では始めます"!
     - Trích xuất đúng 8 câu hỏi chính thức: từ 1番 đến 8番 (mỗi câu có 3 đáp án 1, 2, 3)."""
    elif level == "N5":
        mondai_rules = """3. CẤU TRÚC ĐẶC THÙ BẮT BUỘC CỦA ĐỀ THI NGHE JLPT N5 (TỔNG CỘNG ĐÚNG 24 CÂU HỎI CHÍNH THỨC):
   - Đề thi N5 BẮT BUỘC gồm 4 Mondai:
   ★ MONDAI 1 (TASK_BASED): ĐÚNG 7 CÂU HỎI (1番 đến 7番).
   ★ MONDAI 2 (KEY_POINT): ĐÚNG 6 CÂU HỎI (1番 đến 6番).
   ★ MONDAI 3 (UTTERANCE_EXPRESSIONS): ĐÚNG 5 CÂU HỎI (1番 đến 5番, 3 đáp án).
   ★ MONDAI 4 (QUICK_RESPONSE): ĐÚNG 6 CÂU HỎI (1番 đến 6番, bỏ qua câu ví dụ mẫu '例' ở đầu)."""
    elif level == "N3":
        mondai_rules = """3. CẤU TRÚC ĐẶC THÙ CỦA ĐỀ THI NGHE JLPT N3 (GỒM 5 MONDAI, TỔNG CỘNG 28 CÂU):
   - TASK_BASED (問題1): 6 câu (1番 đến 6番)
   - KEY_POINT (問題2): 6 câu (1番 đến 6番)
   - GENERAL_OUTLINE (問題3): 3 câu (1番 đến 3番 - Đề không in gì)
   - UTTERANCE_EXPRESSIONS (問題4): 4 câu (1番 đến 4番 - Tranh mũi tên, 3 đáp án)
   - QUICK_RESPONSE (問題5): 9 câu (1番 đến 9番 - 3 đáp án, bỏ qua câu ví dụ '例')"""
    else:  # N1, N2
        mondai_rules = f"""3. CẤU TRÚC ĐẶC THÙ CỦA ĐỀ THI NGHE JLPT {level} (GỒM 5 MONDAI):
   - TASK_BASED (問題1): Hiểu nhiệm vụ (課題理解)
   - KEY_POINT (問題2): Hiểu điểm mấu chốt (ポイント理解)
   - GENERAL_OUTLINE (問題3): Hiểu khái quát (概要理解)
   - QUICK_RESPONSE (問題4): Phản xạ nhanh (即時応答 - 3 đáp án, bỏ qua câu ví dụ '例')
   - INTEGRATED_LISTENING (問題5): Nghe hiểu tích hợp (統合理解)"""

    return f"""Bạn là chuyên gia phân tích đề thi nghe JLPT cấp {level}.

Hãy nghe file audio bài nghe JLPT này và trích xuất thông tin cho TỪNG câu hỏi nghe.

## Quy tắc:
1. Xác định mốc thời gian bắt đầu (audio_start_time) và kết thúc (audio_end_time) của từng câu hỏi (BẮT BUỘC TÍNH BẰNG GIÂY DẠNG SỐ NGUYÊN ĐƠN THUẦN):
   - VÍ DỤ: 2 phút 46 giây = 166 (KHÔNG VIẾT 246 hay 02:46), 11 phút 10 giây = 670 (KHÔNG VIẾT 1110), 32 phút 31 giây = 1951 (KHÔNG VIẾT 3231).
   - Với câu hỏi Phản xạ nhanh (Mondai 4 - QUICK_RESPONSE): Thời lượng cả câu (audio_end_time - audio_start_time) khoảng 20-25 GIÂY (bao gồm cả lúc đọc câu hỏi và đọc xong 3 phương án lựa chọn).
   - Với các Mondai 1, 2, 3: Thời lượng mỗi câu thông thường từ 30 đến 75 giây.
2. TUYỆT ĐỐI KHÔNG trích xuất các câu ví dụ mẫu ("例" - Rei) và phần hướng dẫn đầu mỗi Mondai!
   - Chỉ trích xuất các câu hỏi THỰC TẾ của bài thi (sau khi nghe "1番", "2番"...).
   - Nếu câu nào có cụm từ "一番いいものは○番です" hoặc "練習しましょう" hoặc "例" (Rei), ĐÓ LÀ CÂU VÍ DỤ MẪU, PHẢI BỎ QUA HOÀN TOÀN, KHÔNG ĐƯỢC ĐƯA VÀO KẾT QUẢ!
3. Viết lại transcript tiếng Nhật đầy đủ cho từng câu nghe.
{mondai_rules}
4. Phân biệt rõ từng Mondai dựa trên lời hướng dẫn (指示文) trong audio.
5. QUY TẮC BẮT BUỘC VỀ PHƯƠNG ÁN LỰA CHỌN (OPTIONS):
   - ĐẶC THÙ JLPT: Trong Mondai 1 (TASK_BASED) và Mondai 2 (KEY_POINT), các phương án lựa chọn ĐƯỢC IN TRÊN GIẤY (BOOKLET), HOÀN TOÀN KHÔNG ĐƯỢC ĐỌC TRONG AUDIO!
   - Do đó, với Mondai 1 và Mondai 2: BẮT BUỘC để options là:
     [
       {{"option_key": 1, "option_text": "選択肢 1", "is_correct": false}},
       {{"option_key": 2, "option_text": "選択肢 2", "is_correct": false}},
       {{"option_key": 3, "option_text": "選択肢 3", "is_correct": false}},
       {{"option_key": 4, "option_text": "選択肢 4", "is_correct": false}}
     ]
     TUYỆT ĐỐI KHÔNG TỰ BỊA RA PHƯƠNG ÁN CHỮ HÁN từ ngữ cảnh audio! Các phương án in trên giấy sẽ được lấy từ file PDF.
   - Với Mondai 3 và Mondai 4: Người đọc có đọc 3 phương án 1, 2, 3 bằng lời trong audio, hãy ghi lại transcript của 3 phương án này (hoặc "選択肢 1", "選択肢 2", "選択肢 3").

## Trả về CHÍNH XÁC JSON theo format sau (không kèm markdown fences):
{{
  "questions": [
    {{
      "question_number": 1,
      "mondai_code": "TASK_BASED",
      "question_text": "男の人と女の人が話しています。男の人はこれから何をしますか。",
      "audio_start_time": 45,
      "audio_end_time": 110,
      "transcript_jp": "男：すみません、図書館はどこですか。\\n女：あそこの白い建物の2階ですよ。\\n男：ありがとうございます。",
      "explanation_vi": "Giải thích ngắn gọn bằng tiếng Việt",
      "options": [
        {{"option_key": 1, "option_text": "選択肢 1", "is_correct": false}},
        {{"option_key": 2, "option_text": "選択肢 2", "is_correct": false}},
        {{"option_key": 3, "option_text": "選択肢 3", "is_correct": false}},
        {{"option_key": 4, "option_text": "選択肢 4", "is_correct": false}}
      ]
    }}
  ]
}}

Lưu ý quan trọng:
- BỎ QUA HOÀN TOÀN TẤT CẢ CÂU VÍ DỤ ("例" / "練習")!
- Nếu đáp án chỉ được đọc trong audio (không in trên đề), vẫn phải viết ra option_text.
- Nếu bạn không thể xác định chính xác đáp án đúng, hãy đánh dấu đáp án hợp lý nhất.
- Mốc thời gian phải chính xác trong phạm vi ±5 giây.
"""


class GeminiExamParser:
    """
    Trích xuất đề thi JLPT bằng Gemini Vision (PDF) + Gemini Audio.
    Sử dụng google-generativeai SDK.
    """

    def __init__(self, api_key: Optional[str] = None):
        self.api_key = api_key or os.environ.get("GEMINI_API_KEY", "")
        if not self.api_key:
            raise ValueError(
                "Cần cung cấp GEMINI_API_KEY qua biến môi trường hoặc tham số khởi tạo. "
                "Lấy API key miễn phí tại: https://aistudio.google.com/app/apikey"
            )

        try:
            from google import genai
            self._client = genai.Client(api_key=self.api_key)
            self._genai = genai
        except ImportError:
            raise ImportError(
                "Cần cài thư viện google-genai: pip install google-genai"
            )

        self._model_name = os.environ.get("GEMINI_MODEL", "gemini-3.5-flash")
        self._fallback_models = ["gemini-flash-latest", "gemini-3.5-flash-lite", "gemini-2.5-flash", "gemini-3.8-flash", "gemini-2.5-pro"]
        self._last_request_time = 0.0
        self._min_request_interval = 2.0  # Giãn cách tối thiểu giữa các API call (an toàn cho 15 RPM)
        logger.info(f"✅ GeminiExamParser khởi tạo thành công, model: {self._model_name} (fallbacks: {self._fallback_models})")

    # ================================================================
    # MODULE 4: CACHE TRÍCH XUẤT ĐỀ THI
    # ================================================================
    @staticmethod
    def _get_cache_path(pdf_bytes: bytes, jlpt_level: str) -> Path:
        CACHE_DIR.mkdir(parents=True, exist_ok=True)
        pdf_hash = hashlib.sha256(pdf_bytes).hexdigest()
        return CACHE_DIR / f"{pdf_hash}_{jlpt_level}.json"

    @classmethod
    def _load_from_cache(cls, pdf_bytes: bytes, jlpt_level: str) -> Optional[Dict[str, Any]]:
        cache_file = cls._get_cache_path(pdf_bytes, jlpt_level)
        if cache_file.exists():
            try:
                with open(cache_file, "r", encoding="utf-8") as f:
                    data = json.load(f)
                    q_count = len(data.get("questions") or [])
                    t_pages = data.get("total_pages", 0)
                    if t_pages >= 5 and q_count < 15:
                        logger.warning(
                            f"⚠️ [CACHE INVALID] Cache {cache_file.name} chỉ có {q_count} câu hỏi cho {t_pages} trang "
                            f"(nghi ngờ trích xuất bị ngắt quãng hoặc thiếu). Bỏ qua cache để trích xuất lại!"
                        )
                        return None
                    logger.info(f"⚡ [CACHE HIT] Đã lấy kết quả trích xuất ({q_count} câu hỏi) từ cache ({cache_file.name})!")
                    return data
            except Exception as e:
                logger.warning(f"⚠️ Lỗi đọc cache {cache_file.name}: {e}")
        return None

    @classmethod
    def _save_to_cache(cls, pdf_bytes: bytes, jlpt_level: str, data: Dict[str, Any]):
        cache_file = cls._get_cache_path(pdf_bytes, jlpt_level)
        try:
            # Chỉ serialize các trường JSON hợp lệ (bỏ rendered_pages_map chứa raw bytes)
            cacheable_data = {
                "questions": data.get("questions", []),
                "images": data.get("images", []),
                "total_pages": data.get("total_pages", 0),
                "parsed_pages": data.get("parsed_pages", 0),
                "empty_or_failed_pages": data.get("empty_or_failed_pages", []),
            }
            with open(cache_file, "w", encoding="utf-8") as f:
                json.dump(cacheable_data, f, ensure_ascii=False, indent=2)
            logger.info(f"💾 [CACHE SAVE] Đã lưu kết quả trích xuất vào cache ({cache_file.name})")
        except Exception as e:
            logger.warning(f"⚠️ Không thể lưu cache {cache_file.name}: {e}")

    # ================================================================
    # MODULE 1: PRE-FILTER TRANG DƯ THỪA (BÌA, HƯỚNG DẪN, NHÁP, HOA VĂN XÁM)
    # ================================================================
    @staticmethod
    def _is_cover_or_instruction_page(page: Any, text: str) -> Tuple[bool, str]:
        """Nhận diện trang bìa và trang hướng dẫn/chú ý thi (Notes)."""
        if not text:
            return False, ""
        text_clean = text.lower()
        cover_signatures = [
            "examinee registration number",
            "do not open this question booklet",
            "do not take this question booklet",
            "this question booklet has",
            "one of the row numbers",
            "じゅけんばんごう", "受験番号",
            "ちゅうい", "注意事項", "注意",
            "しけんが はじまるまで", "試験が始まるまで",
            "もんだいようしを あけないで", "問題用紙を開けないで",
            "もって かえる ことは できません", "持って帰ることはできません",
            "かきこんではいけません", "メモをとってもいいです",
        ]
        matches = [sig for sig in cover_signatures if sig in text_clean]
        if len(matches) >= 2:
            # Đảm bảo không bỏ nhầm trang đề thật có câu hỏi (問題 1, 問題 2, ...)
            has_exam_mondai = bool(re.search(r'問題\s*[1-9１-９]\s*[\(（]', text)) or bool(re.search(r'問\s*[1-9１-９]\s*[\(（]', text))
            if not has_exam_mondai:
                return True, f"Trang bìa/hướng dẫn thi (khớp từ khóa: {', '.join(matches[:2])})"
        return False, ""

    @staticmethod
    def _is_blank_or_texture_page(page: Any, image_bytes: bytes) -> Tuple[bool, str]:
        """Nhận diện trang trắng hoặc trang hoa văn nháp/caro xám không có chữ."""
        try:
            text = page.get_text("text") or ""
            stripped_text = re.sub(r'[\s\d\-_/|・•\(\)\[\]]', '', text)
            if len(stripped_text) <= 5:
                img = Image.open(io.BytesIO(image_bytes)).convert("L")
                arr = np.array(img)
                # Tính tỉ lệ điểm ảnh nét mực in đậm (< 80)
                dark_ratio = float((arr < 80).sum()) / float(arr.size)
                if dark_ratio < 0.0025:
                    return True, f"Trang trắng / hoa văn nháp (tỉ lệ mực in {dark_ratio*100:.3f}% < 0.25%)"
        except Exception as e:
            logger.debug(f"Lỗi kiểm tra _is_blank_or_texture_page: {e}")
        return False, ""

    @classmethod
    def _should_skip_page(cls, page: Any, image_bytes: bytes) -> Tuple[bool, str]:
        """Tổng hợp kiểm tra tiền lọc để bỏ qua trang rác mà không tốn request AI."""
        text = page.get_text("text") or ""
        is_cover, reason = cls._is_cover_or_instruction_page(page, text)
        if is_cover:
            return True, reason
        is_blank, b_reason = cls._is_blank_or_texture_page(page, image_bytes)
        if is_blank:
            return True, b_reason
        return False, ""

    # ================================================================
    # MODULE 2: BATCHING CÓ OVERLAP & DEDUPLICATION
    # ================================================================
    @staticmethod
    def _create_overlapping_batches(
        items: List[Any],
        batch_size: int = 3,
        overlap: int = 1
    ) -> List[List[Any]]:
        """
        Gom trang thành các batch có gối đầu (overlap) để giữ trọn vẹn câu hỏi ở ranh giới trang.
        """
        if not items:
            return []
        if len(items) <= batch_size:
            return [items]

        batches = []
        step = max(1, batch_size - overlap)
        i = 0
        while i < len(items):
            batch = items[i : i + batch_size]
            batches.append(batch)
            if i + batch_size >= len(items):
                break
            i += step

        return batches

    @staticmethod
    def _merge_batch_questions(
        all_batch_results: List[Dict[str, Any]]
    ) -> Tuple[List[Dict[str, Any]], List[Dict[str, Any]]]:
        """Gộp kết quả từ các batch và khử trùng lặp theo (mondai_code, raw_question_number)."""
        merged_questions: Dict[Tuple[str, int], Dict[str, Any]] = {}
        merged_passages: List[Dict[str, Any]] = []

        for res in all_batch_results:
            if not res:
                continue

            passages = res.get("passages", [])
            if passages:
                merged_passages.extend(passages)

            questions = res.get("questions", [])
            for q in questions:
                m_code = q.get("mondai_code") or "UNKNOWN"
                rn = q.get("raw_question_number") or q.get("question_number")
                try:
                    rn_int = int(rn)
                except (ValueError, TypeError):
                    rn_int = None

                if rn_int is not None:
                    key = (m_code, rn_int)
                    if key not in merged_questions:
                        merged_questions[key] = q
                    else:
                        existing_q = merged_questions[key]
                        existing_opts = len([o for o in (existing_q.get("options") or []) if (o.get("option_text") or "").strip()])
                        new_opts = len([o for o in (q.get("options") or []) if (o.get("option_text") or "").strip()])
                        existing_text_len = len((existing_q.get("question_text") or "").strip())
                        new_text_len = len((q.get("question_text") or "").strip())

                        # Bảo tồn passage nếu một trong hai bản ghi có passage
                        ex_p = existing_q.get("passage")
                        new_p = q.get("passage")
                        chosen_p = None
                        if isinstance(ex_p, dict) and isinstance(new_p, dict):
                            ex_p_len = len(ex_p.get("content_jp", "") or "")
                            new_p_len = len(new_p.get("content_jp", "") or "")
                            chosen_p = new_p if new_p_len >= ex_p_len else ex_p
                        else:
                            chosen_p = new_p if isinstance(new_p, dict) else (ex_p if isinstance(ex_p, dict) else None)

                        if (new_opts > existing_opts) or (new_opts == existing_opts and new_text_len > existing_text_len):
                            merged_questions[key] = q
                        if chosen_p:
                            merged_questions[key]["passage"] = chosen_p
                else:
                    temp_key = (m_code, len(merged_questions) + 10000)
                    merged_questions[temp_key] = q

        def _safe_sort_key(x):
            raw = x.get("raw_question_number") or x.get("question_number") or 999
            try:
                return int(raw)
            except (ValueError, TypeError):
                return 999

        sorted_questions = sorted(
            merged_questions.values(),
            key=_safe_sort_key
        )
        return sorted_questions, merged_passages

    def _generate_with_fallback(self, contents: Any, config: Any = None) -> Any:
        """Thực hiện gọi Gemini API với cơ chế tự động chuyển đổi model và xử lý Rate Limit (429)."""
        import time
        # Module 3: Adaptive Rate Limiter - pacing giãn cách tối thiểu giữa các request
        now = time.time()
        elapsed = now - getattr(self, "_last_request_time", 0.0)
        min_interval = getattr(self, "_min_request_interval", 2.0)
        if elapsed < min_interval:
            time.sleep(min_interval - elapsed)
        self._last_request_time = time.time()

        candidate_models = [self._model_name] + [m for m in self._fallback_models if m != self._model_name]
        last_error = None
        for model in candidate_models:
            for attempt in range(2):
                try:
                    logger.info(f"Đang gửi request tới Gemini model: {model} (thử {attempt + 1})...")
                    response = self._client.models.generate_content(
                        model=model,
                        contents=contents,
                        config=config,
                    )
                    return response
                except Exception as e:
                    err_str = str(e)
                    is_rate_limit = "429" in err_str or "RESOURCE_EXHAUSTED" in err_str or "quota" in err_str.lower()
                    last_error = e

                    is_daily_quota = "per day" in err_str.lower() or "limit exceeded" in err_str.lower()
                    if is_daily_quota:
                        logger.warning(f"⚠️ Model {model} đã dùng hết quota ngày (Daily Quota Exceeded), chuyển ngay sang model tiếp theo...")
                        break

                    if is_rate_limit:
                        wait_sec = 8.0
                        retry_match = re.search(r'retry.*?(\d+)(?:\.\d+)?s', err_str, re.IGNORECASE)
                        if retry_match:
                            retry_val = float(retry_match.group(1))
                            if retry_val > 60:
                                logger.warning(f"⚠️ Model {model} yêu cầu chờ {retry_val}s, chuyển ngay sang model tiếp theo...")
                                break
                            wait_sec = min(retry_val + 1.5, 30.0)
                        logger.warning(
                            f"⚠️ Model {model} chạm giới hạn tốc độ (429 Rate Limit). "
                            f"Tạm dừng {wait_sec:.1f}s trước khi tiếp tục..."
                        )
                        time.sleep(wait_sec)
                    else:
                        logger.warning(f"⚠️ Model {model} gặp lỗi ({e}), chuyển sang model tiếp theo...")
                        break

        raise last_error or RuntimeError("Không thể gọi bất kỳ Gemini model nào.")

    @staticmethod
    def detect_jlpt_level(pdf_bytes: bytes, filename: Optional[str] = None) -> Optional[str]:
        """Tự động nhận diện cấp độ JLPT (N1 - N5) từ tên file và nội dung PDF."""
        # 1. Từ tên file
        if filename:
            m = re.search(r'(?i)(?:^|[^a-zA-Z0-9])(N[1-5])(?:[^a-zA-Z0-9]|$)', filename)
            if m:
                return m.group(1).upper()
            m2 = re.search(r'(?i)(N[1-5])', filename)
            if m2:
                return m2.group(1).upper()

        # 2. Từ text trang 1 và 2 trong PDF
        try:
            import fitz
            doc = fitz.open(stream=pdf_bytes, filetype="pdf")
            sample_text = ""
            for p_idx in range(min(2, len(doc))):
                sample_text += " " + doc[p_idx].get_text("text")

            norm_text = sample_text.replace("Ｎ", "N").replace("１", "1").replace("２", "2").replace("３", "3").replace("４", "4").replace("５", "5")
            level_match = re.search(r'\b(N[1-5])\b', norm_text, re.IGNORECASE)
            if level_match:
                return level_match.group(1).upper()

            m_jlpt = re.search(r'(?:JLPT|日本語能力試験).*?(N[1-5])', norm_text, re.IGNORECASE)
            if m_jlpt:
                return m_jlpt.group(1).upper()
        except Exception as e:
            logger.debug(f"detect_jlpt_level error: {e}")

        return None

    def extract_listening_booklet_text(
        self,
        pdf_bytes: bytes,
        jlpt_level: str = "N4",
    ) -> Dict[Tuple[str, int], List[str]]:
        """
        Bóc tách tự động và linh hoạt các lựa chọn bằng chữ (text options) từ tờ đề thi nghe PDF (聴解問題用紙).
        Chỉ trích xuất các câu có lựa chọn in chữ (ví dụ Mondai 1 các câu chữ, Mondai 2), không tạo câu hỏi mới.
        """
        results: Dict[Tuple[str, int], List[str]] = {}
        try:
            import fitz
            page_parts = []
            with fitz.open(stream=pdf_bytes, filetype="pdf") as doc:
                if len(doc) == 0:
                    return results

                # Render các trang đề thi có thể chứa lựa chọn in (thường từ trang 3 đến trang 13)
                scan_pages = range(min(2, len(doc)), len(doc))
                for p_no in scan_pages:
                    pix = doc[p_no].get_pixmap(dpi=150)
                    img_data = pix.tobytes("png")
                    page_parts.append(
                        self._genai.types.Part.from_bytes(data=img_data, mime_type="image/png")
                    )

            if not page_parts:
                return results

            prompt = f"""Bạn là chuyên gia phân tích đề thi tiếng Nhật JLPT cấp {jlpt_level}.
Đây là các trang trong tờ đề thi nghe (聴解問題用紙).
Hãy tìm tất cả các câu hỏi có các lựa chọn in sẵn trên giấy (1, 2, 3, 4).

LƯU Ý CỰC KỲ QUAN TRỌNG:
1. Đọc CHÍNH XÁC NGUYÊN VĂN từng chữ được in trên đề giấy:
   - Nếu câu có tranh nhưng 4 phương án bên dưới in bằng ký hiệu Katakana hoặc chữ cái (ví dụ 'ア　イ', '1', 'A'...) thì BẮT BUỘC giữ nguyên ký hiệu in trên giấy, TUYỆT ĐỐI KHÔNG bỏ qua và TUYỆT ĐỐI KHÔNG tự ý mô tả tranh vẽ hay thay bằng chữ khác!
   - Nếu đề in bằng Hiragana thì BẮT BUỘC giữ nguyên Hiragana nguyên bản theo đúng trình độ đề thi, TUYỆT ĐỐI KHÔNG tự ý chuyển đổi thành chữ Hán (Kanji)!
   - Nếu chữ Hán có Furigana nhỏ ở trên, giữ nguyên hoặc dùng thẻ `<ruby>漢字<rt>furigana</rt></ruby>`.
2. Bỏ qua các câu mà 4 phương án hoàn toàn là 4 hình vẽ (không có chữ in đáp án). Chỉ lấy các câu có phương án bằng chữ hoặc ký hiệu in sẵn.
3. PHÂN ĐỊNH CHÍNH XÁC TỪNG MONDAI THEO TIÊU ĐỀ IN TRÊN ĐỀ:
   - Ở 問題1 (TASK_BASED): Chỉ trích xuất các câu có phương án bằng chữ in trên giấy (ví dụ câu 2: 1かい 2かい... và câu 7: 1ばん 3ばん...). Gán mondai_code: 'TASK_BASED', raw_question_number tương ứng (ví dụ 2 hoặc 7).
   - Ở 問題2 (KEY_POINT): Bắt đầu từ trang có tiêu đề 問題2 / もんだい2.
     + TUYỆT ĐỐI BỎ QUA câu ví dụ mẫu "例" (Rei / Example) ở đầu 問題2! Không được lấy câu "例"!
     + Chỉ lấy các câu hỏi chính thức từ 1番 đến câu cuối của 問題2 với mondai_code: 'KEY_POINT' và raw_question_number: 1, 2, 3, 4, 5, 6...
   - Loại bỏ số thứ tự phương án ở đầu (ví dụ "1 1かい" thì chỉ lấy "1かい", "1 としょかん" thì chỉ lấy "としょかん").

Trả về JSON duy nhất theo format schema mẫu:
{{
  "booklet_options": [
    {{
      "mondai_code": "TASK_BASED",
      "raw_question_number": 2,
      "options": ["1かい", "2かい", "3かい", "4かい"]
    }},
    {{
      "mondai_code": "KEY_POINT",
      "raw_question_number": 1,
      "options": ["1じかん", "2じかん", "3じかん", "4じかん"]
    }}
  ]
}}"""

            config = self._genai.types.GenerateContentConfig(
                response_mime_type="application/json"
            )
            response = self._generate_with_fallback(
                contents=[prompt] + page_parts,
                config=config,
            )
            if response and response.text:
                raw_text = response.text.strip()
                raw_text = re.sub(r'^```(?:json)?\s*', '', raw_text)
                raw_text = re.sub(r'\s*```$', '', raw_text)
                data = json.loads(raw_text)
                items = data.get("booklet_options", [])
                for it in items:
                    m = it.get("mondai_code")
                    rn = it.get("raw_question_number")
                    opts = it.get("options")
                    if m and rn is not None and isinstance(opts, list) and len(opts) == 4:
                        rn_match = re.search(r'\d+', str(rn))
                        if not rn_match:
                            continue
                        clean_opts = []
                        for o in opts:
                            txt = str(o).strip()
                            txt = re.sub(r'^[1-4①-④A-Da-d][\.\s、:\-]\s*', '', txt)
                            clean_opts.append(txt)
                        results[(m, int(rn_match.group(0)))] = clean_opts
                logger.info(f"✅ [GEMINI_BOOKLET] Đã bóc tách tự động {len(results)} câu hỏi có text options từ PDF nghe!")
        except Exception as e:
            logger.warning(f"⚠️ [GEMINI_BOOKLET] Không thể trích xuất tự động text options từ PDF ({e}). Giữ options mặc định.")

        return results

    # ================================================================
    # PUBLIC API: PARSE PDF
    # ================================================================
    def parse_pdf(
        self,
        pdf_bytes: bytes,
        jlpt_level: str = "N3",
        extracted_images_dir: Optional[str] = None,
        use_cache: bool = True,
    ) -> Dict[str, Any]:
        """
        Parse toàn bộ file PDF đề thi thành JSON câu hỏi chuẩn.
        Hỗ trợ cache kết quả trích xuất, tiền lọc trang rác và batching gối đầu chống mất ngữ cảnh.

        Returns:
            {
                "questions": [...],
                "images": [...],
                "total_pages": int,
                "parsed_pages": int
            }
        """
        # 0. Kiểm tra Cache trước khi gọi AI API
        if use_cache:
            cached_result = self._load_from_cache(pdf_bytes, jlpt_level)
            if cached_result:
                cached_result["rendered_pages_map"] = {}
                return cached_result

        try:
            import fitz  # PyMuPDF
        except ImportError:
            raise ImportError("Cần cài PyMuPDF: pip install pymupdf")

        doc = fitz.open(stream=pdf_bytes, filetype="pdf")
        total_pages = len(doc)
        logger.info(f"📄 PDF có {total_pages} trang, bắt đầu parse với Gemini Vision (Batching + Pre-filter)...")

        all_questions: List[Dict[str, Any]] = []
        all_passages: List[Dict[str, Any]] = []
        all_images: List[Dict[str, Any]] = []
        parsed_pages = 0

        # Bóc tách ảnh nhúng (giữ nguyên logic cũ)
        if extracted_images_dir:
            all_images = self._extract_embedded_images(doc, extracted_images_dir)

        # 1. Render và Tiền lọc trang dư thừa (bìa, hướng dẫn, nháp, hoa văn xám)
        rendered_pages_map = {}
        valid_pages = []
        for page_idx in range(total_pages):
            page = doc[page_idx]
            page_num = page_idx + 1
            image_bytes = self._render_page_to_png(page, dpi=180)
            rendered_pages_map[page_num] = image_bytes

            should_skip, skip_reason = self._should_skip_page(page, image_bytes)
            if should_skip:
                logger.info(f"  ⏭️ [PRE-FILTER] Bỏ qua trang {page_num}/{total_pages}: {skip_reason}")
                continue

            valid_pages.append((page_num, image_bytes))

        # Nếu lọc hết sạch (trường hợp biên hy hữu), fallback giữ lại toàn bộ trang
        if not valid_pages:
            logger.warning("⚠️ Không tìm thấy trang hợp lệ sau khi lọc, sử dụng toàn bộ các trang.")
            valid_pages = [(p_idx + 1, rendered_pages_map[p_idx + 1]) for p_idx in range(total_pages)]

        # 2. Gom trang thành các Batch có Overlap 1 trang để chống mất ngữ cảnh câu hỏi nằm giữa ranh giới
        batches = self._create_overlapping_batches(valid_pages, batch_size=3, overlap=1)
        logger.info(
            f"📦 [BATCHING] Đã lọc còn {len(valid_pages)}/{total_pages} trang thực tế. "
            f"Gom thành {len(batches)} batch (batch_size=3, overlap=1) để gửi Gemini Vision..."
        )

        batch_results = []
        empty_or_failed_pages = []
        for b_idx, batch in enumerate(batches):
            pages_desc = [p_num for p_num, _ in batch]
            logger.info(f"  🚀 [BATCH {b_idx + 1}/{len(batches)}] Đang gửi trang {pages_desc} tới Gemini Vision...")
            res = self._call_gemini_vision_batch(batch, jlpt_level)
            if res:
                batch_results.append(res)
                q_count = len(res.get("questions") or [])
                logger.info(f"  ✅ [BATCH {b_idx + 1}/{len(batches)}] Trang {pages_desc}: trích xuất được {q_count} câu hỏi")
            else:
                logger.warning(f"  ⚠️ [BATCH {b_idx + 1}/{len(batches)}] Không nhận được kết quả từ trang {pages_desc}.")
                empty_or_failed_pages.extend(pages_desc)

        # 3. Gộp câu hỏi từ các batch và khử trùng lặp (deduplicate) thông minh theo (mondai_code, raw_question_number)
        all_questions, all_passages = self._merge_batch_questions(batch_results)
        parsed_pages = len(valid_pages)
        logger.info(f"📊 [TỔNG KẾT BATCH] Đã gom và khử trùng lặp thành công {len(all_questions)} câu hỏi từ {len(batches)} batch!")

        # Gộp passage vào câu hỏi tương ứng
        all_questions = self._merge_passages_into_questions(all_questions, all_passages)

        # Chuẩn hóa mondai_code và dọn dẹp các trường passage/image cho câu hỏi
        for idx, q in enumerate(all_questions):
            if "raw_question_number" not in q:
                q["raw_question_number"] = q.get("question_number") or (idx + 1)
            q["options"] = self._normalize_options(q.get("options", []))
            if q.get("mondai_code") not in VALID_MONDAI_CODES:
                q["mondai_code"] = self._infer_mondai_code(q)

            # Cấp độ N5 / N4 không có bài đọc dài LONG_PASSAGE: tự động chuyển về MEDIUM_PASSAGE
            if jlpt_level in ("N5", "N4") and q.get("mondai_code") == "LONG_PASSAGE":
                q["mondai_code"] = "MEDIUM_PASSAGE"

            # Các câu hỏi từ vựng và ngữ pháp câu TUYỆT ĐỐI không được có passage hay image_url
            if q.get("mondai_code") in {
                "KANJI_READING", "ORTHOGRAPHY", "CONTEXTUAL_USE", "SYNONYMS",
                "PROPER_USAGE", "SENTENTIAL_GRAMMAR", "SENTENCE_COMPOSITION"
            }:
                q["passage"] = None
                q["image_url"] = None
                q["has_visual"] = False

            # TẤT CẢ các bài đọc hiểu (kể cả INFORMATION_RETRIEVAL - tờ rơi, bảng thông báo) TUYỆT ĐỐI không gán ảnh cắt/render
            # Toàn bộ nội dung, bảng biểu, quy định được trích xuất 100% bằng văn bản định dạng đẹp
            if q.get("mondai_code") in {
                "TEXT_GRAMMAR", "SHORT_PASSAGE", "MEDIUM_PASSAGE", "LONG_PASSAGE", "INFORMATION_RETRIEVAL"
            }:
                if q.get("passage"):
                    q["passage"]["image_url"] = None
                    q["passage"]["passage_type"] = "TEXT"
                # Chỉ tắt visual nếu câu hỏi KHÔNG có lựa chọn dạng hình ảnh
                if not q.get("has_visual_options"):
                    q["image_url"] = None
                    q["has_visual"] = False

            self._clean_question_text(q)

        # Tự động crop hoặc gán ảnh minh họa cho các câu hỏi Nghe hiểu (LISTENING) có tranh vẽ
        if extracted_images_dir:
            logger.info(f"🖼️ [IMAGE] Tổng số ảnh nhúng trích xuất từ PDF: {len(all_images)}")
            for q in all_questions:
                m_code = q.get("mondai_code")
                is_listening_q = m_code in {
                    "TASK_BASED", "KEY_POINT", "GENERAL_OUTLINE",
                    "UTTERANCE_EXPRESSIONS", "QUICK_RESPONSE", "INTEGRATED_LISTENING"
                }
                is_text_only = m_code in {"KEY_POINT", "QUICK_RESPONSE"}

                if is_text_only:
                    q["image_url"] = None
                    q["has_visual"] = False
                    q["has_visual_options"] = False
                    q["image_box"] = None
                    for o in (q.get("options") or []):
                        o["option_image_url"] = None
                    continue

                has_visual_flag = q.get("has_visual") or q.get("has_visual_options") or m_code in {"TASK_BASED", "UTTERANCE_EXPRESSIONS"}
                if (is_listening_q or q.get("has_visual_options")) and has_visual_flag:
                    p_num = q.get("source_page")
                    q_id = q.get("question_number") or q.get("raw_question_number") or 1

                    # 1. Nếu Gemini trả về tọa độ khung ảnh (image_box: [ymin, xmin, ymax, xmax])
                    box = q.get("image_box")
                    if box and isinstance(box, (list, tuple)) and len(box) == 4 and p_num in rendered_pages_map:
                        try:
                            ymin, xmin, ymax, xmax = box
                            ymin = max(0, min(1000, int(ymin)))
                            xmin = max(0, min(1000, int(xmin)))
                            ymax = max(0, min(1000, int(ymax)))
                            xmax = max(0, min(1000, int(xmax)))
                            if ymax > ymin + 20 and xmax > xmin + 20:
                                pil_page = Image.open(io.BytesIO(rendered_pages_map[p_num]))
                                pw, ph = pil_page.size
                                left = max(0, int(xmin * pw / 1000) - 4)
                                top = max(0, int(ymin * ph / 1000) - 4)
                                right = min(pw, int(xmax * pw / 1000) + 4)
                                bottom = min(ph, int(ymax * ph / 1000) + 4)
                                cropped = pil_page.crop((left, top, right, bottom))
                                c_filename = f"visual_page_{p_num}_q_{q_id}.png"
                                c_path = os.path.join(extracted_images_dir, c_filename)
                                cropped.save(c_path, "PNG")
                                q["image_url"] = f"http://localhost:8000/static/extracted_images/{c_filename}"
                                q["has_visual"] = True
                                logger.info(f"🖼️ [LISTENING] Đã crop tranh câu {q_id} từ trang {p_num} bằng image_box: {q['image_url']}")
                        except Exception as e:
                            logger.warning(f"Lỗi crop tranh câu hỏi trang {p_num}: {e}")

                    # 2. Fallback: TUYỆT ĐỐI KHÔNG gán ảnh toàn trang A4 làm ảnh câu hỏi.
                    if not q.get("image_url"):
                        q["image_url"] = None
                        q["has_visual"] = False

        # Phân biệt chính xác SHORT_PASSAGE (1 câu/bài) và MEDIUM_PASSAGE (>= 2 câu/bài)
        # Đếm số lượng câu hỏi dùng chung từng đoạn văn
        passage_usage_count: Dict[str, List[Dict[str, Any]]] = {}
        for q in all_questions:
            if q.get("passage") and q.get("passage", {}).get("content_jp"):
                p_text = q["passage"]["content_jp"].strip()
                passage_usage_count.setdefault(p_text, []).append(q)

        for p_text, qs in passage_usage_count.items():
            if len(qs) >= 2:
                for q in qs:
                    if q.get("mondai_code") in {"SHORT_PASSAGE", "LONG_PASSAGE"}:
                        q["mondai_code"] = "MEDIUM_PASSAGE"

        # Khử trùng lặp và sắp xếp lại theo trật tự Mondai chuẩn JLPT
        all_questions = self.deduplicate_and_sort_questions(all_questions, jlpt_level=jlpt_level)

        logger.info(
            f"📊 Tổng kết PDF: {parsed_pages}/{total_pages} trang parse thành công, "
            f"{len(all_questions)} câu hỏi, {len(all_images)} ảnh nhúng/tờ rơi"
        )

        final_result = {
            "questions": all_questions,
            "images": all_images,
            "total_pages": total_pages,
            "parsed_pages": parsed_pages,
            "empty_or_failed_pages": empty_or_failed_pages,
            "rendered_pages_map": rendered_pages_map,
        }
        if use_cache and all_questions:
            self._save_to_cache(pdf_bytes, jlpt_level, final_result)

        return final_result

    # ================================================================
    # PUBLIC API: PARSE AUDIO
    # ================================================================
    def parse_audio(
        self,
        audio_file_path: str,
        jlpt_level: str = "N3",
    ) -> Dict[str, Any]:
        """
        Parse file audio bài nghe JLPT thành JSON câu hỏi nghe chuẩn.

        Returns:
            {
                "questions": [...],
                "duration_seconds": int
            }
        """
        from google.genai import types as genai_types

        logger.info(f"🎧 Bắt đầu parse audio: {audio_file_path}")

        # Xác định MIME type
        ext = os.path.splitext(audio_file_path)[1].lower()
        mime_map = {
            ".mp3": "audio/mpeg",
            ".wav": "audio/wav",
            ".ogg": "audio/ogg",
            ".m4a": "audio/mp4",
            ".flac": "audio/flac",
        }
        mime_type = mime_map.get(ext, "audio/mpeg")

        # Upload file lên Gemini File API
        logger.info("  📤 Đang upload audio lên Gemini File API...")
        uploaded_file = self._client.files.upload(
            file=audio_file_path,
            config=genai_types.UploadFileConfig(mime_type=mime_type),
        )
        logger.info(f"  ✅ Upload thành công: {uploaded_file.name}")

        # Chờ file xử lý xong
        import time
        while uploaded_file.state.name == "PROCESSING":
            logger.info("  ⏳ Đang chờ Gemini xử lý file audio...")
            time.sleep(3)
            uploaded_file = self._client.files.get(name=uploaded_file.name)

        if uploaded_file.state.name == "FAILED":
            raise RuntimeError(f"Gemini không thể xử lý file audio: {uploaded_file.state}")

        # Gọi Gemini multimodal với file audio
        prompt = _build_audio_extraction_prompt(jlpt_level)

        try:
            response = self._generate_with_fallback(
                contents=[
                    genai_types.Content(
                        parts=[
                            genai_types.Part.from_uri(
                                file_uri=uploaded_file.uri,
                                mime_type=mime_type,
                            ),
                            genai_types.Part.from_text(text=prompt),
                        ]
                    )
                ],
                config=genai_types.GenerateContentConfig(
                    temperature=0.1,
                    max_output_tokens=8192,
                ),
            )
        except Exception as e:
            logger.error(f"  ❌ Lỗi gọi Gemini Audio: {e}")
            # Cleanup uploaded file
            try:
                self._client.files.delete(name=uploaded_file.name)
            except Exception:
                pass
            raise

        # Cleanup uploaded file
        try:
            self._client.files.delete(name=uploaded_file.name)
        except Exception:
            pass

        # Parse response
        result = self._parse_gemini_json_response(response.text)
        raw_questions = result.get("questions", []) if isinstance(result, dict) else (result if isinstance(result, list) else [])
        # Lọc bỏ triệt để mọi câu ví dụ mẫu ("例" - Rei) nếu Gemini vô tình trả về
        clean_questions = []
        for q in raw_questions:
            txt = (str(q.get("question_text", "")) + " " + str(q.get("transcript_jp", "")) + " " + str(q.get("explanation_vi", ""))).lower()
            if any(k in txt for k in ["一番いいものは", "いちばんいいものは", "練習しましょう", "では練習", "例題"]) or (q.get("question_text", "").strip() in ["例", "れい", "例（れい）"]):
                logger.info(f"🗑️ Loại bỏ câu ví dụ mẫu trong Gemini Audio: {txt[:60]}...")
                continue
            clean_questions.append(q)
        questions = clean_questions

        # Lấy tổng thời lượng file audio để hỗ trợ nhận diện và quy đổi timestamp
        total_audio_sec = 0.0
        try:
            import av
            with av.open(audio_file_path) as av_c:
                if av_c.streams.audio:
                    in_s = av_c.streams.audio[0]
                    if in_s.duration and in_s.time_base:
                        total_audio_sec = float(in_s.duration * in_s.time_base)
        except Exception:
            pass

        def _to_pure_seconds(val: Any) -> int:
            if val is None:
                return 0
            if isinstance(val, str):
                val = val.strip()
                if ":" in val:
                    parts = val.split(":")
                    if len(parts) == 2:
                        return int(float(parts[0]) * 60 + float(parts[1]))
                    elif len(parts) == 3:
                        return int(float(parts[0]) * 3600 + float(parts[1]) * 60 + float(parts[2]))
            try:
                num = float(val)
            except Exception:
                return 0

            int_val = int(num)
            # Phát hiện trường hợp Gemini ghi phút giây gộp (ví dụ 1110 cho 11:10 = 670s, 3231 cho 32:31 = 1951s)
            mm = int_val // 100
            ss = int_val % 100
            if ss < 60 and mm > 0:
                candidate = mm * 60 + ss
                if total_audio_sec > 0:
                    if int_val > total_audio_sec or (int_val > 150 and candidate <= total_audio_sec):
                        return candidate
                elif int_val > 150:
                    return candidate
            return int_val

        # Validate và chuẩn hóa
        for idx, q in enumerate(questions):
            q["question_number"] = idx + 1
            q["options"] = self._normalize_options(q.get("options", []))
            if q.get("mondai_code") not in VALID_MONDAI_CODES:
                q["mondai_code"] = "TASK_BASED"
            if (jlpt_level or "N5").upper() in ("N5", "N4") and q.get("mondai_code") == "GENERAL_OUTLINE":
                q["mondai_code"] = "UTTERANCE_EXPRESSIONS"

            st = max(0, _to_pure_seconds(q.get("audio_start_time")))
            et = _to_pure_seconds(q.get("audio_end_time"))
            m_code = q.get("mondai_code")

            # Xử lý thời lượng câu hỏi phù hợp từng dạng bài
            if m_code == "QUICK_RESPONSE":
                # Phản xạ nhanh: thông thường 22 - 28s để nghe cả câu hỏi và 3 lựa chọn
                if et <= st or et - st < 12:
                    et = st + 24
                elif et - st > 40:
                    et = st + 28
            else:
                if et <= st or et - st < 15:
                    et = st + 55
                elif et - st > 110:
                    et = st + 75

            q["audio_start_time"] = st
            q["audio_end_time"] = et

        # Đảm bảo không chồng lấn mốc với câu tiếp theo
        for idx in range(len(questions) - 1):
            curr_q = questions[idx]
            next_q = questions[idx + 1]
            if next_q["audio_start_time"] > curr_q["audio_start_time"]:
                curr_q["audio_end_time"] = min(curr_q["audio_end_time"], next_q["audio_start_time"] - 2)

        logger.info(f"🎧 Audio parse xong: {len(questions)} câu hỏi nghe (đã chuẩn hóa timestamp thành giây thuần túy)")
        return {"questions": questions}

        # ================================================================
    # PUBLIC API: PARSE ANSWER SHEET (正答表)
    # ================================================================
    @classmethod
    def normalize_answer_sheet_data(
        cls,
        answer_data: Dict[str, Any],
        jlpt_level: str = "N5"
    ) -> Dict[str, Any]:
        """
        Chuẩn hóa kết quả trích xuất từ bảng đáp án chính thức (正答表):
        - Áp dụng cấu trúc JLPT_STRUCTURE_SPEC[jlpt_level] để chuẩn hóa phần Nghe (LISTENING)
          từ dạng {mondai_1: {1: 2}, mondai_2: {1: 4}} thành số thứ tự liên tục 1..N.
        - Tự động nhận diện tên section (cả tiếng Anh lẫn tiếng Nhật: 聴解, 文字・語彙, 文法・読解).
        - Loại bỏ cột câu ví dụ '例' nếu có.
        - Sinh flat_answers chuẩn xác cho từng phần thi.
        """
        from .jlpt_rules import JLPT_STRUCTURE_SPEC
        lvl = (jlpt_level or "N5").upper()
        sections = answer_data.get("sections", {})
        normalized_sections: Dict[str, Dict[str, int]] = {}

        specs_by_session = JLPT_STRUCTURE_SPEC.get(lvl, {})

        def _canonical_sec_key(name: str) -> str:
            n = name.upper()
            if "LISTEN" in n or "CHOUKAI" in n or "聴解" in name:
                return "LISTENING"
            if "VOCAB" in n or "MOJI" in n or "GOI" in n or "文字" in name or "語彙" in name:
                return "VOCABULARY"
            if "GRAMMAR" in n or "BUNPOU" in n or "DOKKAI" in n or "文法" in name or "読解" in name:
                return "GRAMMAR_READING"
            if "KNOWLEDGE" in n or "知識" in name:
                return "KNOWLEDGE_READING"
            return n

        for sec_name, sec_data in sections.items():
            if not isinstance(sec_data, dict):
                continue
            sec_key = _canonical_sec_key(sec_name)

            if sec_key == "LISTENING":
                norm_listening: Dict[str, int] = {}
                listening_specs = specs_by_session.get("LISTENING", [])

                has_mondai_structure = any(
                    isinstance(v, dict) or re.search(r'mondai[_\s]*\d+|問題[_\s]*\d+|^\d+_\d+$', str(k), re.IGNORECASE)
                    for k, v in sec_data.items()
                )

                if has_mondai_structure:
                    mondai_groups: Dict[int, List[Tuple[Any, Any]]] = {}
                    for k, v in sec_data.items():
                        if isinstance(v, dict):
                            m_nums = re.findall(r'\d+', str(k))
                            if m_nums:
                                m_idx = int(m_nums[0])
                                for raw_q, ans in v.items():
                                    mondai_groups.setdefault(m_idx, []).append((raw_q, ans))
                        else:
                            nums = re.findall(r'\d+', str(k))
                            if len(nums) >= 2:
                                m_idx = int(nums[0])
                                raw_q = int(nums[1])
                                mondai_groups.setdefault(m_idx, []).append((raw_q, v))

                    prev_m_idx = None
                    curr_q = 1
                    for m_idx in sorted(mondai_groups.keys()):
                        spec = next((s for s in listening_specs if s.mondai_number == m_idx), None)
                        if prev_m_idx is None:
                            if spec:
                                curr_q = spec.typical_range[0]
                        elif m_idx > prev_m_idx + 1:
                            if spec and curr_q < spec.typical_range[0]:
                                curr_q = spec.typical_range[0]
                        prev_m_idx = m_idx

                        q_list = mondai_groups[m_idx]
                        filtered_q = [
                            (rq, ans) for (rq, ans) in q_list
                            if "例" not in str(rq) and str(rq).strip().lower() not in ("rei", "example", "ex", "0")
                        ]
                        def _sort_key(item):
                            digits = re.findall(r'\d+', str(item[0]))
                            return [int(d) for d in digits] if digits else [0]
                        filtered_q.sort(key=_sort_key)

                        for rq, ans in filtered_q:
                            try:
                                norm_listening[str(curr_q)] = int(ans)
                                curr_q += 1
                            except (ValueError, TypeError):
                                pass
                else:
                    int_items = []
                    for k, v in sec_data.items():
                        if "例" in str(k) or str(k).strip().lower() in ("rei", "example", "ex", "0"):
                            continue
                        try:
                            int_items.append((int(k), int(v)))
                        except (ValueError, TypeError):
                            pass
                    int_items.sort(key=lambda x: x[0])

                    min_k = int_items[0][0] if int_items else 1
                    max_listening_q = listening_specs[-1].typical_range[1] if listening_specs else 24
                    offset = (min_k - 1) if min_k > max_listening_q else 0

                    for ik, iv in int_items:
                        cont_q = ik - offset
                        norm_listening[str(cont_q)] = iv

                normalized_sections["LISTENING"] = norm_listening

            else:
                norm_sec: Dict[str, int] = {}
                for k, v in sec_data.items():
                    if "例" in str(k) or str(k).strip().lower() in ("rei", "example", "ex", "0"):
                        continue
                    num_match = re.search(r'\d+', str(k))
                    if num_match:
                        try:
                            norm_sec[str(int(num_match.group(0)))] = int(v)
                        except (ValueError, TypeError):
                            pass
                normalized_sections[sec_key] = norm_sec

        combined_flat: Dict[str, int] = {}
        for s_map in normalized_sections.values():
            combined_flat.update(s_map)

        return {
            "sections": normalized_sections,
            "flat_answers": combined_flat,
        }

    def parse_answer_sheet(
        self,
        file_bytes: bytes,
        filename: str = "",
        jlpt_level: str = "N3",
    ) -> Dict[str, Any]:
        """
        Trích xuất bảng đáp án chính thức (正答表 - Seitouhyou) từ file ảnh hoặc PDF.
        Trả về mapping {sections: {...}, flat_answers: {...}}
        """
        from google.genai import types as genai_types
        import fitz

        images_to_process = []
        is_pdf = filename.lower().endswith(".pdf") or file_bytes.startswith(b"%PDF")

        if is_pdf:
            try:
                doc = fitz.open(stream=file_bytes, filetype="pdf")
                for page in doc:
                    img_bytes = self._render_page_to_png(page, dpi=200)
                    images_to_process.append(img_bytes)
                doc.close()
            except Exception as e:
                logger.error(f"Lỗi mở PDF bảng đáp án: {e}")
                return {"sections": {}, "flat_answers": {}}
        else:
            images_to_process.append(file_bytes)

        combined_sections: Dict[str, Dict[str, int]] = {}
        combined_flat: Dict[str, int] = {}

        for idx, img_b in enumerate(images_to_process):
            prompt = ANSWER_SHEET_PROMPT.format(jlpt_level=jlpt_level)
            try:
                response = self._generate_with_fallback(
                    contents=[
                        genai_types.Content(
                            parts=[
                                genai_types.Part.from_bytes(
                                    data=img_b,
                                    mime_type="image/png",
                                ),
                                genai_types.Part.from_text(text=prompt),
                            ]
                        )
                    ],
                    config=genai_types.GenerateContentConfig(
                        temperature=0.1,
                        max_output_tokens=4096,
                    ),
                )
                res = self._parse_gemini_json_response(response.text)
                secs = res.get("sections", {})
                for s_key, s_map in secs.items():
                    combined_sections.setdefault(s_key, {}).update(s_map)
                flats = res.get("flat_answers", {})
                combined_flat.update(flats)
            except Exception as e:
                logger.error(f"Lỗi gọi Gemini parse bảng đáp án trang {idx + 1}: {e}")

        raw_res = {
            "sections": combined_sections,
            "flat_answers": combined_flat,
        }
        normalized = self.normalize_answer_sheet_data(raw_res, jlpt_level)
        logger.info(f"📊 Đã parse & chuẩn hóa bảng đáp án 正答表: {list(normalized['sections'].keys())}, {len(normalized['flat_answers'])} flat answers")
        return normalized

    @classmethod
    def deduplicate_and_sort_questions(
        cls,
        questions: List[Dict[str, Any]],
        jlpt_level: str = "N5",
        section_type: Optional[str] = None,
    ) -> List[Dict[str, Any]]:
        """
        1. Khử trùng lặp câu hỏi (đặc biệt bài đục lỗ TEXT_GRAMMAR và câu hỏi bị scan 2 lần ở 2 trang liền kề).
        2. Chuẩn hóa câu hỏi theo đúng thứ tự Mondai chính thức của kỳ thi JLPT.
        3. Đánh lại số thứ tự question_number chuẩn xác từ 1 đến N.
        """
        if not questions:
            return []

        import re

        JLPT_MONDAI_ORDER = {
            # Từ vựng (Vocabulary / Moji-Goi)
            "KANJI_READING": 10,
            "ORTHOGRAPHY": 20,
            "WORD_FORMATION": 25,
            "CONTEXTUAL_USE": 30,
            "SYNONYMS": 40,
            "PROPER_USAGE": 50,

            # Ngữ pháp (Grammar / Bunpou)
            "SENTENTIAL_GRAMMAR": 110,
            "SENTENCE_COMPOSITION": 120,
            "TEXT_GRAMMAR": 130,

            # Đọc hiểu (Reading / Dokkai)
            "SHORT_PASSAGE": 210,
            "MEDIUM_PASSAGE": 220,
            "LONG_PASSAGE": 230,
            "INTEGRATED_COMPREHENSION": 240,
            "THEMATIC_COMPREHENSION": 250,
            "INFORMATION_RETRIEVAL": 260,

            # Nghe hiểu (Listening / Choukai)
            "TASK_BASED": 310,
            "KEY_POINT": 320,
            "GENERAL_OUTLINE": 330,
            "UTTERANCE_EXPRESSIONS": 340,
            "QUICK_RESPONSE": 350,
            "INTEGRATED_LISTENING": 360,
        }

        # Điều chỉnh thứ tự Mondai cho N4 / N5 nghe hiểu:
        # N4/N5 chỉ có 4 Mondai: Mondai 1 (TASK_BASED) -> Mondai 2 (KEY_POINT) -> Mondai 3 (UTTERANCE_EXPRESSIONS) -> Mondai 4 (QUICK_RESPONSE)
        if (jlpt_level or "").upper() in ("N5", "N4"):
            for q in questions:
                if q.get("mondai_code") == "GENERAL_OUTLINE":
                    q["mondai_code"] = "UTTERANCE_EXPRESSIONS"
            JLPT_MONDAI_ORDER["UTTERANCE_EXPRESSIONS"] = 330
            JLPT_MONDAI_ORDER["QUICK_RESPONSE"] = 340

        # Lưu lại raw_question_number trước khi đánh lại số
        for q in questions:
            if "raw_question_number" not in q:
                q["raw_question_number"] = q.get("question_number")

        # 1. Khử trùng lặp dạng bài đục lỗ (TEXT_GRAMMAR)
        cloze_by_blank: Dict[int, Dict[str, Any]] = {}
        non_cloze_questions: List[Dict[str, Any]] = []

        for q in questions:
            m_code = q.get("mondai_code", "")
            q_text = q.get("question_text", "")

            is_cloze = (m_code == "TEXT_GRAMMAR") or bool(
                re.search(r'^[【\[\(（]?\s*(\d{1,2})\s*[】\]\)）]?$', q_text.strip()) and q.get("passage")
            )

            if is_cloze:
                q["mondai_code"] = "TEXT_GRAMMAR"
                blank_num = None
                if q.get("raw_question_number"):
                    try:
                        blank_num = int(q["raw_question_number"])
                    except (ValueError, TypeError):
                        pass
                if blank_num is None and q.get("question_number"):
                    try:
                        blank_num = int(q["question_number"])
                    except (ValueError, TypeError):
                        pass
                if blank_num is None:
                    blank_match = re.search(r'[【\[\(（]?\s*(\d{1,2})\s*[】\]\)）]?', q_text)
                    if blank_match:
                        blank_num = int(blank_match.group(1))

                if blank_num is not None:
                    if blank_num not in cloze_by_blank:
                        cloze_by_blank[blank_num] = q
                    else:
                        existing = cloze_by_blank[blank_num]
                        ex_opts_filled = sum(1 for o in existing.get("options", []) if o.get("option_text"))
                        new_opts_filled = sum(1 for o in q.get("options", []) if o.get("option_text"))
                        if new_opts_filled > ex_opts_filled:
                            existing["options"] = q.get("options", existing.get("options", []))

                        ex_p = existing.get("passage")
                        new_p = q.get("passage")
                        if not ex_p and new_p:
                            existing["passage"] = new_p
                        elif ex_p and new_p:
                            ex_c = ex_p.get("content_jp", "") or ""
                            new_c = new_p.get("content_jp", "") or ""
                            if len(new_c) > len(ex_c):
                                existing["passage"] = new_p
                else:
                    non_cloze_questions.append(q)
            else:
                non_cloze_questions.append(q)

        # 2. Khử trùng lặp thông minh và an toàn cho các câu hỏi thông thường:
        # - Nếu câu hỏi có raw_question_number (số thứ tự in trên đề 1..35):
        #   Mỗi số câu hỏi trên đề chỉ tồn tại DUY NHẤT 1 câu!
        #   Nếu quét trùng 2 câu cùng raw_question_number (do trang quét chồng mép): giữ bản ghi có nhiều phương án đầy đủ hơn.
        #   ĐẶC BIỆT: Với LISTENING, mỗi Mondai đánh số độc lập từ 1..N, nên khóa trùng lặp là (mondai_code, rn).
        # - Nếu câu hỏi KHÔNG CÓ raw_question_number:
        #   Chỉ coi là trùng lặp nếu trùng cả question_text LẪN toàn bộ các phương án lựa chọn!
        deduped_by_raw_num: Dict[Any, Dict[str, Any]] = {}
        unnum_questions: List[Dict[str, Any]] = []

        for q in non_cloze_questions:
            raw_num = q.get("raw_question_number")
            if raw_num is not None:
                try:
                    rn = int(raw_num)
                    is_listening_q = (
                        section_type == "LISTENING"
                        or q.get("mondai_code") in {
                            "TASK_BASED", "KEY_POINT", "GENERAL_OUTLINE",
                            "UTTERANCE_EXPRESSIONS", "QUICK_RESPONSE", "INTEGRATED_LISTENING"
                        }
                    )
                    dedup_key = (q.get("mondai_code"), rn) if is_listening_q else rn
                    if dedup_key not in deduped_by_raw_num:
                        deduped_by_raw_num[dedup_key] = q
                    else:
                        existing = deduped_by_raw_num[dedup_key]
                        ex_opts_cnt = sum(1 for o in existing.get("options", []) if o.get("option_text"))
                        new_opts_cnt = sum(1 for o in q.get("options", []) if o.get("option_text"))
                        if new_opts_cnt > ex_opts_cnt or len(q.get("question_text", "")) > len(existing.get("question_text", "")):
                            deduped_by_raw_num[dedup_key] = q
                except (ValueError, TypeError):
                    unnum_questions.append(q)
            else:
                unnum_questions.append(q)

        # Lọc các câu không số: chỉ loại trừ nếu trùng cả text lẫn options
        seen_unnum = set()
        deduped_unnum = []
        for q in unnum_questions:
            clean_t = re.sub(r'[\s　\n\r]+', '', q.get("question_text", "")).strip()
            opts_sig = tuple((o.get("option_key"), o.get("option_text", "").strip()) for o in q.get("options", []))
            sig = (q.get("mondai_code", ""), clean_t, opts_sig)
            if sig not in seen_unnum:
                seen_unnum.add(sig)
                deduped_unnum.append(q)

        deduped_non_cloze = list(deduped_by_raw_num.values()) + deduped_unnum

        deduped_cloze = [cloze_by_blank[b] for b in sorted(cloze_by_blank.keys())]
        combined = deduped_non_cloze + deduped_cloze

        # 3. Sắp xếp toàn bộ câu hỏi theo đúng chuẩn Mondai của JLPT
        def _sort_key(item: Dict[str, Any]):
            m = item.get("mondai_code", "")
            order = JLPT_MONDAI_ORDER.get(m, 999)
            try:
                raw_num = int(item.get("raw_question_number") or 0)
            except (ValueError, TypeError):
                raw_num = 0
            if m == "TEXT_GRAMMAR":
                bm = re.search(r'(\d{1,2})', item.get("question_text", ""))
                if bm:
                    raw_num = int(bm.group(1))
            return (order, raw_num)

        combined.sort(key=_sort_key)

        # 4. Đánh số thứ tự thông minh:
        # Với LISTENING: mỗi Mondai mới bắt đầu lại từ 1 theo đúng chuẩn thi thật
        # Với các phần khác (VOCABULARY, GRAMMAR_READING): đánh số liên tục 1..N
        if section_type == "LISTENING":
            current_mondai = None
            mondai_q_num = 1
            for q in combined:
                m_code = q.get("mondai_code", "")
                if current_mondai is None or current_mondai != m_code:
                    current_mondai = m_code
                    mondai_q_num = 1
                q["question_number"] = mondai_q_num
                mondai_q_num += 1
        else:
            for idx, q in enumerate(combined):
                if q.get("raw_question_number") is not None:
                    try:
                        q["question_number"] = int(q["raw_question_number"])
                    except (ValueError, TypeError):
                        q["question_number"] = idx + 1
                else:
                    q["question_number"] = idx + 1

        for q in combined:
            if q.get("mondai_code") == "TEXT_GRAMMAR":
                q["question_text"] = f"【 {q['question_number']} 】"

        # 5. Đồng bộ hóa toàn diện bài đục lỗ (TEXT_GRAMMAR) và bài đọc xuyên trang
        combined = cls._inherit_passages_cross_questions(combined)

        cloze_in_final = [q for q in combined if q.get("mondai_code") == "TEXT_GRAMMAR"]
        if cloze_in_final:
            for q in cloze_in_final:
                if q.get("passage") and q["passage"].get("content_jp"):
                    q["passage"]["content_jp"] = cls._format_cloze_passage_blanks(
                        q["passage"]["content_jp"], cloze_in_final
                    )

        return combined

    @staticmethod
    def apply_answer_sheet_to_questions(
        questions: List[Dict[str, Any]],
        answer_data: Dict[str, Any],
        section_type: Optional[str] = None
    ) -> List[Dict[str, Any]]:
        """
        Khớp đáp án từ bảng 正答表 vào danh sách câu hỏi:
        - Ưu tiên map theo section_type (VOCABULARY / GRAMMAR_READING / LISTENING)
        - Fallback sang flat_answers
        - Với mỗi câu hỏi, tìm theo question_number:
          opt["is_correct"] = (opt["option_key"] == correct_opt)
        """
        if not questions or not answer_data:
            return questions

        sec_map = answer_data.get("sections", {})
        flat_map = answer_data.get("flat_answers", {})

        target_map = {}
        if section_type and section_type in sec_map:
            target_map = sec_map[section_type]
        elif len(sec_map) == 1:
            target_map = next(iter(sec_map.values()))
        else:
            target_map = flat_map

        int_target_map = {}
        for k, v in target_map.items():
            try:
                int_target_map[int(k)] = int(v)
            except (ValueError, TypeError):
                pass

        int_flat_map = {}
        for k, v in flat_map.items():
            try:
                int_flat_map[int(k)] = int(v)
            except (ValueError, TypeError):
                pass

        if section_type == "LISTENING":
            # Với phần nghe: Khớp theo cặp (Mondai, question_number)
            mondai_order_map = {}
            m_counter = 0
            for q in questions:
                mc = q.get("mondai_code", "")
                if mc not in mondai_order_map:
                    m_counter += 1
                    mondai_order_map[mc] = m_counter

            listen_data = sec_map.get("LISTENING", {}) or target_map
            matched_count = 0
            for q in questions:
                mc = q.get("mondai_code", "")
                m_num = mondai_order_map.get(mc, 1)
                qn_str = str(q.get("question_number") or 1)
                correct_opt = None

                m_group = listen_data.get(f"mondai_{m_num}") or listen_data.get(f"m{m_num}")
                if isinstance(m_group, dict) and qn_str in m_group:
                    correct_opt = m_group[qn_str]
                elif f"m{m_num}_{qn_str}" in flat_map:
                    correct_opt = flat_map[f"m{m_num}_{qn_str}"]
                elif f"mondai_{m_num}_{qn_str}" in flat_map:
                    correct_opt = flat_map[f"mondai_{m_num}_{qn_str}"]
                elif qn_str in target_map:
                    correct_opt = target_map[qn_str]

                if correct_opt is not None:
                    try:
                        c_int = int(correct_opt)
                        matched_count += 1
                        for opt in q.get("options", []):
                            opt["is_correct"] = (opt.get("option_key") == c_int)
                    except (ValueError, TypeError):
                        pass

            logger.info(f"🎯 Đã khớp đáp án chính thức phần Nghe cho {matched_count}/{len(questions)} câu hỏi")
            return questions

        matched_count = 0
        for q in questions:
            raw_num = q.get("raw_question_number")
            q_num = q.get("question_number")
            correct_opt = None

            # 1. Ưu tiên khớp theo số thứ tự câu hỏi liên tục của bài thi (question_number 1..N)
            if q_num is not None:
                try:
                    qn = int(q_num)
                    if qn in int_target_map:
                        correct_opt = int_target_map[qn]
                    elif qn in int_flat_map:
                        correct_opt = int_flat_map[qn]
                except (ValueError, TypeError):
                    pass

            # 2. Fallback sang raw_question_number CHỈ KHI không có question_number
            if correct_opt is None and raw_num is not None:
                try:
                    rn = int(raw_num)
                    if rn in int_target_map:
                        correct_opt = int_target_map[rn]
                    elif rn in int_flat_map:
                        correct_opt = int_flat_map[rn]
                except (ValueError, TypeError):
                    pass

            if correct_opt is not None:
                matched_count += 1
                for opt in q.get("options", []):
                    opt_k = opt.get("option_key")
                    opt["is_correct"] = (opt_k == correct_opt)

        logger.info(f"🎯 Đã khớp đáp án chính thức cho {matched_count}/{len(questions)} câu hỏi")
        return questions

    # ================================================================
    # PUBLIC API: PARSE PDF FOR SPECIFIC SECTION
    # ================================================================
    def parse_pdf_for_section(
        self,
        pdf_bytes: bytes,
        section_type: str,
        jlpt_level: str = "N3",
        extracted_images_dir: Optional[str] = None,
        filename: Optional[str] = None,
        use_cache: bool = True,
    ) -> Dict[str, Any]:
        """
        Parse PDF và lọc câu hỏi theo section_type cụ thể.
        """
        # Tự động nhận diện cấp độ nếu PDF hoặc filename chỉ rõ N1-N5
        detected_level = self.detect_jlpt_level(pdf_bytes, filename=filename)
        if detected_level and detected_level != jlpt_level:
            logger.info(
                f"💡 [AUTO-DETECT] Tự động phát hiện cấp độ JLPT từ file '{filename}': {detected_level} "
                f"(ghi đè tham số '{jlpt_level}')"
            )
            jlpt_level = detected_level

        full_result = self.parse_pdf(pdf_bytes, jlpt_level, extracted_images_dir, use_cache=use_cache)
        questions = full_result["questions"]

        # Mapping section_type → mondai_codes
        section_mondai_map = {
            "VOCABULARY": {
                "KANJI_READING", "ORTHOGRAPHY", "CONTEXTUAL_USE",
                "SYNONYMS", "PROPER_USAGE"
            },
            "GRAMMAR_READING": {
                "SENTENTIAL_GRAMMAR", "SENTENCE_COMPOSITION", "TEXT_GRAMMAR",
                "SHORT_PASSAGE", "MEDIUM_PASSAGE", "LONG_PASSAGE",
                "INFORMATION_RETRIEVAL"
            },
            "KNOWLEDGE_READING": {
                "KANJI_READING", "ORTHOGRAPHY", "CONTEXTUAL_USE",
                "SYNONYMS", "PROPER_USAGE",
                "SENTENTIAL_GRAMMAR", "SENTENCE_COMPOSITION", "TEXT_GRAMMAR",
                "SHORT_PASSAGE", "MEDIUM_PASSAGE", "LONG_PASSAGE",
                "INFORMATION_RETRIEVAL"
            },
            "LISTENING": {
                "TASK_BASED", "KEY_POINT", "GENERAL_OUTLINE",
                "UTTERANCE_EXPRESSIONS", "QUICK_RESPONSE"
            },
        }

        valid_codes = section_mondai_map.get(section_type, set())
        filtered = []
        default_fallback = {
            "VOCABULARY": "CONTEXTUAL_USE",
            "GRAMMAR_READING": "SENTENTIAL_GRAMMAR",
            "KNOWLEDGE_READING": "SENTENTIAL_GRAMMAR",
            "LISTENING": "TASK_BASED",
        }.get(section_type, "SENTENTIAL_GRAMMAR")

        for q in questions:
            q_copy = dict(q)
            m_code = q_copy.get("mondai_code")
            raw_num = q_copy.get("raw_question_number") or q_copy.get("question_number")

            # Xử lý đặc biệt cho phần TỪ VỰNG (VOCABULARY):
            if section_type == "VOCABULARY":
                q_text = q_copy.get("question_text", "").strip()
                is_just_number = bool(re.match(r'^[【\[\(（]?\s*\d+\s*[】\]\)）]?$', q_text))

                if (is_just_number or m_code == "TEXT_GRAMMAR") and q_copy.get("passage"):
                    p_content = q_copy["passage"].get("content_jp", "")
                    if raw_num and p_content:
                        pattern = rf'([^。\n]*?[【\[\(（]?\s*{raw_num}\s*[】\]\)）]?[^。\n]*[。]?)'
                        match_sentence = re.search(pattern, p_content)
                        if match_sentence:
                            rescued = match_sentence.group(1).strip()
                            rescued = re.sub(rf'[【\[\(（]?\s*{raw_num}\s*[】\]\)）]?', '（　　）', rescued)
                            q_copy["question_text"] = rescued

                # Phần Từ vựng TUYỆT ĐỐI không có passage hay image_url
                q_copy["passage"] = None
                q_copy["image_url"] = None

            # LUÔN NẮN MONDAI_CODE CHUẨN XÁC THEO SỐ CÂU HỎI VÀ ĐẶC TRƯNG JLPT (chỉ áp dụng cho VOCABULARY/GRAMMAR_READING nơi số câu liên tục 1..35)
            if section_type != "LISTENING":
                raw_n = None
                if raw_num is not None:
                    try:
                        raw_n = int(raw_num)
                    except (ValueError, TypeError):
                        pass

                q_copy["mondai_code"] = infer_mondai_from_spec(
                    raw_n,
                    q_copy.get("question_text", ""),
                    jlpt_level,
                    section_type,
                    q_copy.get("options"),
                    current_mondai_code=m_code,
                )
            else:
                # Với LISTENING: mỗi Mondai đều đánh số lại từ 1番, 2番...
                # Do đó tin tưởng mondai_code từ Gemini Vision, chỉ fallback nếu mã không hợp lệ
                if m_code not in valid_codes or not m_code:
                    q_copy["mondai_code"] = default_fallback

            self._clean_question_text(q_copy)
            filtered.append(q_copy)

        # Khử trùng lặp và sắp xếp câu hỏi chuẩn xác theo Mondai JLPT
        filtered = self.deduplicate_and_sort_questions(filtered, jlpt_level=jlpt_level, section_type=section_type)
        # Kế thừa bài đọc cho các câu hỏi cùng bài đọc (xuyên trang ví dụ câu 30~33 và tờ rơi)
        filtered = self._inherit_passages_cross_questions(filtered)

        # -------------------------------------------------------------
        # RÀ SOÁT CẤU TRÚC THEO CHUẨN JLPT (AUDIT & VERIFICATION)
        # -------------------------------------------------------------
        audit_result = audit_exam_section(
            filtered,
            jlpt_level=jlpt_level,
            section_type=section_type,
            total_pages=full_result.get("total_pages", 0),
            parsed_pages=full_result.get("parsed_pages", 0),
        )

        # TỰ ĐỘNG CỨU TRỢ NẾU THIẾU MONDAI (Ví dụ: mất Mondai 2 ORTHOGRAPHY do lỗi trang hoặc 429)
        empty_pages = full_result.get("empty_or_failed_pages", [])
        rendered_map = full_result.get("rendered_pages_map", {})
        if audit_result.get("missing_mondais") and empty_pages and rendered_map:
            candidate_pages = [p for p in empty_pages if p >= 2]
            if candidate_pages:
                logger.info(
                    f"🔄 [AUTO-RECOVERY] Phát hiện thiếu {len(audit_result['missing_mondais'])} Mondai! "
                    f"Tự động quét lại các trang nghi vấn {candidate_pages}..."
                )
                recovered_questions = []
                for p_num in candidate_pages:
                    p_bytes = rendered_map.get(p_num)
                    if p_bytes:
                        rec_res = self._call_gemini_vision_targeted(
                            p_bytes, jlpt_level, p_num, audit_result["missing_mondais"]
                        )
                        if rec_res and rec_res.get("questions"):
                            for rq in rec_res["questions"]:
                                rq["source_page"] = p_num
                                if "raw_question_number" not in rq:
                                    rq["raw_question_number"] = rq.get("question_number")
                                if section_type == "VOCABULARY":
                                    rq["passage"] = None
                                    rq["image_url"] = None
                                    raw_n = int(rq.get("raw_question_number") or 1)
                                    rq["mondai_code"] = infer_mondai_from_spec(
                                        raw_n,
                                        rq.get("question_text", ""),
                                        jlpt_level,
                                        "VOCABULARY",
                                        rq.get("options"),
                                        current_mondai_code=rq.get("mondai_code"),
                                    )
                            recovered_questions.extend(rec_res["questions"])
                            logger.info(f"  🎉 Cứu trợ thành công {len(rec_res['questions'])} câu hỏi từ trang {p_num}!")

                if recovered_questions:
                    filtered.extend(recovered_questions)
                    filtered = self.deduplicate_and_sort_questions(filtered, jlpt_level=jlpt_level, section_type=section_type)
                    filtered = self._inherit_passages_cross_questions(filtered)
                    audit_result = audit_exam_section(
                        filtered,
                        jlpt_level=jlpt_level,
                        section_type=section_type,
                        total_pages=full_result.get("total_pages", 0),
                        parsed_pages=full_result.get("parsed_pages", 0) + len(candidate_pages),
                    )

        # Phần thi Từ vựng VOCABULARY TUYỆT ĐỐI không có passage cấp section
        if section_type == "VOCABULARY":
            return {
                "questions": filtered,
                "passage": None,
                "images": full_result["images"],
                "audit": audit_result,
                "detected_jlpt_level": jlpt_level,
            }

        # Chỉ trả về passage cấp section nếu TOÀN BỘ câu hỏi trong section dùng chung DUY NHẤT 1 passage
        passage = None
        passages_in_sec = [
            q["passage"].get("content_jp", "").strip()
            for q in filtered
            if q.get("passage") and q["passage"].get("content_jp")
        ]
        unique_passages = set(passages_in_sec)
        if len(unique_passages) == 1 and len(passages_in_sec) == len(filtered):
            for q in filtered:
                if q.get("passage"):
                    passage = q["passage"]
                    break

        return {
            "questions": filtered,
            "passage": passage,
            "images": full_result["images"],
            "audit": audit_result,
            "detected_jlpt_level": jlpt_level,
        }

    # ================================================================
    # PRIVATE: Gemini Vision Call (Batch & Single)
    # ================================================================
    def _call_gemini_vision_batch(
        self,
        batch_pages: List[Tuple[int, bytes]],
        jlpt_level: str,
    ) -> Optional[Dict[str, Any]]:
        """Gửi một batch gồm 2-3 trang ảnh liên tiếp lên Gemini Vision và parse kết quả."""
        from google.genai import types as genai_types

        num_pages = len(batch_pages)
        pages_str = ", ".join(str(p[0]) for p in batch_pages)
        prompt = PDF_BATCH_EXTRACTION_PROMPT.format(
            jlpt_level=jlpt_level,
            num_pages=num_pages,
            pages_str=pages_str,
        )

        parts = []
        for page_num, img_b in batch_pages:
            parts.append(genai_types.Part.from_text(text=f"=== HÌNH ẢNH TRANG {page_num} ==="))
            parts.append(
                genai_types.Part.from_bytes(
                    data=img_b,
                    mime_type="image/png",
                )
            )
        parts.append(genai_types.Part.from_text(text=prompt))

        try:
            response = self._generate_with_fallback(
                contents=[genai_types.Content(parts=parts)],
                config=genai_types.GenerateContentConfig(
                    temperature=0.1,
                    max_output_tokens=8192,
                ),
            )
            res = self._parse_gemini_json_response(response.text)
            first_p = batch_pages[0][0] if batch_pages else 1
            if res:
                for q in res.get("questions") or []:
                    if not q.get("source_page"):
                        q["source_page"] = first_p
                for p in res.get("passages") or []:
                    if not p.get("source_page"):
                        p["source_page"] = first_p
            return res
        except Exception as e:
            logger.error(f"Lỗi Gemini Vision batch ({pages_str}): {e}")
            return None

    def _call_gemini_vision(
        self,
        image_bytes: bytes,
        jlpt_level: str,
        page_num: int,
    ) -> Optional[Dict[str, Any]]:
        """Gửi 1 trang ảnh lên Gemini Vision và parse kết quả."""
        from google.genai import types as genai_types

        prompt = PDF_EXTRACTION_PROMPT.format(
            jlpt_level=jlpt_level,
            page_num=page_num,
        )

        try:
            response = self._generate_with_fallback(
                contents=[
                    genai_types.Content(
                        parts=[
                            genai_types.Part.from_bytes(
                                data=image_bytes,
                                mime_type="image/png",
                            ),
                            genai_types.Part.from_text(text=prompt),
                        ]
                    )
                ],
                config=genai_types.GenerateContentConfig(
                    temperature=0.1,
                    max_output_tokens=8192,
                ),
            )

            return self._parse_gemini_json_response(response.text)

        except Exception as e:
            logger.error(f"Lỗi Gemini Vision trang {page_num}: {e}")
            return None

    def _call_gemini_vision_targeted(
        self,
        image_bytes: bytes,
        jlpt_level: str,
        page_num: int,
        missing_mondais: List[Dict[str, Any]],
    ) -> Optional[Dict[str, Any]]:
        """Gửi trang ảnh lên Gemini Vision với prompt tập trung vào các dạng bài (Mondai) đang bị thiếu sót."""
        from google.genai import types as genai_types

        missing_desc = "\n".join([
            f"- {m['title_jp']} ({m['title_vi']}): Dải câu dự kiến {m['expected_range']} (khoảng {m['typical_count']} câu)"
            for m in missing_mondais
        ])

        targeted_prompt = f"""Bạn là chuyên gia số hóa đề thi JLPT cấp {jlpt_level}.
Trang {page_num} của file PDF đề thi đang bị hệ thống nghi ngờ bỏ sót câu hỏi.
Cụ thể, đề thi đang THIẾU các dạng bài (Mondai) sau:
{missing_desc}

## NHIỆM VỤ ĐẶC BIỆT CỦA BẠN:
1. Hãy đọc từng dòng trên trang ảnh {page_num} và tìm kiếm TẤT CẢ các câu hỏi thuộc các Mondai trên.
2. ĐẶC BIỆT CHÚ Ý DẠNG ORTHOGRAPHY (表記 - CÁCH VIẾT CHỮ HÁN):
   - Đề bài có từ Hiragana gạch chân đặt trong ngoặc vuông: ví dụ `すずきさんは 【あおい】 シャツを きて います。`
   - 4 phương án lựa chọn là chữ Hán: 1. 青い 2. 黒い 3. 赤い 4. 白い.
   - Gán `mondai_code`: "ORTHOGRAPHY".
3. Trích xuất đầy đủ câu hỏi và 4 phương án (1, 2, 3, 4), mặc định `is_correct: false`.
4. Trả về đúng format JSON (không kèm markdown fences):
{{
  "page_number": {page_num},
  "questions": [
    {{
      "question_number": 10,
      "mondai_code": "ORTHOGRAPHY",
      "question_text": "すずきさんは 【あおい】 シャツを きて います。",
      "star_position": null,
      "has_visual": false,
      "explanation_vi": "",
      "passage": null,
      "options": [
        {{"option_key": 1, "option_text": "青い", "is_correct": false}},
        {{"option_key": 2, "option_text": "黒い", "is_correct": false}},
        {{"option_key": 3, "option_text": "赤い", "is_correct": false}},
        {{"option_key": 4, "option_text": "白い", "is_correct": false}}
      ]
    }}
  ]
}}
Nếu trang này thực sự không có câu hỏi nào (chỉ là bìa hoặc hướng dẫn), trả về: {{"page_number": {page_num}, "questions": []}}
"""
        try:
            response = self._generate_with_fallback(
                contents=[
                    genai_types.Content(
                        parts=[
                            genai_types.Part.from_bytes(
                                data=image_bytes,
                                mime_type="image/png",
                            ),
                            genai_types.Part.from_text(text=targeted_prompt),
                        ]
                    )
                ],
                config=genai_types.GenerateContentConfig(
                    temperature=0.1,
                    max_output_tokens=8192,
                ),
            )
            return self._parse_gemini_json_response(response.text)
        except Exception as e:
            logger.error(f"Lỗi Gemini Vision Targeted trang {page_num}: {e}")
            return None

    # ================================================================
    # PRIVATE: Parse Gemini JSON Response
    # ================================================================
    def _parse_gemini_json_response(self, text: str) -> Dict[str, Any]:
        """Parse JSON từ response text của Gemini (hỗ trợ tự sửa lỗi cú pháp và cứu trợ JSON bị cắt cụt)."""
        if not text:
            return {"questions": [], "passages": []}

        # Loại bỏ markdown code fences nếu có
        cleaned = text.strip()
        json_match = re.search(r'```(?:json)?\s*\n?(.*?)\n?\s*```', cleaned, re.DOTALL)
        if json_match:
            cleaned = json_match.group(1).strip()
        else:
            cleaned = re.sub(r'^```(?:json)?\s*', '', cleaned)
            cleaned = re.sub(r'\s*```$', '', cleaned)

        # 1. Thử parse trực tiếp bằng json.loads chuẩn
        try:
            result = json.loads(cleaned)
            if isinstance(result, dict):
                return result
        except json.JSONDecodeError as e:
            logger.warning(f"JSON parse chuẩn thất bại ({e}), kích hoạt cứu trợ tự động sửa JSON...")

        # 2. Thử dùng json_repair (cứu các trường hợp thiếu dấu ngoặc, cụt đuôi)
        try:
            import json_repair
            repaired = json_repair.repair_json(cleaned, return_objects=True)
            if isinstance(repaired, dict) and ("questions" in repaired or "passages" in repaired or "sections" in repaired):
                logger.info(f"✅ json_repair cứu trợ thành công: trích xuất được {len(repaired.get('questions', []))} câu hỏi!")
                return repaired
        except Exception as e:
            logger.warning(f"json_repair gặp lỗi: {e}")

        # 3. Regex Scanner bóc tách từng khối câu hỏi JSON độc lập
        extracted_questions = []
        q_matches = re.finditer(
            r'\{\s*"question_number"\s*:\s*(\d+).*?"options"\s*:\s*\[(.*?)\]\s*\}',
            cleaned,
            re.DOTALL
        )
        for qm in q_matches:
            raw_block = qm.group(0)
            try:
                q_obj = json.loads(raw_block)
                extracted_questions.append(q_obj)
            except Exception:
                try:
                    import json_repair
                    q_obj = json_repair.repair_json(raw_block, return_objects=True)
                    if isinstance(q_obj, dict) and "question_number" in q_obj:
                        extracted_questions.append(q_obj)
                except Exception:
                    pass

        if extracted_questions:
            logger.info(f"✅ Regex Scanner đã cứu trợ thành công {len(extracted_questions)} câu hỏi từ phản hồi bị đứt đoạn!")
            return {"questions": extracted_questions, "passages": []}

        logger.error(f"❌ Không thể parse JSON từ Gemini response: {text[:500]}")
        return {"questions": [], "passages": []}

    # ================================================================
    # PRIVATE: Render page to PNG
    # ================================================================
    def _render_page_to_png(self, page, dpi: int = 200) -> bytes:
        """Render 1 trang PyMuPDF thành bytes PNG."""
        zoom = dpi / 72
        mat = page.parent.__class__.Matrix(zoom, zoom) if hasattr(page.parent.__class__, 'Matrix') else None

        import fitz
        mat = fitz.Matrix(zoom, zoom)
        pix = page.get_pixmap(matrix=mat)
        return pix.tobytes("png")

    # ================================================================
    # PRIVATE: Extract embedded images
    # ================================================================
    def _extract_embedded_images(
        self, doc, output_dir: str
    ) -> List[Dict[str, Any]]:
        """Bóc tách ảnh nhúng từ PDF (giữ logic cũ của PDFExtractor)."""
        from PIL import Image

        os.makedirs(output_dir, exist_ok=True)
        extracted = []
        total_found = 0
        skipped_small = 0

        for page_idx, page in enumerate(doc):
            page_images = page.get_images(full=True)
            total_found += len(page_images)
            for img_idx, img in enumerate(page_images):
                xref = img[0]
                try:
                    base_image = doc.extract_image(xref)
                    image_bytes = base_image["image"]
                    image_ext = base_image["ext"]

                    pil_image = Image.open(io.BytesIO(image_bytes))
                    # Giảm threshold xuống 50x50 để bắt được tranh nhỏ trong đề nghe JLPT
                    if pil_image.width < 50 or pil_image.height < 50:
                        skipped_small += 1
                        continue

                    filename = f"page_{page_idx + 1}_img_{img_idx + 1}.{image_ext}"
                    filepath = os.path.join(output_dir, filename)

                    with open(filepath, "wb") as f:
                        f.write(image_bytes)

                    url = f"http://localhost:8000/static/extracted_images/{filename}"
                    extracted.append({
                        "page": page_idx + 1,
                        "filename": filename,
                        "filepath": filepath,
                        "url": url,
                        "width": pil_image.width,
                        "height": pil_image.height,
                    })
                except Exception as e:
                    logger.debug(f"Không thể extract ảnh xref={xref} trang {page_idx + 1}: {e}")
                    continue

        logger.info(
            f"🖼️ [IMAGE EXTRACT] Tìm thấy {total_found} ảnh nhúng trong PDF, "
            f"trích xuất được {len(extracted)} ảnh (bỏ {skipped_small} ảnh quá nhỏ)"
        )
        return extracted

    @classmethod
    def _sanitize_passage_title_and_content(cls, passage: Dict[str, Any]) -> Dict[str, Any]:
        """
        Làm sạch tiêu đề và nội dung bài đọc hiểu:
        1. Tiêu đề bài đọc (title) hiển thị ở thẻ Header, TUYỆT ĐỐI KHÔNG chứa mã HTML raw (<ruby>, <rt>, <p>...)
        2. Nếu Gemini trích xuất nhầm câu chỉ dẫn đề thi Mondai ("もんだい５ つぎの ぶんしょうを 読んで...") vào title:
           Tự động thay thế bằng tiêu đề chuẩn "Đoạn văn đọc hiểu".
        3. Nếu Gemini trích xuất nhầm câu dẫn tình huống/bối cảnh (ví dụ: "(3) 森さんの机の上に、山口先生のメモと本があります。") vào title:
           - Tự động chuyển câu này vào ĐẦU của `passage.content_jp` (nếu chưa có) kèm thẻ ruby để người học đọc được bối cảnh.
           - Đặt lại tiêu đề gọn gàng là "Bài đọc (3)".
        """
        if not passage or not isinstance(passage, dict):
            return passage

        raw_title = (passage.get("title") or "").strip()
        content_jp = passage.get("content_jp") or ""

        if not raw_title:
            passage["title"] = "Đoạn văn đọc hiểu"
            return passage

        # Bóc tách text trần không chứa HTML để phân tích ngữ nghĩa
        clean_text = re.sub(r'<rt>[\s\S]*?</rt>', '', raw_title)
        clean_text = re.sub(r'<[^>]+>', '', clean_text).strip()
        clean_text = re.sub(r'\s+', ' ', clean_text)

        # 1. Kiểm tra xem có phải câu chỉ dẫn đề thi Mondai không
        is_mondai_instruction = bool(re.search(
            r'(?:もんだい|問題)\s*[１２３４５６7890\d]|'
            r'つぎの\s*(?:ぶんしょう|文章)|'
            r'しつもんに\s*(?:こたえて|かたえて|答えて)|'
            r'いちばん\s*いい\s*ものを|'
            r'えらんで\s*ください|'
            r'何を入れますか',
            clean_text,
            flags=re.IGNORECASE
        ))

        if is_mondai_instruction:
            passage["title"] = "Đoạn văn đọc hiểu"
            return passage

        # 2. Kiểm tra xem có phải câu dẫn bối cảnh/tình huống bài đọc không (vd: "(3) 森さんの机の上に...")
        intro_num_match = re.match(r'^[（\(]\s*([0-9０-９\d]+)\s*[）\)]', clean_text)
        is_context_intro = False
        if intro_num_match and (len(clean_text) >= 15 or any(p in clean_text for p in ["あります", "います", "でした", "ました", "メモ", "手紙", "について", "。"])):
            is_context_intro = True
        elif len(clean_text) >= 18 and any(p in clean_text for p in ["あります", "います", "でした", "ました", "メモ", "手紙", "について", "。"]):
            is_context_intro = True

        if is_context_intro:
            clean_content = re.sub(r'<[^>]+>', '', content_jp)
            core_intro = re.sub(r'^[（\(]\s*[0-9０-９\d]+\s*[）\)]\s*', '', clean_text).strip()
            if core_intro and core_intro not in clean_content:
                passage["content_jp"] = f"{raw_title}\n\n{content_jp}".strip()
                logger.info(f"Đã chuyển câu dẫn bối cảnh từ passage.title vào passage.content_jp: {core_intro[:35]}...")

            if intro_num_match:
                passage["title"] = f"Bài đọc ({intro_num_match.group(1)})"
            else:
                passage["title"] = "Đoạn văn đọc hiểu"
            return passage

        # 3. Tiêu đề bình thường: Bóc sạch toàn bộ thẻ HTML tags (kể cả <ruby>)
        final_title = re.sub(r'<rt>[\s\S]*?</rt>', '', raw_title)
        final_title = re.sub(r'<[^>]+>', '', final_title).strip()
        final_title = re.sub(r'\s+', ' ', final_title)
        if len(final_title) > 50:
            final_title = "Đoạn văn đọc hiểu"

        passage["title"] = final_title or "Đoạn văn đọc hiểu"
        return passage

    # ================================================================
    # PRIVATE: Merge passages into questions
    # ================================================================
    def _merge_passages_into_questions(
        self,
        questions: List[Dict[str, Any]],
        passages: List[Dict[str, Any]],
    ) -> List[Dict[str, Any]]:
        """Gộp passage metadata vào các câu hỏi tương ứng và kế thừa giữa các câu cùng bài đọc."""
        # 1. Nhóm câu hỏi theo source_page
        page_to_questions: Dict[int, List[Dict[str, Any]]] = {}
        for q in questions:
            raw_p = q.get("source_page")
            try:
                p = int(raw_p) if raw_p is not None else 1
            except (ValueError, TypeError):
                p = 1
            q["source_page"] = p
            page_to_questions.setdefault(p, []).append(q)

        # 2. Xử lý passages trả về từ Gemini
        unassigned_passages = []
        for passage in passages:
            q_indices = passage.get("question_indices", [])
            raw_sp = passage.get("source_page")
            try:
                source_page = int(raw_sp) if raw_sp is not None else 1
            except (ValueError, TypeError):
                source_page = 1

            passage_data = {
                "title": passage.get("title", "Bài đọc"),
                "passage_type": passage.get("passage_type", "TEXT"),
                "content_jp": passage.get("content_jp", ""),
                "translation_vi": passage.get("translation_vi", ""),
                "image_url": passage.get("image_url"),
                "source_page": source_page,
            }
            passage_data = self._sanitize_passage_title_and_content(passage_data)
            if not passage_data["content_jp"] or len(passage_data["content_jp"].strip()) < 10:
                continue

            target_qs = page_to_questions.get(source_page, [])
            if q_indices:
                matched_any = False
                for idx in q_indices:
                    if 0 <= idx < len(target_qs):
                        matched_any = True
                        if not target_qs[idx].get("passage") or len(target_qs[idx].get("passage", {}).get("content_jp", "")) < len(passage_data["content_jp"]):
                            target_qs[idx]["passage"] = passage_data.copy()
                if not matched_any:
                    unassigned_passages.append(passage_data)
            else:
                reading_qs = [
                    q for q in target_qs
                    if q.get("mondai_code") in {
                        "TEXT_GRAMMAR", "SHORT_PASSAGE", "MEDIUM_PASSAGE",
                        "LONG_PASSAGE", "INFORMATION_RETRIEVAL"
                    }
                ]
                if reading_qs:
                    for q in reading_qs:
                        if not q.get("passage") or len(q.get("passage", {}).get("content_jp", "")) < len(passage_data["content_jp"]):
                            q["passage"] = passage_data.copy()
                else:
                    unassigned_passages.append(passage_data)

        # Gán unassigned_passages (trang chỉ in bài đọc/tờ rơi, câu hỏi ở trang kế tiếp)
        for u_pass in unassigned_passages:
            raw_sp = u_pass.get("source_page")
            try:
                s_page = int(raw_sp) if raw_sp is not None else 1
            except (ValueError, TypeError):
                s_page = 1

            for next_page in [s_page + 1, s_page + 2]:
                next_qs = page_to_questions.get(next_page, [])
                next_reading_qs = [
                    q for q in next_qs
                    if q.get("mondai_code") in {
                        "TEXT_GRAMMAR", "SHORT_PASSAGE", "MEDIUM_PASSAGE",
                        "LONG_PASSAGE", "INFORMATION_RETRIEVAL"
                    }
                ]
                if next_reading_qs:
                    for q in next_reading_qs:
                        existing_p = q.get("passage")
                        if not existing_p or not existing_p.get("content_jp") or len(existing_p.get("content_jp", "").strip()) < len(u_pass["content_jp"]):
                            q["passage"] = u_pass.copy()
                    break

        # 3. Kế thừa bài đọc xuyên suốt các câu hỏi
        return self._inherit_passages_cross_questions(questions)

    @classmethod
    def _inherit_passages_cross_questions(cls, questions: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
        """
        Kế thừa và đồng bộ hóa passage giữa các câu hỏi cùng bài đọc / Mondai:
        1. INFORMATION_RETRIEVAL: Toàn bộ các câu hỏi tìm kiếm thông tin chia sẻ chung bài đọc tờ rơi đầy đủ nhất.
           Nếu có nhiều phần đoạn rời rạc của tờ rơi trích xuất ở các trang khác nhau, tự động gộp lại.
        2. TEXT_GRAMMAR: Toàn bộ câu hỏi đục lỗ chia sẻ chung bài văn đục lỗ đầy đủ nhất.
        3. MEDIUM_PASSAGE / LONG_PASSAGE: Các câu hỏi kế tiếp (ví dụ câu 32, 33 ở trang sau) kế thừa bài đọc của câu 30, 31.
        """
        if not questions:
            return questions

        # 1. Đồng bộ và hợp nhất INFORMATION_RETRIEVAL (Tờ rơi / Thông báo)
        info_qs = [q for q in questions if q.get("mondai_code") == "INFORMATION_RETRIEVAL"]
        if info_qs:
            best_info_passage = None
            max_info_len = 0
            distinct_passages = []

            for q in info_qs:
                p = q.get("passage")
                if p and isinstance(p, dict) and p.get("content_jp"):
                    c_text = p["content_jp"].strip()
                    if c_text and not any(c_text in dp or dp in c_text for dp in distinct_passages):
                        distinct_passages.append(c_text)
                    if len(c_text) > max_info_len:
                        max_info_len = len(c_text)
                        best_info_passage = p

            if best_info_passage:
                # Nếu có nhiều phần khác nhau của tờ rơi trích xuất từ các trang, gộp lại
                if len(distinct_passages) >= 2 and sum(len(d) for d in distinct_passages) > max_info_len + 30:
                    combined_content = "\n\n".join(distinct_passages)
                    best_info_passage = {
                        "title": best_info_passage.get("title", "Tìm kiếm thông tin"),
                        "passage_type": "TEXT",
                        "content_jp": combined_content,
                        "translation_vi": "",
                        "image_url": None,
                    }
                    best_info_passage = cls._sanitize_passage_title_and_content(best_info_passage)
                for q in info_qs:
                    q["passage"] = best_info_passage.copy()

        # 2. Đồng bộ TEXT_GRAMMAR (Ngữ pháp đoạn văn / đục lỗ)
        cloze_qs = [q for q in questions if q.get("mondai_code") == "TEXT_GRAMMAR"]
        if cloze_qs:
            all_cloze_texts: List[str] = []
            for q in cloze_qs:
                p = q.get("passage")
                if p and isinstance(p, dict) and p.get("content_jp"):
                    txt = p["content_jp"].strip()
                    if txt and not any(txt in act for act in all_cloze_texts):
                        all_cloze_texts = [act for act in all_cloze_texts if act not in txt]
                        all_cloze_texts.append(txt)

            if all_cloze_texts:
                intro_text = ""
                ken_text = ""
                minji_text = ""
                full_text = None

                # 1. Tìm bản ghi đã chứa trọn vẹn cả Ken và Minji
                for t in all_cloze_texts:
                    has_k = any(k in t for k in ["ケン", "すし", "22", "23"])
                    has_m = any(m in t for m in ["ミンジ", "本屋", "24", "25", "26"])
                    if has_k and has_m:
                        if not full_text or len(t) > len(full_text):
                            full_text = t

                # 2. Tìm câu mở đầu bối cảnh ngoài khung
                for t in all_cloze_texts:
                    intro_match = re.search(
                        r'([\s\S]*?(?:日本で|学生が|「すきな|前で\s*読みました|読みました[\s\S]*?。))(?=\s*[(（]1[)）]|\s*ケンさん|\s*わたしは\s*すし)',
                        t
                    )
                    if intro_match:
                        cand = intro_match.group(1).strip()
                        if len(cand) >= 15 and ("学生" in cand or "日本" in cand or "店" in cand):
                            if len(cand) > len(intro_text):
                                intro_text = cand

                if full_text:
                    combined_content = full_text
                    if intro_text and not any(part in combined_content for part in ["学生が", "「すきな", "日本で　勉強", "日本で 勉強"]):
                        combined_content = f"{intro_text}\n\n{combined_content}".strip()
                else:
                    for t in all_cloze_texts:
                        if any(k in t for k in ["ケン", "すし", "22"]) and not any(m in t for m in ["ミンジ", "本屋", "24"]):
                            if len(t) > len(ken_text):
                                ken_text = t
                        elif any(m in t for m in ["ミンジ", "本屋", "24", "26"]) and not any(k in t for k in ["ケン", "すし", "22"]):
                            if len(t) > len(minji_text):
                                minji_text = t

                    parts = []
                    if intro_text:
                        parts.append(intro_text)
                    if ken_text:
                        clean_ken = ken_text
                        if intro_text and intro_text in clean_ken:
                            clean_ken = clean_ken.replace(intro_text, "").strip()
                        if any(w in clean_ken for w in ["ケン", "すし"]) and "(1)" not in clean_ken and "ケンさん" not in clean_ken:
                            clean_ken = f"(1) ケンさんの　ぶんしょう\n\n{clean_ken}"
                        parts.append(clean_ken)
                    if minji_text:
                        clean_minji = minji_text
                        if any(w in clean_minji for w in ["ミンジ", "本屋"]) and "(2)" not in clean_minji and "ミンジさん" not in clean_minji:
                            clean_minji = f"(2) ミンジさんの　ぶんしょう\n\n{clean_minji}"
                        parts.append(clean_minji)

                    combined_content = "\n\n".join(parts) if parts else "\n\n".join(all_cloze_texts)

                # Chuẩn hóa tiêu đề chung thành "文章の文法"
                master_cloze_passage = {
                    "title": "文章の文法",
                    "passage_type": "TEXT",
                    "content_jp": combined_content,
                    "translation_vi": "",
                    "image_url": None,
                }
                master_cloze_passage = cls._sanitize_passage_title_and_content(master_cloze_passage)
                master_cloze_passage["title"] = "文章の文法"

                for q in cloze_qs:
                    q["passage"] = master_cloze_passage.copy()

        # 3. Kế thừa tuần tự cho các bài đọc hiểu xuyên trang (MEDIUM_PASSAGE, LONG_PASSAGE, SHORT_PASSAGE)
        # TUYỆT ĐỐI KHÔNG áp dụng cho INFORMATION_RETRIEVAL (Mondai 6) để tránh câu 32 bị ghi đè thành MEDIUM_PASSAGE.
        active_passage = None
        active_mondai = None

        for q in questions:
            m_code = q.get("mondai_code")
            # INFORMATION_RETRIEVAL là câu hỏi độc lập của Mondai 6, xóa ngữ cảnh kế thừa
            if m_code == "INFORMATION_RETRIEVAL":
                active_passage = None
                active_mondai = None
                continue

            # Bao phủ toàn bộ các dạng đọc hiểu từ N5 đến N1:
            # N5/N4: SHORT_PASSAGE, MEDIUM_PASSAGE
            # N3: SHORT_PASSAGE, MEDIUM_PASSAGE, LONG_PASSAGE
            # N2/N1: SHORT_PASSAGE, MEDIUM_PASSAGE, INTEGRATED_COMPREHENSION, THEMATIC_COMPREHENSION
            is_reading = m_code in {
                "SHORT_PASSAGE", "MEDIUM_PASSAGE", "LONG_PASSAGE",
                "INTEGRATED_COMPREHENSION", "THEMATIC_COMPREHENSION"
            }
            if not is_reading:
                active_passage = None
                active_mondai = None
                continue

            p = q.get("passage")
            p_text = (p.get("content_jp", "") if (p and isinstance(p, dict)) else "").strip()

            if p_text and len(p_text) >= 100:
                # Gặp một bài đọc hoàn chỉnh mới
                active_passage = p
                active_mondai = m_code
            elif active_passage and active_mondai == m_code:
                # Kế thừa bài đọc đầy đủ của câu trước cùng bài đọc xuyên trang
                if not p or not p_text or len(p_text) < 100:
                    q["passage"] = active_passage.copy()
                    logger.info(f"📖 Câu {q.get('question_number')} đã tự động kế thừa bài đọc xuyên trang từ câu trước ({active_mondai})")
            else:
                active_passage = None
                active_mondai = None

        return questions

    # ================================================================
    # PRIVATE: Infer mondai code
    # ================================================================
    def _infer_mondai_code(self, question: Dict[str, Any]) -> str:
        """Suy luận mondai_code từ nội dung câu hỏi nếu Gemini trả sai."""
        text = question.get("question_text", "")
        has_passage = question.get("passage") is not None
        has_star = question.get("star_position") is not None

        if has_star or "★" in text:
            return "SENTENCE_COMPOSITION"
        if has_passage:
            return "SHORT_PASSAGE"
        if re.search(r'【[\u4e00-\u9faf]+】', text):
            return "KANJI_READING"
        if re.search(r'【[\u3040-\u309f]+】', text):
            return "ORTHOGRAPHY"
        if "(" in text or "（" in text:
            return "CONTEXTUAL_USE"
        if "同じ" in text or "おなじ" in text:
            return "SYNONYMS"
        return "SENTENTIAL_GRAMMAR"  # Default fallback

    @staticmethod
    def _normalize_options(options: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
        """
        Chuẩn hóa danh sách options:
        1. Đảm bảo DUY NHẤT 1 đáp án có is_correct=True.
        2. Nếu có từ 2 đáp án is_correct=True: chỉ giữ đáp án đúng đầu tiên, các đáp án còn lại thành False.
        3. Nếu không có đáp án nào đúng: mặc định chọn option đầu tiên là True.
        """
        if not options:
            return options

        correct_indices = [i for i, opt in enumerate(options) if opt.get("is_correct") is True]

        if len(correct_indices) > 1:
            first_idx = correct_indices[0]
            for i, opt in enumerate(options):
                opt["is_correct"] = (i == first_idx)
        # Nếu len(correct_indices) == 0: giữ nguyên tất cả là False (chờ file đáp án chính thức hoặc giáo viên tick chọn)

        return options

    @staticmethod
    def _format_cloze_passage_blanks(content_jp: str, cloze_items: list) -> str:
        """
        Tự động chuẩn hóa các chỗ trống trong bài đục lỗ thành 【 XX 】 rõ ràng:
        Xử lý: [ 21 ], [21], (21), （21）, 21。, 22 から, 24、, ポチ 25
        Đồng thời phát hiện và khôi phục chỗ trống bị AI tự ý điền sẵn trợ từ (ví dụ câu 25 bị điền thành 'ポチは わたしたち').
        """
        if not content_jp or not cloze_items:
            return content_jp

        import re
        result = content_jp

        for item in cloze_items:
            if isinstance(item, dict):
                n = item.get("question_number")
                q_opts = item.get("options", [])
            else:
                n = item
                q_opts = []

            if not n:
                continue

            num_str = str(n)
            # 1. Nếu đã có ngoặc [ 21 ], (21), （ 21 ）, 【21】...
            result = re.sub(
                r'[\[\(（［【]\s*' + num_str + r'\s*[\]\)）］】]',
                f'【 {num_str} 】',
                result
            )
            # 2. Nếu là số trần không ngoặc (e.g. " 21。", "22 から", "24、", "名詞 25")
            if f'【 {num_str} 】' not in result:
                result = re.sub(
                    r'(?<![\d【])' + num_str + r'(?![\d】])',
                    f'【 {num_str} 】',
                    result
                )

            # 3. Nếu AI tự điền đáp án làm mất chỗ trống: tìm vị trí từ trùng với options để khôi phục chỗ trống
            if f'【 {num_str} 】' not in result and q_opts:
                opt_words = [opt.get("option_text", "").strip() for opt in q_opts if opt.get("option_text")]
                for word in opt_words:
                    if word and len(word) <= 3 and word in result:
                        last_pos = result.rfind(word)
                        if last_pos != -1:
                            result = result[:last_pos] + f'【 {num_str} 】' + result[last_pos + len(word):]
                            break

        return result

    @staticmethod
    def flatten_nested_ruby(text: str) -> str:
        """Loại bỏ thẻ ruby lồng nhau gây lỗi hiển thị furigana xếp chồng trên giao diện."""
        if not text or "<ruby>" not in text:
            return text

        def clean_nested(m):
            base = m.group(1).strip()
            inner_rt = m.group(2)
            rt_match = re.search(r'<rt>([^<]+)</rt>', inner_rt)
            if rt_match:
                furi = rt_match.group(1).strip()
            else:
                rb_match = re.search(r'<rb>([^<]+)</rb>', inner_rt)
                if rb_match:
                    furi = rb_match.group(1).strip()
                else:
                    furi = re.sub(r'<[^>]+>', '', inner_rt).strip()
            return f'<ruby>{base}<rt>{furi}</rt></ruby>'

        pat = r'<ruby>\s*([^<]+?)\s*<rt>\s*(<ruby>.*?</ruby>)\s*</rt>\s*</ruby>'
        text = re.sub(pat, clean_nested, text, flags=re.DOTALL)
        text = re.sub(r'<ruby>\s*<rb>\s*([^<]+?)\s*</rb>\s*<rt>\s*([^<]+?)\s*</rt>\s*</ruby>', r'<ruby>\1<rt>\2</rt></ruby>', text)
        return text

    @staticmethod
    def _clean_question_text(question: Dict[str, Any]) -> Dict[str, Any]:
        """Tự động phát hiện và chuẩn hóa dấu chỗ trống, ngoặc chữ Hán, gạch sao ★ và từ cách dùng."""
        text = question.get("question_text", "")
        if not text:
            return question

        # Tự động chuẩn hóa Furigana: Nếu gặp dạng 漢字(ふりがな) hoặc 漢字（ふりがな）
        # tự động chuyển đổi thành thẻ HTML <ruby>漢字<rt>ふりがな</rt></ruby> chuẩn
        text = GeminiExamParser.flatten_nested_ruby(text)
        text = re.sub(
            r'([\u4e00-\u9faf]+)[(（]([\u3040-\u309f]+)[)）]',
            r'<ruby>\1<rt>\2</rt></ruby>',
            text
        )
        text = GeminiExamParser.flatten_nested_ruby(text)
        # Loại bỏ các dấu markdown ** quanh chữ Hán nếu Gemini vô tình sinh ra
        text = re.sub(r'\*\*([\u4e00-\u9faf]+)\*\*', r'\1', text)

        for opt in question.get("options") or []:
            if opt.get("option_text"):
                opt["option_text"] = GeminiExamParser.flatten_nested_ruby(opt["option_text"])

        # Chuẩn hóa cả trong passage nếu có
        if question.get("passage") and isinstance(question["passage"], dict):
            question["passage"] = GeminiExamParser._sanitize_passage_title_and_content(question["passage"])
            if question["passage"].get("content_jp"):
                c_jp = question["passage"]["content_jp"]
                c_jp = GeminiExamParser.flatten_nested_ruby(c_jp)
                c_jp = re.sub(r'([\u4e00-\u9faf]+)[(（]([\u3040-\u309f]+)[)）]', r'<ruby>\1<rt>\2</rt></ruby>', c_jp)
                c_jp = GeminiExamParser.flatten_nested_ruby(c_jp)
                c_jp = re.sub(r'\*\*([\u4e00-\u9faf]+)\*\*', r'\1', c_jp)
                question["passage"]["content_jp"] = c_jp

        # Tự động tách lượt hội thoại đối đáp (A「...」 B「...」 hoặc 山田「...」 田中「...」) thành 2 dòng riêng biệt
        text = re.sub(r'(」)\s*([A-Za-z0-9\u3040-\u30ff\u4e00-\u9faf]{1,10}[「：:])', r'\1\n\2', text)
        text = re.sub(r'([。？！\?!])\s*([A-Za-z0-9\u3040-\u30ff\u4e00-\u9faf]{1,8}[「：:])', r'\1\n\2', text)

        mondai_code = question.get("mondai_code", "")

        # 1b. Dạng bài đục lỗ (TEXT_GRAMMAR): question_text BẮT BUỘC là 【 XX 】
        if mondai_code == "TEXT_GRAMMAR":
            is_instruction = any(k in text for k in ["何を入れますか", "文章の意味", "えらんで", "選んで", "もんだい", "問題"])
            is_paren_or_empty = text.strip() in {"", "()", "( )", "（）", "（　）", "（ ）"}
            q_num = question.get("raw_question_number") or question.get("question_number")

            if is_instruction or is_paren_or_empty:
                # Nếu text là câu lệnh đề bài (vd "もんだい３ 22 から 26 に 何を..."),
                # các con số trong text chỉ là số câu lệnh, KHÔNG PHẢI số của câu này!
                # BẮT BUỘC dùng số thứ tự câu hỏi q_num (vd: 22, 23, 24, 25, 26)
                target_num = str(q_num) if q_num else ""
            else:
                match_num = re.search(r'[【\[\(（]?\s*(\d{1,2})\s*[】\]\)）]?', text)
                target_num = match_num.group(1) if match_num else (str(q_num) if q_num else "")

            if target_num:
                text = f"【 {target_num} 】"
            elif q_num:
                text = f"【 {q_num} 】"
            question["question_text"] = text
            return question

        # 1. Dạng điền từ vào chỗ trống / Ngữ pháp câu đơn: BẮT BUỘC phải có chỗ trống （　　）
        if mondai_code in {"CONTEXTUAL_USE", "SENTENTIAL_GRAMMAR"}:
            if mondai_code == "SENTENTIAL_GRAMMAR":
                # Trong Mondai 1 Ngữ pháp, loại bỏ ngoặc vuông 【 】 hoặc thẻ <u>...</u> bị gắn nhầm vào chữ Hán (như 【米】 -> 米)
                text = re.sub(r'【(?!\s*\d+\s*】)(.*?)】', r'\1', text)
                text = re.sub(r'<u>(.*?)</u>', r'\1', text)

            has_blank = any(mark in text for mark in ["（", "(", "【", "___", "＿＿", "―", "──"])
            if not has_blank:
                if re.search(r'\s+[、，,]', text):
                    text = re.sub(r'\s+[、，,]', '（　　）、', text, count=1)
                elif "  " in text or "　" in text:
                    text = re.sub(r'[ 　]{2,}', '（　　）', text, count=1)
                    if "（　　）" not in text:
                        text = re.sub(r'　', '（　　）', text, count=1)
                elif " " in text:
                    parts = text.split(" ", 1)
                    if len(parts) == 2 and parts[0] and parts[1]:
                        text = f"{parts[0]}（　　）{parts[1]}"
                else:
                    if "。" in text:
                        text = text.replace("。", "（　　）。", 1)
                    else:
                        text = f"{text}（　　）"
            question["question_text"] = text

        # 2a. Dạng cách đọc chữ Hán (KANJI_READING - Hán tự -> Hiragana):
        elif mondai_code == "KANJI_READING":
            # TUYỆT ĐỐI KHÔNG ĐƯỢC ĐỂ FURIGANA TRÊN TỪ ĐƯỢC HỎI / GẠCH CHÂN (LÀM LỘ ĐÁP ÁN!)
            # 1. Bóc bỏ ruby nếu từ gạch chân <u> hoặc 【 】 bị gắn ruby
            text = re.sub(r'<u>\s*<ruby>(.*?)(?:<rt>.*?</rt>)?</ruby>\s*</u>', r'<u>\1</u>', text)
            text = re.sub(r'<ruby>\s*<u>(.*?)</u>(?:<rt>.*?</rt>)?</ruby>', r'<u>\1</u>', text)
            text = re.sub(r'【\s*<ruby>(.*?)(?:<rt>.*?</rt>)?</ruby>\s*】', r'<u>\1</u>', text)
            text = re.sub(r'<ruby>\s*【(.*?)】(?:<rt>.*?</rt>)?</ruby>', r'<u>\1</u>', text)

            # 2. Nếu có <ruby>漢字<rt>cách_đọc</rt></ruby> mà cách_đọc trùng với 1 trong các options:
            # Đây chính là từ đang được hỏi cách đọc -> Bóc bỏ <rt> và chuyển thành <u>漢字</u>
            opt_texts = [o.get("option_text", "").strip() for o in question.get("options", []) if o.get("option_text")]
            ruby_matches = list(re.finditer(r'<ruby>(.*?)<rt>(.*?)</rt></ruby>', text))
            for m in reversed(ruby_matches):
                k_word, rt_word = m.group(1).strip(), m.group(2).strip()
                k_clean = re.sub(r'</?u>', '', k_word)
                if rt_word in opt_texts or k_clean in opt_texts:
                    text = text[:m.start()] + f'<u>{k_clean}</u>' + text[m.end():]

            # 3. Nếu chưa có <u> và có <ruby>, nếu câu chỉ có 1 khối <ruby>, đó chính là từ cần hỏi
            if "<u>" not in text and "【" not in text:
                rem_rubies = list(re.finditer(r'<ruby>(.*?)<rt>(.*?)</rt></ruby>', text))
                if len(rem_rubies) == 1:
                    m = rem_rubies[0]
                    k_clean = re.sub(r'</?u>', '', m.group(1).strip())
                    text = text[:m.start()] + f'<u>{k_clean}</u>' + text[m.end():]

            # 4. Nếu vẫn chưa có <u> và cũng không có ruby, dò chữ Hán và gạch chân bằng pykakasi
            if "【" not in text and "<u>" not in text and "「" not in text:
                kanji_blocks = re.findall(r'[\u4e00-\u9faf]+', text)
                if len(kanji_blocks) == 1:
                    target = kanji_blocks[0]
                    text = text.replace(target, f"<u>{target}</u>", 1)
                elif len(kanji_blocks) > 1:
                    try:
                        import pykakasi
                        k = pykakasi.kakasi()
                        matched = False
                        for kb in kanji_blocks:
                            conv = "".join(item["hira"] for item in k.convert(kb)).strip()
                            if conv in opt_texts:
                                text = text.replace(kb, f"<u>{kb}</u>", 1)
                                matched = True
                                break
                        if not matched and kanji_blocks:
                            text = text.replace(kanji_blocks[0], f"<u>{kanji_blocks[0]}</u>", 1)
                    except Exception:
                        pass

            # 5. Đảm bảo cuối cùng: Bất kỳ thẻ <u> nào chứa <ruby> đều phải lột sạch <rt>
            text = re.sub(r'<u>\s*<ruby>(.*?)(?:<rt>.*?</rt>)?</ruby>\s*</u>', r'<u>\1</u>', text)
            text = re.sub(r'<ruby>\s*<u>(.*?)</u>(?:<rt>.*?</rt>)?</ruby>', r'<u>\1</u>', text)
            question["question_text"] = text

        # 2b. Dạng cách viết chữ Hán (ORTHOGRAPHY - Hiragana -> Hán tự):
        # Từ trong câu hỏi là Hiragana (e.g. あおい), 4 options là chữ Hán (青い, 黒い, 赤い, 白い)
        elif mondai_code == "ORTHOGRAPHY":
            if "【" not in text and "<u>" not in text and "「" not in text:
                try:
                    import pykakasi
                    k = pykakasi.kakasi()
                    for opt in question.get("options", []):
                        opt_text = (opt.get("option_text") or "").strip()
                        if not opt_text:
                            continue
                        # Chuyển chữ Hán sang Hiragana
                        conv_items = k.convert(opt_text)
                        hira_reading = "".join(item["hira"] for item in conv_items).strip()
                        if hira_reading and len(hira_reading) >= 2 and hira_reading in text:
                            text = text.replace(hira_reading, f"<u>{hira_reading}</u>", 1)
                            question["question_text"] = text
                            break
                        elif opt_text and len(opt_text) >= 2 and opt_text in text:
                            # Từ Katakana (như シャツ, カメラ)
                            text = text.replace(opt_text, f"<u>{opt_text}</u>", 1)
                            question["question_text"] = text
                            break
                except Exception as e:
                    logger.debug(f"ORTHOGRAPHY pykakasi error: {e}")

        # 3. Dạng cách dùng từ (PROPER_USAGE - Mondai 5):
        elif mondai_code == "PROPER_USAGE":
            kw_match = re.search(r'【(.*?)】', text)
            target_word = kw_match.group(1).strip() if kw_match else ""

            # Nếu text là câu lệnh dài do AI copy đề bài, dò từ khóa từ các options
            if not target_word:
                for opt in question.get("options", []):
                    u_match = re.search(r'<u>(.*?)</u>', opt.get("option_text", ""))
                    if u_match:
                        target_word = u_match.group(1).strip()
                        break

            if not target_word:
                candidate = re.sub(r'^[0-9０-９.\s]+', '', text).strip()
                if 1 <= len(candidate) <= 8 and not any(p in candidate for p in ["は", "が", "を", "に", "で", "。", "、"]):
                    target_word = candidate

            if target_word:
                question["question_text"] = f"【{target_word}】"

                for opt in question.get("options", []):
                    opt_text = opt.get("option_text", "")
                    if opt_text and target_word in opt_text:
                        if f"<u>{target_word}</u>" not in opt_text:
                            if f"【{target_word}】" in opt_text:
                                opt["option_text"] = opt_text.replace(f"【{target_word}】", f"<u>{target_word}</u>")
                            else:
                                opt["option_text"] = opt_text.replace(target_word, f"<u>{target_word}</u>", 1)

        # 4. Dạng sắp xếp câu có dấu sao ★ (SENTENCE_COMPOSITION):
        elif mondai_code == "SENTENCE_COMPOSITION" or "★" in text:
            star_pos = question.get("star_position")
            if not isinstance(star_pos, int) or star_pos not in (1, 2, 3, 4):
                if "★" in text:
                    prefix = text.split("★")[0]
                    underlines = re.findall(r'[_＿]{2,}', prefix)
                    star_pos = min(4, max(1, len(underlines) + 1))
                else:
                    star_pos = 3  # Mặc định vị trí 3 chuẩn JLPT
            question["star_position"] = star_pos

            # Đảm bảo question_text có 4 vị trí gạch với dấu sao ở đúng star_pos
            if "★" not in text:
                slots = ["＿＿＿", "＿＿＿", "＿＿＿", "＿＿＿"]
                slots[star_pos - 1] = "＿★＿"
                blank_pattern = "　".join(slots)
                if not re.search(r'[_＿]{2,}', text):
                    if "。" in text:
                        text = text.replace("。", f"　{blank_pattern}　。", 1)
                    else:
                        text = f"{text}　{blank_pattern}"
                else:
                    all_blanks = list(re.finditer(r'[_＿]{2,}', text))
                    if len(all_blanks) >= 4:
                        target_blank = all_blanks[star_pos - 1]
                        text = text[:target_blank.start()] + "＿★＿" + text[target_blank.end():]
            question["question_text"] = text

        return question


# ====================================================================
# Singleton instance (lazy init)
# ====================================================================
_parser_instance: Optional[GeminiExamParser] = None


def get_gemini_parser() -> GeminiExamParser:
    """Lấy instance singleton của GeminiExamParser."""
    global _parser_instance
    if _parser_instance is None:
        _parser_instance = GeminiExamParser()
    return _parser_instance
