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
    String audioUrl;
    Integer audioStartTime;
    Integer audioEndTime;
    String transcriptJp;
    String transcriptVi;
    Short starPosition;
    Boolean hasVisualOptions;
    String mondaiCode;
    String explanationVi;
    Short correctOptionKey;
    String passageTitle;
    String passageContentJp;
    String passageTranslationVi;
    String passageImageUrl;
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
        Boolean correct;

        public Boolean getIsCorrect() {
            return isCorrect != null ? isCorrect : correct;
        }
    }
}
