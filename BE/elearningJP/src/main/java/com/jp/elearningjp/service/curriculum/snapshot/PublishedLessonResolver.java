package com.jp.elearningjp.service.curriculum.snapshot;

import com.jp.elearningjp.entity.curriculum.Course;
import com.jp.elearningjp.entity.curriculum.Lesson;
import com.jp.elearningjp.entity.user.User;
import com.jp.elearningjp.exception.AppException;
import com.jp.elearningjp.exception.ErrorCode;
import com.jp.elearningjp.repository.curriculum.LessonRepository;
import com.jp.elearningjp.shared.util.CourseAccessGuard;
import com.jp.elearningjp.shared.util.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Quyết định người dùng hiện tại đọc bản xuất bản (snapshot) hay bản đang soạn của bài học.
 *
 * <ul>
 *   <li>ADMIN / giáo viên được giao → bản đang soạn (trả {@code Optional.empty()}, service chạy luồng cũ).</li>
 *   <li>Khóa chưa có snapshot → bản đang soạn như cũ.</li>
 *   <li>Còn lại (học viên) → kiểm tra quyền rồi trả snapshot; bài không có trong bản xuất bản → 404,
 *       bài đã xóa ở bản đang soạn nhưng còn trong bản xuất bản → vẫn học được.</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class PublishedLessonResolver {

    private final LessonRepository lessonRepository;
    private final CourseAccessGuard courseAccessGuard;
    private final CurrentUserService currentUserService;
    private final CourseSnapshotReader snapshotReader;

    /** Bài học trong bản xuất bản kèm entity (để ghi tiến độ). */
    public record PublishedLesson(Lesson lesson, LessonSnapshot snapshot) {
    }

    /** Xem nội dung (bài, section, item, quiz): quyền như {@link CourseAccessGuard#requireContentAccess}. */
    public Optional<PublishedLesson> forReading(Long lessonId) {
        return resolve(lessonId, false);
    }

    /** Thao tác học (bắt đầu, hoàn thành, nộp quiz): quyền như {@link CourseAccessGuard#requireEnrolled}. */
    public Optional<PublishedLesson> forLearning(Long lessonId) {
        return resolve(lessonId, true);
    }

    /**
     * Không kiểm tra quyền — dùng cho API vốn không yêu cầu đăng ký (danh sách luyện nói).
     * Trả snapshot nếu người dùng hiện tại không quản lý khóa.
     */
    public Optional<PublishedLesson> forPublicRead(Long lessonId) {
        Lesson lesson = lessonRepository.findById(lessonId).orElse(null);
        if (lesson == null || !usesSnapshot(lesson.getCourse())) {
            return Optional.empty();
        }
        return snapshotReader.findLesson(lesson.getCourse().getPublishedVersionId(), lessonId)
                .map(s -> new PublishedLesson(lesson, s));
    }

    /** Người dùng hiện tại có đọc snapshot của khóa không (không quản lý khóa và khóa đã có snapshot). */
    public boolean usesSnapshot(Course course) {
        if (course == null || course.isDeleted() || course.getPublishedVersionId() == null) {
            return false;
        }
        return !isManager(course);
    }

    /** ADMIN hoặc giáo viên được giao — luôn làm việc trên bản đang soạn. */
    public boolean isManager(Course course) {
        User user = currentUserService.getCurrentUserOrNull();
        if (user == null) {
            return false;
        }
        return currentUserService.isAdmin() || courseAccessGuard.isAssignedTeacher(course, user.getId());
    }

    private Optional<PublishedLesson> resolve(Long lessonId, boolean learning) {
        Lesson lesson = lessonRepository.findById(lessonId).orElse(null);
        if (lesson == null || !usesSnapshot(lesson.getCourse())) {
            // Luồng cũ tự ném LESSON_NOT_FOUND / kiểm tra quyền
            return Optional.empty();
        }
        Course course = lesson.getCourse();
        if (learning) {
            courseAccessGuard.requireEnrolled(course.getId());
        } else {
            courseAccessGuard.requireContentAccess(course);
        }
        LessonSnapshot snapshot = snapshotReader.findLesson(course.getPublishedVersionId(), lessonId)
                .orElseThrow(() -> new AppException(ErrorCode.LESSON_NOT_FOUND));
        return Optional.of(new PublishedLesson(lesson, snapshot));
    }
}
