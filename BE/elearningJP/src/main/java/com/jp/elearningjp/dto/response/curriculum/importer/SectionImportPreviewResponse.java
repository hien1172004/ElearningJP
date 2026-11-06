package com.jp.elearningjp.dto.response.curriculum.importer;

import com.jp.elearningjp.shared.enums.LessonSectionType;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.List;

/**
 * Kết quả xem trước file import. {@code itemRows} là các dòng ở sheet chính (Kanji/TuVung/NguPhap),
 * {@code childRows} là các dòng ở sheet phụ (TuLienQuan / ViDu).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SectionImportPreviewResponse {
    LessonSectionType sectionType;
    int totalItems;
    int totalChildren;
    int warningRows;
    int errorRows;
    /** true khi không có dòng lỗi nào và có ít nhất 1 item. */
    boolean canCommit;
    List<ImportRowResult> itemRows;
    List<ImportRowResult> childRows;
}
