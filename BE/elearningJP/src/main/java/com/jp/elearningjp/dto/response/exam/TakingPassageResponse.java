package com.jp.elearningjp.dto.response.exam;

import com.jp.elearningjp.shared.enums.PassageType;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TakingPassageResponse {
    Long id;
    String title;
    PassageType passageType;
    String contentJp;
    String imageUrl;
}
