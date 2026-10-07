package com.jp.elearningjp.controller.curriculum;

import com.jp.elearningjp.dto.request.curriculum.CourseCreateRequest;
import com.jp.elearningjp.dto.request.curriculum.CourseUpdateRequest;
import com.jp.elearningjp.dto.response.curriculum.CourseDetailResponse;
import com.jp.elearningjp.dto.response.curriculum.CourseProgressResponse;
import com.jp.elearningjp.dto.response.curriculum.CourseResponse;
import com.jp.elearningjp.service.curriculum.CourseService;
import com.jp.elearningjp.shared.constants.ApiPaths;
import com.jp.elearningjp.shared.enums.JlptLevel;
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

@Tag(name = "Courses", description = "Quản lý khóa học, đăng ký học và theo dõi tiến độ")
@RestController
@RequestMapping(ApiPaths.API_V1 + ApiPaths.Course.BASE)
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CourseController {

    CourseService courseService;

    @Operation(summary = "Lấy danh sách khóa học công khai", description = "Dành cho học viên và khách xem danh sách khóa học, có thể lọc theo JLPT (N5 - N1) và phân trang", security = {})
    @GetMapping
    public ApiResponse<PageResponse<CourseResponse>> getPublicCourses(
            @RequestParam(required = false) JlptLevel jlptLevel,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.<PageResponse<CourseResponse>>builder()
                .success(true)
                .data(courseService.getPublicCourses(jlptLevel, page, size))
                .build();
    }

    @Operation(summary = "Xem chi tiết khóa học & lộ trình", description = "Xem thông tin khóa học, mục lục các bài học (outline). Nếu đã đăng ký sẽ có cờ isEnrolled=true và % tiến độ.")
    @GetMapping(ApiPaths.Course.BY_ID)
    public ApiResponse<CourseDetailResponse> getCourseDetail(@PathVariable Long id) {
        return ApiResponse.<CourseDetailResponse>builder()
                .success(true)
                .data(courseService.getCourseDetail(id))
                .build();
    }

    @Operation(summary = "Xem % tiến độ học tập của khóa học", description = "Xem số bài đã hoàn thành và % tiến độ của học viên trong khóa")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping(ApiPaths.Course.PROGRESS)
    public ApiResponse<CourseProgressResponse> getCourseProgress(@PathVariable Long id) {
        return ApiResponse.<CourseProgressResponse>builder()
                .success(true)
                .data(courseService.getCourseProgress(id))
                .build();
    }

    // =========================================================
    // ADMIN ENDPOINTS
    // =========================================================

    @Operation(summary = "[Admin] Lấy toàn bộ danh sách khóa học", description = "Dành cho quản trị viên xem toàn bộ khóa học (kể cả khóa chưa xuất bản)")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin")
    public ApiResponse<PageResponse<CourseResponse>> getAllCoursesForAdmin(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.<PageResponse<CourseResponse>>builder()
                .success(true)
                .data(courseService.getAllCoursesForAdmin(page, size))
                .build();
    }

    @Operation(summary = "[Admin] Tạo khóa học mới", description = "Tạo khóa học mới, tự động sinh slug từ tiêu đề")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ApiResponse<CourseResponse> createCourse(@Valid @RequestBody CourseCreateRequest request) {
        return ApiResponse.<CourseResponse>builder()
                .success(true)
                .message("Tạo khóa học mới thành công")
                .data(courseService.createCourse(request))
                .build();
    }

    @Operation(summary = "[Admin] Cập nhật thông tin khóa học", description = "Chỉnh sửa thông tin khóa học theo ID")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping(ApiPaths.Course.BY_ID)
    public ApiResponse<CourseResponse> updateCourse(
            @PathVariable Long id,
            @Valid @RequestBody CourseUpdateRequest request
    ) {
        return ApiResponse.<CourseResponse>builder()
                .success(true)
                .message("Cập nhật khóa học thành công")
                .data(courseService.updateCourse(id, request))
                .build();
    }

    @Operation(summary = "[Admin] Xóa mềm khóa học", description = "Ẩn khóa học khỏi hệ thống mà không làm mất dữ liệu liên quan")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping(ApiPaths.Course.BY_ID)
    public ApiResponse<Void> deleteCourse(@PathVariable Long id) {
        courseService.deleteCourse(id);
        return ApiResponse.<Void>builder()
                .success(true)
                .message("Đã xóa khóa học thành công")
                .build();
    }

    @Operation(summary = "[Admin] Bật/tắt xuất bản khóa học", description = "Chuyển đổi trạng thái hiển thị công khai của khóa học")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping(ApiPaths.Course.PUBLISH)
    public ApiResponse<CourseResponse> publishCourse(
            @PathVariable Long id,
            @RequestParam boolean publish
    ) {
        return ApiResponse.<CourseResponse>builder()
                .success(true)
                .message(publish ? "Đã xuất bản khóa học công khai" : "Đã chuyển khóa học về trạng thái nháp")
                .data(courseService.publishCourse(id, publish))
                .build();
    }
}
