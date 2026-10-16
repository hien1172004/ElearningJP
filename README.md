# 🇯🇵 ElearningJP - Hệ Thống Học Tiếng Nhật Trực Tuyến

> Dự án E-learning học tiếng Nhật thông minh tích hợp phương pháp lặp lại ngắt quãng (Spaced Repetition System - SRS) và lộ trình chuẩn hóa JLPT (N5 - N1).

---

## 📌 1. TỔNG QUAN TIẾN ĐỘ DỰ ÁN (PROJECT STATUS)

Dự án được xây dựng theo kiến trúc **Monorepo** với 2 phần chính:
- **Backend**: Spring Boot 3.3.4, Java 21, PostgreSQL, Flyway, Redis, Spring Security & JWT.
- **Frontend**: React 19, TypeScript, Vite, Tailwind CSS v4, Zustand, TanStack Query.

| STT | Phân hệ / Module | Công việc đã hoàn thành | Trạng thái |
| :---: | :--- | :--- | :---: |
| **1** | **Xác thực & Người dùng (Auth)** | • Đăng ký, đăng nhập JWT, phân quyền (ADMIN, TEACHER, STUDENT).<br>• Quản lý thông tin cá nhân, đổi mật khẩu, Refresh Token. | ✅ **100% Hoàn thành** |
| **2** | **Thư viện Từ điển (Dictionary)** | • CSDL Từ vựng (Vocabulary), Hán tự (Kanji), Điểm ngữ pháp (Grammar).<br>• Migration cơ sở dữ liệu `V1` và `V2`. | ✅ **100% Hoàn thành** |
| **3** | **Khóa học & Bài học (Curriculum)** | • Quản lý Khóa học CRUD, phân trang, lọc theo JLPT.<br>• Quản lý Bài học, giáo trình Markdown, kiểm soát bản quyền bài giảng.<br>• Tính toán tự động tiến độ học tập (% hoàn thành khóa học). | ✅ **100% Hoàn thành** |
| **4** | **Kiểm tra trắc nghiệm (Lesson Quiz)** | • Bài kiểm tra trắc nghiệm 4 lựa chọn (A, B, C, D) sau mỗi bài học.<br>• Chống gian lận F12 (ẩn đáp án đúng ở client).<br>• Chấm điểm tự động trên Server, vượt qua $\ge 80\%$ hoàn thành bài học.<br>• Migration Flyway `V3__create_lesson_quizzes.sql`. | ✅ **100% Hoàn thành** |
| **5** | **Thuật toán lặp lại ngắt quãng (SRS)** | • Triển khai giải thuật toán học **SM-2 (SuperMemo-2 / Anki)**.<br>• Tự động tính toán hệ số dễ dãi ($EF$), chu kỳ giãn cách ngày ôn ($Interval$) theo 4 mức: `AGAIN`, `HARD`, `GOOD`, `EASY`.<br>• **Tự động liên kết Quiz $\rightarrow$ SRS**: Câu hỏi làm sai trong bài kiểm tra sẽ tự động vào sổ tay SRS để bắt buộc ôn lại vào ngày hôm sau.<br>• Migration Flyway `V4__add_srs_indexes.sql` (Composite Index tối ưu hàng triệu bản ghi). | ✅ **100% Hoàn thành** |

---

## 🔄 2. LUỒNG HOẠT ĐỘNG NGHIỆP VỤ (CORE WORKFLOWS)

### 🌊 Luồng 1: Đăng ký & Học bài có bảo vệ bản quyền
```
[Học viên xem Khóa học] 
       │
       ▼
[Chưa đăng ký] ──► Chỉ xem được Tổng quan & Đề cương tiêu đề bài học
       │
       ▼ (Bấm Enroll)
[Đã đăng ký]   ──► Mở khóa toàn bộ nội dung bài giảng Markdown chi tiết
                   Hệ thống tự động ghi nhận trạng thái: IN_PROGRESS
```

