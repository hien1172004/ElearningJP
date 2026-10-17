package com.jp.elearningjp.controller.curriculum;

import com.jp.elearningjp.dto.request.curriculum.ItemBulkReorderRequest;
import com.jp.elearningjp.dto.request.curriculum.QuizQuestionCreateRequest;
import com.jp.elearningjp.dto.request.curriculum.QuizQuestionUpdateRequest;
import com.jp.elearningjp.dto.request.curriculum.QuizSubmitRequest;
import com.jp.elearningjp.dto.response.curriculum.LessonQuizManageResponse;
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

import java.util.List;

/**
 * Controller lấy đề và nộp bài Quiz theo {@code lessonId}.
 *
 * <p><b>Sau refactor V20/V21:</b> Quiz giờ chỉ liên kết trực tiếp với Lesson (lesson_id
 * UNIQUE). Không còn qua section TEST trung gian. Endpoint vẫn nhận
 * {@code lessonId}, server tự tìm quiz của lesson rồi trả về.</p>
 */
@Tag(name = "Lesson Quiz", description = "Lấy đề và nộp bài quiz theo lessonId")
@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class QuizController {

    QuizService quizService;

    @Operation(
            summary = "Lấy đề thi trắc nghiệm của bài học",
            description = "Chỉ học viên ĐÃ ĐĂNG KÝ KHÓA HỌC mới có quyền lấy đề. Server tự tìm quiz của lesson. Đề thi được bảo mật, ẩn hoàn toàn đáp án đúng."
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
            summary = "[Giáo viên / Admin] Lấy đề quiz kèm đáp án đúng để quản lý",
            description = "Tương tự GET /lessons/{id}/quiz nhưng trả thêm đáp án đúng cho từng câu hỏi (correct, correctOptionId, correctTextAnswer, correctMatchingAnswers, explanation). Chỉ Admin/Teacher (owner course) mới có quyền truy cập — dùng cho trang quản lý câu hỏi."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @GetMapping(ApiPaths.API_V1 + ApiPaths.Lesson.BASE + ApiPaths.Lesson.QUIZ_MANAGE)
    public ApiResponse<LessonQuizManageResponse> getLessonQuizManage(@PathVariable Long id) {
        return ApiResponse.<LessonQuizManageResponse>builder()
                .success(true)
                .data(quizService.getLessonQuizManage(id))
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

    @Operation(
            summary = "[Giáo viên / Admin] Thêm câu hỏi trắc nghiệm vào bài học",
            description = "Thêm 1 câu hỏi (kèm options nếu MULTIPLE_CHOICE/MATCHING) vào quiz của lesson. Nếu lesson chưa có quiz thì tự tạo. Teacher chỉ thêm được câu hỏi vào lesson thuộc course do mình tạo."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
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

    @Operation(
            summary = "[Giáo viên / Admin] Cập nhật câu hỏi trắc nghiệm",
            description = "Sửa nội dung câu hỏi quiz. Hỗ trợ partial update — chỉ field nào gửi mới thay đổi. Nếu gửi options sẽ THAY THẾ toàn bộ options cũ. Teacher chỉ sửa được câu hỏi thuộc course do mình tạo."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @PutMapping(ApiPaths.API_V1 + ApiPaths.Quiz.BASE + ApiPaths.Quiz.QUESTION_BY_ID)
    public ApiResponse<QuizQuestionResponse> updateQuestion(
            @PathVariable Long id,
            @Valid @RequestBody QuizQuestionUpdateRequest request
    ) {
        return ApiResponse.<QuizQuestionResponse>builder()
                .success(true)
                .message("Cập nhật câu hỏi trắc nghiệm thành công")
                .data(quizService.updateQuestion(id, request))
                .build();
    }

    @Operation(
            summary = "[Giáo viên / Admin] Xoá mềm câu hỏi trắc nghiệm",
            description = "Xoá mềm 1 câu hỏi theo questionId. Teacher chỉ xoá được câu hỏi thuộc course do mình tạo. Idempotent — gọi lại với câu đã xoá sẽ trả 404."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @DeleteMapping(ApiPaths.API_V1 + ApiPaths.Quiz.BASE + ApiPaths.Quiz.QUESTION_BY_ID)
    public ApiResponse<Void> deleteQuestion(@PathVariable Long id) {
        quizService.deleteQuestion(id);
        return ApiResponse.<Void>builder()
                .success(true)
                .message("Đã xoá câu hỏi thành công")
                .build();
    }

    @Operation(
            summary = "[Giáo viên / Admin] Sắp xếp lại thứ tự hàng loạt các câu hỏi trong quiz (drag-drop)",
            description = "Reorder nhiều câu hỏi cùng lúc. Body chứa danh sách questionId theo thứ tự MỚI. " +
                    "Danh sách phải chứa đúng tất cả câu hỏi active của quiz. (Teacher chỉ reorder trong course do mình tạo)"
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @PostMapping(ApiPaths.API_V1 + ApiPaths.Lesson.BASE + ApiPaths.Lesson.QUIZ_QUESTIONS_REORDER)
    public ApiResponse<List<QuizQuestionResponse>> reorderQuizQuestions(
            @PathVariable Long id,
            @Valid @RequestBody ItemBulkReorderRequest request
    ) {
        return ApiResponse.<List<QuizQuestionResponse>>builder()
                .success(true)
                .message("Sắp xếp lại thứ tự câu hỏi thành công")
                .data(quizService.reorderQuizQuestions(id, request))
                .build();
    }
}
