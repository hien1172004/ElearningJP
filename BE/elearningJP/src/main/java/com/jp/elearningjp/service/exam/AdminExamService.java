package com.jp.elearningjp.service.exam;

import com.jp.elearningjp.dto.request.exam.UpdateQuestionInlineRequest;
import com.jp.elearningjp.dto.response.exam.AdminExamPreviewResponse;

public interface AdminExamService {
    AdminExamPreviewResponse getExamPreview(Long examId);
    void updateQuestionInline(Long examId, Long questionId, UpdateQuestionInlineRequest request);
    void publishExam(Long examId);
    void deleteExam(Long examId);
}
