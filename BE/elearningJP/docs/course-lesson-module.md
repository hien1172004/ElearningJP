# Báo cáo Module Course & Lesson — ElearningJP

> Phân tích tổng quan về luồng hoạt động, kiến trúc, dữ liệu và API của module
> **Course (khóa học) – Lesson (bài học)** trong dự án `ElearningJP`.
>
> - **Frontend (FE):** `FE/src/...` — React + TypeScript, Vite, React Query, Ant Design.
> - **Backend (BE):** `BE/elearningJP/src/main/java/com/jp/elearningjp/...` — Spring Boot, package nội bộ gọi là `curriculum`.

---

## 1. Tổng quan & phạm vi

Module này phụ trách toàn bộ vòng đời của **khóa học tiếng Nhật theo cấp độ JLPT**, từ lúc giáo viên tạo khóa học → biên soạn bài học (gồm Hán tự – Từ vựng – Ngữ pháp – Quiz) → đến lúc học viên đăng ký, học, làm bài và theo dõi tiến độ.

### Các tính năng chính

| Vai trò | Tính năng |
|---|---|
| **Public (khách)** | Xem danh sách khóa học công khai, xem chi tiết khóa học. |
| **Học viên (Student)** | Đăng ký (enroll), học bài (xem Kanji/Vocab/Grammar, làm Quiz), theo dõi tiến độ, hoàn thành bài. |
| **Giáo viên / Admin (TEACHER, ADMIN)** | CRUD khóa học, xuất bản/nháp, CRUD bài học, sắp xếp thứ tự, quản lý Section (Kanji/Vocab/Grammar) và Quiz. |

### Cấu trúc URL (FE)

| Đường dẫn | Trang | Quyền |
|---|---|---|
| `/courses` | `CourseCatalogPage` | Public |
| `/courses/:id` | `CourseDetailPage` | Public |
| `/learn/:courseSlug/:lessonSlug` | `LessonViewerPage` | Protected (đã đăng nhập) |
| `/teacher/courses` | `TeacherCoursesPage` | TEACHER / ADMIN |
| `/teacher/courses/:courseId` | `TeacherCourseEditorPage` | TEACHER / ADMIN |

---

## 2. Kiến trúc tổng thể

```
┌──────────────────────────────────────────────────────────────────────────┐
│                              FRONTEND (React)                            │
│                                                                          │
│  pages/courses/                                                          │
│    ├─ CourseCatalogPage          (Public - danh sách)                    │
│    ├─ CourseDetailPage           (Public - chi tiết)                     │
│    ├─ learn/LessonViewerPage     (Student - xem bài học)                 │
│    └─ teacher/                                                        ── │
│        ├─ TeacherCoursesPage     (Teacher - quản lý khóa học)            │
│        └─ TeacherCourseEditorPage(Teacher - biên soạn bài học)            │
│                                                                          │
│  components/course/                                                      │
│    ├─ course/                  (Card, FormModal, FilterBar)              │
│    ├─ lesson-viewer/           (KanjiStudy, VocabStudy, GrammarStudy,    │
│    │                            QuizStudy, SectionRenderer, NavSidebar)  │
│    ├─ section-editor/          (SectionList, Kanji/Vocab/Grammar editor)  │
│    └─ quiz-editor/             (QuizPanel, MatchingPairsEditor)          │
│                                                                          │
│  hooks/             (React Query: useCourses, useCourseDetail,           │
│                       useLessonDetail, useStartLesson, useCompleteLesson)│
│  services/          (axiosClient gọi REST)                               │
│  types/             (course.types.ts)                                    │
│  stores/            (chỉ dùng useAuthStore lấy user)                     │
└──────────────────────────────────────────────────────────────────────────┘
                                │  HTTP/JSON
                                ▼
┌──────────────────────────────────────────────────────────────────────────┐
│                          BACKEND (Spring Boot)                           │
│                                                                          │
│  controller/curriculum/                                                  │
│    CourseController            /api/v1/courses                           │
│    EnrollmentController        /api/v1/enrollments                      │
│    LessonController            /api/v1/lessons                           │
│    LessonSectionController     /api/v1/lessons/{id}/sections             │
│    LessonKanjiItemController   /api/v1/lesson-kanji-items/...            │
│    LessonVocabularyItemController /api/v1/lesson-vocabulary-items/...     │
│    LessonGrammarItemController /api/v1/lesson-grammar-items/...          │
│    QuizController              /api/v1/lessons/{id}/quiz[/*]             │
│    ProgressController          /api/v1/progress/lessons/{id}/start        │
│                                                                          │
│  service/curriculum/                                                     │
│    CourseService, EnrollmentService, LessonService, ...                  │
│    section/  (Section, KanjiItem, VocabItem, GrammarItem services)       │
│                                                                          │
│  entity/curriculum/  (Course, CourseEnrollment, Lesson, LessonSection,    │
│                       LessonKanjiItem, LessonVocabularyItem,              │
│                       LessonGrammarItem, Quiz, QuizQuestion, QuizOption,  │
│                       UserLessonProgress, ...)                           │
│                                                                          │
│  repository/curriculum/  (JPA Repositories)                              │
│  mapper/curriculum/      (MapStruct)                                     │
│  dto/request,response/   (curriculum/*)                                  │
└──────────────────────────────────────────────────────────────────────────┘
```

