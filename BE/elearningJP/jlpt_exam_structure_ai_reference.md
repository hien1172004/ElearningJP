# Tài liệu đặc tả cấu trúc đề thi JLPT
## Hướng dẫn cho AI đọc, phân loại câu hỏi và thiết kế hệ thống thi thử

- **Phiên bản:** 1.0
- **Ngày biên soạn:** 2026-10-16
- **Ngôn ngữ:** Tiếng Việt
- **Mục đích:** Làm tài liệu ngữ cảnh cho AI hỗ trợ xây dựng ngân hàng câu hỏi, chức năng thi thử và quản lý đề thi JLPT.
- **Nguồn đối chiếu chính:** Website JLPT chính thức: https://www.jlpt.jp/e/guideline/testsections.html và câu hỏi mẫu: https://www.jlpt.jp/e/samples/forlearners.html

> **Lưu ý quan trọng:** Tên dạng câu hỏi và số lượng câu có thể khác nhau theo cấp độ, phiên bản đề và cách phân nhóm. Không tự suy diễn rằng mọi dạng đều xuất hiện ở cả 5 cấp độ. Khi cần khẳng định cấu trúc chính thức, hãy kiểm tra lại website JLPT tại thời điểm sử dụng. JLPT không có phần thi nói hoặc viết tự luận trong bài thi chuẩn.

---

## 1. Tổng quan kỳ thi JLPT

JLPT (Japanese-Language Proficiency Test) có 5 cấp độ, từ dễ đến khó:

- **N5:** Cơ bản nhất.
- **N4:** Cơ bản.
- **N3:** Cấp độ trung gian.
- **N2:** Có thể hiểu tiếng Nhật trong nhiều tình huống đời sống và công việc.
- **N1:** Cấp độ cao nhất, yêu cầu hiểu tiếng Nhật phức tạp hơn.

### 1.1 Các nhóm bài thi

Về mặt năng lực, có thể phân nhóm nội dung thành:

1. **言語知識 — Kiến thức ngôn ngữ:** chữ viết, từ vựng và ngữ pháp.
2. **読解 — Đọc hiểu:** đọc văn bản và trả lời câu hỏi.
3. **聴解 — Nghe hiểu:** nghe âm thanh rồi trả lời câu hỏi.

Cách chia phiên thi khác nhau theo cấp độ:

- **N5, N4:** Chữ viết/từ vựng, ngữ pháp và đọc hiểu được tổ chức thành các phần thi theo cấu trúc dành cho cấp độ; từ vựng có thời lượng riêng.
- **N3:** Từ vựng; ngữ pháp và đọc hiểu; nghe hiểu.
- **N2, N1:** Kiến thức ngôn ngữ (chữ viết, từ vựng, ngữ pháp) và đọc hiểu nằm trong cùng bài thi; nghe hiểu là bài riêng.

Không nên nhầm **nhóm năng lực**, **phần/phiên thi**, **dạng câu hỏi** và **loại tài liệu**. Đây là bốn khái niệm khác nhau.

---

## 2. Phân biệt các khái niệm dùng trong hệ thống

### 2.1 Nhóm năng lực (`skill_section`)

Ví dụ:

- `LANGUAGE_KNOWLEDGE`
- `READING`
- `LISTENING`

### 2.2 Nhóm nội dung (`content_group`)

Ví dụ:

- `KANJI_VOCABULARY`
- `GRAMMAR`
- `READING_COMPREHENSION`
- `LISTENING_COMPREHENSION`

### 2.3 Dạng câu hỏi (`question_type`)

Đây là cách thí sinh phải giải quyết câu hỏi, ví dụ:

- Chọn cách đọc Kanji.
- Điền từ phù hợp.
- Sắp xếp cụm từ.
- Đọc đoạn văn và tìm thông tin.
- Nghe hội thoại và xác định hành động tiếp theo.

### 2.4 Loại tài liệu nguồn (`material_type`)

Ví dụ:

- `PLAIN_TEXT`
- `EMAIL`
- `CHAT_MESSAGE`
- `ADVERTISEMENT`
- `NOTICE`
- `SCHEDULE`
- `PRICE_TABLE`
- `RECEIPT`
- `FORM`
- `DIALOGUE_AUDIO`
- `ANNOUNCEMENT_AUDIO`
- `MONOLOGUE_AUDIO`

**Một tài liệu có thể được dùng cho nhiều câu hỏi.** Ví dụ, một email có thể có hai câu hỏi về thời gian và mục đích. Email là tài liệu nguồn, không mặc nhiên là một dạng câu hỏi riêng.

---

## 3. Chữ viết và từ vựng — 文字・語彙

Phần này kiểm tra khả năng đọc Kanji, hiểu nghĩa từ, chọn từ theo ngữ cảnh và sử dụng từ phù hợp.

### 3.1 `KANJI_READING` — Đọc cách đọc Kanji (漢字読み)

**Đầu vào:** Câu hoặc từ có Kanji được đánh dấu.

