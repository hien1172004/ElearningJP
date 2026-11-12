package com.jp.elearningjp.service.curriculum.listener;

import com.jp.elearningjp.service.curriculum.realtime.CourseContentPublisher;
import com.jp.elearningjp.shared.enums.DraftKind;
import com.jp.elearningjp.shared.event.curriculum.CourseContentChangedEvent;
import com.jp.elearningjp.shared.event.curriculum.CourseContentChangedEvent.Scope;
import com.jp.elearningjp.shared.event.curriculum.CourseReviewEvent;
import com.jp.elearningjp.shared.event.curriculum.LessonReviewEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Báo realtime cho người đang mở khóa học khi nội dung đổi, để họ tải lại:
 * <ul>
 *   <li>{@link CourseContentChangedEvent} (mỗi thao tác ghi nội dung) → DRAFT kèm phía nháp bị đổi;</li>
 *   <li>{@link CourseReviewEvent}: chốt phiên bản mới (kể cả khi khóa đang nháp — học viên đã đăng ký vẫn học),
 *       hoặc ẩn / hiện khóa → PUBLISHED (học viên cũng nhận); gửi / rút / từ chối / bỏ nháp → REVIEW (chỉ
 *       trạng thái duyệt đổi).</li>
 * </ul>
 * AFTER_COMMIT: thao tác rollback thì không báo. {@code fallbackExecution}: vài đường ghi không mở
 * transaction vẫn được báo. Lỗi chỉ log.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CourseContentRealtimeListener {

    private final CourseContentPublisher publisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onContentChanged(CourseContentChangedEvent event) {
        push(event.getCourseId(), event.getScope(), event.getActorId(), event.getDraftKind());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onReview(CourseReviewEvent event) {
        push(event.getCourseId(), scopeOf(event), event.getActorId(), null);
    }

    /** Duyệt theo bài: khóa / mở khóa bài → người soạn tải lại; bài lên bản live khi khóa đang mở → cả học viên. */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onLessonReview(LessonReviewEvent event) {
        push(event.getCourseId(), scopeOf(event), event.getActorId(), null);
    }

    /*
     * Duyệt / Lưu luôn tạo phiên bản học viên học mới, kể cả khi khóa đang nháp: học viên đã đăng ký vẫn
     * học khóa nháp (theo snapshot) nên cũng phải được báo → PUBLISHED, không phụ thuộc cờ "đang xuất bản"
     * (cờ đó chỉ dùng cho thông báo).
     */
    static Scope scopeOf(LessonReviewEvent event) {
        return switch (event.getAction()) {
            case APPROVED, SAVED -> Scope.PUBLISHED;
            /*
             * Chỉ trạng thái duyệt đổi; nội dung người xem không đổi (ADMIN không đọc nháp giáo viên, giáo viên
             * không đọc nháp ADMIN) → REVIEW: tự làm mới trạng thái, không nhắc tải lại. Phần nháp bị dựng lại khi
             * bỏ nháp đã được báo riêng (DRAFT kèm phía) qua CourseAccessGuard.
             */
            case SUBMITTED, WITHDRAWN, REJECTED, DISCARDED -> Scope.REVIEW;
        };
    }

    static Scope scopeOf(CourseReviewEvent event) {
        return switch (event.getAction()) {
            case APPROVED, SAVED -> Scope.PUBLISHED;
            case PUBLISHED, UNPUBLISHED -> Scope.PUBLISHED;
            case SUBMITTED, WITHDRAWN, REJECTED -> Scope.REVIEW;
        };
    }

    private void push(Long courseId, Scope scope, Long actorId, DraftKind draftKind) {
        try {
            publisher.publish(courseId, scope, actorId, draftKind);
        } catch (Exception ex) {
            log.warn("[COURSE-CONTENT] realtime push failed (course {}, {}): {}", courseId, scope, ex.getMessage());
        }
    }
}
