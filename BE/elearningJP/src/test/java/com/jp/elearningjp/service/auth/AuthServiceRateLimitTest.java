package com.jp.elearningjp.service.auth;

import com.jp.elearningjp.dto.request.auth.LoginRequest;
import com.jp.elearningjp.dto.response.user.ForgotPasswordRequest;
import com.jp.elearningjp.entity.user.User;
import com.jp.elearningjp.exception.AppException;
import com.jp.elearningjp.exception.ErrorCode;
import com.jp.elearningjp.repository.user.UserRepository;
import com.jp.elearningjp.service.redis.PasswordResetTokenService;
import com.jp.elearningjp.service.redis.RateLimitService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Luồng rate limit của đăng nhập và quên mật khẩu. */
@ExtendWith(MockitoExtension.class)
class AuthServiceRateLimitTest {

    private static final String IP = "1.2.3.4";
    private static final String FAIL_KEY = "login:fail:a@gmail.com";

    @Mock UserRepository userRepo;
    @Mock PasswordEncoder passwordEncoder;
    @Mock RateLimitService rateLimitService;
    @Mock PasswordResetTokenService tokenService;
    @Mock EmailService emailService;

    @InjectMocks AuthService authService;
    @InjectMocks PasswordResetService passwordResetService;

    private LoginRequest login(String email, String password) {
        LoginRequest r = new LoginRequest();
        r.setEmail(email);
        r.setPassword(password);
        return r;
    }

    private ErrorCode codeOf(Throwable e) {
        return ((AppException) e).getErrorCode();
    }

    @Test
    void lockedEmail_rejectedEvenWithCorrectPassword_withoutTouchingDb() {
        when(rateLimitService.isOverLimit(FAIL_KEY, AuthService.LOGIN_MAX_FAILS)).thenReturn(true);

        assertThatThrownBy(() -> authService.loginUser(login(" A@gmail.com ", "right"), IP))
                .satisfies(e -> assertThat(codeOf(e)).isEqualTo(ErrorCode.TOO_MANY_REQUESTS));
        verifyNoInteractions(userRepo, passwordEncoder);
    }

    @Test
    void unknownEmail_countsAsFailure() {
        when(userRepo.findUserByEmail("a@gmail.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.loginUser(login("a@gmail.com", "x"), IP))
                .satisfies(e -> assertThat(codeOf(e)).isEqualTo(ErrorCode.USER_NOT_FOUND));
        verify(rateLimitService).hit(FAIL_KEY, AuthService.LOGIN_LOCK_DURATION);
    }

    @Test
    void wrongPassword_countsAsFailure() {
        User user = User.builder().email("a@gmail.com").password("hash").build();
        when(userRepo.findUserByEmail("a@gmail.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hash")).thenReturn(false);

        assertThatThrownBy(() -> authService.loginUser(login("a@gmail.com", "wrong"), IP))
                .satisfies(e -> assertThat(codeOf(e)).isEqualTo(ErrorCode.PASSWORD_INVALID));
        verify(rateLimitService).hit(FAIL_KEY, AuthService.LOGIN_LOCK_DURATION);
        verify(rateLimitService, never()).reset(anyString());
    }

    @Test
    void correctPassword_resetsFailCounter() {
        ReflectionTestUtils.setField(authService, "signerKey", "test-signer-key");
        ReflectionTestUtils.setField(authService, "validDuration", 3600L);
        User user = User.builder().email("a@gmail.com").password("hash").roles(Set.of()).build();
        user.setId(7L);
        when(userRepo.findUserByEmail("a@gmail.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("right", "hash")).thenReturn(true);

        assertThat(authService.loginUser(login("a@gmail.com", "right"), IP).getAccessToken()).isNotBlank();
        verify(rateLimitService).check(eq("login:ip:" + IP), anyInt(), any());
        verify(rateLimitService).reset(FAIL_KEY);
        verify(rateLimitService, never()).hit(anyString(), any());
    }

    @Test
    void forgotPassword_overEmailLimit_silentlySkipsMail() {
        ForgotPasswordRequest req = ForgotPasswordRequest.builder().email("A@gmail.com").build();
        when(rateLimitService.tryAcquire(eq("forgot:a@gmail.com"), anyInt(), any())).thenReturn(false);

        passwordResetService.forgotPassword(req, IP); // không ném lỗi → FE vẫn thấy thành công

        verifyNoInteractions(userRepo, emailService);
    }
}
