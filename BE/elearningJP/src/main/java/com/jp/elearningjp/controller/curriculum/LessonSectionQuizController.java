package com.jp.elearningjp.controller.curriculum;

import com.jp.elearningjp.dto.request.curriculum.QuizQuestionCreateRequest;
import com.jp.elearningjp.dto.response.curriculum.LessonQuizResponse;
import com.jp.elearningjp.service.curriculum.QuizService;
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

/**
 * Controller CRUD Quiz gắn với 1 section TEST cụ thể trong bài học.
 *
 * <p>Endpoints:</p>
 * <ul>
 *   <li>{@code GET    /api/v1/lesson-sections/{sectionId}/quiz}
 *       — Lấy quiz của section TEST</li>
 *   <li>{@code POST   /api/v1/lesson-sections/{sectionId}/quiz}
 *       — [GV/Admin] Tạo quiz mới cho section TEST (mỗi TEST section tối đa 1 quiz)</li>
 *   <li>{@code POST   /api/v1/lesson-sections/{sectionId}/quiz/questions}
 *       — [GV/Admin] Thêm 1 câu hỏi mới vào quiz đã tồn tại</li>
 * </ul>
 *
 * <p>Endpoint xoá câu hỏi nằm ở {@link QuizController} (vì chỉ cần questionId).</p>
 */
@Tag(name = "Lesson Section Quiz", description = "Quản lý quiz gắn với section TEST trong bài học")
@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LessonSectionQuizController {

    QuizService quizService;

    // =========================================================
    // READ
    // =========================================================

    @Operation(
            summary = "Lấy quiz của 1 section TEST",
            description = "Trả về quiz kèm toàn bộ câu hỏi + lựa chọn (đã ẩn đáp án đúng)."
    )
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping(ApiPaths.API_V1 + ApiPaths.Lesson.LESSON_SECTIONS + ApiPaths.Lesson.SECTION_QUIZ)
    public ApiResponse<LessonQuizResponse> getQuizBySection(@PathVariable("sectionId") Long sectionId) {
        return ApiResponse.<LessonQuizResponse>builder()
                .success(true)
                .data(quizService.getQuizBySection(sectionId))
                .build();
    }

    // =========================================================
    // CREATE
    // =========================================================

    @Operation(
            summary = "[Giáo viên / Admin] Tạo quiz mới cho section TEST",
            description = """
                    Tạo 1 quiz mới gắn vào section có `sectionType = TEST`. Body tạo luôn câu hỏi đầu tiên + các lựa chọn.
                    Mỗi section TEST chỉ được phép có tối đa 1 quiz.
                    Sau khi tạo, dùng `POST /lesson-sections/{sectionId}/quiz/questions` để thêm câu hỏi tiếp theo.
                    """
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @PostMapping(ApiPaths.API_V1 + ApiPaths.Lesson.LESSON_SECTIONS + ApiPaths.Lesson.SECTION_QUIZ)
    public ApiResponse<LessonQuizResponse> createQuizForSection(
            @PathVariable("sectionId") Long sectionId,
            @Valid @RequestBody QuizQuestionCreateRequest request
    ) {
        return ApiResponse.<LessonQuizResponse>builder()
                .success(true)
                .message("Tạo quiz cho section thành công")
                .data(quizService.createQuizForSection(sectionId, request))
                .build();
    }

    // =========================================================
    // ADD QUESTION
    // =========================================================

    @Operation(
            summary = "[Giáo viên / Admin] Thêm 1 câu hỏi mới vào quiz đã tồn tại",
            description = """
                    Thêm 1 câu hỏi (kèm options) vào quiz của section TEST. Section phải đã có quiz.
                    orderIndex sẽ tự động = (max orderIndex hiện tại + 1).
                    """
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @PostMapping(ApiPaths.API_V1 + ApiPaths.Lesson.LESSON_SECTIONS + ApiPaths.Lesson.SECTION_QUIZ_QUESTIONS)
    public ApiResponse<LessonQuizResponse> addQuestionToQuiz(
            @PathVariable("sectionId") Long sectionId,
            @Valid @RequestBody QuizQuestionCreateRequest request
    ) {
        return ApiResponse.<LessonQuizResponse>builder()
                .success(true)
                .message("Thêm câu hỏi vào quiz thành công")
                .data(quizService.addQuestionToQuiz(sectionId, request))
                .build();
    }
}
