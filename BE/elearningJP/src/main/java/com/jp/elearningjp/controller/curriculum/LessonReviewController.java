package com.jp.elearningjp.controller.curriculum;

import com.jp.elearningjp.dto.request.curriculum.CourseReviewRejectRequest;
import com.jp.elearningjp.dto.request.curriculum.LessonReviewApproveRequest;
import com.jp.elearningjp.dto.request.curriculum.LessonReviewSubmitRequest;
import com.jp.elearningjp.dto.response.curriculum.LessonReviewRequestResponse;
import com.jp.elearningjp.dto.response.curriculum.LessonReviewStateResponse;
import com.jp.elearningjp.dto.response.curriculum.LessonReviewSubmitResponse;
import com.jp.elearningjp.service.curriculum.LessonReviewService;
import com.jp.elearningjp.service.curriculum.snapshot.LessonSnapshot;
import com.jp.elearningjp.shared.constants.ApiPaths;
import com.jp.elearningjp.shared.enums.ReviewRequestStatus;
import com.jp.elearningjp.shared.response.ApiResponse;
import com.jp.elearningjp.shared.response.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Lesson Review", description = "Duyệt xuất bản theo bài: giáo viên gửi nhiều bài, quản trị viên duyệt / từ chối / lưu từng bài")
@RestController
@RequestMapping(ApiPaths.API_V1)
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@SecurityRequirement(name = "bearerAuth")
public class LessonReviewController {

        LessonReviewService lessonReviewService;

        @Operation(summary = "[Admin/Teacher] Trạng thái duyệt từng bài của khóa", description = "Đang soạn / chờ duyệt, có thay đổi chưa duyệt, draftRevision (gửi kèm khi gửi duyệt), "
                        + "bài đã có trong bản học viên đang học chưa. Quản trị viên hoặc giáo viên được giao")
        @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
        @GetMapping(ApiPaths.Course.BASE + ApiPaths.Course.LESSON_REVIEW_STATES)
        public ApiResponse<List<LessonReviewStateResponse>> getStates(@PathVariable Long courseId) {
                return ApiResponse.<List<LessonReviewStateResponse>>builder()
                                .success(true)
                                .data(lessonReviewService.getStates(courseId))
                                .build();
        }

        @Operation(summary = "[Admin/Teacher] Lịch sử gửi duyệt theo bài của khóa", description = "Mới nhất trước, có phân trang (page bắt đầu từ 1, size tối đa 100). "
                        + "Quản trị viên hoặc giáo viên được giao")
        @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
        @GetMapping(ApiPaths.Course.BASE + ApiPaths.Course.LESSON_REVIEWS)
        public ApiResponse<PageResponse<LessonReviewRequestResponse>> getHistory(
                        @PathVariable Long courseId,
                        @RequestParam(defaultValue = "1") int page,
                        @RequestParam(defaultValue = "5") int size) {
                return ApiResponse.<PageResponse<LessonReviewRequestResponse>>builder()
                                .success(true)
                                .data(lessonReviewService.getHistory(courseId, page, size))
                                .build();
        }

        @Operation(summary = "[Teacher] Gửi một hoặc nhiều bài lên duyệt", description = "Chỉ giáo viên được giao. Mỗi bài kèm draftRevision đang thấy và được xử lý độc lập: "
                        + "bài bị người khác sửa sau đó nhận mã 9001 (tải bản mới nhất rồi gửi lại bài đó), bài đang chờ / "
                        + "không có thay đổi nhận mã tương ứng; các bài khác vẫn được gửi. Bài đã gửi chỉ đọc với mọi người "
                        + "tới khi quản trị viên quyết định hoặc giáo viên rút. Quản trị viên nhận 1 thông báo cho cả lượt")
        @PreAuthorize("hasRole('TEACHER')")
        @PostMapping(ApiPaths.Course.BASE + ApiPaths.Course.LESSON_REVIEWS)
        public ApiResponse<LessonReviewSubmitResponse> submit(
                        @PathVariable Long courseId,
                        @Valid @RequestBody LessonReviewSubmitRequest request) {
                LessonReviewSubmitResponse result = lessonReviewService.submit(courseId, request.getLessons(),
                                request.getNote());
                return ApiResponse.<LessonReviewSubmitResponse>builder()
                                .success(true)
                                .message(result.getFailedCount() == 0
                                                ? "Đã gửi " + result.getSubmittedCount()
                                                                + " bài lên quản trị viên duyệt"
                                                : "Đã gửi " + result.getSubmittedCount() + "/"
                                                                + (result.getSubmittedCount() + result.getFailedCount())
                                                                + " bài — xem lý do từng bài chưa gửi được")
                                .data(result)
                                .build();
        }

