package com.jp.elearningjp.dto.response.dictionary;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CharacterStrokeResponse {
    Integer strokeNumber;
    String svgPathData;
}
