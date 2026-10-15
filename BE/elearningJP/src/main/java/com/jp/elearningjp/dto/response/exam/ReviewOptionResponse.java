package com.jp.elearningjp.dto.response.exam;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReviewOptionResponse {
    Long id;
    Short optionKey;
    String optionText;
    String optionImageUrl;
    boolean correct;
}
