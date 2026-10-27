package com.jp.elearningjp.controller.user;

import com.jp.elearningjp.dto.request.user.UpdateProfileRequest;
import com.jp.elearningjp.dto.response.user.ChangePasswordRequest;
import com.jp.elearningjp.dto.response.user.UserProfileResponse;
import com.jp.elearningjp.service.user.UserService;
import com.jp.elearningjp.shared.constants.ApiPaths;
import com.jp.elearningjp.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Tag(name = "User Management", description = "Quản lý thông tin tài khoản, profile và mật khẩu người dùng")
@RestController
@RequestMapping(ApiPaths.API_V1 + ApiPaths.User.BASE)
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@SecurityRequirement(name = "bearerAuth")
@Slf4j
public class UserController {

    UserService userService;

    @Operation(summary = "Lấy profile cá nhân", description = "Lấy toàn bộ thông tin tài khoản và profile học tập của người dùng đang đăng nhập.")
    @GetMapping(ApiPaths.User.ME)
    public ApiResponse<UserProfileResponse> getMyProfile() {
        return ApiResponse.<UserProfileResponse>builder()
                .success(true)
                .data(userService.getMyProfile())
                .build();
    }

    @Operation(summary = "Cập nhật profile cá nhân", description = "Cập nhật họ tên, số điện thoại, bio và mục tiêu học tập JLPT.")
    @PutMapping(ApiPaths.User.ME)
    public ApiResponse<UserProfileResponse> updateMyProfile(@Valid @RequestBody UpdateProfileRequest request) {
        return ApiResponse.<UserProfileResponse>builder()
                .success(true)
                .message("Cập nhật thông tin thành công")
                .data(userService.updateMyProfile(request))
                .build();
    }

    @Operation(summary = "Đổi ảnh đại diện (Avatar)", description = "Upload file ảnh đại diện mới lên Cloudinary và cập nhật đường dẫn vào tài khoản.")
    @PostMapping(value = ApiPaths.User.CHANGE_AVATAR, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Map<String, String>> changeAvatar(@RequestParam("file") MultipartFile file) {
        String avatarUrl = userService.changeAvatar(file);
        return ApiResponse.<Map<String, String>>builder()
                .success(true)
                .message("Cập nhật ảnh đại diện thành công")
                .data(Map.of("avatarUrl", avatarUrl))
                .build();
    }

    @Operation(summary = "Đổi mật khẩu", description = "Cho phép người dùng đang đăng nhập đổi mật khẩu tài khoản.")
    @PutMapping(ApiPaths.User.CHANGE_PASSWORD)
    public ApiResponse<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(request);
        return ApiResponse.<Void>builder()
                .success(true)
                .message("Đổi mật khẩu thành công")
                .build();
    }
}
