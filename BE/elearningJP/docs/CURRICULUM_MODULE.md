# Module Curriculum — Tài liệu nghiệp vụ & cách xử lý trong code

> Phạm vi: `BE/elearningJP/src/main/java/com/jp/elearningjp/`
> Module này gồm 4 sub-module: **Course**, **Lesson**, **Enrollment**, **Progress** (4 controller, 4 service, 4 repository, 9 entity, 4 mapper, 10 event class).
> Tài liệu này mô tả **đúng những gì source code hiện đang làm**, không phải lý thuyết chung. Mọi "suy luận" đã được đánh dấu rõ.

---

## Mục lục

1. [Tổng quan kiến trúc](#1-tổng-quan-kiến-trúc)
2. [Domain model (Entity)](#2-domain-model-entity)
3. [Bảng API → Controller → Service → Repository](#3-bảng-api--controller--service--repository)
4. [Phân quyền (Authentication / Authorization)](#4-phân-quyền-authentication--authorization)
5. [Nghiệp vụ chi tiết từng sub-module](#5-nghiệp-vụ-chi-tiết-từng-sub-module)
6. [Soft-delete & cascade](#6-soft-delete--cascade)
7. [Mapper & DTO](#7-mapper--dto)
8. [Event bus (in-process)](#8-event-bus-in-process)
9. [Redis / Kafka / Cache](#9-redis--kafka--cache)
10. [Helper class — CurrentUserService](#10-helper-class--currentuserservice)
11. [Lifecycle end-to-end của một Course](#11-lifecycle-end-to-end-của-một-course)
12. [Helper / Guard / Interceptor can thiệp vào flow](#12-helper--guard--interceptor-can-thiệp-vào-flow)
13. [Điểm chưa xác định / hạn chế](#13-điểm-chưa-xác-định--hạn-chế)

---

## 1. Tổng quan kiến trúc

### 1.1 Stack
- **Spring Boot** + **Spring Security** (OAuth2 Resource Server / JWT HS512)
- **JPA / Hibernate** (qua `JpaRepository`)
- **MapStruct** (mapper dạng interface + `@Mapper(componentModel = "spring")`)
- **Lombok** (`@RequiredArgsConstructor`, `@Builder`, `@Getter/Setter`, `@Slf4j`)
- **Spring `ApplicationEventPublisher`** cho event in-process
- **Redis** chỉ dùng cho auth (token blacklist + password reset) — chưa cache course/lesson (chỉ có comment TODO)
- **Không có Kafka** trong code hiện tại

### 1.2 Layered architecture
```
HTTP Request
   ↓
[Controller]  ← DTO Request/Response, @Valid, @SecurityRequirement (Swagger doc)
   ↓
[Service]     ← @Transactional, business rules, guard helpers, event publish
   ↓
[Repository]  ← Spring Data JPA, query derivation + @Query JPQL/native
   ↓
[Database]    ← SoftDeletableEntity (mọi entity, có cờ `deleted` + `deletedAt`)
```

### 1.3 Quy ước soft-delete toàn hệ thống
Mọi entity kế thừa `SoftDeletableEntity` (xem `shared/persitence/SoftDeletableEntity.java`), có:
- `deleted: boolean` (mặc định `false`)
- `deletedAt: Instant`
- method `softDelete()` / `restore()`

Mọi query method ở Repository đều kết thúc bằng `AndDeletedFalse` — **record đã soft-delete không bao giờ xuất hiện ở Service**. Đây là quy ước cứng, không phải filter ở Service.

> **Suy luận:** Vì cột `deleted` không có index được nhìn thấy trong entity (chỉ `@Column`), performance phụ thuộc vào việc DB có index hay không. Hiện không thấy file migration nên chưa xác định.

### 1.4 Cấu trúc thư mục (phần curriculum)
```
controller/curriculum/
  ├─ CourseController.java        — CRUD + state transition
  ├─ LessonController.java        — sub-resource của course
  ├─ EnrollmentController.java    — enroll/drop
  └─ ProgressController.java      — start/complete lesson + xem progress
service/curriculum/
  ├─ CourseService.java
  ├─ LessonService.java
  ├─ EnrollmentService.java
  └─ ProgressService.java
repository/curriculum/
  ├─ CourseRepository.java
  ├─ LessonRepository.java
  ├─ CourseEnrollmentRepository.java
  └─ UserLessonProgressRepository.java
entity/curriculum/
  ├─ Course.java
  ├─ Lesson.java
  ├─ LessonItem.java
  ├─ CourseEnrollment.java
  ├─ UserLessonProgress.java
  ├─ DailyTask.java
  ├─ DailyTaskItem.java
  ├─ LearningPath.java
  └─ LearningPathTemplate.java
dto/request/curriculum/  — 6 file (CourseCreate/Update, LessonCreate/Update/Reorder, LessonComplete)
dto/response/curriculum/ — 9 file (CourseSummary, CourseDetail, CourseProgress, LessonSummary, LessonResponse, LessonView, EnrollmentResponse, ProgressResponse)
mapper/curriculum/      — 4 file MapStruct
shared/event/curriculum/ — 10 file (CurriculumEvent base + 9 concrete)
```

---

## 2. Domain model (Entity)

### 2.1 Sơ đồ quan hệ (chỉ liên quan curriculum)

```
User (entity/user) ─┬─< createdBy  ── Course
                    ├─< user       ── CourseEnrollment >─ Course
                    └─< user       ── UserLessonProgress >─ Lesson >─ Course

Vocabulary / GrammarPoint / ReadingPassage / WritingCharacter
                  └─< (LAZY, optional) ── LessonItem >─ Lesson

DailyTask >─< DailyTaskItem (tham chiếu tuỳ chọn đến vocab/character/grammar/lesson/exam)

LearningPath >─ Course
LearningPathTemplate >─ Course
```

### 2.2 Bảng entity — field & quan hệ chính

| Entity | Bảng | Field chính | Quan hệ |
|---|---|---|---|
| `Course` | `courses` | `title, slug, jlptLevel, description, thumbnailUrl, published, archivedAt` | `@ManyToOne(LAZY) createdBy → User` |
| `Lesson` | `lessons` | `title, lessonType, orderIndex, durationMinutes, contentMarkdown, archivedAt` | `@ManyToOne(LAZY, optional=false) course → Course`; `@OneToMany(cascade=ALL, orphanRemoval=true) items → List<LessonItem>` `@OrderBy("orderIndex ASC")` |
| `LessonItem` | `lesson_items` | `orderIndex` | 4 FK tuỳ chọn đến `Vocabulary, WritingCharacter, GrammarPoint, ReadingPassage` (tất cả LAZY, optional) |
| `CourseEnrollment` | `course_enrollments` | `status: EnrollmentStatus = ACTIVE` | `user → User`, `course → Course` (cả 2 LAZY, optional=false) |
| `UserLessonProgress` | `user_lesson_progress` | `status: ProgressStatus = NOT_STARTED, startedAt, lastAccessedAt, completedAt, bestScore (BigDecimal 5,2)` | `user → User`, `lesson → Lesson` |
| `DailyTask` | `daily_tasks` | `taskDate, taskType, targetCount, completedCount, status, rescheduledFromDate, completedAt` | `learningPath → LearningPath` |
| `DailyTaskItem` | `daily_task_items` | `completed, completedAt` | `task → DailyTask`; FK tuỳ chọn: vocab, character, grammar, lesson, exam |
| `LearningPath` | `learning_paths` | `targetLevel, startDate, targetDate, status, totalVocab/Kanji/GrammarPlanned, lastRecalculatedAt` | `user → User`, `template → LearningPathTemplate`, `course → Course` |
| `LearningPathTemplate` | `learning_path_templates` | `jlptLevel, title, durationDays, dailyVocab/Kanji/GrammarQuota, description, active` | `course → Course` (LAZY) |

### 2.3 Enum quan trọng (xem `shared/enums/`)

- `JlptLevel`: N5, N4, N3, N2, N1
- `LessonType`: THEORY, VOCABULARY, KANJI, GRAMMAR, READING, **MINI_TEST** (loại này mới có điểm)
- `EnrollmentStatus`: ACTIVE, COMPLETED, DROPPED
- `ProgressStatus`: NOT_STARTED, IN_PROGRESS, COMPLETED

> Comment trong `EnrollmentStatus.java` và `ProgressStatus.java` nói: "Giá trị khớp ràng buộc CHECK trong database" — nghĩa là DB có CHECK constraint cho cột này, nếu thêm giá trị mới phải đổi cả migration DB.

### 2.4 Entity nào chưa có Service/Repository trong module curriculum?

> **Quan sát từ source code:** `LessonItem`, `DailyTask`, `DailyTaskItem`, `LearningPath`, `LearningPathTemplate` có entity nhưng **không có service/repository trong package `curriculum`**. Chỉ là schema placeholder — chưa có endpoint nào đụng đến chúng. (Suy luận: có thể chuẩn bị cho module gamification/learning path sau này.)

---

## 3. Bảng API → Controller → Service → Repository

> Path base `/api/v1` từ `ApiPaths.API_V1`. Mọi path dưới đây đã được giải ra từ annotation `@RequestMapping` + `ApiPaths` constant tương ứng (xem `shared/constants/ApiPaths.java`).

### 3.1 CourseController

| HTTP | Path | Method Service | Repo call chính |
|---|---|---|---|
| POST | `/api/v1/courses` | `createCourse` | `existsBySlugAndDeletedFalse` → `save` |
| GET | `/api/v1/courses/{id}` | `getCourseById` → `buildDetailResponse` | `findByIdAndDeletedFalse`; nếu cần thêm `countByCourse_IdAndDeletedFalse` (lesson), `findByUser_IdAndCourse_IdAndStatusAndDeletedFalse` (enrollment), `calculateCourseProgressPercent` (progress), `findByCourse_IdAndDeletedFalseOrderByOrderIndexAsc` (lessons) |
| GET | `/api/v1/courses/slug/{slug}` | `getCourseBySlug` | `findBySlugAndDeletedFalse` |
| GET | `/api/v1/courses` | `getAllPublishedCourses` | `findByDeletedFalseAndPublishedTrue(Pageable)` |
| GET | `/api/v1/courses/level/{level}` | `getCoursesByLevel` | `findByDeletedFalseAndPublishedTrueAndJlptLevel` |
| GET | `/api/v1/courses/admin/all` | `getAllCoursesAdmin` | TEACHER: `findByCreatedBy_IdAndDeletedFalse` + manual Page; ADMIN: `findByDeletedFalse(Pageable)` |
| PUT | `/api/v1/courses/{id}` | `updateCourse` | `existsBySlugAndDeletedFalseAndIdNot` (nếu đổi slug), `countByCourse_IdAndStatusAndDeletedFalse` (nếu đổi level), `save` |
| POST | `/api/v1/courses/{id}/publish` | `publishCourse` | `save` |
| POST | `/api/v1/courses/{id}/unpublish` | `unpublishCourse` | `save` |
| POST | `/api/v1/courses/{id}/archive` | `archiveCourse` | `save` |
| DELETE | `/api/v1/courses/{id}` | `deleteCourse` (cascade) | `findByCourse_IdAndDeletedFalseOrderByOrderIndexAsc` (lessons), `findByCourse_IdAndDeletedFalse` (enrollments); soft-delete từng cái |

### 3.2 LessonController (sub-resource)

| HTTP | Path | Method Service | Repo call chính |
|---|---|---|---|
| POST | `/api/v1/courses/{courseId}/lessons` | `createLesson` | `existsByCourse_IdAndOrderIndexAndDeletedFalse` (check trùng order) → `save` |
| GET | `/api/v1/courses/{courseId}/lessons` | `getLessonsByCourseId` | `findByCourse_IdAndDeletedFalseOrderByOrderIndexAsc` |
| GET | `/api/v1/courses/{courseId}/lessons/{lessonId}` | `getLessonById` | `findByIdAndCourse_IdAndDeletedFalse` (chống hack URL); check enrollment ACTIVE |
| PUT | `/api/v1/courses/{courseId}/lessons/{lessonId}` | `updateLesson` | `existsByCourse_IdAndOrderIndexAndDeletedFalseAndIdNot`, `save` |
| POST | `/api/v1/courses/{courseId}/lessons/reorder` | `reorderLessons` | `findByCourse_IdAndDeletedFalseOrderByOrderIndexAsc`, `saveAll`, `flush`, `updateOrderIndex` (JPQL `@Modifying`) |
| DELETE | `/api/v1/courses/{courseId}/lessons/{lessonId}` | `deleteLesson` | `save` (soft-delete) |

> Comment trong `LessonController.java` (dòng 145-151) ghi rõ: "Endpoint complete chính nằm ở ProgressController để tránh duplicate, không khai báo ở đây."

### 3.3 EnrollmentController

| HTTP | Path | Method Service | Repo call chính |
|---|---|---|---|
| POST | `/api/v1/courses/{id}/enroll` | `enrollInCourse` | `findByIdAndDeletedFalse` (course) → `findByUser_IdAndCourse_IdAndDeletedFalse` (existing) → `save` |
| GET | `/api/v1/enrollments/me` | `getMyEnrollments` | `findByUser_IdAndDeletedFalseOrderByCreatedAtDesc` |
| GET | `/api/v1/enrollments/me/active` | `getMyActiveEnrollments` | `findByUser_IdAndStatusAndDeletedFalseOrderByCreatedAtDesc` (status=ACTIVE) |
| GET | `/api/v1/enrollments/{enrollmentId}` | `getEnrollmentById` | `findById` + check owner |
| POST | `/api/v1/enrollments/{enrollmentId}/drop` | `dropEnrollment` | `findById` + check owner + `save` |
| POST | `/api/v1/enrollments/by-course/{courseId}/drop` | `dropEnrollmentByCourseId` | `findByUser_IdAndCourse_IdAndStatusAndDeletedFalse` (status=ACTIVE) → gọi lại `dropEnrollment` |

### 3.4 ProgressController

| HTTP | Path | Method Service | Repo call chính |
|---|---|---|---|
| POST | `/api/v1/progress/lessons/{lessonId}/start` | `startLesson` | `findByIdAndDeletedFalse` (lesson), `existsByUser_IdAndCourse_IdAndStatusAndDeletedFalse` (enrollment), `findByUser_IdAndLesson_IdAndDeletedFalse` (progress), `save` |
| POST | `/api/v1/progress/lessons/{lessonId}/complete` | `completeLesson` | giống start + `calculateCourseProgressPercent` (native SQL) + gọi `enrollmentService.markAsCompletedIfFull` |
| GET | `/api/v1/progress/lessons/{lessonId}` | `getMyProgressForLesson` | `findByUser_IdAndLesson_IdAndDeletedFalse` (trả `null` nếu không có) |
| GET | `/api/v1/progress/courses/{courseId}` | `getMyProgressForCourse` | `findByIdAndDeletedFalse` (course) + `findByUserAndCourseWithLesson` (JPQL `JOIN FETCH p.lesson`) + `countByCourse_IdAndDeletedFalse` |

---

## 4. Phân quyền (Authentication / Authorization)

> Phần này tổng hợp từ `config/SecurityConfig.java` + helper `CurrentUserService` + guard trong từng Service.

### 4.1 Cơ chế tổng thể

| Cấp độ | Cơ chế | File |
|---|---|---|
| **URL-level** | `requestMatchers(HttpMethod.GET, ...).permitAll()` cho các GET công khai; `.anyRequest().authenticated()` cho phần còn lại | `SecurityConfig` |
| **Role-level (annotation)** | `@EnableMethodSecurity` được bận nhưng **không có `@PreAuthorize`/`@Secured` nào trong module curriculum** | `SecurityConfig` |
| **Owner-level (Service)** | Helper `requireOwnerOrAdmin(course, user)` / `requireTeacherOrAdmin(user)` / `requireEnrolledUser(userId, courseId)` trong từng Service | `CourseService`, `LessonService`, `EnrollmentService`, `ProgressService` |
| **Resource-level** | `findByIdAndCourse_IdAndDeletedFalse` — lesson chỉ tìm thấy nếu đúng courseId (chống URL hacking) | `LessonRepository` |

### 4.2 URL public (không cần JWT)
Từ `SecurityConfig.PUBLIC_GET_ENDPOINTS`:
- `GET /api/v1/courses` (list)
- `GET /api/v1/courses/*` (by id)
- `GET /api/v1/courses/slug/*`
- `GET /api/v1/courses/level/*`
- `GET /api/v1/courses/*/lessons` (list lesson summary)
- `GET /api/v1/courses/*/lessons/*` (lesson detail — **LƯU Ý: Service vẫn check quyền xem content**)

Mọi POST/PUT/DELETE trên course, lesson, enrollment, progress **đều yêu cầu JWT**.

### 4.3 JWT setup
- `SecurityConfig` cấu hình OAuth2 Resource Server với `CustomJwtDecoder`
- `JwtAuthenticationConverter` với `JwtGrantedAuthoritiesConverter.setAuthorityPrefix("")` — bỏ prefix mặc định, vì `AuthService.generateToken` đã nhét role dưới dạng `scope = "ROLE_ADMIN ROLE_TEACHER ..."`
- Stateless session (`SessionCreationPolicy.STATELESS`)
- CORS: `localhost:*`, `127.0.0.1:*`, `*.up.railway.app`, `*.onrender.com`
- `JwtAuthenticationEntryPoint` (Bean) xử lý response khi thiếu/sai token

### 4.4 Ma trận quyền cho từng hành động

| Hành động | Yêu cầu | Check ở đâu |
|---|---|---|
| Tạo course | TEACHER hoặc ADMIN | `CourseService.requireTeacherOrAdmin` |
| Sửa/Xoá/Publish/Unpublish/Archive course | Owner (`createdBy == currentUser`) HOẶC ADMIN | `CourseService.requireOwnerOrAdmin` |
| Xem course detail (course đã publish) | Public (bất kỳ ai) | URL permit + Service vẫn gắn `isEnrolled` (Boolean: null=anonymous, true/false) |
| Xem course detail (course CHƯA publish) | Owner hoặc ADMIN | `buildDetailResponse` wrap trong `try/catch`, throw `COURSE_NOT_PUBLISHED` nếu fail |
| Tạo/Sửa/Xoá/Reorder lesson | Owner của course chứa lesson HOẶC ADMIN | `LessonService.requireOwnerOrAdmin` |
| Xem list lesson trong course | Public | URL permit |
| Xem lesson detail (course đã publish) | Owner/ADMIN HOẶC user có enrollment ACTIVE | `LessonService.getLessonById` |
| Xem lesson detail (course chưa publish) | Owner/ADMIN | `LessonService.getLessonById` (nhánh đầu) |
| Enroll | User authenticated; course đã publish, chưa archived; chưa có enrollment ACTIVE | `EnrollmentService.enrollInCourse` |
| Drop enrollment | Owner (`enrollment.user == currentUser`) HOẶC ADMIN | `EnrollmentService.dropEnrollment` |
| Xem enrollment detail | Owner hoặc ADMIN | `EnrollmentService.getEnrollmentById` |
| Start/Complete lesson | Có enrollment ACTIVE | `ProgressService.requireEnrolledUser` |

### 4.5 Quy tắc "owner" chính xác
Trong `CourseService.requireOwnerOrAdmin`:
```java
if (currentUserService.isAdmin()) return; // admin bypass
if (course.getCreatedBy() != null && course.getCreatedBy().getId().equals(user.getId())) {
    return; // teacher owner
}
throw new AppException(ErrorCode.FORBIDDEN_COURSE_ACCESS);
```
> **Suy luận:** TEACHER chỉ là owner nếu `createdBy.id == currentUser.id`. Một TEACHER khác (dù cùng role) sẽ bị 403. ADMIN bypass hoàn toàn.

### 4.6 Helper lấy user hiện tại — `CurrentUserService`

Xem chi tiết ở [mục 10](#10-helper-class--currentuserservice).

---

## 5. Nghiệp vụ chi tiết từng sub-module

### 5.1 Course (CourseService)

#### 5.1.1 State machine
```
            create (TEACHER/ADMIN)
                ↓
        [DRAFT, published=false]
            ↙          ↘
     publish           archive
        ↓                ↓
[PUBLISHED,           [ARCHIVED,
 published=true,      published=false,
 archivedAt=null]     archivedAt=now]
        ↑                  │
        │                  │
     unpublish             │
        ↓                  │
     [DRAFT]               │
                           │
           ┌───────────────┘
           │ soft-delete (cascade)
           ↓
      [DELETED]
```
- **publish:** set `published=true`. Idempotent — nếu đã publish thì log skip, không throw.
- **unpublish:** set `published=false`. Idempotent.
- **archive:** set `archivedAt=now()` + `published=false` đồng thời. Không idempotent nhưng nếu đã archive thì skip.
- **delete:** soft-delete course + cascade soft-delete lesson + enrollment (xem [mục 6](#6-soft-delete--cascade)).

#### 5.1.2 Validate nghiệp vụ
- **Slug uniqueness:** khi tạo check `existsBySlugAndDeletedFalse`; khi update check `existsBySlugAndDeletedFalseAndIdNot` (chỉ check nếu slug thay đổi).
- **Đổi JLPT level khi đã có enrollment:** nếu `request.getJlptLevel() != course.getJlptLevel()` và `countByCourse_IdAndStatusAndDeletedFalse(courseId, ACTIVE) > 0` → throw `FORBIDDEN_COURSE_ACCESS`. (Suy luận: vì lý do chính sách học, không cho phép "di chuyển" học viên sang level khác.)

#### 5.1.3 Build detail response (logic phức tạp nhất)
Trong `CourseService.buildDetailResponse` (mục 5.1.3 trong code):

1. Nếu course chưa publish → check owner/admin (try-catch chuyển `COURSE_NOT_PUBLISHED` nếu fail).
2. Đếm `totalLessons` (từ `LessonRepository.countByCourse_IdAndDeletedFalse`).
3. Đếm `totalEnrollments` (từ `EnrollmentRepository.countByCourse_IdAndStatusAndDeletedFalse`, status=ACTIVE).
4. Nếu có current user (try-catch):
   - `canViewFullContent = true` nếu admin hoặc owner.
   - Check enrollment ACTIVE:
     - Có → `isEnrolled=true`, tính `progressPercent` từ `UserLessonProgressRepository.calculateCourseProgressPercent` (native SQL), `canViewFullContent=true`.
     - Không → `isEnrolled=false`.
   - Nếu exception (anonymous) → `isEnrolled=null`.
5. Load `lessons` (list summary có thứ tự) → `lessonMapper.toViewResponse(lesson, finalCanViewFullContent)`:
   - Nếu `canViewFullContent=true` → kèm `contentMarkdown`.
   - Nếu `false` → `contentMarkdown=null` (sẽ bị ẩn khỏi JSON nhờ `@JsonInclude(NON_NULL)` của `LessonViewResponse`).

> **Tại sao 1 DTO duy nhất?** Vì `LessonViewResponse` có `@JsonInclude(JsonInclude.Include.NON_NULL)`, các field null không xuất hiện trong JSON → ẩn `contentMarkdown` mà không cần tách DTO khác.

#### 5.1.4 Admin list
- ADMIN: dùng `findByDeletedFalse(Pageable)` (mọi course, kể cả chưa publish).
- TEACHER: dùng `findByCreatedBy_IdAndDeletedFalse(userId)` rồi manual convert `List<Course>` → `Page` (vì method này không trả `Page`). Suy luận: có thể vì lý do đơn giản (không phân trang phức tạp), nhưng hiệu năng có thể kém khi teacher tạo nhiều course.

### 5.2 Lesson (LessonService)

#### 5.2.1 Quy tắc orderIndex
- `orderIndex` phải **unique trong phạm vi 1 course** (kiểm tra qua `existsByCourse_IdAndOrderIndexAndDeletedFalse` / `...AndIdNot`).
- Service không tự động tìm index trống khi tạo — client phải gửi `orderIndex` hợp lệ. Nếu trùng → `LESSON_ORDER_CONFLICT` (409).

#### 5.2.2 Reorder thuật toán 2 bước (rất quan trọng)
Trong `LessonService.reorderLessons`:
```
Bước 1: Bump tất cả lesson bị ảnh hưởng: orderIndex += 10000
         saveAll() + flush()  ← flush để DB thấy giá trị tạm TRƯỚC khi set mới
Bước 2: Set orderIndex mới theo thứ tự list gửi lên
         updateOrderIndex(lessonId, i+1)  ← JPQL @Modifying
```
> **Tại sao 2 bước?** Nếu set trực tiếp, sẽ có lúc 2 lesson cùng có orderIndex=2 (vd: A từ 1→3, B từ 3→1, tại 1 thời điểm cả 2 đều là 2 hoặc 3) → nếu có UNIQUE constraint ở DB thì sẽ throw. Bump +10000 là "vùng đệm" để không bao giờ đụng nhau.

Validate trước khi reorder:
- Mọi `lessonId` trong request phải thuộc course (nếu không → `LESSON_NOT_FOUND`).
- Số lượng lessonId phải bằng tổng lesson hiện tại (nếu thiếu → `LESSON_ORDER_CONFLICT`).

#### 5.2.3 Quy tắc truy cập lesson detail
Trong `LessonService.getLessonById`:
```
if (course.published == false) {
    requireOwnerOrAdmin  → trả LessonResponse (kèm contentMarkdown)
}
if (currentUser là admin hoặc owner) {
    return toResponse(lesson)  // kèm contentMarkdown
}
if (enrollmentRepository.existsByUser_IdAndCourse_IdAndStatusAndDeletedFalse(ACTIVE)) {
    return toResponse(lesson)  // kèm contentMarkdown
}
throw FORBIDDEN_LESSON_ACCESS
```

### 5.3 Enrollment (EnrollmentService)

#### 5.3.1 Quy tắc nghiệp vụ
1. **Course phải:** `published=true` VÀ `archivedAt=null`.
2. **Mỗi user chỉ có TỐI ĐA 1 enrollment ACTIVE cho 1 course tại 1 thời điểm.**
3. **Re-enroll:** Nếu enrollment cũ status=DROPPED → set lại ACTIVE (cùng record, không tạo mới).
4. **Drop:** set status=DROPPED, **giữ nguyên progress** (user xem lại lịch sử được).
5. **Drop sau khi COMPLETED:** cho phép (edge case) — log warning, vẫn set DROPPED.
6. **Auto-complete:** khi progress 100% → `EnrollmentService.markAsCompletedIfFull` đổi status sang COMPLETED.

#### 5.3.2 Đặc biệt: ownership check
`dropEnrollment(enrollmentId)`:
- Owner (`enrollment.user.id == currentUser.id`) HOẶC ADMIN.
- Throw `FORBIDDEN_COURSE_ACCESS` nếu không phải 2 loại trên.

`dropEnrollmentByCourseId(courseId)`: tiện cho FE, không cần biết enrollmentId.

#### 5.3.3 markAsCompletedIfFull (Idempotent)
- Gọi từ `ProgressService.completeLesson` (không qua event, gọi trực tiếp qua bean injection — đảm bảo cùng transaction).
- Nếu enrollment không tồn tại hoặc đã COMPLETED → return.
- Nếu `progressPercent >= 100` → set COMPLETED, publish `CourseFullyCompletedEvent`.
- (Suy luận: Nếu dùng event, có thể không chạy được do chưa có consumer; gọi trực tiếp đảm bảo chắc chắn chạy.)

### 5.4 Progress (ProgressService)

#### 5.4.1 State machine
```
NOT_STARTED  --start()-->  IN_PROGRESS  --complete()-->  COMPLETED
                                                     ↑
                          re-complete (cùng lesson) → PROGRESS_ALREADY_COMPLETED (throw)
```
Trong `startLesson`:
- Nếu chưa có progress: tạo mới IN_PROGRESS, `isNewStart=true`, publish `LessonStartedEvent`.
- Nếu có và status=NOT_STARTED: → IN_PROGRESS, `isNewStart=true`, publish.
- Nếu có và status=IN_PROGRESS/COMPLETED: chỉ update `lastAccessedAt`, KHÔNG publish event (idempotent).

#### 5.4.2 Score rules (chỉ cho `LessonType.MINI_TEST`)

| `lessonType` | `request.getScore()` | Hành động |
|---|---|---|
| `MINI_TEST` | `null` | Throw `CANNOT_COMPLETE_LESSON` (DTO `@DecimalMin/@DecimalMax` cũng enforce) |
| `MINI_TEST` | 0-100 (BigDecimal) | OK |
| Khác `MINI_TEST` | `null` | OK |
| Khác `MINI_TEST` | có giá trị | Throw `INVALID_BEST_SCORE` |

**bestScore logic:**
- Chỉ update khi `score > bestScore` hiện tại.
- Nếu chưa từng complete (tạo mới progress) → set `bestScore = score` (nếu MINI_TEST), else `null`.
- Nếu re-complete (nhưng code hiện throw `PROGRESS_ALREADY_COMPLETED` ở nhánh này, nên không thực sự vào nhánh re-update) — suy luận: logic này dự phòng trường hợp sau này bỏ throw.

#### 5.4.3 Auto-complete enrollment
Sau khi save progress, `calculateCourseProgressPercent` (native SQL) → nếu `>= 100` → gọi `enrollmentService.markAsCompletedIfFull`.

#### 5.4.4 Công thức tính progress
Native SQL trong `UserLessonProgressRepository.calculateCourseProgressPercent`:
```sql
CASE
  WHEN (SELECT COUNT(*) FROM lessons l WHERE l.course_id = :courseId AND l.deleted = false) = 0
    THEN 0
  ELSE CAST(ROUND(
    (SELECT COUNT(*) FROM user_lesson_progress p
       JOIN lessons l ON p.lesson_id = l.id
      WHERE p.user_id = :userId AND l.course_id = :courseId
        AND p.status = 'COMPLETED' AND p.deleted = false)::numeric * 100.0
    / (SELECT COUNT(*) FROM lessons l WHERE l.course_id = :courseId AND l.deleted = false)
  ) AS INTEGER)
END
```
- Tử số: số progress COMPLETED của user trong course.
- Mẫu số: tổng lesson của course (chưa xoá).
- Làm tròn (`ROUND`) rồi ép Integer.
- Trả 0 nếu course rỗng.

> **Quan sát:** Query này **không filter theo lesson.deleted trong JOIN của user_lesson_progress**, chỉ filter ở mệnh đề `JOIN lessons l ON p.lesson_id = l.id` (không có điều kiện). Nếu lesson bị soft-delete, progress của lesson đó vẫn được đếm trong tử số. Đây có thể là chỗ chưa chuẩn xác — chưa xác định từ source code là intentional hay bug. (Suy luận: vì `LessonService.deleteLesson` chỉ soft-delete lesson, progress cũ không bị cascade → tử số có thể "không khớp" với mẫu số sau khi xoá lesson. Nhưng `LessonService.deleteLesson` cũng publish `CourseUpdatedEvent` nên có thể client FE biết để cập nhật UI.)

---

## 6. Soft-delete & cascade

### 6.1 Soft-delete cơ bản
Gọi `entity.softDelete()` → set `deleted=true`, `deletedAt=now()`. Record vẫn còn trong DB, chỉ không xuất hiện qua query có `AndDeletedFalse`.

### 6.2 Cascade khi xoá course
Trong `CourseService.deleteCourse`:
```
1. course.softDelete() + save
2. forEach lesson in lessonRepository.findByCourse_IdAndDeletedFalseOrderByOrderIndexAsc(courseId):
       lesson.softDelete() + save
3. forEach enrollment in enrollmentRepository.findByCourse_IdAndDeletedFalse(courseId):
       enrollment.softDelete() + save
4. log.warn("Progress cascade for courseId={} needs dedicated method — see TODO", courseId)
```
> **Đây là 1 TODO đã được ghi nhận trong code** (dòng 218 `CourseService.java`): cascade progress chưa được thực hiện. Progress của các lesson trong course sẽ **không bị soft-delete**, chỉ trở thành "orphan" (không còn lesson tương ứng đang active).
>
> **Suy luận:** Tác động có thể không nghiêm trọng vì:
> - `UserLessonProgressRepository.findByUser_IdAndLesson_Course_IdAndDeletedFalseOrderByLesson_OrderIndexAsc` filter theo `lesson.course.id` và `deleted=false` ở progress. Nhưng course đã bị soft-delete → từ phía `findByIdAndDeletedFalse(courseId)` sẽ trả empty, nên các API khác sẽ 404 trước.
> - Nhưng với native SQL `calculateCourseProgressPercent` không tham chiếu `course.deleted` — chỉ tham chiếu `lesson.deleted`. Nếu lesson bị soft-delete (như cascade ở bước 2) thì `WHERE l.deleted = false` loại trừ, OK. Nhưng nếu lesson chưa bị soft-delete (vì lý do gì), progress vẫn được đếm.

### 6.3 Cascade thực sự ở tầng JPA
`Lesson.items` có `cascade = CascadeType.ALL, orphanRemoval = true` — nếu xoá `Lesson` khỏi DB (không phải soft-delete), `LessonItem` liên quan cũng bị xoá. Nhưng trong code hiện tại **không có chỗ nào gọi `lessonRepository.delete()`**, đều dùng `softDelete()`.

---

## 7. Mapper & DTO

### 7.1 MapStruct config chung
- `@Mapper(componentModel = "spring")` → Spring tạo bean
- `nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE` → field null ở request không ghi đè field hiện tại khi update

### 7.2 Các mapper & method chính

| Mapper | Method | Mục đích |
|---|---|---|
| `CourseMapper` | `toSummaryResponse(course, totalLessons)` | List view |
| | `toDetailResponse(course, totalLessons, totalEnrollments, isEnrolled, progressPercent, lessons)` | Detail view |
| | `toEntity(req)` | Tạo mới; ignore `createdBy`, `published`, `archivedAt` |
| | `updateEntity(@MappingTarget course, req)` | Cập nhật; ignore các field tương tự |
| | `userToDisplayName` (default `@Named`) | Ưu tiên `fullName`, fallback `email` |
| `LessonMapper` | `toSummaryResponse(lesson)` | List view, **không** `contentMarkdown` |
| | `toResponse(lesson)` | Full view, **có** `contentMarkdown` |
| | `toViewResponse(lesson, includeContent)` (default method) | Dùng cho `CourseService.buildDetailResponse` — null `contentMarkdown` nếu `includeContent=false` |
| | `toEntity(req)` | Tạo mới; ignore `course`, `items`, `archivedAt` |
| | `updateEntity(@MappingTarget lesson, req)` | Tương tự |
| `EnrollmentMapper` | `toResponse(enrollment, progressPercent)` | Service tính progressPercent truyền vào |
| `ProgressMapper` | `toResponse(progress)` | Dùng `@Named` để map `lessonTitle`, `lessonOrderIndex`, `lessonTypeName` (vì cùng nguồn `lesson`) |

### 7.3 DTO Response — cấu trúc

| DTO | Field đặc biệt | Ai nhận |
|---|---|---|
| `CourseSummaryResponse` | `totalLessons`, `createdByUserId`, `createdByName` | List, admin list |
| `CourseDetailResponse` | + `totalEnrollments`, `isEnrolled` (Boolean: null anonymous), `progressPercent`, `lessons: List<LessonViewResponse>` | Detail |
| `LessonSummaryResponse` | KHÔNG có `contentMarkdown` | List lesson |
| `LessonResponse` | CÓ `contentMarkdown` | Lesson detail khi đủ quyền |
| `LessonViewResponse` | `@JsonInclude(NON_NULL)` → contentMarkdown ẩn nếu null | Dùng trong Course detail (1 DTO cho mọi mức quyền) |
| `EnrollmentResponse` | `progressPercent` (truyền từ Service), thông tin user + course | Tất cả enrollment API |
| `ProgressResponse` | `lessonType` là `String` (từ `lesson.getLessonType().name()`) | Progress API |
| `CourseProgressResponse` | `totalLessons, completedLessons, inProgressLessons, notStartedLessons, progressPercent, firstStartedAt as enrolledAt, lastAccessedAt, progress: List<ProgressResponse>` | GET /progress/courses/{id} |

### 7.4 DTO Request — validation

| DTO | Annotation chính |
|---|---|
| `CourseCreateRequest` | `@NotBlank` (title, slug), `@NotNull` (jlptLevel), `@Size` |
| `CourseUpdateRequest` | Tất cả optional; `@Size`; level không đổi được khi có enrollment (check ở Service) |
| `LessonCreateRequest` | `@NotBlank` (title), `@NotNull` (lessonType, orderIndex), `@Positive` (orderIndex), `@Min(1)` (durationMinutes), `@Size(max=50000)` (contentMarkdown) |
| `LessonUpdateRequest` | Tất cả optional; tương tự Create |
| `LessonCompleteRequest` | `score: BigDecimal`; `@DecimalMin("0.00") @DecimalMax("100.00")` (chỉ áp dụng khi MINI_TEST) |
| `LessonReorderRequest` | `lessonIds: List<Long>`; `@NotEmpty @NotNull` |

---

## 8. Event bus (in-process)

### 8.1 Tổng quan
- Tất cả event extends `CurriculumEvent extends ApplicationEvent`.
- `CurriculumEvent` có field `occurredAt: long` (auto = `System.currentTimeMillis()`).
- Publish qua `ApplicationEventPublisher.publishEvent(...)` trong mỗi Service.
- **Comment trong `CurriculumEvent.java` nói rõ:** "Sau này khi cần scale đa instance, chỉ cần thay bean ApplicationEventPublisher từ in-process sang Kafka (zero refactor ở producer)" — tức là kiến trúc này chuẩn bị sẵn cho việc chuyển sang Kafka sau.

### 8.2 Danh sách 9 event

| Event | Payload | Publish ở đâu |
|---|---|---|
| `CourseCreatedEvent` | courseId, title, createdByUserId | `CourseService.createCourse` |
| `CourseUpdatedEvent` | courseId, updatedByUserId | `CourseService.updateCourse/unpublishCourse/archiveCourse`; `LessonService.createLesson/updateLesson/reorderLessons/deleteLesson` |
| `CoursePublishedEvent` | courseId, title, jlptLevel (String), publishedByUserId | `CourseService.publishCourse` |
| `CourseDeletedEvent` | courseId, deletedByUserId | `CourseService.deleteCourse` |
| `UserEnrolledEvent` | userId, courseId, enrollmentId | `EnrollmentService.enrollInCourse` |
| `UserDroppedCourseEvent` | userId, courseId, enrollmentId | `EnrollmentService.dropEnrollment` |
| `CourseFullyCompletedEvent` | userId, courseId, enrollmentId | `EnrollmentService.markAsCompletedIfFull` |
| `LessonStartedEvent` | userId, lessonId, courseId | `ProgressService.startLesson` (chỉ khi `isNewStart=true`) |
| `LessonCompletedEvent` | userId, lessonId, courseId, bestScore (BigDecimal), isMiniTest (boolean), currentCourseProgressPercent (int) | `ProgressService.completeLesson` |

### 8.3 Quan trọng: KHÔNG có consumer
- Tìm toàn bộ `src/main/java` không có `@EventListener`, `@TransactionalEventListener`, hay `@KafkaListener` nào.
- **Event publish ra nhưng không có ai nghe** — chỉ là hook log/instrumentation cho tương lai.
- Comment trong `LessonCompletedEvent.java` gợi ý 4 nhóm consumer tương lai:
  1. **Gamification:** cộng XP, badge, streak.
  2. **LearningPath:** cập nhật progress cho daily_task, recalc target_date.
  3. **Notification:** gửi email "Chúc mừng hoàn thành bài X!".
  4. **Analytics:** cập nhật completion rate.

### 8.4 Có 1 chỗ dùng event-driven *trong nội bộ service*
`ProgressService.completeLesson` publish `LessonCompletedEvent`, **NHƯNG** việc auto-complete enrollment KHÔNG qua event — gọi trực tiếp `enrollmentService.markAsCompletedIfFull` (bean injection). Lý do suy luận: nếu qua event, khi chưa có consumer thì enrollment sẽ không bao giờ COMPLETED, gây bug.

---

## 9. Redis / Kafka / Cache

### 9.1 Redis — chỉ dùng cho auth
- `TokenBlacklistService`: lưu `jwtId` vào blacklist khi logout/refresh.
- `PasswordResetTokenService`: lưu token reset password.
- Cấu hình: `RedisTemplate<String, Object>` với `GenericJackson2JsonRedisSerializer` (ObjectMapper có JavaTimeModule, default typing giới hạn trong package `com.jp.elearningjp`, `java.util`, `java.time`, `java.math`, `java.lang`).
- `application-dev.yaml`: TTL 600000ms, key-prefix `elearningjp:`, không cache null.
- `@EnableCaching` được bận (trong `RedisConfig`).

### 9.2 Cache cho course/lesson — KHÔNG có
- `CourseService.updateCourse` có comment: "Invalidate cache (sẽ dùng @CacheEvict khi có Spring Cache cho course)" — đây là TODO, **chưa có @CacheEvict / @Cacheable nào trong module curriculum**.
- Toàn bộ query course/lesson đi thẳng xuống DB.

### 9.3 Kafka — KHÔNG có
- Không tìm thấy dependency hoặc config nào.
- Event hiện tại chỉ là in-process.

---

## 10. Helper class — CurrentUserService

File: `shared/util/CurrentUserService.java`. Đây là **helper duy nhất** cho việc "lấy current user" và check role ở Service layer.

### 10.1 Cách hoạt động

1. Đọc `SecurityContextHolder.getContext().getAuthentication()`.
2. Nếu principal là `Jwt` (do Spring Security OAuth2 Resource Server tạo) → đọc claim `userId` → query `userRepository.findById(userId)`.
3. Fallback: nếu thiếu claim `userId` → dùng `jwt.getSubject()` (= email) → `findUserByEmail`.
4. Throw `UNAUTHENTICATED` nếu không có auth, `USER_NOT_FOUND` nếu không tìm thấy user.

> **Suy luận:** Helper này tốn 1 query DB mỗi lần gọi. Vì Service method thường gọi nhiều lần (vd: check owner + check role + query data), có thể có N+1 query ở đây. Caching theo request scope (qua `@RequestScope` bean) là cải tiến tiềm năng.

### 10.2 Method public

| Method | Trả về | Throw |
|---|---|---|
| `getCurrentUser()` | `User` entity | `UNAUTHENTICATED`, `USER_NOT_FOUND` |
| `getCurrentUserId()` | `Long` | `UNAUTHENTICATED` |
| `getCurrentRoles()` | `Set<String>` (đã strip prefix `ROLE_`) | (không throw) |
| `hasRole(String roleName)` | `boolean` | (không throw) |
| `isAdmin()` | `boolean` | (không throw) |
| `isTeacher()` | `boolean` | (không throw) |
| `isTeacherOrAdmin()` | `boolean` | (không throw) |
| `getCurrentUserOrThrowIfMissing()` | alias `getCurrentUser()` | giống |
| `getCurrentRoleEntities()` | `Set<Role>` | (không throw) |

### 10.3 Lưu ý về role name
JWT scope được `AuthService.generateToken` build là `"ROLE_" + role.name` (xem `service/auth/AuthService.java`). Sau khi qua `JwtGrantedAuthoritiesConverter.setAuthorityPrefix("")`, authority lưu trong `Authentication` là `"ROLE_ADMIN"`, `"ROLE_TEACHER"`, `"ROLE_USER"` (vẫn còn prefix).

`getCurrentRoles()` có 1 dòng `auth.startsWith("ROLE_") ? auth.substring(5) : auth` — strip thêm lần nữa. Nhưng vì `setAuthorityPrefix("")` rồi thì authority đã là `"ROLE_ADMIN"`, nên strip thành `"ADMIN"`. (Suy luận: dòng này là dự phòng, vì config hiện tại không double-prefix.)

---

## 11. Lifecycle end-to-end của một Course

### 11.1 Sơ đồ tổng
```
[1] CREATE     POST /api/v1/courses  (TEACHER/ADMIN)
   → validate slug
   → save (status=DRAFT, published=false)
   → publish CourseCreatedEvent

[2] ADD LESSON POST /api/v1/courses/{id}/lessons  (owner/ADMIN)
   → check orderIndex unique
   → save lesson (status=DRAFT của lesson)
   → publish CourseUpdatedEvent

[3] PUBLISH    POST /api/v1/courses/{id}/publish  (owner/ADMIN)
   → set published=true
   → publish CoursePublishedEvent

[4] ENROLL     POST /api/v1/courses/{id}/enroll  (user authenticated)
   → check course.published && !archived
   → tìm existing enrollment
       • ACTIVE → throw ALREADY_ENROLLED
       • DROPPED → set ACTIVE (re-enroll)
       • chưa có → tạo mới
   → save
   → publish UserEnrolledEvent

[5] XEM DETAIL GET /api/v1/courses/{id}  (public, nhưng logic phức tạp)
   → buildDetailResponse:
       • if !published: requireOwnerOrAdmin (throw COURSE_NOT_PUBLISHED nếu fail)
       • totalLessons, totalEnrollments
       • isEnrolled (Boolean), progressPercent, canViewFullContent
       • lessons = list, mỗi lesson qua toViewResponse(lesson, canViewFullContent)

[6] START      POST /api/v1/progress/lessons/{lessonId}/start  (user đã enroll)
   → requireEnrolledUser (throw NOT_ENROLLED nếu không)
   → tìm existing progress
       • chưa có: tạo mới IN_PROGRESS, isNewStart=true
       • NOT_STARTED: → IN_PROGRESS, isNewStart=true
       • IN_PROGRESS/COMPLETED: chỉ update lastAccessedAt (idempotent)
   → save
   → if isNewStart: publish LessonStartedEvent

[7] COMPLETE   POST /api/v1/progress/lessons/{lessonId}/complete  (user đã enroll)
   → requireEnrolledUser
   → validate score theo lessonType (MINI_TEST bắt buộc, khác phải null)
   → tìm existing progress
       • COMPLETED: throw PROGRESS_ALREADY_COMPLETED
       • khác: set COMPLETED, completedAt=now, lastAccessedAt=now
         • if MINI_TEST && score > bestScore: update bestScore
       • chưa có: tạo mới
   → save
   → calculateCourseProgressPercent (native SQL)
   → publish LessonCompletedEvent
   → if >= 100: enrollmentService.markAsCompletedIfFull (cùng transaction)
       • set enrollment status = COMPLETED
       • publish CourseFullyCompletedEvent
```

### 11.2 Soft-delete flow
```
DELETE /api/v1/courses/{id}  (owner/ADMIN)
   → requireOwnerOrAdmin
   → course.softDelete() + save
   → cascade: lesson.softDelete() cho từng lesson
   → cascade: enrollment.softDelete() cho từng enrollment
   → TODO: progress cascade (chưa thực hiện)
   → publish CourseDeletedEvent
```

### 11.3 Reorder flow
```
POST /api/v1/courses/{id}/lessons/reorder  (owner/ADMIN)
body: { lessonIds: [3, 1, 2, 4] }   // thứ tự mong muốn
   → requireOwnerOrAdmin
   → validate mọi lessonId tồn tại trong course
   → validate số lượng khớp
   → Bump tất cả lesson orderIndex += 10000
       saveAll + flush  ← flush quan trọng
   → Set orderIndex mới từng cái qua updateOrderIndex JPQL
   → publish CourseUpdatedEvent
```

---

## 12. Helper / Guard / Interceptor can thiệp vào flow

| Hook / Guard | File / method | Vai trò |
|---|---|---|
| `requireOwnerOrAdmin(Course, user)` | `CourseService`, `LessonService` | Admin bypass; teacher phải là `createdBy.id == user.id` |
| `requireTeacherOrAdmin(user)` | `CourseService` | Tạo course: cần TEACHER/ADMIN |
| `requireEnrolledUser(userId, courseId)` | `ProgressService` | Start/Complete: cần enrollment ACTIVE |
| `getCourseOrThrow(id)` | `CourseService`, `LessonService` | `findByIdAndDeletedFalse` + throw `COURSE_NOT_FOUND` |
| `getLessonOrThrow(courseId, lessonId)` | `LessonService` | `findByIdAndCourse_IdAndDeletedFalse` + throw `LESSON_NOT_FOUND` (chống URL hack) |
| `buildDetailResponse(course)` | `CourseService` | Logic tổng hợp isEnrolled/progressPercent/canViewFullContent + map lessons |
| `JwtAuthenticationEntryPoint` (Bean) | `config/` | Trả 401 khi thiếu/sai JWT |
| `@Valid` ở Controller | Mọi POST/PUT | Validate DTO trước khi vào Service |
| `MapStruct` với `IGNORE_NULL` | Mọi mapper | Field null ở request không ghi đè khi update |
| `GlobalExceptionHandle` (suy luận) | `exception/` | Tập trung hoá error response: `AppException` → `ApiResponse{success:false, code:ErrorCode.code, message:ErrorCode.message}` |
| `AppException(ErrorCode)` | Mọi service | `RuntimeException` mang ErrorCode, GlobalException xử lý |
| `CurrentUserService` | `shared/util/` | Helper duy nhất cho "lấy current user" + role check |
| `LessonViewResponse.@JsonInclude(NON_NULL)` | DTO | 1 DTO phục vụ nhiều mức quyền (ẩn `contentMarkdown` khi null) |
| `LessonService.reorderLessons` 2-step | Service | Bump + flush + set mới, tránh UNIQUE conflict |
| `markAsCompletedIfFull` (gọi trực tiếp) | `EnrollmentService` | Auto-complete enrollment, đảm bảo cùng transaction |
| `calculateCourseProgressPercent` (native SQL) | `UserLessonProgressRepository` | Tính % progress, dùng nhiều nơi |

---

## 13. Điểm chưa xác định / hạn chế

> Phần này tổng hợp những chỗ source code chưa rõ ràng hoặc có khả năng gây bug.

### 13.1 Đã xác định từ source code

1. **Progress cascade khi xoá course chưa được thực hiện** (TODO trong `CourseService.deleteCourse` dòng 218, có `log.warn`). Progress sẽ trở thành "orphan" sau khi xoá course.
2. **Không có consumer cho 9 event** — event publish ra nhưng không có ai nghe. Khi có consumer (cho gamification/notification), cần implement `@EventListener` hoặc `@TransactionalEventListener`.
3. **Không có cache cho course/lesson list/detail** — comment TODO trong `CourseService.updateCourse`. Mỗi request list/detail đều query DB.
4. **Native SQL `calculateCourseProgressPercent`** filter `lesson.deleted=false` chỉ ở mệnh đề chính (đếm tổng) chứ không filter khi JOIN progress — có thể gây sai lệch nếu progress tồn tại cho lesson đã soft-delete. (Suy luận: vì cascade soft-delete lesson khi xoá course đã thực hiện, nên tỷ lệ này thường OK, nhưng vẫn là điểm cần kiểm tra.)
5. **`LessonService.deleteLesson` chỉ soft-delete lesson, không soft-delete progress** — tương tự trên, progress orphan nếu lesson bị xoá đơn lẻ.
6. **Error code `LESSON_TYPE_INVALID` bị reuse** trong `CourseService.getCoursesByLevel` khi parse enum fail (dòng 254) — suy luận: đây là typo, nên có code `JLPT_LEVEL_INVALID` riêng.
7. **`CourseEnrollment` không có field `completedAt`** — comment trong `EnrollmentService.markAsCompletedIfFull` thừa nhận: "Có thể set completedAt (nhưng CourseEnrollment chưa có field này) → bỏ qua". Khi hoàn thành course, không có timestamp.
8. **Manual Page convert trong `CourseService.getAllCoursesAdmin`** cho TEACHER — không pagination DB, có thể tải tất cả course của teacher rồi mới subList. Hiệu năng kém nếu teacher tạo nhiều course.
9. **`CurrentUserService.getCurrentUser()` tốn 1 query DB mỗi lần gọi** — không cache. Có thể tối ưu bằng `@RequestScope` bean.
10. **Trong `CourseService.getAllCoursesAdmin` cho TEACHER:** dùng `courseRepository.findByCreatedBy_IdAndDeletedFalse(userId)` (trả `List`) rồi manual subList. Nếu teacher tạo 1000 course, tải hết 1000 về RAM mỗi request.

### 13.2 Chưa xác định từ source code

1. **Migration / Flyway / Liquibase:** Chưa xác định có dùng migration tool nào. Các entity có annotation JPA, có thể dựa vào `ddl-auto` ở `application.yaml/dev.yaml`. **Chưa xác định từ source code.**
2. **Rate limiting, CSRF:** SecurityConfig disable CSRF, không thấy rate limiter. Có thể có ở layer khác (nginx, cloudflare).
3. **Pagination mặc định cho lesson list:** `GET /api/v1/courses/{id}/lessons` trả full list (không phân trang). Nếu course có 1000 lesson sẽ trả hết 1 lần. **Chưa xác định từ source code** là intentional.
4. **Transaction propagation khi gọi `enrollmentService.markAsCompletedIfFull` từ `ProgressService`:** cùng transaction (mặc định `REQUIRED`) — đảm bảo atomic. Nhưng nếu event-driven sau này, sẽ khác.
5. **Multi-instance deployment:** Event in-process không scale. Suy luận: dự án chuẩn bị cho Kafka nhưng chưa implement.
6. **Khi nào auto-complete enrollment fire?** Chỉ khi `progressPercent >= 100` SAU khi complete. Nếu user tạo progress 100% thông qua bulk import (chưa có API), sẽ không trigger.
7. **Authorization cho `GET /api/v1/courses/admin/all`:** chỉ check authenticated (URL `.anyRequest().authenticated()`), role check ở Service `requireTeacherOrAdmin`. Nếu `SecurityConfig` cấu hình sai, có thể lộ.

---

## Phụ lục A — Một số convention & best-practice trong code

- **Mọi query method đều kết thúc `AndDeletedFalse`** — đây là quy ước cứng, vi phạm sẽ trả record đã xoá.
- **`@Transactional(readOnly = true)` cho mọi read** — tối ưu Hibernate dirty checking.
- **Mọi write đều có `@Transactional`**.
- **Mọi controller method trả `ApiResponse<T>`** với cấu trúc `{success, code, message, data}`.
- **Mọi ID là `Long`**, auto-increment (`@GeneratedValue(strategy = IDENTITY)`).
- **Mọi timestamp là `Instant`**, không phải `LocalDateTime`.
- **Mọi soft-delete đều qua `softDelete()` method**, không gán trực tiếp `setDeleted(true)`.
- **Mọi event đều extends `CurriculumEvent`**, có `occurredAt` auto.
- **Mọi role check đều qua `CurrentUserService`**, không gọi `User.getRoles()` trực tiếp ở Service.
- **Mọi error đều throw `AppException(ErrorCode.XXX)`**, không throw trực tiếp RuntimeException.
- **Sử dụng MapStruct `@Named` qualified method** khi cùng một nguồn map sang nhiều field (vd: `lesson` → `lessonTitle`, `lessonOrderIndex`, `lessonTypeName`).
- **`@DynamicInsert` trên mọi entity** — Hibernate chỉ insert những cột NOT NULL, bỏ qua field null.

---

## Phụ lục B — Trace ví dụ: "User A enroll vào course X, hoàn thành lesson Y"

```
[1] User A gửi POST /api/v1/auth/login
    → AuthService tạo JWT với userId=A.id, scope=ROLE_USER
    → Trả token về client

[2] User A gửi POST /api/v1/courses/X/enroll  (kèm Bearer token)
    → SecurityFilter: CustomJwtDecoder decode → Authentication(principal=Jwt, authorities=[ROLE_USER])
    → SecurityConfig: anyRequest().authenticated() → pass
    → EnrollmentController.enrollInCourse(X)
    → EnrollmentService.enrollInCourse(X):
        - currentUserService.getCurrentUser() → query DB lấy User A (1 query)
        - courseRepository.findByIdAndDeletedFalse(X) → query course (1 query)
        - if !course.published → throw COURSE_NOT_PUBLISHED
        - if course.archivedAt != null → throw COURSE_ARCHIVED
        - enrollmentRepository.findByUser_IdAndCourse_IdAndDeletedFalse(A.id, X)
            - if ACTIVE → throw ALREADY_ENROLLED
            - if DROPPED → set ACTIVE
            - if empty → tạo mới
        - enrollmentRepository.save (1 query INSERT)
        - eventPublisher.publishEvent(UserEnrolledEvent)
        - progressRepository.calculateCourseProgressPercent(A.id, X) (native SQL)
        - enrollmentMapper.toResponse(saved, progressPercent)
    → Trả 201 ApiResponse<EnrollmentResponse>

[3] User A gửi GET /api/v1/courses/X  (kèm Bearer token)
    → SecurityFilter: pass (URL permit)
    → CourseController.getCourseById(X)
    → CourseService.getCourseById(X)
    → buildDetailResponse:
        - course.published == true → skip owner check
        - count lessons (1 query)
        - count active enrollments (1 query)
        - currentUserService.getCurrentUser() → query DB (1 query, có thể ở session cache)
        - findByUser_IdAndCourse_IdAndStatusAndDeletedFalse(ACTIVE) → (1 query)
        - isEnrolled=true, calculateCourseProgressPercent (1 native query)
        - lessons = findByCourse_IdAndDeletedFalseOrderByOrderIndexAsc (1 query)
        - map mỗi lesson qua toViewResponse(lesson, canViewFullContent=true)
    → Trả ApiResponse<CourseDetailResponse> với contentMarkdown

[4] User A gửi POST /api/v1/progress/lessons/Y/complete  body: {"score": 85.5}
    → ProgressController.completeLesson(Y, req)
    → ProgressService.completeLesson:
        - currentUserService.getCurrentUser() (1 query)
        - lessonRepository.findByIdAndDeletedFalse(Y) (1 query)
        - requireEnrolledUser:
            - enrollmentRepository.existsByUser_IdAndCourse_IdAndStatusAndDeletedFalse (1 query)
            - if !isEnrolled → throw NOT_ENROLLED
        - isMiniTest? (từ lesson entity đã load)
            - if true && score==null → throw CANNOT_COMPLETE_LESSON
            - if false && score!=null → throw INVALID_BEST_SCORE
        - findByUser_IdAndLesson_IdAndDeletedFalse(A.id, Y) (1 query)
        - if COMPLETED → throw PROGRESS_ALREADY_COMPLETED
        - set COMPLETED, completedAt=now, lastAccessedAt=now
            - if isMiniTest && score > bestScore → set bestScore=85.5
        - save (1 query UPDATE)
        - calculateCourseProgressPercent (1 native query)
        - publishEvent(LessonCompletedEvent)
        - if progressPercent >= 100:
            - enrollmentService.markAsCompletedIfFull(A.id, X)  ← cùng transaction
                - findByUser_IdAndCourse_IdAndStatusAndDeletedFalse(ACTIVE) (1 query)
                - calculateCourseProgressPercent (1 query, redundant nhưng idempotent)
                - set COMPLETED, save (1 query UPDATE)
                - publishEvent(CourseFullyCompletedEvent)
    → Trả 200 ApiResponse<ProgressResponse>
```

Tổng số query DB cho 1 lần complete: ~10-12 query, gồm 2 lần gọi `currentUserService.getCurrentUser()` (login + complete). Caching ở tầng helper có thể giảm đáng kể.

---

## Phụ lục C — File test liên quan

- `src/test/java/com/jp/elearningjp/controller/curriculum/CurriculumRoutingTest.java` — MockMvc standalone (không load Spring context), mock4 service, assert status ≠ 404 cho mọi endpoint để verify routing. (Chưa đọc chi tiết.)
- `http-tests/02-course-crud.http`, `03-lesson-crud.http`, `04-enrollment-flow.http`, `05-progress-flow.http` — REST Client file dùng để test thủ công qua IntelliJ/VSCode.

---

> **Ghi chú cuối:** Tài liệu này được viết dựa trên việc đọc trực tiếp source code (`*.java`) trong `BE/elearningJP/src/main/java/com/jp/elearningjp/`. Mọi phát biểu "Suy luận" là phần **tôi suy ra từ code**, không phải yêu cầu nghiệp vụ chính thức. Mọi phát biểu "Chưa xác định từ source code" là phần **tôi không tìm thấy trong code** và cần xác nhận từ nguồn khác (migration, design doc, FE, DevOps, v.v.).
