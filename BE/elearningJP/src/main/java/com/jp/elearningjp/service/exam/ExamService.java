package com.jp.elearningjp.service.exam;

import com.jp.elearningjp.dto.response.exam.ExamDetailResponse;
import com.jp.elearningjp.dto.response.exam.ExamSummaryResponse;
import com.jp.elearningjp.shared.enums.ExamType;
import com.jp.elearningjp.shared.enums.JlptLevel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ExamService {
    Page<ExamSummaryResponse> getPublishedExams(JlptLevel level, ExamType examType, Pageable pageable);
    ExamDetailResponse getExamDetail(Long examId);
}
