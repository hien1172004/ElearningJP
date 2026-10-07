package com.jp.elearningjp.service.curriculum;

import com.jp.elearningjp.dto.request.curriculum.LessonCreateRequest;
import com.jp.elearningjp.dto.request.curriculum.LessonUpdateRequest;
import com.jp.elearningjp.dto.response.curriculum.CourseProgressResponse;
import com.jp.elearningjp.dto.response.curriculum.LessonDetailResponse;
import com.jp.elearningjp.entity.curriculum.Course;
import com.jp.elearningjp.entity.curriculum.Lesson;
import com.jp.elearningjp.entity.curriculum.UserLessonProgress;
import com.jp.elearningjp.entity.user.User;
import com.jp.elearningjp.exception.AppException;
import com.jp.elearningjp.exception.ErrorCode;
import com.jp.elearningjp.mapper.LessonMapper;
import com.jp.elearningjp.repository.curriculum.*;
import com.jp.elearningjp.repository.user.UserRepository;
import com.jp.elearningjp.shared.enums.ProgressStatus;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class LessonService {

    LessonRepository lessonRepository;
    CourseRepository courseRepository;
    CourseEnrollmentRepository enrollmentRepository;
    UserLessonProgressRepository progressRepository;
    QuizRepository quizRepository;
    UserRepository userRepository;
    LessonMapper lessonMapper;

    /**
     * Xem nội dung chi tiết bài học
     * BẢO VỆ BẢN QUYỀN: Bắt buộc người dùng phải là ADMIN hoặc ĐÃ ĐĂNG KÝ KHÓA HỌC
     */
    @Transactional
    public LessonDetailResponse getLessonDetail(Long lessonId) {
        User currentUser = getCurrentUserRequired();

        Lesson lesson = lessonRepository.findByIdAndDeletedFalse(lessonId)
                .orElseThrow(() -> new AppException(ErrorCode.LESSON_NOT_FOUND));

        Long courseId = lesson.getCourse().getId();

        // 1. Kiểm tra quyền truy cập bản quyền
        boolean isAdmin = currentUser.getRoles().stream()
                .anyMatch(role -> "ADMIN".equalsIgnoreCase(role.getName()));

        if (!isAdmin) {
            boolean isEnrolled = enrollmentRepository.existsByUserIdAndCourseIdAndDeletedFalse(currentUser.getId(), courseId);
            if (!isEnrolled) {
                log.warn("[SECURITY] User id={} attempted to access lesson id={} without course enrollment", currentUser.getId(), lessonId);
                throw new AppException(ErrorCode.COURSE_NOT_ENROLLED);
            }
        }

        // 2. Ghi nhận / cập nhật tiến độ học tập của người dùng (IN_PROGRESS)
        UserLessonProgress progress = progressRepository
                .findByUserIdAndLessonIdAndDeletedFalse(currentUser.getId(), lessonId)
                .orElseGet(() -> {
                    UserLessonProgress newProgress = UserLessonProgress.builder()
                            .user(currentUser)
                            .lesson(lesson)
                            .status(ProgressStatus.IN_PROGRESS)
                            .startedAt(Instant.now())
                            .lastAccessedAt(Instant.now())
                            .build();
                    return progressRepository.save(newProgress);
                });

        if (progress.getStatus() == ProgressStatus.NOT_STARTED) {
            progress.setStatus(ProgressStatus.IN_PROGRESS);
            progress.setStartedAt(Instant.now());
        }
        progress.setLastAccessedAt(Instant.now());
        progressRepository.save(progress);

        // 3. Chuẩn bị dữ liệu phản hồi
        LessonDetailResponse response = lessonMapper.toLessonDetailResponse(lesson);
        response.setHasQuiz(quizRepository.existsByLessonIdAndDeletedFalse(lessonId));
        response.setStatus(progress.getStatus());
        response.setBestScore(progress.getBestScore());
        response.setLastAccessedAt(progress.getLastAccessedAt());
        response.setCompletedAt(progress.getCompletedAt());

        return response;
    }

    /**
     * Admin thêm bài học mới vào một khóa học
     */
    @Transactional
    public LessonDetailResponse createLesson(Long courseId, LessonCreateRequest request) {
        Course course = courseRepository.findByIdAndDeletedFalse(courseId)
                .orElseThrow(() -> new AppException(ErrorCode.COURSE_NOT_FOUND));

        // Tự động gán thứ tự tiếp theo nếu chưa cung cấp
        Integer orderIndex = request.getOrderIndex();
        if (orderIndex == null || orderIndex <= 0) {
            Integer maxOrder = lessonRepository.findMaxOrderIndexByCourseId(courseId);
            orderIndex = (maxOrder != null ? maxOrder : 0) + 1;
        }

        Lesson lesson = lessonMapper.toLessonEntity(request);
        lesson.setCourse(course);
        lesson.setOrderIndex(orderIndex);

        Lesson saved = lessonRepository.save(lesson);
        log.info("[LESSON] Created lesson id={}, title='{}' in course id={}", saved.getId(), saved.getTitle(), courseId);

        LessonDetailResponse response = lessonMapper.toLessonDetailResponse(saved);
        response.setHasQuiz(false);
        response.setStatus(ProgressStatus.NOT_STARTED);
        return response;
    }

    /**
     * Admin cập nhật thông tin bài học
     */
    @Transactional
    public LessonDetailResponse updateLesson(Long lessonId, LessonUpdateRequest request) {
        Lesson lesson = lessonRepository.findByIdAndDeletedFalse(lessonId)
                .orElseThrow(() -> new AppException(ErrorCode.LESSON_NOT_FOUND));

        lessonMapper.updateLessonFromRequest(request, lesson);
        Lesson updated = lessonRepository.save(lesson);
        log.info("[LESSON] Updated lesson id={}", updated.getId());

        LessonDetailResponse response = lessonMapper.toLessonDetailResponse(updated);
        response.setHasQuiz(quizRepository.existsByLessonIdAndDeletedFalse(lessonId));
        return response;
    }

    /**
     * Admin xóa mềm bài học
     */
    @Transactional
    public void deleteLesson(Long lessonId) {
        Lesson lesson = lessonRepository.findByIdAndDeletedFalse(lessonId)
                .orElseThrow(() -> new AppException(ErrorCode.LESSON_NOT_FOUND));

        lesson.softDelete();
        lessonRepository.save(lesson);
        log.info("[LESSON] Soft-deleted lesson id={}", lessonId);
    }

    /**
     * Học viên đánh dấu hoàn thành bài học thủ công (cho các bài lý thuyết không có quiz)
     * Trả về % tiến độ cập nhật của khóa học
     */
    @Transactional
    public CourseProgressResponse completeLesson(Long lessonId) {
        User currentUser = getCurrentUserRequired();

        Lesson lesson = lessonRepository.findByIdAndDeletedFalse(lessonId)
                .orElseThrow(() -> new AppException(ErrorCode.LESSON_NOT_FOUND));

        Long courseId = lesson.getCourse().getId();

        boolean isEnrolled = enrollmentRepository.existsByUserIdAndCourseIdAndDeletedFalse(currentUser.getId(), courseId);
        if (!isEnrolled) {
            throw new AppException(ErrorCode.COURSE_NOT_ENROLLED);
        }

        UserLessonProgress progress = progressRepository
                .findByUserIdAndLessonIdAndDeletedFalse(currentUser.getId(), lessonId)
                .orElseGet(() -> UserLessonProgress.builder()
                        .user(currentUser)
                        .lesson(lesson)
                        .startedAt(Instant.now())
                        .build());

        progress.setStatus(ProgressStatus.COMPLETED);
        progress.setCompletedAt(Instant.now());
        progress.setLastAccessedAt(Instant.now());
        progressRepository.save(progress);

        // Tính toán lại % tiến độ khóa học
        int totalLessons = (int) lessonRepository.countByCourseIdAndDeletedFalse(courseId);
        int completedCount = (int) progressRepository.countCompletedLessonsByCourse(
                currentUser.getId(), courseId, ProgressStatus.COMPLETED);

        BigDecimal progressPercent = totalLessons > 0
                ? BigDecimal.valueOf((double) completedCount / totalLessons * 100).setScale(1, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        log.info("[PROGRESS] User id={} completed lesson id={}, course progress={} %", currentUser.getId(), lessonId, progressPercent);

        return CourseProgressResponse.builder()
                .courseId(courseId)
                .courseTitle(lesson.getCourse().getTitle())
                .totalLessons(totalLessons)
                .completedLessons(completedCount)
                .progressPercent(progressPercent)
                .isCompleted(totalLessons > 0 && completedCount >= totalLessons)
                .build();
    }

    // =========================================================
    // HELPER METHODS
    // =========================================================

    public User getCurrentUserRequired() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        return userRepository.findUserByEmail(auth.getName())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }
}
