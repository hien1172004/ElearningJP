package com.jp.elearningjp.controller.notebook;

import com.jp.elearningjp.dto.request.notebook.CreateNotebookWordRequest;
import com.jp.elearningjp.dto.request.notebook.UpdateNotebookWordRequest;
import com.jp.elearningjp.dto.response.notebook.ExcelImportResultResponse;
import com.jp.elearningjp.dto.response.notebook.KanjiPdfMetaResponse;
import com.jp.elearningjp.dto.response.notebook.KanjiPdfPreviewResponse;
import com.jp.elearningjp.dto.response.notebook.NotebookStatsResponse;
import com.jp.elearningjp.dto.response.notebook.NotebookWordResponse;
import com.jp.elearningjp.service.notebook.NotebookExportService;
import com.jp.elearningjp.service.notebook.NotebookWordService;
import com.jp.elearningjp.shared.constants.ApiPaths;
import com.jp.elearningjp.shared.response.ApiResponse;
import com.jp.elearningjp.shared.response.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping(ApiPaths.API_V1 + ApiPaths.Notebook.BASE)
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Notebook Words", description = "Quản lý Từ vựng trong Sổ tay và Nhập/Xuất")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("isAuthenticated()")
public class NotebookWordController {

    NotebookWordService notebookWordService;
    NotebookExportService notebookExportService;

    @Operation(summary = "Thêm từ vựng vào sổ tay")
    @PostMapping(ApiPaths.Notebook.WORDS)
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<NotebookWordResponse> addWord(
            @PathVariable Long id,
            @Valid @RequestBody CreateNotebookWordRequest request
    ) {
        return ApiResponse.<NotebookWordResponse>builder()
                .success(true)
                .data(notebookWordService.addWord(id, request))
                .build();
    }

    private static final java.util.Set<String> ALLOWED_WORD_SORT_FIELDS = java.util.Set.of(
            "id", "word", "displayOrder", "memoryLevel", "createdAt", "updatedAt", "repetitionCount", "nextReviewAt"
    );

    @Operation(summary = "Lấy danh sách từ vựng trong sổ tay có phân trang và tìm kiếm")
    @GetMapping(ApiPaths.Notebook.WORDS)
    public ApiResponse<PageResponse<NotebookWordResponse>> getWords(
            @PathVariable Long id,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id,desc") String sort
    ) {
        String[] sortParts = sort.split(",");
        String sortField = sortParts[0];
        if (!ALLOWED_WORD_SORT_FIELDS.contains(sortField)) {
            sortField = "id";
        }
        Sort.Direction direction = sortParts.length > 1 && sortParts[1].equalsIgnoreCase("asc")
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        PageRequest pageRequest = PageRequest.of(Math.max(0, page), Math.min(Math.max(size, 1), 100), Sort.by(direction, sortField));
        Page<NotebookWordResponse> pageResult = notebookWordService.searchWords(id, keyword, pageRequest);

        PageResponse<NotebookWordResponse> pageResponse = PageResponse.<NotebookWordResponse>builder()
                .items(pageResult.getContent())
                .page(pageResult.getNumber())
                .size(pageResult.getSize())
                .totalElements(pageResult.getTotalElements())
                .totalPages(pageResult.getTotalPages())
                .build();

        return ApiResponse.<PageResponse<NotebookWordResponse>>builder()
                .success(true)
                .data(pageResponse)
                .build();
    }

    @Operation(summary = "Lấy toàn bộ từ vựng xáo trộn ngẫu nhiên (dành cho chế độ lật thẻ / autoplay)")
    @GetMapping(ApiPaths.Notebook.WORDS_SHUFFLE)
    public ApiResponse<List<NotebookWordResponse>> getShuffledWords(@PathVariable Long id) {
        return ApiResponse.<List<NotebookWordResponse>>builder()
                .success(true)
                .data(notebookWordService.getShuffledWords(id))
                .build();
    }

    @Operation(summary = "Cập nhật từ vựng trong sổ tay")
    @PutMapping(ApiPaths.Notebook.WORD_BY_ID)
    public ApiResponse<NotebookWordResponse> updateWord(
            @PathVariable Long id,
            @PathVariable Long wordId,
            @Valid @RequestBody UpdateNotebookWordRequest request
    ) {
        return ApiResponse.<NotebookWordResponse>builder()
                .success(true)
                .data(notebookWordService.updateWord(id, wordId, request))
                .build();
    }

    @Operation(summary = "Xóa một từ vựng khỏi sổ tay")
    @DeleteMapping(ApiPaths.Notebook.WORD_BY_ID)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ApiResponse<Void> deleteWord(
            @PathVariable Long id,
            @PathVariable Long wordId
    ) {
        notebookWordService.deleteWord(id, wordId);
        return ApiResponse.<Void>builder()
                .success(true)
                .message("Đã xóa từ vựng thành công")
                .build();
    }

