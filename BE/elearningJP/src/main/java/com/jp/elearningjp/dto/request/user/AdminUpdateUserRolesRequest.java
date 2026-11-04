package com.jp.elearningjp.dto.request.user;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Set;

/**
 * Admin gán lại danh sách role cho tài khoản.
 * Role USER (học viên) luôn được giữ, nên có thể gửi tập rỗng để hạ về học viên thường.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AdminUpdateUserRolesRequest {

    @NotNull(message = "Danh sách vai trò không được để trống")
    Set<String> roles;
}
