package com.jp.elearningjp.controller.curriculum;

import com.jp.elearningjp.dto.request.curriculum.LessonVocabularyExampleRequest;
import com.jp.elearningjp.dto.request.curriculum.LessonVocabularyItemCreateRequest;
import com.jp.elearningjp.dto.request.curriculum.LessonVocabularyItemUpdateRequest;
import com.jp.elearningjp.dto.response.curriculum.LessonVocabularyExampleResponse;
import com.jp.elearningjp.dto.response.curriculum.LessonVocabularyItemResponse;
import com.jp.elearningjp.service.curriculum.LessonItemService;
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

@Tag(name = "Lesson Vocabulary Items", description = "Quản lý từ vựng + ví dụ trong section VOCABULARY")
@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LessonVocabularyItemController {

    LessonItemService lessonItemService;

    // ============================================================
    // VOCABULARY ITEM CRUD
    // ============================================================

    @Operation(summary = "[GV/Admin] Thêm 1 từ vựng vào section")
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
                .data(lessonItemService.addVocabularyItem(sectionId, request))
                .build();
    }

    @Operation(summary = "[GV/Admin] Cập nhật vocabulary item")
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
                .data(lessonItemService.updateVocabularyItem(itemId, request))
                .build();
    }

    @Operation(summary = "[GV/Admin] Xoá mềm vocabulary item")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @DeleteMapping(ApiPaths.API_V1 + ApiPaths.Lesson.LESSON_VOCABULARY_ITEMS + ApiPaths.Lesson.VOCABULARY_ITEM_BY_ID)
    public ApiResponse<Void> deleteVocabularyItem(@PathVariable Long itemId) {
        lessonItemService.deleteVocabularyItem(itemId);
        return ApiResponse.<Void>builder()
                .success(true)
                .message("Đã xoá từ vựng")
                .build();
    }

    // ============================================================
    // VOCABULARY EXAMPLES
    // ============================================================

    @Operation(summary = "[GV/Admin] Thêm ví dụ cho vocabulary item")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @PostMapping(ApiPaths.API_V1 + ApiPaths.Lesson.LESSON_VOCABULARY_ITEMS + ApiPaths.Lesson.LESSON_VOCABULARY_EXAMPLES)
    public ApiResponse<LessonVocabularyExampleResponse> addVocabularyExample(
            @PathVariable Long vocabItemId,
            @Valid @RequestBody LessonVocabularyExampleRequest request
    ) {
        return ApiResponse.<LessonVocabularyExampleResponse>builder()
                .success(true)
                .message("Thêm ví dụ từ vựng thành công")
                .data(lessonItemService.addVocabularyExample(vocabItemId, request))
                .build();
    }

    @Operation(summary = "[GV/Admin] Cập nhật ví dụ từ vựng")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @PutMapping(ApiPaths.API_V1 + ApiPaths.Lesson.LESSON_VOCABULARY_ITEMS + ApiPaths.Lesson.VOCAB_EXAMPLE_BY_ID)
    public ApiResponse<LessonVocabularyExampleResponse> updateVocabularyExample(
            @PathVariable Long exampleId,
            @Valid @RequestBody LessonVocabularyExampleRequest request
    ) {
        return ApiResponse.<LessonVocabularyExampleResponse>builder()
                .success(true)
                .message("Cập nhật ví dụ thành công")
                .data(lessonItemService.updateVocabularyExample(exampleId, request))
                .build();
    }

    @Operation(summary = "[GV/Admin] Xoá mềm ví dụ từ vựng")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @DeleteMapping(ApiPaths.API_V1 + ApiPaths.Lesson.LESSON_VOCABULARY_ITEMS + ApiPaths.Lesson.VOCAB_EXAMPLE_BY_ID)
    public ApiResponse<Void> deleteVocabularyExample(@PathVariable Long exampleId) {
        lessonItemService.deleteVocabularyExample(exampleId);
        return ApiResponse.<Void>builder()
                .success(true)
                .message("Đã xoá ví dụ")
                .build();
    }
}