**Nhiệm vụ:** Chọn cách đọc đúng, thường bằng Hiragana.

**Ví dụ minh họa tự tạo:**

> 学校へ行きます。
>
> Từ được hỏi: 学校
>
> A. がっこう  
> B. がくこう  
> C. がっこ  
> D. がこう

Đáp án: A.

**Dữ liệu cần lưu:** từ đích, cách đọc đúng, các lựa chọn, giải thích, cấp độ.

### 3.2 `ORTHOGRAPHY` — Chọn cách viết đúng (表記)

**Nhiệm vụ:** Chọn cách viết Kanji/kana đúng cho từ được cho bằng cách đọc hoặc ngữ cảnh.

**Ví dụ minh họa tự tạo:**

> きょうしつで勉強します。
>
> Từ được hỏi: きょうしつ
>
> A. 教室  
> B. 強室  
> C. 京室  
> D. 教失

Đáp án: A.

### 3.3 `WORD_FORMATION` — Cấu tạo từ (語形成)

Kiểm tra thành phần cấu tạo, cách ghép hoặc biến đổi từ theo yêu cầu của câu hỏi. Dạng này gắn với cấu trúc từ vựng cấp độ cao, đặc biệt trong danh mục N1; không tự động áp dụng cho mọi cấp.

**Thiết kế:** Cho phép lưu từ/các thành tố và quy tắc giải thích. Cần lấy ví dụ từ nguồn đề hợp pháp, có kiểm chứng.

### 3.4 `CONTEXTUAL_VOCABULARY` — Chọn từ theo ngữ cảnh (文脈規定)

**Nhiệm vụ:** Chọn từ phù hợp với nghĩa toàn câu hoặc đoạn.

**Ví dụ minh họa tự tạo:**

> 毎朝、新聞を（　）。
>
> A. 読みます  
> B. 書きます  
> C. 作ります  
> D. 洗います

Đáp án: A.

**Chú ý:** Không chỉ so khớp từ điển; cần xét kết hợp từ, ngữ pháp và ngữ cảnh.

### 3.5 `PARAPHRASE_SYNONYM` — Từ gần nghĩa/diễn đạt tương đương (言い換え類義)

**Nhiệm vụ:** Chọn từ/cụm từ gần nghĩa nhất với từ hoặc cụm được đánh dấu.

Ví dụ minh họa: すぐに có thể gần nghĩa với ただちに trong ngữ cảnh phù hợp.

**Thiết kế:** Phải xét sắc thái, văn phong và ngữ cảnh; không coi các từ gần nghĩa là luôn thay thế được cho nhau.

### 3.6 `WORD_USAGE` — Cách dùng từ (用法)

**Nhiệm vụ:** Cho một từ và một số câu; chọn câu sử dụng từ đúng về nghĩa, kết hợp từ và ngữ pháp.

**Thiết kế:** Lưu từ mục tiêu, các câu lựa chọn, đáp án và lý do từng lựa chọn đúng/sai. Dạng này không có trong danh mục N5; hãy kiểm tra ma trận cấp độ trước khi gán.

---

## 4. Ngữ pháp — 文法

### 4.1 `GRAMMAR_FORM` — Chọn cấu trúc ngữ pháp (文法形式の判断)

Chọn mẫu ngữ pháp phù hợp để hoàn thành câu.

Ví dụ minh họa tự tạo:

> 日本へ行く（　）、パスポートを準備しました。
>
> A. ために  
> B. ほど  
> C. ばかり  
> D. しか

Đáp án dự kiến: A — diễn đạt mục đích. Khi dùng làm câu hỏi thực tế, cần kiểm tra độ tự nhiên và bảo đảm chỉ có một đáp án tốt nhất.

### 4.2 `SENTENCE_COMPOSITION` — Sắp xếp câu (文の組み立て)

Cho các cụm từ đã xáo trộn. Thí sinh phải xác định thứ tự tạo câu tự nhiên, đúng ngữ pháp.

Ví dụ:

- ① 日本語を
- ② 毎日
- ③ 勉強しています
- ④ 私は

Câu đúng: 私は毎日日本語を勉強しています。

**Yêu cầu dữ liệu:** Lưu các cụm, thứ tự đúng và cách hiển thị giao diện. Cần làm rõ đề yêu cầu chọn vị trí của cụm được đánh dấu hay sắp xếp toàn bộ câu, vì hình thức giao diện có thể khác.

### 4.3 `GRAMMAR_IN_CONTEXT` — Ngữ pháp trong đoạn văn (文章の文法)

Một đoạn văn có một hoặc nhiều chỗ trống. Chọn trợ từ, liên từ, cấu trúc hoặc cách diễn đạt phù hợp với cả câu và mạch văn.

Ví dụ minh họa:

> 雨が降っていました。（　）、試合は行われました。
>
> A. しかし  
> B. だから  
> C. つまり  
> D. それに

Đáp án: A — biểu thị quan hệ tương phản.

