package com.jp.elearningjp.controller.user;

import com.jp.elearningjp.dto.request.user.AdminUpdateUserRolesRequest;
import com.jp.elearningjp.dto.request.user.AdminUpdateUserStatusRequest;
import com.jp.elearningjp.dto.response.user.AdminUserResponse;
import com.jp.elearningjp.service.user.AdminUserService;
import com.jp.elearningjp.shared.constants.ApiPaths;
import com.jp.elearningjp.shared.enums.UserStatus;
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

@RestController
@RequestMapping(ApiPaths.API_V1 + ApiPaths.AdminUser.BASE)
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Admin User", description = "APIs dành cho Quản trị viên quản lý tài khoản người dùng")
public class AdminUserController {

    AdminUserService adminUserService;

    @GetMapping
    @Operation(summary = "[Admin] Danh sách người dùng",
            description = "Tìm theo email/họ tên, lọc theo vai trò (USER/TEACHER/ADMIN) và trạng thái (ACTIVE/INACTIVE/BANNED). Trang bắt đầu từ 1.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<PageResponse<AdminUserResponse>> getUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.<PageResponse<AdminUserResponse>>builder()
                .success(true)
                .data(adminUserService.getUsers(keyword, role, status, page, size))
                .build();
    }

    @GetMapping(ApiPaths.AdminUser.BY_ID)
    @Operation(summary = "[Admin] Xem chi tiết tài khoản", description = "Thông tin tài khoản, vai trò và trạng thái của một người dùng")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<AdminUserResponse> getUser(@PathVariable Long id) {
        return ApiResponse.<AdminUserResponse>builder()
                .success(true)
                .data(adminUserService.getUser(id))
                .build();
    }

    @PatchMapping(ApiPaths.AdminUser.STATUS)
    @Operation(summary = "[Admin] Khóa / mở khóa tài khoản",
            description = "Đổi trạng thái tài khoản. Chuyển sang INACTIVE/BANNED sẽ đăng xuất người dùng khỏi mọi thiết bị. Không thể tự đổi trạng thái của chính mình.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<AdminUserResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody AdminUpdateUserStatusRequest request
    ) {
        return ApiResponse.<AdminUserResponse>builder()
                .success(true)
                .message("Cập nhật trạng thái tài khoản thành công")
                .data(adminUserService.updateStatus(id, request))
                .build();
    }

    @PutMapping(ApiPaths.AdminUser.ROLES)
    @Operation(summary = "[Admin] Phân quyền tài khoản",
            description = "Gán lại vai trò (TEACHER/ADMIN); vai trò USER luôn được giữ. Đổi vai trò sẽ buộc người dùng đăng nhập lại. Không thể tự đổi vai trò của chính mình.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<AdminUserResponse> updateRoles(
            @PathVariable Long id,
            @Valid @RequestBody AdminUpdateUserRolesRequest request
    ) {
        return ApiResponse.<AdminUserResponse>builder()
                .success(true)
                .message("Cập nhật vai trò thành công")
                .data(adminUserService.updateRoles(id, request))
                .build();
    }
}
