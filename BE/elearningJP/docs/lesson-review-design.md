# Thiết kế: duyệt theo bài, lock của admin, xử lý xung đột, autosave

> Trạng thái: **đã triển khai xong Phase 0–7 và "bỏ nháp"** (mục 7). Ngày chốt: 2026-11-11; hoàn tất: 2026-11-12.
> Hành vi hiện tại + ma trận xung đột: `course-teacher-assignment.md`.
> Hiện trạng trước thiết kế này (duyệt theo cả khóa): xem `course-teacher-assignment.md`.

## 1. Mô hình dữ liệu (ánh xạ lên kiến trúc snapshot V56)

| Khái niệm trong spec | Hiện thực |
| --- | --- |
| **Bản live** (học viên học) | Snapshot V56: `course_published_versions` + `course_published_lessons`. |
| **Bản nháp** của bài | Chính các bảng soạn hiện có (`lessons`, `lesson_sections`, item, quiz…). Mỗi bài tự nhiên chỉ có **1 bản nháp, dùng chung** cho mọi người soạn (GV được giao + ADMIN). |
| Bài chưa từng duyệt (DRAFT) | Bài không có trong phiên bản live (`lessons.in_published_version = false`). |
| Duyệt / admin Lưu 1 bài | Tạo phiên bản live mới = **phiên bản trước + bài đó chụp lại từ nháp**; các bài khác giữ nguyên nội dung cũ. |

Trạng thái tách 2 chiều độc lập (như V55, nhưng theo bài):

- **Live**: bài có trong bản live hay không (`in_published_version`) và khóa có xuất bản hay không (`courses.is_published`, chỉ ADMIN).
- **Nháp** (`lessons.review_status`): `EDITING` | `PENDING_REVIEW`; kèm `lessons.has_unpublished_changes`, `lessons.draft_revision`, người / thời điểm sửa gần nhất (như V61 nhưng theo bài).
  Từ chối = quay về `EDITING` kèm lý do (lưu ở yêu cầu duyệt).
- **`draft_revision`** (thay cho ý "draft_base_version_no" ban đầu): tăng mỗi lần ghi nội dung bài và mỗi lần ADMIN lưu bài.
  Vì mỗi bài chỉ có 1 nháp dùng chung và bản live chỉ đổi khi chính nháp đó được duyệt / lưu, nháp không thể "lệch" bản live;
  thứ cần bắt là "người gửi đang thấy bản cũ" — so `draft_revision` là đủ.
- Các cột này **chỉ ghi bằng UPDATE có điều kiện** (`LessonRepository`), entity đọc-chỉ. Hai thao tác chồng nhau trên cùng bài được
  Postgres xếp hàng theo khóa dòng; thao tác sau đánh giá lại `WHERE` trên dữ liệu đã commit → chỉ 1 bên thắng.
- Thứ tự khóa dòng luôn là **khóa học → bài** (ghi nội dung, duyệt, ADMIN lưu) để không deadlock.

## 2. Luồng duyệt

- GV soạn tự do (lưu nháp không cần duyệt), **không** tự đưa lên live.
- GV **gửi duyệt theo bài**, chọn được nhiều bài một lần. Mỗi bài xử lý độc lập, trả kết quả riêng từng bài.
  Mỗi bài gửi kèm `version` của bài mà GV đang thấy — lệch ⇒ bài đó nhận xung đột (mục 4), không tự gửi lại.
- Mỗi bài tối đa 1 yêu cầu `PENDING`.
- Bài `PENDING_REVIEW`: **chỉ đọc với mọi người, kể cả ADMIN**. Chỉ đổi trạng thái bằng: ADMIN duyệt / từ chối (bắt buộc lý do), hoặc **bất kỳ GV đang được giao** khóa rút yêu cầu.
- GV bị gỡ khỏi khóa khi bài đang chờ: bài vẫn chờ ADMIN quyết định; GV đã gỡ không rút được nữa.
- Hai ADMIN cùng duyệt: người đầu thắng; người sau nhận thông báo kết quả của người trước (ai, duyệt hay từ chối, lúc nào).
- ADMIN xuất bản / chuyển nháp cả khóa như hiện tại (chỉ hiện lại bản live, không đụng nháp).

