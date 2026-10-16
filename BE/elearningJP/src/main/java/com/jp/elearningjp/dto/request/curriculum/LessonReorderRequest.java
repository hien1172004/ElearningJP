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

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LessonReorderRequest {

    @NotEmpty(message = "LESSON_REORDER_LIST_EMPTY")
    @NotNull(message = "LESSON_REORDER_LIST_REQUIRED")
    List<Long> lessonIds;
}