package com.jp.elearningjp.dto.request.curriculum;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

/** Giá trị form đang soạn dở (JSON tùy form) để tự lưu. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LessonAutosaveRequest {

    @NotNull(message = "Thiếu nội dung tự lưu")
    JsonNode content;
}
