package com.jp.elearningjp.dto.request.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;


@AllArgsConstructor
@NoArgsConstructor
@Getter
@Builder
public class ResetPasswordRequest {

    @NotBlank(message = "TOKEN_REQUIRED")
    private String token;

    @NotBlank(message = "PASSWORD_REQUIRED")
    @Size(min = 8, message = "PASSWORD_TOO_SHORT")
    private String newPassword;

    @NotBlank(message = "CONFIRM_PASSWORD_REQUIRED")
    private String confirmPassword;
}