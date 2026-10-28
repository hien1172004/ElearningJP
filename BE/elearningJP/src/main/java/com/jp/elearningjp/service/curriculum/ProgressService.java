package com.jp.elearningjp.service.curriculum;

import com.jp.elearningjp.dto.request.curriculum.LessonCompleteRequest;
import com.jp.elearningjp.dto.response.curriculum.CourseProgressResponse;
import com.jp.elearningjp.dto.response.curriculum.ProgressResponse;
import com.jp.elearningjp.entity.curriculum.Course;
import com.jp.elearningjp.entity.curriculum.Lesson;
import com.jp.elearningjp.entity.curriculum.UserLessonProgress;
import com.jp.elearningjp.entity.user.User;
import com.jp.elearningjp.exception.AppException;
import com.jp.elearningjp.exception.ErrorCode;
import com.jp.elearningjp.mapper.curriculum.ProgressMapper;
import com.jp.elearningjp.repository.curriculum.CourseEnrollmentRepository;
import com.jp.elearningjp.repository.curriculum.CourseRepository;
import com.jp.elearningjp.repository.curriculum.LessonRepository;
import com.jp.elearningjp.repository.curriculum.QuizRepository;
import com.jp.elearningjp.repository.curriculum.UserLessonProgressRepository;
import com.jp.elearningjp.shared.enums.EnrollmentStatus;
import com.jp.elearningjp.shared.enums.ProgressStatus;
import com.jp.elearningjp.shared.event.curriculum.LessonCompletedEvent;
import com.jp.elearningjp.shared.event.curriculum.LessonStartedEvent;
import com.jp.elearningjp.shared.util.CourseAccessGuard;
import com.jp.elearningjp.shared.util.CurrentUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service("curriculumProgressService")
@Slf4j
@RequiredArgsConstructor
public class ProgressService {

    private final UserLessonProgressRepository progressRepository;
    private final LessonRepository lessonRepository;
    private final CourseRepository courseRepository;
    private final CourseEnrollmentRepository enrollmentRepository;
    private final QuizRepository quizRepository;
    private final ProgressMapper progressMapper;
    private final CurrentUserService currentUserService;
    private final CourseAccessGuard courseAccessGuard;
    private final EnrollmentService enrollmentService;
    private final ApplicationEventPublisher eventPublisher;

    // ============================================================
    // START LESSON
    // ============================================================

    /**
     * User bắt đầu học 1 lesson (chuyển sang IN_PROGRESS).
     * <p>Idempotent: nếu đã IN_PROGRESS / COMPLETED thì chỉ cập nhật lastAccessedAt.</p>
     */
    @Transactional
    public ProgressResponse startLesson(Long lessonId) {
        User currentUser = currentUserService.getCurrentUser();
        Lesson lesson = lessonRepository.findByIdAndDeletedFalse(lessonId)
                .orElseThrow(() -> new AppException(ErrorCode.LESSON_NOT_FOUND));

        courseAccessGuard.requireEnrolled(lesson.getCourse().getId());

        Optional<UserLessonProgress> existing = progressRepository
                .findByUser_IdAndLesson_IdAndDeletedFalse(currentUser.getId(), lessonId);

        UserLessonProgress progress;
        boolean isNewStart = false;
        Instant now = Instant.now();

        if (existing.isPresent()) {
            progress = existing.get();
            if (progress.getStatus() == ProgressStatus.NOT_STARTED) {
                progress.setStatus(ProgressStatus.IN_PROGRESS);
                progress.setStartedAt(now);
                isNewStart = true;
            }
            progress.setLastAccessedAt(now);
        } else {
            progress = UserLessonProgress.builder()
                    .user(currentUser)
                    .lesson(lesson)
                    .status(ProgressStatus.IN_PROGRESS)
                    .startedAt(now)
                    .lastAccessedAt(now)
                    .build();
            isNewStart = true;
        }

        UserLessonProgress saved = progressRepository.save(progress);

        if (isNewStart) {
            eventPublisher.publishEvent(new LessonStartedEvent(this,
                    currentUser.getId(), lessonId, lesson.getCourse().getId()));
        }

        return progressMapper.toResponse(saved);
    }

    // ============================================================
    // COMPLETE LESSON
    // ============================================================

