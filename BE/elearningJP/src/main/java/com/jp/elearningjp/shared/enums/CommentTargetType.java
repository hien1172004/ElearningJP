package com.jp.elearningjp.shared.enums;

/**
 * Loại đối tượng mà một {@code Comment} gắn vào.
 * <p>Dùng cho cả comment gốc lẫn reply (reply chỉ gắn với comment_id,
 * nhưng comment thì gắn với target qua cặp {@code (target_type, target_id)}).</p>
 *
 * <p>Bảng tương ứng:</p>
 * <ul>
 *   <li>{@link #LESSON} → {@code lessons}</li>
 *   <li>{@link #VOCABULARY} → {@code vocabulary}</li>
 *   <li>{@link #KANJI} → {@code writing_characters}</li>
 *   <li>{@link #GRAMMAR} → {@code grammar_points}</li>
 * </ul>
 */
public enum CommentTargetType {
    LESSON,
    VOCABULARY,
    KANJI,
    GRAMMAR
}
