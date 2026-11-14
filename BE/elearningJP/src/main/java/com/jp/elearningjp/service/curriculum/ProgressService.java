package com.jp.elearningjp.service.curriculum;

import com.jp.elearningjp.dto.request.curriculum.LessonProgressUpdateRequest;
import com.jp.elearningjp.dto.response.curriculum.CourseProgressResponse;
import com.jp.elearningjp.dto.response.curriculum.ProgressResponse;
import com.jp.elearningjp.entity.curriculum.Course;
import com.jp.elearningjp.entity.curriculum.Lesson;
import com.jp.elearningjp.entity.curriculum.Quiz;
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
import com.jp.elearningjp.shared.enums.LessonSectionType;
import com.jp.elearningjp.shared.enums.ProgressStatus;
import com.jp.elearningjp.shared.event.curriculum.LessonStartedEvent;
import com.jp.elearningjp.service.curriculum.snapshot.PublishedLessonResolver;
import com.jp.elearningjp.service.curriculum.snapshot.QuizSnapshot;
import com.jp.elearningjp.shared.util.CourseAccessGuard;
import com.jp.elearningjp.shared.util.CurrentUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private final ApplicationEventPublisher eventPublisher;
    private final LessonProgressRecorder lessonProgressRecorder;
    private final LessonDraftService lessonDraftService;
    private final PublishedLessonResolver publishedLessonResolver;

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
        Lesson lesson = resolveLessonForLearning(lessonId);

        // Khoá dòng progress (tạo nếu chưa có): 2 request đồng thời không còn cùng INSERT → 500.
        UserLessonProgress progress = lessonProgressRecorder.lockOrCreate(currentUser.getId(), lessonId);
        boolean isNewStart = false;
        Instant now = Instant.now();

        if (progress.getStatus() == ProgressStatus.NOT_STARTED) {
            progress.setStatus(ProgressStatus.IN_PROGRESS);
            progress.setStartedAt(now);
            isNewStart = true;
        }
        progress.setLastAccessedAt(now);

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
     * <p>Lesson có quiz (có row trong {@code lesson_quizzes}): chỉ hoàn thành được khi
     * {@code bestScore} do <b>server chấm</b> lúc nộp quiz đã đạt {@code passingScorePercent}.
     * Client không gửi điểm.</p>
     */
    @Transactional
    public ProgressResponse completeLesson(Long lessonId) {
        User currentUser = currentUserService.getCurrentUser();
        Optional<PublishedLessonResolver.PublishedLesson> published = publishedLessonResolver.forLearning(lessonId);
        Lesson lesson = published.map(PublishedLessonResolver.PublishedLesson::lesson)
                .orElseGet(() -> requireLiveLessonForLearning(lessonId));

        Long courseId = lesson.getCourse().getId();

        // Khoá dòng: request trùng chờ request đầu xong rồi nhận PROGRESS_ALREADY_COMPLETED thay vì 500.
        UserLessonProgress progress = lessonProgressRecorder.lockOrCreate(currentUser.getId(), lessonId);

        if (progress.getStatus() == ProgressStatus.COMPLETED) {
            throw new AppException(ErrorCode.PROGRESS_ALREADY_COMPLETED);
        }

        // Học viên của khóa đã xuất bản: quiz theo bản đã duyệt
        Optional<Integer> passingScore = published.isPresent()
                ? Optional.ofNullable(published.get().snapshot().getQuiz()).map(QuizSnapshot::getPassingScorePercent)
                : quizRepository.findByDraftIdAndDeletedFalse(lessonDraftService.readDraftId(lesson))
                        .map(Quiz::getPassingScorePercent);
        boolean isMiniTest = passingScore.isPresent();
        if (isMiniTest) {
            BigDecimal bestScore = progress.getBestScore();
            if (bestScore == null || bestScore.compareTo(BigDecimal.valueOf(passingScore.get())) < 0) {
                throw new AppException(ErrorCode.CANNOT_COMPLETE_LESSON);
            }
        }

        Instant now = Instant.now();
        if (progress.getStartedAt() == null) {
            progress.setStartedAt(now);
        }
        progress.setStatus(ProgressStatus.COMPLETED);
        progress.setCompletedAt(now);
        progress.setLastAccessedAt(now);

        UserLessonProgress saved = progressRepository.save(progress);

        // XP/streak qua LessonCompletedEvent + tự hoàn thành khóa khi đạt 100%
        int courseProgressPercent = lessonProgressRecorder.onLessonCompleted(
                currentUser.getId(), lesson, saved.getBestScore(), isMiniTest);

        log.info("Lesson {} completed: userId={}, courseId={}, progressPercent={}, hasQuiz={}",
                lessonId, currentUser.getId(), courseId, courseProgressPercent, isMiniTest);

        return progressMapper.toResponse(saved);
    }

    // ============================================================
    // UPDATE SECTION PROGRESS
    // ============================================================

    /**
     * Ghi số mục đã xem của 1 phần và/hoặc % cả bài. Chỉ tăng, không giảm (xem lại mục cũ không làm lùi tiến độ).
     * <p>shortcut: % cả bài do client tính (server không tính lại tình huống luyện nói) nên chỉ dùng hiển thị,
     * không ảnh hưởng hoàn thành bài/XP; chuyển sang server tính nếu % được dùng cho nghiệp vụ.</p>
     */
    @Transactional
    public ProgressResponse updateSectionProgress(Long lessonId, LessonProgressUpdateRequest request) {
        User currentUser = currentUserService.getCurrentUser();
        resolveLessonForLearning(lessonId);

        UserLessonProgress progress = lessonProgressRecorder.lockOrCreate(currentUser.getId(), lessonId);
        Instant now = Instant.now();
        if (progress.getStatus() == ProgressStatus.NOT_STARTED) {
            progress.setStatus(ProgressStatus.IN_PROGRESS);
            progress.setStartedAt(now);
        }
        progress.setLastAccessedAt(now);

        if (request.getSectionType() != null && request.getViewedCount() != null) {
            // Copy map mới để Hibernate nhận ra thay đổi của cột JSON
            Map<LessonSectionType, Integer> sections = new HashMap<>(progress.getSectionProgress());
            sections.merge(request.getSectionType(), request.getViewedCount(), Math::max);
            progress.setSectionProgress(sections);
        }
        if (request.getLessonPercent() != null) {
            progress.setProgressPercent(Math.max(progress.getProgressPercent(), request.getLessonPercent()));
        }

        return progressMapper.toResponse(progressRepository.save(progress));
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
        // Số bài học viên đang học (bản xuất bản nếu khóa đã có)
        long totalLessons = lessonRepository.countLearnerLessons(courseId);
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

    /** Bài trong bản xuất bản (học viên) hoặc bản đang soạn — kèm kiểm tra đã đăng ký. */
    private Lesson resolveLessonForLearning(Long lessonId) {
        return publishedLessonResolver.forLearning(lessonId)
                .map(PublishedLessonResolver.PublishedLesson::lesson)
                .orElseGet(() -> requireLiveLessonForLearning(lessonId));
    }

    private Lesson requireLiveLessonForLearning(Long lessonId) {
        Lesson lesson = lessonRepository.findByIdAndDeletedFalse(lessonId)
                .orElseThrow(() -> new AppException(ErrorCode.LESSON_NOT_FOUND));
        courseAccessGuard.requireEnrolledInLesson(lesson);
        return lesson;
    }
}
