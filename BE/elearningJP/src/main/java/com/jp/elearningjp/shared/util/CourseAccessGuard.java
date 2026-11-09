package com.jp.elearningjp.shared.util;

import com.jp.elearningjp.entity.curriculum.LessonSection;
import com.jp.elearningjp.entity.curriculum.Lesson;
import com.jp.elearningjp.entity.curriculum.Course;
import com.jp.elearningjp.exception.AppException;
import com.jp.elearningjp.exception.ErrorCode;
import com.jp.elearningjp.repository.curriculum.CourseEnrollmentRepository;
import com.jp.elearningjp.repository.curriculum.CourseRepository;
import com.jp.elearningjp.repository.curriculum.CourseTeacherRepository;
import com.jp.elearningjp.shared.enums.CourseReviewStatus;
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
 *   <li>{@link #requireAssignedOrAdmin(Course)} — admin luôn pass; teacher chỉ
 *       pass khi được giao course (bảng {@code course_teachers}).</li>
 *   <li>{@link #requireContentAccess(Course)} — xem nội dung: admin, giáo viên được giao
 *       hoặc user đã enroll (kể cả đã học xong).</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CourseAccessGuard {

    private final CurrentUserService currentUserService;
    private final CourseEnrollmentRepository enrollmentRepository;
    private final CourseTeacherRepository courseTeacherRepository;
    private final CourseRepository courseRepository;

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
     * BẢO VỆ BẢN QUYỀN: chỉ admin, giáo viên được giao hoặc học viên đã đăng ký mới được xem nội dung.
     * Khác {@link #requireEnrolled(Long)}: cho giáo viên được giao xem bài của khóa.
     */
    public void requireContentAccess(Course course) {
        if (currentUserService.isAdmin()) {
            return;
        }
        Long userId = currentUserService.getCurrentUserId();
        if (isAssignedTeacher(course, userId)
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
     * admin, giáo viên được giao hoặc người đã đăng ký. {@code userId} null (chưa đăng nhập) → không.
     */
    public boolean canViewUnpublished(Course course, Long userId) {
        if (userId == null) {
            return false;
        }
        return currentUserService.isAdmin()
                || isAssignedTeacher(course, userId)
                || enrollmentRepository.existsByUserIdAndCourseIdAndDeletedFalse(userId, course.getId());
    }

    /**
     * User có phải giáo viên được ADMIN giao quản lý course không
     * ({@code userId} null = chưa đăng nhập → false).
     */
    public boolean isAssignedTeacher(Course course, Long userId) {
        return userId != null
                && course != null
                && course.getId() != null
                && courseTeacherRepository.existsByCourseIdAndTeacherId(course.getId(), userId);
    }

    /**
     * Bắt buộc user hiện tại là ADMIN hoặc giáo viên được giao course.
     * Dùng cho: quản lý bài học, section, quiz, đề thi gắn với khóa, xem học viên của khóa...
     *
     * @param course           course cần kiểm tra
     * @param forbiddenError   ErrorCode ném ra nếu fail (mặc định FORBIDDEN_COURSE_ACCESS).
     *                         Một số module (lesson, quiz) muốn trả code riêng.
     */
    public void requireAssignedOrAdmin(Course course, ErrorCode forbiddenError) {
        if (currentUserService.isAdmin()) {
            return;
        }
        Long userId = currentUserService.getCurrentUserId();
        if (!isAssignedTeacher(course, userId)) {
            log.warn("[SECURITY] User id={} attempted to manage course id={} without assignment",
                    userId, course.getId());
            throw new AppException(forbiddenError);
        }
    }

    /**
     * Dùng cho MỌI thao tác GHI nội dung khóa (bài học, section, item, quiz, đề thi gắn khóa...):
     * <ol>
     *   <li>chỉ giáo viên được giao — ADMIN chỉ duyệt, không soạn ({@code COURSE_CONTENT_ADMIN_READ_ONLY});</li>
     *   <li>khóa đang PENDING_REVIEW → khóa sửa tới khi ADMIN quyết định / giáo viên rút yêu cầu;</li>
     *   <li>đánh dấu bản đang soạn có thay đổi chưa duyệt.</li>
     * </ol>
     */
    public void requireContentEditable(Course course, ErrorCode forbiddenError) {
        // ADMIN chỉ duyệt: không soạn nội dung khóa (giáo viên được giao soạn, ADMIN duyệt)
        if (currentUserService.isAdmin()) {
            throw new AppException(ErrorCode.COURSE_CONTENT_ADMIN_READ_ONLY);
        }
        requireAssignedTeacher(course, forbiddenError);
        if (course.getReviewStatus() == CourseReviewStatus.PENDING_REVIEW) {
            throw new AppException(ErrorCode.COURSE_PENDING_REVIEW_LOCKED);
        }
        // Không set field trên entity: làm entity "bẩn" sẽ ghi lại cả dòng courses và tăng version,
        // khiến các giáo viên sửa những bài khác nhau cùng lúc xung đột optimistic lock trên khóa.
        if (!Boolean.TRUE.equals(course.getHasUnpublishedChanges())) {
            courseRepository.markUnpublishedChanges(course.getId());
        }
    }

    /** Shortcut tương đương {@code requireContentEditable(course, FORBIDDEN_COURSE_ACCESS)}. */
    /** Bắt buộc user hiện tại là giáo viên được giao course (ADMIN không tính). */
    public void requireAssignedTeacher(Course course, ErrorCode forbiddenError) {
        Long userId = currentUserService.getCurrentUserId();
        if (!isAssignedTeacher(course, userId)) {
            log.warn("[SECURITY] User id={} is not an assigned teacher of course id={}", userId, course.getId());
            throw new AppException(forbiddenError);
        }
    }

    public void requireContentEditable(Course course) {
        requireContentEditable(course, ErrorCode.FORBIDDEN_COURSE_ACCESS);
    }

    /**
     * Shortcut tương đương {@code requireAssignedOrAdmin(course, FORBIDDEN_COURSE_ACCESS)}.
     */
    public void requireAssignedOrAdmin(Course course) {
        requireAssignedOrAdmin(course, ErrorCode.FORBIDDEN_COURSE_ACCESS);
    }
}