## 3. ADMIN sửa nội dung — lock

- ADMIN bấm **"Sửa"** trên 1 bài ⇒ server kiểm tra: bài đang `PENDING_REVIEW` ⇒ từ chối (phải duyệt / từ chối trước).
- Nếu bài có thay đổi chưa duyệt của GV: hiện cảnh báo "Bài có thay đổi chưa duyệt của <tên>" kèm phần khác biệt so với live; ADMIN chọn **sửa tiếp trên nháp đó** hoặc **bỏ nháp, sửa từ bản live**.
- ADMIN sửa vào **nháp chung** của bài (không sửa thẳng bản live). Bấm **"Lưu"** ⇒ đưa nháp lên live ngay (không cần duyệt) và nhả lock. Học viên không bao giờ thấy bản sửa dở.
- Lock lưu ở Redis, khóa theo bài: `{adminId, tên, từ lúc}`, TTL **5 phút**, FE gửi heartbeat 60 giây/lần khi editor mở.
  - Nhả lock: Lưu, hoặc "Hủy sửa" (phần đã ghi vẫn ở nháp, chưa lên live), hoặc hết TTL.
  - ADMIN khác: thấy banner, có nút "Tiếp quản" (xác nhận) ⇒ ADMIN cũ ở lần ghi kế tiếp nhận "phiên sửa đã hết / bị tiếp quản".
  - Redis lỗi ⇒ không cho bắt đầu sửa (không sửa khi không có lock).
- Khi lock đang được giữ: GV **xem được**, **không lưu, không gửi duyệt** (mã lỗi riêng, không phải xung đột version). Autosave của GV vẫn ghi.
  FE: banner "Admin <tên> đang sửa bài này", tắt nút Lưu / Gửi duyệt. Lock lấy / nhả / tiếp quản đẩy realtime qua `/topic/courses/{id}/content`.
- Sau khi ADMIN lưu, GV phải tải bản mới (version đổi) trước khi gửi duyệt — xung đột version tự đảm bảo.

## 4. Xung đột

- Hai người cùng sửa: optimistic locking bằng `version` (đã có: `VersionCheck` + `@Version`).
- Người thua: modal 1 nút **"Tải bản mới nhất"**, không hiện lỗi 409, không có lựa chọn ghi đè. Bấm ⇒ tải bản mới nhất, form sửa đang mở tự đóng, nội dung chưa lưu mở ở panel **"Bản chưa lưu của bạn"** để chép lại. **(Đã làm — FE `ConcurrentEditModal`.)**
- Đang gửi duyệt mà thua: tải lại xong phải bấm gửi duyệt lần nữa; không tự gửi.
- Gửi duyệt và sửa cùng lúc: chỉ một thao tác thành công (gửi duyệt mang `version`, cùng cơ chế).
- Modal chỉ dành cho lệch version (`9001`). Bị chặn vì lock / chờ duyệt có mã lỗi và thông báo riêng.
- Ràng buộc DB (giao trùng GV, 2 yêu cầu chờ cùng bài) bị vi phạm khi chạy song song ⇒ trả mã nghiệp vụ 409 tương ứng, không 500.

## 5. Autosave

- Bảng riêng theo **(người dùng, bài)**: nội dung form đang sửa dở (JSON), thời điểm.
- Không tăng version, không ai khác thấy (kể cả ADMIN), vẫn ghi được khi bài bị lock / chờ duyệt. GV đã bị gỡ khỏi khóa thì không ghi được.
- Chỉ xóa khi **lưu thành công** hoặc người dùng bấm **"Bỏ bản chưa lưu"** (không xóa lúc bấm "Tải bản mới nhất" — panel còn cần nội dung đó, F5 không mất).
- Giới hạn kích thước mỗi bản; job dọn bản quá 30 ngày (ShedLock).

