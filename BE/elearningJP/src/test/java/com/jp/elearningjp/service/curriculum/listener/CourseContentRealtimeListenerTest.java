package com.jp.elearningjp.service.curriculum.listener;

import com.jp.elearningjp.service.curriculum.realtime.CourseContentPublisher;
import com.jp.elearningjp.shared.event.curriculum.CourseContentChangedEvent;
import com.jp.elearningjp.shared.event.curriculum.CourseContentChangedEvent.Scope;
import com.jp.elearningjp.shared.event.curriculum.CourseReviewEvent;
import com.jp.elearningjp.shared.event.curriculum.CourseReviewEvent.Action;
import com.jp.elearningjp.shared.event.curriculum.LessonReviewEvent;
import com.jp.elearningjp.shared.enums.DraftKind;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

/** Báo realtime "nội dung khóa vừa đổi": đúng phạm vi, lỗi push không lan ra. */
@ExtendWith(MockitoExtension.class)
class CourseContentRealtimeListenerTest {

    @Mock CourseContentPublisher publisher;
    @InjectMocks CourseContentRealtimeListener listener;

    private static CourseReviewEvent review(Action action, boolean whilePublished) {
        return new CourseReviewEvent(new Object(), 10L, "N5", action, 1L, null, whilePublished);
    }

    @Test
    void contentChanged_pushesDraftWithActor() {
        listener.onContentChanged(new CourseContentChangedEvent(new Object(), 10L, Scope.DRAFT, 5L));

        verify(publisher).publish(10L, Scope.DRAFT, 5L, null);
    }

    @Test
    void contentChanged_forwardsDraftSide() {
        listener.onContentChanged(new CourseContentChangedEvent(new Object(), 10L, Scope.DRAFT, 5L, DraftKind.TEACHER));

        verify(publisher).publish(10L, Scope.DRAFT, 5L, DraftKind.TEACHER);
    }

    @Test
    void lessonWithdraw_onlyRefreshesReviewStatus() {
        listener.onLessonReview(lessonReview(LessonReviewEvent.Action.WITHDRAWN));

        verify(publisher).publish(10L, Scope.REVIEW, 1L, null);
    }

    @Test
    void scopeOf_lessonReview() {
        assertThat(CourseContentRealtimeListener.scopeOf(lessonReview(LessonReviewEvent.Action.APPROVED)))
                .isEqualTo(Scope.PUBLISHED);
        assertThat(CourseContentRealtimeListener.scopeOf(lessonReview(LessonReviewEvent.Action.SAVED)))
                .isEqualTo(Scope.PUBLISHED);
        // Gửi / rút / từ chối / bỏ nháp: nội dung người xem không đổi → chỉ làm mới trạng thái duyệt
        for (LessonReviewEvent.Action a : List.of(LessonReviewEvent.Action.SUBMITTED, LessonReviewEvent.Action.WITHDRAWN,
                LessonReviewEvent.Action.REJECTED, LessonReviewEvent.Action.DISCARDED)) {
            assertThat(CourseContentRealtimeListener.scopeOf(lessonReview(a))).as(a.name()).isEqualTo(Scope.REVIEW);
        }
    }

    private static LessonReviewEvent lessonReview(LessonReviewEvent.Action action) {
        return new LessonReviewEvent(new Object(), 10L, "N5", action, List.of(1L), List.of("Bài 1"), 1L, 5L,
                null, false, false);
    }

    @Test
    void review_pushesScopeByAction() {
        listener.onReview(review(Action.SAVED, true));

        verify(publisher).publish(10L, Scope.PUBLISHED, 1L, null);
    }

    @Test
    void scopeOf_newVersionAlwaysReachesLearners() {
        assertThat(CourseContentRealtimeListener.scopeOf(review(Action.APPROVED, true))).isEqualTo(Scope.PUBLISHED);
        assertThat(CourseContentRealtimeListener.scopeOf(review(Action.SAVED, true))).isEqualTo(Scope.PUBLISHED);
        // Khóa đang nháp: học viên đã đăng ký vẫn học bản snapshot → phiên bản mới cũng phải báo họ
        assertThat(CourseContentRealtimeListener.scopeOf(review(Action.APPROVED, false))).isEqualTo(Scope.PUBLISHED);
        assertThat(CourseContentRealtimeListener.scopeOf(review(Action.SAVED, false))).isEqualTo(Scope.PUBLISHED);
        assertThat(CourseContentRealtimeListener.scopeOf(review(Action.UNPUBLISHED, false))).isEqualTo(Scope.PUBLISHED);
        assertThat(CourseContentRealtimeListener.scopeOf(review(Action.PUBLISHED, false))).isEqualTo(Scope.PUBLISHED);
        assertThat(CourseContentRealtimeListener.scopeOf(review(Action.SUBMITTED, false))).isEqualTo(Scope.REVIEW);
        assertThat(CourseContentRealtimeListener.scopeOf(review(Action.WITHDRAWN, false))).isEqualTo(Scope.REVIEW);
        assertThat(CourseContentRealtimeListener.scopeOf(review(Action.REJECTED, false))).isEqualTo(Scope.REVIEW);
    }

    @Test
    void publisherFailure_doesNotPropagate() {
        doThrow(new RuntimeException("broker down")).when(publisher).publish(any(), any(), any(), any());

        assertThatCode(() -> listener.onContentChanged(
                new CourseContentChangedEvent(new Object(), 10L, Scope.DRAFT, 5L))).doesNotThrowAnyException();
    }
}
