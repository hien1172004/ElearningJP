package com.jp.elearningjp.controller.curriculum;

import com.jp.elearningjp.dto.request.curriculum.LessonSpeakingAssignRequest;
import com.jp.elearningjp.dto.response.curriculum.LessonSpeakingResponse;
import com.jp.elearningjp.entity.user.User;
import com.jp.elearningjp.service.curriculum.LessonSpeakingService;
import com.jp.elearningjp.shared.constants.ApiPaths;
import com.jp.elearningjp.shared.response.ApiResponse;
import com.jp.elearningjp.shared.util.CurrentUserService;
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

@Tag(name = "Lesson Speaking", description = "Quản lý và truy xuất kịch bản luyện nói hội thoại trong bài học")
@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LessonSpeakingController {

    LessonSpeakingService lessonSpeakingService;
    CurrentUserService currentUserService;

    @Operation(summary = "Xem danh sách kịch bản luyện nói của bài học", description = "Lấy danh sách các kịch bản hội thoại gắn với bài học kèm trạng thái phiên nói của học viên")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping(ApiPaths.API_V1 + ApiPaths.Lesson.BASE + ApiPaths.Lesson.SPEAKING)
    public ApiResponse<List<LessonSpeakingResponse>> getSpeakingListForLesson(@PathVariable("id") Long lessonId) {
        User currentUser = currentUserService.getCurrentUserOrNull();
        Long userId = (currentUser != null) ? currentUser.getId() : null;

        List<LessonSpeakingResponse> data = lessonSpeakingService.getSpeakingListForLesson(lessonId, userId);
        return ApiResponse.<List<LessonSpeakingResponse>>builder()
                .success(true)
                .data(data)
                .build();
    }

    @Operation(summary = "[Admin/Teacher] Gán kịch bản luyện nói vào bài học", description = "Gán kịch bản AI có sẵn vào bài học hoặc cập nhật số lượt nói tối thiểu")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @PostMapping(ApiPaths.API_V1 + ApiPaths.Lesson.BASE + ApiPaths.Lesson.SPEAKING)
    public ApiResponse<LessonSpeakingResponse> assignSpeakingScenario(
            @PathVariable("id") Long lessonId,
            @Valid @RequestBody LessonSpeakingAssignRequest request) {
        return ApiResponse.<LessonSpeakingResponse>builder()
                .success(true)
                .message("Gán kịch bản luyện nói vào bài học thành công")
                .data(lessonSpeakingService.assignSpeakingScenario(lessonId, request))
                .build();
    }

    @Operation(summary = "[Admin/Teacher] AI tự động tạo kịch bản từ nội dung bài học", description = "AI phân tích từ vựng và ngữ pháp của bài học để tạo ra kịch bản hội thoại phù hợp")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @PostMapping(ApiPaths.API_V1 + ApiPaths.Lesson.BASE + ApiPaths.Lesson.SPEAKING_GENERATE)
    public ApiResponse<LessonSpeakingResponse> generateSpeakingScenario(@PathVariable("id") Long lessonId) {
        return ApiResponse.<LessonSpeakingResponse>builder()
                .success(true)
                .message("AI đã tạo và gán kịch bản hội thoại từ bài học thành công")
                .data(lessonSpeakingService.generateScenarioFromLesson(lessonId))
                .build();
    }

    @Operation(summary = "[Admin/Teacher] Gỡ kịch bản luyện nói khỏi bài học")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @DeleteMapping(ApiPaths.API_V1 + ApiPaths.Lesson.BASE + ApiPaths.Lesson.SPEAKING)
    public ApiResponse<Void> removeSpeakingScenario(
            @PathVariable("id") Long lessonId,
            @RequestParam(value = "scenarioId", required = false) Long scenarioId) {
        lessonSpeakingService.removeSpeakingScenario(lessonId, scenarioId);
        return ApiResponse.<Void>builder()
                .success(true)
                .message("Gỡ kịch bản luyện nói khỏi bài học thành công")
                .build();
    }

    @Operation(summary = "[Admin/Teacher] Gỡ 1 kịch bản cụ thể khỏi bài học")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @DeleteMapping(ApiPaths.API_V1 + ApiPaths.Lesson.BASE + ApiPaths.Lesson.SPEAKING + "/{scenarioId}")
    public ApiResponse<Void> removeSingleSpeakingScenario(
            @PathVariable("id") Long lessonId,
            @PathVariable("scenarioId") Long scenarioId) {
        lessonSpeakingService.removeSpeakingScenario(lessonId, scenarioId);
        return ApiResponse.<Void>builder()
                .success(true)
                .message("Gỡ kịch bản luyện nói khỏi bài học thành công")
                .build();
    }
}
