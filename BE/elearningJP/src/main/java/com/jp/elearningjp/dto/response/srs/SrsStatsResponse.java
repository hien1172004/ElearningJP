package com.jp.elearningjp.dto.response.srs;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SrsStatsResponse {

    /** Tổng thẻ đang hoạt động (không gồm ARCHIVED). */
    long totalItems;
    /** Thẻ cần làm hôm nay = thẻ ôn đến hạn + thẻ mới trong hạn mức. */
    long dueTodayCount;
    /** Số thẻ mới sẽ học hôm nay (đã áp hạn mức). */
    long newTodayCount;
    long newCount;
    long learningCount;
    long reviewingCount;
    long relearningCount;
    long masteredCount;
    long archivedCount;
}
