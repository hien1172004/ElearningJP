package com.jp.elearningjp.dto.response.curriculum;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

/**
 * Ví dụ riêng cho 1 từ vựng trong bài học, do giáo viên tự thêm/chọn.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LessonVocabularyExampleResponse {

    Long id;
    Integer orderIndex;
    String exampleJp;
    String exampleVi;
    String exampleRomaji;
    String audioUrl;
    Boolean isPrimary;
}
