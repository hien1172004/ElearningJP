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
 * Từ vựng liên quan tới 1 chữ Hán (do giáo viên tự chọn trong bài học).
 * Khác với {@code vocabulary_characters} (đó là quan hệ tự động trong từ điển).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LessonRelatedVocabResponse {

    Long id;
    String note;

    // ===== Thông tin từ vựng từ từ điển =====
    Long vocabId;
    String word;
    String hiragana;
    String meaningVi;
}
