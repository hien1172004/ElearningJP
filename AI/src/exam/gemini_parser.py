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

import io
import json
import logging
import os
import re
import tempfile
from pathlib import Path
from typing import Any, Dict, List, Optional, Tuple
from dotenv import load_dotenv
from PIL import Image

load_dotenv()

from .jlpt_rules import audit_exam_section, infer_mondai_from_spec, get_expected_mondais

logger = logging.getLogger("kanji_ai")

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
     + Đề thi chứa một bài văn có các chỗ trống đánh số trong khung ô vuông ví dụ [ 21 ], [ 22 ], [ 23 ], [ 24 ], [ 25 ]...
     + BẮT BUỘC TRÍCH XUẤT ĐỦ 100% CÁC CHỖ TRỐNG ĐÁNH SỐ TRÊN ĐỀ VÀO TRONG `passage.content_jp`: các chỗ trống này PHẢI ĐƯỢC ĐẶT TRONG NGOẶC VUÔNG RÕ RÀNG: `【 21 】`, `【 22 】`, `【 23 】`, `【 24 】`, `【 25 】`... TUYỆT ĐỐI KHÔNG để số trần không ngoặc (như "21。", "22 から", "23。", "24、", "名詞 25")!
     + TUYỆT ĐỐI KHÔNG TỰ ĐIỀN TỪ, TRỢ TỪ (như は, が, を, に, で...) HOẶC SỬA VĂN LÀM MẤT BẤT KỲ CHỖ TRỐNG NÀO (ĐẶC BIỆT CHỖ TRỐNG CUỐI CÙNG NHƯ 【 25 】)! Ví dụ trên đề là "名詞 [ 25 ] わたしたちの" thì PHẢI giữ nguyên là "名詞 【 25 】 わたしたちの", TUYỆT ĐỐI KHÔNG được tự ý điền trợ từ (ví dụ "名詞は わたしたちの").
     + Vì dạng bài đục lỗ trên đề thi KHÔNG CÓ CÂU HỎI RIÊNG cho từng số, TUYỆT ĐỐI KHÔNG đưa câu chỉ dẫn đề bài ("もんだい３... 何を入れますか...") hay "( )" vào question_text!
     + `question_text` của từng câu đục lỗ BẮT BUỘC chỉ ghi số chỗ trống: "【 21 】", "【 22 】", "【 23 】", "【 24 】", "【 25 】"...
     + Toàn bộ bài văn này đưa vào trường `passage` của tất cả các câu hỏi đục lỗ trong bài.
     + Mondai code của TẤT CẢ các câu hỏi trong bài đục lỗ BẮT BUỘC là "TEXT_GRAMMAR".
     + Mỗi ô trống chỉ tạo DUY NHẤT 1 câu hỏi tương ứng, TUYỆT ĐỐI KHÔNG lặp lại câu hỏi.

   - DẠNG ĐỌC HIỂU ĐOẠN VĂN (SHORT_PASSAGE, MEDIUM_PASSAGE, LONG_PASSAGE):
     + BẮT BUỘC trích xuất đầy đủ 100% nội dung đoạn văn tiếng Nhật vào `passage.content_jp`. Bất kỳ chữ Hán nào có furigana thì dùng `<ruby>漢字<rt>furigana</rt></ruby>`.
     + `passage.title` đặt tên bài đọc (ví dụ: "Bài đọc 1", "Bài đọc hiểu trung"...).
     + Gán đối tượng `passage` này vào câu hỏi tương ứng trong trang.

   - QUY TẮC CÂU HỎI BÀI ĐỌC NẰM Ở TRANG TIẾP THEO (VÍ DỤ CÂU 32, 33 NẰM Ở TRANG SAU BÀI ĐỌC CÂU 30, 31):
     + Trong phần Đọc hiểu (MEDIUM_PASSAGE, LONG_PASSAGE, TEXT_GRAMMAR, INFORMATION_RETRIEVAL), một bài đọc thường kèm nhiều câu hỏi (ví dụ câu 30, 31, 32, 33).
     + Nếu bài đọc in ở trang trước cùng câu 30, 31, còn câu 32, 33 in ở đầu trang này mà KHÔNG in lại bài đọc:
       * BẮT BUỘC để `passage: null` cho các câu 32, 33 này.
       * BẮT BUỘC gán đúng `mondai_code` (ví dụ "MEDIUM_PASSAGE" hoặc "LONG_PASSAGE").
       * TUYỆT ĐỐI KHÔNG tự bịa một bài đọc giả, tóm tắt hoặc copy đề câu hỏi vào `passage`! Hệ thống sẽ tự động gán bài đọc đầy đủ từ câu 30, 31 ở trang trước sang câu 32, 33.

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
     + TOÀN BỘ PHẦN ĐỌC HIỂU (TEXT_GRAMMAR, SHORT_PASSAGE, MEDIUM_PASSAGE, LONG_PASSAGE, INFORMATION_RETRIEVAL) VÀ TỪ VỰNG, NGỮ PHÁP:
       * BẮT BUỘC có `has_visual: false`, `image_url: null`, `image_box: null`.
       * TUYỆT ĐỐI KHÔNG CẮT ẢNH VÀO BÀI ĐỌC. Toàn bộ chữ, cấu trúc bảng biểu, chú thích phải nằm trong `passage.content_jp`.
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

