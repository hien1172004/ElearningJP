package com.jp.elearningjp.service.exam.impl;

import com.jp.elearningjp.dto.response.exam.*;
import com.jp.elearningjp.entity.exam.Exam;
import com.jp.elearningjp.entity.exam.ExamSection;
import com.jp.elearningjp.entity.exam.Question;
import com.jp.elearningjp.entity.exam.QuestionOption;
import com.jp.elearningjp.entity.exam.StudentAnswer;
import com.jp.elearningjp.mapper.ExamMapper;
import com.jp.elearningjp.repository.exam.ExamSectionQuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.stream.Collectors;

/**
 * Dựng DTO đề thi cho học viên (chi tiết đề, câu hỏi khi làm bài / xem lại).
 * Dùng chung cho đề trực tiếp và snapshot đề gắn khóa ({@link com.jp.elearningjp.service.exam.snapshot.ExamSnapshotService}).
 */
@Component
@RequiredArgsConstructor
public class ExamViewBuilder {

    private final ExamMapper examMapper;
    private final ExamSectionQuestionRepository examSectionQuestionRepository;

    /** Chi tiết đề (nhóm điểm + tóm tắt từng ca thi). Exam cần nạp sẵn sections + scoreGroups. */
    public ExamDetailResponse buildExamDetail(Exam exam) {
        var scoreGroupResponses = exam.getScoreGroups().stream()
                .map(examMapper::toScoreGroupResponse)
                .collect(Collectors.toList());

        var sectionResponses = exam.getSections().stream()
                .filter(s -> !s.isDeleted())
                .collect(Collectors.toMap(
                        ExamSection::getId,
                        s -> s,
                        (existing, replacement) -> existing,
                        java.util.LinkedHashMap::new
                ))
                .values().stream()
                .map(s -> {
                    int questionCount = examSectionQuestionRepository
                            .findBySectionIdAndDeletedFalseOrderByQuestionNumberAsc(s.getId()).size();
                    String title = formatSectionTitle(s);
                    ExamSectionSummaryResponse resp = examMapper.toSectionSummaryResponse(s);
                    resp.setSectionTitle(title);
                    resp.setQuestionCount(questionCount);
                    return resp;
                })
                .collect(Collectors.toList());

        ExamDetailResponse detail = examMapper.toDetailResponse(exam);
        detail.setScoreGroups(scoreGroupResponses);
        detail.setSections(sectionResponses);
        return detail;
    }

    private String formatSectionTitle(ExamSection s) {
        String viDesc = switch (s.getSessionType()) {
            case VOCABULARY -> "Kiến thức ngôn ngữ (Từ vựng)";
            case GRAMMAR_READING -> "Ngữ pháp & Đọc hiểu";
            case LISTENING -> "Nghe hiểu";
            case KNOWLEDGE_READING -> "Kiến thức ngôn ngữ & Đọc hiểu";
        };
        if (s.getSectionTitle() != null && s.getSectionTitle().contains(viDesc)) {
            return s.getSectionTitle();
        }
        return "Ca " + s.getOrderIndex() + ": " + viDesc + " - " + s.getSectionTitle();
    }

    /** Bài làm của học viên cho 1 câu (null nếu chưa trả lời). */
    public ReviewStudentAnswerResponse toStudentAnswerResponse(StudentAnswer ans) {
        if (ans == null) {
            return null;
        }
        return ReviewStudentAnswerResponse.builder()
                .selectedOptionId(ans.getSelectedOption() != null ? ans.getSelectedOption().getId() : null)
                .orderedOptionIds(ans.getOrderedOptionIds())
                .correct(ans.isCorrect())
                .pointsEarned(ans.getPointsEarned())
                .audioPlayCount(ans.getAudioPlayCount())
                .build();
    }

