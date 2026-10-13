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
 * Mỗi section thuộc 1 loại (KANJI / VOCABULARY / GRAMMAR) và chứa items tương ứng.
 *
 * <p><b>Refactor V20/V21:</b> Section TEST đã bị loại bỏ — Quiz được
 * liên kết trực tiếp với Lesson. Lấy quiz qua endpoint {@code GET /lessons/{id}/quiz}.</p>
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
}
