package com.jp.elearningjp.controller.exam;

import com.jp.elearningjp.dto.request.exam.StartExamRequest;
import com.jp.elearningjp.dto.response.exam.ExamDetailResponse;
import com.jp.elearningjp.dto.response.exam.ExamSummaryResponse;
import com.jp.elearningjp.dto.response.exam.ExamTakingResponse;
import com.jp.elearningjp.entity.user.User;
import com.jp.elearningjp.service.exam.ExamAttemptService;
import com.jp.elearningjp.service.exam.ExamService;
import com.jp.elearningjp.shared.constants.ApiPaths;
import com.jp.elearningjp.shared.enums.ExamType;
import com.jp.elearningjp.shared.enums.JlptLevel;
import com.jp.elearningjp.shared.response.ApiResponse;
import com.jp.elearningjp.shared.util.CurrentUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(ApiPaths.API_V1 + ApiPaths.Exam.BASE)
@RequiredArgsConstructor
@Tag(name = "Exam", description = "APIs quản lý và thi thử JLPT")
public class ExamController {

    private final ExamService examService;
    private final ExamAttemptService examAttemptService;
    private final CurrentUserService currentUserService;

    @GetMapping
    @Operation(summary = "Lấy danh sách đề thi đã xuất bản (hỗ trợ lọc theo cấp độ JLPT và loại đề thi)")
    public ApiResponse<Page<ExamSummaryResponse>> getPublishedExams(
            @RequestParam(required = false) JlptLevel level,
            @RequestParam(required = false) ExamType examType,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<ExamSummaryResponse> exams = examService.getPublishedExams(level, examType, pageable);
        return ApiResponse.<Page<ExamSummaryResponse>>builder()
                .success(true)
                .code(HttpStatus.OK.value())
                .message("Lấy danh sách đề thi thành công")
                .data(exams)
                .build();
    }

    @GetMapping(ApiPaths.Exam.BY_ID)
    @Operation(summary = "Xem thông tin chi tiết cấu trúc đề thi (các phần thi và nhóm điểm)")
    public ApiResponse<ExamDetailResponse> getExamDetail(@PathVariable Long id) {
        ExamDetailResponse detail = examService.getExamDetail(id);
        return ApiResponse.<ExamDetailResponse>builder()
                .success(true)
                .code(HttpStatus.OK.value())
                .message("Lấy chi tiết đề thi thành công")
                .data(detail)
                .build();
    }

    @PostMapping(ApiPaths.Exam.START)
    @Operation(summary = "Bắt đầu làm bài thi (hỗ trợ thi cả bài hoặc luyện riêng từng phần)")
    public ApiResponse<ExamTakingResponse> startExam(
            @PathVariable Long id,
            @Valid @RequestBody StartExamRequest request) {

        User currentUser = currentUserService.getCurrentUserOrNull();
        Long currentUserId = (currentUser != null) ? currentUser.getId() : 1L;
        ExamTakingResponse takingResponse = examAttemptService.startExam(id, currentUserId, request);

        return ApiResponse.<ExamTakingResponse>builder()
                .success(true)
                .code(HttpStatus.CREATED.value())
                .message("Bắt đầu bài thi thành công")
                .data(takingResponse)
                .build();
    }
}
