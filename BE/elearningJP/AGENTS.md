# 🤖 AGENTS.MD — HƯỚNG DẪN DỰ ÁN DÀNH CHO AI AGENT

> **File này được thiết kế để AI Agent (Antigravity, Claude, ChatGPT, Cursor, Copilot) đọc hiểu toàn bộ kiến trúc, luồng nghiệp vụ và quy ước lập trình của Backend E-Learning Tiếng Nhật (elearningJP) chỉ trong vài giây.**

---

## 📌 1. TỔNG QUAN HỆ THỐNG (SYSTEM OVERVIEW)

* **Tên dự án:** elearningJP (Backend Microservice / Monolith cho nền tảng học tiếng Nhật & Tra cứu từ điển).
* **Mục tiêu chính:** 
  1. Nền tảng học tiếng Nhật toàn diện: Lộ trình khóa học (Curriculum), Luyện thi JLPT (Exam), Ôn tập ngắt quãng (SRS/Flashcard), Gamification (Streak, Rank, Badges).
  2. Hệ thống tra cứu từ điển chuyên sâu kiểu **Mazii**: Tra từ vựng, tra Hán tự (Kanji), phân tích nét viết SVG (KanjiVG), gom nhóm âm On/Kun, tra cứu ngữ pháp, popup tra từ bôi đen.
* **Technology Stack:**
  * **Core:** Java 21 (LTS), Spring Boot 3.3.4
  * **Database:** PostgreSQL 16 (Migration bằng Flyway)
  * **Cache & Session:** Redis 7 (Alpine)
  * **Security:** Spring Security 6, Stateless JWT Token
  * **Storage/CDN:** Cloudinary (Lưu trữ ảnh & file phát âm MP3)
  * **Code Generation:** Lombok, MapStruct 1.5.5
  * **DevOps / Deployment:** Docker, Docker Compose

---

## 🗂️ 2. CẤU TRÚC THƯ MỤC & PACKAGE MAPPING

Mã nguồn tuân thủ kiến trúc phân tầng chuẩn (**Layered Architecture**):

`
d:\đồ án\BE\elearningJP\
├── src/main/java/com/jp/elearningjp/
│   ├── config/              # Cấu hình hệ thống (Security, Redis, Cloudinary, Init...)
│   ├── controller/          # REST API endpoints (Mapping DTO và trả về ApiResponse)
│   ├── dto/                 # Request & Response Data Transfer Objects
│   ├── entity/              # JPA Entities (Được phân theo Domain con)
│   │   ├── content/         # [TRỌNG TÂM] Từ điển, Kanji, Từ vựng, Nét chữ, Ngữ pháp
│   │   ├── curriculum/      # Khóa học, bài học, lộ trình học
│   │   ├── exam/            # Đề thi JLPT, câu hỏi, bài làm
│   │   ├── srs/             # Spaced Repetition System (Thẻ ghi nhớ Anki/SM-2)
│   │   ├── gamification/    # Điểm thưởng, chuỗi streak, huy hiệu, bảng xếp hạng
│   │   ├── handwriting/     # Đánh giá & nhận diện nét viết chữ Hán trên canvas
│   │   ├── community/       # Diễn đàn, bài viết, bình luận, like
│   │   ├── chat/            # Realtime chat / AI chat trợ giảng
│   │   └── user/            # Quản lý người dùng, phân quyền, xác thực
│   ├── mapper/              # MapStruct interfaces (Chuyển đổi Entity <-> DTO)
│   ├── repository/          # Spring Data JPA Repositories (Kế thừa JpaRepository)
│   ├── service/             # Business Logic (Interface + Implementation)
│   ├── exception/           # Custom Exceptions & GlobalExceptionHandler
│   └── shared/              # Base entities, Enums dùng chung, DTO dùng chung
├── src/main/resources/
│   ├── db/migration/        # Flyway SQL migrations (V1, V2, V3...)
│   └── application.yaml     # Cấu hình môi trường (hỗ trợ profile 'dev')
├── docker-compose.yml       # Khởi chạy Postgres (5432), Redis (6379), Backend (8080)
├── Dockerfile               # Multi-stage Docker build cho Spring Boot
└── pom.xml                  # Quản lý dependencies Maven
`

---

## 🧩 3. CÁC MODULE NGHIỆP VỤ CHÍNH (CORE DOMAINS)

