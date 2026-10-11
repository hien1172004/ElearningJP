package com.jp.elearningjp.controller.curriculum;

import com.jp.elearningjp.dto.request.curriculum.LessonKanjiItemCreateRequest;
import com.jp.elearningjp.dto.request.curriculum.LessonKanjiItemUpdateRequest;
import com.jp.elearningjp.dto.request.curriculum.LessonKanjiRelatedVocabRequest;
import com.jp.elearningjp.dto.response.curriculum.LessonKanjiItemResponse;
import com.jp.elearningjp.dto.response.curriculum.LessonRelatedVocabResponse;
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

@Tag(name = "Lesson Kanji Items", description = "Quản lý chữ Hán + từ vựng liên quan trong section KANJI")
@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LessonKanjiItemController {

    LessonItemService lessonItemService;

    // ============================================================
    // KANJI ITEM CRUD
    // ============================================================

    @Operation(summary = "[GV/Admin] Thêm 1 chữ Hán vào section")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @PostMapping(ApiPaths.API_V1 + ApiPaths.Lesson.LESSON_SECTIONS + ApiPaths.Lesson.KANJI_ITEMS)
    public ApiResponse<LessonKanjiItemResponse> addKanjiItem(
            @PathVariable Long sectionId,
            @Valid @RequestBody LessonKanjiItemCreateRequest request
    ) {
        return ApiResponse.<LessonKanjiItemResponse>builder()
                .success(true)
                .message("Thêm chữ Hán vào section thành công")
                .data(lessonItemService.addKanjiItem(sectionId, request))
                .build();
    }

    @Operation(summary = "[GV/Admin] Cập nhật kanji item")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @PutMapping(ApiPaths.API_V1 + ApiPaths.Lesson.LESSON_KANJI_ITEMS + ApiPaths.Lesson.KANJI_ITEM_BY_ID)
    public ApiResponse<LessonKanjiItemResponse> updateKanjiItem(
            @PathVariable Long itemId,
            @Valid @RequestBody LessonKanjiItemUpdateRequest request
    ) {
        return ApiResponse.<LessonKanjiItemResponse>builder()
                .success(true)
                .message("Cập nhật chữ Hán thành công")
                .data(lessonItemService.updateKanjiItem(itemId, request))
                .build();
    }

    @Operation(summary = "[GV/Admin] Xoá mềm kanji item")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @DeleteMapping(ApiPaths.API_V1 + ApiPaths.Lesson.LESSON_KANJI_ITEMS + ApiPaths.Lesson.KANJI_ITEM_BY_ID)
    public ApiResponse<Void> deleteKanjiItem(@PathVariable Long itemId) {
        lessonItemService.deleteKanjiItem(itemId);
        return ApiResponse.<Void>builder()
                .success(true)
                .message("Đã xoá chữ Hán")
                .build();
    }

    // ============================================================
    // KANJI → RELATED VOCABULARY
    // ============================================================

    @Operation(summary = "[GV/Admin] Thêm từ vựng liên quan cho chữ Hán")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @PostMapping(ApiPaths.API_V1 + ApiPaths.Lesson.LESSON_KANJI_ITEMS + ApiPaths.Lesson.KANJI_RELATED_VOCAB)
    public ApiResponse<LessonRelatedVocabResponse> addRelatedVocab(
            @PathVariable Long kanjiItemId,
            @Valid @RequestBody LessonKanjiRelatedVocabRequest request
    ) {
        return ApiResponse.<LessonRelatedVocabResponse>builder()
                .success(true)
                .message("Thêm từ vựng liên quan thành công")
                .data(lessonItemService.addRelatedVocab(kanjiItemId, request))
                .build();
    }

    @Operation(summary = "[GV/Admin] Xoá mềm từ vựng liên quan")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @DeleteMapping(ApiPaths.API_V1 + ApiPaths.Lesson.LESSON_KANJI_ITEMS + ApiPaths.Lesson.KANJI_RELATED_VOCAB_BY_ID)
    public ApiResponse<Void> deleteRelatedVocab(@PathVariable Long relatedVocabId) {
        lessonItemService.deleteRelatedVocab(relatedVocabId);
        return ApiResponse.<Void>builder()
                .success(true)
                .message("Đã xoá từ vựng liên quan")
                .build();
    }
}