ANSWER_SHEET_PROMPT = """Bạn là chuyên gia số hóa đề thi JLPT.
Trước mặt bạn là hình ảnh/trang PDF bảng đáp án chính thức (正答表 - Seitouhyou) của kỳ thi năng lực tiếng Nhật JLPT cấp {jlpt_level}.

Hãy đọc kỹ từng bảng đáp án và trích xuất TOÀN BỘ đáp án cho từng câu hỏi theo định dạng JSON.

## Quy tắc đọc bảng đáp án (正答表):
1. Mỗi khối bảng tương ứng với một phần thi hoặc một Mondai (問題 1, 問題 2, 問題 3...).
2. Hàng trên cùng trong mỗi bảng là SỐ THỨ TỰ CÂU HỎI (1, 2, 3... hoặc 10, 11, 12... hoặc 21, 22...).
3. Hàng bên dưới ngay sau số thứ tự câu hỏi là SỐ ĐÁP ÁN ĐÚNG tương ứng (1, 2, 3 hoặc 4).
4. Phân loại theo các phần thi chính nếu có tiêu đề:
   - "VOCABULARY": 言語知識（文字・語彙）
   - "GRAMMAR_READING": 言語知識（文法）・読解
   - "LISTENING": 聴解
   Nếu không phân phần thi hoặc chỉ có 1 bảng duy nhất, gom tất cả vào "ALL".

## Trả về DUY NHẤT mã JSON theo định dạng sau (không kèm markdown fences):
{{
  "sections": {{
    "VOCABULARY": {{
      "1": 1, "2": 1, "3": 4, "4": 2, "5": 2, "6": 3, "7": 1, "8": 2, "9": 4,
      "10": 1, "11": 4, "12": 3, "13": 4, "14": 4, "15": 1
    }},
    "GRAMMAR_READING": {{
      "1": 3, "2": 4, "3": 1, "4": 2, "5": 4, "6": 2, "7": 3, "8": 1, "9": 2, "10": 4,
      "11": 1, "12": 1, "13": 3, "14": 4, "15": 2,
      "16": 3, "17": 2, "18": 4, "19": 3, "20": 3,
      "21": 2, "22": 3, "23": 2, "24": 1, "25": 4
    }},
    "LISTENING": {{
      "1": 1, "2": 3
    }}
  }},
  "flat_answers": {{
    "1": 1, "2": 1, "3": 4
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
       {"option_key": 1, "option_text": "選択肢 1", "is_correct": false},
       {"option_key": 2, "option_text": "選択肢 2", "is_correct": false},
       {"option_key": 3, "option_text": "選択肢 3", "is_correct": false},
       {"option_key": 4, "option_text": "選択肢 4", "is_correct": false}
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
        logger.info(f"✅ GeminiExamParser khởi tạo thành công, model: {self._model_name} (fallbacks: {self._fallback_models})")

    def _generate_with_fallback(self, contents: Any, config: Any = None) -> Any:
        """Thực hiện gọi Gemini API với cơ chế tự động chuyển đổi model và xử lý Rate Limit (429)."""
        import time
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
                        wait_sec = 6.0
                        retry_match = re.search(r'retry.*?(\d+)(?:\.\d+)?s', err_str, re.IGNORECASE)
                        if retry_match:
                            retry_val = float(retry_match.group(1))
                            if retry_val > 60:
                                logger.warning(f"⚠️ Model {model} yêu cầu chờ {retry_val}s, chuyển ngay sang model tiếp theo...")
                                break
                            wait_sec = min(retry_val + 1.0, 15.0)
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
            doc = fitz.open(stream=pdf_bytes, filetype="pdf")
            if len(doc) == 0:
                return results

            # Render các trang đề thi có thể chứa lựa chọn in (thường từ trang 3 đến trang 13)
            page_parts = []
            scan_pages = range(min(2, len(doc)), min(13, len(doc)))
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
3. Xác định đúng mondai_code ('TASK_BASED' cho 問題1, 'KEY_POINT' cho 問題2) và raw_question_number (số thứ tự câu 1, 2, 3... trong Mondai đó).

Trả về JSON duy nhất theo format schema mẫu:
{{
  "booklet_options": [
    {{
      "mondai_code": "TASK_BASED",
      "raw_question_number": 1,
      "options": ["選択肢 1", "選択肢 2", "選択肢 3", "選択肢 4"]
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
                data = json.loads(response.text)
                items = data.get("booklet_options", [])
                for it in items:
                    m = it.get("mondai_code")
                    rn = it.get("raw_question_number")
                    opts = it.get("options")
                    if m and rn and isinstance(opts, list) and len(opts) == 4:
                        results[(m, int(rn))] = [str(o).strip() for o in opts]
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
    ) -> Dict[str, Any]:
        """
        Parse toàn bộ file PDF đề thi thành JSON câu hỏi chuẩn.

        Returns:
            {
                "questions": [...],
                "images": [...],
                "total_pages": int,
                "parsed_pages": int
            }
        """
        try:
            import fitz  # PyMuPDF
        except ImportError:
            raise ImportError("Cần cài PyMuPDF: pip install pymupdf")

        doc = fitz.open(stream=pdf_bytes, filetype="pdf")
        total_pages = len(doc)
        logger.info(f"📄 PDF có {total_pages} trang, bắt đầu parse với Gemini Vision...")

        all_questions: List[Dict[str, Any]] = []
        all_passages: List[Dict[str, Any]] = []
        all_images: List[Dict[str, Any]] = []
        parsed_pages = 0

        # Bóc tách ảnh nhúng (giữ nguyên logic cũ)
        if extracted_images_dir:
            all_images = self._extract_embedded_images(doc, extracted_images_dir)

        # 1. Render trước toàn bộ các trang thành ảnh PNG (DPI 180 giảm ~25% dung lượng, tốc độ upload nhanh hơn nhiều)
        rendered_pages = []
        for page_idx in range(total_pages):
            page = doc[page_idx]
            page_num = page_idx + 1
            image_bytes = self._render_page_to_png(page, dpi=180)
            rendered_pages.append((page_num, image_bytes))

        # 2. Xử lý qua ThreadPoolExecutor với số luồng an toàn (max 2 luồng để không chạm 5 RPM Free Tier)
        from concurrent.futures import ThreadPoolExecutor, as_completed
        import time
        max_workers = min(2, total_pages)
        page_results = {}
        rendered_pages_map = {p_num: img_bytes for p_num, img_bytes in rendered_pages}

        def _process_page(p_num: int, img_bytes: bytes):
            logger.info(f"  🚀 Đang gửi trang {p_num}/{total_pages} tới Gemini Vision...")
            if p_num > 1:
                time.sleep((p_num % 2) * 0.4)
            return p_num, self._call_gemini_vision(img_bytes, jlpt_level, p_num)

        with ThreadPoolExecutor(max_workers=max_workers) as executor:
            futures = [
                executor.submit(_process_page, p_num, img_bytes)
                for p_num, img_bytes in rendered_pages
            ]
            for future in as_completed(futures):
                try:
                    p_num, res = future.result()
                    page_results[p_num] = res
                except Exception as e:
                    logger.warning(f"  ⚠️ Lỗi parse song song trang: {e}")

        # 3. Gom kết quả theo đúng thứ tự trang 1 -> total_pages
        empty_or_failed_pages = []
        for page_num in range(1, total_pages + 1):
            result = page_results.get(page_num)
            if result and result.get("questions"):
                for q in result["questions"]:
                    q["source_page"] = page_num
                    if "raw_question_number" not in q:
                        q["raw_question_number"] = q.get("question_number")
                all_questions.extend(result["questions"])
                parsed_pages += 1
                logger.info(
                    f"  ✅ Trang {page_num}/{total_pages}: "
                    f"trích xuất được {len(result['questions'])} câu hỏi"
                )
            else:
                empty_or_failed_pages.append(page_num)

            if result and result.get("passages"):
                for p in result["passages"]:
                    p["source_page"] = page_num
                all_passages.extend(result["passages"])

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
                q["image_url"] = None
                q["has_visual"] = False
                if q.get("passage"):
                    q["passage"]["image_url"] = None
                    q["passage"]["passage_type"] = "TEXT"

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

                if is_listening_q and (q.get("has_visual") or m_code in {"TASK_BASED", "UTTERANCE_EXPRESSIONS"}):
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

        return {
            "questions": all_questions,
            "images": all_images,
            "total_pages": total_pages,
            "parsed_pages": parsed_pages,
            "empty_or_failed_pages": empty_or_failed_pages,
            "rendered_pages_map": rendered_pages_map,
        }

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

        logger.info(f"📊 Đã parse bảng đáp án 正答表: {list(combined_sections.keys())}, {len(combined_flat)} flat answers")
        return {
            "sections": combined_sections,
            "flat_answers": combined_flat,
        }

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
                blank_match = re.search(r'(\d{1,2})', q_text)
                blank_num = None
                if blank_match:
                    blank_num = int(blank_match.group(1))
                elif q.get("raw_question_number"):
                    blank_num = int(q["raw_question_number"])

                if blank_num is not None:
                    if blank_num not in cloze_by_blank:
                        cloze_by_blank[blank_num] = q
                    else:
                        existing = cloze_by_blank[blank_num]
                        ex_opts_filled = sum(1 for o in existing.get("options", []) if o.get("option_text"))
                        new_opts_filled = sum(1 for o in q.get("options", []) if o.get("option_text"))
                        if new_opts_filled > ex_opts_filled:
                            existing["options"] = q.get("options", existing.get("options", []))

                        ex_p_len = len(existing.get("passage", {}).get("content_jp", "") if existing.get("passage") else "")
                        new_p_len = len(q.get("passage", {}).get("content_jp", "") if q.get("passage") else "")
                        if new_p_len > ex_p_len:
                            existing["passage"] = q.get("passage")
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
            raw_num = item.get("raw_question_number") or 0
            if m == "TEXT_GRAMMAR":
                bm = re.search(r'(\d{1,2})', item.get("question_text", ""))
                if bm:
                    raw_num = int(bm.group(1))
            return (order, raw_num)

        combined.sort(key=_sort_key)

        # 4. Đánh số thứ tự thông minh (với LISTENING: đánh số tuần tự 1..N xuyên suốt đề thi)
        for idx, q in enumerate(combined):
            if section_type != "LISTENING" and q.get("raw_question_number") is not None:
                try:
                    q["question_number"] = int(q["raw_question_number"])
                except (ValueError, TypeError):
                    q["question_number"] = idx + 1
            else:
                q["question_number"] = idx + 1

        for q in combined:
            if q.get("mondai_code") == "TEXT_GRAMMAR":
                q["question_text"] = f"【 {q['question_number']} 】"

        # 5. Chuẩn hóa passage bài đục lỗ sau khi đánh lại số thứ tự
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

        matched_count = 0
        for q in questions:
            raw_num = q.get("raw_question_number")
            q_num = q.get("question_number")
            correct_opt = None

            # 1. Ưu tiên khớp theo số thứ tự in trên đề gốc (raw_question_number) để tránh lệch câu khi đề có khoảng trống
            if raw_num is not None:
                try:
                    rn = int(raw_num)
                    if rn in int_target_map:
                        correct_opt = int_target_map[rn]
                    elif rn in int_flat_map:
                        correct_opt = int_flat_map[rn]
                except (ValueError, TypeError):
                    pass

            # 2. Fallback sang question_number
            if correct_opt is None and q_num is not None:
                try:
                    qn = int(q_num)
                    if qn in int_target_map:
                        correct_opt = int_target_map[qn]
                    elif qn in int_flat_map:
                        correct_opt = int_flat_map[qn]
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

        full_result = self.parse_pdf(pdf_bytes, jlpt_level, extracted_images_dir)
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
                if raw_num is not None:
                    try:
                        raw_n = int(raw_num)
                        q_copy["mondai_code"] = infer_mondai_from_spec(
                            raw_n,
                            q_copy.get("question_text", ""),
                            jlpt_level,
                            section_type,
                            q_copy.get("options")
                        )
                    except (ValueError, TypeError):
                        if m_code not in valid_codes or not m_code:
                            q_copy["mondai_code"] = infer_mondai_from_spec(
                                None,
                                q_copy.get("question_text", ""),
                                jlpt_level,
                                section_type,
                                q_copy.get("options")
                            )
                elif m_code not in valid_codes or not m_code:
                    q_copy["mondai_code"] = infer_mondai_from_spec(
                        None,
                        q_copy.get("question_text", ""),
                        jlpt_level,
                        section_type,
                        q_copy.get("options")
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
                                        raw_n, rq.get("question_text", ""), jlpt_level, "VOCABULARY", rq.get("options")
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
    # PRIVATE: Gemini Vision Call
    # ================================================================
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
            p = q.get("source_page", 1)
            page_to_questions.setdefault(p, []).append(q)

        # 2. Xử lý passages trả về từ Gemini
        unassigned_passages = []
        for passage in passages:
            q_indices = passage.get("question_indices", [])
            source_page = passage.get("source_page")
            passage_data = {
                "title": passage.get("title", "Bài đọc"),
                "passage_type": passage.get("passage_type", "TEXT"),
                "content_jp": passage.get("content_jp", ""),
                "translation_vi": passage.get("translation_vi", ""),
                "image_url": passage.get("image_url"),
            }
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
                    passage_data["source_page"] = source_page
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
                    passage_data["source_page"] = source_page
                    unassigned_passages.append(passage_data)

        # Gán unassigned_passages (trang chỉ in bài đọc/tờ rơi, câu hỏi ở trang kế tiếp)
        for u_pass in unassigned_passages:
            s_page = u_pass.get("source_page", 1)
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
                for q in info_qs:
                    q["passage"] = best_info_passage.copy()

        # 2. Đồng bộ TEXT_GRAMMAR (Ngữ pháp đoạn văn / đục lỗ)
        cloze_qs = [q for q in questions if q.get("mondai_code") == "TEXT_GRAMMAR"]
        if cloze_qs:
            best_cloze_passage = None
            max_c_len = 0
            for q in cloze_qs:
                p = q.get("passage")
                if p and isinstance(p, dict) and p.get("content_jp"):
                    c_len = len(p["content_jp"].strip())
                    if c_len > max_c_len:
                        max_c_len = c_len
                        best_cloze_passage = p
            if best_cloze_passage and max_c_len >= 30:
                for q in cloze_qs:
                    cur_p = q.get("passage")
                    if not cur_p or not cur_p.get("content_jp") or len(cur_p.get("content_jp", "").strip()) < max_c_len:
                        q["passage"] = best_cloze_passage.copy()

        # 3. Kế thừa tuần tự cho các bài đọc hiểu xuyên trang (MEDIUM_PASSAGE, LONG_PASSAGE, SHORT_PASSAGE)
        # Khi câu hỏi (ví dụ câu 32, 33) nằm ở trang sau mà không in lại bài đọc,
        # hệ thống sẽ tự động kế thừa bài đọc đầy đủ từ câu trước đó (câu 30, 31).
        active_passage = None
        active_mondai = None

        for q in questions:
            m_code = q.get("mondai_code")
            is_reading = m_code in {"SHORT_PASSAGE", "MEDIUM_PASSAGE", "LONG_PASSAGE", "INFORMATION_RETRIEVAL", "TEXT_GRAMMAR"}
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
            elif active_passage:
                # Câu hỏi này thiếu bài đọc hoặc bài đọc rỗng/cụt (<100 ký tự)
                # Kế thừa bài đọc đầy đủ của câu trước (ví dụ câu 32, 33 ở trang kế thừa từ câu 30, 31)
                if not p or not p_text or len(p_text) < 100:
                    q["passage"] = active_passage.copy()
                    if active_mondai:
                        q["mondai_code"] = active_mondai
                    logger.info(f"📖 Câu {q.get('question_number')} đã tự động kế thừa bài đọc xuyên trang từ câu trước ({active_mondai})")

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
    def _clean_question_text(question: Dict[str, Any]) -> Dict[str, Any]:
        """Tự động phát hiện và chuẩn hóa dấu chỗ trống, ngoặc chữ Hán, gạch sao ★ và từ cách dùng."""
        text = question.get("question_text", "")
        if not text:
            return question

        # Tự động chuẩn hóa Furigana: Nếu gặp dạng 漢字(ふりがな) hoặc 漢字（ふりがな）
        # tự động chuyển đổi thành thẻ HTML <ruby>漢字<rt>ふりがな</rt></ruby> chuẩn
        text = re.sub(
            r'([\u4e00-\u9faf]+)[(（]([\u3040-\u309f]+)[)）]',
            r'<ruby>\1<rt>\2</rt></ruby>',
            text
        )
        # Loại bỏ các dấu markdown ** quanh chữ Hán nếu Gemini vô tình sinh ra
        text = re.sub(r'\*\*([\u4e00-\u9faf]+)\*\*', r'\1', text)

        # Chuẩn hóa cả trong passage nếu có
        if question.get("passage") and isinstance(question["passage"], dict) and question["passage"].get("content_jp"):
            c_jp = question["passage"]["content_jp"]
            c_jp = re.sub(r'([\u4e00-\u9faf]+)[(（]([\u3040-\u309f]+)[)）]', r'<ruby>\1<rt>\2</rt></ruby>', c_jp)
            c_jp = re.sub(r'\*\*([\u4e00-\u9faf]+)\*\*', r'\1', c_jp)
            question["passage"]["content_jp"] = c_jp

        # Tự động tách lượt hội thoại đối đáp (A「...」 B「...」 hoặc 山田「...」 田中「...」) thành 2 dòng riêng biệt
        text = re.sub(r'(」)\s*([A-Za-z0-9\u3040-\u30ff\u4e00-\u9faf]{1,10}[「：:])', r'\1\n\2', text)
        text = re.sub(r'([。？！\?!])\s*([A-Za-z0-9\u3040-\u30ff\u4e00-\u9faf]{1,8}[「：:])', r'\1\n\2', text)

        mondai_code = question.get("mondai_code", "")

        # 1b. Dạng bài đục lỗ (TEXT_GRAMMAR): question_text BẮT BUỘC là 【 XX 】
        if mondai_code == "TEXT_GRAMMAR":
            q_num = question.get("question_number")
            match_num = re.search(r'[【\[\(（]?\s*(\d{1,2})\s*[】\]\)）]?', text)
            target_num = match_num.group(1) if match_num else (str(q_num) if q_num else "")
            
            is_instruction = any(k in text for k in ["何を入れますか", "文章の意味", "えらんで", "選んで", "もんだい", "問題"])
            is_paren_or_empty = text.strip() in {"", "()", "( )", "（）", "（　）", "（ ）"}
            
            if is_instruction or is_paren_or_empty or not text.strip().startswith("【"):
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
            if "【" not in text and "<u>" not in text and "「" not in text:
                kanji_blocks = re.findall(r'[\u4e00-\u9faf]+', text)
                if len(kanji_blocks) == 1:
                    target = kanji_blocks[0]
                    text = text.replace(target, f"<u>{target}</u>", 1)
                    question["question_text"] = text
                elif len(kanji_blocks) > 1:
                    # Dùng pykakasi so khớp chữ Hán có cách đọc trùng với 1 trong các phương án
                    try:
                        import pykakasi
                        k = pykakasi.kakasi()
                        opt_texts = [o.get("option_text", "").strip() for o in question.get("options", []) if o.get("option_text")]
                        matched = False
                        for kb in kanji_blocks:
                            conv = "".join(item["hira"] for item in k.convert(kb)).strip()
                            if conv in opt_texts:
                                text = text.replace(kb, f"<u>{kb}</u>", 1)
                                question["question_text"] = text
                                matched = True
                                break
                        if not matched and kanji_blocks:
                            text = text.replace(kanji_blocks[0], f"<u>{kanji_blocks[0]}</u>", 1)
                            question["question_text"] = text
                    except Exception:
                        pass

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