        @Operation(summary = "[Teacher] Rút yêu cầu duyệt đang chờ của bài", description = "Giáo viên đang được giao khóa (không chỉ người gửi). Giáo viên đã bị gỡ không rút được. "
                        + "Bài quay về đang soạn")
        @PreAuthorize("hasRole('TEACHER')")
        @PostMapping(ApiPaths.Lesson.BASE + ApiPaths.Lesson.REVIEW_WITHDRAW)
        public ApiResponse<LessonReviewRequestResponse> withdraw(@PathVariable Long id) {
                return ApiResponse.<LessonReviewRequestResponse>builder()
                                .success(true)
                                .message("Đã rút yêu cầu duyệt bài học")
                                .data(lessonReviewService.withdraw(id))
                                .build();
        }

        @Operation(summary = "[Admin] Lưu bài lên bản học viên đang học", description = "Đưa nháp ADMIN của bài lên bản live ngay, không qua duyệt (không bật xuất bản khóa). Cần giữ khóa "
                        + "sửa khóa học phía ADMIN (POST /courses/{id}/edit-lock). Giáo viên đang có yêu cầu chờ duyệt → 1263; "
                        + "nháp ADMIN đã cũ → 1261 (cập nhật bản mới trước). Nháp giáo viên chưa sửa gì tự theo bản live mới. "
                        + "majorChange=true: học viên đã hoàn thành bài nhận thông báo (khi khóa đang xuất bản)")
        @PreAuthorize("hasRole('ADMIN')")
        @PostMapping(ApiPaths.Lesson.BASE + ApiPaths.Lesson.SAVE_TO_LIVE)
        public ApiResponse<Void> saveToLive(
                        @PathVariable Long id,
                        @RequestParam(defaultValue = "false") boolean majorChange) {
                lessonReviewService.saveLesson(id, majorChange);
                return ApiResponse.<Void>builder()
                                .success(true)
                                .message("Đã lưu bài học lên bản học viên đang học")
                                .build();
        }

        @Operation(summary = "[Admin/Teacher] Bỏ nháp của phía mình", description = "Nháp của phía người gọi (giáo viên / ADMIN) dựng lại từ bản học viên đang học — thay đổi chưa xuất "
                        + "bản của phía mình bị bỏ, không khôi phục được. Cần giữ khóa sửa khóa học phía mình; nháp đã gửi duyệt "
                        + "→ 1240. Bài chưa từng duyệt → nháp trống")
        @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
        @PostMapping(ApiPaths.Lesson.BASE + ApiPaths.Lesson.DISCARD_DRAFT)
        public ApiResponse<Void> discardDraft(@PathVariable Long id) {
                lessonReviewService.restartDraft(id, false);
                return ApiResponse.<Void>builder()
                                .success(true)
                                .message("Đã bỏ nháp — bài quay về bản học viên đang học")
                                .build();
        }

        @Operation(summary = "[Admin/Teacher] Cập nhật bản mới", description = "Nháp của phía người gọi đã cũ (bản học viên đang học vừa đổi): nháp hiện tại thành nháp cũ chỉ đọc "
                        + "(GET .../draft/archived), nháp mới dựng từ bản live để tự chép lại phần còn thiếu. Cần giữ khóa sửa "
                        + "khóa học phía mình; nháp đã gửi duyệt → 1240")
        @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
        @PostMapping(ApiPaths.Lesson.BASE + ApiPaths.Lesson.DRAFT_REBASE)
        public ApiResponse<Void> rebaseDraft(@PathVariable Long id) {
                lessonReviewService.restartDraft(id, true);
                return ApiResponse.<Void>builder()
                                .success(true)
                                .message("Đã tạo nháp mới từ bản học viên đang học — nháp cũ được giữ để chép lại")
                                .build();
        }