> Lưu ý: **FE** gọi nhóm tài nguyên là "Course/Lesson", trong khi **BE** đặt tên package là `curriculum`. Hai bên cùng nói về một thực thể nghiệp vụ.

---

## 3. Mô hình dữ liệu (BE — JPA entities)

### 3.1 Sơ đồ quan hệ

```
┌────────────┐ 1   N ┌────────────┐ 1   N ┌───────────────┐ 1   N ┌────────────────┐
│  Course    │──────<│   Lesson   │──────<│ LessonSection │──────<│LessonKanjiItem │
└────────────┘       └────────────┘       │  (type:       │       └────────────────┘
                                           │   KANJI/      │ 1   N ┌─────────────────────┐
                                           │   VOCAB/      │──────<│LessonKanjiRelated  │
                                           │   GRAMMAR)    │       │Vocab               │
                                           └───────────────┘       └─────────────────────┘
                                                  │ 1   N ┌──────────────────────┐
                                                  │──────<│LessonVocabularyItem  │
                                                  │       └──────────────────────┘
                                                  │ 1   N ┌──────────────────────┐
                                                  │──────<│LessonGrammarItem     │
                                                  │       └──────────────────────┘

┌──────────────────────┐ N   1 ┌────────────┐
│  CourseEnrollment    │──────>│   Course   │   (User ↔ Course)
└──────────────────────┘       └────────────┘
┌──────────────────────┐ N   1 ┌────────────┐
│ UserLessonProgress   │──────>│   Lesson   │   (User ↔ Lesson)
└──────────────────────┘       └────────────┘
┌──────────────┐ 1   N ┌───────────────┐ 1   N ┌────────────┐
│    Quiz      │──────<│ QuizQuestion  │──────<│ QuizOption │
└──────────────┘       └───────────────┘       └────────────┘
       │ N   1
       └──────>  Lesson
```

### 3.2 Các trường quan trọng

| Entity | Trường cốt lõi |
|---|---|
| `Course` | `id, title, slug, jlptLevel (N5..N1), description, thumbnailUrl, published, archivedAt, createdByUserId` |
| `Lesson` | `id, courseId, title, orderIndex, hasQuiz, createdAt, updatedAt` |
| `LessonSection` | `id, lessonId, sectionType (KANJI/VOCAB/GRAMMAR), title, orderIndex` |
| `LessonKanjiItem` | `id, sectionId, kanjiId (FK tới dictionary), note, orderIndex` |
| `LessonVocabularyItem` | `id, sectionId, vocabId (FK tới dictionary), customHiragana/Romaji/Audio/Meaning, note, orderIndex` |
| `LessonGrammarItem` | `id, sectionId, grammarId (FK tới dictionary), customMeaning, customStructure, note, orderIndex` |
| `LessonKanjiRelatedVocab` | `id, kanjiItemId, vocabId, note` (hoặc derive tự động từ `vocabulary_characters`) |
| `CourseEnrollment` | `id, userId, courseId, status (ACTIVE/COMPLETED/DROPPED), enrolledAt, completedAt, progressPercent` |
| `UserLessonProgress` | `id, userId, lessonId, status (NOT_STARTED/IN_PROGRESS/COMPLETED), bestScore, lastAccessedAt, completedAt` |
| `Quiz` | `id, lessonId, title, description, passingScorePercent` |
| `QuizQuestion` | `id, quizId, questionText, questionType (MCQ/MATCHING/FILL_BLANK), orderIndex, matchingData, correctTextAnswer` |
| `QuizOption` | `id, questionId, optionKey, optionText, correct, matchKey` |

