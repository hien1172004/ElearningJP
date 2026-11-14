package com.jp.elearningjp.dto.request.exam;

import com.jp.elearningjp.shared.enums.ExamType;
import com.jp.elearningjp.shared.enums.JlptLevel;
import com.jp.elearningjp.shared.enums.SessionType;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateExamRequest {

    @NotBlank(message = "Tiêu đề đề thi không được để trống")
    private String title;

    private JlptLevel jlptLevel;

    private ExamType examType;

    private Integer totalDurationMinutes;

    private Integer overallPassingScore;

    private Long courseId;

    private List<SectionUpdateRequest> sections;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SectionUpdateRequest {
        private Long id;
        private SessionType sessionType;
        private String sectionTitle;
        private Integer durationMinutes;
    }
}
