package com.jp.elearningjp.controller.curriculum;

import com.jp.elearningjp.dto.request.curriculum.ItemBulkReorderRequest;
import com.jp.elearningjp.dto.request.curriculum.LessonVocabularyItemCreateRequest;
import com.jp.elearningjp.dto.request.curriculum.LessonVocabularyItemUpdateRequest;
import com.jp.elearningjp.dto.response.curriculum.LessonVocabularyItemResponse;
import com.jp.elearningjp.service.curriculum.section.VocabularySectionItemService;
import com.jp.elearningjp.shared.constants.ApiPaths;
import com.jp.elearningjp.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import com.jp.elearningjp.dto.response.curriculum.importer.SectionImportCommitResponse;
import com.jp.elearningjp.dto.response.curriculum.importer.SectionImportPreviewResponse;
import com.jp.elearningjp.service.curriculum.importer.ImportTemplateFile;
import com.jp.elearningjp.service.curriculum.importer.vocabulary.VocabularyImportService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Lesson Vocabulary Items", description = "Quản lý từ vựng trong section VOCABULARY. "
        + "Examples do giáo viên cấu hình được nhúng trong payload create/update của vocab item "
        + "(xem LessonVocabularyItemCreateRequest.examples / LessonVocabularyItemUpdateRequest.examples).")