> **Liên kết với Dictionary:** Kanji/Vocab/Grammar items trong lesson **tham chiếu** sang bảng từ điển (`dictionary` schema). Giáo viên có thể override một số trường (customHiragana, customMeaning, …) mà không sửa dữ liệu gốc.

---

## 4. Luồng nghiệp vụ chính

### 4.1 Luồng Học viên — Học bài

```
1. User mở /courses                          → CourseCatalogPage
   GET /courses?jlptLevel=N5&page=0&size=20

2. User click 1 course                       → CourseDetailPage
   GET /courses/{id}                         (trả về lessons[] outline)
   GET /enrollments/me/active?                (check đã enroll chưa)

3. Nếu chưa enroll → bấm "Đăng ký học ngay"
   POST /courses/{id}/enroll                 → trả về Enrollment
   Status = ACTIVE, progressPercent = 0

4. Bấm "Vào học ngay" → /learn/:courseSlug/:lessonSlug
   → LessonViewerPage load:
     GET /courses/{id}                        (lấy course.slug, course.title)
     GET /lessons/{id}                        (lấy sections[] + kanjiItems/vocabItems/grammarItems)
     POST /progress/lessons/{id}/start        (ghi status = IN_PROGRESS, lastAccessedAt)

5. Trong bài học có 4 loại "tab":
     • Kanji      → render KanjiStudyView (flashcard + practice tuỳ chọn)
     • Vocabulary → render VocabStudyView
     • Grammar    → render GrammarStudyView
     • Quiz       → render QuizStudyView
         GET /lessons/{id}/quiz               (lấy câu hỏi, ẩn đáp án)
         User nộp bài: POST /lessons/{id}/quiz/submit
         BE chấm điểm, cập nhật bestScore, nếu đạt → status = COMPLETED

6. Cuối bài: POST /lessons/{id}/complete      (FE có thể gọi tường minh)
   → BE cập nhật UserLessonProgress, đồng thời tính lại
     progressPercent của CourseEnrollment.
```

**Cơ chế "xem xong":**
`LessonViewerPage` theo dõi `SectionDoneMap` đánh dấu user đã xem qua từng nhóm (KANJI/VOCAB/GRAMMAR/QUIZ). Khi đủ điều kiện (đã xem đủ hoặc đạt quiz) mới cho phép `completeLesson`.

### 4.2 Luồng Giáo viên — Biên soạn khóa học

```
1. /teacher/courses
   GET /courses/admin?page=0&size=20         (list tất cả course kể cả draft)

2. Bấm "Tạo khóa học"
   POST /courses
   { title, slug, jlptLevel, description?, thumbnailUrl?, published? }

3. Trong TeacherCourseEditorPage (2 tabs: Tổng quan | Bài học):
   3.1. Sửa thông tin:
        PUT /courses/{id}
        PATCH /courses/{id}/publish?publish=true|false

   3.2. Thêm bài học:
        POST /courses/{id}/lessons
        { title, orderIndex }

   3.3. Sắp xếp lại bài học (kéo thả / nút ↑ ↓):
        POST /courses/{id}/lessons/reorder
        { lessonIds: [3, 1, 2, 4] }

   3.4. Mở SectionEditorModal cho 1 lesson:
        - Quản lý Section:
            POST   /lessons/{id}/sections          { sectionType, title, orderIndex }
            PUT    /lessons/sections/{sectionId}
            DELETE /lessons/sections/{sectionId}
            POST   /lessons/{id}/sections/reorder
        - Trong mỗi Section, dùng các Editor tương ứng:
            • KanjiItemsEditor    → gắn Kanji từ dictionary
            • VocabItemsEditor    → gắn Vocab, override custom
            • GrammarItemsEditor  → gắn Grammar, override custom

   3.5. Quiz:
        GET /lessons/{id}/quiz/manage          (lấy đề + đáp án đúng)
        PUT  /lessons/{id}/quiz/manage          (lưu đề mới — tùy BE)
        Hỗ trợ 3 dạng câu hỏi:
            - MULTIPLE_CHOICE
            - MATCHING          (BE shuffle right-side mỗi lần save)
            - FILL_IN_THE_BLANK
```

