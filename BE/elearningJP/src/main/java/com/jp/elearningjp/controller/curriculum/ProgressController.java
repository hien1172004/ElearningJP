package com.jp.elearningjp.controller.curriculum;

import com.jp.elearningjp.dto.request.curriculum.LessonCompleteRequest;
import com.jp.elearningjp.dto.response.curriculum.CourseProgressResponse;
import com.jp.elearningjp.dto.response.curriculum.ProgressResponse;
import com.jp.elearningjp.service.curriculum.ProgressService;
import com.jp.elearningjp.shared.constants.ApiPaths;
import com.jp.elearningjp.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Progress", description = "Theo dõi tiến độ học tập của user")
@RestController
@RequestMapping(ApiPaths.API_V1 + ApiPaths.Progress.BASE)
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProgressController {

    ProgressService progressService;

    // ============================================================
    // START
    // ============================================================

    @Operation(summary = "Bắt đầu 1 lesson (chuyển sang IN_PROGRESS)",
            description = "Idempotent. Cần enrollment ACTIVE.")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping(ApiPaths.Progress.START)
    public ResponseEntity<ApiResponse<ProgressResponse>> startLesson(
            @PathVariable Long lessonId) {
        ProgressResponse started = progressService.startLesson(lessonId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<ProgressResponse>builder()
                        .success(true)
                        .code(201)
                        .message("Lesson started")
                        .data(started)
                        .build());
    }

    // ============================================================
    // COMPLETE
    // ============================================================

    @Operation(summary = "Hoàn thành 1 lesson",
            description = "Body: { \"score\": 85.5 } — score chỉ bắt buộc nếu lessonType = MINI_TEST. " +
                    "bestScore chỉ update khi score mới cao hơn. " +
                    "Khi progress đạt 100%, enrollment tự động COMPLETED.")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping(ApiPaths.Progress.COMPLETE)
    public ApiResponse<ProgressResponse> completeLesson(
            @PathVariable Long lessonId,
            @RequestBody @Valid LessonCompleteRequest request) {
        return ApiResponse.<ProgressResponse>builder()
                .success(true)
                .message("Lesson completed")
                .data(progressService.completeLesson(lessonId, request))
                .build();
    }

    // ============================================================
    // READ
    // ============================================================

    @Operation(summary = "Lấy progress của user hiện tại cho 1 lesson (null nếu chưa start)")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping(ApiPaths.Progress.BY_LESSON)
    public ApiResponse<ProgressResponse> getMyProgressForLesson(
            @PathVariable Long lessonId) {
        ProgressResponse progress = progressService.getMyProgressForLesson(lessonId);
        return ApiResponse.<ProgressResponse>builder()
                .success(true)
                .data(progress)  // null được phép
                .message(progress == null ? "Chưa bắt đầu lesson này" : null)
                .build();
    }

    @Operation(summary = "Lấy toàn bộ progress của user hiện tại trong 1 course (kèm stats)")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping(ApiPaths.Progress.BY_COURSE)
    public ApiResponse<CourseProgressResponse> getMyProgressForCourse(
            @PathVariable Long courseId) {
        return ApiResponse.<CourseProgressResponse>builder()
                .success(true)
                .data(progressService.getMyProgressForCourse(courseId))
                .build();
    }
}