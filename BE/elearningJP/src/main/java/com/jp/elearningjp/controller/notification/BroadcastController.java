package com.jp.elearningjp.controller.notification;

import com.jp.elearningjp.dto.request.notification.BroadcastRequest;
import com.jp.elearningjp.dto.response.notification.BroadcastResponse;
import com.jp.elearningjp.service.notification.BroadcastService;
import com.jp.elearningjp.shared.constants.ApiPaths;
import com.jp.elearningjp.shared.enums.BroadcastAudience;
import com.jp.elearningjp.shared.response.ApiResponse;
import com.jp.elearningjp.shared.response.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(ApiPaths.API_V1 + ApiPaths.AdminBroadcast.BASE)
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Admin Broadcast", description = "Gửi thông báo hàng loạt (Quản trị viên: mọi đối tượng; Giáo viên: học viên khóa được giao)")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
public class BroadcastController {

    BroadcastService broadcastService;

    @GetMapping(ApiPaths.AdminBroadcast.RECIPIENT_COUNT)
    @Operation(summary = "[Admin/Teacher] Đếm số người sẽ nhận",
            description = "Xem trước số người nhận trước khi gửi. audience: ALL_USERS | ROLE (kèm role) | COURSE_STUDENTS (kèm courseId)")
    public ApiResponse<Long> countRecipients(
            @RequestParam BroadcastAudience audience,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Long courseId
    ) {
        return ApiResponse.<Long>builder()
                .success(true)
                .data(broadcastService.countRecipients(audience, role, courseId))
                .build();
    }

    @PostMapping
    @Operation(summary = "[Admin/Teacher] Gửi thông báo hàng loạt",
            description = "Tạo thông báo cho mọi tài khoản đang hoạt động thuộc đối tượng đã chọn (không gồm người gửi) và đẩy realtime. "
                    + "Giáo viên chỉ được gửi cho học viên khóa học được giao.")
    public ApiResponse<BroadcastResponse> send(@Valid @RequestBody BroadcastRequest request) {
        BroadcastResponse result = broadcastService.send(request);
        return ApiResponse.<BroadcastResponse>builder()
                .success(true)
                .code(HttpStatus.CREATED.value())
                .message("Đã gửi thông báo tới " + result.getRecipientCount() + " người")
                .data(result)
                .build();
    }

    @GetMapping
    @Operation(summary = "[Admin/Teacher] Lịch sử gửi thông báo",
            description = "Quản trị viên xem mọi lần gửi; giáo viên chỉ xem lần gửi của mình. Trang bắt đầu từ 1.")
    public ApiResponse<PageResponse<BroadcastResponse>> getHistory(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.<PageResponse<BroadcastResponse>>builder()
                .success(true)
                .data(broadcastService.getHistory(page, size))
                .build();
    }
}
