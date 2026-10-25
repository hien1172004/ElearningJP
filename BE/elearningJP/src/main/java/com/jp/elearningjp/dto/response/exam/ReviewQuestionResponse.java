package com.jp.elearningjp.dto.response.exam;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReviewQuestionResponse {
    Long id;
    Integer questionNumber;
    String mondaiCode;
    String mondaiNameVi;
    String instructionJp;
    String instructionVi;
    String questionText;
    String imageUrl;
    String audioUrl;
    Integer audioStartTime;
    Integer audioEndTime;
    String transcriptJp;
    String transcriptVi;
    Short starPosition;
    boolean hasVisualOptions;
    String explanationVi;
    TakingPassageResponse passage;
    List<ReviewOptionResponse> options;
    ReviewStudentAnswerResponse studentAnswer;
}
