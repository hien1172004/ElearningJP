package com.jp.elearningjp.controller.curriculum;

import com.jp.elearningjp.dto.response.curriculum.CourseStudentsResponse;
import com.jp.elearningjp.service.curriculum.CourseStudentService;
import com.jp.elearningjp.shared.constants.ApiPaths;
import com.jp.elearningjp.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(ApiPaths.API_V1 + ApiPaths.TeacherStudents.BASE)
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Teacher Students", description = "Theo dõi học viên của khóa học (Quản trị viên: mọi khóa; Giáo viên: khóa được giao)")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
public class CourseStudentController {

    CourseStudentService courseStudentService;

    @GetMapping
    @Operation(summary = "[Admin/Teacher] Học viên của khóa học",
            description = "Tổng quan + danh sách học viên kèm % tiến độ, lần học gần nhất, điểm quiz trung bình. "
                    + "Tìm theo tên/email; sort: LAST_ACTIVE (mặc định) | PROGRESS | NAME | ENROLLED. Trang bắt đầu từ 1.")
    public ApiResponse<CourseStudentsResponse> getStudents(
            @PathVariable Long courseId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.<CourseStudentsResponse>builder()
                .success(true)
                .data(courseStudentService.getStudents(courseId, keyword, sort, page, size))
                .build();
    }

    @GetMapping(ApiPaths.TeacherStudents.BY_USER)
    @Operation(summary = "[Admin/Teacher] Tiến độ từng bài của một học viên")
    public ApiResponse<CourseStudentsResponse.StudentDetail> getStudentDetail(
            @PathVariable Long courseId,
            @PathVariable Long userId
    ) {
        return ApiResponse.<CourseStudentsResponse.StudentDetail>builder()
                .success(true)
                .data(courseStudentService.getStudentDetail(courseId, userId))
                .build();
    }
}