    @Operation(summary = "Xóa hàng loạt từ vựng khỏi sổ tay")
    @DeleteMapping(ApiPaths.Notebook.WORDS_BATCH)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ApiResponse<Void> batchDeleteWords(
            @PathVariable Long id,
            @RequestBody List<Long> wordIds
    ) {
        notebookWordService.batchDeleteWords(id, wordIds);
        return ApiResponse.<Void>builder()
                .success(true)
                .message("Đã xóa hàng loạt từ vựng thành công")
                .build();
    }

    @Operation(summary = "Nhập từ vựng từ file Excel (giới hạn <= 500 dòng, <= 5MB)")
    @PostMapping(value = ApiPaths.Notebook.WORDS_IMPORT, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ExcelImportResultResponse> importWordsFromExcel(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file
    ) {
        return ApiResponse.<ExcelImportResultResponse>builder()
                .success(true)
                .data(notebookWordService.importWordsFromExcel(id, file))
                .build();
    }

    @Operation(summary = "Xuất danh sách từ vựng ra file Excel")
    @GetMapping(ApiPaths.Notebook.EXPORT_EXCEL)
    public ResponseEntity<byte[]> exportExcel(@PathVariable Long id) {
        byte[] excelBytes = notebookExportService.exportWordsToExcel(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=notebook_" + id + "_words.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excelBytes);
    }

    @Operation(summary = "Tải PDF vở luyện viết chữ Kanji ô chữ Mễ vector")
    @GetMapping(ApiPaths.Notebook.EXPORT_KANJI_PDF)
    public ResponseEntity<byte[]> exportKanjiPdf(@PathVariable Long id) {
        byte[] pdfBytes = notebookExportService.generateKanjiPracticePdf(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=notebook_" + id + "_kanji_practice.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    @Operation(summary = "Xem trước ảnh từng trang của PDF luyện viết Kanji")
    @GetMapping(ApiPaths.Notebook.EXPORT_KANJI_PDF_PREVIEW)
    public ResponseEntity<byte[]> exportKanjiPdfPreview(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "1") Integer page
    ) {
        KanjiPdfPreviewResponse preview = notebookExportService.generateKanjiPracticePdfPreview(id, page);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .header("X-Total-Pages", String.valueOf(preview.getTotalPages()))
                .header("X-Current-Page", String.valueOf(preview.getCurrentPage()))
                .header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, "X-Total-Pages, X-Current-Page")
                .body(preview.getImageBytes());
    }

    @Operation(summary = "Lấy thông tin số trang của PDF luyện viết Kanji")
    @GetMapping(ApiPaths.Notebook.EXPORT_KANJI_PDF_META)
    public ApiResponse<KanjiPdfMetaResponse> getKanjiPdfMeta(@PathVariable Long id) {
        return ApiResponse.<KanjiPdfMetaResponse>builder()
                .success(true)
                .data(notebookExportService.getKanjiPracticePdfMeta(id))
                .build();
    }

    @Operation(summary = "Lấy thống kê tổng quan mức ghi nhớ của toàn bộ sổ tay người dùng")
    @GetMapping(ApiPaths.Notebook.STATS_OVERVIEW)
    public ApiResponse<NotebookStatsResponse> getStatsOverview() {
        return ApiResponse.<NotebookStatsResponse>builder()
                .success(true)
                .data(notebookWordService.getStatsOverview())
                .build();
    }

    @Operation(summary = "Lấy danh sách từ vựng cần ôn tập SRS / làm Quizz")
    @GetMapping(ApiPaths.Notebook.REVIEW_ITEMS)
    public ApiResponse<List<com.jp.elearningjp.dto.response.notebook.NotebookReviewItemResponse>> getReviewItems(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "20") Integer limit
    ) {
        return ApiResponse.<List<com.jp.elearningjp.dto.response.notebook.NotebookReviewItemResponse>>builder()
                .success(true)
                .data(notebookWordService.getReviewItems(id, limit))
                .build();
    }

    @Operation(summary = "Gửi kết quả đánh giá chất lượng ôn tập (SRS SM-2 Rating)")
    @PostMapping(ApiPaths.Notebook.REVIEW_SUBMIT)
    public ApiResponse<com.jp.elearningjp.dto.response.notebook.NotebookReviewSubmitResponse> submitReview(
            @PathVariable Long id,
            @Valid @RequestBody com.jp.elearningjp.dto.request.notebook.NotebookReviewSubmitRequest request
    ) {
        return ApiResponse.<com.jp.elearningjp.dto.response.notebook.NotebookReviewSubmitResponse>builder()
                .success(true)
                .data(notebookWordService.submitReview(id, request))
                .build();
    }
}

