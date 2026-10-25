package com.jp.elearningjp.controller.exam;

import com.jp.elearningjp.dto.request.exam.SaveAnswerRequest;
import com.jp.elearningjp.dto.request.exam.SubmitExamRequest;
import com.jp.elearningjp.dto.response.exam.ExamAttemptHistoryResponse;
import com.jp.elearningjp.dto.response.exam.ExamResultResponse;
import com.jp.elearningjp.dto.response.exam.ExamReviewResponse;
import com.jp.elearningjp.entity.user.User;
import com.jp.elearningjp.service.exam.ExamAttemptService;
import com.jp.elearningjp.shared.constants.ApiPaths;
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
@RequestMapping(ApiPaths.API_V1 + ApiPaths.ExamAttempt.BASE)
@RequiredArgsConstructor
@Tag(name = "Exam Attempt", description = "APIs làm bài, lưu tạm, nộp bài, xem lại và lịch sử thi cử")
public class ExamAttemptController {

    private final ExamAttemptService examAttemptService;
    private final CurrentUserService currentUserService;

    private Long resolveCurrentUserId() {
        User currentUser = currentUserService.getCurrentUserOrNull();
        return (currentUser != null) ? currentUser.getId() : 1L;
    }

    @PutMapping(ApiPaths.ExamAttempt.SAVE_ANSWER)
    @Operation(summary = "Tự động lưu tạm câu trả lời của thí sinh (Auto-save theo từng câu)")
    public ApiResponse<Void> saveAnswer(
            @PathVariable Long id,
            @Valid @RequestBody SaveAnswerRequest request) {

        Long currentUserId = resolveCurrentUserId();
        examAttemptService.saveAnswer(id, currentUserId, request);

        return ApiResponse.<Void>builder()
                .success(true)
                .code(HttpStatus.OK.value())
                .message("Lưu câu trả lời thành công")
                .build();
    }

    @PostMapping(ApiPaths.ExamAttempt.SUBMIT)
    @Operation(summary = "Nộp bài thi và chấm điểm tự động theo nhóm & kiểm tra điểm liệt")
    public ApiResponse<ExamResultResponse> submitExam(
            @PathVariable Long id,
            @RequestBody(required = false) SubmitExamRequest request) {

        Long currentUserId = resolveCurrentUserId();
        ExamResultResponse result = examAttemptService.submitExam(id, currentUserId, request);

        return ApiResponse.<ExamResultResponse>builder()
                .success(true)
                .code(HttpStatus.OK.value())
                .message("Nộp bài thi thành công")
                .data(result)
                .build();
    }

    @GetMapping(ApiPaths.ExamAttempt.RESULT)
    @Operation(summary = "Xem phiếu điểm tổng quan của bài thi đã nộp")
    public ApiResponse<ExamResultResponse> getResult(@PathVariable Long id) {
        Long currentUserId = resolveCurrentUserId();
        ExamResultResponse result = examAttemptService.getResult(id, currentUserId);

        return ApiResponse.<ExamResultResponse>builder()
                .success(true)
                .code(HttpStatus.OK.value())
                .message("Lấy kết quả bài thi thành công")
                .data(result)
                .build();
    }

    @GetMapping(ApiPaths.ExamAttempt.REVIEW)
    @Operation(summary = "Xem lại chi tiết toàn bộ bài thi cuộn dọc kèm đáp án đúng, giải thích và nghe lại audio")
    public ApiResponse<ExamReviewResponse> getReview(@PathVariable Long id) {
        Long currentUserId = resolveCurrentUserId();
        ExamReviewResponse review = examAttemptService.getReview(id, currentUserId);

        return ApiResponse.<ExamReviewResponse>builder()
                .success(true)
                .code(HttpStatus.OK.value())
                .message("Lấy dữ liệu xem lại bài thi thành công")
                .data(review)
                .build();
    }

    @GetMapping(ApiPaths.ExamAttempt.MY_HISTORY)
    @Operation(summary = "Lấy toàn bộ lịch sử thi cử của học viên hiện tại")
    public ApiResponse<Page<ExamAttemptHistoryResponse>> getMyHistory(
            @PageableDefault(size = 10, sort = "startedAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Long currentUserId = resolveCurrentUserId();
        Page<ExamAttemptHistoryResponse> history = examAttemptService.getMyHistory(currentUserId, pageable);

        return ApiResponse.<Page<ExamAttemptHistoryResponse>>builder()
                .success(true)
                .code(HttpStatus.OK.value())
                .message("Lấy lịch sử thi cử thành công")
                .data(history)
                .build();
    }
}
