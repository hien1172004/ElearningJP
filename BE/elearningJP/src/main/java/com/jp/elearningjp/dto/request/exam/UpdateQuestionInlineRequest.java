package com.jp.elearningjp.dto.request.exam;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateQuestionInlineRequest {
    String questionText;
    String imageUrl;
    Integer audioStartTime;
    Integer audioEndTime;
    String transcriptJp;
    String transcriptVi;
    Short starPosition;
    String explanationVi;
    Short correctOptionKey;
    List<OptionInlineUpdateRequest> options;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class OptionInlineUpdateRequest {
        Long id;
        Short optionKey;
        String optionText;
        String optionImageUrl;
        Boolean isCorrect;
    }
}