@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LessonVocabularyItemController {

    VocabularySectionItemService vocabularySectionItemService;
    VocabularyImportService vocabularyImportService;

    // ============================================================
    // VOCABULARY ITEM CRUD (4 endpoints chính)
    // V26+: examples được xử lý trong payload create/update (giống relatedVocab của kanji item).
    //       Khi xoá vocab item → cascade soft-delete tất cả examples thuộc item đó.
    //       KHÔNG còn endpoint POST/PUT/DELETE riêng cho example.
    // ============================================================

    @Operation(summary = "[GV/Admin] Thêm 1 từ vựng + ví dụ vào section (kèm examples do giáo viên cấu hình)")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @PostMapping(ApiPaths.API_V1 + ApiPaths.Lesson.LESSON_SECTIONS + ApiPaths.Lesson.VOCABULARY_ITEMS)
    public ApiResponse<LessonVocabularyItemResponse> addVocabularyItem(
            @PathVariable Long sectionId,
            @Valid @RequestBody LessonVocabularyItemCreateRequest request
    ) {
        return ApiResponse.<LessonVocabularyItemResponse>builder()
                .success(true)
                .message("Thêm từ vựng vào section thành công")
                .data(vocabularySectionItemService.addVocabularyItem(sectionId, request))
                .build();
    }

    @Operation(summary = "Lấy chi tiết 1 vocabulary item (kèm examples đã cấu hình)")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping(ApiPaths.API_V1 + ApiPaths.Lesson.LESSON_VOCABULARY_ITEMS + ApiPaths.Lesson.VOCABULARY_ITEM_BY_ID)
    public ApiResponse<LessonVocabularyItemResponse> getVocabularyItem(@PathVariable Long itemId) {
        return ApiResponse.<LessonVocabularyItemResponse>builder()
                .success(true)
                .message("Lấy chi tiết từ vựng thành công")
                .data(vocabularySectionItemService.getVocabularyItem(itemId))
                .build();
    }

    @Operation(summary = "[GV/Admin] Cập nhật vocabulary item (orderIndex, customXxx, examples)")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @PutMapping(ApiPaths.API_V1 + ApiPaths.Lesson.LESSON_VOCABULARY_ITEMS + ApiPaths.Lesson.VOCABULARY_ITEM_BY_ID)
    public ApiResponse<LessonVocabularyItemResponse> updateVocabularyItem(
            @PathVariable Long itemId,
            @Valid @RequestBody LessonVocabularyItemUpdateRequest request
    ) {
        return ApiResponse.<LessonVocabularyItemResponse>builder()
                .success(true)
                .message("Cập nhật từ vựng thành công")
                .data(vocabularySectionItemService.updateVocabularyItem(itemId, request))
                .build();
    }

    @Operation(summary = "[GV/Admin] Xoá mềm vocabulary item (cascade soft-delete tất cả examples)")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @DeleteMapping(ApiPaths.API_V1 + ApiPaths.Lesson.LESSON_VOCABULARY_ITEMS + ApiPaths.Lesson.VOCABULARY_ITEM_BY_ID)
    public ApiResponse<Void> deleteVocabularyItem(@PathVariable Long itemId) {
        vocabularySectionItemService.deleteVocabularyItem(itemId);
        return ApiResponse.<Void>builder()
                .success(true)
                .message("Đã xoá từ vựng (kèm examples)")
                .build();
    }

    @Operation(summary = "[GV/Admin] Sắp xếp lại thứ tự hàng loạt các vocabulary item trong section (drag-drop)")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @PostMapping(ApiPaths.API_V1 + ApiPaths.Lesson.LESSON_SECTIONS + ApiPaths.Lesson.VOCABULARY_ITEMS_REORDER)
    public ApiResponse<List<LessonVocabularyItemResponse>> reorderVocabularyItems(
            @PathVariable Long sectionId,
            @Valid @RequestBody ItemBulkReorderRequest request
    ) {
        return ApiResponse.<List<LessonVocabularyItemResponse>>builder()
                .success(true)
                .message("Sắp xếp lại thứ tự vocabulary item thành công")
                .data(vocabularySectionItemService.reorderVocabularyItems(sectionId, request))
                .build();
    }

    // ============================================================
    // IMPORT EXCEL (mẫu riêng cho section VOCABULARY)
    // ============================================================

    @Operation(summary = "[GV/Admin] Tải file Excel mẫu để import từ vựng vào section VOCABULARY")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @GetMapping(ApiPaths.API_V1 + ApiPaths.Lesson.LESSON_SECTIONS + ApiPaths.Lesson.VOCABULARY_IMPORT_TEMPLATE)
    public ResponseEntity<byte[]> downloadVocabularyImportTemplate() {
        ImportTemplateFile template = vocabularyImportService.buildTemplate();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(template.fileName()).build().toString())
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(template.content());
    }

    @Operation(summary = "[GV/Admin] Kiểm tra file Excel từ vựng trước khi import (không ghi dữ liệu)")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @PostMapping(value = ApiPaths.API_V1 + ApiPaths.Lesson.LESSON_SECTIONS + ApiPaths.Lesson.VOCABULARY_IMPORT_PREVIEW,
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<SectionImportPreviewResponse> previewVocabularyImport(
            @PathVariable Long sectionId,
            @RequestParam("file") MultipartFile file
    ) {
        return ApiResponse.<SectionImportPreviewResponse>builder()
                .success(true)
                .message("Kiểm tra file Excel thành công")
                .data(vocabularyImportService.preview(sectionId, file))
                .build();
    }

    @Operation(summary = "[GV/Admin] Import từ vựng từ file Excel vào section (file còn dòng lỗi thì không ghi gì)")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @PostMapping(value = ApiPaths.API_V1 + ApiPaths.Lesson.LESSON_SECTIONS + ApiPaths.Lesson.VOCABULARY_IMPORT_COMMIT,
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<SectionImportCommitResponse> commitVocabularyImport(
            @PathVariable Long sectionId,
            @RequestParam("file") MultipartFile file
    ) {
        return ApiResponse.<SectionImportCommitResponse>builder()
                .success(true)
                .message("Import từ vựng từ Excel thành công")
                .data(vocabularyImportService.commit(sectionId, file))
                .build();
    }

    // ============================================================
    // EXPORT EXCEL (cùng định dạng mẫu import)
    // ============================================================

    @Operation(summary = "[GV/Admin] Xuất từ vựng (+ câu ví dụ) của section ra file Excel (cùng định dạng mẫu import)")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @GetMapping(ApiPaths.API_V1 + ApiPaths.Lesson.LESSON_SECTIONS + ApiPaths.Lesson.VOCABULARY_EXPORT)
    public ResponseEntity<byte[]> exportVocabularySection(@PathVariable Long sectionId) {
        return vocabularyImportService.exportSection(sectionId).toResponse();
    }
}
