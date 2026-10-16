package com.jp.elearningjp.controller.srs;

import com.jp.elearningjp.shared.response.ApiResponse;
import com.jp.elearningjp.dto.request.srs.SrsBatchCreateRequest;
import com.jp.elearningjp.dto.request.srs.SrsItemCreateRequest;
import com.jp.elearningjp.dto.request.srs.SrsReviewRequest;
import com.jp.elearningjp.dto.response.srs.SrsDueItemResponse;
import com.jp.elearningjp.dto.response.srs.SrsReviewResultResponse;
import com.jp.elearningjp.dto.response.srs.SrsStatsResponse;
import com.jp.elearningjp.service.srs.SrsService;
import com.jp.elearningjp.shared.constants.ApiPaths;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(ApiPaths.API_V1 + ApiPaths.Srs.BASE)
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "SRS (Spaced Repetition System)", description = "Quản lý Flashcard học tập & Thuật toán lặp lại ngắt quãng SM-2")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("isAuthenticated()")
public class SrsController {

    SrsService srsService;

    @Operation(summary = "Lấy danh sách thẻ cần ôn hôm nay", description = "Trả về hàng đợi các Flashcard đã đến hạn ôn tập (nextReviewAt <= NOW) của học viên.")
    @GetMapping(ApiPaths.Srs.DUE)
    public ApiResponse<List<SrsDueItemResponse>> getDueCards(
            @RequestParam(defaultValue = "20") int limit
    ) {
        return ApiResponse.<List<SrsDueItemResponse>>builder()
                .success(true)
                .data(srsService.getDueCards(limit))
                .build();
    }

    @Operation(summary = "Thêm một mục lẻ vào SRS", description = "Học viên chủ động thêm 1 từ vựng, chữ Hán hoặc điểm ngữ pháp vào danh sách ôn tập cá nhân.")
    @PostMapping(ApiPaths.Srs.ITEMS)
    public ApiResponse<SrsDueItemResponse> addItem(
            @Valid @RequestBody SrsItemCreateRequest request
    ) {
        return ApiResponse.<SrsDueItemResponse>builder()
                .success(true)
                .data(srsService.addItem(request))
                .build();
    }

    @Operation(summary = "Thêm hàng loạt từ vựng trong bài học vào SRS", description = "Tự động trích xuất toàn bộ từ vựng/chữ Hán trong bài học và thêm vào danh sách ôn tập của học viên.")
    @PostMapping(ApiPaths.Srs.LESSON_ADD_ALL)
    public ApiResponse<Integer> batchAddFromLesson(@PathVariable Long lessonId) {
        int addedCount = srsService.batchAddFromLesson(lessonId);
        return ApiResponse.<Integer>builder()
                .success(true)
                .message("Đã thêm thành công " + addedCount + " mục vào sổ tay ôn tập SRS")
                .data(addedCount)
                .build();
    }

    @Operation(summary = "Thêm hàng loạt theo danh sách ID", description = "Thêm nhiều từ vựng, chữ Hán, ngữ pháp cùng lúc vào SRS.")
    @PostMapping(ApiPaths.Srs.BATCH)
    public ApiResponse<Integer> batchAdd(@Valid @RequestBody SrsBatchCreateRequest request) {
        int addedCount = srsService.batchAdd(request);
        return ApiResponse.<Integer>builder()
                .success(true)
                .message("Đã thêm thành công " + addedCount + " mục vào sổ tay ôn tập SRS")
                .data(addedCount)
                .build();
    }

    @Operation(summary = "Nộp kết quả ôn tập thẻ (Bấm nút Again, Hard, Good, Easy)", description = "Thuật toán SM-2 tự động tính toán lại khoảng cách ngày ôn tiếp theo, hệ số Ease Factor và thời gian ôn tiếp theo.")
    @PostMapping(ApiPaths.Srs.REVIEW)
    public ApiResponse<SrsReviewResultResponse> reviewCard(
            @PathVariable Long id,
            @Valid @RequestBody SrsReviewRequest request
    ) {
        return ApiResponse.<SrsReviewResultResponse>builder()
                .success(true)
                .data(srsService.reviewCard(id, request))
                .build();
    }

    @Operation(summary = "Xem thống kê SRS của cá nhân", description = "Trả về tổng số thẻ, số thẻ cần ôn hôm nay, số thẻ đang học và số thẻ đã thành thạo.")
    @GetMapping(ApiPaths.Srs.STATS)
    public ApiResponse<SrsStatsResponse> getStatistics() {
        return ApiResponse.<SrsStatsResponse>builder()
                .success(true)
                .data(srsService.getStatistics())
                .build();
    }
}
