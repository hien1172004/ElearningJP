package com.jp.elearningjp.dto.request.curriculum;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.List;

/**
 * Request body chung để bulk reorder các item trong 1 section
 * (KANJI items / VOCABULARY items / GRAMMAR items / QUIZ questions).
 *
 * <p>Client gửi danh sách ID theo thứ tự MỚI (vị trí 0 = orderIndex 1).</p>
 *
 * <p>Danh sách phải chứa đúng tất cả active item của section hiện tại.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ItemBulkReorderRequest {

    @NotEmpty(message = "Danh sách item cần reorder không được rỗng")
    @NotNull(message = "Danh sách item cần reorder là bắt buộc")
    List<Long> itemIds;
}