### 4.3 Luồng Tiến độ

```
POST /progress/lessons/{id}/start    → UserLessonProgress = IN_PROGRESS
POST /lessons/{id}/complete          → COMPLETED
POST /lessons/{id}/quiz/submit       → cập nhật bestScore, nếu passed → COMPLETED

GET /courses/{id}/progress           → CourseProgress
  { totalLessons, completedLessons, inProgressLessons, notStartedLessons,
    progressPercent, progress: [{ lessonId, status, bestScore, lastAccessedAt, completedAt }] }
```

---

## 5. REST API — danh sách đầy đủ

> Base: `/api/v1`

### 5.1 Course

| Method | Endpoint | Mô tả |
|---|---|---|
| GET    | `/courses?jlptLevel=&page=&size=` | Danh sách public (chỉ course đã publish) |
| GET    | `/courses/admin?page=&size=` | Danh sách cho Teacher/Admin (gồm draft) |
| GET    | `/courses/{id}` | Chi tiết (kèm `lessons[]` outline) |
| GET    | `/courses/slug/{slug}` | Chi tiết theo slug |
| POST   | `/courses` | Tạo mới |
| PUT    | `/courses/{id}` | Cập nhật |
| DELETE | `/courses/{id}` | Xoá (mềm — set `archivedAt`) |
| PATCH  | `/courses/{id}/publish?publish=true\|false` | Publish / chuyển nháp |
| GET    | `/courses/{id}/progress` | Tiến độ của user hiện tại trong course |

### 5.2 Enrollment

| Method | Endpoint | Mô tả |
|---|---|---|
| POST   | `/courses/{courseId}/enroll` | Đăng ký học |
| GET    | `/enrollments/me` | Tất cả enrollment của tôi |
| GET    | `/enrollments/me/active` | Chỉ các enrollment ACTIVE |
| GET    | `/enrollments/{enrollmentId}` | Chi tiết 1 enrollment |
| POST   | `/enrollments/{enrollmentId}/drop` | Bỏ học (set DROPPED) |
| POST   | `/enrollments/by-course/{courseId}/drop` | Bỏ học theo courseId |

### 5.3 Lesson

| Method | Endpoint | Mô tả |
|---|---|---|
| GET    | `/courses/{courseId}/lessons` | Danh sách lesson (summary) |
| GET    | `/lessons/{id}` | Chi tiết lesson (kèm `sections[]` + items) |
| POST   | `/courses/{courseId}/lessons` | Tạo lesson |
| PUT    | `/lessons/{id}` | Cập nhật (title, orderIndex) |
| DELETE | `/lessons/{id}` | Xoá lesson |
| POST   | `/courses/{courseId}/lessons/reorder` | Sắp xếp lại (`{ lessonIds: [...] }`) |
| POST   | `/lessons/{id}/complete` | Đánh dấu hoàn thành |

### 5.4 Lesson Section

| Method | Endpoint | Mô tả |
|---|---|---|
| GET    | `/lessons/{id}/sections` | Danh sách section |
| POST   | `/lessons/{id}/sections` | Tạo section |
| PUT    | `/lessons/sections/{sectionId}` | Cập nhật |
| DELETE | `/lessons/sections/{sectionId}` | Xoá |
| POST   | `/lessons/{id}/sections/reorder` | Sắp xếp lại |

### 5.5 Items trong Section

