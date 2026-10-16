package com.jp.elearningjp.dto.response.srs;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SrsStatsResponse {

    long totalItems;
    long dueTodayCount;
    long learningCount;
    long reviewingCount;
    long masteredCount;
}
