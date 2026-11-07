package com.jp.elearningjp.controller.handwriting;

import com.jp.elearningjp.dto.request.handwriting.HandwritingPracticeLogRequest;
import com.jp.elearningjp.dto.response.handwriting.HandwritingPracticeLogResponse;
import com.jp.elearningjp.dto.response.handwriting.HandwritingPracticeSummaryResponse;
import com.jp.elearningjp.service.handwriting.HandwritingPracticeService;
import com.jp.elearningjp.shared.constants.ApiPaths;
import com.jp.elearningjp.shared.response.ApiResponse;
import com.jp.elearningjp.shared.util.CurrentUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(ApiPaths.API_V1 + ApiPaths.Handwriting.BASE)
@RequiredArgsConstructor
@Validated
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Handwriting (Luyện viết Kanji)", description = "Lưu điểm luyện viết (chuột / bằng tay) và xem tổng hợp")
public class HandwritingPracticeController {

    HandwritingPracticeService practiceService;
    CurrentUserService currentUserService;

    @Operation(summary = "Lưu kết quả một lần luyện viết",
            description = "FE gọi sau khi chấm xong (Hanzi-Writer hoàn thành quiz hoặc AI trả điểm). "
                    + "Trả về cờ kỷ lục mới và tổng hợp điểm sau khi lưu.")
    @PostMapping(ApiPaths.Handwriting.PRACTICE_LOGS)
    public ApiResponse<HandwritingPracticeLogResponse> save(@Valid @RequestBody HandwritingPracticeLogRequest request) {
        Long userId = currentUserService.getCurrentUserId();
        return ApiResponse.<HandwritingPracticeLogResponse>builder()
                .success(true)
                .data(practiceService.save(userId, request))
                .build();
    }

    @Operation(summary = "Tổng hợp điểm luyện viết của tôi cho 1 chữ",
            description = "lessonId bỏ trống = các lần luyện ngoài bài học (từ điển / sổ tay).")
    @GetMapping(ApiPaths.Handwriting.SUMMARY)
    public ApiResponse<HandwritingPracticeSummaryResponse> summary(
            @RequestParam @NotBlank @Size(max = 8) String character,
            @RequestParam(required = false) Long lessonId) {
        Long userId = currentUserService.getCurrentUserId();
        return ApiResponse.<HandwritingPracticeSummaryResponse>builder()
                .success(true)
                .data(practiceService.summary(userId, character, lessonId))
                .build();
    }
}
