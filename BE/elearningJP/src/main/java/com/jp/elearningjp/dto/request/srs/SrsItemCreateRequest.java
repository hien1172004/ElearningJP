package com.jp.elearningjp.dto.request.srs;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SrsItemCreateRequest {
    Long vocabId;
    Long characterId;
    Long grammarId;
}