### 🌊 Luồng 2: Làm bài kiểm tra trắc nghiệm cuối bài (Lesson Quiz)
```
[Bắt đầu làm Quiz] ──► Nhận đề thi (Ẩn tuyệt đối isCorrect chống soi F12)
       │
       ▼
[Nộp bài trắc nghiệm (A, B, C, D)]
       │
       ├─► Server so khớp đáp án bí mật & tính điểm %
       │
       ├─► Nếu Điểm >= 80%: Đánh dấu bài học COMPLETED -> Tăng % tiến độ khóa học
       │
       └─► Trả về kết quả: Đúng/Sai từng câu kèm lời giải thích (Explanation)
```

### 🌊 Luồng 3: Thuật toán SRS & Tự động đồng bộ từ Quiz
```
                    [Học viên nộp bài Quiz]
                              │
               ┌──────────────┴──────────────┐
               ▼                             ▼
       [Làm SAI câu hỏi]             [Làm ĐÚNG câu hỏi]
               │                             │
    Đánh giá: AGAIN (q = 1)          Đo thời gian phản xạ:
    Tự động đưa vào SRS:             • < 3s   : EASY (q = 5)
    • Reset chu kỳ = 1 ngày          • 3s-15s : GOOD (q = 4)
    • Hạ hệ số dễ dãi (EF)           • > 15s  : HARD (q = 3)
               │                             │
               └──────────────┬──────────────┘
                              ▼
                 [Cỗ máy thuật toán SM-2]
                 • Tính toán Interval mới
                 • Tính toán EF mới
                 • Lên lịch: next_review_at
                              │
                              ▼
    [Ngày hôm sau: Mở App vào mục "Ôn tập hàng ngày"]
    Hệ thống lọc các từ có next_review_at <= NOW để học viên lật Flashcard!
```

---

## 📡 3. DANH SÁCH TẤT CẢ CÁC REST API ĐÃ TRIỂN KHAI

### I. Quản lý Khóa học & Bài học (`/api/v1/courses`, `/api/v1/lessons`)
| Phương thức | Endpoint | Quyền hạn | Mô tả |
| :---: | :--- | :--- | :--- |
| `GET` | `/api/v1/courses` | Public | Danh sách khóa học công khai (phân trang, lọc theo JLPT N5 - N1). |
| `GET` | `/api/v1/courses/{id}` | Public / Auth | Chi tiết khóa học & outline bài giảng. Báo trạng thái đăng ký & % tiến độ. |
| `POST` | `/api/v1/courses/{id}/enroll` | Auth | Học viên bấm đăng ký khóa học. |
| `GET` | `/api/v1/courses/{id}/progress` | Auth | Xem chi tiết tiến độ học tập trong khóa học. |
| `GET` | `/api/v1/courses/admin` | ADMIN | Danh sách toàn bộ khóa học (bao gồm bản nháp). |
| `POST` | `/api/v1/courses` | ADMIN | Tạo khóa học mới (tự động tạo slug thân thiện). |
| `PUT` | `/api/v1/courses/{id}` | ADMIN | Chỉnh sửa thông tin khóa học. |
| `DELETE` | `/api/v1/courses/{id}` | ADMIN | Xóa mềm khóa học (`softDelete`). |
| `PATCH` | `/api/v1/courses/{id}/publish` | ADMIN | Bật/Tắt trạng thái xuất bản khóa học. |
| `GET` | `/api/v1/lessons/{id}` | Auth (Enrolled) | Xem bài giảng Markdown (chặn `403` nếu chưa đăng ký khóa học). |
| `POST` | `/api/v1/courses/{id}/lessons` | ADMIN | Thêm bài học mới vào khóa học. |
| `PUT` | `/api/v1/lessons/{id}` | ADMIN | Chỉnh sửa nội dung bài giảng. |
| `DELETE` | `/api/v1/lessons/{id}` | ADMIN | Xóa mềm bài học. |
| `POST` | `/api/v1/lessons/{id}/complete` | Auth | Đánh dấu hoàn thành bài học thủ công (cho bài lý thuyết). |