### 3.1. Module Tra cứu Từ điển kiểu Mazii (entity/content/)
Đây là module cốt lõi cho tính năng từ điển:
* **writing_characters (WritingCharacter):** Lưu ký tự Kanji, Hiragana, Katakana: số nét (stroke_count), âm On (onyomi TEXT[]), âm Kun (kunyomi TEXT[]), âm Hán Việt (han_viet), cấp độ (jlpt_level), bộ thủ (
adicals).
* **character_strokes (CharacterStroke):** Lưu thứ tự nét (stroke_number) và đường vẽ vector SVG (svg_path) lấy chuẩn từ KanjiVG để vẽ hoạt ảnh thứ tự nét viết.
* **ocabulary (Vocabulary):** Từ vựng tiếng Nhật: word, cách đọc hiragana, 
omaji, âm han_viet, nghĩa tiếng Việt meaning_vi, cấp độ jlpt_level, link âm thanh udio_url.
* **ocabulary_characters (VocabularyCharacter):** Bảng liên kết Many-to-Many giữa Từ vựng và Chữ Hán.
  * 
eading_type: Loại âm đọc ('ONYOMI', 'KUNYOMI', 'OTHER').
  * 
eading: Âm đọc cụ thể (ví dụ: つき, ゲツ, ガツ).
  * 👉 **Quy tắc:** Khi tra 1 chữ Kanji (như 月), query bảng này để nhóm các từ vựng theo từng âm On/Kun y hệt giao diện Mazii.
* **ocabulary_senses / ocabulary_examples:** Hỗ trợ từ đa nghĩa và câu ví dụ song ngữ Nhật - Việt.
* **grammar_points (GrammarPoint):** Cấu trúc ngữ pháp, giải thích và mẫu câu.

### 3.2. Module Học tập & Lộ trình (entity/curriculum/)
* Phân cấp: **Khóa học (Course) ➔ Học phần/Module ➔ Bài học (Lesson) ➔ Đơn vị bài học (LessonUnit)**.
* Hỗ trợ theo dõi tiến độ học tập của từng học viên.

### 3.3. Module Ôn tập thông minh (entity/srs/)
* Ứng dụng thuật toán ngắt quãng (Spaced Repetition / SM-2) tương tự Anki.
* Lưu chu kỳ ôn tập, hệ số dễ nhớ (Ease Factor), khoảng cách ngày ôn (Interval) cho từ vựng và Kanji.

### 3.4. Module Gamification (entity/gamification/)
* Quản lý điểm kinh nghiệm (EXP), số ngày liên tục (Streak), bảng xếp hạng hàng tuần và huy hiệu (Badges).

---

## 🛡️ 4. QUY TẮC PHÁT TRIỂN & CODE CONVENTIONS BẮT BUỘC

Khi AI Agent tạo mới hoặc sửa mã nguồn, **BẮT BUỘC** tuân thủ các nguyên tắc sau:

1. **Soft Delete & Base Entity:**
   * Các Entity nghiệp vụ kế thừa từ SoftDeletableEntity.
   * Các trường cơ sở: deleted (boolean), deleted_at, created_at, updated_at, ersion.
   * **Mọi câu truy vấn Repository** phải lọc deleted = false (hoặc deletedFalse).
2. **Tránh lỗi N+1 Query:**
   * Khi truy vấn quan hệ @ManyToOne hoặc @OneToMany, ưu tiên sử dụng JOIN FETCH trong HQL/JPQL hoặc @EntityGraph.
3. **DTO & Validation:**
   * Không bao giờ nhận hoặc trả về trực tiếp JPA Entity qua REST Controller.
   * Sử dụng DTO phân loại rõ: ...Request và ...Response.
   * Sử dụng jakarta.validation.constraints (@NotBlank, @NotNull, @Size...) trong Request DTO.
4. **MapStruct & Lombok:**
   * Dùng MapStruct mapper cho việc chuyển đổi giữa Entity và DTO.
   * Dùng Lombok: @Getter, @Setter, @Builder, @NoArgsConstructor, @AllArgsConstructor.
5. **Database Migration với Flyway:**
   * **KHÔNG BAO GIỜ** sửa file migration cũ đã chạy trên production/database (V1, V2, V3).
   * Luôn tạo file migration mới với số thứ tự tăng dần: V{N}__<mo_ta_ngan_gon>.sql tại src/main/resources/db/migration/.
   * Đặt tên bảng dạng số nhiều chữ thường (snake_case), ví dụ: writing_characters, ocabulary_characters.

---

## 🚀 5. MÔI TRƯỜNG DOCKER & CÁC LỆNH VẬN HÀNH NHANH

* **Vị trí project:** d:\đồ án\BE\elearningJP
* **File docker-compose:** d:\đồ án\BE\elearningJP\docker-compose.yml

| Hành động | Lệnh Terminal |
| :--- | :--- |
| **Khởi động toàn bộ cụm:** | docker compose up -d |
| **Rebuild chỉ backend sau khi sửa code:** | docker compose up --build -d backend |
| **Xem log realtime của backend:** | docker logs -f elearning_backend |
| **Xem trạng thái các container:** | docker ps |
| **Container names:** | elearning_backend (8080), elearning_postgres (5432), elearning_redis (6379) |

---
*Tài liệu được sinh tự động và chuẩn hóa cho Agentic AI Development.*