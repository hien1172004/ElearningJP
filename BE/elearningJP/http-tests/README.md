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

### Lesson
- [x] 3.1-3.3: Tạo 3 loại lesson (THEORY, VOCABULARY, MINI_TEST)
- [x] 3.4: orderIndex trùng → 409
- [x] 3.5: Student tạo lesson → 401/403
- [x] 3.7: Student chưa enroll xem detail → 403
- [x] 3.8: Update lesson
- [x] 3.9: Reorder
- [x] 3.10: Reorder sai số lượng → 400
- [x] 3.11-3.12: Delete + 404 sau delete

### Enrollment
- [x] 4.1: Enroll OK
- [x] 4.2: Enroll lần 2 → 409
- [x] 4.6: Xem enrollment người khác → 403
- [x] 4.7-4.8: Sau khi enroll → xem được content
- [x] 4.9-4.10: Drop + double drop → 400
- [x] 4.11: Re-enroll sau drop
- [x] 4.12: Drop theo courseId

### Progress
- [x] 5.1-5.2: Start → complete THEORY
- [x] 5.3: Complete lại → 400
- [x] 5.10: MINI_TEST thiếu score → 400
- [x] 5.11: MINI_TEST có score OK
- [x] 5.13: THEORY có score → 400 (sai rule)
- [x] 5.14: Complete khi chưa enroll → 403

### Auto-complete flow
- [x] 5.8: GET progress → 100% khi complete hết
- [x] 5.9: Enrollment ACTIVE → COMPLETED tự động

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
