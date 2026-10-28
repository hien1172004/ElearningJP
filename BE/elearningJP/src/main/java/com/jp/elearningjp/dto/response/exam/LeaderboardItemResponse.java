package com.jp.elearningjp.dto.response.exam;

import com.jp.elearningjp.shared.enums.JlptLevel;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LeaderboardItemResponse {
    int rank;
    Long userId;
    String fullName;
    String avatarUrl;
    Integer score;
    Integer maxScore;
    JlptLevel jlptLevel;
    String examTitle;
    Integer timeSpentSeconds;
    Instant submittedAt;
    boolean isCurrentUser;
}
