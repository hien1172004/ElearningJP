package com.jp.elearningjp.service.exam;

import com.jp.elearningjp.dto.request.exam.SaveAnswerRequest;
import com.jp.elearningjp.dto.request.exam.StartExamRequest;
import com.jp.elearningjp.dto.request.exam.SubmitExamRequest;
import com.jp.elearningjp.dto.response.exam.ExamAttemptHistoryResponse;
import com.jp.elearningjp.dto.response.exam.ExamResultResponse;
import com.jp.elearningjp.dto.response.exam.ExamReviewResponse;
import com.jp.elearningjp.dto.response.exam.ExamTakingResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ExamAttemptService {
    ExamTakingResponse startExam(Long examId, Long userId, StartExamRequest request);
    void saveAnswer(Long attemptId, Long userId, SaveAnswerRequest request);
    ExamResultResponse submitExam(Long attemptId, Long userId, SubmitExamRequest request);
    ExamResultResponse getResult(Long attemptId, Long userId);
    ExamReviewResponse getReview(Long attemptId, Long userId);
    Page<ExamAttemptHistoryResponse> getMyHistory(Long userId, Pageable pageable);
}
