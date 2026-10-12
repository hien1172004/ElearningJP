package com.jp.elearningjp.controller.curriculum;

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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Lesson Grammar Items", description = "Quản lý điểm ngữ pháp trong section GRAMMAR. "
        + "Examples do giáo viên cấu hình được nhúng trong payload create/update của grammar item "
        + "(xem LessonGrammarItemCreateRequest.examples / LessonGrammarItemUpdateRequest.examples).")
@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LessonGrammarItemController {

    GrammarSectionItemService grammarSectionItemService;

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
}