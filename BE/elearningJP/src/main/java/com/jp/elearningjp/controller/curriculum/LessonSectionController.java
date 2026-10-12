package com.jp.elearningjp.controller.curriculum;

import com.jp.elearningjp.dto.request.curriculum.LessonSectionCreateRequest;
import com.jp.elearningjp.dto.request.curriculum.LessonSectionUpdateRequest;
import com.jp.elearningjp.dto.response.curriculum.LessonSectionResponse;
import com.jp.elearningjp.service.curriculum.SectionService;
import com.jp.elearningjp.shared.constants.ApiPaths;
import com.jp.elearningjp.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
 * Controller quản lý Section (nhóm nội dung) trong 1 bài học.
 *
 * <p>Endpoints:</p>
 * <ul>
 *   <li>{@code GET    /api/v1/lessons/{id}/sections}      — Lấy tất cả section của lesson</li>
 *   <li>{@code GET    /api/v1/lessons/{id}/sections/{sid}} — Lấy chi tiết 1 section (kèm items)</li>
 *   <li>{@code POST   /api/v1/lessons/{id}/sections}      — [GV/Admin] Tạo section mới</li>
 *   <li>{@code PUT    /api/v1/lessons/{id}/sections/{sid}} — [GV/Admin] Cập nhật section</li>
 *   <li>{@code DELETE /api/v1/lessons/{id}/sections/{sid}} — [GV/Admin] Xoá mềm section</li>
 * </ul>
 */
@Tag(name = "Lesson Sections", description = "Quản lý section (nhóm nội dung KANJI / VOCABULARY / GRAMMAR / TEST) trong bài học")
@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LessonSectionController {

    SectionService sectionService;

    // ============================================================
    // READ
    // ============================================================

    @Operation(
            summary = "Lấy tất cả section của 1 bài học",
            description = "Trả về danh sách section sắp xếp theo orderIndex, kèm items bên trong từng section."
    )
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping(ApiPaths.API_V1 + ApiPaths.Lesson.SECTIONS)
    public ApiResponse<List<LessonSectionResponse>> getLessonSections(
            @Parameter(description = "ID của bài học", example = "1", required = true)
            @PathVariable("id") Long lessonId
    ) {
        return ApiResponse.<List<LessonSectionResponse>>builder()
                .success(true)
                .data(sectionService.getLessonSections(lessonId))
                .build();
    }

    @Operation(
            summary = "Lấy chi tiết 1 section (kèm toàn bộ items)",
            description = "Trả về section theo ID, bao gồm: kanji items (với related vocab), vocabulary items (với examples), grammar items (với examples), hoặc quiz (nếu là TEST section)."
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Tìm thấy section"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Section không tồn tại")
    })
    @GetMapping(ApiPaths.API_V1 + ApiPaths.Lesson.SECTION_BY_ID)
    public ApiResponse<LessonSectionResponse> getSection(
            @Parameter(description = "ID của bài học", example = "1", required = true)
            @PathVariable("id") Long lessonId,
            @Parameter(description = "ID của section", example = "5", required = true)
            @PathVariable("sectionId") Long sectionId
    ) {
        return ApiResponse.<LessonSectionResponse>builder()
                .success(true)
                .data(sectionService.getSection(sectionId))
                .build();
    }

    // ============================================================
    // CREATE
    // ============================================================

    @Operation(
            summary = "[Giáo viên / Admin] Tạo section mới trong bài học",
            description = """
                    Tạo 1 section mới với `sectionType` thuộc: **KANJI / VOCABULARY / GRAMMAR / TEST**.
                    orderIndex sẽ được tự động gán = (max orderIndex hiện tại + 1).
                    Sau khi tạo section, dùng các endpoint `/api/v1/lesson-items/...` để thêm kanji/vocab/grammar items.
                    """
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @PostMapping(ApiPaths.API_V1 + ApiPaths.Lesson.SECTIONS)
    public ApiResponse<LessonSectionResponse> createSection(
            @Parameter(description = "ID của bài học", example = "1", required = true)
            @PathVariable("id") Long lessonId,
            @Valid @RequestBody LessonSectionCreateRequest request
    ) {
        return ApiResponse.<LessonSectionResponse>builder()
                .success(true)
                .message("Tạo section thành công")
                .data(sectionService.createSection(lessonId, request.getSectionType(), request.getTitle()))
                .build();
    }

    // ============================================================
    // UPDATE
    // ============================================================

    @Operation(
            summary = "[Giáo viên / Admin] Cập nhật section",
            description = "Cập nhật tiêu đề và/hoặc thứ tự của section. Tất cả field đều optional — chỉ gửi field muốn đổi."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @PutMapping(ApiPaths.API_V1 + ApiPaths.Lesson.SECTION_BY_ID)
    public ApiResponse<LessonSectionResponse> updateSection(
            @Parameter(description = "ID của bài học", example = "1", required = true)
            @PathVariable("id") Long lessonId,
            @Parameter(description = "ID của section", example = "5", required = true)
            @PathVariable("sectionId") Long sectionId,
            @Valid @RequestBody LessonSectionUpdateRequest request
    ) {
        return ApiResponse.<LessonSectionResponse>builder()
                .success(true)
                .message("Cập nhật section thành công")
                .data(sectionService.updateSection(sectionId, request.getTitle(), request.getOrderIndex()))
                .build();
    }

    // ============================================================
    // DELETE
    // ============================================================

    @Operation(
            summary = "[Giáo viên / Admin] Xoá mềm section",
            description = "Soft-delete section (giáo viên/admin). Section sẽ bị ẩn khỏi API nhưng không bị xoá khỏi database. Idempotent — gọi nhiều lần không lỗi."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @DeleteMapping(ApiPaths.API_V1 + ApiPaths.Lesson.SECTION_BY_ID)
    public ApiResponse<Void> deleteSection(
            @Parameter(description = "ID của bài học", example = "1", required = true)
            @PathVariable("id") Long lessonId,
            @Parameter(description = "ID của section", example = "5", required = true)
            @PathVariable("sectionId") Long sectionId
    ) {
        sectionService.deleteSection(sectionId);
        return ApiResponse.<Void>builder()
                .success(true)
                .message("Đã xoá section thành công")
                .build();
    }
}
