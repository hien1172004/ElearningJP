package com.jp.elearningjp.controller.curriculum;

import com.jp.elearningjp.dto.request.curriculum.QuizSubmitRequest;
import com.jp.elearningjp.dto.response.curriculum.LessonQuizResponse;
import com.jp.elearningjp.dto.response.curriculum.QuizSubmitResponse;
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
import org.springframework.web.bind.annotation.*;

/**
 * Controller lấy đề và nộp bài Quiz theo {@code lessonId}.
 *
 * <p><b>Sau refactor (V13/V14):</b> Quiz giờ gắn với TEST section. 2 endpoint dưới
 * vẫn nhận {@code lessonId} để giữ tương thích ngược — server tự tìm TEST section
 * của lesson rồi trả quiz tương ứng. Nếu lesson chưa có TEST section hoặc TEST
 * section chưa có quiz -> lỗi nghiệp vụ rõ ràng.</p>
 *
 * <p>Endpoint CRUD quiz / question theo section xem
 * {@link LessonSectionQuizController}.</p>
 */
@Tag(name = "Lesson Quiz", description = "Lấy đề và nộp bài quiz theo lessonId (tự động tìm TEST section)")
@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class QuizController {

    QuizService quizService;

    @Operation(
            summary = "Lấy đề thi trắc nghiệm của bài học",
            description = "Chỉ học viên ĐÃ ĐĂNG KÝ KHÓA HỌC mới có quyền lấy đề. Server tự tìm TEST section của lesson. Đề thi được bảo mật, ẩn hoàn toàn đáp án đúng."
    )
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping(ApiPaths.API_V1 + ApiPaths.Lesson.BASE + ApiPaths.Lesson.QUIZ)
    public ApiResponse<LessonQuizResponse> getLessonQuiz(@PathVariable Long id) {
        return ApiResponse.<LessonQuizResponse>builder()
                .success(true)
                .data(quizService.getLessonQuiz(id))
                .build();
    }

    @Operation(
            summary = "Nộp bài kiểm tra trắc nghiệm & Chấm điểm tự động",
            description = "Chấm điểm trực tiếp tại Server. Nếu đạt >= 80% (hoặc điểm passing) sẽ tự động chuyển bài học sang trạng thái COMPLETED."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping(ApiPaths.API_V1 + ApiPaths.Lesson.BASE + ApiPaths.Lesson.QUIZ_SUBMIT)
    public ApiResponse<QuizSubmitResponse> submitQuiz(
            @PathVariable Long id,
            @Valid @RequestBody QuizSubmitRequest request
    ) {
        return ApiResponse.<QuizSubmitResponse>builder()
                .success(true)
                .message("Đã chấm điểm bài thi thành công!")
                .data(quizService.submitQuiz(id, request))
                .build();
    }
}
