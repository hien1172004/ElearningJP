package com.jp.elearningjp.dto.response.curriculum;

import com.jp.elearningjp.shared.enums.ProgressStatus;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProgressResponse {
    Long id;
    Long userId;
    Long lessonId;
    String lessonTitle;
    Integer lessonOrderIndex;
    String lessonType;
    ProgressStatus status;
    BigDecimal bestScore;
    Instant startedAt;
    Instant lastAccessedAt;
    Instant completedAt;
    Instant createdAt;
    Instant updatedAt;
}