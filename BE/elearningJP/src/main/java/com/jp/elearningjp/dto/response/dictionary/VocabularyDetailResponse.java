package com.jp.elearningjp.dto.response.dictionary;

import com.jp.elearningjp.shared.enums.JlptLevel;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VocabularyDetailResponse {
    Long id;
    String word;
    String hiragana;
    String romaji;
    String hanViet;
    String meaningVi;
    String meaningEn;
    JlptLevel jlptLevel;
    String partOfSpeech;
    String audioUrl;
    String exampleJp;
    String exampleVi;

    List<VocabularySenseResponse> senses;
    List<VocabularyExampleResponse> examples;
    List<KanjiSimpleResponse> kanjiComponents;
}
