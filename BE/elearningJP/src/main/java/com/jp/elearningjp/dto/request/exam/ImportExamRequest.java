package com.jp.elearningjp.dto.request.exam;

import com.jp.elearningjp.shared.enums.ExamType;
import com.jp.elearningjp.shared.enums.JlptLevel;
import com.jp.elearningjp.shared.enums.PassageType;
import com.jp.elearningjp.shared.enums.SessionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImportExamRequest {

    @NotBlank(message = "Tiêu đề đề thi không được để trống")
    private String title;

    @NotNull(message = "Cấp độ JLPT không được để trống")
    private JlptLevel jlptLevel;

    @Builder.Default
    private ExamType examType = ExamType.JLPT_OFFICIAL_MOCK;

    @Builder.Default
    private Integer totalDurationMinutes = 140;

    private String sourceFileName;

    private Long courseId;

    @Builder.Default
    private List<ImportSectionRequest> sections = new ArrayList<>();

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ImportSectionRequest {
        @NotNull(message = "sessionType không được để trống")
        private SessionType sessionType;

        @NotBlank(message = "sectionTitle không được để trống")
        private String sectionTitle;

        private String audioUrl;
        private Integer orderIndex;
        private Integer durationMinutes;

        @Builder.Default
        private List<ImportQuestionRequest> questions = new ArrayList<>();
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ImportQuestionRequest {
        private Integer questionNumber;
        private String mondaiCode;

        @NotBlank(message = "Nội dung câu hỏi không được để trống")
        private String questionText;

        private String imageUrl;
        private Integer audioStartTime;
        private Integer audioEndTime;
        private String transcriptJp;
        private String transcriptVi;
        private Short starPosition;

        @Builder.Default
        private Boolean hasVisualOptions = false;

        private String explanationVi;
        private ImportPassageRequest passage;

        @Builder.Default
        private List<ImportOptionRequest> options = new ArrayList<>();
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ImportOptionRequest {
        private Short optionKey;
        private String optionText;
        private String optionImageUrl;

        @Builder.Default
        private Boolean isCorrect = false;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ImportPassageRequest {
        private String title;

        @Builder.Default
        private PassageType passageType = PassageType.TEXT;

        private String contentJp;
        private String imageUrl;
    }
}
