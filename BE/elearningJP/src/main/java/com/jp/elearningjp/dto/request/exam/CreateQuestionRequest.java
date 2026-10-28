package com.jp.elearningjp.dto.request.exam;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateQuestionRequest {

    private Integer questionNumber;
    private String mondaiCode;

    @NotBlank(message = "Nội dung câu hỏi không được để trống")
    private String questionText;

    private String imageUrl;
    private String audioUrl;
    private Integer audioStartTime;
    private Integer audioEndTime;
    private String transcriptJp;
    private String transcriptVi;
    private Short starPosition;

    @Builder.Default
    private Boolean hasVisualOptions = false;

    private String explanationVi;
    private Long passageId;
    private String passageTitle;
    private String passageContentJp;
    private String passageTranslationVi;
    private String passageImageUrl;

    @Builder.Default
    private BigDecimal points = new BigDecimal("1.00");

    @Builder.Default
    private List<CreateOptionRequest> options = new ArrayList<>();

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateOptionRequest {
        private Short optionKey;
        private String optionText;
        private String optionImageUrl;

        @Builder.Default
        private Boolean isCorrect = false;
    }
}