**Thiết kế:** Một đoạn văn có thể chứa nhiều chỗ trống; nên lưu đoạn văn riêng và liên kết từng câu hỏi với vị trí trống.

---

## 5. Đọc hiểu — 読解

Đọc hiểu sử dụng văn bản nguồn, sau đó yêu cầu thí sinh trả lời một hoặc nhiều câu hỏi. Các dạng cụ thể và cấp độ áp dụng không hoàn toàn giống nhau.

### 5.1 `SHORT_PASSAGE` — Đọc đoạn ngắn (短文)

**Tài liệu thường gặp:** Đoạn văn ngắn về đời sống, học tập, công việc hoặc tình huống đơn giản.

**Câu hỏi thường kiểm tra:**

- Một chi tiết cụ thể.
- Lý do hoặc kết quả.
- Ý nghĩa của một câu trong ngữ cảnh.
- Điều người viết muốn truyền đạt.

### 5.2 `MID_LENGTH_PASSAGE` — Đọc đoạn trung bình (中文)

Đọc một đoạn dài hơn, có nhiều câu liên kết.

**Câu hỏi thường kiểm tra:**

- Thông tin cụ thể.
- Quan hệ nguyên nhân–kết quả.
- Ý chính của đoạn.
- Cách hiểu một cụm từ hoặc phát biểu.

### 5.3 `LONG_PASSAGE` — Đọc đoạn dài (長文)

Đọc bài dài hơn, thường có lập luận, giải thích hoặc nhiều ý liên quan.

**Câu hỏi thường kiểm tra:**

- Chủ đề và ý chính.
- Chi tiết hỗ trợ.
- Mối quan hệ giữa các đoạn.
- Kết luận hoặc suy luận dựa trên văn bản.

**Lưu ý:** Đừng mặc định dạng này xuất hiện ở mọi cấp độ. Đối chiếu cấu trúc chính thức theo cấp độ.

### 5.4 `INTEGRATED_READING` — Đọc hiểu tổng hợp (統合理解)

Đọc và đối chiếu từ hai phần thông tin trở lên, hoặc nhiều quan điểm/tài liệu có liên hệ.

**Ví dụ tài liệu:**

- Hai ý kiến về một phương pháp học.
- Hai thông báo có điều kiện khác nhau.
- Một đề xuất và phần phản hồi.

**Câu hỏi thường kiểm tra:**

- Điểm giống và khác.
- Quan điểm của từng người.
- Kết luận có thể rút ra khi tổng hợp thông tin.
- Lựa chọn đáp án đáp ứng nhiều điều kiện.

### 5.5 `AUTHOR_CLAIM` — Hiểu quan điểm/chủ trương (主張理解)

Tập trung vào điều tác giả hoặc người nói muốn khẳng định, nhấn mạnh hoặc truyền đạt.

**Câu hỏi thường kiểm tra:**

- Tác giả muốn nói điều gì?
- Quan điểm chính là gì?
- Tác giả đánh giá vấn đề ra sao?
- Câu nào tóm tắt đúng lập luận?

Không nhầm dạng này với câu hỏi tìm một chi tiết đơn lẻ.

### 5.6 `INFORMATION_RETRIEVAL` — Tìm kiếm thông tin (情報検索)

Thí sinh đọc tài liệu thực tế và tìm thông tin thỏa điều kiện trong đề.

**Loại tài liệu nguồn có thể dùng:**

- Email.
- Tin nhắn.
- Thông báo.
- Quảng cáo.
- Lịch trình.
- Bảng giá.
- Bảng giờ mở cửa.
- Hướng dẫn đăng ký.
- Biểu mẫu hoặc thông tin dịch vụ.

**Câu hỏi thường kiểm tra:**

- Chọn lựa chọn phù hợp với thời gian.
- Tìm mức giá đáp ứng ngân sách.
- Xác định hạn đăng ký.
- Tìm địa điểm hoặc điều kiện phù hợp.
- Đối chiếu nhiều điều kiện đồng thời.

**Phân biệt quan trọng:** `EMAIL`, `ADVERTISEMENT`, `SCHEDULE`, `PRICE_TABLE` là `material_type`, còn `INFORMATION_RETRIEVAL` là `question_type`. Một quảng cáo không nhất thiết luôn thuộc dạng tìm kiếm thông tin; cách hỏi quyết định dạng câu hỏi.

---

## 6. Nghe hiểu — 聴解

Mỗi câu nghe thường liên kết tới một tệp âm thanh hoặc một mục âm thanh trong đề. Một audio có thể dùng cho một hoặc nhiều câu hỏi tùy cấu trúc đề.

### 6.1 `TASK_COMPREHENSION` — Hiểu nhiệm vụ (課題理解)

Nghe hội thoại/tình huống và xác định người nói cần làm gì hoặc sẽ làm gì tiếp theo.

**Ví dụ tình huống:** Khách hỏi nhân viên về thủ tục; thí sinh chọn hành động tiếp theo.

**Dữ liệu:** Audio, câu hỏi, lựa chọn, đáp án, transcript nội bộ nếu có quyền sử dụng.

