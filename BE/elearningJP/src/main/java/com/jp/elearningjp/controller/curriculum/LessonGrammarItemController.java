package com.jp.elearningjp.controller.curriculum;

import com.jp.elearningjp.dto.request.curriculum.ItemBulkReorderRequest;
import com.jp.elearningjp.dto.request.curriculum.LessonGrammarItemCreateRequest;
import com.jp.elearningjp.dto.request.curriculum.LessonGrammarItemUpdateRequest;
import com.jp.elearningjp.dto.response.curriculum.LessonGrammarItemResponse;
import com.jp.elearningjp.service.curriculum.section.GrammarSectionItemService;
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
import com.jp.elearningjp.service.curriculum.importer.grammar.GrammarImportService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Lesson Grammar Items", description = "Quản lý điểm ngữ pháp trong section GRAMMAR. "
        + "Examples do giáo viên cấu hình được nhúng trong payload create/update của grammar item "
        + "(xem LessonGrammarItemCreateRequest.examples / LessonGrammarItemUpdateRequest.examples).")
@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LessonGrammarItemController {

    GrammarSectionItemService grammarSectionItemService;
    GrammarImportService grammarImportService;

    // ============================================================
    // GRAMMAR ITEM CRUD (4 endpoints chính)
    // V27+: examples được xử lý trong payload create/update (giống example của vocab item).
    //       Khi xoá grammar item → cascade soft-delete tất cả examples thuộc item đó.
    //       KHÔNG còn endpoint POST/PUT/DELETE riêng cho example.
    // ============================================================

    @Operation(summary = "[GV/Admin] Thêm 1 điểm ngữ pháp + ví dụ vào section (kèm examples do giáo viên cấu hình)")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @PostMapping(ApiPaths.API_V1 + ApiPaths.Lesson.LESSON_SECTIONS + ApiPaths.Lesson.GRAMMAR_ITEMS)
    public ApiResponse<LessonGrammarItemResponse> addGrammarItem(
            @PathVariable Long sectionId,
            @Valid @RequestBody LessonGrammarItemCreateRequest request
    ) {
        return ApiResponse.<LessonGrammarItemResponse>builder()
                .success(true)
                .message("Thêm ngữ pháp vào section thành công")
                .data(grammarSectionItemService.addGrammarItem(sectionId, request))
                .build();
    }

    @Operation(summary = "Lấy chi tiết 1 grammar item (kèm examples đã cấu hình)")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping(ApiPaths.API_V1 + ApiPaths.Lesson.LESSON_GRAMMAR_ITEMS + ApiPaths.Lesson.GRAMMAR_ITEM_BY_ID)
    public ApiResponse<LessonGrammarItemResponse> getGrammarItem(@PathVariable Long itemId) {
        return ApiResponse.<LessonGrammarItemResponse>builder()
                .success(true)
                .message("Lấy chi tiết ngữ pháp thành công")
                .data(grammarSectionItemService.getGrammarItem(itemId))
                .build();
    }

    @Operation(summary = "[GV/Admin] Cập nhật grammar item (orderIndex, customXxx, examples)")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @PutMapping(ApiPaths.API_V1 + ApiPaths.Lesson.LESSON_GRAMMAR_ITEMS + ApiPaths.Lesson.GRAMMAR_ITEM_BY_ID)
    public ApiResponse<LessonGrammarItemResponse> updateGrammarItem(
            @PathVariable Long itemId,
            @Valid @RequestBody LessonGrammarItemUpdateRequest request
    ) {
        return ApiResponse.<LessonGrammarItemResponse>builder()
                .success(true)
                .message("Cập nhật ngữ pháp thành công")
                .data(grammarSectionItemService.updateGrammarItem(itemId, request))
                .build();
    }

    @Operation(summary = "[GV/Admin] Xoá mềm grammar item (cascade soft-delete tất cả examples)")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @DeleteMapping(ApiPaths.API_V1 + ApiPaths.Lesson.LESSON_GRAMMAR_ITEMS + ApiPaths.Lesson.GRAMMAR_ITEM_BY_ID)
    public ApiResponse<Void> deleteGrammarItem(@PathVariable Long itemId) {
        grammarSectionItemService.deleteGrammarItem(itemId);
        return ApiResponse.<Void>builder()
                .success(true)
                .message("Đã xoá ngữ pháp (kèm examples)")
                .build();
    }

    @Operation(summary = "[GV/Admin] Sắp xếp lại thứ tự hàng loạt các grammar item trong section (drag-drop)")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @PostMapping(ApiPaths.API_V1 + ApiPaths.Lesson.LESSON_SECTIONS + ApiPaths.Lesson.GRAMMAR_ITEMS_REORDER)
    public ApiResponse<List<LessonGrammarItemResponse>> reorderGrammarItems(
            @PathVariable Long sectionId,
            @Valid @RequestBody ItemBulkReorderRequest request
    ) {
        return ApiResponse.<List<LessonGrammarItemResponse>>builder()
                .success(true)
                .message("Sắp xếp lại thứ tự grammar item thành công")
                .data(grammarSectionItemService.reorderGrammarItems(sectionId, request))
                .build();
    }

    // ============================================================
    // IMPORT EXCEL (mẫu riêng cho section GRAMMAR)
    // ============================================================

    @Operation(summary = "[GV/Admin] Tải file Excel mẫu để import ngữ pháp vào section GRAMMAR")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @GetMapping(ApiPaths.API_V1 + ApiPaths.Lesson.LESSON_SECTIONS + ApiPaths.Lesson.GRAMMAR_IMPORT_TEMPLATE)
    public ResponseEntity<byte[]> downloadGrammarImportTemplate() {
        ImportTemplateFile template = grammarImportService.buildTemplate();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(template.fileName()).build().toString())
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(template.content());
    }

    @Operation(summary = "[GV/Admin] Kiểm tra file Excel ngữ pháp trước khi import (không ghi dữ liệu)")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @PostMapping(value = ApiPaths.API_V1 + ApiPaths.Lesson.LESSON_SECTIONS + ApiPaths.Lesson.GRAMMAR_IMPORT_PREVIEW,
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<SectionImportPreviewResponse> previewGrammarImport(
            @PathVariable Long sectionId,
            @RequestParam("file") MultipartFile file
    ) {
        return ApiResponse.<SectionImportPreviewResponse>builder()
                .success(true)
                .message("Kiểm tra file Excel thành công")
                .data(grammarImportService.preview(sectionId, file))
                .build();
    }

    @Operation(summary = "[GV/Admin] Import ngữ pháp từ file Excel vào section (file còn dòng lỗi thì không ghi gì)")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @PostMapping(value = ApiPaths.API_V1 + ApiPaths.Lesson.LESSON_SECTIONS + ApiPaths.Lesson.GRAMMAR_IMPORT_COMMIT,
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<SectionImportCommitResponse> commitGrammarImport(
            @PathVariable Long sectionId,
            @RequestParam("file") MultipartFile file
    ) {
        return ApiResponse.<SectionImportCommitResponse>builder()
                .success(true)
                .message("Import ngữ pháp từ Excel thành công")
                .data(grammarImportService.commit(sectionId, file))
                .build();
    }
}
