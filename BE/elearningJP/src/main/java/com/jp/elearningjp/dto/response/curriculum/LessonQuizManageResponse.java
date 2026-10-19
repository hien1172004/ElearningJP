package com.jp.elearningjp.dto.response.curriculum;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

/**
 * Biến thể của {@link LessonQuizResponse} dành cho <b>Admin/Teacher</b>.
 * Trả đầy đủ đáp án đúng cho từng câu hỏi.
 *
 * <p><b>Chỉ sử dụng trong trang quản lý</b> (xem / sửa / xoá câu hỏi).
 * Học viên vẫn dùng {@code GET /lessons/{id}/quiz} (đã ẩn đáp án).</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LessonQuizManageResponse {

    Long quizId;
    Long lessonId;
    String title;
    String description;
    Integer passingScorePercent;
    Integer totalQuestions;
    List<QuizQuestionWithAnswerResponse> questions;
}
