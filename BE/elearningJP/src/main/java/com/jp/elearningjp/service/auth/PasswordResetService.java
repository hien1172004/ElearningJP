package com.jp.elearningjp.service.auth;

import com.jp.elearningjp.dto.request.auth.ResetPasswordRequest;
import com.jp.elearningjp.dto.response.user.ForgotPasswordRequest;
import com.jp.elearningjp.exception.AppException;
import com.jp.elearningjp.exception.ErrorCode;
import com.jp.elearningjp.repository.user.UserRepository;
import com.jp.elearningjp.service.redis.PasswordResetTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class PasswordResetService {

    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetTokenService tokenService;
    private final EmailService emailService;

    /**
     * Bước 1: User nhập email → tạo token → gửi link qua email
     *
     * Lưu ý: Dù email không tồn tại vẫn trả về thành công
     * → tránh lộ thông tin tài khoản nào đang tồn tại (security best practice)
     */
    public void forgotPassword(ForgotPasswordRequest request) {
        userRepo.findUserByEmail(request.getEmail()).ifPresent(user -> {
            String token = tokenService.createToken(user.getEmail());
            emailService.sendPasswordResetEmail(
                    user.getEmail(),
                    token,
                    tokenService.getExpiryMinutes()
            );
        });
        // Luôn trả về thành công dù email có tồn tại hay không
    }

    /**
     * Bước 2: User click link trong email → verify token → đổi mật khẩu
     */
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        // Kiểm tra confirm password
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new AppException(ErrorCode.PASSWORD_CONFIRM_NOT_MATCH);
        }

        // Lấy email từ token trong Redis
        String email = tokenService.getEmailByToken(request.getToken());
        if (email == null) {
            // Token không tồn tại hoặc đã hết hạn
            throw new AppException(ErrorCode.INVALID_RESET_TOKEN);
        }

        // Tìm user
        var user = userRepo.findUserByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        // Kiểm tra mật khẩu mới không được trùng mật khẩu cũ
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new AppException(ErrorCode.SAME_OLD_PASSWORD);
        }

        // Cập nhật mật khẩu
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepo.save(user);

        // Xóa token — tránh dùng lại
        tokenService.deleteToken(request.getToken(), email);

        log.info("Password reset successfully for email: {}", email);
    }

    /**
     * Bước kiểm tra token còn hợp lệ không (dùng cho FE validate trước khi hiện form)
     */
    public boolean validateToken(String token) {
        return tokenService.getEmailByToken(token) != null;
    }
}