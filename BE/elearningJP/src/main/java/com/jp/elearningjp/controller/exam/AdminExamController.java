package com.jp.elearningjp.controller.exam;

import com.jp.elearningjp.dto.request.exam.CreateQuestionRequest;
import com.jp.elearningjp.dto.request.exam.ImportExamRequest;
import com.jp.elearningjp.dto.request.exam.UpdateQuestionInlineRequest;
import com.jp.elearningjp.dto.response.exam.AdminExamPreviewResponse;
import com.jp.elearningjp.dto.response.exam.ExamSummaryResponse;
import java.util.List;
import com.jp.elearningjp.entity.user.User;
import com.jp.elearningjp.service.exam.AdminExamService;
import com.jp.elearningjp.shared.constants.ApiPaths;
import com.jp.elearningjp.shared.response.ApiResponse;
import com.jp.elearningjp.shared.util.CurrentUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.domain.Sort;
import com.jp.elearningjp.shared.enums.JlptLevel;
import com.jp.elearningjp.shared.enums.ExamType;

@RestController
@RequestMapping(ApiPaths.API_V1 + ApiPaths.AdminExam.BASE)
@RequiredArgsConstructor
@Tag(name = "Admin Exam", description = "APIs dành cho Quản trị viên/Giáo viên quản lý, xem trước và chỉnh sửa đề thi")
public class AdminExamController {

    private final AdminExamService adminExamService;
    private final CurrentUserService currentUserService;

    @PostMapping
    @Operation(summary = "Tạo đề thi mới từ cấu trúc trích xuất AI hoặc thủ công (ở trạng thái DRAFT)")
    public ApiResponse<Long> importExam(@Valid @RequestBody ImportExamRequest request) {
        User currentUser = currentUserService.getCurrentUserOrNull();
        Long currentUserId = (currentUser != null) ? currentUser.getId() : 1L;
        Long examId = adminExamService.importExam(request, currentUserId);
        return ApiResponse.<Long>builder()
                .success(true)
                .code(HttpStatus.CREATED.value())
                .message("Tạo đề thi thành công")
                .data(examId)
                .build();
    }

    @GetMapping(ApiPaths.AdminExam.PREVIEW)
    @Operation(summary = "Xem trước toàn bộ đề thi dạng cuộn dọc (kèm đáp án đúng, mốc audio và transcript để rà soát)")
    public ApiResponse<AdminExamPreviewResponse> getExamPreview(@PathVariable Long id) {
        AdminExamPreviewResponse preview = adminExamService.getExamPreview(id);
        return ApiResponse.<AdminExamPreviewResponse>builder()
                .success(true)
                .code(HttpStatus.OK.value())
                .message("Lấy dữ liệu xem trước đề thi thành công")
                .data(preview)
                .build();
    }

    @PutMapping(ApiPaths.AdminExam.UPDATE_QUESTION)
    @Operation(summary = "Chỉnh sửa trực tiếp tại chỗ (Inline Edit): text câu hỏi, mốc giây audio, transcript, vị trí dấu sao ★, đáp án đúng")
    public ApiResponse<Void> updateQuestionInline(
            @PathVariable Long id,
            @PathVariable Long questionId,
            @Valid @RequestBody UpdateQuestionInlineRequest request) {

        adminExamService.updateQuestionInline(id, questionId, request);
        return ApiResponse.<Void>builder()
                .success(true)
                .code(HttpStatus.OK.value())
                .message("Cập nhật câu hỏi thành công")
                .build();
    }

