package com.jp.elearningjp.controller.exam;

import com.jp.elearningjp.dto.request.exam.UpdateQuestionInlineRequest;
import com.jp.elearningjp.dto.response.exam.AdminExamPreviewResponse;
import com.jp.elearningjp.service.exam.AdminExamService;
import com.jp.elearningjp.shared.constants.ApiPaths;
import com.jp.elearningjp.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(ApiPaths.API_V1 + ApiPaths.AdminExam.BASE)
@RequiredArgsConstructor
@Tag(name = "Admin Exam", description = "APIs dành cho Quản trị viên/Giáo viên quản lý, xem trước và chỉnh sửa đề thi")
public class AdminExamController {

    private final AdminExamService adminExamService;

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
}