### 6.2 `KEY_POINT_COMPREHENSION` — Hiểu trọng điểm (ポイント理解)

Đề bài yêu cầu chú ý một thông tin cụ thể trước hoặc trong khi nghe.

**Các thông tin có thể hỏi:**

- Thời gian.
- Địa điểm.
- Giá tiền.
- Lý do.
- Lựa chọn cuối cùng.
- Việc cần chuẩn bị.

Không được chỉ dựa vào từ khóa đơn lẻ; đáp án có thể được sửa đổi trong diễn biến hội thoại.

### 6.3 `GENERAL_COMPREHENSION` — Hiểu khái quát (概要理解)

Nghe một đoạn hội thoại hoặc bài nói để nắm nội dung, mục đích hoặc ý định tổng thể.

**Câu hỏi thường gặp:**

- Người nói đang nói về chủ đề gì?
- Mục đích của thông báo là gì?
- Người nói muốn truyền đạt điều gì?
- Thái độ hoặc ý định tổng thể là gì?

Dạng này không xuất hiện giống nhau ở tất cả các cấp độ; cần đối chiếu ma trận chính thức.

### 6.4 `VERBAL_EXPRESSION` — Chọn cách diễn đạt phù hợp (発話表現)

Đặt thí sinh vào một tình huống giao tiếp, đôi khi kèm tranh minh họa, rồi yêu cầu chọn câu nói phù hợp nhất.

**Ví dụ tình huống:** Muốn nhờ người khác mở cửa; chọn lời nhờ vả tự nhiên.

**Lưu ý triển khai:** Một câu hỏi có thể cần ảnh tình huống nhưng không nhất thiết cần audio dài. Cần lưu ảnh riêng và mô tả tình huống.

### 6.5 `QUICK_RESPONSE` — Phản hồi nhanh (即時応答)

Nghe một câu ngắn, sau đó chọn phản hồi tự nhiên nhất.

Ví dụ minh họa:

> 明日の会議、何時からですか。
>
> A. 10時からです。  
> B. 会議室です。  
> C. 昨日でした。

Đáp án: A.

**Yêu cầu:** Âm thanh câu hỏi và các lựa chọn (nếu đề phát lựa chọn bằng audio) phải được mô hình hóa phù hợp. Không mặc định mọi lựa chọn đều hiển thị bằng văn bản.

### 6.6 `INTEGRATED_LISTENING` — Nghe hiểu tổng hợp (統合理解)

Nghe thông tin dài hoặc nhiều lượt trao đổi, kết hợp chi tiết để trả lời.

**Câu hỏi có thể kiểm tra:**

- So sánh ý kiến.
- Tổng hợp lựa chọn.
- Xác định quyết định cuối cùng.
- Kết hợp thông tin từ nhiều người nói.

Dạng này thuộc cấu trúc cấp độ cao hơn theo danh mục JLPT; cần xác minh cấp độ trước khi dùng.

---

## 7. Ma trận dạng câu hỏi theo cấp độ

Bảng dưới đây là **ma trận định hướng sơ bộ**, không thay thế tài liệu chính thức. Dấu `✓` nghĩa là dạng được liệt kê trong cấu trúc tham chiếu; dấu `—` nghĩa là không được liệt kê trong ma trận tham chiếu. Trước khi dùng để sinh đề thi chuẩn, hãy xác minh từng hàng trên trang JLPT chính thức.

| Nhóm | `question_type` | Tên tiếng Nhật | N5 | N4 | N3 | N2 | N1 |
|---|---|---|:---:|:---:|:---:|:---:|:---:|
| Từ vựng | `KANJI_READING` | 漢字読み | ✓ | ✓ | ✓ | ✓ | ✓ |
| Từ vựng | `ORTHOGRAPHY` | 表記 | ✓ | ✓ | ✓ | ✓ | — |
| Từ vựng | `WORD_FORMATION` | 語形成 | — | — | — | — | ✓ |
| Từ vựng | `CONTEXTUAL_VOCABULARY` | 文脈規定 | ✓ | ✓ | ✓ | ✓ | ✓ |
| Từ vựng | `PARAPHRASE_SYNONYM` | 言い換え類義 | ✓ | ✓ | ✓ | ✓ | ✓ |
| Từ vựng | `WORD_USAGE` | 用法 | — | ✓ | ✓ | ✓ | ✓ |
| Ngữ pháp | `GRAMMAR_FORM` | 文法形式の判断 | ✓ | ✓ | ✓ | ✓ | ✓ |
| Ngữ pháp | `SENTENCE_COMPOSITION` | 文の組み立て | ✓ | ✓ | ✓ | ✓ | ✓ |
| Ngữ pháp | `GRAMMAR_IN_CONTEXT` | 文章の文法 | ✓ | ✓ | ✓ | ✓ | ✓ |
| Đọc | `SHORT_PASSAGE` | 短文 | ✓ | ✓ | ✓ | ✓ | ✓ |
| Đọc | `MID_LENGTH_PASSAGE` | 中文 | ✓ | ✓ | ✓ | ✓ | ✓ |
| Đọc | `LONG_PASSAGE` | 長文 | — | — | ✓ | — | ✓ |
| Đọc | `INTEGRATED_READING` | 統合理解 | — | — | — | ✓ | ✓ |
| Đọc | `AUTHOR_CLAIM` | 主張理解 | — | — | — | ✓ | ✓ |
| Đọc | `INFORMATION_RETRIEVAL` | 情報検索 | ✓ | ✓ | ✓ | ✓ | ✓ |
| Nghe | `TASK_COMPREHENSION` | 課題理解 | ✓ | ✓ | ✓ | ✓ | ✓ |
| Nghe | `KEY_POINT_COMPREHENSION` | ポイント理解 | ✓ | ✓ | ✓ | ✓ | ✓ |
| Nghe | `GENERAL_COMPREHENSION` | 概要理解 | — | — | ✓ | ✓ | ✓ |
| Nghe | `VERBAL_EXPRESSION` | 発話表現 | ✓ | ✓ | ✓ | — | — |
| Nghe | `QUICK_RESPONSE` | 即時応答 | ✓ | ✓ | ✓ | ✓ | ✓ |
| Nghe | `INTEGRATED_LISTENING` | 統合理解 | — | — | — | ✓ | ✓ |

