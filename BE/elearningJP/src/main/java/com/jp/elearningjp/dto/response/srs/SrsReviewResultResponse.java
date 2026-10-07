package com.jp.elearningjp.dto.response.srs;

import com.jp.elearningjp.shared.enums.SrsStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SrsReviewResultResponse {

    Long srsItemId;
    String rating;
    Integer previousInterval;
    Integer newInterval;
    BigDecimal previousEf;
    BigDecimal newEf;
    SrsStatus newStatus;
    Instant nextReviewAt;
    Integer xpEarned;
}