    /** Câu hỏi khi làm bài (không có đáp án). */
    public TakingQuestionResponse toTakingQuestionResponse(Question q, Integer questionNumber) {
        var options = q.getOptions().stream()
                .filter(o -> !o.isDeleted())
                .sorted(Comparator.comparing(QuestionOption::getOptionKey))
                .map(examMapper::toTakingOptionResponse)
                .collect(Collectors.toList());

        TakingPassageResponse passage = (q.getPassage() != null && !q.getPassage().isDeleted())
                ? examMapper.toTakingPassageResponse(q.getPassage()) : null;

        TakingQuestionResponse resp = examMapper.toTakingQuestionResponse(q);
        resp.setQuestionNumber(questionNumber);
        resp.setOptions(options);
        resp.setPassage(passage);

        String mCode = q.getMondaiType() != null ? q.getMondaiType().getCode() : "";
        boolean isKeyPointOrQuick = "KEY_POINT".equals(mCode) || "QUICK_RESPONSE".equals(mCode);
        boolean isTextOnly = false;
        if (!"UTTERANCE_EXPRESSIONS".equals(mCode) && (q.getImageUrl() == null || q.getImageUrl().isBlank()) && options != null) {
            long meaningful = options.stream()
                    .filter(o -> {
                        String t = o.getOptionText() != null ? o.getOptionText().trim() : "";
                        if (t.length() <= 1) return false;
                        if (t.startsWith("Lựa chọn") || t.startsWith("Đáp án") ||
                            t.startsWith("Option") || t.startsWith("Phương án")) return false;
                        if (t.matches("^[ア-ンA-Za-z0-9\\s・、,/\\-()（）①-④]+$")) return false;
                        return true;
                    })
                    .count();
            if (meaningful >= 2) isTextOnly = true;
        }
        if (isKeyPointOrQuick || isTextOnly) {
            resp.setImageUrl(null);
            resp.setHasVisualOptions(false);
            if (options != null) {
                for (var opt : options) opt.setOptionImageUrl(null);
            }
        }
        return resp;
    }

    /** Câu hỏi khi xem lại (kèm đáp án, giải thích); {@code ans} null = chưa ghép bài làm. */
    public ReviewQuestionResponse toReviewQuestionResponse(Question q, Integer questionNumber, StudentAnswer ans) {
        var options = q.getOptions().stream()
                .filter(o -> !o.isDeleted())
                .sorted(Comparator.comparing(QuestionOption::getOptionKey))
                .map(examMapper::toReviewOptionResponse)
                .collect(Collectors.toList());

        TakingPassageResponse passage = (q.getPassage() != null && !q.getPassage().isDeleted())
                ? examMapper.toTakingPassageResponse(q.getPassage()) : null;

        ReviewStudentAnswerResponse studentAnsResp = toStudentAnswerResponse(ans);

        ReviewQuestionResponse resp = examMapper.toReviewQuestionResponse(q);
        resp.setQuestionNumber(questionNumber);
        resp.setOptions(options);
        resp.setPassage(passage);
        resp.setStudentAnswer(studentAnsResp);

        String mCode = q.getMondaiType() != null ? q.getMondaiType().getCode() : "";
        boolean isKeyPointOrQuick = "KEY_POINT".equals(mCode) || "QUICK_RESPONSE".equals(mCode);
        boolean hasAnyOptionImage = options != null && options.stream().anyMatch(o -> o.getOptionImageUrl() != null && !o.getOptionImageUrl().isBlank());
        boolean isTextOnly = false;
        if (!q.isHasVisualOptions() && !hasAnyOptionImage && !"UTTERANCE_EXPRESSIONS".equals(mCode) && (q.getImageUrl() == null || q.getImageUrl().isBlank()) && options != null) {
            long meaningful = options.stream()
                    .filter(o -> {
                        String t = o.getOptionText() != null ? o.getOptionText().trim() : "";
                        if (t.length() <= 1) return false;
                        if (t.startsWith("Lựa chọn") || t.startsWith("Đáp án") ||
                            t.startsWith("Option") || t.startsWith("Phương án") ||
                            t.startsWith("選択肢")) return false;
                        if (t.matches("^[ア-ンA-Za-z0-9\\s・、,/\\-()（）①-④]+$")) return false;
                        return true;
                    })
                    .count();
            if (meaningful >= 2) isTextOnly = true;
        }
        if (isKeyPointOrQuick || isTextOnly) {
            resp.setImageUrl(null);
            resp.setHasVisualOptions(false);
            if (options != null) {
                for (var opt : options) opt.setOptionImageUrl(null);
            }
        }
        return resp;
    }
}
