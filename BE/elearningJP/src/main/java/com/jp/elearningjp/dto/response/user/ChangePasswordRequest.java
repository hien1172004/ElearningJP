package com.jp.elearningjp.dto.response.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ChangePasswordRequest {

    @NotBlank(message = "OLD_PASSWORD_REQUIRED")
    String oldPassword;

    @NotBlank(message = "NEW_PASSWORD_REQUIRED")
    @Size(min = 8, message = "PASSWORD_TOO_SHORT")
    String newPassword;

    @NotBlank(message = "CONFIRM_PASSWORD_REQUIRED")
    String confirmPassword;
}