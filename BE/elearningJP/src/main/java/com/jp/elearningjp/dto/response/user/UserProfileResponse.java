package com.jp.elearningjp.dto.response.user;

import com.jp.elearningjp.shared.enums.JlptLevel;
import com.jp.elearningjp.shared.enums.LearnerLevel;
import com.jp.elearningjp.shared.enums.UserStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserProfileResponse {

    Long id;
    String email;
    String fullName;
    String avatarUrl;
    Set<String> roles;
    UserStatus status;

    String phoneNumber;
    String bio;
    LearnerLevel currentLevel;
    JlptLevel targetLevel;
    LocalDate targetExamDate;
    Integer dailyStudyTimeMinutes;
    String timezone;

    Instant createdAt;
    Instant updatedAt;
}
