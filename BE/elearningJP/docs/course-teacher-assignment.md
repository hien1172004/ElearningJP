# Phân công giáo viên & duyệt nội dung khóa học — ma trận xung đột & cách xử lý

> Cập nhật: 2026-11-12. Tài liệu mô tả **hành vi hiện tại của code** (sau Phase 0–6 + "bỏ nháp" của
> `lesson-review-design.md`), không phải thiết kế mong muốn. Mục 7 liệt kê chỗ hành vi còn chưa chặt.
> Thiết kế, quyết định và lý do: `lesson-review-design.md`. Tổng quan module: `CURRICULUM_MODULE.md`.

## 1. Mô hình

| Thành phần | Vị trí | Ghi chú |
| --- | --- | --- |
| Phân công | V54 `course_teachers`, `CourseTeacherService` | 1 dòng / cặp (khóa, giáo viên), UNIQUE. Chỉ ADMIN giao / gỡ. Gỡ = xóa dòng, nội dung đã soạn giữ nguyên. |
| Bản nháp / bản live | Bảng soạn (`lessons`, `lesson_sections`, item, quiz…) = **bản nháp dùng chung** (mỗi bài 1 bản). Snapshot V56 (`course_published_versions` / `course_published_lessons`) = **bản học viên đang học** | Học viên đọc snapshot (`PublishedLessonResolver`); ADMIN / giáo viên được giao đọc bản nháp. |
| Duyệt **theo bài** (luồng chính) | V62 `lessons.review_status / has_unpublished_changes / draft_revision`, `lesson_review_requests`, `LessonReviewService` | Gửi nhiều bài một lần, mỗi bài độc lập. Duyệt / ADMIN lưu 1 bài = phiên bản live mới = bản live trước + bài đó. |
| Duyệt **cả khóa** (luồng cũ) | V55 `courses.review_status`, `course_review_requests`, `CourseReviewService` | Giáo viên không còn nút gửi cả khóa; ADMIN vẫn "Lưu cả khóa" (thứ tự bài, bài đã xóa, đề thi). Hai luồng chặn nhau khi đang chờ. |
| Phiên sửa của ADMIN | `LessonEditLockService` (Redis `lesson-edit-lock:{id}`, TTL 5 phút, heartbeat 60 giây, Lua so khớp người giữ) | ADMIN phải bấm "Sửa bài" mới ghi được nội dung bài; trong phiên, người khác chỉ xem. |
| Bỏ nháp | `LessonReviewService.discardDraft` + `LessonDraftRestoreRepository` | Bản nháp của bài quay về đúng bản live (khôi phục theo id từ snapshot). |
| Autosave | V63 `lesson_autosaves`, `LessonAutosaveService`, FE `useFormAutosave` | Bản chưa lưu theo (người, bài, form). Chỉ người viết thấy. |
| Kiểm quyền ghi | `CourseAccessGuard.requireLessonEditable(lesson, err, adminSessionRequired)` | Quyền theo khóa → bài chờ duyệt → phiên sửa ADMIN. Mọi thao tác ghi nội dung bài đi qua đây. |
| Chống "sửa / gửi duyệt cùng lúc" | `LessonRepository.markLessonDraftEdited` / `lockForReview` / `lockForPublish` | UPDATE có điều kiện trên cùng dòng `lessons` → Postgres xếp hàng theo khóa dòng, chỉ 1 bên thắng. |
| Xung đột version | `VersionCheck` + JPA `@Version` → `9001` | FE: modal "Tải bản mới nhất" + panel "Bản chưa lưu của bạn" (`ConcurrentEditModal`). |
| Realtime | `CourseContentRealtimeListener` → Redis → STOMP `/topic/courses/{id}/content` | Scope `DRAFT` / `PUBLISHED` / `LOCK`. |
| Thông báo | `LessonReviewNotificationListener`, `LessonLiveChangeNotificationListener`, `CourseReviewNotificationListener`, `CourseTeacherAssignmentNotificationListener` | Xem mục 5. |

Nguyên tắc:

- Chỉ **ADMIN** tạo / sửa thông tin / xóa / xuất bản khóa và giao / gỡ giáo viên. Mọi giáo viên được giao **quyền ngang nhau**.
- Thứ tự khóa dòng luôn là **khóa học → bài** (ghi nội dung, duyệt, lưu, bỏ nháp) để không deadlock.
- Xuất bản chỉ hiện lại **phiên bản đã duyệt gần nhất**; nội dung mới tới học viên chỉ qua duyệt / ADMIN lưu.

