package com.jp.elearningjp.shared.enums;

/**
 * Loại section trong một bài học (Lesson).
 * Mỗi lesson có thể chứa nhiều section, mỗi section thuộc 1 trong 3 loại này.
 *
 * <p><b>Sau V20/V21:</b> Bài kiểm tra (quiz) được gắn trực tiếp với
 * {@code Lesson} qua bảng {@code lesson_quizzes} — không còn section TEST
 * trung gian. Nếu client gửi {@code sectionType: "TEST"}, request sẽ bị
 * Jackson reject vì không match enum nào trong danh sách này.</p>
 *
 * <p>Giá trị khớp ràng buộc CHECK trong database.</p>
 */
public enum LessonSectionType {
    /** Nhóm chữ Hán trong bài học */
    KANJI,

    /** Nhóm từ vựng trong bài học */
    VOCABULARY,

    /** Nhóm ngữ pháp trong bài học */
    GRAMMAR
}
