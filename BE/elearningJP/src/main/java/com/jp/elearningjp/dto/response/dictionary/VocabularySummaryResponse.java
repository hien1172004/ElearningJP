package com.jp.elearningjp.dto.response.dictionary;

import com.jp.elearningjp.shared.enums.JlptLevel;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VocabularySummaryResponse {
    Long id;
    String word;
    String hiragana;
    String romaji;
    String hanViet;
    String meaningVi;
    JlptLevel jlptLevel;
    String partOfSpeech;
    String audioUrl;
}
