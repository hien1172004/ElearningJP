package com.jp.elearningjp.dto.response.curriculum;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.jp.elearningjp.shared.enums.LessonSectionType;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.List;

/**
 * Thông tin 1 section trong bài học.
 * Mỗi section thuộc 1 loại (KANJI / VOCABULARY / GRAMMAR / TEST) và chứa items tương ứng.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LessonSectionResponse {

    Long id;
    LessonSectionType sectionType;
    String title;
    Integer orderIndex;

    /** Items theo loại section. Mỗi trường có thể null tuỳ loại section. */
    List<LessonKanjiItemResponse> kanjiItems;
    List<LessonVocabularyItemResponse> vocabularyItems;
    List<LessonGrammarItemResponse> grammarItems;

    /** Quiz (chỉ section TEST mới có) */
    LessonQuizResponse quiz;
}
