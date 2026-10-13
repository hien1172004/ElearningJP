package com.jp.elearningjp.service.curriculum;

import com.jp.elearningjp.dto.request.curriculum.LessonCreateRequest;
import com.jp.elearningjp.dto.request.curriculum.LessonUpdateRequest;
import com.jp.elearningjp.dto.response.curriculum.CourseProgressResponse;
import com.jp.elearningjp.dto.response.curriculum.LessonDetailResponse;
import com.jp.elearningjp.entity.curriculum.Course;
import com.jp.elearningjp.entity.curriculum.Lesson;
import com.jp.elearningjp.entity.curriculum.LessonSection;
import com.jp.elearningjp.entity.curriculum.UserLessonProgress;
import com.jp.elearningjp.entity.user.User;
import com.jp.elearningjp.exception.AppException;
import com.jp.elearningjp.exception.ErrorCode;
import com.jp.elearningjp.mapper.LessonMapper;
import com.jp.elearningjp.mapper.LessonSectionMapper;
import com.jp.elearningjp.repository.curriculum.CourseEnrollmentRepository;
import com.jp.elearningjp.repository.curriculum.CourseRepository;
import com.jp.elearningjp.repository.curriculum.LessonRepository;
import com.jp.elearningjp.repository.curriculum.LessonSectionRepository;
import com.jp.elearningjp.repository.curriculum.UserLessonProgressRepository;
import com.jp.elearningjp.shared.enums.LessonSectionType;
import com.jp.elearningjp.shared.enums.ProgressStatus;
import com.jp.elearningjp.shared.util.CourseAccessGuard;
import com.jp.elearningjp.shared.util.CurrentUserService;
import com.jp.elearningjp.shared.util.OrderIndexResolver;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;