## 2. Ma trận quyền

### 2.1 Theo bài (luồng chính)

| Thao tác | ADMIN đang giữ phiên sửa bài | ADMIN chưa mở phiên | GV được giao — bài đang soạn | Bất kỳ ai — bài **chờ duyệt** | ADMIN **khác** đang giữ phiên | GV không được giao / đã gỡ |
| --- | --- | --- | --- | --- | --- | --- |
| Ghi nội dung bài (section, item, quiz, luyện nói, import) | ✅ | ❌ `1251` | ✅ | ❌ `1240` (kể cả ADMIN) | ❌ GV `1250`, ADMIN `1252` | ❌ `1500` / `1501` |
| Sửa tiêu đề / xóa bài (trang khóa) | ✅ | ✅ (không cần phiên) | ✅ | ❌ `1240` | ❌ `1250` / `1252` | ❌ |
| Gửi duyệt (kèm `draftRevision`) | — | ❌ `1245` | ✅ nếu có thay đổi | ❌ `1241` | ❌ `1250` | ❌ `1245` |
| Rút yêu cầu | ❌ `1246` | ❌ `1246` | ✅ **mọi GV đang được giao** | ✅ (đúng lúc chờ) | ✅ | ❌ `1246` (kể cả người gửi đã bị gỡ) |
| Duyệt / từ chối (bắt buộc lý do) | ✅ | ✅ | ❌ | ✅ | ✅ | ❌ |
| Lưu bài lên bản live | ✅ (kết thúc phiên) | ✅ | ❌ | ❌ `1240` | ❌ `1252` | ❌ |
| Bắt đầu phiên sửa | ✅ (gia hạn) | ✅ | ❌ | ❌ `1240` | ❌ `1252` (hoặc **tiếp quản**) | ❌ |
| Bỏ nháp, sửa từ bản live | ✅ | ❌ `1251` | ❌ | ❌ `1240` | ❌ `1252` | ❌ |
| Autosave (đọc / ghi bản của mình) | ✅ | ✅ | ✅ | ✅ | ✅ | ❌ `1501` |

### 2.2 Cả khóa (luồng cũ, còn cho ADMIN lưu cả khóa và yêu cầu gửi trước đây)

| Thao tác | ADMIN | GV được giao, khóa `EDITING` | GV được giao, khóa `PENDING_REVIEW` | GV không được giao |
| --- | --- | --- | --- | --- |
| Ghi nội dung | ✅ (vẫn theo khóa bài / phiên ở 2.1) | ✅ | ❌ `1108` | ❌ `1500` |
| Gửi duyệt cả khóa | ❌ `1119` | (FE không còn nút; API còn) — bị chặn `1123` khi có bài chờ duyệt theo bài | ❌ `1109` | ❌ `1119` |
| Rút / Duyệt / Từ chối yêu cầu cả khóa | Duyệt / Từ chối | Rút (mọi GV được giao) | Rút | ❌ |
| Lưu cả khóa | ✅ khi không có yêu cầu chờ (`1120`) và không có bài chờ duyệt (`1123`) | ❌ | ❌ | ❌ |

## 3. Ma trận xung đột — thao tác chồng nhau

Cột "Cơ chế" là thứ thực sự chặn / phát hiện. Dòng đánh dấu ✔IT có integration test trên Postgres / Redis thật.

### 3.1 Phân công

