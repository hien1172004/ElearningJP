package com.jp.elearningjp.controller.curriculum;

import com.jp.elearningjp.dto.request.curriculum.LessonGrammarExampleRequest;
import com.jp.elearningjp.dto.request.curriculum.LessonGrammarItemCreateRequest;
import com.jp.elearningjp.dto.request.curriculum.LessonGrammarItemUpdateRequest;
import com.jp.elearningjp.dto.response.curriculum.LessonGrammarExampleResponse;
import com.jp.elearningjp.dto.response.curriculum.LessonGrammarItemResponse;
import com.jp.elearningjp.service.curriculum.LessonItemService;
import com.jp.elearningjp.shared.constants.ApiPaths;
import com.jp.elearningjp.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller cho Grammar items bên trong section GRAMMAR.
 * Endpoints:
 * <ul>
 *   <li>POST   /api/v1/lesson-sections/{sectionId}/grammar-items</li>
 *   <li>PUT    /api/v1/lesson-grammar-items/{itemId}</li>
 *   <li>DELETE /api/v1/lesson-grammar-items/{itemId}</li>
 *   <li>POST   /api/v1/lesson-grammar-items/{grammarItemId}/examples</li>
 *   <li>PUT    /api/v1/lesson-grammar-items/examples/{exampleId}</li>
 *   <li>DELETE /api/v1/lesson-grammar-items/examples/{exampleId}</li>
 * </ul>
 */
@Tag(name = "Lesson Grammar Items", description = "Quản lý điểm ngữ pháp + ví dụ trong section GRAMMAR")
@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LessonGrammarItemController {

    LessonItemService lessonItemService;

    // ============================================================
    // GRAMMAR ITEM CRUD
    // ============================================================

    @Operation(summary = "[GV/Admin] Thêm 1 điểm ngữ pháp vào section")
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
                .data(lessonItemService.addGrammarItem(sectionId, request))
                .build();
    }

    @Operation(summary = "[GV/Admin] Cập nhật grammar item")
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
                .data(lessonItemService.updateGrammarItem(itemId, request))
                .build();
    }

    @Operation(summary = "[GV/Admin] Xoá mềm grammar item")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @DeleteMapping(ApiPaths.API_V1 + ApiPaths.Lesson.LESSON_GRAMMAR_ITEMS + ApiPaths.Lesson.GRAMMAR_ITEM_BY_ID)
    public ApiResponse<Void> deleteGrammarItem(@PathVariable Long itemId) {
        lessonItemService.deleteGrammarItem(itemId);
        return ApiResponse.<Void>builder()
                .success(true)
                .message("Đã xoá ngữ pháp")
                .build();
    }

    // ============================================================
    // GRAMMAR EXAMPLES
    // ============================================================

    @Operation(summary = "[GV/Admin] Thêm ví dụ cho grammar item")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @PostMapping(ApiPaths.API_V1 + ApiPaths.Lesson.LESSON_GRAMMAR_ITEMS + ApiPaths.Lesson.LESSON_GRAMMAR_EXAMPLES)
    public ApiResponse<LessonGrammarExampleResponse> addGrammarExample(
            @PathVariable Long grammarItemId,
            @Valid @RequestBody LessonGrammarExampleRequest request
    ) {
        return ApiResponse.<LessonGrammarExampleResponse>builder()
                .success(true)
                .message("Thêm ví dụ ngữ pháp thành công")
                .data(lessonItemService.addGrammarExample(grammarItemId, request))
                .build();
    }

    @Operation(summary = "[GV/Admin] Cập nhật ví dụ ngữ pháp")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @PutMapping(ApiPaths.API_V1 + ApiPaths.Lesson.LESSON_GRAMMAR_ITEMS + ApiPaths.Lesson.GRAMMAR_EXAMPLE_BY_ID)
    public ApiResponse<LessonGrammarExampleResponse> updateGrammarExample(
            @PathVariable Long exampleId,
            @Valid @RequestBody LessonGrammarExampleRequest request
    ) {
        return ApiResponse.<LessonGrammarExampleResponse>builder()
                .success(true)
                .message("Cập nhật ví dụ thành công")
                .data(lessonItemService.updateGrammarExample(exampleId, request))
                .build();
    }

    @Operation(summary = "[GV/Admin] Xoá mềm ví dụ ngữ pháp")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @DeleteMapping(ApiPaths.API_V1 + ApiPaths.Lesson.LESSON_GRAMMAR_ITEMS + ApiPaths.Lesson.GRAMMAR_EXAMPLE_BY_ID)
    public ApiResponse<Void> deleteGrammarExample(@PathVariable Long exampleId) {
        lessonItemService.deleteGrammarExample(exampleId);
        return ApiResponse.<Void>builder()
                .success(true)
                .message("Đã xoá ví dụ")
                .build();
    }
}
