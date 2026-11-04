package com.jp.elearningjp.dto.request.user;

import com.jp.elearningjp.shared.enums.UserStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

/** Admin đổi trạng thái tài khoản (ACTIVE / INACTIVE / BANNED). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AdminUpdateUserStatusRequest {

    @NotNull(message = "Trạng thái tài khoản không được để trống")
    UserStatus status;
}