**Không sử dụng ma trận này như quy tắc tuyệt đối nếu chưa kiểm chứng.** Nếu cần tính chính xác theo đề thi, nên quản lý danh mục dạng câu hỏi theo phiên bản cấu trúc và nguồn tham chiếu.

---

## 8. Thời lượng bài thi tham chiếu

Thời lượng có thể được cập nhật theo thông báo chính thức. Bảng sau là thời lượng tham chiếu thường được công bố:

| Cấp độ | Từ vựng/chữ viết | Ngữ pháp và đọc hiểu | Nghe hiểu |
|---|---:|---:|---:|
| N5 | 20 phút | 40 phút | 30 phút |
| N4 | 25 phút | 55 phút | 35 phút |
| N3 | 30 phút | 70 phút | 40 phút |
| N2 | Bài kiến thức ngôn ngữ + đọc hiểu: 105 phút | Gộp chung | 50 phút |
| N1 | Bài kiến thức ngôn ngữ + đọc hiểu: 110 phút | Gộp chung | 55 phút |

Nên lưu thời lượng ở cấu hình đề thi (`exam_blueprint` hoặc `exam_sections`), không hard-code rải rác trong frontend.

---

## 9. Mô hình dữ liệu gợi ý cho hệ thống thi thử

Đây là gợi ý thiết kế phần mềm, không phải schema chính thức của JLPT.

### 9.1 Các thực thể chính

#### `exam_blueprints`

Lưu cấu hình một loại đề thi.

Các trường gợi ý:

- `id`
- `name`
- `jlpt_level`: `N5`, `N4`, `N3`, `N2`, `N1`
- `structure_version`
- `duration_seconds`
- `is_official_replica`
- `source_reference`
- `status`

#### `exam_sections`

Lưu các phần/phiên thi trong blueprint.

- `id`
- `blueprint_id`
- `section_code`
- `display_name`
- `duration_seconds`
- `display_order`

Không giả định tất cả cấp độ có cùng số section.

#### `question_types`

Danh mục dạng câu hỏi.

- `id`
- `code`, ví dụ `KANJI_READING`
- `name_vi`
- `name_ja`
- `content_group`
- `description`
- `active`

Nên có phiên bản hoặc bảng liên kết cấp độ nếu một dạng chỉ áp dụng cho một số cấp.

#### `passages`

Lưu tài liệu nguồn cho đọc hiểu hoặc nhóm câu hỏi.

- `id`
- `material_type`
- `title`
- `body_text`
- `body_html` (nếu cần, phải kiểm soát HTML)
- `image_asset_id`
- `metadata_json`
- `source_license`
- `source_reference`

`body_text` nên giữ nội dung gốc. Không nên lưu ảnh quảng cáo dưới dạng một chuỗi văn bản không có bố cục nếu câu hỏi phụ thuộc vào vị trí, bảng hoặc hình ảnh.

#### `audio_assets`

Lưu thông tin audio.

- `id`
- `storage_key` hoặc `audio_url`
- `duration_ms`
- `mime_type`
- `transcript`
- `source_license`
- `source_reference`
- `checksum`

Ưu tiên lưu storage key và dùng URL có quyền truy cập phù hợp, thay vì để URL công khai cố định trong dữ liệu nếu hệ thống cần kiểm soát quyền.

#### `questions`

- `id`
- `question_type_id`
- `jlpt_level`
- `prompt_text`
- `passage_id` (nullable)
- `audio_asset_id` (nullable)
- `image_asset_id` (nullable)
- `question_order`
- `difficulty`
- `explanation`
- `status`
- `source_license`
- `source_reference`

