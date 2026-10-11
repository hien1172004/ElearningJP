# HƯỚNG DẪN TEST CURRICULUM MODULE

## Công cụ cần thiết

1. **VS Code Extension**: [REST Client](https://marketplace.visualstudio.com/items?itemName=humao.rest-client)
2. **App đang chạy**: `cd BE/elearningJP && mvn spring-boot:run`

## Thứ tự chạy

Mở từng file theo thứ tự và click "Send Request" phía trên mỗi request:

| # | File | Mô tả | Pre-req |
|---|------|-------|---------|
| 1 | `01-auth.http` | Đăng ký + đăng nhập TEACHER/USER | App running |
| 2 | `02-course-crud.http` | Course CRUD + publish + state transition | File 1 |
| 3 | `03-lesson-crud.http` | Lesson CRUD + reorder + delete | File 2 |
| 4 | `04-enrollment-flow.http` | Enroll/drop + re-enroll + permission check | File 3 |
| 5 | `05-progress-flow.http` | Start/complete lesson + score + auto-complete | File 4 |
| 6 | `06-lesson-section-flow.http` | Section CRUD (KANJI/VOCAB/GRAMMAR/TEST) | File 3 |
| 7 | `07-lesson-items-flow.http` | Items CRUD cho từng section type | File 6 |
| 8 | `08-create-lesson-full-flow.http` | **Quy trình tạo 1 lesson hoàn chỉnh** (course → lesson → 4 section → items → quiz) | File 1 |
| 9 | `09-course-ownership-test.http` | **Test ownership**: teacher chỉ sửa được course/lesson do mình tạo; admin bypass | File 1 |
| 10 | `10-order-index-flow.http` | **E2E test logic orderIndex**: auto max+1, shift khi chen đầu, move-up/move-down, clamp về MAX | File 1 |

## Quy ước trong file

- Comment `# @name xxx` → đặt tên cho response, có thể dùng `{{xxx.response.body.data.id}}` ở file khác
- Comment `###` → phân cách request
- Comment bắt đầu bằng `(EXPECTED xxx)` → kỳ vọng HTTP status code
- `@variable` → khai báo biến tạm trong file

## Test cases quan trọng cần cover

### Auth & Permission
- [x] 1.1: Register TEACHER
- [x] 1.2: Register USER
- [x] 1.3: Login TEACHER (ghi nhớ token)
- [x] 1.4: Login USER (ghi nhớ token)
- [x] 1.5: Introspect token

### Course
- [x] 2.1: Teacher tạo course OK
- [x] 2.3: Slug trùng → 409
- [x] 2.4: Student tạo course → 401/403
- [x] 2.5-2.8: Public read variants
- [x] 2.9: Update course
- [x] 2.10: Publish course
- [x] 2.12: Teacher xem course của mình (admin/all)
- [x] 2.14-2.15: Archive + unpublish

### Lesson (SAU REFACTOR V14)
- [x] 3.1-3.3: Tạo 3 lesson (body chỉ có title + orderIndex, KHÔNG có lessonType/contentMarkdown)
- [x] 3.4: Tạo lesson thiếu title → 400
- [x] 3.5: Student tạo lesson → 403
- [x] 3.7: Student chưa enroll xem detail → 403
- [x] 3.8-3.10: Update lesson (title/orderIndex)
- [x] 3.12-3.13: Đổi orderIndex từng lesson + orderIndex ≤ 0 → 400
- [x] 3.14-3.15: Delete + 404 sau delete
- [x] **Xem thêm file 10 cho test chuyên sâu về shift/auto max+1/clamp**

### Enrollment
- [x] 4.1: Enroll OK
- [x] 4.2: Enroll lần 2 → 409
- [x] 4.6: Xem enrollment người khác → 403
- [x] 4.7-4.8: Sau khi enroll → xem được content
- [x] 4.9-4.10: Drop + double drop → 400
- [x] 4.11: Re-enroll sau drop
- [x] 4.12: Drop theo courseId

### Progress (SAU REFACTOR V14)
- [x] 5.1-5.2: Start → complete lesson KHÔNG có section TEST (không cần score)
- [x] 5.3: Complete lại → 400
- [x] 5.6: Tạo section TEST cho lesson #3
- [x] 5.7: Lesson CÓ section TEST thiếu score → 400
- [x] 5.8: Lesson CÓ section TEST có score OK
- [x] 5.12: Lesson KHÔNG có section TEST nhưng gửi score → 400
- [x] 5.13: Complete/start khi chưa enroll → 403

### Auto-complete flow
- [x] 5.8: GET progress → 100% khi complete hết
- [x] 5.9: Enrollment ACTIVE → COMPLETED tự động

### Quy trình tạo 1 lesson hoàn chỉnh (file 08)
File `08-create-lesson-full-flow.http` đi qua đầy đủ 7 bước:
1. Tạo course + publish
2. Tạo lesson (chỉ ADMIN)
3. Tạo 4 section: KANJI / VOCABULARY / GRAMMAR / TEST
4. Thêm items vào từng section (kanji + related vocab / vocab + examples / grammar + examples)
5. Tạo quiz cho section TEST
6. Thêm câu hỏi cho quiz
7. Verify toàn bộ bằng cách GET lesson + từng section

### Phân quyền sau refactor V15 (file 09)
- **ADMIN**: full quyền course/lesson (bypass ownership)
- **TEACHER**: được tạo/sửa/xoá/publish course & lesson, NHƯNG chỉ với course do mình tạo (course.createdBy = currentUser). Nếu sửa course/lesson của teacher khác → 403 FORBIDDEN_COURSE_ACCESS / FORBIDDEN_LESSON_ACCESS
- **STUDENT**: luôn 403 với các endpoint write
- Endpoint `GET /api/v1/courses/admin` vẫn ADMIN-only (teacher không xem được)

### OrderIndex flow (file 10)
Test e2e cho logic MAX+1 / SHIFT / CLAMP của `OrderIndexResolver`:
- **A. Auto max+1**: tạo lesson không gửi `orderIndex` → service tự gán max+1
- **B. Shift khi chen đầu**: tạo mới với `orderIndex=1` → các lesson cũ được shift lên +1
- **C. Move-up**: cập nhật `orderIndex` từ cuối về đầu → shift các lesson giữa
- **D. Move-down**: cập nhật `orderIndex` từ đầu về cuối → shift các lesson giữa
- **E. Clamp về MAX**: cập nhật `orderIndex=99` khi max=4 → kẹp về 4
- **F. Section orderIndex tự gán max+1**
- **G. Kanji item orderIndex tự gán max+1 + shift khi chen đầu**

> Mỗi step trong file 10 đều có comment `# → expect ...` để người chạy tự verify response.

## Lỗi hay gặp

### 401 Unauthorized
- Token sai/hết hạn → gọi lại 1.3/1.4 để lấy token mới
- Thiếu `Authorization: Bearer xxx` header

### 403 Forbidden
- Đúng user nhưng sai role (student cố tạo course)
- Đúng role nhưng không phải owner (teacher cố sửa course teacher khác)
- Student xem lesson content khi chưa enroll

### 409 Conflict
- Slug trùng khi tạo course
- orderIndex trùng trong cùng course
- Enroll lần 2 khi đang ACTIVE

### 404 Not Found
- ID không tồn tại
- Resource đã soft-delete

## Lưu ý

- Token lưu trong `@studentToken` / `@teacherToken` chỉ dùng trong cùng session REST Client
- Khi restart app, cần đăng nhập lại
- Mỗi lần test cần re-create user vì email là unique
