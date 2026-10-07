package com.jp.elearningjp.dto.response.curriculum;

import com.jp.elearningjp.shared.enums.JlptLevel;
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
public class CourseSummaryResponse {
    Long id;
    String title;
    String slug;
    JlptLevel jlptLevel;
    String description;
    String thumbnailUrl;
    boolean published;
    Long createdByUserId;
    String createdByName;
    Long totalLessons;
    Instant createdAt;
    Instant updatedAt;
    Instant archivedAt;
}