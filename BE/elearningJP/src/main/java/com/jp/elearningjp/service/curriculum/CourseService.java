package com.jp.elearningjp.service.curriculum;

import com.jp.elearningjp.dto.request.curriculum.CourseCreateRequest;
import com.jp.elearningjp.dto.request.curriculum.CourseUpdateRequest;
import com.jp.elearningjp.dto.response.curriculum.*;
import com.jp.elearningjp.entity.curriculum.Course;
import com.jp.elearningjp.entity.curriculum.Lesson;
import com.jp.elearningjp.entity.curriculum.UserLessonProgress;
import com.jp.elearningjp.entity.user.User;
import com.jp.elearningjp.exception.AppException;
import com.jp.elearningjp.exception.ErrorCode;
import com.jp.elearningjp.mapper.CourseMapper;
import com.jp.elearningjp.mapper.LessonMapper;
import com.jp.elearningjp.repository.curriculum.*;
import com.jp.elearningjp.repository.user.UserRepository;
import com.jp.elearningjp.shared.enums.JlptLevel;
import com.jp.elearningjp.shared.enums.ProgressStatus;
import com.jp.elearningjp.shared.response.PageResponse;
import com.jp.elearningjp.shared.utils.SlugUtils;
import com.jp.elearningjp.shared.util.CourseAccessGuard;
import com.jp.elearningjp.shared.util.CurrentUserService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class CourseService {

    CourseRepository courseRepository;
    LessonRepository lessonRepository;
    CourseEnrollmentRepository enrollmentRepository;
    UserLessonProgressRepository progressRepository;
    QuizRepository quizRepository;
    UserRepository userRepository;
    CourseMapper courseMapper;
    LessonMapper lessonMapper;
    CurrentUserService currentUserService;
    CourseAccessGuard courseAccessGuard;

    /**
     * Lấy danh sách khóa học công khai cho học viên (có phân trang và lọc theo cấp độ JLPT)
     */
    @Transactional(readOnly = true)
    public PageResponse<CourseResponse> getPublicCourses(JlptLevel jlptLevel, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page - 1), size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Course> coursePage = (jlptLevel != null)
                ? courseRepository.findByJlptLevelAndPublishedTrueAndDeletedFalse(jlptLevel, pageable)
                : courseRepository.findByPublishedTrueAndDeletedFalse(pageable);

        List<CourseResponse> items = coursePage.getContent().stream()
                .map(course -> {
                    CourseResponse response = courseMapper.toCourseResponse(course);
                    long totalLessons = lessonRepository.countByCourseIdAndDeletedFalse(course.getId());
                    response.setTotalLessons((int) totalLessons);
                    return response;
                })
                .toList();

        return PageResponse.<CourseResponse>builder()
                .items(items)
                .page(page)
                .size(size)
                .totalElements(coursePage.getTotalElements())
                .totalPages(coursePage.getTotalPages())
                .build();
    }

    /**
     * Dành cho Quản trị viên xem toàn bộ khóa học (kể cả chưa xuất bản)
     */
    @Transactional(readOnly = true)
    public PageResponse<CourseResponse> getAllCoursesForAdmin(int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page - 1), size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Course> coursePage = courseRepository.findByDeletedFalse(pageable);

        List<CourseResponse> items = coursePage.getContent().stream()
                .map(course -> {
                    CourseResponse response = courseMapper.toCourseResponse(course);
                    long totalLessons = lessonRepository.countByCourseIdAndDeletedFalse(course.getId());
                    response.setTotalLessons((int) totalLessons);
                    return response;
                })
                .toList();

        return PageResponse.<CourseResponse>builder()
                .items(items)
                .page(page)
                .size(size)
                .totalElements(coursePage.getTotalElements())
                .totalPages(coursePage.getTotalPages())
                .build();
    }

    /**
     * Xem chi tiết khóa học và mục lục lộ trình bài học
     * Tự động nhận diện tài khoản: nếu đã đăng ký thì trả về isEnrolled = true và % tiến độ
     */
    @Transactional(readOnly = true)
    public CourseDetailResponse getCourseDetail(Long courseId) {
        Course course = courseRepository.findByIdAndDeletedFalse(courseId)
                .orElseThrow(() -> new AppException(ErrorCode.COURSE_NOT_FOUND));

        User currentUser = currentUserService.getCurrentUserOrNull();
        boolean isEnrolled = false;
        Map<Long, UserLessonProgress> progressMap = Collections.emptyMap();
        int completedCount = 0;

        if (currentUser != null) {
            isEnrolled = enrollmentRepository.existsByUserIdAndCourseIdAndDeletedFalse(currentUser.getId(), courseId);
            if (isEnrolled) {
                List<UserLessonProgress> progressList = progressRepository.findAllByUserIdAndCourseId(currentUser.getId(), courseId);
                progressMap = progressList.stream()
                        .collect(Collectors.toMap(p -> p.getLesson().getId(), p -> p));
                completedCount = (int) progressRepository.countCompletedLessonsByCourse(
                        currentUser.getId(), courseId, ProgressStatus.COMPLETED);
            }
        }

        List<Lesson> lessons = lessonRepository.findByCourseIdAndDeletedFalseOrderByOrderIndexAsc(courseId);
        int totalLessons = lessons.size();

        Map<Long, UserLessonProgress> finalProgressMap = progressMap;
        List<LessonOutlineResponse> outlineList = lessons.stream()
                .map(lesson -> {
                    LessonOutlineResponse outline = lessonMapper.toLessonOutlineResponse(lesson);
                    outline.setHasQuiz(quizRepository.existsByLessonIdAndDeletedFalse(lesson.getId()));

                    UserLessonProgress progress = finalProgressMap.get(lesson.getId());
                    if (progress != null) {
                        outline.setStatus(progress.getStatus());
                        outline.setBestScore(progress.getBestScore());
                    } else {
                        outline.setStatus(ProgressStatus.NOT_STARTED);
                    }
                    return outline;
                })
                .toList();

        // Bước 2: dùng query SQL tổng hợp đã có sẵn (chính xác, tránh lệch do tính tay)
        BigDecimal progressPercent = isEnrolled && currentUser != null
                ? toProgressDecimal(progressRepository.calculateCourseProgressPercent(
                        currentUser.getId(), courseId))
                : BigDecimal.ZERO;

        CourseDetailResponse response = courseMapper.toCourseDetailResponse(course);
        response.setEnrolled(isEnrolled);
        response.setTotalLessons(totalLessons);
        response.setCompletedLessonsCount(completedCount);
        response.setProgressPercent(progressPercent);
        response.setLessons(outlineList);

        return response;
    }

    /**
     * Admin/Teacher tạo khóa học mới.
     * ADMIN tạo được tất cả. TEACHER tạo được và course sẽ gán createdBy = teacher.
     */
    @Transactional
    public CourseResponse createCourse(CourseCreateRequest request) {
        User owner = currentUserService.getCurrentUser();

        String baseSlug = SlugUtils.toSlug(request.getTitle());
        String slug = baseSlug;
        int counter = 1;
        while (courseRepository.existsBySlugAndDeletedFalse(slug)) {
            slug = baseSlug + "-" + counter++;
        }

        Course course = courseMapper.toCourseEntity(request);
        course.setSlug(slug);
        course.setCreatedBy(owner);
        course.setPublished(request.getPublished() != null ? request.getPublished() : false);

        Course saved = courseRepository.save(course);
        log.info("[COURSE] Created course id={}, title='{}' by user id={}", saved.getId(), saved.getTitle(), owner.getId());

        CourseResponse response = courseMapper.toCourseResponse(saved);
        response.setTotalLessons(0);
        return response;
    }

    /**
     * Admin hoặc Teacher (chỉ course do mình tạo) cập nhật thông tin khóa học.
     */
    @Transactional
    public CourseResponse updateCourse(Long courseId, CourseUpdateRequest request) {
        Course course = courseRepository.findByIdAndDeletedFalse(courseId)
                .orElseThrow(() -> new AppException(ErrorCode.COURSE_NOT_FOUND));
        ensureCourseOwnerOrAdmin(course);

        courseMapper.updateCourseFromRequest(request, course);
        Course updated = courseRepository.save(course);
        log.info("[COURSE] Updated course id={}", updated.getId());

        CourseResponse response = courseMapper.toCourseResponse(updated);
        long totalLessons = lessonRepository.countByCourseIdAndDeletedFalse(courseId);
        response.setTotalLessons((int) totalLessons);
        return response;
    }

    /**
     * Admin hoặc Teacher (chỉ course do mình tạo) xóa mềm khóa học.
     */
    @Transactional
    public void deleteCourse(Long courseId) {
        Course course = courseRepository.findByIdAndDeletedFalse(courseId)
                .orElseThrow(() -> new AppException(ErrorCode.COURSE_NOT_FOUND));
        ensureCourseOwnerOrAdmin(course);

        course.softDelete();
        courseRepository.save(course);
        log.info("[COURSE] Soft-deleted course id={}", courseId);
    }

    /**
     * Admin hoặc Teacher (chỉ course do mình tạo) bật/tắt xuất bản khóa học.
     */
    @Transactional
    public CourseResponse publishCourse(Long courseId, boolean publish) {
        Course course = courseRepository.findByIdAndDeletedFalse(courseId)
                .orElseThrow(() -> new AppException(ErrorCode.COURSE_NOT_FOUND));
        ensureCourseOwnerOrAdmin(course);

        course.setPublished(publish);
        Course saved = courseRepository.save(course);
        log.info("[COURSE] Course id={} published status set to {}", courseId, publish);

        CourseResponse response = courseMapper.toCourseResponse(saved);
        long totalLessons = lessonRepository.countByCourseIdAndDeletedFalse(courseId);
        response.setTotalLessons((int) totalLessons);
        return response;
    }

    /**
     * Lấy tiến độ học tập chi tiết của học viên trong khóa học
     */
    @Transactional(readOnly = true)
    public CourseProgressResponse getCourseProgress(Long courseId) {
        User user = currentUserService.getCurrentUser();
        Course course = courseRepository.findByIdAndDeletedFalse(courseId)
                .orElseThrow(() -> new AppException(ErrorCode.COURSE_NOT_FOUND));

        if (!enrollmentRepository.existsByUserIdAndCourseIdAndDeletedFalse(user.getId(), courseId)) {
            throw new AppException(ErrorCode.COURSE_NOT_ENROLLED);
        }

        int totalLessons = (int) lessonRepository.countByCourseIdAndDeletedFalse(courseId);
        int completedLessons = (int) progressRepository.countCompletedLessonsByCourse(
                user.getId(), courseId, ProgressStatus.COMPLETED);

        // Bước 2: dùng query SQL tổng hợp có sẵn
        BigDecimal progressPercent = toProgressDecimal(
                progressRepository.calculateCourseProgressPercent(user.getId(), courseId));

        // Bước 4: dùng factory summary để gom logic build response
        return CourseProgressResponse.summary(
                course.getId(),
                course.getTitle(),
                totalLessons,
                completedLessons,
                progressPercent
        );
    }

    // =========================================================
    // HELPER METHODS
    // =========================================================

    /**
     * Kiểm tra quyền sửa/xoá/publish course:
     *  - ADMIN: luôn pass
     *  - TEACHER: chỉ pass nếu course.createdBy == currentUser
     *  - Các role khác: 403
     */
    private void ensureCourseOwnerOrAdmin(Course course) {
        courseAccessGuard.requireOwnerOrAdmin(course);
    }

    /**
     * Chuyển Integer từ {@code calculateCourseProgressPercent} (đã làm tròn số nguyên ở SQL)
     * sang BigDecimal có 1 chữ số thập phân — đồng nhất với format cũ khi tính tay.
     */
    private static BigDecimal toProgressDecimal(Integer percent) {
        if (percent == null) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(percent).setScale(1, RoundingMode.HALF_UP);
    }
}
