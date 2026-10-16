package com.jp.elearningjp.shared.util;

import com.jp.elearningjp.entity.curriculum.Course;
import com.jp.elearningjp.entity.user.User;
import com.jp.elearningjp.exception.AppException;
import com.jp.elearningjp.exception.ErrorCode;
import com.jp.elearningjp.repository.curriculum.CourseEnrollmentRepository;
import com.jp.elearningjp.shared.enums.EnrollmentStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Tập trung các quy tắc kiểm tra quyền truy cập course ở Service layer.
 *
 * <p>Trước đây logic này bị lặp ở nhiều service (CourseService, LessonService,
 * QuizService, ProgressService) → dễ lệch nhau, đặc biệt phần "admin bypass".</p>
 *
 * <p>Class này cung cấp:</p>
 * <ul>
 *   <li>{@link #requireEnrolled(Long, Long)} — user phải có enrollment ACTIVE
 *       cho course; admin thì pass.</li>
 *   <li>{@link #requireOwnerOrAdmin(Course)} — admin luôn pass; teacher chỉ
 *       pass khi là người tạo course.</li>
 *   <li>{@link #requireReadAccess(Course)} — admin hoặc user đã enroll.</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CourseAccessGuard {

    private final CurrentUserService currentUserService;
    private final CourseEnrollmentRepository enrollmentRepository;

    /**
     * Bắt buộc user hiện tại đã enroll course (ACTIVE) hoặc là admin.
     * Dùng cho: mở lesson, làm quiz, xem progress chi tiết...
     */
    public void requireEnrolled(Long courseId) {
        if (currentUserService.isAdmin()) {
            return;
        }
        Long userId = currentUserService.getCurrentUserId();
        if (!enrollmentRepository.existsByUser_IdAndCourse_IdAndStatusAndDeletedFalse(
                userId, courseId, EnrollmentStatus.ACTIVE)) {
            Long currentUserId = userId; // chỉ log khi cần
            log.warn("[SECURITY] User id={} attempted to access course id={} without enrollment",
                    currentUserId, courseId);
            throw new AppException(ErrorCode.COURSE_NOT_ENROLLED);
        }
    }

    /**
     * Bắt buộc user hiện tại phải là người tạo course (teacher) hoặc admin.
     * Dùng cho: create/update/delete course, lesson, section, quiz...
     *
     * @param course           course cần kiểm tra
     * @param forbiddenError   ErrorCode ném ra nếu fail (mặc định FORBIDDEN_COURSE_ACCESS).
     *                         Một số module (lesson, quiz) muốn trả code riêng.
     */
    public void requireOwnerOrAdmin(Course course, ErrorCode forbiddenError) {
        if (currentUserService.isAdmin()) {
            return;
        }
        User currentUser = currentUserService.getCurrentUser();
        if (course.getCreatedBy() == null
                || !course.getCreatedBy().getId().equals(currentUser.getId())) {
            log.warn("[SECURITY] User id={} attempted to modify course id={} without ownership",
                    currentUser.getId(), course.getId());
            throw new AppException(forbiddenError);
        }
    }

    /**
     * Shortcut tương đương {@code requireOwnerOrAdmin(course, FORBIDDEN_COURSE_ACCESS)}.
     */
    public void requireOwnerOrAdmin(Course course) {
        requireOwnerOrAdmin(course, ErrorCode.FORBIDDEN_COURSE_ACCESS);
    }
}
