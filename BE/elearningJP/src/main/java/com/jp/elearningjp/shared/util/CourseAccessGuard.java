package com.jp.elearningjp.shared.util;

import com.jp.elearningjp.entity.curriculum.LessonDraft;
import com.jp.elearningjp.entity.curriculum.LessonSection;
import com.jp.elearningjp.entity.curriculum.Lesson;
import com.jp.elearningjp.entity.curriculum.Course;
import com.jp.elearningjp.entity.user.User;
import com.jp.elearningjp.exception.AppException;
import com.jp.elearningjp.exception.ErrorCode;
import com.jp.elearningjp.repository.curriculum.CourseEnrollmentRepository;
import com.jp.elearningjp.repository.curriculum.CourseRepository;
import com.jp.elearningjp.repository.curriculum.CourseTeacherRepository;
import com.jp.elearningjp.repository.curriculum.LessonRepository;
import com.jp.elearningjp.service.curriculum.CourseEditLockService;
import com.jp.elearningjp.service.curriculum.LessonDraftService;
import com.jp.elearningjp.shared.enums.CourseReviewStatus;
import com.jp.elearningjp.shared.enums.DraftKind;
import com.jp.elearningjp.shared.event.curriculum.CourseContentChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

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
    private final LessonRepository lessonRepository;
    private final CourseEditLockService courseEditLockService;
    private final LessonDraftService lessonDraftService;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    /** Cùng người sửa liên tục → ghi lại "người / thời điểm sửa gần nhất" tối đa 1 lần mỗi khoảng này. */
    static final Duration DRAFT_EDIT_TOUCH_INTERVAL = Duration.ofMinutes(1);

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

    /**
     * Kiểm tra quyền xem nội dung section; section đã xóa hoặc thuộc nháp của phía bên kia coi như không tồn tại.
     */
    public void requireSectionContentAccess(LessonSection section) {
        if (section.isDeleted()) {
            throw new AppException(ErrorCode.LESSON_SECTION_NOT_FOUND);
        }
        requireDraftContentAccess(section.getDraft(), ErrorCode.LESSON_SECTION_NOT_FOUND);
    }

    /**
     * Xem phần tử nội dung (section / item / quiz / kịch bản) theo nháp chứa nó (V65): quyền xem bài + nháp đúng phía
     * người xem. Sai phía → {@code notFound} (không để lộ phần tử của phía bên kia tồn tại).
     */
    public void requireDraftContentAccess(LessonDraft draft, ErrorCode notFound) {
        requireLessonContentAccess(draft.getLesson());
        lessonDraftService.requireReadable(draft, notFound);
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
     *   <li>ADMIN toàn quyền: sửa mọi khóa, kể cả khi đang chờ duyệt;</li>
     *   <li>giáo viên: chỉ khóa được giao, và bị khóa sửa khi PENDING_REVIEW (tới khi ADMIN quyết định / rút yêu cầu);</li>
     *   <li>đánh dấu bản đang soạn có thay đổi chưa duyệt (ADMIN cũng vậy — học viên chỉ thấy sau khi ADMIN bấm Lưu / Duyệt)
     *       và ghi người / thời điểm sửa gần nhất (gộp: chỉ ghi khi đổi người hoặc đã quá
     *       {@link #DRAFT_EDIT_TOUCH_INTERVAL});</li>
     *   <li>phát {@link CourseContentChangedEvent} (DRAFT) — listener chạy AFTER_COMMIT nên thao tác ghi
     *       lỗi / rollback thì không báo; người đang soạn khóa này nhận yêu cầu tải lại.</li>
     * </ol>
     */
    public void requireContentEditable(Course course, ErrorCode forbiddenError) {
        requireContentEditable(course, forbiddenError, true, null);
    }

    /**
     * Như {@link #requireContentEditable(Course, ErrorCode)} nhưng thao tác chỉ đổi <b>nháp phía người dùng hiện tại</b>
     * (nội dung bài, bỏ nháp, lưu nháp ADMIN lên live): tín hiệu realtime kèm phía đó để chỉ người soạn cùng phía được
     * nhắc tải lại — phía kia không đọc nháp này nên chỉ làm mới trạng thái.
     */
    public void requireOwnDraftEditable(Course course, ErrorCode forbiddenError) {
        requireContentEditable(course, forbiddenError, true, lessonDraftService.currentKind());
    }

    /**
     * Ghi đề thi gắn khóa: như {@link #requireContentEditable(Course, ErrorCode)} nhưng <b>không</b> bắt giữ khóa sửa
     * khóa học — đề thi được sửa ở trang riêng ({@code /admin/exams/...}) và chưa tách nháp theo phía (PR 5).
     */
    public void requireExamContentEditable(Course course) {
        requireContentEditable(course, ErrorCode.FORBIDDEN_COURSE_ACCESS, false, null);
    }

    /** @param draftKind nháp phía nào bị đổi (null = phần dùng chung: cấu trúc khóa, đề thi) — chỉ để báo realtime */
    private void requireContentEditable(Course course, ErrorCode forbiddenError, boolean editLockRequired,
                                        DraftKind draftKind) {
        if (!currentUserService.isAdmin()) {
            requireAssignedTeacher(course, forbiddenError);
            if (course.getReviewStatus() == CourseReviewStatus.PENDING_REVIEW) {
                throw new AppException(ErrorCode.COURSE_PENDING_REVIEW_LOCKED);
            }
        }
        if (editLockRequired) {
            // V65: phải đang giữ khóa sửa khóa học của phía mình (giáo viên / ADMIN)
            courseEditLockService.requireHeld(course.getId());
        }
        Long userId = currentUserService.getCurrentUserId();
        // Không set field trên entity: làm entity "bẩn" sẽ ghi lại cả dòng courses và tăng version,
        // khiến các giáo viên sửa những bài khác nhau cùng lúc xung đột optimistic lock trên khóa.
        Instant now = Instant.now(clock);
        if (needsDraftTouch(course, userId, now)) {
            courseRepository.markDraftEdited(course.getId(), userId, currentUserDisplayName(), now);
        }
        eventPublisher.publishEvent(new CourseContentChangedEvent(
                this, course.getId(), CourseContentChangedEvent.Scope.DRAFT, userId, draftKind));
    }

    /**
     * Dùng cho thao tác GHI nội dung <b>của 1 bài</b> (section, item, quiz, luyện nói): như
     * {@link #requireContentEditable(Course, ErrorCode)} (quyền + khóa sửa khóa học của phía mình), rồi trả nháp của
     * phía mình để ghi (V65). Nháp đã gửi duyệt → chỉ đọc ({@code LESSON_PENDING_REVIEW_LOCKED}).
     *
     * <p>Ghi nhận bằng 1 câu UPDATE có điều kiện ({@code status = 'EDITING'}): tăng {@code draft_revision}, đánh dấu
     * có thay đổi, ghi người / lúc sửa. Câu lệnh giữ khóa dòng tới hết giao dịch nên "gửi duyệt" chạy chồng lên sẽ chờ
     * rồi thấy {@code draft_revision} đã đổi → chỉ 1 bên thắng.</p>
     *
     * @return nháp của phía người dùng hiện tại — phần tử mới tạo phải gắn vào nháp này
     */
    public LessonDraft requireLessonEditable(Lesson lesson, ErrorCode forbiddenError) {
        requireNotDeleted(lesson);
        requireOwnDraftEditable(lesson.getCourse(), forbiddenError);
        return lessonDraftService.writableDraft(lesson);
    }

    /**
     * Ghi phần tử nội dung đã có (theo id) thuộc nháp {@code draft}: nháp phải là nháp đang dùng của phía mình
     * (sai phía / nháp cũ → {@code notFound}), rồi như {@link #requireLessonEditable(Lesson, ErrorCode)}.
     */
    public LessonDraft requireDraftEditable(LessonDraft draft, ErrorCode forbiddenError, ErrorCode notFound) {
        lessonDraftService.requireReadable(draft, notFound);
        if (!isManager(draft.getLesson().getCourse())) {
            throw new AppException(forbiddenError);
        }
        LessonDraft writable = requireLessonEditable(draft.getLesson(), forbiddenError);
        if (!writable.getId().equals(draft.getId())) {
            throw new AppException(notFound);
        }
        return writable;
    }

    /** ADMIN hoặc giáo viên được giao. */
    private boolean isManager(Course course) {
        return currentUserService.isAdmin() || isAssignedTeacher(course, currentUserService.getCurrentUserId());
    }

    /** Shortcut tương đương {@code requireLessonEditable(lesson, FORBIDDEN_COURSE_ACCESS)}. */
    public LessonDraft requireLessonEditable(Lesson lesson) {
        return requireLessonEditable(lesson, ErrorCode.FORBIDDEN_COURSE_ACCESS);
    }

    /** Chưa đánh dấu có thay đổi, đổi người sửa, hoặc lần ghi trước đã cũ → cần ghi lại. */
    static boolean needsDraftTouch(Course course, Long userId, Instant now) {
        return !Boolean.TRUE.equals(course.getHasUnpublishedChanges())
                || !Objects.equals(course.getDraftUpdatedById(), userId)
                || course.getDraftUpdatedAt() == null
                || course.getDraftUpdatedAt().plus(DRAFT_EDIT_TOUCH_INTERVAL).isBefore(now);
    }

    private String currentUserDisplayName() {
        User user = currentUserService.getCurrentUserOrNull();
        if (user == null) return null;
        return user.getFullName() != null && !user.getFullName().isBlank() ? user.getFullName() : user.getEmail();
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
