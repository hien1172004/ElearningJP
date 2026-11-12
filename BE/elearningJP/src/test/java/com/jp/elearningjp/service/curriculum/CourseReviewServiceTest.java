package com.jp.elearningjp.service.curriculum;

import com.jp.elearningjp.entity.curriculum.Course;
import com.jp.elearningjp.entity.curriculum.Lesson;
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
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** ADMIN lưu cả khóa (nháp ADMIN) và sự kiện xuất bản — luồng gửi / duyệt cả khóa đã bỏ. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CourseReviewServiceTest {

    private static final Long COURSE_ID = 10L;
    private static final Long TEACHER_ID = 5L;
    private static final Long ADMIN_ID = 1L;

    @Mock CourseRepository courseRepository;
    @Mock LessonRepository lessonRepository;
    @Mock CurrentUserService currentUserService;
    @Mock ApplicationEventPublisher eventPublisher;
    @Mock CourseSnapshotService courseSnapshotService;
    @Mock LessonDraftService lessonDraftService;
    @InjectMocks CourseReviewService service;

    private Course course;

    private static void assertRejected(ThrowingCallable call, ErrorCode expected) {
        assertThatThrownBy(call)
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(expected);
    }

    private void actAs(Long userId, boolean admin) {
        when(currentUserService.getCurrentUserId()).thenReturn(userId);
        when(currentUserService.isAdmin()).thenReturn(admin);
    }

    private CourseReviewEvent capturedReviewEvent() {
        ArgumentCaptor<ApplicationEvent> captor = ArgumentCaptor.forClass(ApplicationEvent.class);
        verify(eventPublisher, org.mockito.Mockito.atLeastOnce()).publishEvent(captor.capture());
        return captor.getAllValues().stream()
                .filter(CourseReviewEvent.class::isInstance).map(CourseReviewEvent.class::cast)
                .findFirst().orElseThrow();
    }

    @BeforeEach
    void setUp() {
        course = Course.builder().title("JLPT N5").published(false)
                .reviewStatus(CourseReviewStatus.EDITING).hasUnpublishedChanges(true).build();
        course.setId(COURSE_ID);
        when(courseRepository.findByIdAndDeletedFalse(COURSE_ID)).thenReturn(Optional.of(course));
        when(lessonRepository.countByCourseIdAndDeletedFalse(COURSE_ID)).thenReturn(3L);
        actAs(ADMIN_ID, true);
    }

    @Test
    void saveVersion_snapshotsAdminDrafts_withoutPublishing() {
        service.saveVersion(COURSE_ID);

        verify(courseSnapshotService).publish(eq(course), any(), eq(DraftKind.ADMIN));
        assertThat(course.getPublished()).isFalse();
        assertThat(course.getHasUnpublishedChanges()).isFalse();
        verify(lessonRepository).clearLessonChanges(COURSE_ID);
        CourseReviewEvent event = capturedReviewEvent();
        assertThat(event.getAction()).isEqualTo(CourseReviewEvent.Action.SAVED);
        assertThat(event.isContentUpdatedWhilePublished()).isFalse();
    }

    @Test
    void saveVersion_onlySnapshottedLessonsBumpLive_hiddenTeacherLessonUntouched() {
        // Bài 1 ADMIN thấy được chụp; bài mới giáo viên đang soạn (ẩn với ADMIN) không đổi live → nháp không cũ
        Lesson visible = Lesson.builder().course(course).title("Bài 1").orderIndex(1).build();
        visible.setId(1L);
        when(lessonRepository.findVisibleByCourseId(COURSE_ID, DraftKind.ADMIN)).thenReturn(List.of(visible));
        LessonDraft adminDraft = LessonDraft.builder().lesson(visible).kind(DraftKind.ADMIN).build();
        adminDraft.setId(11L);
        when(lessonDraftService.findActive(1L, DraftKind.ADMIN)).thenReturn(Optional.of(adminDraft));

        service.saveVersion(COURSE_ID);

        verify(lessonRepository).bumpLiveRevision(List.of(1L));
        verify(lessonDraftService).afterLivePublished(1L, 11L);
    }

    @Test
    void saveVersion_publishedCourse_flagsContentUpdate() {
        course.setPublished(true);

        service.saveVersion(COURSE_ID);

        assertThat(course.getPublished()).isTrue();
        assertThat(capturedReviewEvent().isContentUpdatedWhilePublished()).isTrue();
    }

    @Test
    void saveVersion_byTeacher_forbidden() {
        actAs(TEACHER_ID, false);
        assertRejected(() -> service.saveVersion(COURSE_ID), ErrorCode.FORBIDDEN_COURSE_ACCESS);
        verify(courseSnapshotService, never()).publish(any(), any(), any());
    }

    @Test
    void saveVersion_lessonsPending_mustApproveOrRejectFirst() {
        when(lessonRepository.countPendingReviewLessons(COURSE_ID)).thenReturn(2L);
        assertRejected(() -> service.saveVersion(COURSE_ID), ErrorCode.COURSE_REVIEW_LESSONS_PENDING);
        verify(courseSnapshotService, never()).publish(any(), any(), any());
    }

    @Test
    void saveVersion_noChangesOrNoLessons_rejected() {
        course.setHasUnpublishedChanges(false);
        assertRejected(() -> service.saveVersion(COURSE_ID), ErrorCode.COURSE_SAVE_NO_CHANGES);

        course.setHasUnpublishedChanges(true);
        when(lessonRepository.countByCourseIdAndDeletedFalse(COURSE_ID)).thenReturn(0L);
        assertRejected(() -> service.saveVersion(COURSE_ID), ErrorCode.COURSE_SAVE_NO_LESSONS);
        verify(courseSnapshotService, never()).publish(any(), any(), any());
    }

    @Test
    void adminReopenOrUnpublish_noNewSnapshot_onlyEvent() {
        service.onAdminPublishChanged(course, true);
        assertThat(capturedReviewEvent().getAction()).isEqualTo(CourseReviewEvent.Action.PUBLISHED);
        verify(courseSnapshotService, never()).publish(any(), any(), any());
        assertThat(course.getHasUnpublishedChanges()).isTrue();
    }
}
