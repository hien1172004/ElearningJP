package com.jp.elearningjp.shared.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Loại kiến thức của một thẻ trong sổ tay.
 * <p>Giá trị khớp ràng buộc CHECK {@code chk_notebook_words_item_type} (V59).</p>
 */
@Getter
@RequiredArgsConstructor
public enum NotebookItemType {
    VOCAB("Từ vựng"),
    KANJI("Chữ Hán"),
    GRAMMAR("Ngữ pháp");

    /** Nhãn hiển thị (cũng là giá trị part_of_speech mặc định của loại đó). */
    private final String label;
}
