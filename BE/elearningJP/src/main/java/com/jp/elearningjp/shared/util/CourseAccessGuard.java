package com.jp.elearningjp.shared.util;

import com.jp.elearningjp.entity.curriculum.LessonSection;
import com.jp.elearningjp.entity.curriculum.Lesson;
import com.jp.elearningjp.entity.curriculum.Course;
import com.jp.elearningjp.entity.user.User;
import com.jp.elearningjp.exception.AppException;
import com.jp.elearningjp.exception.ErrorCode;
import com.jp.elearningjp.repository.curriculum.CourseEnrollmentRepository;
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
 *   <li>{@link #requireEnrolled(Long)} — user phải có enrollment chưa xoá
 *       (ACTIVE hoặc COMPLETED) cho course; admin thì pass.</li>
 *   <li>{@link #requireOwnerOrAdmin(Course)} — admin luôn pass; teacher chỉ
 *       pass khi là người tạo course.</li>
 *   <li>{@link #requireContentAccess(Course)} — xem nội dung: admin, chủ khóa học
 *       hoặc user đã enroll (kể cả đã học xong).</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CourseAccessGuard {

    private final CurrentUserService currentUserService;
    private final CourseEnrollmentRepository enrollmentRepository;

    /**
     * Bắt buộc user hiện tại đã enroll course hoặc là admin.
     * Học xong khóa (COMPLETED) vẫn được vào học lại, cùng cách hiểu "đã đăng ký" với getCourseDetail.
     * Dùng cho: mở lesson, làm quiz, xem progress chi tiết...
     */
    public void requireEnrolled(Long courseId) {
        if (currentUserService.isAdmin()) {
            return;
        }
        Long userId = currentUserService.getCurrentUserId();
        if (!enrollmentRepository.existsByUserIdAndCourseIdAndDeletedFalse(userId, courseId)) {
            Long currentUserId = userId; // chỉ log khi cần
            log.warn("[SECURITY] User id={} attempted to access course id={} without enrollment",
                    currentUserId, courseId);
            throw new AppException(ErrorCode.COURSE_NOT_ENROLLED);
        }
    }

    /**
     * BẢO VỆ BẢN QUYỀN: chỉ admin, chủ khóa học hoặc học viên đã đăng ký mới được xem nội dung.
     * Khác {@link #requireEnrolled(Long)}: cho giáo viên xem bài của chính mình.
     */
    public void requireContentAccess(Course course) {
        if (currentUserService.isAdmin()) {
            return;
        }
        Long userId = currentUserService.getCurrentUserId();
        if (isOwner(course, userId)
                || enrollmentRepository.existsByUserIdAndCourseIdAndDeletedFalse(userId, course.getId())) {
            return;
        }
        log.warn("[SECURITY] User id={} attempted to read content of course id={} without enrollment",
                userId, course.getId());
        throw new AppException(ErrorCode.COURSE_NOT_ENROLLED);
    }

    /** Kiểm tra quyền xem nội dung bài học; bài hoặc khóa đã xóa coi như không tồn tại. */
    public void requireLessonContentAccess(Lesson lesson) {
        requireNotDeleted(lesson);
        requireContentAccess(lesson.getCourse());
    }

    /**
     * Dùng cho thao tác học (bắt đầu, hoàn thành, làm/nộp quiz): như {@link #requireEnrolled(Long)}
     * nhưng chặn cả bài thuộc khóa đã xoá — dữ liệu cũ có thể còn enrollment và lesson chưa xoá mềm.
     */
    public void requireEnrolledInLesson(Lesson lesson) {
        requireNotDeleted(lesson);
        requireEnrolled(lesson.getCourse().getId());
    }

    private static void requireNotDeleted(Lesson lesson) {
        if (lesson.isDeleted()) {
            throw new AppException(ErrorCode.LESSON_NOT_FOUND);
        }
        if (lesson.getCourse().isDeleted()) {
            throw new AppException(ErrorCode.COURSE_NOT_FOUND);
        }
    }

    /** Kiểm tra quyền xem nội dung section; section đã xóa coi như không tồn tại. */
    public void requireSectionContentAccess(LessonSection section) {
        if (section.isDeleted()) {
            throw new AppException(ErrorCode.LESSON_SECTION_NOT_FOUND);
        }
        requireLessonContentAccess(section.getLesson());
    }

    /**
     * Ai được thấy nội dung gắn với khóa chưa publish (vd: bình luận của bài học):
     * admin, chủ khóa hoặc người đã đăng ký. {@code userId} null (chưa đăng nhập) → không.
     */
    public boolean canViewUnpublished(Course course, Long userId) {
        if (userId == null) {
            return false;
        }
        return currentUserService.isAdmin()
                || isOwner(course, userId)
                || enrollmentRepository.existsByUserIdAndCourseIdAndDeletedFalse(userId, course.getId());
    }

    /** User có phải người tạo course không ({@code userId} null = chưa đăng nhập). */
    public boolean isOwner(Course course, Long userId) {
        return userId != null
                && course.getCreatedBy() != null
                && userId.equals(course.getCreatedBy().getId());
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