        @Operation(summary = "[Admin/Teacher] Xem nháp cũ chỉ đọc của phía mình", description = "Nội dung nháp được giữ lại ở lần cập nhật bản mới gần nhất (cùng dạng bản chụp bài học). Không có → 1264")
        @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
        @GetMapping(ApiPaths.Lesson.BASE + ApiPaths.Lesson.DRAFT_ARCHIVED)
        public ApiResponse<LessonSnapshot> getArchivedDraft(@PathVariable Long id) {
                return ApiResponse.<LessonSnapshot>builder()
                                .success(true)
                                .data(lessonReviewService.getArchivedDraft(id))
                                .build();
        }

        @Operation(summary = "[Admin] Hàng chờ duyệt theo bài", description = "Lọc theo trạng thái (mặc định PENDING) và từ khóa (tên bài, tên khóa, tên người gửi), cũ nhất "
                        + "trước. page bắt đầu từ 1")
        @PreAuthorize("hasRole('ADMIN')")
        @GetMapping(ApiPaths.LessonReview.BASE)
        public ApiResponse<PageResponse<LessonReviewRequestResponse>> getQueue(
                        @RequestParam(required = false) ReviewRequestStatus status,
                        @RequestParam(required = false) String keyword,
                        @RequestParam(defaultValue = "1") int page,
                        @RequestParam(defaultValue = "20") int size) {
                return ApiResponse.<PageResponse<LessonReviewRequestResponse>>builder()
                                .success(true)
                                .data(lessonReviewService.getQueue(status, keyword, page, size))
                                .build();
        }

        @Operation(summary = "[Admin] Xem bản giáo viên đã gửi duyệt", description = "Bản chụp nội dung bài lúc giáo viên gửi (cùng dạng bản chụp bài học) — duyệt sẽ đưa đúng bản này "
                        + "lên bản học viên. Quản trị viên không đọc được nháp đang soạn của giáo viên")
        @PreAuthorize("hasRole('ADMIN')")
        @GetMapping(ApiPaths.LessonReview.BASE + ApiPaths.LessonReview.CONTENT)
        public ApiResponse<LessonSnapshot> getRequestContent(@PathVariable Long requestId) {
                return ApiResponse.<LessonSnapshot>builder()
                                .success(true)
                                .data(lessonReviewService.getRequestContent(requestId))
                                .build();
        }

        @Operation(summary = "[Admin] Duyệt bài", description = "Bài lên bản học viên đang học (các bài khác giữ nguyên). Hai quản trị viên cùng duyệt: "
                        + "người sau nhận kết quả của người trước. Giáo viên gửi nhận thông báo; majorChange=true thì học viên "
                        + "đã hoàn thành bài cũng được báo (khi khóa đang xuất bản)")
        @PreAuthorize("hasRole('ADMIN')")
        @PostMapping(ApiPaths.LessonReview.BASE + ApiPaths.LessonReview.APPROVE)
        public ApiResponse<LessonReviewRequestResponse> approve(
                        @PathVariable Long requestId,
                        @Valid @RequestBody(required = false) LessonReviewApproveRequest request) {
                return ApiResponse.<LessonReviewRequestResponse>builder()
                                .success(true)
                                .message("Đã duyệt bài học")
                                .data(lessonReviewService.approve(requestId, request != null ? request.getNote() : null,
                                                request != null && request.isMajorChange()))
                                .build();
        }

        @Operation(summary = "[Admin] Từ chối bài", description = "Bắt buộc lý do. Bài quay về đang soạn, bản học viên đang học giữ nguyên. Giáo viên gửi nhận thông báo")
        @PreAuthorize("hasRole('ADMIN')")
        @PostMapping(ApiPaths.LessonReview.BASE + ApiPaths.LessonReview.REJECT)
        public ApiResponse<LessonReviewRequestResponse> reject(
                        @PathVariable Long requestId,
                        @Valid @RequestBody CourseReviewRejectRequest request) {
                return ApiResponse.<LessonReviewRequestResponse>builder()
                                .success(true)
                                .message("Đã từ chối bài học")
                                .data(lessonReviewService.reject(requestId, request.getReason()))
                                .build();
        }
}
