package com.jp.elearningjp.dto.request.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RegisterRequest {
    @NotBlank(message = "PASSWORD_REQUIRED")
    @Size(min = 8, message = "PASSWORD_TOO_SHORT")
    String password;
    @NotBlank(message = "CONFIRM_PASSWORD_REQUIRED")
    String confirmPassword;
    @NotBlank(message = "FULL_NAME_REQUIRED")
    String fullName;
    @Email(message = "EMAIL_INVALID")
    @NotBlank(message = "EMAIL_REQUIRED")
    String email;
    @NotBlank(message = "PHONE_REQUIRED")
    String phone;
}