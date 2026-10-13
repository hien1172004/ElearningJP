package com.jp.elearningjp.shared.enums;

/**
 * Loại section trong một bài học (Lesson).
 * Mỗi lesson có thể chứa nhiều section, mỗi section thuộc 1 trong 4 loại này.
 *
 * Giá trị khớp ràng buộc CHECK trong database.
 */
public enum LessonSectionType {
    /** Nhóm chữ Hán trong bài học */
    KANJI,

    /** Nhóm từ vựng trong bài học */
    VOCABULARY,

    /** Nhóm ngữ pháp trong bài học */
    GRAMMAR,

    /** Bài kiểm tra / quiz trong bài học */
    TEST
}