#### Kanji Item
- `POST /lesson-sections/{sectionId}/kanji-items`
- `PUT  /lesson-kanji-items/kanji-items/{itemId}`
- `DELETE /lesson-kanji-items/kanji-items/{itemId}`
- `GET  /lesson-kanji-items/kanji-items/{itemId}` (fallback chi tiết)

#### Vocab Item
- `POST /lesson-sections/{sectionId}/vocabulary-items`
- `PUT  /lesson-vocabulary-items/vocabulary-items/{itemId}`
- `DELETE /lesson-vocabulary-items/vocabulary-items/{itemId}`
- `POST /lessons/vocab-items/{itemId}/examples` — thêm 1 example
- `PUT  /lessons/vocab-items/{itemId}/examples` — upsert (id null = tạo, có id = cập nhật)
- `GET  /lesson-vocabulary-items/vocabulary-items/{itemId}` (fallback)

#### Grammar Item
- `POST /lesson-sections/{sectionId}/grammar-items`
- `PUT  /lesson-grammar-items/grammar-items/{itemId}`
- `DELETE /lesson-grammar-items/grammar-items/{itemId}`
- `POST /lessons/grammar-items/{itemId}/examples`
- `PUT  /lessons/grammar-items/{itemId}/examples`
- `GET  /lesson-grammar-items/grammar-items/{itemId}` (fallback)

### 5.6 Quiz

| Method | Endpoint | Mô tả |
|---|---|---|
| GET    | `/lessons/{id}/quiz` | Lấy đề cho học viên (ẩn đáp án) |
| GET    | `/lessons/{id}/quiz/manage` | Lấy đề **kèm đáp án** (chỉ Teacher/Admin owner) |
| PUT    | `/lessons/{id}/quiz/manage` | Lưu đề (Teacher) |
| POST   | `/lessons/{id}/quiz/submit` | Nộp bài |

### 5.7 Progress

| Method | Endpoint | Mô tả |
|---|---|---|
| POST | `/progress/lessons/{lessonId}/start` | Đánh dấu bắt đầu |
| GET  | `/progress/lessons/{lessonId}` | Tiến độ 1 lesson của tôi |

---

## 6. Frontend — Pages & Components

### 6.1 Pages

| Page | Mục đích | Hooks chính |
|---|---|---|
| `CourseCatalogPage` | Lưới card khóa học + filter JLPT | `useCourses`, `useCreateCourse` |
| `CourseDetailPage` | Hero + outline lesson + nút Enroll/Start | `useCourseDetail`, `useEnrollCourse`, `useMyEnrollments` |
| `LessonViewerPage` | Học bài: 4 tab Kanji/Vocab/Grammar/Quiz + sidebar nav | `useCourseDetail`, `useLessonDetail`, `useStartLesson`, `useCompleteLesson`, `useLessonQuiz`, `useSubmitQuiz` |
| `TeacherCoursesPage` | Bảng quản lý + tạo/sửa/xoá/publish | `useCoursesAdmin`, `useCreateCourse`, `useUpdateCourse`, `useDeleteCourse`, `usePublishCourse` |
| `TeacherCourseEditorPage` | Biên soạn: 2 tabs (Tổng quan, Bài học) + Section editor | `useCourseDetail`, `useCreateLesson`, `useUpdateLesson`, `useDeleteLesson`, `useReorderLessons`, `useSectionMutations` |

### 6.2 Components

#### Trang chính (`components/course/`)
- **`CourseCard`** — Card hiển thị 1 khóa học (title, JLPT, lesson count, enroll count).
- **`CourseFilterBar`** — Filter theo `jlptLevel`, kết hợp search.
- **`CourseFormModal`** — Modal tạo/sửa course.
- **`LessonCard`** — Card hiển thị 1 lesson (title, hasQuiz, status, bestScore).
- **`LessonOutlineList`** — Danh sách lesson outline trong CourseDetailPage.
- **`LessonFormModal`** — Modal tạo/sửa lesson.

