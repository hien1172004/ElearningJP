package com.jp.elearningjp.dto.request.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LoginRequest {
    @NotBlank(message = "EMAIL_REQUIRED")
    String email;
    @NotBlank(message = "PASSWORD_REQUIRED")
    String password;
}