package com.jp.elearningjp.dto.response.exam;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LeaderboardResponse {
    List<LeaderboardItemResponse> rankings;
    LeaderboardItemResponse myRank;
    long totalParticipants;
}
