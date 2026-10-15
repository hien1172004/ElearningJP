package com.jp.elearningjp.dto.response.exam;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TakingQuestionResponse {
    Long id;
    Integer questionNumber;
    String mondaiCode;
    String mondaiNameVi;
    String instructionJp;
    String instructionVi;
    String questionText;
    String imageUrl;
    Integer audioStartTime;
    Integer audioEndTime;
    Short starPosition;
    boolean hasVisualOptions;
    TakingPassageResponse passage;
    List<TakingOptionResponse> options;
}
