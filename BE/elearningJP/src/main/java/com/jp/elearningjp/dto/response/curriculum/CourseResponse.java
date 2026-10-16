package com.jp.elearningjp.dto.response.curriculum;

import com.jp.elearningjp.shared.enums.JlptLevel;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CourseResponse {

    Long id;
    String title;
    String slug;
    JlptLevel jlptLevel;
    String description;
    String thumbnailUrl;
    boolean published;
    Integer totalLessons;
    Instant createdAt;
}