#### Lesson Viewer (`components/course/lesson-viewer/`)
- **`LessonNavSidebar`** — Sidebar điều hướng giữa các lesson, hiển thị trạng thái.
- **`SectionRenderer`** — Renderer chung, phân loại theo `sectionType`.
- **`KanjiStudyView` / `VocabStudyView` / `GrammarStudyView`** — Chế độ flashcard/study cho từng loại nội dung.
- **`KanjiSectionView` / `VocabSectionView` / `GrammarSectionView`** — Hiển thị dạng danh sách.
- **`QuizStudyView`** — Làm bài quiz (MCQ, MATCHING, FILL_IN_THE_BLANK) + nộp bài.

#### Section Editor (`components/course/section-editor/`)
- **`SectionList`** — Danh sách section trong 1 lesson.
- **`SectionEditorModal`** — Modal thêm/sửa section (chọn type KANJI/VOCAB/GRAMMAR).
- **`SectionItemsModal`** — Modal quản lý items trong section.
- **`KanjiItemsEditor` / `VocabItemsEditor` / `GrammarItemsEditor`** — Editor riêng cho từng loại item, cho phép:
  - Gắn từ dictionary (chọn `kanjiId` / `vocabId` / `grammarId`)
  - Override trường custom (customMeaning, customHiragana, …)
  - Quản lý related vocabulary (chỉ Kanji)
  - Quản lý examples (Vocab/Grammar)

#### Quiz Editor (`components/course/quiz-editor/`)
- **`QuizPanel`** — Panel chính: danh sách câu hỏi, thêm/sửa/xoá, set passingScore.
- **`MatchingPairsEditor`** — Editor cho câu hỏi MATCHING (cặp left/right).
- **`matchingFromLesson.ts`** — Helper suy ra cặp matching từ lesson items.
- **`constants.ts`** — Hằng số (loại câu hỏi, default passing score, …).

### 6.3 Hooks (React Query)

| Hook | Tương ứng API |
|---|---|
| `useCourses` | `GET /courses` |
| `useCourseDetail` | `GET /courses/{id}` |
| `useCreateCourse` / `useUpdateCourse` / `useDeleteCourse` / `usePublishCourse` | CRUD course |
| `useEnrollCourse` | `POST /courses/{id}/enroll` |
| `useMyEnrollments` | `GET /enrollments/me/active` |
| `useLessonsByCourse` | `GET /courses/{id}/lessons` |
| `useLessonDetail` | `GET /lessons/{id}` |
| `useCreateLesson` / `useUpdateLesson` / `useDeleteLesson` | CRUD lesson |
| `useReorderLessons` | `POST /courses/{id}/lessons/reorder` |
| `useStartLesson` | `POST /progress/lessons/{id}/start` |
| `useCompleteLesson` | `POST /lessons/{id}/complete` |
| `useLessonQuiz` | `GET /lessons/{id}/quiz` |
| `useLessonQuizManage` | `GET /lessons/{id}/quiz/manage` |
| `useSectionMutations` | CRUD section + reorder |

### 6.4 Types chính (`types/course.types.ts`)

```ts
// Course
type EnrollmentStatus  = 'ACTIVE' | 'COMPLETED' | 'DROPPED'
type ProgressStatus    = 'NOT_STARTED' | 'IN_PROGRESS' | 'COMPLETED'
type LessonSectionType = 'KANJI' | 'VOCABULARY' | 'GRAMMAR'

interface CourseSummary { id, title, slug, jlptLevel, thumbnailUrl, published, ... }
interface CourseDetail  extends CourseSummary { lessons?: LessonOutline[], isEnrolled, progressPercent, ... }
interface LessonOutline { id, title, orderIndex, hasQuiz, status?, bestScore? }
interface LessonDetail  { id, courseId, title, sections?: LessonSection[] }
interface LessonSection { id, sectionType, title, orderIndex,
                          kanjiItems?, vocabularyItems?, grammarItems? }
```

---

## 7. Đặc điểm kỹ thuật đáng chú ý

### 7.1 Fallback pattern
Các hook/item detail có cơ chế **fallback** gọi endpoint chi tiết riêng
(`GET /lesson-kanji-items/kanji-items/{id}`, …) khi response tổng từ
`GET /lessons/{id}` thiếu `relatedVocabulary` hoặc `examples`. Đảm bảo UI luôn hiển thị đủ dữ liệu.

