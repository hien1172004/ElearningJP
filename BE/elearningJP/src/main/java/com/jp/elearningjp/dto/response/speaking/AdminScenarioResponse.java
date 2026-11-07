package com.jp.elearningjp.dto.response.speaking;

import com.fasterxml.jackson.databind.JsonNode;
import com.jp.elearningjp.shared.enums.JlptLevel;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

/** Kịch bản luyện nói cho trang quản lý (kèm thông tin người tạo). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AdminScenarioResponse {
    Long id;
    String topicId;
    JlptLevel jlptLevel;
    String title;
    String description;
    String userRole;
    String aiRole;
    JsonNode tasks;
    JsonNode sampleDialogue;
    JsonNode vocabulary;
    JsonNode grammar;
    JsonNode tips;
    Boolean isPremium;
    Boolean isLessonOnly;
    String thumbnailUrl;
    /** null = kịch bản hệ thống hoặc do khách chưa đăng nhập tạo. */
    Long createdById;
    String createdByName;
    String createdByEmail;
    Instant createdAt;
    Instant updatedAt;
}
