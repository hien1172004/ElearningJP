package com.jp.elearningjp.service.curriculum;

import com.jp.elearningjp.dto.response.curriculum.CourseStudentsResponse;
import com.jp.elearningjp.dto.response.curriculum.CourseStudentsResponse.LessonProgress;
import com.jp.elearningjp.dto.response.curriculum.CourseStudentsResponse.StudentProgress;
import com.jp.elearningjp.entity.curriculum.Course;
import com.jp.elearningjp.entity.curriculum.CourseEnrollment;
import com.jp.elearningjp.entity.curriculum.Lesson;
import com.jp.elearningjp.entity.curriculum.UserLessonProgress;
import com.jp.elearningjp.entity.user.User;
import com.jp.elearningjp.exception.AppException;
import com.jp.elearningjp.exception.ErrorCode;
import com.jp.elearningjp.repository.curriculum.CourseEnrollmentRepository;
import com.jp.elearningjp.repository.curriculum.CourseRepository;
import com.jp.elearningjp.repository.curriculum.LessonRepository;
import com.jp.elearningjp.repository.curriculum.UserLessonProgressRepository;
import com.jp.elearningjp.shared.enums.EnrollmentStatus;
import com.jp.elearningjp.shared.enums.ProgressStatus;
import com.jp.elearningjp.shared.response.PageResponse;
import com.jp.elearningjp.shared.util.CourseAccessGuard;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Theo dõi học viên của khóa học: danh sách kèm tiến độ và chi tiết từng bài.
 * ADMIN xem mọi khóa; TEACHER chỉ khóa được giao ({@link CourseAccessGuard#requireAssignedOrAdmin}).
 */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CourseStudentService {

    static final int MAX_PAGE_SIZE = 100;
    static final int RECENT_DAYS = 7;
    static final Set<String> SORTS = Set.of("PROGRESS", "NAME", "ENROLLED", "LAST_ACTIVE");

    CourseRepository courseRepository;
    LessonRepository lessonRepository;
    CourseEnrollmentRepository enrollmentRepository;
    UserLessonProgressRepository progressRepository;
    CourseAccessGuard courseAccessGuard;

    @Transactional(readOnly = true)
    public CourseStudentsResponse getStudents(Long courseId, String keyword, String sort, int page, int size) {
        Course course = findManageableCourse(courseId);
        int totalLessons = countLessons(courseId);

        String pattern = keyword == null || keyword.isBlank()
                ? "" : "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
        String sortKey = sort == null ? "LAST_ACTIVE" : sort.trim().toUpperCase(Locale.ROOT);
        if (!SORTS.contains(sortKey)) sortKey = "LAST_ACTIVE";
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);

        Page<Object[]> rows = enrollmentRepository.findStudentProgress(
                courseId, pattern, sortKey, PageRequest.of(safePage - 1, safeSize));
        List<StudentProgress> students = rows.getContent().stream()
                .map(r -> toStudent(r, totalLessons))
                .toList();

        Object[] s = firstRow(enrollmentRepository.summarizeCourseStudents(
                courseId, totalLessons, Instant.now().minus(RECENT_DAYS, ChronoUnit.DAYS)));
        double avgCompleted = toDouble(s[4]);

        return CourseStudentsResponse.builder()
                .courseId(course.getId())
                .courseTitle(course.getTitle())
                .totalLessons(totalLessons)
                .summary(CourseStudentsResponse.Summary.builder()
                        .totalStudents(toLong(s[0]))
                        .finishedStudents(toLong(s[1]))
                        .notStartedStudents(toLong(s[2]))
                        .activeRecently(toLong(s[3]))
                        .recentDays(RECENT_DAYS)
                        .avgProgressPercent(totalLessons == 0 ? 0
                                : Math.round(Math.min(avgCompleted / totalLessons, 1.0) * 1000) / 10.0)
                        .build())
                .students(PageResponse.<StudentProgress>builder()
                        .items(students)
                        .page(safePage)
                        .size(safeSize)
                        .totalElements(rows.getTotalElements())
                        .totalPages(rows.getTotalPages())
                        .build())
                .build();
    }

    /** Tiến độ từng bài của một học viên (học viên phải đang đăng ký khóa). */
    @Transactional(readOnly = true)
    public CourseStudentsResponse.StudentDetail getStudentDetail(Long courseId, Long userId) {
        Course course = findManageableCourse(courseId);
        CourseEnrollment enrollment = enrollmentRepository.findByUserIdAndCourseIdAndDeletedFalse(userId, courseId)
                .filter(e -> e.getUser() != null && !e.getUser().isDeleted())
                .orElseThrow(() -> new AppException(ErrorCode.NOT_ENROLLED));

        List<Lesson> lessons = lessonRepository.findByCourseIdAndDeletedFalseOrderByOrderIndexAsc(courseId);
        Map<Long, UserLessonProgress> progressByLesson = progressRepository.findAllByUserIdAndCourseId(userId, courseId)
                .stream()
                .collect(Collectors.toMap(p -> p.getLesson().getId(), Function.identity(), (a, b) -> a));

        List<LessonProgress> lessonItems = new ArrayList<>();
        int completed = 0;
        int inProgress = 0;
        Instant lastActive = null;
        double scoreSum = 0;
        int scored = 0;
        for (Lesson lesson : lessons) {
            UserLessonProgress p = progressByLesson.get(lesson.getId());
            ProgressStatus status = p != null ? p.getStatus() : ProgressStatus.NOT_STARTED;
            if (status == ProgressStatus.COMPLETED) completed++;
            if (status == ProgressStatus.IN_PROGRESS) inProgress++;
            if (p != null && p.getLastAccessedAt() != null
                    && (lastActive == null || p.getLastAccessedAt().isAfter(lastActive))) {
                lastActive = p.getLastAccessedAt();
            }
            Double best = p != null ? toNullableDouble(p.getBestScore()) : null;
            if (best != null) {
                scoreSum += best;
                scored++;
            }
            lessonItems.add(LessonProgress.builder()
                    .lessonId(lesson.getId())
                    .title(lesson.getTitle())
                    .orderIndex(lesson.getOrderIndex())
                    .status(status)
                    .startedAt(p != null ? p.getStartedAt() : null)
                    .lastAccessedAt(p != null ? p.getLastAccessedAt() : null)
                    .completedAt(p != null ? p.getCompletedAt() : null)
                    .bestScore(best)
                    .build());
        }

        User u = enrollment.getUser();
        int total = lessons.size();
        StudentProgress student = StudentProgress.builder()
                .userId(u.getId())
                .fullName(u.getFullName())
                .email(u.getEmail())
                .avatarUrl(u.getAvatarUrl())
                .enrollmentStatus(enrollment.getStatus())
                .enrolledAt(enrollment.getCreatedAt())
                .completedLessons(completed)
                .inProgressLessons(inProgress)
                .totalLessons(total)
                .progressPercent(percent(completed, total))
                .lastActiveAt(lastActive)
                .avgQuizScore(scored == 0 ? null : round1(scoreSum / scored))
                .build();

        return CourseStudentsResponse.StudentDetail.builder()
                .courseId(course.getId())
                .courseTitle(course.getTitle())
                .student(student)
                .lessons(lessonItems)
                .build();
    }

    // =========================================================
    // HELPERS
    // =========================================================

    private Course findManageableCourse(Long courseId) {
        Course course = courseRepository.findByIdAndDeletedFalse(courseId)
                .orElseThrow(() -> new AppException(ErrorCode.COURSE_NOT_FOUND));
        courseAccessGuard.requireAssignedOrAdmin(course);
        return course;
    }

    private int countLessons(Long courseId) {
        return lessonRepository.countActiveLessonsByCourseIds(List.of(courseId)).stream()
                .findFirst()
                .map(c -> c.getLessonCount().intValue())
                .orElse(0);
    }

    private static StudentProgress toStudent(Object[] r, int totalLessons) {
        int completed = (int) toLong(r[6]);
        Double avg = r[9] == null ? null : round1(toDouble(r[9]));
        Double avgExam = r[10] == null ? null : round1(toDouble(r[10]));
        return StudentProgress.builder()
                .userId(toLong(r[0]))
                .fullName((String) r[1])
                .email((String) r[2])
                .avatarUrl((String) r[3])
                .enrollmentStatus(r[4] == null ? null : EnrollmentStatus.valueOf(r[4].toString()))
                .enrolledAt(toInstant(r[5]))
                .completedLessons(completed)
                .inProgressLessons((int) toLong(r[7]))
                .totalLessons(totalLessons)
                .progressPercent(percent(completed, totalLessons))
                .lastActiveAt(toInstant(r[8]))
                .avgQuizScore(avg)
                .avgExamScore(avgExam)
                .build();
    }

    static int percent(int completed, int total) {
        if (total <= 0) return 0;
        return (int) Math.min(100, Math.round(completed * 100.0 / total));
    }

    private static Object[] firstRow(List<Object[]> rows) {
        return rows == null || rows.isEmpty() || rows.get(0) == null ? new Object[5] : rows.get(0);
    }

    private static long toLong(Object o) {
        return o instanceof Number n ? n.longValue() : 0L;
    }

    private static double toDouble(Object o) {
        return o instanceof Number n ? n.doubleValue() : 0d;
    }

    private static Double toNullableDouble(BigDecimal v) {
        return v == null ? null : v.doubleValue();
    }

    private static double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }

    private static Instant toInstant(Object o) {
        if (o instanceof Instant i) return i;
        if (o instanceof java.sql.Timestamp t) return t.toInstant();
        if (o instanceof java.time.OffsetDateTime odt) return odt.toInstant();
        return null;
    }
}