| # | Tình huống | Cơ chế | Kết quả |
| --- | --- | --- | --- |
| A1 | Giao GV đã được giao | `existsByCourseIdAndTeacherId` | `409 1106` |
| A2 | Hai ADMIN giao cùng GV **cùng lúc** | UNIQUE `uq_course_teachers_course_teacher` → `knownConstraintError` | Một bên thắng; bên kia `409 1106` (không 500) |
| A3 | Gỡ GV chưa được giao | | `404 1107` |
| A4 | Hai ADMIN gỡ cùng lúc | `@Version` | Bên sau `409 9001` → modal "Tải bản mới nhất" |
| A5 | Giao tài khoản bị khóa / đã xóa / không có role TEACHER | lọc trong `assignTeacher` | `400 1105` |
| B1 | Gỡ GV đang mở trang soạn | Kiểm phân công ở mỗi request | Lần ghi kế tiếp `403`; đã lưu thì giữ. Có thông báo chuông, **không** có realtime trên topic khóa (mục 7) |
| B2 | Gỡ GV là người gửi yêu cầu đang chờ | `requireAssignedTeacher` khi rút | Yêu cầu vẫn chờ; GV đã gỡ không rút được; GV còn lại rút được |
| B3 | Khóa / đổi role tài khoản GV | thu hồi token | Dòng phân công giữ nguyên; mở lại → quyền tự khôi phục |
| B4 | Xóa mềm khóa | `findByIdAndDeletedFalse` | Mọi thao tác `1100`; hàng chờ lọc khóa đã xóa |

### 3.2 Nhiều người cùng soạn

| # | Tình huống | Cơ chế | Kết quả / FE |
| --- | --- | --- | --- |
| C1 | Hai người sửa **cùng** bản ghi (item, câu hỏi, section, thông tin bài / khóa) | `VersionCheck` + `@Version` | Bên sau `9001` → modal 1 nút **"Tải bản mới nhất"** (không hiện 409, không có ghi đè) → form sửa tự đóng, nội dung vừa gửi mở ở panel **"Bản chưa lưu của bạn"** để chép lại; bản tự lưu của form vẫn giữ |
| C2 | Hai người sửa **khác** bản ghi cùng bài | `markLessonDraftEdited` khóa dòng bài ngắn | Không lỗi, chỉ xếp hàng vài ms; người kia nhận push `DRAFT` → thẻ "Tải lại" |
| C3 | Ghi bị rollback | listener `AFTER_COMMIT` | Không push / không thông báo |

### 3.3 Soạn × duyệt theo bài

| # | Tình huống | Cơ chế | Kết quả |
| --- | --- | --- | --- |
| D1 | Gửi duyệt và sửa **cùng lúc** ✔IT | `markLessonDraftEdited` (WHERE EDITING) và `lockForReview` (WHERE draft_revision = bản đang thấy) trên cùng dòng | Chỉ 1 bên thắng: sửa trước → gửi nhận `9001` cho bài đó (modal, phải bấm gửi lại — không tự gửi); gửi trước → sửa nhận `1240` |
| D2 | Gửi nhiều bài, vài bài lỗi | mỗi bài 1 giao dịch (`TransactionTemplate`) | Bài hợp lệ vẫn gửi; kết quả từng bài (`9001` / `1241` / `1243` / `1249` / `1250`); ADMIN nhận **1** thông báo cho cả lượt |
| D3 | Hai GV gửi cùng bài cùng lúc ✔IT | partial unique `uq_lesson_review_requests_pending` + điều kiện UPDATE | Một bên thắng; bên kia `1241` |
| D4 | Sửa bài đang chờ duyệt (kể cả ADMIN) ✔IT | `review_status = 'EDITING'` trong UPDATE | `409 1240`; FE khóa nút + banner |
| D5 | GV rút trong lúc ADMIN duyệt / từ chối | cả hai khóa dòng khóa học trước khi đọc yêu cầu | Người sau `1242` kèm kết quả của người trước ("đã được X duyệt / từ chối lúc HH:mm dd/MM/yyyy. Lý do: …") |
| D6 | Hai ADMIN cùng duyệt / từ chối | như D5 | Người đầu thắng; người sau được báo kết quả của người trước |
| D7 | Hai ADMIN duyệt **hai bài khác nhau** cùng khóa cùng lúc | `findByIdForUpdate` (SELECT … FOR UPDATE khóa học) | Xếp hàng; phiên bản live thứ 2 gồm cả bài thứ nhất; không vướng `version_no` |
| D8 | ADMIN lưu bài khi bài chờ duyệt / không có thay đổi | `lockForPublish` | `1240` / `1243` |
| D9 | ADMIN lưu bài → GV đang mở bài gửi duyệt bằng bản cũ | `lockForPublish` tăng `draft_revision` | Gửi nhận `9001` → tải bản mới nhất rồi gửi lại |
| D10 | Gửi cả khóa khi có bài chờ theo bài / gửi bài khi cả khóa đang chờ | `countPendingReviewLessons` / `courses.review_status` | `1123` / `1247` |
| D11 | ADMIN lưu cả khóa | `snapshotDraft` → `clearLessonChanges` | Mọi bài hết "có thay đổi"; bị chặn nếu có bài chờ (`1123`) |

