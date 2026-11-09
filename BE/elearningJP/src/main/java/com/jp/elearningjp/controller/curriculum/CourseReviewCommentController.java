package com.jp.elearningjp.controller.curriculum;

import com.jp.elearningjp.dto.request.curriculum.CourseReviewCommentRequest;
import com.jp.elearningjp.dto.request.curriculum.CourseReviewCommentResolveRequest;
import com.jp.elearningjp.dto.response.curriculum.CourseChangesResponse;
import com.jp.elearningjp.dto.response.curriculum.CourseReviewCommentResponse;
import com.jp.elearningjp.service.curriculum.CourseReviewCommentService;
import com.jp.elearningjp.service.curriculum.snapshot.CourseChangesService;
import com.jp.elearningjp.shared.constants.ApiPaths;
import com.jp.elearningjp.shared.response.ApiResponse;
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

@Tag(name = "Course Review Detail", description = "Nhận xét duyệt theo bài và danh sách thay đổi chờ duyệt")
@RestController
@RequestMapping(ApiPaths.API_V1)
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@SecurityRequirement(name = "bearerAuth")
public class CourseReviewCommentController {

    CourseReviewCommentService commentService;
    CourseChangesService changesService;

    @Operation(summary = "[Admin/Teacher] Thay đổi chờ duyệt",
            description = "So bản đang soạn với bản học viên đang học: bài / mục / câu hỏi thêm, bỏ, sửa và đề thi gắn khóa")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @GetMapping(ApiPaths.Course.BASE + ApiPaths.Course.PENDING_CHANGES)
    public ApiResponse<CourseChangesResponse> getChanges(@PathVariable Long courseId) {
        return ApiResponse.<CourseChangesResponse>builder()
                .success(true)
                .data(changesService.getChanges(courseId))
                .build();
    }

    @Operation(summary = "[Admin/Teacher] Nhận xét của lượt duyệt gần nhất",
            description = "Giáo viên xem để sửa theo; rỗng nếu khóa chưa từng gửi duyệt")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @GetMapping(ApiPaths.Course.BASE + ApiPaths.Course.REVIEW_COMMENTS)
    public ApiResponse<List<CourseReviewCommentResponse>> getLatestComments(@PathVariable Long courseId) {
        return ApiResponse.<List<CourseReviewCommentResponse>>builder()
                .success(true)
                .data(commentService.getLatestForCourse(courseId))
                .build();
    }

    @Operation(summary = "[Admin/Teacher] Nhận xét của 1 lượt duyệt")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @GetMapping(ApiPaths.CourseReview.BASE + ApiPaths.CourseReview.COMMENTS)
    public ApiResponse<List<CourseReviewCommentResponse>> getComments(@PathVariable Long requestId) {
        return ApiResponse.<List<CourseReviewCommentResponse>>builder()
                .success(true)
                .data(commentService.getByRequest(requestId))
                .build();
    }

    @Operation(summary = "[Admin] Thêm nhận xét duyệt",
            description = "Chỉ khi lượt duyệt đang chờ. Gắn với cả khóa (bỏ trống lessonId), 1 bài, hoặc 1 mục của bài")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(ApiPaths.CourseReview.BASE + ApiPaths.CourseReview.COMMENTS)
    public ApiResponse<CourseReviewCommentResponse> addComment(
            @PathVariable Long requestId,
            @Valid @RequestBody CourseReviewCommentRequest request
    ) {
        return ApiResponse.<CourseReviewCommentResponse>builder()
                .success(true)
                .message("Đã thêm nhận xét")
                .data(commentService.add(requestId, request))
                .build();
    }

    @Operation(summary = "[Admin] Xóa nhận xét duyệt", description = "Nhận xét của chính mình, khi lượt duyệt còn đang chờ")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping(ApiPaths.CourseReviewComment.BASE + ApiPaths.CourseReviewComment.BY_ID)
    public ApiResponse<Void> deleteComment(@PathVariable Long commentId) {
        commentService.delete(commentId);
        return ApiResponse.<Void>builder()
                .success(true)
                .message("Đã xóa nhận xét")
                .build();
    }

    @Operation(summary = "[Teacher] Đánh dấu nhận xét đã sửa / chưa sửa", description = "Giáo viên được giao khóa")
    @PreAuthorize("hasRole('TEACHER')")
    @PatchMapping(ApiPaths.CourseReviewComment.BASE + ApiPaths.CourseReviewComment.RESOLVE)
    public ApiResponse<CourseReviewCommentResponse> resolveComment(
            @PathVariable Long commentId,
            @Valid @RequestBody CourseReviewCommentResolveRequest request
    ) {
        return ApiResponse.<CourseReviewCommentResponse>builder()
                .success(true)
                .message(Boolean.TRUE.equals(request.getResolved()) ? "Đã đánh dấu đã sửa" : "Đã đánh dấu chưa sửa")
                .data(commentService.resolve(commentId, request.getResolved()))
                .build();
    }
}
