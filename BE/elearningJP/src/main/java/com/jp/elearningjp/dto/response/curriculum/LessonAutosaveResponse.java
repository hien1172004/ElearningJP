package com.jp.elearningjp.dto.response.curriculum;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

/** Một bản chưa lưu của người dùng hiện tại trong bài. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LessonAutosaveResponse {

    String formKey;
    /** Giá trị form như FE đã gửi. */
    JsonNode content;
    Instant updatedAt;
}