### 3.4 Phiên sửa của ADMIN (lock) ✔IT (Redis thật)

| # | Tình huống | Cơ chế | Kết quả / FE |
| --- | --- | --- | --- |
| E1 | ADMIN mở trang soạn bài | — | Chỉ xem; banner "Bấm Sửa bài để bắt đầu phiên sửa" |
| E2 | Bấm "Sửa bài" khi bài có thay đổi chưa duyệt | FE đọc `hasUnpublishedChanges` + `/pending-changes` | Hộp cảnh báo: ai sửa, lúc nào, tóm tắt; chọn **sửa tiếp trên nháp** hoặc **bỏ nháp, sửa từ bản live** (bài đã có trong bản live) |
| E3 | GV / ADMIN khác lưu / gửi duyệt khi ADMIN giữ phiên | `requireCanWrite` | GV `1250`, ADMIN `1252`; banner "Quản trị viên X đang sửa bài này (từ …)", nút Lưu / Gửi duyệt tắt; autosave vẫn ghi |
| E4 | ADMIN khác **tiếp quản** | SET đè giá trị Redis | Người cũ: heartbeat / lần ghi kế tiếp `1251` ("phiên đã kết thúc") |
| E5 | Đóng tab / mất mạng | TTL 5 phút | Khóa tự hết; banner người khác cập nhật trong ≤ 30 giây (làm mới định kỳ) |
| E6 | Rời trang soạn bài / bấm "Hủy sửa" | `RELEASE_IF_OWNER` | Nhả khóa; phần đã ghi vẫn ở bản nháp (chưa lên live) |
| E7 | "Lưu bài" | `releaseAfterCommit` | Lên live rồi mới nhả khóa |
| E8 | Bắt đầu sửa bài chờ duyệt | `acquire` đọc `review_status` | `1240` |
| E9 | Redis lỗi | | ADMIN không bắt đầu / tiếp tục sửa được (`1253`); GV vẫn ghi được |
| E10 | Giá trị lock hỏng trong Redis | `parse` → null | Coi như không có khóa, không chặn ai |

### 3.5 Bỏ nháp, sửa từ bản live ✔IT

| # | Tình huống | Cơ chế | Kết quả |
| --- | --- | --- | --- |
| F1 | ADMIN chọn "bỏ nháp" (2 bước xác nhận) | bắt đầu phiên → `discardDraft` (cần giữ phiên) | Bản nháp = bản live: dòng cũ khôi phục theo id, dòng mới thêm bị xóa mềm, dòng đã xóa hẳn (đáp án quiz, luyện nói) tạo lại; tiêu đề bài về bản live; bài hết "có thay đổi" |
| F2 | Bài chưa từng duyệt | snapshot không có bài | `1257` (FE không hiện lựa chọn) |
| F3 | Người khác đang mở form sửa bản ghi vừa khôi phục | mọi dòng khôi phục tăng `version` | Lưu → `9001` → modal "Tải bản mới nhất" |
| F4 | GV đang có bản tự lưu của bài | autosave không bị xóa | Mở lại form → bản ghi đã đổi version → "Mở để chép lại" (không điền đè) |
| F5 | Snapshot lệch dữ liệu hiện tại (từ điển / kịch bản đã đổi) | `draftMatchesLive` sau khôi phục | Vẫn khôi phục; ghi log cảnh báo |
| F6 | GV gửi duyệt bằng bản cũ ngay sau khi bỏ nháp | `draft_revision` tăng | `9001` / không còn thay đổi `1243` |

### 3.6 Autosave ✔IT

