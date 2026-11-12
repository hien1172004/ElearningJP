package com.jp.elearningjp.controller.curriculum;

import com.jp.elearningjp.dto.response.curriculum.CourseChangesResponse;
import com.jp.elearningjp.service.curriculum.CourseReviewService;
import com.jp.elearningjp.service.curriculum.snapshot.CourseChangesService;
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

/**
 * Phiên bản cả khóa: ADMIN lưu nháp ADMIN của mọi bài (kèm đề thi gắn khóa) thành phiên bản mới. Luồng gửi / duyệt
 * cả khóa đã bỏ — duyệt chỉ còn theo bài ({@link LessonReviewController}).
 */
@Tag(name = "Course Version", description = "Quản trị viên lưu cả khóa thành phiên bản mới của bản học viên")
@RestController
@RequestMapping(ApiPaths.API_V1)
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@SecurityRequirement(name = "bearerAuth")
public class CourseReviewController {

    CourseReviewService courseReviewService;
    CourseChangesService changesService;

    @Operation(summary = "[Admin/Teacher] Thay đổi chưa lưu so với bản học viên đang học",
            description = "Mỗi phía so nháp của mình với bản học viên đang học: bài / mục / câu hỏi thêm, bỏ, sửa và "
                    + "đề thi gắn khóa; riêng quản trị viên, bài giáo viên đang chờ duyệt so theo bản đã gửi. "
                    + "Quản trị viên hoặc giáo viên được giao")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @GetMapping(ApiPaths.Course.BASE + ApiPaths.Course.PENDING_CHANGES)
    public ApiResponse<CourseChangesResponse> getChanges(@PathVariable Long courseId) {
        return ApiResponse.<CourseChangesResponse>builder()
                .success(true)
                .data(changesService.getChanges(courseId))
                .build();
    }

    @Operation(summary = "[Admin] Lưu cả khóa thành phiên bản mới",
            description = "Chụp nháp quản trị viên của mọi bài (kèm đề thi gắn khóa) lên bản học viên đang học, không qua "
                    + "duyệt, không bật xuất bản. Cần có thay đổi chưa lưu, ít nhất 1 bài học và không còn bài chờ duyệt "
                    + "(hãy duyệt / từ chối trước). Bài mới giáo viên đang soạn / chờ duyệt không được chụp")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(ApiPaths.Course.BASE + ApiPaths.Course.VERSIONS)
    public ApiResponse<Void> saveVersion(@PathVariable Long courseId) {
        courseReviewService.saveVersion(courseId);
        return ApiResponse.<Void>builder()
                .success(true)
                .message("Đã lưu phiên bản mới của khóa học")
                .build();
    }
}