    /**
     * User hoàn thành 1 lesson.
     *
     * <p>SAU REFACTOR (V20/V21): khái niệm "lesson có quiz" được xác định
     * trực tiếp bằng sự tồn tại của row trong bảng {@code lesson_quizzes}
     * (đã bỏ hoàn toàn {@code LessonSectionType.TEST}).</p>
     *
     * <ul>
     *   <li>Nếu lesson có quiz → bắt buộc phải gửi {@code score} để
     *       hoàn thành (đây là điểm quiz).</li>
     *   <li>Nếu lesson KHÔNG có quiz → KHÔNG được gửi score (vì
     *       không có quiz để chấm).</li>
     * </ul>
     */
    @Transactional
    public ProgressResponse completeLesson(Long lessonId, LessonCompleteRequest request) {
        User currentUser = currentUserService.getCurrentUser();
        Lesson lesson = lessonRepository.findByIdAndDeletedFalse(lessonId)
                .orElseThrow(() -> new AppException(ErrorCode.LESSON_NOT_FOUND));

        Long courseId = lesson.getCourse().getId();
        courseAccessGuard.requireEnrolled(courseId);

        // Xác định "có quiz" dựa trên bảng lesson_quizzes (source of truth)
        boolean isMiniTest = quizRepository.existsByLessonIdAndDeletedFalse(lessonId);

        BigDecimal score = request.getScore();
        if (isMiniTest) {
            if (score == null) {
                throw new AppException(ErrorCode.CANNOT_COMPLETE_LESSON);
            }
        } else {
            if (score != null) {
                throw new AppException(ErrorCode.INVALID_BEST_SCORE);
            }
        }

        // Lấy / tạo progress
        Optional<UserLessonProgress> existing = progressRepository
                .findByUser_IdAndLesson_IdAndDeletedFalse(currentUser.getId(), lessonId);

        UserLessonProgress progress;
        Instant now = Instant.now();

        if (existing.isPresent()) {
            progress = existing.get();
            if (progress.getStatus() == ProgressStatus.COMPLETED) {
                throw new AppException(ErrorCode.PROGRESS_ALREADY_COMPLETED);
            }
            progress.setStatus(ProgressStatus.COMPLETED);
            progress.setCompletedAt(now);
            progress.setLastAccessedAt(now);

            // Update bestScore (chỉ khi cao hơn và có quiz)
            if (isMiniTest && score != null) {
                if (progress.getBestScore() == null || score.compareTo(progress.getBestScore()) > 0) {
                    progress.setBestScore(score);
                }
            }
        } else {
            progress = UserLessonProgress.builder()
                    .user(currentUser)
                    .lesson(lesson)
                    .status(ProgressStatus.COMPLETED)
                    .startedAt(now)
                    .lastAccessedAt(now)
                    .completedAt(now)
                    .bestScore(isMiniTest ? score : null)
                    .build();
        }

        UserLessonProgress saved = progressRepository.save(progress);

        // Tính progress % sau khi complete
        int courseProgressPercent = progressRepository
                .calculateCourseProgressPercent(currentUser.getId(), courseId);

        // Publish event (EnrollmentService sẽ tự complete enrollment nếu 100%)
        eventPublisher.publishEvent(new LessonCompletedEvent(this,
                currentUser.getId(), lessonId, courseId,
                saved.getBestScore(), isMiniTest, courseProgressPercent));

        // Auto-complete enrollment nếu 100%
        if (courseProgressPercent >= 100) {
            enrollmentService.markAsCompletedIfFull(currentUser.getId(), courseId);
        }

        log.info("Lesson {} completed: userId={}, courseId={}, progressPercent={}, hasQuiz={}",
                lessonId, currentUser.getId(), courseId, courseProgressPercent, isMiniTest);

        return progressMapper.toResponse(saved);
    }

    // ============================================================
    // READ
    // ============================================================

    @Transactional(readOnly = true)
    public ProgressResponse getMyProgressForLesson(Long lessonId) {
        User currentUser = currentUserService.getCurrentUser();
        return progressRepository
                .findByUser_IdAndLesson_IdAndDeletedFalse(currentUser.getId(), lessonId)
                .map(progressMapper::toResponse)
                .orElse(null);
    }

    /**
     * Lấy toàn bộ progress của user hiện tại trong 1 course, kèm thống kê tổng quan.
     */
    @Transactional(readOnly = true)
    public CourseProgressResponse getMyProgressForCourse(Long courseId) {
        User currentUser = currentUserService.getCurrentUser();
        Course course = courseRepository.findByIdAndDeletedFalse(courseId)
                .orElseThrow(() -> new AppException(ErrorCode.COURSE_NOT_FOUND));

        List<UserLessonProgress> progresses = progressRepository
                .findByUserAndCourseWithLesson(currentUser.getId(), courseId);

        // Tính stats
        long totalLessons = lessonRepository.countByCourse_IdAndDeletedFalse(courseId);
        long completed = progresses.stream()
                .filter(p -> p.getStatus() == ProgressStatus.COMPLETED).count();
        long inProgress = progresses.stream()
                .filter(p -> p.getStatus() == ProgressStatus.IN_PROGRESS).count();
        long notStarted = Math.max(0, totalLessons - completed - inProgress);
        int progressPercent = progressRepository
                .calculateCourseProgressPercent(currentUser.getId(), courseId);

        // Check enrollment COMPLETED để xác định isCompleted chính xác theo nghiệp vụ
        boolean isCompleted = enrollmentRepository
                .findByUser_IdAndCourse_IdAndStatusAndDeletedFalse(
                        currentUser.getId(), courseId, EnrollmentStatus.COMPLETED)
                .isPresent();

        Instant firstStartedAt = progresses.stream()
                .map(UserLessonProgress::getStartedAt)
                .filter(t -> t != null)
                .min(Instant::compareTo)
                .orElse(null);
        Instant lastAccessedAt = progresses.stream()
                .map(UserLessonProgress::getLastAccessedAt)
                .filter(t -> t != null)
                .max(Instant::compareTo)
                .orElse(null);

        // Bước 4: dùng factory detailed, sau đó override isCompleted theo nghiệp vụ
        // (ProgressService: isCompleted = enrollment.status = COMPLETED,
        //  KHÔNG dựa vào completed/totalLessons như công thức mặc định của factory).
        CourseProgressResponse response = CourseProgressResponse.detailed(
                courseId,
                course.getTitle(),
                course.getJlptLevel(),
                (int) totalLessons,
                completed,
                inProgress,
                firstStartedAt,
                lastAccessedAt,
                progresses.stream().map(progressMapper::toResponse).toList(),
                progressPercent
        );
        response.setCompleted(isCompleted);
        return response;
    }

    // ============================================================
    // (Đã chuyển logic check enrollment sang CourseAccessGuard)
    // ============================================================
}
