package com.jp.elearningjp.dto.response.dictionary;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VocabularySenseResponse {
    Long id;
    Short senseNo;
    String meaningVi;
    String meaningEn;
    String partOfSpeech;
    String usageNotes;
}
