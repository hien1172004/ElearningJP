# 🤖 Prompt Mẫu cho AI Agent — Làm UI cho Module BE Mới

> **Mục đích**: File này là **prompt mẫu** để dán vào Cursor khi bắt đầu làm UI cho 1 module backend mới của ElearningJP (ví dụ: Quiz, SRS review history, comment, certificate…).
>
> **Cách dùng**:
> 1. Mở file này, **điền các chỗ `[...]`** cho phù hợp với module anh đang làm.
> 2. Paste nguyên si vào chat Cursor Agent (composer mới hoặc Cmd+L).
> 3. Agent sẽ tự đọc codebase theo các gợi ý, tóm tắt lại + hỏi các chỗ mơ hồ, **chưa code ngay**.
> 4. Sau khi anh duyệt phần "tóm tắt + chỗ mơ hồ", agent mới đề xuất cấu trúc file để anh duyệt tiếp.
> 5. Chỉ khi anh `viết đi` / `ok` thì agent mới code từng màn hình một.

---

## Prompt mẫu (copy từ đây trở xuống)

```
Tôi vừa hoàn thành backend module [TÊN_MODULE], giờ cần làm UI cho nó.

## Bối cảnh
- FE stack: [đã cố định — React 19 + TypeScript + Vite]
- UI lib: [đã cố định — Ant Design v6 + sonner + tailwind-merge + clsx]
- State/fetch: [đã cố định — Zustand + @tanstack/react-query + axios]
- Form/validate: [đã cố định — react-hook-form + zod (qua @hookform/resolvers)]
- Routing: [đã cố định — react-router-dom v7]
- BE: [Spring Boot, base URL `/api/v1`, JWT Bearer qua header `Authorization`]
- Tài liệu API: đọc file `[ĐƯỜNG_DẪN_FILE_.HTTP_HOẶC_MD]` (nếu có nhiều file
  thì liệt kê và đánh dấu file chính)
- Service/Type/Hook skeleton cho module này nếu đã có sẵn ở FE
  (kiểm tra `services/`, `types/`, `hooks/`) → tiết kiệm rất nhiều công code

## Luồng nghiệp vụ (điền theo BE)
1. Role Teacher (Own)/Admin: [quản lý crd]
3. Ràng buộc nghiệp vụ: [giới hạn, điều kiện, side-effect...]
4. Side-effect tự động (nếu có): [VD: tự COMPLETED progress, sync SRS, gửi notification...]

## Endpoint BE sẽ gọi (điền theo file .http / Controller thực tế)
- [METHOD] [/path/...]               # mô tả ngắn
- [METHOD] [/path/.../{id}/...]      # mô tả ngắn
- ...

## Màn hình cần làm (đề xuất ban đầu — agent sẽ tinh chỉnh)
- [Danh sách X]                # ai truy cập? URL?
- [Chi tiết / Làm X]           # ai truy cập? URL?
- [Kết quả / Form X]           # ai truy cập? URL?
- [Quản lý X cho teacher]      # ai truy cập? URL?

## Yêu cầu kỹ thuật (giữ nguyên convention repo)
- Service layer: export object dùng `axiosClient`, return `ApiResponse<T>`,
  lấy `.data` ở hook (xem `course.service.ts` cuối file làm mẫu)
- Types: đặt trong `types/<module>.types.ts` theo camelCase khớp response BE
- Hooks: `use<X>Query` cho read, `use<X>Mutation` cho write; `queryKeys`
  thêm vào `lib/queryKeys.ts`
- Loading/Error/Empty: dùng `LoadingState`, `ErrorBoundary`, `EmptyState`
  (đã có trong `components/common/`)
- Mã lỗi: xử lý 401/403/404/409/400 qua axios interceptor + `sonner` toast
- i18n/text: tiếng Việt, chữ Hán + romaji cho nội dung Nhật (giống phần còn lại)

## Cách làm việc (BẮT BUỘC, không bỏ bước nào)
1. Đọc tài liệu API + code FE hiện có (nếu có skeleton service/type/hook)
2. Tóm tắt lại luồng bạn hiểu + liệt kê endpoint + chỉ ra **chỗ nào mơ hồ**
   để tôi xác nhận. **CHƯA code vội.**
3. Khi tôi xác nhận xong → đề xuất cấu trúc file/màn hình (component tree
   + props + state), đợi tôi duyệt.
4. Chỉ khi tôi nói `viết đi` / `ok` thì mới code từng màn hình một,
   không làm hết một lượt.
5. Mỗi màn hình xong → dừng, cho tôi xem, rồi mới sang màn hình tiếp.
```

