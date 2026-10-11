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
 * Điểm ngữ pháp trong 1 section GRAMMAR của bài học.
 * Field "custom" là override của giáo viên (nếu null thì dùng giá trị từ từ điển).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LessonGrammarItemResponse {

    Long id;
    Integer orderIndex;
    String note;

    // ===== Thông tin gốc từ từ điển =====
    Long grammarId;
    String title;              // tên ngữ pháp
    String structure;          // cấu trúc
    String meaningVi;
    String usageNotes;
    String jlptLevel;

    // ===== Override của giáo viên (nếu có) =====
    String customMeaning;
    String customStructure;

    // ===== Field thực tế sẽ hiển thị (ưu tiên custom nếu có) =====
    String displayMeaning;
    String displayStructure;

    // ===== Ví dụ do giáo viên tự thêm =====
    List<LessonGrammarExampleResponse> examples;
}
