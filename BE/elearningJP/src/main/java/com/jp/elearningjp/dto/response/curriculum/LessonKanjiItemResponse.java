package com.jp.elearningjp.dto.response.curriculum;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.List;

/**
 * Chữ Hán trong 1 section KANJI của bài học.
 * Kèm theo từ vựng liên quan do giáo viên tự chọn.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LessonKanjiItemResponse {

    Long id;
    Integer orderIndex;
    String note;

    // ===== Thông tin từ từ điển (WritingCharacter) =====
    Long characterId;
    String character;          // chữ Hán
    String hanViet;
    String meaningVi;
    String meaningEn;
    Integer strokeCount;
    List<String> onyomi;
    List<String> kunyomi;
    String romaji;
    String jlptLevel;
    List<String> radicals;
    String imageUrl;           // ảnh chữ Hán từ từ điển

    // ===== Từ vựng liên quan do giáo viên tự chọn =====
    List<LessonRelatedVocabResponse> relatedVocabulary;
}
