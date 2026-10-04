package com.jp.elearningjp.dto.response.user;

import com.jp.elearningjp.shared.enums.UserStatus;
import lombok.*;

import java.time.LocalDate;
import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private Long id;

    private String fullName;

    private String email;

    private String avatarUrl;

    private UserStatus status;

    private Set<String> roles;
}