#### `question_options`

- `id`
- `question_id`
- `option_label`
- `option_text`
- `image_asset_id` (nullable)
- `audio_asset_id` (nullable)
- `display_order`
- `is_correct`

Nếu có nhiều đáp án đúng, thay `is_correct` bằng mô hình answer key riêng hoặc hỗ trợ nhiều đáp án theo quy tắc của loại câu hỏi. Không phải mọi câu JLPT đều có đúng cùng một kiểu cấu trúc lựa chọn; cần thiết kế linh hoạt.

#### `exam_blueprint_questions`

Liên kết câu hỏi với đề mẫu.

- `blueprint_id`
- `question_id`
- `section_id`
- `question_order`
- `score_weight`
- `group_id` (nếu câu hỏi thuộc một nhóm tài liệu/audio)

#### `exam_attempts`

Lưu lượt thi của người dùng.

- `id`
- `user_id`
- `blueprint_id`
- `started_at`
- `submitted_at`
- `status`
- `raw_score`
- `result_json`

#### `exam_attempt_answers`

- `id`
- `attempt_id`
- `question_id`
- `selected_option_id` hoặc `answer_json`
- `is_correct`
- `earned_score`
- `answered_at`
- `time_spent_ms`

`answer_json` hữu ích với dạng sắp xếp câu hoặc tương tác đặc biệt. Cần kiểm tra dữ liệu và giới hạn schema để tránh dữ liệu tùy ý.

### 9.2 Quan hệ tài liệu–câu hỏi

Một tài liệu đọc có thể có nhiều câu hỏi:

`passages 1 ─── N questions`

Một audio có thể có nhiều câu hỏi:

`audio_assets 1 ─── N questions`

Một đề có nhiều câu hỏi và một câu hỏi có thể được dùng lại trong nhiều đề:

`exam_blueprints N ─── N questions` thông qua `exam_blueprint_questions`.

Nếu một nhóm câu hỏi dùng chung audio hoặc passage, giữ liên kết dùng chung; không sao chép nội dung nguồn vào từng câu.

### 9.3 Lưu lựa chọn và đáp án

- Đáp án đúng không nên được gửi về client trước khi nộp bài nếu muốn hạn chế gian lận.
- Có thể tách `question_options` và `question_answer_keys` để kiểm soát quyền truy cập.
- Lưu lời giải tiếng Việt, lý do đáp án đúng và giải thích vì sao các lựa chọn nhiễu sai.
- Với câu hỏi do AI tạo, thêm `generated_by_ai`, `review_status`, `reviewed_by`, `reviewed_at`.
- Không coi nội dung AI tạo là chuẩn JLPT nếu chưa được người có năng lực tiếng Nhật rà soát.

---

## 10. Xử lý audio nghe hiểu dài

Đối với đề nghe có một tệp dài hoặc nhiều câu liên tiếp, không nên cắt audio tùy ý chỉ theo thời lượng. Hãy mô hình hóa audio và các đoạn theo timestamp.

### 10.1 Bảng `audio_segments` (tùy chọn)

- `id`
- `audio_asset_id`
- `segment_order`
- `start_ms`
- `end_ms`
- `segment_type`
- `transcript`
- `group_id`

`segment_type` có thể là `INSTRUCTION`, `QUESTION`, `DIALOGUE`, `CHOICES`, `PAUSE` tùy dữ liệu thực tế.

### 10.2 Quy trình xử lý đề audio

1. Lưu audio nguồn hợp pháp và checksum.
2. Nghe/kiểm tra audio để xác định ranh giới thực tế.
3. Ghi timestamp bắt đầu/kết thúc cho từng đoạn.
4. Liên kết câu hỏi với audio nguồn và đoạn liên quan.
5. Chỉ cắt thành file riêng khi thực sự cần; có thể phát đoạn theo timestamp.
6. Kiểm tra tiếng đầu/cuối, khoảng lặng và việc cắt có làm mất ngữ cảnh không.
7. Lưu transcript nếu được phép sử dụng; transcript không nhất thiết phải hiển thị cho thí sinh.
8. Tuân thủ bản quyền và điều khoản của nguồn đề.

Công cụ VAD như Silero VAD có thể giúp phát hiện vùng có tiếng nói và khoảng lặng, nhưng **không tự xác định chính xác ranh giới câu hỏi JLPT**. Cần kết hợp timestamp, nghe kiểm tra và cấu trúc đề.

### 10.3 Ví dụ dữ liệu timestamp

```json
{
  "audio_asset_id": "audio_001",
  "segments": [
    {
      "segment_order": 1,
      "start_ms": 0,
      "end_ms": 4200,
      "segment_type": "INSTRUCTION"
    },
    {
      "segment_order": 2,
      "start_ms": 4200,
      "end_ms": 12800,
      "segment_type": "DIALOGUE"
    }
  ]
}
```

Các timestamp ở trên chỉ minh họa định dạng, không phải timestamp của một đề JLPT thực tế.

---

## 11. Xử lý bài đọc có hình ảnh và bố cục

