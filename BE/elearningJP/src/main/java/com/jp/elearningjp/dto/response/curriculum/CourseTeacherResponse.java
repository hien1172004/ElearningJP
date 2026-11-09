package com.jp.elearningjp.dto.response.curriculum;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

/** Một giáo viên được giao khóa học. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CourseTeacherResponse {

    Long teacherId;
    String fullName;
    String email;
    String avatarUrl;
    /** Thời điểm được giao. */
    Instant assignedAt;
}