### II. Kiểm tra trắc nghiệm cuối bài (`QuizController`)
| Phương thức | Endpoint | Quyền hạn | Mô tả |
| :---: | :--- | :--- | :--- |
| `GET` | `/api/v1/lessons/{id}/quiz` | Auth (Enrolled) | Lấy đề kiểm tra 4 phương án A/B/C/D (ẩn đáp án đúng chống F12). |
| `POST` | `/api/v1/lessons/{id}/quiz/submit` | Auth | Nộp bài, chấm điểm tự động, cập nhật tiến độ và đồng bộ SRS. |
| `POST` | `/api/v1/lessons/{id}/quiz/questions` | ADMIN | Admin thêm câu hỏi trắc nghiệm kèm 4 lựa chọn. |
| `DELETE` | `/api/v1/quiz/questions/{id}` | ADMIN | Xóa mềm câu hỏi trắc nghiệm khỏi đề thi. |

### III. Hệ thống lặp lại ngắt quãng SRS (`SrsController`)
| Phương thức | Endpoint | Quyền hạn | Mô tả |
| :---: | :--- | :--- | :--- |
| `GET` | `/api/v1/srs/due` | Auth | Lấy danh sách Flashcard cần ôn hôm nay (`nextReviewAt <= NOW`). |
| `POST` | `/api/v1/srs/items` | Auth | Thêm một từ vựng / Kanji / Ngữ pháp lẻ vào sổ tay SRS. |
| `POST` | `/api/v1/srs/lessons/{id}/add-all`| Auth | Thêm nhanh toàn bộ từ vựng trong bài học vào SRS. |
| `POST` | `/api/v1/srs/batch` | Auth | Thêm hàng loạt từ theo danh sách ID. |
| `POST` | `/api/v1/srs/items/{id}/review` | Auth | Nộp đánh giá ôn tập Flashcard (Again, Hard, Good, Easy) -> chạy SM-2. |
| `GET` | `/api/v1/srs/stats` | Auth | Xem thống kê số thẻ đến hạn hôm nay, đang học, đã thành thạo. |

---

## 🗄️ 4. CƠ SỞ DỮ LIỆU & MIGRATION (DATABASE)

Dự án sử dụng **Flyway** để quản lý phiên bản cơ sở dữ liệu có kiểm soát:
- `V1__init_schema.sql`: Khởi tạo toàn bộ các bảng hệ thống (Users, Curriculum, Content, Gamification, Chat,...).
- `V2__add_dictionary_expansion.sql`: Bổ sung mở rộng các bảng cho Thư viện Từ điển tiếng Nhật.
- `V3__create_lesson_quizzes.sql`: Tạo các bảng phục vụ bài kiểm tra trắc nghiệm (`lesson_quizzes`, `quiz_questions`, `quiz_options`).
- `V4__add_srs_indexes.sql`: Đánh Composite Partial Index (`idx_srs_items_user_due`) và Unique Constraints cho hệ thống SRS nhằm tối ưu truy vấn dưới 5ms cho hàng chục triệu bản ghi.

---

## 🚀 5. HƯỚNG DẪN KHỞI CHẠY BACKEND

### Yêu cầu môi trường:
- Java JDK 21+
- PostgreSQL 15+
- Redis Server (tùy chọn cho cache token)

### Các bước khởi chạy:
1. Clone dự án và cấu hình biến môi trường trong file `.env` hoặc `application.properties`:
   ```properties
   spring.datasource.url=jdbc:postgresql://localhost:5432/elearningjp
   spring.datasource.username=postgres
   spring.datasource.password=your_password
   ```
2. Mở terminal tại thư mục `BE/elearningJP`:
   ```powershell
   mvn clean compile
   mvn spring-boot:run
   ```
3. Truy cập Swagger UI để kiểm thử trực quan các API:
   👉 `http://localhost:8080/swagger-ui/index.html`