/**
 * Service quản lý {@link Lesson} — bài học trong một khóa học.
 * <p>Logic CRUD cho {@code LessonSection} đã được tách ra {@link SectionService}.</p>
 */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class LessonService {

    LessonRepository lessonRepository;
    CourseRepository courseRepository;
    CourseEnrollmentRepository enrollmentRepository;
    UserLessonProgressRepository progressRepository;
    LessonSectionRepository lessonSectionRepository;
    SectionService sectionService;
    LessonMapper lessonMapper;
    LessonSectionMapper lessonSectionMapper;
    CurrentUserService currentUserService;
    CourseAccessGuard courseAccessGuard;

    /**
     * Xem nội dung chi tiết bài học (kèm sections + items).
     * BẢO VỆ BẢN QUYỀN: Bắt buộc người dùng phải là ADMIN hoặc ĐÃ ĐĂNG KÝ KHÓA HỌC.
     */
    @Transactional
    public LessonDetailResponse getLessonDetail(Long lessonId) {
        Lesson lesson = lessonRepository.findByIdAndDeletedFalse(lessonId)
                .orElseThrow(() -> new AppException(ErrorCode.LESSON_NOT_FOUND));

        Long courseId = lesson.getCourse().getId();

        // 1. Kiểm tra quyền truy cập (admin pass; user khác phải enrolled)
        courseAccessGuard.requireEnrolled(courseId);
        User currentUser = currentUserService.getCurrentUser();

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
        // "Lesson có quiz" = lesson có section TEST chưa bị xoá (sau refactor V13/V14)
        response.setHasQuiz(lessonSectionRepository
                .existsByLessonIdAndSectionTypeAndDeletedFalse(lessonId, LessonSectionType.TEST));
        response.setStatus(progress.getStatus());
        response.setBestScore(progress.getBestScore());
        response.setLastAccessedAt(progress.getLastAccessedAt());
        response.setCompletedAt(progress.getCompletedAt());

        // 4. Load sections + nested collections qua SectionService
        List<LessonSection> sections = lessonSectionRepository.findByLessonIdWithAllItems(lessonId);
        sectionService.loadNestedCollections(sections);
        response.setSections(lessonSectionMapper.toSectionResponses(sections));

        return response;
    }

    /**
     * Admin hoặc Teacher (chỉ trong course do mình tạo) thêm bài học mới.
     *
     * <p>Sau V19: orderIndex được xử lý an toàn theo 2 mode:</p>
     * <ul>
     *   <li><b>Auto-assign</b> (client không gửi orderIndex hoặc gửi <= 0):
     *       gán = MAX + 1, chống race condition bằng cách check trùng sau save.</li>
     *   <li><b>Client chỉ định</b> (orderIndex > 0): dời các lesson có
     *       orderIndex >= newIndex lên +1 để chèn vào giữa, nếu newIndex
     *       vượt quá MAX+1 thì gán lại = MAX + 1.</li>
     * </ul>
     *
     * <p>Trước khi save, kiểm tra EXISTS để trả lỗi 409 LESSON_ORDER_CONFLICT
     * sớm và thân thiện với client thay vì để DB ném
     * {@code DataIntegrityViolationException}.</p>
     */
    @Transactional
    public LessonDetailResponse createLesson(Long courseId, LessonCreateRequest request) {
        Course course = courseRepository.findByIdAndDeletedFalse(courseId)
                .orElseThrow(() -> new AppException(ErrorCode.COURSE_NOT_FOUND));
        ensureCourseOwnerOrAdmin(course);

        // Bước 5: gom logic MAX/shift vào OrderIndexResolver
        Integer resolvedIndex = OrderIndexResolver.resolveForCreate(
                request.getOrderIndex(),
                () -> lessonRepository.findMaxOrderIndexByCourseId(courseId)
        );
        // Nếu client chỉ định index hợp lệ (1..max+1) → shift các record >= index lên +1
        if (request.getOrderIndex() != null && request.getOrderIndex() > 0
                && resolvedIndex == request.getOrderIndex()) {
            int shifted = OrderIndexResolver.shiftUpForCreate(
                    resolvedIndex,
                    () -> lessonRepository.shiftOrderIndexesUp(courseId, resolvedIndex, null)
            );
            log.info("[LESSON] Shifted {} lesson(s) with orderIndex >= {} in course id={}",
                    shifted, resolvedIndex, courseId);
        }
        int orderIndex = resolvedIndex;

        // Check phòng trường hợp hiếm (race condition giữa shift và save)
        if (lessonRepository.existsByCourseIdAndOrderIndexExcludingId(courseId, orderIndex, null)) {
            log.warn("[LESSON] OrderIndex conflict detected at createLesson for course id={}, orderIndex={}",
                    courseId, orderIndex);
            throw new AppException(ErrorCode.LESSON_ORDER_CONFLICT);
        }

        Lesson lesson = lessonMapper.toLessonEntity(request);
        lesson.setCourse(course);
        lesson.setOrderIndex(orderIndex);

        Lesson saved = lessonRepository.save(lesson);
        log.info("[LESSON] Created lesson id={}, title='{}', orderIndex={} in course id={}",
                saved.getId(), saved.getTitle(), saved.getOrderIndex(), courseId);

        LessonDetailResponse response = lessonMapper.toLessonDetailResponse(saved);
        response.setHasQuiz(false);
        response.setStatus(ProgressStatus.NOT_STARTED);
        response.setSections(List.of()); // bài mới chưa có section
        return response;
    }

    /**
     * Admin hoặc Teacher (chỉ trong course do mình tạo) cập nhật bài học.
     *
     * <p>Sau V19: nếu client đổi orderIndex sang giá trị khác:</p>
     * <ul>
     *   <li>Đổi sang index mới <b>hợp lệ</b> trong khoảng [1, MAX+1]:
     *       dời các lesson khác để giải phóng slot (shift down/up), tránh
     *       để lại lỗ hổng số.</li>
     *   <li>Đổi sang index <b>vượt MAX+1</b>: kẹp lại về MAX+1.</li>
     *   <li>Không đổi orderIndex: giữ nguyên.</li>
     * </ul>
     */
    @Transactional
    public LessonDetailResponse updateLesson(Long lessonId, LessonUpdateRequest request) {
        Lesson lesson = lessonRepository.findByIdAndDeletedFalse(lessonId)
                .orElseThrow(() -> new AppException(ErrorCode.LESSON_NOT_FOUND));
        ensureCourseOwnerOrAdmin(lesson.getCourse());

        Integer newOrderIndex = request.getOrderIndex();
        Integer oldOrderIndex = lesson.getOrderIndex();

        if (newOrderIndex != null && newOrderIndex > 0 && !newOrderIndex.equals(oldOrderIndex)) {
            Long courseId = lesson.getCourse().getId();
            int target = OrderIndexResolver.clampToMaxPlusOne(
                    newOrderIndex,
                    () -> lessonRepository.findMaxOrderIndexByCourseId(courseId)
            );

            OrderIndexResolver.Movement movement = OrderIndexResolver.planMove(
                    oldOrderIndex, target,
                    () -> lessonRepository.findMaxOrderIndexByCourseId(courseId)
            );

            if (movement == OrderIndexResolver.Movement.MOVE_UP) {
                // Di chuyển lên trên: dời các lesson trong [target, oldOrderIndex-1] xuống +1.
                // Dùng marker tạm (-oldOrderIndex) để tránh UNIQUE conflict khi shift.
                lesson.setOrderIndex(-oldOrderIndex);
                int shifted = lessonRepository.shiftOrderIndexesUp(courseId, target, lessonId);
                log.info("[LESSON] Move-up: shifted {} lesson(s) in [{}, {}] for lesson id={}",
                        shifted, target, oldOrderIndex - 1, lessonId);
                lesson.setOrderIndex(target);
            } else if (movement == OrderIndexResolver.Movement.MOVE_DOWN) {
                // Di chuyển xuống dưới: dời các lesson trong (oldOrderIndex, target] xuống -1.
                Integer maxOrder = lessonRepository.findMaxOrderIndexByCourseId(courseId);
                int currentMax = (maxOrder != null ? maxOrder : 0);
                if (target <= currentMax) {
                    lesson.setOrderIndex(-oldOrderIndex);
                    lessonRepository.shiftOrderIndexesDownInRange(courseId, oldOrderIndex, target, lessonId);
                    log.info("[LESSON] Move-down: shifted lessons in ({}, {}] for lesson id={}",
                            oldOrderIndex, target, lessonId);
                }
                lesson.setOrderIndex(target);
            }
            // Movement.NOOP → giữ nguyên index

            // Check phòng race condition
            if (lessonRepository.existsByCourseIdAndOrderIndexExcludingId(courseId, lesson.getOrderIndex(), lessonId)) {
                log.warn("[LESSON] OrderIndex conflict detected at updateLesson for lesson id={}, orderIndex={}",
                        lessonId, lesson.getOrderIndex());
                throw new AppException(ErrorCode.LESSON_ORDER_CONFLICT);
            }
        }

        lessonMapper.updateLessonFromRequest(request, lesson);
        Lesson updated = lessonRepository.save(lesson);
        log.info("[LESSON] Updated lesson id={}, orderIndex={}", updated.getId(), updated.getOrderIndex());

        LessonDetailResponse response = lessonMapper.toLessonDetailResponse(updated);
        response.setHasQuiz(lessonSectionRepository
                .existsByLessonIdAndSectionTypeAndDeletedFalse(lessonId, LessonSectionType.TEST));

        // Load sections sau khi update
        List<LessonSection> sections = lessonSectionRepository.findByLessonIdWithAllItems(lessonId);
        sectionService.loadNestedCollections(sections);
        response.setSections(lessonSectionMapper.toSectionResponses(sections));

        return response;
    }

    /**
     * Admin hoặc Teacher (chỉ trong course do mình tạo) xóa mềm bài học.
     */
    @Transactional
    public void deleteLesson(Long lessonId) {
        Lesson lesson = lessonRepository.findByIdAndDeletedFalse(lessonId)
                .orElseThrow(() -> new AppException(ErrorCode.LESSON_NOT_FOUND));
        ensureCourseOwnerOrAdmin(lesson.getCourse());

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
        Lesson lesson = lessonRepository.findByIdAndDeletedFalse(lessonId)
                .orElseThrow(() -> new AppException(ErrorCode.LESSON_NOT_FOUND));

        Long courseId = lesson.getCourse().getId();
        courseAccessGuard.requireEnrolled(courseId);
        User currentUser = currentUserService.getCurrentUser();

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

        // Bước 2: dùng query có sẵn của repository để tránh tính tay
        Integer progressPercent = progressRepository
                .calculateCourseProgressPercent(currentUser.getId(), courseId);
        BigDecimal progressPercentDecimal = progressPercent != null
                ? BigDecimal.valueOf(progressPercent).setScale(1, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        log.info("[PROGRESS] User id={} completed lesson id={}, course progress={} %", currentUser.getId(), lessonId, progressPercentDecimal);

        // Bước 4: dùng factory summary để gom logic build response
        return CourseProgressResponse.summary(
                courseId,
                lesson.getCourse().getTitle(),
                totalLessons,
                completedCount,
                progressPercentDecimal
        );
    }

    // =========================================================
    // HELPER METHODS
    // =========================================================

    /**
     * Kiểm tra quyền sửa/xoá lesson:
     *  - ADMIN: luôn pass
     *  - TEACHER: chỉ pass nếu lesson.course.createdBy == currentUser
     *  - Các role khác: 403
     */
    private void ensureCourseOwnerOrAdmin(Course course) {
        // Giữ ErrorCode FORBIDDEN_LESSON_ACCESS (1501) để không phá contract với FE.
        courseAccessGuard.requireOwnerOrAdmin(course, ErrorCode.FORBIDDEN_LESSON_ACCESS);
    }
}
