package com.jp.elearningjp.shared.pdf;

import com.jp.elearningjp.entity.content.CharacterStroke;

import java.util.List;
import java.util.Map;

/**
 * 1 khối luyện viết (1 từ vựng hoặc 1 chữ Hán).
 *
 * @param word        từ/chữ gốc
 * @param reading     cách đọc in ở ô đầu hàng đồ chữ
 * @param strokeChars các ký tự (không trùng) có hàng thứ tự nét
 * @param wordChars   chuỗi ký tự lặp lại ở hàng đồ chữ mờ
 * @param strokes     nét theo ký tự; ký tự không có nét → chỉ in chữ mẫu + chữ mờ
 */
public record PracticeSheetBlock(String word, String reading, List<String> strokeChars, List<String> wordChars,
                                 Map<String, List<CharacterStroke>> strokes) {

    /** Tổng số hàng ô: hàng nét của từng ký tự + 1 hàng đồ chữ + 2 hàng trống. */
    public int totalRows(int strokeColsPerRow) {
        int rows = 0;
        for (String c : strokeChars) {
            List<CharacterStroke> s = strokes.get(c);
            int count = s != null ? s.size() : 0;
            rows += Math.max(1, (int) Math.ceil((double) count / strokeColsPerRow));
        }
        return rows + 1 + 2;
    }
}