Với email, quảng cáo, hóa đơn, bảng giá hoặc lịch trình:

- Lưu ảnh gốc hoặc HTML có cấu trúc nếu có quyền sử dụng.
- Lưu bản text đã trích xuất để tìm kiếm và hỗ trợ tiếp cận.
- Giữ bảng thành bảng, không nối mọi ô thành một chuỗi văn bản.
- Lưu thứ tự đọc, nhãn cột/hàng và đơn vị tiền/thời gian.
- Cho phép một tài liệu có cả text và image.
- Câu hỏi nên liên kết đến tài liệu nguồn, còn phần prompt chứa yêu cầu cụ thể.

Nếu dùng OCR để số hóa tài liệu, phải rà soát ký tự tiếng Nhật, Kanji tương tự nhau, dấu câu, chữ nhỏ và các cột trong bảng. OCR có thể đọc sai ngày, giờ hoặc giá tiền, dẫn đến đáp án sai.

---

## 12. Quy trình sinh câu hỏi bằng AI

AI có thể hỗ trợ tạo câu hỏi luyện tập, nhưng nên tách quy trình thành các bước kiểm soát:

1. **Chọn cấp độ:** N5–N1.
2. **Chọn nhóm nội dung và dạng câu hỏi:** Ví dụ N4 + `CONTEXTUAL_VOCABULARY`.
3. **Chọn mục tiêu học:** Từ vựng, Kanji, mẫu ngữ pháp hoặc kỹ năng đọc/nghe.
4. **Tạo nội dung:** Prompt, tài liệu nguồn nếu có, lựa chọn và đáp án dự kiến.
5. **Kiểm tra cấu trúc:** Đủ trường, đúng loại dữ liệu, đúng số lựa chọn theo blueprint.
6. **Kiểm tra ngôn ngữ:** Ngữ pháp, cách dùng từ, độ tự nhiên, mức độ phù hợp cấp độ.
7. **Kiểm tra đáp án:** Chỉ có một đáp án tốt nhất nếu loại câu hỏi yêu cầu một đáp án.
8. **Kiểm tra giải thích:** Giải thích phải hỗ trợ đáp án, không mâu thuẫn.
9. **Kiểm tra nguồn và bản quyền:** Không sao chép đề có bản quyền nếu không được phép.
10. **Duyệt bởi người có chuyên môn:** Chuyển câu hỏi sang trạng thái `REVIEW_REQUIRED` hoặc `APPROVED`.

### 12.1 JSON minh họa cho câu hỏi trắc nghiệm

```json
{
  "question_type": "CONTEXTUAL_VOCABULARY",
  "jlpt_level": "N4",
  "prompt_text": "毎朝、新聞を（　）。",
  "options": [
    { "label": "A", "text": "読みます" },
    { "label": "B", "text": "書きます" },
    { "label": "C", "text": "作ります" },
    { "label": "D", "text": "洗います" }
  ],
  "correct_option_label": "A",
  "explanation_vi": "「新聞を読む」là cách kết hợp từ tự nhiên, nghĩa là đọc báo.",
  "passage_id": null,
  "audio_asset_id": null,
  "review_status": "REVIEW_REQUIRED"
}
```

Đây là JSON minh họa để thiết kế hệ thống, không phải câu hỏi chính thức từ JLPT.

### 12.2 Prompt mẫu cho AI tạo câu hỏi

```text
Bạn là trợ lý thiết kế câu hỏi luyện thi JLPT.

Hãy tạo câu hỏi luyện tập mới, không sao chép nguyên văn đề thi có bản quyền.

Đầu vào:
- Cấp độ: {JLPT_LEVEL}
- Nhóm nội dung: {CONTENT_GROUP}
- Dạng câu hỏi: {QUESTION_TYPE}
- Chủ điểm: {TOPIC}
- Số câu: {COUNT}

Yêu cầu:
1. Nội dung và từ vựng phù hợp cấp độ được chỉ định.
2. Mỗi câu có prompt rõ ràng và các lựa chọn không mơ hồ.
3. Chỉ có một đáp án tốt nhất nếu dạng câu hỏi là trắc nghiệm một đáp án.
4. Giải thích đáp án bằng tiếng Việt, nêu lý do và điểm ngôn ngữ liên quan.
5. Kiểm tra lỗi chính tả tiếng Nhật, Kanji, kana, trợ từ và ngữ pháp.
6. Không khẳng định câu hỏi là đề JLPT chính thức.
7. Trả về JSON hợp lệ theo schema được cung cấp.
8. Nếu không chắc về tính tự nhiên hoặc độ khó, đánh dấu REVIEW_REQUIRED.
```

---

## 13. Chấm điểm và kết quả thi

