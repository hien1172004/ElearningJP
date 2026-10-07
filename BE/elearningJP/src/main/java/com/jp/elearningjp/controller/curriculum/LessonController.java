package com.jp.elearningjp.controller.curriculum;

import com.jp.elearningjp.dto.request.curriculum.LessonCreateRequest;
import com.jp.elearningjp.dto.request.curriculum.LessonUpdateRequest;
import com.jp.elearningjp.dto.response.curriculum.CourseProgressResponse;
import com.jp.elearningjp.dto.response.curriculum.LessonDetailResponse;
import com.jp.elearningjp.service.curriculum.LessonService;
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

@Tag(name = "Lessons", description = "Quản lý bài học, xem nội dung bài giảng và theo dõi tiến độ")
@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LessonController {

    LessonService lessonService;

    @Operation(summary = "Xem nội dung chi tiết bài học", description = "Chỉ người dùng ĐÃ ĐĂNG KÝ KHÓA HỌC mới có quyền xem bài giảng. Tự động ghi nhận trạng thái IN_PROGRESS.")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping(ApiPaths.API_V1 + ApiPaths.Lesson.BASE + ApiPaths.Lesson.BY_ID)
    public ApiResponse<LessonDetailResponse> getLessonDetail(@PathVariable Long id) {
        return ApiResponse.<LessonDetailResponse>builder()
                .success(true)
                .data(lessonService.getLessonDetail(id))
                .build();
    }

    @Operation(summary = "[Admin] Thêm bài học mới vào khóa học", description = "Thêm bài học mới, tự động tính số thứ tự tiếp theo nếu không nhập")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(ApiPaths.API_V1 + ApiPaths.Course.BASE + ApiPaths.Course.LESSONS)
    public ApiResponse<LessonDetailResponse> createLesson(
            @PathVariable Long id,
            @Valid @RequestBody LessonCreateRequest request
    ) {
        return ApiResponse.<LessonDetailResponse>builder()
                .success(true)
                .message("Thêm bài học vào khóa học thành công")
                .data(lessonService.createLesson(id, request))
                .build();
    }

    @Operation(summary = "[Admin] Chỉnh sửa bài học", description = "Cập nhật nội dung, tiêu đề hoặc thời lượng bài học")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping(ApiPaths.API_V1 + ApiPaths.Lesson.BASE + ApiPaths.Lesson.BY_ID)
    public ApiResponse<LessonDetailResponse> updateLesson(
            @PathVariable Long id,
            @Valid @RequestBody LessonUpdateRequest request
    ) {
        return ApiResponse.<LessonDetailResponse>builder()
                .success(true)
                .message("Cập nhật bài học thành công")
                .data(lessonService.updateLesson(id, request))
                .build();
    }

    @Operation(summary = "[Admin] Xóa mềm bài học", description = "Ẩn bài học khỏi khóa học")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping(ApiPaths.API_V1 + ApiPaths.Lesson.BASE + ApiPaths.Lesson.BY_ID)
    public ApiResponse<Void> deleteLesson(@PathVariable Long id) {
        lessonService.deleteLesson(id);
        return ApiResponse.<Void>builder()
                .success(true)
                .message("Đã xóa bài học thành công")
                .build();
    }

    @Operation(summary = "Đánh dấu hoàn thành bài học thủ công", description = "Dành cho các bài học lý thuyết/không có quiz. Đánh dấu COMPLETED và cập nhật lại % tiến độ của khóa học.")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping(ApiPaths.API_V1 + ApiPaths.Lesson.BASE + ApiPaths.Lesson.COMPLETE)
    public ApiResponse<CourseProgressResponse> completeLesson(@PathVariable Long id) {
        return ApiResponse.<CourseProgressResponse>builder()
                .success(true)
                .message("Chúc mừng bạn đã hoàn thành bài học!")
                .data(lessonService.completeLesson(id))
                .build();
    }
}
