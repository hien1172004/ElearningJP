package com.jp.elearningjp.dto.response.curriculum;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LessonQuizResponse {

    Long quizId;
    Long lessonId;
    String title;
    String description;
    Integer passingScorePercent;
    Integer totalQuestions;
    List<QuizQuestionResponse> questions;
}
