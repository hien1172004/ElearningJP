package com.jp.elearningjp.controller.curriculum;

import com.jp.elearningjp.dto.request.curriculum.CourseCreateRequest;
import com.jp.elearningjp.dto.request.curriculum.CourseUpdateRequest;
import com.jp.elearningjp.dto.response.curriculum.CourseDetailResponse;
import com.jp.elearningjp.dto.response.curriculum.CourseProgressResponse;
import com.jp.elearningjp.dto.response.curriculum.CourseResponse;
import com.jp.elearningjp.service.curriculum.CourseService;
import com.jp.elearningjp.shared.constants.ApiPaths;
import com.jp.elearningjp.shared.enums.CourseReviewStatus;
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

    @Operation(summary = "[Admin/Teacher] Danh sách khóa học để quản lý",
            description = "Kể cả khóa chưa xuất bản. Quản trị viên thấy toàn bộ khóa học; giáo viên chỉ thấy khóa học được giao cho mình. "
                    + "pendingChanges=true: chỉ khóa có thay đổi chưa chốt. "
                    + "reviewStatus: PENDING_REVIEW = đã gửi, chờ duyệt; EDITING (kèm pendingChanges=true) = đang soạn, chưa gửi duyệt. "
                    + "Lọc thêm: keyword (tên khóa), jlptLevel, published (true = đã xuất bản, false = bản nháp)")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @GetMapping("/admin")
    public ApiResponse<PageResponse<CourseResponse>> getAllCoursesForAdmin(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "false") boolean pendingChanges,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) JlptLevel jlptLevel,
            @RequestParam(required = false) Boolean published,
            @RequestParam(required = false) CourseReviewStatus reviewStatus
    ) {
        return ApiResponse.<PageResponse<CourseResponse>>builder()
                .success(true)
                .data(courseService.getAllCoursesForAdmin(page, size, pendingChanges, keyword, jlptLevel, published, reviewStatus))
                .build();
    }

    @Operation(summary = "[Admin] Tạo khóa học mới", description = "Chỉ quản trị viên tạo khóa học, tự động sinh slug từ tiêu đề. Giáo viên được giao sau khi tạo")
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

    @Operation(summary = "[Admin] Cập nhật thông tin khóa học", description = "Chỉnh sửa tiêu đề, mô tả, ảnh bìa, cấp JLPT của khóa học. Chỉ quản trị viên, có hiệu lực ngay")
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

    @Operation(summary = "[Admin] Xóa mềm khóa học", description = "Ẩn khóa học khỏi hệ thống mà không làm mất dữ liệu liên quan. Chỉ quản trị viên")
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

    @Operation(summary = "[Admin] Chuyển nháp / mở lại khóa học",
            description = "publish=false: ẩn khóa với học viên. publish=true: mở lại với đúng bản đã duyệt gần nhất "
                    + "(khóa chưa từng được duyệt → lỗi 1115). Nội dung mới chỉ tới học viên qua duyệt yêu cầu của giáo viên")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping(ApiPaths.Course.PUBLISH)
    public ApiResponse<CourseResponse> publishCourse(
            @PathVariable Long id,
            @RequestParam boolean publish
    ) {
        return ApiResponse.<CourseResponse>builder()
                .success(true)
                .message(publish ? "Đã mở lại khóa học với bản đã duyệt" : "Đã chuyển khóa học về trạng thái nháp")
                .data(courseService.publishCourse(id, publish))
                .build();
    }

    @Operation(summary = "[Admin] Bật/tắt trạng thái Premium (dành riêng cho hội viên PRO) của khóa học")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/premium")
    public ApiResponse<CourseResponse> setCoursePremium(
            @PathVariable Long id,
            @RequestParam boolean isPremium
    ) {
        return ApiResponse.<CourseResponse>builder()
                .success(true)
                .message(isPremium ? "Đã chuyển khóa học sang gói Premium (Chỉ dành cho hội viên PRO)" : "Đã chuyển khóa học sang Miễn phí")
                .data(courseService.setCoursePremium(id, isPremium))
                .build();
    }
}
