package com.jp.elearningjp.controller.curriculum;

import com.jp.elearningjp.dto.request.curriculum.CourseReviewRejectRequest;
import com.jp.elearningjp.dto.request.curriculum.CourseReviewSubmitRequest;
import com.jp.elearningjp.dto.response.curriculum.CourseReviewRequestResponse;
import com.jp.elearningjp.service.curriculum.CourseReviewService;
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

@Tag(name = "Course Review", description = "Luồng duyệt xuất bản khóa học: giáo viên gửi duyệt, quản trị viên duyệt / từ chối")
@RestController
@RequestMapping(ApiPaths.API_V1)
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@SecurityRequirement(name = "bearerAuth")
public class CourseReviewController {

    CourseReviewService courseReviewService;

    @Operation(summary = "[Admin/Teacher] Lịch sử gửi duyệt của khóa học",
            description = "Mới nhất trước. Quản trị viên hoặc giáo viên được giao khóa")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @GetMapping(ApiPaths.Course.BASE + ApiPaths.Course.REVIEW_REQUESTS)
    public ApiResponse<List<CourseReviewRequestResponse>> getHistory(@PathVariable Long courseId) {
        return ApiResponse.<List<CourseReviewRequestResponse>>builder()
                .success(true)
                .data(courseReviewService.getHistory(courseId))
                .build();
    }

    @Operation(summary = "[Teacher] Gửi khóa học lên duyệt",
            description = "Chỉ giáo viên được giao. Khóa chuyển sang chờ duyệt, mọi giáo viên tạm khóa sửa nội dung. "
                    + "Cần có thay đổi chưa duyệt và ít nhất 1 bài học. Quản trị viên nhận thông báo")
    @PreAuthorize("hasRole('TEACHER')")
    @PostMapping(ApiPaths.Course.BASE + ApiPaths.Course.REVIEW_REQUESTS)
    public ApiResponse<CourseReviewRequestResponse> submit(
            @PathVariable Long courseId,
            @Valid @RequestBody(required = false) CourseReviewSubmitRequest request
    ) {
        return ApiResponse.<CourseReviewRequestResponse>builder()
                .success(true)
                .message("Đã gửi khóa học lên quản trị viên duyệt")
                .data(courseReviewService.submit(courseId, request != null ? request.getNote() : null))
                .build();
    }

    @Operation(summary = "[Teacher] Rút yêu cầu duyệt đang chờ",
            description = "Chỉ người gửi. Giáo viên được sửa tiếp nội dung (quản trị viên muốn trả lại thì từ chối kèm lý do)")
    @PreAuthorize("hasRole('TEACHER')")
    @PostMapping(ApiPaths.Course.BASE + ApiPaths.Course.REVIEW_WITHDRAW)
    public ApiResponse<CourseReviewRequestResponse> withdraw(@PathVariable Long courseId) {
        return ApiResponse.<CourseReviewRequestResponse>builder()
                .success(true)
                .message("Đã rút yêu cầu duyệt")
                .data(courseReviewService.withdraw(courseId))
                .build();
    }

    @Operation(summary = "[Admin] Hàng chờ duyệt khóa học",
            description = "Mặc định các yêu cầu đang chờ (PENDING), cũ nhất trước. page bắt đầu từ 1")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping(ApiPaths.CourseReview.BASE)
    public ApiResponse<PageResponse<CourseReviewRequestResponse>> getQueue(
            @RequestParam(required = false) ReviewRequestStatus status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.<PageResponse<CourseReviewRequestResponse>>builder()
                .success(true)
                .data(courseReviewService.getQueue(status, page, size))
                .build();
    }

    @Operation(summary = "[Admin] Duyệt yêu cầu — xuất bản khóa học",
            description = "Khóa được xuất bản với nội dung hiện tại. Giáo viên được giao (và học viên nếu khóa vừa mở) nhận thông báo")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(ApiPaths.CourseReview.BASE + ApiPaths.CourseReview.APPROVE)
    public ApiResponse<CourseReviewRequestResponse> approve(
            @PathVariable Long requestId,
            @Valid @RequestBody(required = false) CourseReviewSubmitRequest request
    ) {
        return ApiResponse.<CourseReviewRequestResponse>builder()
                .success(true)
                .message("Đã duyệt và xuất bản khóa học")
                .data(courseReviewService.approve(requestId, request != null ? request.getNote() : null))
                .build();
    }

    @Operation(summary = "[Admin] Từ chối yêu cầu duyệt",
            description = "Bắt buộc lý do. Bản đang chạy giữ nguyên, giáo viên sửa tiếp và nhận thông báo kèm lý do")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(ApiPaths.CourseReview.BASE + ApiPaths.CourseReview.REJECT)
    public ApiResponse<CourseReviewRequestResponse> reject(
            @PathVariable Long requestId,
            @Valid @RequestBody CourseReviewRejectRequest request
    ) {
        return ApiResponse.<CourseReviewRequestResponse>builder()
                .success(true)
                .message("Đã từ chối yêu cầu duyệt")
                .data(courseReviewService.reject(requestId, request.getReason()))
                .build();
    }
}
