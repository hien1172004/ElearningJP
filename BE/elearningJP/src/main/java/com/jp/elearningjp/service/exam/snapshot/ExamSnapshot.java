package com.jp.elearningjp.service.exam.snapshot;

import com.jp.elearningjp.dto.response.exam.ExamDetailResponse;
import com.jp.elearningjp.dto.response.exam.ExamSummaryResponse;
import com.jp.elearningjp.dto.response.exam.ReviewQuestionResponse;
import com.jp.elearningjp.dto.response.exam.TakingQuestionResponse;
import com.jp.elearningjp.shared.enums.SessionType;
import com.jp.elearningjp.shared.enums.SkillType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Nội dung JSON của {@code exam_published_versions.content}: đề thi gắn khóa đã duyệt.
 * Chứa đáp án — <b>không trả thẳng ra client</b>; khi làm bài chỉ dùng {@code taking}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamSnapshot {

    @Builder.Default
    private int schemaVersion = 1;

    private ExamSummaryResponse summary;
    private ExamDetailResponse detail;

    /** Các ca thi chưa xóa, theo thứ tự. */
    @Builder.Default
    private List<Section> sections = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Section {
        private Long id;
        private SessionType sessionType;
        private String sectionTitle;
        private String audioUrl;
        private Integer orderIndex;
        private Integer durationMinutes;
        /** Nhóm điểm của ca (dùng khi câu hỏi không xác định được nhóm theo kỹ năng). */
        private Long scoreGroupId;

        @Builder.Default
        private List<SectionQuestion> questions = new ArrayList<>();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SectionQuestion {
        private Long questionId;
        private Integer questionNumber;
        private BigDecimal points;
        private SkillType skillType;
        /** Câu hỏi khi làm bài (không có đáp án). */
        private TakingQuestionResponse taking;
        /** Câu hỏi khi xem lại (kèm đáp án); studentAnswer để trống. */
        private ReviewQuestionResponse review;
        @Builder.Default
        private Set<Long> optionIds = new HashSet<>();
        @Builder.Default
        private Set<Long> correctOptionIds = new HashSet<>();
    }
}
