package com.jp.elearningjp.service.exam;

import com.jp.elearningjp.dto.request.exam.CreateQuestionRequest;
import com.jp.elearningjp.dto.request.exam.ImportExamRequest;
import com.jp.elearningjp.dto.request.exam.UpdateQuestionInlineRequest;
import com.jp.elearningjp.dto.response.exam.AdminExamPreviewResponse;
import com.jp.elearningjp.dto.response.exam.ExamSummaryResponse;

import java.util.List;

public interface AdminExamService {
    Long importExam(ImportExamRequest request, Long currentUserId);
    AdminExamPreviewResponse getExamPreview(Long examId);
    void updateQuestionInline(Long examId, Long questionId, UpdateQuestionInlineRequest request);
    Long addQuestion(Long examId, Long sectionId, CreateQuestionRequest request, Long currentUserId);
    void deleteQuestion(Long examId, Long questionId);
    void clearSectionQuestions(Long examId, Long sectionId);
    List<ExamSummaryResponse> getCourseExams(Long courseId);
    void publishExam(Long examId);
    void unpublishExam(Long examId);
    org.springframework.data.domain.Page<ExamSummaryResponse> getAllExams(
            com.jp.elearningjp.shared.enums.JlptLevel level,
            com.jp.elearningjp.shared.enums.ExamType examType,
            Boolean published,
            org.springframework.data.domain.Pageable pageable);
    void deleteExam(Long examId);
    void updateSectionAudio(Long examId, Long sectionId, String audioUrl);
    void updatePassage(Long examId, Long passageId, com.jp.elearningjp.dto.request.exam.UpdatePassageRequest request);
    void deletePassage(Long examId, Long passageId);
    void saveSnapshot(Long examId, AdminExamPreviewResponse snapshot);
    AdminExamPreviewResponse getSnapshot(Long examId);
    AdminExamPreviewResponse rollbackExam(Long examId, AdminExamPreviewResponse snapshot);
    AdminExamPreviewResponse reorderExamQuestions(Long examId);
    Long cloneExam(Long examId, Long currentUserId);
    int regradeExam(Long examId);
}
