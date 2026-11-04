package com.jp.elearningjp.dto.response.user;

import com.jp.elearningjp.shared.enums.AuthProvider;
import com.jp.elearningjp.shared.enums.UserStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;
import java.util.Set;

/** Thông tin tài khoản hiển thị ở trang quản lý người dùng (không chứa mật khẩu/providerId). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AdminUserResponse {
    Long id;
    String email;
    String fullName;
    String avatarUrl;
    AuthProvider authProvider;
    UserStatus status;
    Set<String> roles;
    Instant emailVerifiedAt;
    Instant createdAt;
    Instant updatedAt;
}
