package com.jp.elearningjp.dto.response.srs;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

/**
 * Dự báo số thẻ SRS cần ôn trong hôm nay và 4 ngày tiếp theo.
 * {@code days} luôn có đúng 5 phần tử (hôm nay + 4 ngày kế).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SrsForecastResponse {

    List<SrsForecastDay> days;
}
