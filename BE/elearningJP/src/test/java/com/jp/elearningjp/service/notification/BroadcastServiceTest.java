package com.jp.elearningjp.service.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jp.elearningjp.dto.request.notification.BroadcastRequest;
import com.jp.elearningjp.dto.response.notification.BroadcastResponse;
import com.jp.elearningjp.entity.curriculum.Course;
import com.jp.elearningjp.entity.gamification.NotificationBroadcast;
import com.jp.elearningjp.entity.user.User;
import com.jp.elearningjp.exception.AppException;
import com.jp.elearningjp.exception.ErrorCode;
import com.jp.elearningjp.repository.curriculum.CourseRepository;
import com.jp.elearningjp.repository.gamification.NotificationBroadcastRepository;
import com.jp.elearningjp.shared.enums.BroadcastAudience;
import com.jp.elearningjp.shared.util.CourseAccessGuard;
import com.jp.elearningjp.shared.util.CurrentUserService;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class BroadcastServiceTest {

    private static final Long SENDER_ID = 1L;

    private NotificationBroadcastRepository repository;
    private CourseRepository courseRepository;
    private NotificationService notificationService;
    private CurrentUserService currentUserService;
    private CourseAccessGuard courseAccessGuard;
    private BroadcastService service;
    private Course course;

    @BeforeEach
    void setUp() {
        repository = mock(NotificationBroadcastRepository.class);
        courseRepository = mock(CourseRepository.class);
        notificationService = mock(NotificationService.class);
        currentUserService = mock(CurrentUserService.class);
        courseAccessGuard = mock(CourseAccessGuard.class);
        service = new BroadcastService(repository, courseRepository, notificationService,
                currentUserService, courseAccessGuard, new ObjectMapper());

        User sender = User.builder().email("s@t.local").fullName("Sender").build();
        sender.setId(SENDER_ID);
        when(currentUserService.getCurrentUser()).thenReturn(sender);
        when(currentUserService.getCurrentUserId()).thenReturn(SENDER_ID);
        when(repository.save(any(NotificationBroadcast.class))).thenAnswer(inv -> {
            NotificationBroadcast b = inv.getArgument(0);
            if (b.getId() == null) b.setId(55L);
            return b;
        });

        course = new Course();
        course.setId(10L);
        course.setTitle("JLPT N5");
        when(courseRepository.findByIdAndDeletedFalse(10L)).thenReturn(Optional.of(course));
    }

    private static BroadcastRequest request(BroadcastAudience audience, String role, Long courseId, String link) {
        return BroadcastRequest.builder().title(" Lịch thi ").body(" Nội dung ").audience(audience)
                .role(role).courseId(courseId).link(link).build();
    }

    private void assertRejected(ThrowingCallable call, ErrorCode code) {
        assertThatThrownBy(call).isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode()).isEqualTo(code);
    }

    @Test
    void teacher_cannotSendToAllUsersOrByRole() {
        when(currentUserService.isAdmin()).thenReturn(false);

        assertRejected(() -> service.send(request(BroadcastAudience.ALL_USERS, null, null, null)), ErrorCode.BROADCAST_FORBIDDEN);
        assertRejected(() -> service.send(request(BroadcastAudience.ROLE, "USER", null, null)), ErrorCode.BROADCAST_FORBIDDEN);
        verify(repository, never()).insertNotifications(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void teacher_notOwnerOfCourse_forbiddenByGuard() {
        when(currentUserService.isAdmin()).thenReturn(false);
        doThrow(new AppException(ErrorCode.BROADCAST_FORBIDDEN))
                .when(courseAccessGuard).requireAssignedOrAdmin(course, ErrorCode.BROADCAST_FORBIDDEN);

        assertRejected(() -> service.send(request(BroadcastAudience.COURSE_STUDENTS, null, 10L, null)),
                ErrorCode.BROADCAST_FORBIDDEN);
    }

    @Test
    void teacher_ownCourse_sendsWithCourseInData() {
        when(currentUserService.isAdmin()).thenReturn(false);
        when(repository.countRecipients(SENDER_ID, "COURSE_STUDENTS", null, 10L)).thenReturn(3L);
        when(repository.insertNotifications(eq(SENDER_ID), eq("COURSE_STUDENTS"), isNull(), eq(10L),
                eq("Lịch thi"), eq("Nội dung"), anyString(), any())).thenReturn(3);

        BroadcastResponse res = service.send(request(BroadcastAudience.COURSE_STUDENTS, null, 10L, " "));

        assertThat(res.getRecipientCount()).isEqualTo(3);
        assertThat(res.getCourseTitle()).isEqualTo("JLPT N5");
        assertThat(res.getLink()).isNull();
        ArgumentCaptor<String> data = ArgumentCaptor.forClass(String.class);
        verify(repository).insertNotifications(any(), any(), any(), any(), any(), any(), data.capture(), any());
        assertThat(data.getValue()).contains("\"broadcastId\":55").contains("\"courseId\":10").doesNotContain("link");
        // Không có transaction trong unit test → đẩy realtime chạy ngay
        verify(notificationService).pushBroadcast(55L);
    }

    @Test
    void admin_byRole_normalizesRoleName() {
        when(currentUserService.isAdmin()).thenReturn(true);
        when(repository.countRecipients(SENDER_ID, "ROLE", "TEACHER", null)).thenReturn(2L);

        assertThat(service.countRecipients(BroadcastAudience.ROLE, "role_teacher", null)).isEqualTo(2L);
    }

    @Test
    void invalidRoleOrMissingCourse_rejected() {
        when(currentUserService.isAdmin()).thenReturn(true);

        assertRejected(() -> service.countRecipients(BroadcastAudience.ROLE, "SUPERADMIN", null),
                ErrorCode.BROADCAST_INVALID_AUDIENCE);
        assertRejected(() -> service.countRecipients(BroadcastAudience.COURSE_STUDENTS, null, null),
                ErrorCode.BROADCAST_INVALID_AUDIENCE);
    }

    @Test
    void noRecipients_rejected_withoutSavingBroadcast() {
        when(currentUserService.isAdmin()).thenReturn(true);
        when(repository.countRecipients(any(), any(), any(), any())).thenReturn(0L);

        assertRejected(() -> service.send(request(BroadcastAudience.ALL_USERS, null, null, null)),
                ErrorCode.BROADCAST_NO_RECIPIENTS);
        verify(repository, never()).save(any());
    }

    @Test
    void pushFailure_doesNotFailSend() {
        when(currentUserService.isAdmin()).thenReturn(true);
        when(repository.countRecipients(any(), any(), any(), any())).thenReturn(1L);
        when(repository.insertNotifications(any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(1);
        when(notificationService.pushBroadcast(anyLong())).thenThrow(new RuntimeException("redis down"));

        BroadcastResponse res = service.send(request(BroadcastAudience.ALL_USERS, null, null, "/courses"));

        assertThat(res.getRecipientCount()).isEqualTo(1);
        assertThat(res.getLink()).isEqualTo("/courses");
    }

    @Test
    @SuppressWarnings("unchecked")
    void history_withFilters_usesSpecificationQuery() {
        when(currentUserService.isAdmin()).thenReturn(false);
        when(repository.findAll(any(Specification.class), any(Pageable.class)))
                .thenAnswer(inv -> new PageImpl<NotificationBroadcast>(List.of(), (Pageable) inv.getArgument(1), 0));

        service.getHistory(1, 10, "lịch thi", BroadcastAudience.COURSE_STUDENTS);

        verify(repository).findAll(any(Specification.class), any(Pageable.class));
        verify(repository, never()).findBySenderIdOrderByCreatedAtDesc(anyLong(), any());
    }

    @Test
    void history_withoutFilters_keepsLegacyQuery() {
        when(currentUserService.isAdmin()).thenReturn(true);
        when(repository.findAllByOrderByCreatedAtDesc(any())).thenReturn(new PageImpl<>(List.of()));

        service.getHistory(1, 10, null, null);

        verify(repository).findAllByOrderByCreatedAtDesc(any());
    }
}
