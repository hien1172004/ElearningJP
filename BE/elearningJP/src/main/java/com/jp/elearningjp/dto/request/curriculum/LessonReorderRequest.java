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
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LessonReorderRequest {

    @NotEmpty(message = "LESSON_REORDER_LIST_EMPTY")
    @NotNull(message = "LESSON_REORDER_LIST_REQUIRED")
    List<Long> lessonIds;

    /** Version client đang thấy của từng phần tử (id → version) — lệch thì 409 (optimistic locking). Không bắt buộc. */
    Map<Long, Long> versions;
}