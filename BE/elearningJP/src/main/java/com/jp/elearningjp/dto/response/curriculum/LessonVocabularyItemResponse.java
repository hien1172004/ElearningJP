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
 * Từ vựng trong 1 section VOCABULARY của bài học.
 * Field "custom" là override của giáo viên (nếu null thì dùng giá trị từ từ điển).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LessonVocabularyItemResponse {

    Long id;
    Integer orderIndex;
    String note;

    // ===== Thông tin gốc từ từ điển =====
    Long vocabId;
    String word;               // từ gốc
    String hiragana;
    String romaji;
    String hanViet;
    String meaningVi;
    String meaningEn;
    String partOfSpeech;
    String jlptLevel;

    // ===== Override của giáo viên (nếu có) =====
    String customHiragana;
    String customRomaji;
    String customAudioUrl;
    String customMeaning;
    String imageUrl;           // ảnh riêng cho bài học này

    // ===== Field thực tế sẽ hiển thị (ưu tiên custom nếu có, fallback từ từ điển) =====
    String displayHiragana;
    String displayRomaji;
    String displayAudioUrl;
    String displayMeaning;

    // ===== Ví dụ do giáo viên tự thêm =====
    List<LessonVocabularyExampleResponse> examples;
}