| # | Tình huống | Cơ chế | Kết quả |
| --- | --- | --- | --- |
| G1 | Gõ trong form | debounce 1,5 giây; đóng form / rời trang → lưu ngay | Upsert theo (người, bài, form); **không** đổi version / `draft_revision` của bài |
| G2 | Lưu thành công | `clear()` — DELETE chạy **sau** lệnh lưu đang gửi | Bản tự lưu bị xóa, không "sống lại" |
| G3 | Mở lại form có bản tự lưu | so `baseVersion` với version bản ghi | Chưa đổi → "Khôi phục" điền cả form; đã đổi → "Mở để chép lại" ở panel |
| G4 | Bài chờ duyệt / ADMIN đang sửa | autosave không đi qua `requireLessonEditable` | Vẫn ghi được |
| G5 | GV đã bị gỡ | `requireAssignedOrAdmin` | `1501`, không đọc / ghi được |
| G6 | Quá 64 KB / quá 50 bản / mã form lạ | | `1254` / `1256` / `1255` |
| G7 | Bản không đụng tới > 30 ngày | job 3:30 hằng ngày + ShedLock | Bị dọn |

## 4. Realtime

| Sự kiện | Scope | Ai cập nhật |
| --- | --- | --- |
| Mỗi thao tác ghi nội dung | `DRAFT` | Người soạn đang mở khóa (trừ người thao tác) — thẻ "Tải lại" |
| Gửi / rút / từ chối / bỏ nháp theo bài; gửi / rút / từ chối cả khóa | `DRAFT` | Người soạn (khóa / mở khóa nút) |
| Duyệt / lưu (bài hoặc cả khóa) khi khóa đang xuất bản | `PUBLISHED` | Người soạn + học viên |
| Duyệt / lưu khi khóa nháp | `DRAFT` | Người soạn |
| Xuất bản / chuyển nháp | `PUBLISHED` | Người soạn + học viên |
| Bắt đầu / kết thúc / tiếp quản phiên sửa | `LOCK` | Người soạn: làm mới trạng thái khóa, **không** hiện "Tải lại" |
| Phiên sửa hết hạn (TTL) | — | Không có tín hiệu; FE trang soạn bài làm mới trạng thái mỗi 30 giây |
| Giao / gỡ giáo viên | — | Chỉ thông báo chuông |

Push qua kênh Redis `course-content:push` (mọi instance); payload chỉ `courseId`, `scope`, `actorId`, client tự gọi lại REST.

## 5. Thông báo

| Loại | Người nhận | Khi |
| --- | --- | --- |
| `COURSE_TEACHER_ASSIGNMENT` | GV được giao / gỡ | Giao / gỡ |
| `LESSON_SUBMITTED` / `LESSON_WITHDRAWN` | Mọi ADMIN (1 thông báo / lượt gửi) | Gửi / rút theo bài |
| `LESSON_APPROVED` / `LESSON_REJECTED` | GV đã gửi | Duyệt / từ chối (kèm lý do) |
| `LESSON_DRAFT_DISCARDED` | Mọi GV được giao | ADMIN bỏ nháp |
| `DRAFT_STALE` | Người còn bản tự lưu của bài (lưu / duyệt cả khóa: của bài bất kỳ trong khóa) | Bản live của bài đổi |
| `LESSON_UPDATED` | Học viên đã hoàn thành bài | Duyệt / lưu bài có tick "Thay đổi lớn" và khóa đang xuất bản |
| `COURSE_REVIEW` | Theo luồng cả khóa | Gửi / rút / duyệt / từ chối / lưu / xuất bản / chuyển nháp cả khóa |

Không gửi cho người thao tác; listener chạy AFTER_COMMIT, lỗi từng người nhận chỉ log.

## 6. Mã lỗi

