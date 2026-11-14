package com.jp.elearningjp.dto.request.curriculum;

import com.jp.elearningjp.shared.enums.LessonSectionType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * Học viên báo tiến độ trong 1 bài: số mục đã xem của 1 phần và/hoặc % cả bài.
 * Server chỉ ghi khi giá trị mới lớn hơn giá trị đang lưu.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LessonProgressUpdateRequest {

    /** Loại phần (KANJI/VOCABULARY/GRAMMAR); bỏ trống khi chỉ cập nhật % cả bài. */
    LessonSectionType sectionType;

    @Min(value = 0, message = "Số mục đã xem không được âm")
    @Max(value = 10000, message = "Số mục đã xem không hợp lệ")
    Integer viewedCount;

    @Min(value = 0, message = "Phần trăm không được âm")
    @Max(value = 100, message = "Phần trăm tối đa là 100")
    Integer lessonPercent;
}