## 6. Thông báo

| Loại | Gửi cho | Khi |
| --- | --- | --- |
| `LESSON_SUBMITTED` | Mọi ADMIN | GV gửi duyệt — **gộp theo lượt gửi** (1 thông báo cho nhiều bài) |
| `LESSON_WITHDRAWN` | Mọi ADMIN | Yêu cầu được rút (để hàng chờ cập nhật) |
| `LESSON_APPROVED` / `LESSON_REJECTED` | GV đã gửi | ADMIN quyết định (từ chối kèm lý do) |
| `DRAFT_STALE` | Người còn **bản tự lưu** (nháp riêng) của bài — bản nháp chung không thể lệch bản live | Bản live của bài đổi (duyệt / ADMIN lưu bài, lưu / duyệt cả khóa) |
| `LESSON_UPDATED` | Học viên **đã hoàn thành** bài | Chỉ khi ADMIN đánh dấu "thay đổi lớn" lúc duyệt / lưu |

Giao / gỡ GV vẫn dùng `COURSE_TEACHER_ASSIGNMENT`.

## 7. Phase triển khai

| Phase | Nội dung | Trạng thái |
| --- | --- | --- |
| 0 | Modal xung đột + panel "Bản chưa lưu" (FE) | ✅ |
| 1 | Sửa nhỏ trên luồng hiện tại, giữ nguyên sang thiết kế mới: GV được giao nào cũng rút được (GV đã gỡ thì không); người duyệt sau được báo kết quả người trước; vi phạm unique ⇒ 409 nghiệp vụ | ✅ |
| 2 | BE duyệt theo bài: migration V62 (trạng thái theo bài + `lesson_review_requests`), `CourseAccessGuard.requireLessonEditable`, snapshot theo bài (`CourseSnapshotService.publishLessons`), `LessonReviewService` / `LessonReviewController` (gửi nhiều bài kèm `draftRevision` / rút / duyệt / từ chối / ADMIN lưu 1 bài / trạng thái / lịch sử / hàng chờ), thông báo `LESSON_SUBMITTED/WITHDRAWN/APPROVED/REJECTED`, realtime. Luồng theo khóa vẫn chạy, hai luồng chặn nhau khi đang chờ (`1123` / `1247`) | ✅ |
| 3 | FE duyệt theo bài: `LessonReviewPanel` (tab Duyệt — trạng thái từng bài, chọn nhiều bài gửi duyệt kèm `draftRevision` đang thấy, lịch sử), `LessonReviewActions` (header trang soạn bài: gửi / rút; ADMIN duyệt / từ chối / lưu bài), khóa chỉ đọc theo bài `LESSON_PENDING_REVIEW` (cả ADMIN), hàng chờ ADMIN tab "Theo bài" / "Cả khóa", thông báo `LESSON_*`. Bài bị xung đột khi gửi → modal "Tải bản mới nhất". Giáo viên không còn nút gửi duyệt cả khóa (chỉ xử lý yêu cầu cả khóa cũ) | ✅ |
| 4 | Phiên sửa bài của ADMIN: `LessonEditLockService` (Redis, TTL 5 phút, script Lua so khớp người giữ), API `POST/PUT/DELETE /lessons/{id}/edit-lock` (bắt đầu / tiếp quản, gia hạn, hủy), `requireLessonEditable` kiểm khóa (ADMIN phải giữ phiên; người khác bị chặn `1250`/`1252`), gửi duyệt / ADMIN lưu kiểm khóa, "Lưu bài" nhả khóa sau commit, realtime `Scope.LOCK`. FE: nút Sửa bài / Tiếp quản / Hủy sửa, cảnh báo nháp chưa duyệt kèm tóm tắt thay đổi, banner "Quản trị viên X đang sửa", heartbeat 60 giây, tự nhả khi rời trang. | ✅ |
| 5 | Autosave: V63 `lesson_autosaves` (người + bài + form_key, JSONB, upsert không đụng version / khóa bài), `LessonAutosaveService` (chỉ ADMIN / GV đang được giao, ≤ 64 KB, ≤ 50 bản / bài / người, job dọn 30 ngày + ShedLock), API `GET/PUT/DELETE /lessons/{id}/autosaves[/{formKey}]`. FE: `useFormAutosave` (lưu 1,5 giây sau khi gõ, đóng form / rời trang thì lưu ngay, lưu thành công thì xóa, xóa luôn chạy sau lệnh lưu đang gửi), `AutosaveBar` (Khôi phục / Bỏ; bản ghi đã bị người khác sửa → "Mở để chép lại" ở panel, không điền đè), gắn vào form từ vựng, ngữ pháp, chữ Hán, câu hỏi quiz, section, thông tin bài | ✅ |
| 6 | `DRAFT_STALE` → người còn bản tự lưu của bài khi bản live đổi (duyệt / lưu bài, lưu / duyệt cả khóa); `LESSON_UPDATED` → học viên đã hoàn thành bài khi ADMIN tick "thay đổi lớn" lúc duyệt / lưu bài và khóa đang xuất bản (`LessonLiveChangeNotificationListener`). V64 lưu `major_change` trên yêu cầu duyệt; FE: ô tick ở hộp duyệt / "Lưu bài", nhãn "Thay đổi lớn" trong lịch sử và hàng chờ | ✅ |
| 7 | Viết lại `course-teacher-assignment.md` (hiện trạng + ma trận xung đột đầy đủ); sửa các mục phân quyền / xuất bản lỗi thời trong `CURRICULUM_MODULE.md`, `course-lesson-module.md` | ✅ |
| + | **Bỏ nháp, sửa từ bản live**: `LessonDraftRestoreRepository` (xóa mềm nháp → khôi phục theo id từ snapshot, tạo lại dòng đã xóa hẳn), `LessonReviewService.discardDraft` (ADMIN đang giữ phiên sửa; bài chưa từng duyệt → `1257`; tự kiểm `draftMatchesLive`), API `POST /lessons/{id}/review/discard-draft`, thông báo `LESSON_DRAFT_DISCARDED`; FE: lựa chọn trong hộp cảnh báo khi bấm "Sửa bài" + xác nhận lần 2 | ✅ |