- Không nên coi số câu đúng là điểm JLPT chính thức.
- JLPT sử dụng hệ thống tính điểm theo thang điểm được quy định cho từng cấp độ; điểm thô và điểm báo cáo không nhất thiết tương đương.
- Nếu hệ thống tự tạo đề, hãy ghi rõ kết quả là **điểm luyện tập nội bộ**, trừ khi có cơ chế chấm điểm được kiểm chứng.
- Nên báo cáo theo kỹ năng: từ vựng, ngữ pháp, đọc hiểu, nghe hiểu.
- Có thể thống kê thời gian làm bài, tỷ lệ đúng theo dạng câu hỏi, câu bỏ trống và tiến bộ theo lần thi.
- Không suy ra khả năng đỗ JLPT chính thức chỉ từ một đề luyện tập nhỏ.
- Với đề mô phỏng, cấu hình ngưỡng và trọng số phải được ghi rõ và không quảng bá như chuẩn chính thức nếu chưa được xác minh.

---

## 14. Các lỗi thiết kế cần tránh

1. **Gộp mọi loại tài liệu vào `question_type`:** Email, quảng cáo và audio là tài liệu nguồn; dạng câu hỏi là nhiệm vụ mà thí sinh phải thực hiện.
2. **Coi mọi cấp độ có cùng cấu trúc:** Mỗi cấp độ có danh mục dạng câu hỏi và thời lượng riêng.
3. **Lưu audio dài trực tiếp vào từng câu:** Dẫn đến trùng lặp và khó đồng bộ; hãy dùng audio asset + timestamp/segment.
4. **Cắt audio chỉ theo khoảng lặng:** Khoảng lặng không đảm bảo là ranh giới câu hỏi.
5. **Lưu đáp án đúng ở client trước khi nộp:** Tăng nguy cơ lộ đáp án.
6. **Để AI tự duyệt câu hỏi của chính nó:** Cần kiểm tra logic, độ tự nhiên và đáp án, tốt nhất có người rà soát.
7. **Dùng ảnh nhưng không có bản text:** Khó tìm kiếm, khó tiếp cận và khó kiểm tra OCR.
8. **Dùng nội dung đề thi không rõ giấy phép:** Cần xác minh quyền sử dụng trước khi đưa vào sản phẩm.
9. **Nhầm điểm luyện tập với điểm JLPT:** Cần nêu rõ giới hạn.
10. **Dùng ví dụ minh họa như câu chính thức:** Ví dụ trong tài liệu này được tạo để giải thích khái niệm, không phải trích đề thi chính thức.

---

## 15. Checklist triển khai

### Phân loại nội dung

- [ ] Có đủ 5 cấp độ N5–N1.
- [ ] Phân biệt nhóm năng lực, nhóm nội dung, dạng câu hỏi và loại tài liệu.
- [ ] Có bảng cấu hình dạng câu hỏi theo từng cấp độ.
- [ ] Có phiên bản cấu trúc đề để cập nhật khi JLPT thay đổi.

### Ngân hàng câu hỏi

- [ ] Có prompt, lựa chọn, đáp án và lời giải.
- [ ] Có cấp độ, dạng câu hỏi, độ khó và trạng thái duyệt.
- [ ] Hỗ trợ câu hỏi liên kết với passage/audio/image.
- [ ] Có kiểm tra dữ liệu đầu vào và phát hiện câu hỏi trùng.
- [ ] Có trường nguồn và giấy phép sử dụng.

### Thi thử

- [ ] Cấu hình thời gian theo cấp độ.
- [ ] Hỗ trợ lưu tiến trình và tiếp tục làm bài nếu sản phẩm cần.
- [ ] Không tiết lộ đáp án trước khi nộp.
- [ ] Tính điểm luyện tập rõ ràng.
- [ ] Có báo cáo kết quả theo kỹ năng và dạng câu hỏi.

### Audio và hình ảnh

- [ ] Hỗ trợ audio dùng chung cho nhiều câu.
- [ ] Có timestamp/segment khi cần.
- [ ] Kiểm tra audio trước và sau khi cắt.
- [ ] Hỗ trợ tài liệu đọc dạng ảnh/bảng.
- [ ] Kiểm tra OCR và khả năng hiển thị trên điện thoại.

### AI

- [ ] AI tạo dữ liệu theo JSON schema.
- [ ] Kiểm tra tính hợp lệ của JSON.
- [ ] Kiểm tra đáp án duy nhất và giải thích.
- [ ] Có trạng thái duyệt thủ công.
- [ ] Không gọi câu hỏi do AI tạo là câu hỏi chính thức.

---

## 16. Nguồn tham khảo

1. **JLPT — Test Sections and Test Times**  
   https://www.jlpt.jp/e/guideline/testsections.html
2. **JLPT — Sample Questions for Learners**  
   https://www.jlpt.jp/e/samples/forlearners.html
3. **JLPT — Official website**  
   https://www.jlpt.jp/e/

Ưu tiên nguồn chính thức khi xác minh thời lượng, cấu trúc và dạng câu hỏi. Các ví dụ tiếng Nhật, schema cơ sở dữ liệu và prompt AI trong tài liệu này là nội dung minh họa do người biên soạn tạo ra, không phải đề thi chính thức.
