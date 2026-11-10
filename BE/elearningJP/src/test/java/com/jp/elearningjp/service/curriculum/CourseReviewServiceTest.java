package com.jp.elearningjp.service.curriculum;

import com.jp.elearningjp.service.curriculum.snapshot.CourseSnapshotService;
import com.jp.elearningjp.dto.response.curriculum.CourseReviewRequestResponse;
import com.jp.elearningjp.entity.curriculum.Course;
import com.jp.elearningjp.entity.curriculum.CourseReviewRequest;
import com.jp.elearningjp.entity.user.User;
import com.jp.elearningjp.exception.AppException;
import com.jp.elearningjp.exception.ErrorCode;
import com.jp.elearningjp.repository.curriculum.CourseRepository;
import com.jp.elearningjp.repository.curriculum.CourseReviewCommentRepository;
import com.jp.elearningjp.repository.curriculum.CourseReviewRequestRepository;
import com.jp.elearningjp.repository.curriculum.LessonRepository;
import com.jp.elearningjp.repository.user.UserRepository;
import com.jp.elearningjp.shared.enums.CourseReviewStatus;
import com.jp.elearningjp.shared.enums.ReviewRequestStatus;
import com.jp.elearningjp.shared.event.curriculum.CoursePublishStatusChangedEvent;
import com.jp.elearningjp.shared.event.curriculum.CourseReviewEvent;
import com.jp.elearningjp.shared.util.CourseAccessGuard;
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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Luồng duyệt: gửi / rút / duyệt / từ chối, và ADMIN tự xuất bản. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CourseReviewServiceTest {

    private static final Long COURSE_ID = 10L;
    private static final Long TEACHER_ID = 5L;
    private static final Long OTHER_TEACHER_ID = 6L;
    private static final Long ADMIN_ID = 1L;
    private static final Long REQUEST_ID = 100L;

    @Mock CourseRepository courseRepository;
    @Mock CourseReviewRequestRepository reviewRequestRepository;
    @Mock LessonRepository lessonRepository;
    @Mock UserRepository userRepository;
    @Mock CurrentUserService currentUserService;
    @Mock CourseAccessGuard courseAccessGuard;
    @Mock ApplicationEventPublisher eventPublisher;

    @Mock CourseSnapshotService courseSnapshotService;
    @Mock CourseReviewCommentRepository reviewCommentRepository;
    @InjectMocks CourseReviewService service;

    private Course course;

    private static User user(Long id) {
        User u = User.builder().email("u" + id + "@test.local").fullName("U" + id).build();
        u.setId(id);
        return u;
    }

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

    private CourseReviewRequest pending(Long submitterId) {
        CourseReviewRequest r = CourseReviewRequest.builder()
                .course(course).submittedBy(user(submitterId)).status(ReviewRequestStatus.PENDING).build();
        r.setId(REQUEST_ID);
        when(reviewRequestRepository.findById(REQUEST_ID)).thenReturn(Optional.of(r));
        when(reviewRequestRepository.findByCourseIdAndStatus(COURSE_ID, ReviewRequestStatus.PENDING))
                .thenReturn(Optional.of(r));
        course.setReviewStatus(CourseReviewStatus.PENDING_REVIEW);
        return r;
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
        when(reviewRequestRepository.findByCourseIdAndStatus(COURSE_ID, ReviewRequestStatus.PENDING))
                .thenReturn(Optional.empty());
        when(reviewRequestRepository.save(any(CourseReviewRequest.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.getReferenceById(any())).thenAnswer(inv -> user(inv.getArgument(0)));
        actAs(TEACHER_ID, false);
    }

    // ---------------- submit

    @Test
    void submit_locksCourseAndNotifiesAdmins() {
        CourseReviewRequestResponse res = service.submit(COURSE_ID, "  Đã soạn xong bài 1-3  ");

        verify(courseAccessGuard).requireAssignedTeacher(course, ErrorCode.COURSE_REVIEW_SUBMIT_FORBIDDEN);
        assertThat(course.getReviewStatus()).isEqualTo(CourseReviewStatus.PENDING_REVIEW);
        assertThat(res.getStatus()).isEqualTo(ReviewRequestStatus.PENDING);
        assertThat(res.getNote()).isEqualTo("Đã soạn xong bài 1-3");
        assertThat(res.getSubmittedById()).isEqualTo(TEACHER_ID);
        CourseReviewEvent event = capturedReviewEvent();
        assertThat(event.getAction()).isEqualTo(CourseReviewEvent.Action.SUBMITTED);
        assertThat(event.getActorId()).isEqualTo(TEACHER_ID);
    }

    @Test
    void submit_notAssigned_rejected() {
        doThrow(new AppException(ErrorCode.COURSE_REVIEW_SUBMIT_FORBIDDEN))
                .when(courseAccessGuard).requireAssignedTeacher(course, ErrorCode.COURSE_REVIEW_SUBMIT_FORBIDDEN);

        assertRejected(() -> service.submit(COURSE_ID, null), ErrorCode.COURSE_REVIEW_SUBMIT_FORBIDDEN);
        verify(reviewRequestRepository, never()).save(any());
    }

    @Test
    void submit_byAdmin_rejected_adminOnlyReviews() {
        actAs(ADMIN_ID, true);

        assertRejected(() -> service.submit(COURSE_ID, null), ErrorCode.COURSE_REVIEW_SUBMIT_FORBIDDEN);
        verify(reviewRequestRepository, never()).save(any());
    }

    @Test
    void submit_alreadyPending_rejected() {
        course.setReviewStatus(CourseReviewStatus.PENDING_REVIEW);

        assertRejected(() -> service.submit(COURSE_ID, null), ErrorCode.COURSE_REVIEW_ALREADY_PENDING);
    }

    @Test
    void submit_noChanges_rejected() {
        course.setHasUnpublishedChanges(false);

        assertRejected(() -> service.submit(COURSE_ID, null), ErrorCode.COURSE_REVIEW_NO_CHANGES);
    }

    @Test
    void submit_noLessons_rejected() {
        when(lessonRepository.countByCourseIdAndDeletedFalse(COURSE_ID)).thenReturn(0L);

        assertRejected(() -> service.submit(COURSE_ID, null), ErrorCode.COURSE_REVIEW_NO_LESSONS);
    }

    // ---------------- withdraw

    @Test
    void withdraw_bySubmitter_unlocksCourse() {
        CourseReviewRequest r = pending(TEACHER_ID);

        service.withdraw(COURSE_ID);

        assertThat(r.getStatus()).isEqualTo(ReviewRequestStatus.WITHDRAWN);
        assertThat(r.getReviewedAt()).isNotNull();
        assertThat(course.getReviewStatus()).isEqualTo(CourseReviewStatus.EDITING);
        assertThat(capturedReviewEvent().getAction()).isEqualTo(CourseReviewEvent.Action.WITHDRAWN);
    }

    @Test
    void withdraw_onlySubmitter_notOtherTeacherNorAdmin() {
        CourseReviewRequest r = pending(TEACHER_ID);
        actAs(OTHER_TEACHER_ID, false);
        assertRejected(() -> service.withdraw(COURSE_ID), ErrorCode.COURSE_REVIEW_WITHDRAW_FORBIDDEN);

        // ADMIN muốn trả lại thì từ chối kèm lý do
        actAs(ADMIN_ID, true);
        assertRejected(() -> service.withdraw(COURSE_ID), ErrorCode.COURSE_REVIEW_WITHDRAW_FORBIDDEN);
        assertThat(r.getStatus()).isEqualTo(ReviewRequestStatus.PENDING);
    }

    @Test
    void withdraw_nothingPending_rejected() {
        assertRejected(() -> service.withdraw(COURSE_ID), ErrorCode.COURSE_REVIEW_NOT_PENDING);
    }

    // ---------------- approve / reject

    @Test
    void approve_draftCourse_publishesAndNotifiesStudentsOfNewCourse() {
        CourseReviewRequest r = pending(TEACHER_ID);
        actAs(ADMIN_ID, true);

        service.approve(REQUEST_ID, "Tốt");

        assertThat(r.getStatus()).isEqualTo(ReviewRequestStatus.APPROVED);
        assertThat(r.getReviewNote()).isEqualTo("Tốt");
        assertThat(course.getPublished()).isTrue();
        assertThat(course.getReviewStatus()).isEqualTo(CourseReviewStatus.EDITING);
        assertThat(course.getHasUnpublishedChanges()).isFalse();
        verify(eventPublisher).publishEvent(any(CoursePublishStatusChangedEvent.class));
        CourseReviewEvent event = capturedReviewEvent();
        assertThat(event.getAction()).isEqualTo(CourseReviewEvent.Action.APPROVED);
        assertThat(event.isContentUpdatedWhilePublished()).isFalse();
    }

    @Test
    void approve_alreadyPublishedCourse_flagsContentUpdate_noPublishStatusEvent() {
        course.setPublished(true);
        pending(TEACHER_ID);
        actAs(ADMIN_ID, true);

        service.approve(REQUEST_ID, null);

        verify(eventPublisher, never()).publishEvent(any(CoursePublishStatusChangedEvent.class));
        assertThat(capturedReviewEvent().isContentUpdatedWhilePublished()).isTrue();
    }

    @Test
    void reject_keepsPublishedState_unlocksAndCarriesReason() {
        CourseReviewRequest r = pending(TEACHER_ID);
        actAs(ADMIN_ID, true);

        service.reject(REQUEST_ID, " Thiếu ví dụ bài 2 ");

        assertThat(r.getStatus()).isEqualTo(ReviewRequestStatus.REJECTED);
        assertThat(course.getPublished()).isFalse();
        assertThat(course.getReviewStatus()).isEqualTo(CourseReviewStatus.EDITING);
        assertThat(course.getHasUnpublishedChanges()).isTrue();
        CourseReviewEvent event = capturedReviewEvent();
        assertThat(event.getAction()).isEqualTo(CourseReviewEvent.Action.REJECTED);
        assertThat(event.getNote()).isEqualTo("Thiếu ví dụ bài 2");
    }

    @Test
    void reject_withLessonComments_notificationMentionsCount() {
        pending(TEACHER_ID);
        actAs(ADMIN_ID, true);
        when(reviewCommentRepository.countByReviewRequestId(REQUEST_ID)).thenReturn(3L);

        service.reject(REQUEST_ID, "Cần sửa");

        assertThat(capturedReviewEvent().getNote()).isEqualTo("Cần sửa (kèm 3 nhận xét theo bài)");
    }

    @Test
    void approveOrReject_notPending_rejected() {
        CourseReviewRequest r = pending(TEACHER_ID);
        r.setStatus(ReviewRequestStatus.WITHDRAWN);

        assertRejected(() -> service.approve(REQUEST_ID, null), ErrorCode.COURSE_REVIEW_NOT_PENDING);
        assertRejected(() -> service.reject(REQUEST_ID, "x"), ErrorCode.COURSE_REVIEW_NOT_PENDING);
        assertThat(course.getPublished()).isFalse();
    }

    @Test
    void approve_unknownOrDeletedCourseRequest_notFound() {
        when(reviewRequestRepository.findById(999L)).thenReturn(Optional.empty());
        assertRejected(() -> service.approve(999L, null), ErrorCode.COURSE_REVIEW_REQUEST_NOT_FOUND);

        CourseReviewRequest r = pending(TEACHER_ID);
        r.getCourse().softDelete();
        assertRejected(() -> service.approve(REQUEST_ID, null), ErrorCode.COURSE_REVIEW_REQUEST_NOT_FOUND);
    }

    // ---------------- admin mở lại / chuyển nháp (không qua yêu cầu)

    @Test
    void adminReopen_doesNotTouchPendingRequestOrDraft_noNewSnapshot() {
        CourseReviewRequest r = pending(TEACHER_ID);
        actAs(ADMIN_ID, true);

        service.onAdminPublishChanged(course, true);

        // Mở lại dùng bản đã duyệt: yêu cầu đang chờ và bản đang soạn giữ nguyên
        assertThat(r.getStatus()).isEqualTo(ReviewRequestStatus.PENDING);
        assertThat(course.getReviewStatus()).isEqualTo(CourseReviewStatus.PENDING_REVIEW);
        assertThat(course.getHasUnpublishedChanges()).isTrue();
        verify(courseSnapshotService, never()).publish(any(), any());
        assertThat(capturedReviewEvent().getAction()).isEqualTo(CourseReviewEvent.Action.PUBLISHED);
    }

    @Test
    void adminUnpublish_keepsPendingRequestAndChanges() {
        CourseReviewRequest r = pending(TEACHER_ID);
        actAs(ADMIN_ID, true);

        service.onAdminPublishChanged(course, false);

        assertThat(r.getStatus()).isEqualTo(ReviewRequestStatus.PENDING);
        assertThat(course.getHasUnpublishedChanges()).isTrue();
        verify(reviewRequestRepository, times(0)).save(r);
        assertThat(capturedReviewEvent().getAction()).isEqualTo(CourseReviewEvent.Action.UNPUBLISHED);
    }

    @Test
    void history_checksAccess() {
        when(reviewRequestRepository.findByCourseIdOrderByCreatedAtDesc(COURSE_ID)).thenReturn(List.of());

        assertThat(service.getHistory(COURSE_ID)).isEmpty();
        verify(courseAccessGuard).requireAssignedOrAdmin(course);
    }

    @Test
    void queue_withKeyword_searchesLowercasePattern() {
        when(reviewRequestRepository.searchQueue(any(), any(), any()))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of()));

        service.getQueue(null, "  Khóa N5 ", 1, 10);

        verify(reviewRequestRepository).searchQueue(
                org.mockito.ArgumentMatchers.eq(ReviewRequestStatus.PENDING),
                org.mockito.ArgumentMatchers.eq("%khóa n5%"), any());
        verify(reviewRequestRepository, never()).findByStatusAndCourseDeletedFalseOrderByCreatedAtAsc(any(), any());
    }
}
