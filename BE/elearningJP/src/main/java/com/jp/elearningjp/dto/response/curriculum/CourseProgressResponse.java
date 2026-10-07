package com.jp.elearningjp.dto.response.curriculum;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CourseProgressResponse {

    Long courseId;
    String courseTitle;
    Integer totalLessons;
    Integer completedLessons;
    BigDecimal progressPercent;
    boolean isCompleted;
}
