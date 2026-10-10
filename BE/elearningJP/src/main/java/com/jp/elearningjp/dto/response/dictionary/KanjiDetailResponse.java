package com.jp.elearningjp.dto.response.dictionary;

import com.jp.elearningjp.shared.enums.JlptLevel;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class KanjiDetailResponse {
    Long id;
    String character;
    Integer strokeCount;
    List<String> onyomi;
    List<String> kunyomi;
    String hanViet;
    String meaningVi;
    String meaningEn;
    String romaji;
    JlptLevel jlptLevel;
    List<String> radicals;

    List<CharacterStrokeResponse> strokes;

    // Danh sách từ vựng liên quan gom nhóm theo âm đọc (Mazii-style)
    // Map reading -> List of related words
    Map<String, List<KanjiRelatedWordResponse>> wordsByReading;
    // Toàn bộ từ liên quan phẳng
    List<KanjiRelatedWordResponse> allRelatedWords;
}
