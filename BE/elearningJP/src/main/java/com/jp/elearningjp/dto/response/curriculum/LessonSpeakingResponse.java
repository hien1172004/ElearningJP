package com.jp.elearningjp.dto.response.curriculum;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.jp.elearningjp.shared.enums.JlptLevel;
import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * DTO trả về thông tin kịch bản hội thoại gắn liền với bài học,
 * kèm thông tin phiên luyện tập hiện tại của học viên (nếu có).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LessonSpeakingResponse {

    Long id;
    Long lessonId;
    Long scenarioId;
    Integer orderIndex;
    Integer minTurns;

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

    /** Session ID luyện nói hiện tại của học viên cho bài này (nếu đã bắt đầu) */
    Long activeSessionId;

    /** Số lượt thoại học viên đã nói trong session hiện tại */
    Integer userTurnsCount;

    /** Đã đạt yêu cầu hoàn thành phần nói (số lượt thoại >= minTurns) */
    Boolean completed;
}
