package com.jp.elearningjp.controller.curriculum;

import com.jp.elearningjp.dto.request.curriculum.ItemBulkReorderRequest;
import com.jp.elearningjp.dto.request.curriculum.LessonKanjiItemCreateRequest;
import com.jp.elearningjp.dto.request.curriculum.LessonKanjiItemUpdateRequest;
import com.jp.elearningjp.dto.response.curriculum.LessonKanjiItemResponse;
import com.jp.elearningjp.service.curriculum.section.KanjiSectionItemService;
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

import java.util.List;

@Tag(name = "Lesson Kanji Items", description = "Quản lý chữ Hán trong section KANJI. "
        + "Related vocab do giáo viên chọn được nhúng trong payload create/update của kanji item "
        + "(xem LessonKanjiItemCreateRequest.relatedVocabulary / LessonKanjiItemUpdateRequest.relatedVocabulary).")
@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LessonKanjiItemController {

    KanjiSectionItemService kanjiSectionItemService;

    // ============================================================
    // KANJI ITEM CRUD (4 endpoints chính)
    // V25+: giáo viên có thể nhúng relatedVocabulary trong payload create/update
    //       (mỗi entry có thể chọn vocabId có sẵn hoặc tạo mới qua newVocab).
    //       Nếu giáo viên chưa chọn vocab nào → response tự động fallback
    //       derive từ bảng vocabulary_characters.
    //       Khi xoá kanji item → cascade soft-delete toàn bộ related vocab thuộc item đó.
    // ============================================================

    @Operation(summary = "[GV/Admin] Thêm 1 chữ Hán vào section (kèm relatedVocab do giáo viên chọn)")
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
                .data(kanjiSectionItemService.addKanjiItem(sectionId, request))
                .build();
    }

    @Operation(summary = "Lấy chi tiết 1 chữ Hán trong section (kèm related vocab đã chọn)")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping(ApiPaths.API_V1 + ApiPaths.Lesson.LESSON_KANJI_ITEMS + ApiPaths.Lesson.KANJI_ITEM_BY_ID)
    public ApiResponse<LessonKanjiItemResponse> getKanjiItem(@PathVariable Long itemId) {
        return ApiResponse.<LessonKanjiItemResponse>builder()
                .success(true)
                .message("Lấy chi tiết chữ Hán thành công")
                .data(kanjiSectionItemService.getKanjiItem(itemId))
                .build();
    }

    @Operation(summary = "[GV/Admin] Cập nhật kanji item (orderIndex, note, relatedVocab)")
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
                .data(kanjiSectionItemService.updateKanjiItem(itemId, request))
                .build();
    }

    @Operation(summary = "[GV/Admin] Xoá mềm kanji item (cascade soft-delete tất cả related vocab)")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @DeleteMapping(ApiPaths.API_V1 + ApiPaths.Lesson.LESSON_KANJI_ITEMS + ApiPaths.Lesson.KANJI_ITEM_BY_ID)
    public ApiResponse<Void> deleteKanjiItem(@PathVariable Long itemId) {
        kanjiSectionItemService.deleteKanjiItem(itemId);
        return ApiResponse.<Void>builder()
                .success(true)
                .message("Đã xoá chữ Hán (kèm related vocab)")
                .build();
    }

    @Operation(summary = "[GV/Admin] Sắp xếp lại thứ tự hàng loạt các kanji item trong section (drag-drop)")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @PostMapping(ApiPaths.API_V1 + ApiPaths.Lesson.LESSON_SECTIONS + ApiPaths.Lesson.KANJI_ITEMS_REORDER)
    public ApiResponse<List<LessonKanjiItemResponse>> reorderKanjiItems(
            @PathVariable Long sectionId,
            @Valid @RequestBody ItemBulkReorderRequest request
    ) {
        return ApiResponse.<List<LessonKanjiItemResponse>>builder()
                .success(true)
                .message("Sắp xếp lại thứ tự kanji item thành công")
                .data(kanjiSectionItemService.reorderKanjiItems(sectionId, request))
                .build();
    }
}