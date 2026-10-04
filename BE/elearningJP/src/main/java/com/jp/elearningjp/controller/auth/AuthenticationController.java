package com.jp.elearningjp.controller.auth;


import com.jp.elearningjp.dto.request.auth.*;
import com.jp.elearningjp.dto.request.user.RegisterRequest;
import com.jp.elearningjp.dto.response.auth.AuthResponse;
import com.jp.elearningjp.dto.response.auth.LoginResponse;
import com.jp.elearningjp.dto.response.user.ForgotPasswordRequest;
import com.jp.elearningjp.dto.response.user.RegisterResponse;
import com.jp.elearningjp.service.auth.AuthService;
import com.jp.elearningjp.service.auth.PasswordResetService;
import com.jp.elearningjp.shared.constants.ApiPaths;
import com.jp.elearningjp.shared.response.ApiResponse;
import com.jp.elearningjp.shared.response.IntrospectResponse;
import com.nimbusds.jose.JOSEException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.text.ParseException;

@Tag(name = "Authentication", description = "xác thực người dùng và quản lý mật khẩu")
@RestController
@RequestMapping( ApiPaths.API_V1 + ApiPaths.Auth.BASE)
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class AuthenticationController {
    AuthService authService;
    PasswordResetService passwordResetService;
    //API Register
    @Operation(summary = "Đăng ký người dùng mới", description = "Tạo tài khoản người dùng mới với thông tin cung cấp.", security = {})
    @PostMapping(ApiPaths.Auth.REGISTER)
    public ApiResponse<RegisterResponse> registerUser(@RequestBody @Valid RegisterRequest registerRequest) {
        return ApiResponse.<RegisterResponse>builder()
                .success(true)
                .data(authService.registerUser(registerRequest))
                .build();
    }
    //API Login
    @Operation(summary = "Đăng nhập", description = "Xác thực người dùng và trả về access token.", security = {})
    @PostMapping(ApiPaths.Auth.LOGIN)
    public ApiResponse<LoginResponse> loginUser(@RequestBody @Valid LoginRequest loginRequest) {
        return ApiResponse.<LoginResponse>builder()
                .success(true)
                .data(authService.loginUser(loginRequest)).build();
    }

    @Operation(summary = "Đăng xuất", description = "Thu hồi refresh token của người dùng để đăng xuất.")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping(ApiPaths.Auth.LOGOUT)
    public ApiResponse<Void> logout(@RequestBody @Valid LogoutRequest request)
            throws ParseException, JOSEException {
        authService.logout(request);
        return ApiResponse.<Void>builder()
                .success(true)
                .message("Logged out successfully")
                .build();
    }

    @Operation(summary = "Cấp lại access token", description = "Lấy access token mới bằng refresh token hợp lệ.", security = {})
    @PostMapping(ApiPaths.Auth.REFRESH)
    public ApiResponse<AuthResponse> refresh(@RequestBody @Valid RefreshRequest request)
            throws ParseException, JOSEException {
        return ApiResponse.<AuthResponse>builder()
                .success(true)
                .data(authService.refreshToken(request))
                .build();
    }

    @Operation(summary = "Kiểm tra token", description = "Kiểm tra tính hợp lệ và chi tiết của access token.")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping(ApiPaths.Auth.INTROSPECT)
    public ApiResponse<IntrospectResponse> introspect(@RequestBody @Valid IntrospectReq request) {
        return ApiResponse.<IntrospectResponse>builder()
                .success(true)
                .data(authService.introspect(request))
                .build();
    }

    /**
     * Bước 1: Gửi email reset
     * POST /auth/forgot-password
     * Body: { "email": "user@gmail.com" }
     */
    @Operation(summary = "Quên mật khẩu", description = "Yêu cầu gửi link đặt lại mật khẩu đến email người dùng.", security = {})
    @PostMapping(ApiPaths.Auth.FORGOT_PASSWORD)
    public ApiResponse<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordResetService.forgotPassword(request);
        log.info("[AUTH CONTROLLER] Password reset requested for email: {}", request.getEmail());
        return ApiResponse.<Void>builder()
                .success(true)
                .message("Nếu email tồn tại, link đặt lại mật khẩu đã được gửi.")
                .build();
    }

    /**
     * Bước 1.5: FE kiểm tra token còn hợp lệ không trước khi hiện form
     * GET /auth/reset-password/validate?token=xxx
     */
    @Operation(summary = "Xác thực token đặt lại mật khẩu", description = "Xác thực token đặt lại mật khẩu trước khi cho phép người dùng đặt mật khẩu mới.", security = {})
    @GetMapping(ApiPaths.Auth.RESET_PASSWORD + "/validate")
    public ApiResponse<Boolean> validateToken(@RequestParam String token) {
        boolean valid = passwordResetService.validateToken(token);
        return ApiResponse.<Boolean>builder()
                .data(valid)
                .success(true)
                .message(valid ? "Token hợp lệ" : "Token không hợp lệ hoặc đã hết hạn")
                .build();
    }

    /**
     * Bước 2: Đặt lại mật khẩu
     * POST /auth/reset-password
     * Body: { "token": "xxx", "newPassword": "...", "confirmPassword": "..." }
     */
    @Operation(summary = "Đặt lại mật khẩu", description = "Đặt lại mật khẩu người dùng bằng token hợp lệ.", security = {})
    @PostMapping(ApiPaths.Auth.RESET_PASSWORD)
    public ApiResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(request);
        return ApiResponse.<Void>builder()
                .success(true)
                .message("Đặt lại mật khẩu thành công. Vui lòng đăng nhập lại.")
                .build();
    }
}