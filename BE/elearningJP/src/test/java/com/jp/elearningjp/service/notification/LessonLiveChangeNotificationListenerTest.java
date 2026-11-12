package com.jp.elearningjp.service.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jp.elearningjp.repository.curriculum.LessonAutosaveRepository;
import com.jp.elearningjp.repository.curriculum.UserLessonProgressRepository;
import com.jp.elearningjp.service.notification.listener.LessonLiveChangeNotificationListener;
import com.jp.elearningjp.shared.enums.NotificationType;
import com.jp.elearningjp.shared.enums.ProgressStatus;
import com.jp.elearningjp.shared.event.curriculum.CourseReviewEvent;
import com.jp.elearningjp.shared.event.curriculum.LessonReviewEvent;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** DRAFT_STALE cho người còn bản chưa lưu; LESSON_UPDATED cho học viên đã hoàn thành bài khi có thay đổi lớn. */
class LessonLiveChangeNotificationListenerTest {

    private static final Long ADMIN = 1L;
    private final LessonAutosaveRepository autosaves = mock(LessonAutosaveRepository.class);
    private final UserLessonProgressRepository progress = mock(UserLessonProgressRepository.class);
    private final NotificationService notifications = mock(NotificationService.class);
    private final LessonLiveChangeNotificationListener listener =
            new LessonLiveChangeNotificationListener(autosaves, progress, notifications, new ObjectMapper());

    private static LessonReviewEvent lessonEvent(LessonReviewEvent.Action action, boolean published, boolean major) {
        return new LessonReviewEvent(new Object(), 10L, "N5", action, List.of(7L), List.of("Bài 1"),
                ADMIN, 5L, null, published, major);
    }

    @Test
    void approved_majorChange_published_notifiesDraftHoldersAndCompleters_exceptActor() {
        when(autosaves.findUserIdsByLessonIds(List.of(7L))).thenReturn(List.of(5L, 6L, ADMIN));
        when(progress.findUserIdsByLessonIdAndStatus(7L, ProgressStatus.COMPLETED)).thenReturn(List.of(20L, 21L));

        listener.onLessonLiveChanged(lessonEvent(LessonReviewEvent.Action.APPROVED, true, true));

        verify(notifications).create(eq(5L), eq(NotificationType.DRAFT_STALE), anyString(), anyString(), any());
        verify(notifications).create(eq(6L), eq(NotificationType.DRAFT_STALE), anyString(), anyString(), any());
        verify(notifications, never()).create(eq(ADMIN), any(), anyString(), anyString(), any());
        verify(notifications).create(eq(20L), eq(NotificationType.LESSON_UPDATED), anyString(), anyString(), any());
        verify(notifications).create(eq(21L), eq(NotificationType.LESSON_UPDATED), anyString(), anyString(), any());
    }

    @Test
    void saved_withoutMajorChange_orCourseUnpublished_noLearnerNotification() {
        when(autosaves.findUserIdsByLessonIds(List.of(7L))).thenReturn(List.of());

        listener.onLessonLiveChanged(lessonEvent(LessonReviewEvent.Action.SAVED, true, false));
        listener.onLessonLiveChanged(lessonEvent(LessonReviewEvent.Action.SAVED, false, true));

        verify(progress, never()).findUserIdsByLessonIdAndStatus(anyLong(), any());
        verify(notifications, never()).create(anyLong(), any(), anyString(), anyString(), any());
    }

    @Test
    void submittedOrRejected_ignored() {
        listener.onLessonLiveChanged(lessonEvent(LessonReviewEvent.Action.SUBMITTED, true, true));
        listener.onLessonLiveChanged(lessonEvent(LessonReviewEvent.Action.REJECTED, true, true));
        verifyNoInteractions(autosaves, progress, notifications);
    }

    @Test
    void courseSaved_notifiesDraftHoldersOfAnyLessonOnce() {
        when(autosaves.findUserIdsByCourseId(10L)).thenReturn(List.of(5L, 5L, ADMIN));

        listener.onCourseLiveChanged(new CourseReviewEvent(new Object(), 10L, "N5", CourseReviewEvent.Action.SAVED,
                ADMIN, null, true));
        listener.onCourseLiveChanged(new CourseReviewEvent(new Object(), 10L, "N5", CourseReviewEvent.Action.SUBMITTED,
                5L, null, false));

        verify(notifications, times(1)).create(eq(5L), eq(NotificationType.DRAFT_STALE), anyString(), anyString(), any());
        verify(notifications, never()).create(eq(ADMIN), any(), anyString(), anyString(), any());
    }
}
