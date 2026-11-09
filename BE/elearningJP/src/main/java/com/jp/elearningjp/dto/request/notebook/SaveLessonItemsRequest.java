package com.jp.elearningjp.dto.request.notebook;

import com.jp.elearningjp.shared.enums.LessonSectionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.util.List;

/**
 * Lưu kiến thức (Chữ Hán / Từ vựng / Ngữ pháp) của một bài học vào sổ tay.
 *
 * <p>Phạm vi lưu (ưu tiên từ trên xuống):</p>
 * <ol>
 *   <li>{@code items} khác rỗng → chỉ các mục được chọn.</li>
 *   <li>{@code sectionIds} khác rỗng → toàn bộ mục trong các nhóm đó.</li>
 *   <li>Cả hai rỗng → toàn bộ bài học.</li>
 * </ol>
 *
 * <p>Đích lưu: {@code notebookId} (sổ đã có) HOẶC {@code newNotebookName} (tạo sổ mới, tuỳ chọn {@code groupId}).</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SaveLessonItemsRequest {

    @NotNull(message = "Thiếu bài học")
    Long lessonId;

    /** Sổ tay đích (đã có). */
    Long notebookId;

    /** Tạo sổ tay mới với tên này (khi không truyền notebookId). */
    @Size(max = 100, message = "Tên sổ tay không được vượt quá 100 ký tự")
    String newNotebookName;

    /** Nhóm chứa sổ tay mới (tuỳ chọn). */
    Long groupId;

    List<Long> sectionIds;

    @Valid
    List<ItemRef> items;

    /** Tham chiếu tới 1 mục trong bài: loại nhóm + id của item trong bảng lesson_*_items. */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class ItemRef {
        @NotNull
        LessonSectionType type;
        @NotNull
        Long itemId;
    }
}
