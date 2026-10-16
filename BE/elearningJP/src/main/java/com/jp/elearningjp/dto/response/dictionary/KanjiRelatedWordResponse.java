package com.jp.elearningjp.dto.response.dictionary;

import com.jp.elearningjp.shared.enums.JlptLevel;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class KanjiRelatedWordResponse {
    Long vocabId;
    String word;
    String hiragana;
    String meaningVi;
    String hanViet;
    JlptLevel jlptLevel;
    String readingType;
    String reading;
}
