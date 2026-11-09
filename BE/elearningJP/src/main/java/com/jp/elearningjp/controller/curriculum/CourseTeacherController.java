package com.jp.elearningjp.controller.curriculum;

import com.jp.elearningjp.dto.request.curriculum.CourseTeacherAssignRequest;
import com.jp.elearningjp.dto.response.curriculum.CourseTeacherResponse;
import com.jp.elearningjp.service.curriculum.CourseTeacherService;
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

@Tag(name = "Course Teachers", description = "Phân công giáo viên cho khóa học")
@RestController
@RequestMapping(ApiPaths.API_V1 + ApiPaths.Course.BASE)
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@SecurityRequirement(name = "bearerAuth")
public class CourseTeacherController {

    CourseTeacherService courseTeacherService;

    @Operation(summary = "[Admin/Teacher] Danh sách giáo viên được giao khóa học",
            description = "Quản trị viên xem mọi khóa; giáo viên chỉ xem được khóa mình được giao")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @GetMapping(ApiPaths.Course.TEACHERS)
    public ApiResponse<List<CourseTeacherResponse>> getTeachers(@PathVariable Long courseId) {
        return ApiResponse.<List<CourseTeacherResponse>>builder()
                .success(true)
                .data(courseTeacherService.getTeachers(courseId))
                .build();
    }

    @Operation(summary = "[Admin] Giao giáo viên cho khóa học",
            description = "Giao một giáo viên (vai trò TEACHER, tài khoản đang hoạt động) cho khóa học. Giáo viên nhận thông báo")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(ApiPaths.Course.TEACHERS)
    public ApiResponse<CourseTeacherResponse> assignTeacher(
            @PathVariable Long courseId,
            @Valid @RequestBody CourseTeacherAssignRequest request
    ) {
        return ApiResponse.<CourseTeacherResponse>builder()
                .success(true)
                .message("Đã giao giáo viên cho khóa học")
                .data(courseTeacherService.assignTeacher(courseId, request.getTeacherId()))
                .build();
    }

    @Operation(summary = "[Admin] Gỡ giáo viên khỏi khóa học",
            description = "Giáo viên mất quyền chỉnh sửa khóa học; bài học họ đã tạo vẫn giữ nguyên. Giáo viên nhận thông báo")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping(ApiPaths.Course.TEACHER_BY_ID)
    public ApiResponse<Void> unassignTeacher(@PathVariable Long courseId, @PathVariable Long teacherId) {
        courseTeacherService.unassignTeacher(courseId, teacherId);
        return ApiResponse.<Void>builder()
                .success(true)
                .message("Đã gỡ giáo viên khỏi khóa học")
                .build();
    }
}