### 7.2 Override "3-state semantics"
Trường custom ở Vocab/Grammar Item:
- `null` (không gửi) → **giữ nguyên**
- `""` (chuỗi rỗng) → **xoá override** (dùng giá trị gốc từ dictionary)
- `"value"` → **cập nhật**

Tương tự với `examples`: `null` = giữ, `[]` = reset & auto-copy từ dictionary, `[…]` = upsert.

### 7.3 Related Vocab của Kanji
- Có thể chọn nhiều vocab có sẵn (`vocabIds[]`) **hoặc** tạo mới (`newVocab{ word, hiragana, … }`).
- Nếu payload **không gửi** field `relatedVocabulary` khi update → BE tự derive từ bảng `vocabulary_characters`.
- Nếu **không truyền** khi tạo → BE cũng tự derive.

### 7.4 Matching Quiz — shuffle right side
BE **shuffle sẵn** thứ tự `right[]` mỗi lần teacher save → mỗi lần lưu đề sẽ cho ra một thứ tự khác nhau, chống ghi nhớ máy móc của học viên. Viewer chỉ cần duyệt theo index tự nhiên.

### 7.5 URL thân thiện (slug-based)
`/learn/:courseSlug/:lessonSlug?courseId=...&lessonId=...`
- Ưu tiên dùng slug, **luôn kèm `courseId` + `lessonId` trong query string** để:
  - Đảm bảo tham số truyền xuống các hook/page-level luôn chính xác tuyệt đối.
  - Hỗ trợ backward-compat với URL cũ dạng `/learn/{courseId}/{lessonId}` (số) — tự redirect sang slug khi load xong.

### 7.6 Bảo mật endpoint Quiz Manage
`GET /lessons/{id}/quiz/manage` (lấy đề kèm đáp án) — BE kiểm tra **owner**: chỉ Teacher tạo course đó mới xem được. Teacher khác → 403.

### 7.7 Soft delete
- Course: set `archivedAt` (không xoá cứng).
- Lesson/Section/Item: tuỳ BE; FE gọi `DELETE` rồi invalidate cache.

### 7.8 Cache & React Query
Mọi hook đều dùng React Query → tự động:
- Cache, refetch khi focus.
- `invalidateQueries(['course', id])` sau các mutation để UI tự cập nhật.

---

## 8. Sơ đồ tổng quan nghiệp vụ

```
                          ┌─────────────────┐
                          │  Public User    │
                          └────────┬────────┘
                                   │ browse
                                   ▼
                          ┌─────────────────┐         ┌────────────────┐
                          │ CourseCatalog   │ ◀────── │ CourseFilterBar│
                          └────────┬────────┘         └────────────────┘
                                   │ click
                                   ▼
                          ┌─────────────────┐
                          │ CourseDetail    │  (enroll?)
                          └────────┬────────┘
                                   │ start
                                   ▼
                          ┌────────────────────────────────────┐
                          │          LessonViewerPage          │
                          │  ┌──────────────────────────────┐  │
                          │  │  LessonNavSidebar            │  │
                          │  └──────────────────────────────┘  │
                          │  ┌──────────────────────────────┐  │
                          │  │  SectionRenderer             │  │
                          │  │   ├─ KanjiStudyView          │  │
                          │  │   ├─ VocabStudyView          │  │
                          │  │   ├─ GrammarStudyView        │  │
                          │  │   └─ QuizStudyView ─┐        │  │
                          │  └─────────────────────│───────┘  │
                          │                        │submit    │
                          │                        ▼          │
                          │  ┌──────────────────────────────┐  │
                          │  │  /lessons/{id}/quiz/submit   │  │
                          │  │  → bestScore, status update  │  │
                          │  └──────────────────────────────┘  │
                          └────────────────┬───────────────────┘
                                           │ complete
                                           ▼
                          ┌──────────────────────────────┐
                          │ CourseProgress (recalculated)│
                          └──────────────────────────────┘


                          ┌─────────────────┐
                          │ Teacher/Admin   │
                          └────────┬────────┘
                                   │ manage
                                   ▼
                          ┌────────────────────────┐         ┌────────────────┐
                          │ TeacherCoursesPage     │ ◀────── │ CourseFormModal│
                          └────────┬───────────────┘         └────────────────┘
                                   │ open
                                   ▼
                          ┌────────────────────────────────────┐
                          │     TeacherCourseEditorPage        │
                          │  ┌──────────────────────────────┐  │
                          │  │ Tabs: Overview | Lessons     │  │
                          │  └──────────────────────────────┘  │
                          │   │                              │  │
                          │   │ Lessons list                 │  │
                          │   │   ├─ LessonCard              │  │
                          │   │   ├─ LessonFormModal         │  │
                          │   │   └─ reorder ↑↓              │  │
                          │   │       │                      │  │
                          │   │       ▼                      │  │
                          │   │ SectionEditorModal           │  │
                          │   │   ├─ SectionList             │  │
                          │   │   └─ SectionItemsModal       │  │
                          │   │       ├─ KanjiItemsEditor    │  │
                          │   │       ├─ VocabItemsEditor    │  │
                          │   │       └─ GrammarItemsEditor  │  │
                          │   │                              │  │
                          │   │ Quiz: QuizPanel              │  │
                          │   │   └─ MatchingPairsEditor     │  │
                          └────────────────────────────────────┘
```

