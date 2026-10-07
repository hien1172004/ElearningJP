package com.jp.elearningjp.dto.response.curriculum;

import com.jp.elearningjp.shared.enums.EnrollmentStatus;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EnrollmentResponse {
    Long id;
    Long userId;
    String userEmail;
    String userFullName;
    Long courseId;
    String courseTitle;
    String courseSlug;
    String courseJlptLevel;
    EnrollmentStatus status;
    Integer progressPercent;
    Instant enrolledAt;
    Instant completedAt;
    Instant createdAt;
    Instant updatedAt;
}