    @GetMapping
    @Operation(summary = "Lấy danh sách tất cả đề thi cho Quản trị viên (bao gồm bản nháp DRAFT và đã xuất bản PUBLISHED)")
    public ApiResponse<Page<ExamSummaryResponse>> getAllExams(
            @RequestParam(required = false) JlptLevel level,
            @RequestParam(required = false) ExamType examType,
            @RequestParam(required = false) Boolean published,
            @PageableDefault(size = 50, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<ExamSummaryResponse> exams = adminExamService.getAllExams(level, examType, published, pageable);
        return ApiResponse.<Page<ExamSummaryResponse>>builder()
                .success(true)
                .code(HttpStatus.OK.value())
                .message("Lấy danh sách đề thi thành công")
                .data(exams)
                .build();
    }

    @PostMapping(ApiPaths.AdminExam.PUBLISH)
    @Operation(summary = "Phát hành đề thi chính thức cho học viên thi thử")
    public ApiResponse<Void> publishExam(@PathVariable Long id) {
        adminExamService.publishExam(id);
        return ApiResponse.<Void>builder()
                .success(true)
                .code(HttpStatus.OK.value())
                .message("Phát hành đề thi thành công")
                .build();
    }

    @PostMapping("/{id}/unpublish")
    @Operation(summary = "Thu hồi đề thi về trạng thái bản nháp (DRAFT)")
    public ApiResponse<Void> unpublishExam(@PathVariable Long id) {
        adminExamService.unpublishExam(id);
        return ApiResponse.<Void>builder()
                .success(true)
                .code(HttpStatus.OK.value())
                .message("Thu hồi đề thi về bản nháp thành công")
                .build();
    }

    @PostMapping(ApiPaths.AdminExam.ADD_QUESTION)
    @Operation(summary = "Giáo viên thêm câu hỏi mới thủ công vào một phần thi của đề thi")
    public ApiResponse<Long> addQuestion(
            @PathVariable Long id,
            @PathVariable Long sectionId,
            @Valid @RequestBody CreateQuestionRequest request) {

        User currentUser = currentUserService.getCurrentUserOrNull();
        Long currentUserId = (currentUser != null) ? currentUser.getId() : 1L;
        Long questionId = adminExamService.addQuestion(id, sectionId, request, currentUserId);

        return ApiResponse.<Long>builder()
                .success(true)
                .code(HttpStatus.CREATED.value())
                .message("Thêm câu hỏi mới thành công")
                .data(questionId)
                .build();
    }

    @DeleteMapping(ApiPaths.AdminExam.DELETE_QUESTION)
    @Operation(summary = "Xóa một câu hỏi khỏi đề thi")
    public ApiResponse<Void> deleteQuestion(
            @PathVariable Long id,
            @PathVariable Long questionId) {

        adminExamService.deleteQuestion(id, questionId);
        return ApiResponse.<Void>builder()
                .success(true)
                .code(HttpStatus.OK.value())
                .message("Xóa câu hỏi thành công")
                .build();
    }

    @DeleteMapping("/{id}/sections/{sectionId}/questions")
    @Operation(summary = "Xóa toàn bộ câu hỏi trong một phần thi để chuẩn bị ghi đè / nạp mới")
    public ApiResponse<Void> clearSectionQuestions(
            @PathVariable Long id,
            @PathVariable Long sectionId) {

        adminExamService.clearSectionQuestions(id, sectionId);
        return ApiResponse.<Void>builder()
                .success(true)
                .code(HttpStatus.OK.value())
                .message("Đã xóa toàn bộ câu hỏi trong phần thi thành công")
                .build();
    }

    @PutMapping("/{id}/passages/{passageId}")
    @Operation(summary = "Chỉnh sửa trực tiếp bài đọc dùng chung")
    public ApiResponse<Void> updatePassage(
            @PathVariable Long id,
            @PathVariable Long passageId,
            @RequestBody com.jp.elearningjp.dto.request.exam.UpdatePassageRequest request) {

        adminExamService.updatePassage(id, passageId, request);
        return ApiResponse.<Void>builder()
                .success(true)
                .code(HttpStatus.OK.value())
                .message("Cập nhật bài đọc thành công")
                .build();
    }

    @DeleteMapping("/{id}/passages/{passageId}")
    @Operation(summary = "Xóa bài đọc dùng chung và các câu hỏi liên quan khỏi đề thi")
    public ApiResponse<Void> deletePassage(
            @PathVariable Long id,
            @PathVariable Long passageId) {

        adminExamService.deletePassage(id, passageId);
        return ApiResponse.<Void>builder()
                .success(true)
                .code(HttpStatus.OK.value())
                .message("Xóa bài đọc thành công")
                .build();
    }

    @GetMapping(ApiPaths.AdminExam.BY_COURSE)
    @Operation(summary = "Lấy danh sách đề thi thuộc về một khóa học (giữa khóa, cuối khóa)")
    public ApiResponse<List<ExamSummaryResponse>> getCourseExams(@PathVariable Long courseId) {
        List<ExamSummaryResponse> exams = adminExamService.getCourseExams(courseId);
        return ApiResponse.<List<ExamSummaryResponse>>builder()
                .success(true)
                .code(HttpStatus.OK.value())
                .message("Lấy danh sách đề thi theo khóa học thành công")
                .data(exams)
                .build();
    }

    @DeleteMapping(ApiPaths.AdminExam.BY_ID)
    @Operation(summary = "Xóa đề thi")
    public ApiResponse<Void> deleteExam(@PathVariable Long id) {
        adminExamService.deleteExam(id);
        return ApiResponse.<Void>builder()
                .success(true)
                .code(HttpStatus.OK.value())
                .message("Xóa đề thi thành công")
                .build();
    }

    @PutMapping("/{id}/sections/{sectionId}/audio")
    @Operation(summary = "Cập nhật file âm thanh (Audio URL) cho một phần thi nghe")
    public ApiResponse<Void> updateSectionAudio(
            @PathVariable Long id,
            @PathVariable Long sectionId,
            @RequestBody java.util.Map<String, String> body) {
        String audioUrl = body != null ? body.get("audioUrl") : null;
        adminExamService.updateSectionAudio(id, sectionId, audioUrl);
        return ApiResponse.<Void>builder()
                .success(true)
                .code(HttpStatus.OK.value())
                .message("Cập nhật file âm thanh thành công")
                .build();
    }

    @PostMapping("/{id}/snapshot")
    @Operation(summary = "Lưu điểm phục hồi / Snapshot cho đề thi khi bắt đầu biên tập")
    public ApiResponse<AdminExamPreviewResponse> saveSnapshot(
            @PathVariable Long id,
            @RequestBody(required = false) AdminExamPreviewResponse snapshot) {
        adminExamService.saveSnapshot(id, snapshot);
        AdminExamPreviewResponse saved = adminExamService.getSnapshot(id);
        return ApiResponse.<AdminExamPreviewResponse>builder()
                .success(true)
                .code(HttpStatus.OK.value())
                .message("Lưu snapshot đề thi thành công")
                .data(saved)
                .build();
    }

    @GetMapping("/{id}/snapshot")
    @Operation(summary = "Lấy dữ liệu snapshot hiện tại của đề thi")
    public ApiResponse<AdminExamPreviewResponse> getSnapshot(@PathVariable Long id) {
        AdminExamPreviewResponse snapshot = adminExamService.getSnapshot(id);
        return ApiResponse.<AdminExamPreviewResponse>builder()
                .success(true)
                .code(HttpStatus.OK.value())
                .message("Lấy snapshot đề thi thành công")
                .data(snapshot)
                .build();
    }

    @PostMapping("/{id}/rollback")
    @Operation(summary = "Hủy toàn bộ thay đổi và khôi phục đề thi về bản snapshot ban đầu")
    public ApiResponse<AdminExamPreviewResponse> rollbackExam(
            @PathVariable Long id,
            @RequestBody(required = false) AdminExamPreviewResponse snapshot) {
        AdminExamPreviewResponse restored = adminExamService.rollbackExam(id, snapshot);
        return ApiResponse.<AdminExamPreviewResponse>builder()
                .success(true)
                .code(HttpStatus.OK.value())
                .message("Đã hoàn tác và khôi phục đề thi về trạng thái ban đầu thành công")
                .data(restored)
                .build();
    }

    @PostMapping("/{id}/reorder-questions")
    @Operation(summary = "Tự động sắp xếp và đánh số lại câu hỏi theo chuẩn JLPT (1 -> N liên tục)")
    public ApiResponse<AdminExamPreviewResponse> reorderQuestions(@PathVariable Long id) {
        AdminExamPreviewResponse preview = adminExamService.reorderExamQuestions(id);
        return ApiResponse.<AdminExamPreviewResponse>builder()
                .success(true)
                .code(HttpStatus.OK.value())
                .message("Đã đánh số lại toàn bộ câu hỏi theo chuẩn JLPT thành công")
                .data(preview)
                .build();
    }

    @PostMapping("/{id}/clone")
    @Operation(summary = "Nhân bản đề thi sang phiên bản mới an toàn để biên tập")
    public ApiResponse<Long> cloneExam(@PathVariable Long id) {
        User currentUser = currentUserService.getCurrentUserOrNull();
        Long currentUserId = (currentUser != null) ? currentUser.getId() : 1L;
        Long newExamId = adminExamService.cloneExam(id, currentUserId);
        return ApiResponse.<Long>builder()
                .success(true)
                .code(HttpStatus.CREATED.value())
                .message("Đã nhân bản đề thi sang phiên bản mới thành công")
                .data(newExamId)
                .build();
    }

    @PostMapping("/{id}/regrade")
    @Operation(summary = "Chấm lại toàn bộ điểm các bài thi đã nộp sau khi đính chính đáp án")
    public ApiResponse<Integer> regradeExam(@PathVariable Long id) {
        int regradedCount = adminExamService.regradeExam(id);
        return ApiResponse.<Integer>builder()
                .success(true)
                .code(HttpStatus.OK.value())
                .message("Đã chấm lại thành công cho " + regradedCount + " lượt làm bài")
                .data(regradedCount)
                .build();
    }
}