---

## 9. Tóm tắt các điểm cần nhớ khi báo cáo

1. **Hai tên gọi một thực thể:** FE gọi "Course/Lesson", BE gọi "curriculum". Khi trích dẫn file phải nói rõ đang ở phía nào.
2. **Phân quyền rõ ràng:** Public (catalog/detail), Student (learn), Teacher/Admin (manage). Endpoint `quiz/manage` có check owner.
3. **3 loại nội dung lesson:** Kanji / Vocabulary / Grammar — đều tham chiếu sang `dictionary`, có field `custom*` để override mà không sửa gốc.
4. **Cơ chế "auto-derive"** cho Kanji related vocab và Vocab/Grammar examples: nếu FE không gửi → BE tự suy ra từ dictionary. Có 3-state semantics rất tinh tế (null = giữ, [] = reset, [...] = upsert).
5. **Quiz 3 dạng câu hỏi:** MCQ, MATCHING (BE shuffle right), FILL_IN_THE_BLANK. Matching là điểm đặc biệt vì BE đã chống ghi nhớ.
6. **Progress tính tự động:** `UserLessonProgress` của từng lesson → aggregate lên `CourseEnrollment.progressPercent`. FE hiển thị qua `useCourseDetail` (đã có sẵn `progressPercent`, `completedLessonsCount`) và `GET /courses/{id}/progress`.
7. **URL slug + query string:** vừa thân thiện SEO/UX, vừa đảm bảo tham số chính xác cho các page-level hook.
8. **React Query cache invalidation** là chìa khoá giữ UI nhất quán sau mọi mutation; không có Redux/Zustand cho course (chỉ `useAuthStore`).

---

## 10. Phụ lục — File tham khảo nhanh

### FE — Course & Lesson
- Types: `FE/src/types/course.types.ts`
- Service: `FE/src/services/course.service.ts`
- Pages: `FE/src/pages/courses/**`
- Components: `FE/src/components/course/**`
- Hooks: `FE/src/hooks/use{Course,Lesson,Section,Progress,Quiz}*.ts`
- Routes: `FE/src/routes/index.tsx`

### BE — Curriculum
- Controllers: `BE/elearningJP/src/main/java/com/jp/elearningjp/controller/curriculum/**`
- Services: `BE/elearningJP/src/main/java/com/jp/elearningjp/service/curriculum/**`
- Entities: `BE/elearningJP/src/main/java/com/jp/elearningjp/entity/curriculum/**`
- DTOs: `BE/elearningJP/src/main/java/com/jp/elearningjp/dto/{request,response}/curriculum/**`
- Mapper: `BE/elearningJP/src/main/java/com/jp/elearningjp/mapper/curriculum/**`
- Repository: `BE/elearningJP/src/main/java/com/jp/elearningjp/repository/curriculum/**`
