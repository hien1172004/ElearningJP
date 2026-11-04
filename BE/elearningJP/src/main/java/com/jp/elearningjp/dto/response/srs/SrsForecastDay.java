package com.jp.elearningjp.dto.response.srs;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

/**
 * Một ngày trong dự báo ôn tập SRS.
 *
 * @param date  ngày (yyyy-MM-dd, theo múi giờ {@code Asia/Ho_Chi_Minh})
 * @param count số thẻ đến hạn trong ngày đó (ngày hôm nay bao gồm cả thẻ quá hạn)
 * @param today {@code true} nếu là ngày hôm nay
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SrsForecastDay {

    LocalDate date;
    /** Số thẻ đến hạn trong ngày (hôm nay gồm cả thẻ quá hạn + thẻ mới trong hạn mức; ngày đã qua = 0). */
    int count;
    /** Số thẻ đã ôn trong ngày (ngày đã qua và hôm nay). */
    int reviewedCount;
    boolean today;
    /** Ngày đã qua trong tuần. */
    boolean past;
}
