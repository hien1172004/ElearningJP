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
import com.jp.elearningjp.repository.curriculum.UserLessonProgressRepository;
import com.jp.elearningjp.shared.enums.EnrollmentStatus;
import com.jp.elearningjp.shared.enums.LessonType;
import com.jp.elearningjp.shared.enums.ProgressStatus;
import com.jp.elearningjp.shared.event.curriculum.LessonCompletedEvent;
import com.jp.elearningjp.shared.event.curriculum.LessonStartedEvent;
import com.jp.elearningjp.shared.util.CurrentUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProgressService {

    private final UserLessonProgressRepository progressRepository;
    private final LessonRepository lessonRepository;
    private final CourseRepository courseRepository;
    private final CourseEnrollmentRepository enrollmentRepository;
    private final ProgressMapper progressMapper;
    private final CurrentUserService currentUserService;
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

        requireEnrolledUser(currentUser.getId(), lesson.getCourse().getId());

        var existing = progressRepository
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

    @Transactional
    public ProgressResponse completeLesson(Long lessonId, LessonCompleteRequest request) {
        User currentUser = currentUserService.getCurrentUser();
        Lesson lesson = lessonRepository.findByIdAndDeletedFalse(lessonId)
                .orElseThrow(() -> new AppException(ErrorCode.LESSON_NOT_FOUND));

        Long courseId = lesson.getCourse().getId();
        requireEnrolledUser(currentUser.getId(), courseId);

        // Validate score theo lesson type
        BigDecimal score = request.getScore();
        boolean isMiniTest = lesson.getLessonType() == LessonType.MINI_TEST;
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
        var existing = progressRepository
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

            // Update bestScore (chỉ khi cao hơn)
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

        log.info("Lesson {} completed: userId={}, courseId={}, progressPercent={}",
                lessonId, currentUser.getId(), courseId, courseProgressPercent);

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

        return CourseProgressResponse.builder()
                .courseId(courseId)
                .courseTitle(course.getTitle())
                .courseLevel(course.getJlptLevel())
                .totalLessons((int) totalLessons)
                .completedLessons((int) completed)
                .inProgressLessons((int) inProgress)
                .notStartedLessons((int) notStarted)
                .progressPercent(BigDecimal.valueOf(progressPercent))
                .isCompleted(isCompleted)
                .enrolledAt(firstStartedAt)
                .lastAccessedAt(lastAccessedAt)
                .progress(progresses.stream().map(progressMapper::toResponse).toList())
                .build();
    }

    // ============================================================
    // PRIVATE HELPERS
    // ============================================================

    /**
     * Check user hiện tại có enrollment ACTIVE cho course không.
     */
    private void requireEnrolledUser(Long userId, Long courseId) {
        boolean isEnrolled = enrollmentRepository
                .existsByUser_IdAndCourse_IdAndStatusAndDeletedFalse(
                        userId, courseId, EnrollmentStatus.ACTIVE);
        if (!isEnrolled) {
            throw new AppException(ErrorCode.NOT_ENROLLED);
        }
    }
}