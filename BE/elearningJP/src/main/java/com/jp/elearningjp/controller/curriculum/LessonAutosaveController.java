package com.jp.elearningjp.controller.curriculum;

import com.jp.elearningjp.dto.request.curriculum.LessonAutosaveRequest;
import com.jp.elearningjp.dto.response.curriculum.LessonAutosaveResponse;
import com.jp.elearningjp.service.curriculum.LessonAutosaveService;
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

@Tag(name = "Lesson Autosave", description = "Tự lưu nội dung chưa lưu theo người và theo bài (chỉ người viết thấy)")
@RestController
@RequestMapping(ApiPaths.API_V1 + ApiPaths.Lesson.BASE)
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@SecurityRequirement(name = "bearerAuth")
public class LessonAutosaveController {

    LessonAutosaveService lessonAutosaveService;

    @Operation(summary = "[Admin/Teacher] Bản chưa lưu của tôi trong bài",
            description = "Chỉ của người dùng hiện tại; quản trị viên hoặc giáo viên đang được giao khóa")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @GetMapping(ApiPaths.Lesson.AUTOSAVES)
    public ApiResponse<List<LessonAutosaveResponse>> list(@PathVariable Long id) {
        return ApiResponse.<List<LessonAutosaveResponse>>builder()
                .success(true)
                .data(lessonAutosaveService.list(id))
                .build();
    }

    @Operation(summary = "[Admin/Teacher] Tự lưu form đang soạn dở",
            description = "Ghi đè theo form (tối đa 64 KB, 50 bản / bài / người). Không tăng version nội dung bài, "
                    + "không ai khác thấy; vẫn ghi được khi bài đang chờ duyệt hoặc đang được quản trị viên sửa")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @PutMapping(ApiPaths.Lesson.AUTOSAVE_BY_KEY)
    public ApiResponse<Void> save(
            @PathVariable Long id,
            @PathVariable String formKey,
            @Valid @RequestBody LessonAutosaveRequest request
    ) {
        lessonAutosaveService.save(id, formKey, request.getContent());
        return ApiResponse.<Void>builder().success(true).build();
    }

    @Operation(summary = "[Admin/Teacher] Bỏ bản chưa lưu của form",
            description = "Gọi khi lưu thành công hoặc người dùng bấm Bỏ. Không có thì bỏ qua")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @DeleteMapping(ApiPaths.Lesson.AUTOSAVE_BY_KEY)
    public ApiResponse<Void> delete(@PathVariable Long id, @PathVariable String formKey) {
        lessonAutosaveService.delete(id, formKey);
        return ApiResponse.<Void>builder().success(true).build();
    }
}
