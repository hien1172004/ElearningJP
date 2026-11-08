package com.jp.elearningjp.dto.response.curriculum;

import java.util.List;

/**
 * Kết quả kiểm tra dữ liệu nét trước khi in phiếu luyện viết.
 *
 * @param totalItems   số mục sẽ được in
 * @param missingItems các mục có ký tự chưa có dữ liệu thứ tự nét (in vẫn được, ký tự đó chỉ có chữ mẫu + chữ mờ)
 */
public record PracticeSheetCheckResponse(int totalItems, List<MissingStrokeItem> missingItems) {

    /**
     * @param itemId       id item trong bài
     * @param groupTitle   tên nhóm (section) chứa item
     * @param word         từ / chữ Hán
     * @param missingChars các ký tự thiếu nét
     */
    public record MissingStrokeItem(Long itemId, String groupTitle, String word, List<String> missingChars) {
    }
}
