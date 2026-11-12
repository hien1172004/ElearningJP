package com.jp.elearningjp.controller.curriculum;

import com.jp.elearningjp.service.curriculum.CourseEditLockService;
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

@Tag(name = "Course Edit Lock", description = "Khóa sửa khóa học theo phía (giáo viên / quản trị viên): mỗi phía 1 người sửa tại một thời điểm")
@RestController
@RequestMapping(ApiPaths.API_V1 + ApiPaths.Course.BASE)
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@SecurityRequirement(name = "bearerAuth")
public class CourseEditLockController {

    CourseEditLockService courseEditLockService;

    @Operation(summary = "[Admin/Teacher] Ai đang sửa khóa học (phía mình)",
            description = "Trả người đang giữ khóa sửa của phía người gọi (giáo viên được giao / quản trị viên); null = chưa ai sửa")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @GetMapping(ApiPaths.Course.EDIT_LOCK)
    public ApiResponse<CourseEditLockService.LockInfo> current(@PathVariable Long courseId) {
        return ApiResponse.<CourseEditLockService.LockInfo>builder()
                .success(true)
                .data(courseEditLockService.readForCurrentUser(courseId).orElse(null))
                .build();
    }

    @Operation(summary = "[Admin/Teacher] Bắt đầu sửa khóa học (lấy khóa)",
            description = "Người cùng phía đang sửa → 1259 kèm tên, trừ khi takeover=true (tiếp quản; người cũ ở lần ghi kế tiếp "
                    + "nhận 1258). Khóa sống 5 phút, gia hạn bằng PUT. Hai phía (giáo viên / quản trị viên) sửa hai bản nháp "
                    + "khác nhau nên không chặn nhau")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @PostMapping(ApiPaths.Course.EDIT_LOCK)
    public ApiResponse<CourseEditLockService.LockInfo> acquire(
            @PathVariable Long courseId,
            @RequestParam(defaultValue = "false") boolean takeover
    ) {
        return ApiResponse.<CourseEditLockService.LockInfo>builder()
                .success(true)
                .message(takeover ? "Đã tiếp quản việc sửa khóa học" : "Đã bắt đầu sửa khóa học")
                .data(courseEditLockService.acquire(courseId, takeover))
                .build();
    }

    @Operation(summary = "[Admin/Teacher] Gia hạn khóa sửa khóa học",
            description = "Gọi định kỳ (60 giây) khi trang soạn đang mở. Hết hạn / bị tiếp quản → 1258, bấm Sửa khóa học để bắt đầu lại")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @PutMapping(ApiPaths.Course.EDIT_LOCK)
    public ApiResponse<Void> heartbeat(@PathVariable Long courseId) {
        courseEditLockService.heartbeat(courseId);
        return ApiResponse.<Void>builder().success(true).build();
    }

    @Operation(summary = "[Admin/Teacher] Xong — nhả khóa sửa khóa học",
            description = "Phần đã ghi vẫn nằm trong bản nháp của phía mình. Không giữ khóa thì bỏ qua")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @DeleteMapping(ApiPaths.Course.EDIT_LOCK)
    public ApiResponse<Void> release(@PathVariable Long courseId) {
        courseEditLockService.release(courseId);
        return ApiResponse.<Void>builder().success(true).message("Đã kết thúc sửa khóa học").build();
    }
}
