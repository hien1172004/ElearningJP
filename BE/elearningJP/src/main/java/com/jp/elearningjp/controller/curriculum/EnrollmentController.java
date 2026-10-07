package com.jp.elearningjp.controller.curriculum;

import com.jp.elearningjp.dto.response.curriculum.EnrollmentResponse;
import com.jp.elearningjp.service.curriculum.EnrollmentService;
import com.jp.elearningjp.shared.constants.ApiPaths;
import com.jp.elearningjp.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Enrollment", description = "Quản lý đăng ký học")
@RestController
@RequestMapping(ApiPaths.API_V1)
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class EnrollmentController {

    EnrollmentService enrollmentService;

    // ============================================================
    // ENROLL (sub-resource của Course)
    // ============================================================

    @Operation(summary = "Enroll vào course (current user)",
            description = "Tự động tạo mới hoặc reactivate enrollment nếu đã drop trước đó.")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping(ApiPaths.Course.BASE + ApiPaths.Course.ENROLL)
    public ResponseEntity<ApiResponse<EnrollmentResponse>> enrollInCourse(
            @PathVariable Long id) {
        EnrollmentResponse enrolled = enrollmentService.enrollInCourse(id);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<EnrollmentResponse>builder()
                        .success(true)
                        .code(201)
                        .message("Enrolled successfully")
                        .data(enrolled)
                        .build());
    }

    // ============================================================
    // READ
    // ============================================================

    @Operation(summary = "Lấy tất cả enrollment của user hiện tại (mọi status)")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping(ApiPaths.Enrollment.BASE + ApiPaths.Enrollment.ME)
    public ApiResponse<List<EnrollmentResponse>> getMyEnrollments() {
        return ApiResponse.<List<EnrollmentResponse>>builder()
                .success(true)
                .data(enrollmentService.getMyEnrollments())
                .build();
    }

    @Operation(summary = "Lấy enrollment ACTIVE của user hiện tại (dashboard)")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping(ApiPaths.Enrollment.BASE + ApiPaths.Enrollment.ME + "/active")
    public ApiResponse<List<EnrollmentResponse>> getMyActiveEnrollments() {
        return ApiResponse.<List<EnrollmentResponse>>builder()
                .success(true)
                .data(enrollmentService.getMyActiveEnrollments())
                .build();
    }

    @Operation(summary = "Lấy chi tiết 1 enrollment (owner hoặc admin)")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping(ApiPaths.Enrollment.BASE + ApiPaths.Enrollment.BY_ID)
    public ApiResponse<EnrollmentResponse> getEnrollmentById(
            @PathVariable Long enrollmentId) {
        return ApiResponse.<EnrollmentResponse>builder()
                .success(true)
                .data(enrollmentService.getEnrollmentById(enrollmentId))
                .build();
    }

    // ============================================================
    // DROP
    // ============================================================

    @Operation(summary = "Drop enrollment theo enrollmentId (owner hoặc admin)")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping(ApiPaths.Enrollment.BASE + ApiPaths.Enrollment.DROP)
    public ResponseEntity<ApiResponse<Void>> dropEnrollment(
            @PathVariable Long enrollmentId) {
        enrollmentService.dropEnrollment(enrollmentId);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Enrollment dropped successfully")
                .build());
    }

    @Operation(summary = "Drop enrollment theo courseId (tiện cho FE, không cần biết enrollmentId)")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping(ApiPaths.Enrollment.BASE + "/by-course/{courseId}/drop")
    public ResponseEntity<ApiResponse<Void>> dropEnrollmentByCourseId(
            @PathVariable Long courseId) {
        enrollmentService.dropEnrollmentByCourseId(courseId);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Enrollment dropped successfully")
                .build());
    }
}