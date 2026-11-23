package com.jp.elearningjp.dto.response.exam;

import com.fasterxml.jackson.annotation.JsonProperty;
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
    // Lombok sinh getter isCurrentUser() → Jackson mặc định đặt tên "currentUser"; FE đọc "isCurrentUser".
    // Gắn tên cho cả field lẫn getter, nếu chỉ field thì JSON ra cả 2 khóa.
    @JsonProperty("isCurrentUser")
    @Getter(onMethod_ = @JsonProperty("isCurrentUser"))
    boolean isCurrentUser;
}