| Mã | Tên | HTTP |
| --- | --- | --- |
| 1100 | `COURSE_NOT_FOUND` | 404 |
| 1105 / 1106 / 1107 | `COURSE_TEACHER_INVALID` / `_ALREADY_ASSIGNED` / `_NOT_ASSIGNED` | 400 / 409 / 404 |
| 1108 – 1122 | luồng duyệt cả khóa (`COURSE_PENDING_REVIEW_LOCKED`, `COURSE_REVIEW_*`, `COURSE_SAVE_*`, `COURSE_NOT_YET_APPROVED`) | |
| 1123 | `COURSE_REVIEW_LESSONS_PENDING` | 409 |
| 1240 | `LESSON_PENDING_REVIEW_LOCKED` | 409 |
| 1241 / 1242 / 1243 / 1244 | `LESSON_REVIEW_ALREADY_PENDING` / `_NOT_PENDING` / `_NO_CHANGES` / `_REQUEST_NOT_FOUND` | 409 / 409 / 400 / 404 |
| 1245 / 1246 | `LESSON_REVIEW_SUBMIT_FORBIDDEN` / `_WITHDRAW_FORBIDDEN` | 403 |
| 1247 / 1248 / 1249 | `LESSON_REVIEW_COURSE_PENDING` / `_EMPTY` / `_NOT_IN_COURSE` | 409 / 400 / 400 |
| 1250 / 1251 / 1252 / 1253 | `LESSON_ADMIN_EDITING` / `LESSON_EDIT_SESSION_REQUIRED` / `LESSON_LOCKED_BY_OTHER_ADMIN` / `LESSON_EDIT_LOCK_UNAVAILABLE` | 409 / 409 / 409 / 503 |
| 1254 / 1255 / 1256 | `LESSON_AUTOSAVE_TOO_LARGE` / `_KEY_INVALID` / `_TOO_MANY` | 400 |
| 1257 | `LESSON_NOT_IN_LIVE` | 400 |
| 1500 / 1501 | `FORBIDDEN_COURSE_ACCESS` / `FORBIDDEN_LESSON_ACCESS` | 403 |
| 9001 | `CONCURRENT_MODIFICATION` | 409 (FE: modal "Tải bản mới nhất") |

## 7. Điểm còn hở (chưa sửa)

Xếp theo mức độ:

1. **ADMIN phải mở phiên sửa mới sửa được nội dung bài** — đúng thiết kế, nhưng đổi cách làm việc của ADMIN; cần báo trước khi phát hành.
2. **Kiểm khóa sửa không nguyên tử với giao dịch DB** — GV lưu đúng lúc ADMIN vừa bắt đầu phiên (cửa sổ vài ms) vẫn ghi được.
3. **Bỏ nháp xóa hẳn phần chưa duyệt** — không có bản sao lưu; chỉ bản tự lưu (form đang gõ dở) còn lại.
4. **Gỡ giáo viên không push realtime** (B1) — trang soạn đang mở vẫn hiện nút tới khi gặp 403.
5. **Danh sách GV liệt kê cả tài khoản đã xóa mềm / bị khóa**; phân công của tài khoản bị khóa tự sống lại khi mở khóa.
6. **Autosave chỉ kích hoạt khi gõ** — thao tác chỉ bằng chuột (chọn mục, thêm / xóa dòng) chưa tự lưu tới lần gõ kế tiếp.
7. **`DRAFT_STALE` có thể báo thừa** — gửi cho mọi người còn bản tự lưu của bài, kể cả form không liên quan phần vừa đổi.
8. **Thứ tự bài / bài đã xóa / đề thi** chỉ tới học viên khi ADMIN lưu cả khóa (không có luồng duyệt riêng).

Đã sửa trong quá trình: rút yêu cầu chỉ cho GV đang được giao; vi phạm unique trả 409 nghiệp vụ thay vì 500; tài liệu module cũ đã cập nhật theo mô hình phân công.

## 8. Vị trí code

- Guard: `CourseAccessGuard.requireLessonEditable` (`shared/util/CourseAccessGuard.java`, dòng 220).
- Duyệt theo bài: `LessonReviewService` (`service/curriculum/LessonReviewService.java`) — `submit` 98, `withdraw` 208, `approve` 239,
  `reject` 254, `saveLesson` 274, `discardDraft` 316, `lockAndFindPending` 453.
- UPDATE có điều kiện: `LessonRepository.markLessonDraftEdited` / `lockForReview` / `lockForPublish`.
- Snapshot theo bài: `CourseSnapshotService.publishLessons` (dòng 118), tự kiểm `draftMatchesLive` (dòng 219).
- Bỏ nháp: `LessonDraftRestoreRepository.restore` (dòng 47).
- Phiên sửa: `LessonEditLockService` — `acquire` 96, `heartbeat` 140, `release` 157, `requireCanWrite` 211.
- Autosave: `LessonAutosaveService.save` (dòng 73), `cleanup` (dòng 101); FE `FE/src/hooks/useFormAutosave.ts`.
- Map lỗi DB: `GlobalExceptionHandle.knownConstraintError` (dòng 116).
- FE: `ConcurrentEditModal`, `LessonReviewPanel`, `LessonReviewActions`, `LessonEditSessionControls`, `LessonReviewQueue`,
  `editLockContext.lessonEditLockReason`.
