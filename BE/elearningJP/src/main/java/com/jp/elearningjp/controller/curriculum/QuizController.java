package com.jp.elearningjp.controller.curriculum;

import com.jp.elearningjp.dto.request.curriculum.QuizQuestionCreateRequest;
import com.jp.elearningjp.dto.request.curriculum.QuizSubmitRequest;
import com.jp.elearningjp.dto.response.curriculum.LessonQuizResponse;
import com.jp.elearningjp.dto.response.curriculum.QuizQuestionResponse;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Lesson Quiz", description = "Bài kiểm tra trắc nghiệm củng cố kiến thức sau mỗi bài học")
@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class QuizController {

    QuizService quizService;

    @Operation(summary = "Lấy đề thi trắc nghiệm của bài học", description = "Chỉ học viên ĐÃ ĐĂNG KÝ KHÓA HỌC mới có quyền lấy đề. Đề thi được bảo mật, ẩn hoàn toàn đáp án đúng.")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping(ApiPaths.API_V1 + ApiPaths.Lesson.BASE + ApiPaths.Lesson.QUIZ)
    public ApiResponse<LessonQuizResponse> getLessonQuiz(@PathVariable Long id) {
        return ApiResponse.<LessonQuizResponse>builder()
                .success(true)
                .data(quizService.getLessonQuiz(id))
                .build();
    }

    @Operation(summary = "Nộp bài kiểm tra trắc nghiệm & Chấm điểm tự động", description = "Chấm điểm trực tiếp tại Server. Nếu đạt >= 80% (hoặc điểm passing) sẽ tự động chuyển bài học sang trạng thái COMPLETED.")
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

    @Operation(summary = "[Admin] Thêm câu hỏi trắc nghiệm vào bài học", description = "Thêm câu hỏi và 4 lựa chọn A, B, C, D (đánh dấu đáp án đúng). Tự động tạo bài Quiz nếu bài học chưa có.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(ApiPaths.API_V1 + ApiPaths.Lesson.BASE + ApiPaths.Lesson.QUIZ_QUESTIONS)
    public ApiResponse<QuizQuestionResponse> addQuestionToLesson(
            @PathVariable Long id,
            @Valid @RequestBody QuizQuestionCreateRequest request
    ) {
        return ApiResponse.<QuizQuestionResponse>builder()
                .success(true)
                .message("Thêm câu hỏi trắc nghiệm thành công")
                .data(quizService.addQuestionToLesson(id, request))
                .build();
    }

    @Operation(summary = "[Admin] Xóa câu hỏi trắc nghiệm", description = "Xóa mềm câu hỏi trắc nghiệm khỏi bài kiểm tra")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping(ApiPaths.API_V1 + "/quiz/questions/{questionId}")
    public ApiResponse<Void> deleteQuestion(@PathVariable Long questionId) {
        quizService.deleteQuestion(questionId);
        return ApiResponse.<Void>builder()
                .success(true)
                .message("Đã xóa câu hỏi thành công")
                .build();
    }
}