---

## Ghi chú quan trọng khi dùng

### 1. Pattern FE đã có sẵn, tận dụng tối đa
Trước khi code, luôn kiểm tra các thư mục sau — nhiều khi session trước đã có sẵn:
```
FE/src/types/<module>.types.ts        ← types & DTOs
FE/src/services/<module>.service.ts   ← có thể nằm chung trong course.service.ts
FE/src/hooks/use<X>.ts                ← hooks gọi service
FE/src/components/<module>/            ← UI components
FE/src/pages/<module>/                ← pages theo role
```
Nếu đã có → **không viết lại**, chỉ verify và fix.

### 2. File tham chiếu (luôn mở sẵn khi làm FE)
- `FE/src/services/course.service.ts` — đầy đủ các service pattern (axiosClient, ApiResponse, endpoint style)
- `FE/src/types/course.types.ts` — pattern đặt type (interface, request/response, nested types)
- `FE/src/lib/queryKeys.ts` — nơi duy nhất khai báo query key
- `FE/src/hooks/useSectionMutations.ts` — pattern mutate + invalidate + cache patch + error handling
- `FE/src/components/common/` — các component dùng chung (LoadingState, EmptyState, ErrorBoundary, ConfirmModal, Button, Input…)
- `FE/src/components/course/section-editor/SectionEditorModal.tsx` — pattern Modal + nested Modal

### 3. File tham chiếu BE
- `BE/elearningJP/http-tests/*.http` — file test API có comment giải thích rất rõ (data mẫu, response shape, lỗi hay gặp)
- `BE/elearningJP/docs/*.md` — tài liệu kiến trúc tổng quan
- `BE/elearningJP/src/main/java/com/jp/elearningjp/service/<module>/` — source service để biết rõ nghiệp vụ

### 4. Mã lỗi BE hay gặp (memorize để fix nhanh)
| Status | Ý nghĩa thường gặp trong ElearningJP |
|--------|--------------------------------------|
| `400` | Validation lỗi (thiếu field, định dạng sai) |
| `401` | Token hết hạn / thiếu — interceptor tự logout |
| `403` | Không đủ quyền (chưa enroll / không phải owner course) |
| `404` | Resource không tồn tại hoặc đã xoá mềm |
| `409` | Conflict (trùng orderIndex, đã tồn tại…) |

### 5. Quy ước chấm điểm attempt (áp dụng cho Quiz, có thể áp dụng cho module khác)
- Auto-create Quiz/Course khi chưa có
- Passing score thường là 80%
- Mỗi câu trả lời ghi nhận `responseTimeMs`
- Side-effect: cập nhật `bestScore` + `status` sau khi nộp

### 6. ESLint & format
- Dự án dùng **oxlint** (chạy `npm run lint`)
- Format: prettier mặc định (em không tự ý thêm config)
- Không tạo file `.md` mới nếu không được yêu cầu

---

## Ví dụ đã dùng thực tế (Module Quiz)

Lần lượt các bước đã làm cho module Quiz (xem transcript trước nếu cần):
1. **Đọc**: `10-lesson-quiz-flow.http`, `QuizService.java`, `course.types.ts`, `SectionEditorModal.tsx`, `useQuizItemMutations.ts`, `QuizItemsEditor.tsx` (đã có sẵn session trước)
2. **Tóm tắt + hỏi**: endpoint V20 vs V14, layout teacher Modal/page/tab, attempt history, timeLimit/maxAttempts, UI ghép cặp
3. **Cấu trúc file**: thống nhất pattern với section-editor (giữ nguyên), không tạo file mới vì đã có
4. **Chưa code** vì user yêu cầu viết prompt thôi

## Bài học rút ra
- **Đừng ngại hỏi lại** các chỗ mơ hồ — sửa sai code còn tốn công hơn hỏi 1 câu
- **Đừng giả định** về edge case (VD: FE nghĩ BE có `/quiz/attempts`, BE không có → mất công)
- **Luôn check trước** xem file đã tồn tại chưa (`git status` nhiều khi cho thấy các file `??` chưa commit nhưng đã viết)
- **Repo có pattern rất nhất quán**: section / item / modal — tận dụng để code nhanh và đồng nhất