## 8. Câu hỏi còn mở (đang theo mặc định này cho tới khi chốt khác)

- **"Bỏ nháp"** đã làm (khôi phục theo id, giữ id ổn định). Phần chưa duyệt bị bỏ **không có bản sao lưu** — chỉ bản tự lưu
  của form đang gõ dở còn lại.
- **Sửa tiêu đề / xóa bài** ở trang khóa: ADMIN không cần mở phiên sửa; vẫn bị chặn khi ADMIN khác đang sửa bài đó.
- **Redis lỗi**: ADMIN không bắt đầu / tiếp tục sửa được; giáo viên vẫn ghi được (không xác định được khóa).

- **Thao tác cấp khóa** (thêm / xóa / sắp xếp bài): mặc định thêm bài = bài mới ở trạng thái DRAFT, cần duyệt bài đó; xóa bài đã live và đổi thứ tự bài đi cùng lượt "ADMIN Lưu mục lục" — chưa qua duyệt của GV.
- **Đề thi gắn khóa**: duyệt / lưu **một bài không** chụp lại đề thi (tránh đưa phần đề chưa duyệt lên live); đề thi chỉ lên live
  khi ADMIN lưu / duyệt cả khóa (luồng cũ). Chưa có luồng duyệt riêng cho đề.
- **Thêm / xóa / sắp xếp bài**: sửa / xóa bài đang chờ duyệt bị chặn; bài mới chỉ vào bản live khi được duyệt; thứ tự bài và bài đã
  xóa ở nháp chỉ đổi ở bản live khi ADMIN lưu cả khóa.
- Nhận xét duyệt theo bài (V57) gắn với yêu cầu duyệt theo khóa — Phase 2 chuyển sang gắn với yêu cầu duyệt theo bài.
