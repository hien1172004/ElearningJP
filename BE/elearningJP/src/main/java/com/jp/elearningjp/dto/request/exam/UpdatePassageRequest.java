package com.jp.elearningjp.dto.request.exam;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdatePassageRequest {
    String title;
    String contentJp;
    String translationVi;
    String imageUrl;
}
