package com.jp.elearningjp.service.curriculum;

import com.jp.elearningjp.entity.curriculum.Course;
import com.jp.elearningjp.entity.curriculum.LessonDraft;
import com.jp.elearningjp.exception.AppException;
import com.jp.elearningjp.exception.ErrorCode;
import com.jp.elearningjp.repository.curriculum.CourseRepository;
import com.jp.elearningjp.repository.curriculum.LessonRepository;
import com.jp.elearningjp.service.curriculum.snapshot.CourseSnapshotService;
import com.jp.elearningjp.shared.enums.CourseReviewStatus;
import com.jp.elearningjp.shared.enums.DraftKind;
import com.jp.elearningjp.shared.event.curriculum.CourseReviewEvent;
import com.jp.elearningjp.shared.util.CurrentUserService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Phiên bản cả khóa và sự kiện xuất bản khóa học.
 * <ul>
 *   <li>ADMIN "Lưu cả khóa" ({@link #saveVersion}): chụp nháp ADMIN của mọi bài ADMIN thấy (kèm đề thi gắn khóa) thành
 *       phiên bản mới của bản học viên, không qua duyệt, không bật xuất bản.</li>
 *   <li>ADMIN chuyển nháp / mở lại khóa ({@link #onAdminPublishChanged}).</li>
 * </ul>
 * Luồng gửi / duyệt cả khóa đã bỏ — duyệt chỉ còn theo bài ({@link LessonReviewService}). Bảng
 * {@code course_review_requests} / {@code course_review_comments} giữ lại làm dữ liệu lịch sử.
 */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class CourseReviewService {

    CourseRepository courseRepository;
    LessonRepository lessonRepository;
    CurrentUserService currentUserService;
    ApplicationEventPublisher eventPublisher;
    CourseSnapshotService courseSnapshotService;
    LessonDraftService lessonDraftService;

    /**
     * ADMIN "Lưu cả khóa": chụp nháp ADMIN thành phiên bản mới, không qua duyệt (ADMIN toàn quyền).
     * Còn bài chờ duyệt → phải duyệt / từ chối trước. Không bật xuất bản.
     */
    @Transactional
    public void saveVersion(Long courseId) {
        Course course = courseRepository.findByIdAndDeletedFalse(courseId)
                .orElseThrow(() -> new AppException(ErrorCode.COURSE_NOT_FOUND));
        if (!currentUserService.isAdmin()) {
            throw new AppException(ErrorCode.FORBIDDEN_COURSE_ACCESS);
        }
        // Lưu cả khóa đè lên bài đang chờ duyệt → phải duyệt / từ chối các bài đó trước
        if (lessonRepository.countPendingReviewLessons(courseId) > 0) {
            throw new AppException(ErrorCode.COURSE_REVIEW_LESSONS_PENDING);
        }
        if (!Boolean.TRUE.equals(course.getHasUnpublishedChanges())) {
            throw new AppException(ErrorCode.COURSE_SAVE_NO_CHANGES);
        }
        if (lessonRepository.countByCourseIdAndDeletedFalse(courseId) == 0) {
            throw new AppException(ErrorCode.COURSE_SAVE_NO_LESSONS);
        }

        Long adminId = currentUserService.getCurrentUserId();
        boolean published = Boolean.TRUE.equals(course.getPublished());
        snapshotDraft(course, DraftKind.ADMIN);
        log.info("[COURSE_VERSION] Course id={} saved as new version by admin id={} (published={})",
                courseId, adminId, published);

        publish(course, CourseReviewEvent.Action.SAVED, adminId, published);
    }

    /**
     * ADMIN chuyển nháp / mở lại khóa. Không đụng tới bản đang soạn: mở lại dùng đúng phiên bản đã lưu / duyệt gần nhất.
     * Gọi từ {@code CourseService.publishCourse}.
     */
    @Transactional
    public void onAdminPublishChanged(Course course, boolean publish) {
        publish(course, publish ? CourseReviewEvent.Action.PUBLISHED : CourseReviewEvent.Action.UNPUBLISHED,
                currentUserService.getCurrentUserId(), false);
    }

    /**
     * Chụp nháp phía {@code kind} của mọi bài phía đó thấy thành bản học viên học (kể cả đề thi gắn khóa); giữ nguyên
     * cờ xuất bản. Sau đó (V65): mọi bài tăng {@code live_revision}; nháp vừa chụp trùng live, nháp phía kia chưa sửa
     * thì dựng lại theo live, đã sửa thì thành nháp cũ.
     */
    private void snapshotDraft(Course course, DraftKind kind) {
        courseSnapshotService.publish(course, currentUserService.getCurrentUserOrNull(), kind);
        course.setReviewStatus(CourseReviewStatus.EDITING);
        course.setHasUnpublishedChanges(false);
        courseRepository.save(course);
        lessonRepository.clearLessonChanges(course.getId());
        // Chỉ bài vừa được chụp (bài phía này thấy — V66); bài mới phía bên kia đang soạn không đổi live, nháp không cũ
        List<Long> lessonIds = lessonRepository.findVisibleByCourseId(course.getId(), kind).stream()
                .map(com.jp.elearningjp.entity.curriculum.Lesson::getId).toList();
        if (!lessonIds.isEmpty()) {
            lessonRepository.bumpLiveRevision(lessonIds);
            for (Long lessonId : lessonIds) {
                Long publishedDraftId = lessonDraftService.findActive(lessonId, kind)
                        .map(LessonDraft::getId).orElse(null);
                lessonDraftService.afterLivePublished(lessonId, publishedDraftId);
            }
        }
    }

    private void publish(Course course, CourseReviewEvent.Action action, Long actorId,
                         boolean contentUpdatedWhilePublished) {
        eventPublisher.publishEvent(new CourseReviewEvent(
                this, course.getId(), course.getTitle(), action, actorId, null, contentUpdatedWhilePublished));
    }
}
