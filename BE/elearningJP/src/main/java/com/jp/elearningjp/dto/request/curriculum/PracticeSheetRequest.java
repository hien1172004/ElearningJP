package com.jp.elearningjp.dto.request.curriculum;

import com.jp.elearningjp.shared.enums.LessonSectionType;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Yêu cầu in phiếu luyện viết của 1 bài học.
 *
 * @param sectionType KANJI hoặc VOCABULARY
 * @param itemIds     id các item (LessonKanjiItem / LessonVocabularyItem) muốn in — FE gửi đủ id khi
 *                    chọn "tất cả" hoặc chọn theo nhóm; item không thuộc bài học bị bỏ qua
 */
public record PracticeSheetRequest(
        @NotNull(message = "Loại mục là bắt buộc") LessonSectionType sectionType,
        @NotEmpty(message = "Chưa chọn từ nào để in") List<Long> itemIds) {
}
