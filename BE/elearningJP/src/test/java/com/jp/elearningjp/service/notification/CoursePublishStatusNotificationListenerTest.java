package com.jp.elearningjp.service.notification;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jp.elearningjp.repository.curriculum.CourseEnrollmentRepository;
import com.jp.elearningjp.service.notification.listener.CoursePublishStatusNotificationListener;
import com.jp.elearningjp.shared.enums.NotificationType;
import com.jp.elearningjp.shared.event.curriculum.CoursePublishStatusChangedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CoursePublishStatusNotificationListenerTest {

    private static final Long COURSE_ID = 10L;
    private static final Long TEACHER_ID = 1L;

    private CourseEnrollmentRepository enrollmentRepository;
    private NotificationService notificationService;
    private CoursePublishStatusNotificationListener listener;

    @BeforeEach
    void setUp() {
        enrollmentRepository = mock(CourseEnrollmentRepository.class);
        notificationService = mock(NotificationService.class);
        listener = new CoursePublishStatusNotificationListener(enrollmentRepository, notificationService, new ObjectMapper());
    }

    private static CoursePublishStatusChangedEvent event(boolean published) {
        return new CoursePublishStatusChangedEvent(new Object(), COURSE_ID, "JLPT N5", published, TEACHER_ID);
    }

    @Test
    void published_notifiesEveryEnrolledStudent_withCourseLink() {
        when(enrollmentRepository.findActiveUserIdsByCourseId(COURSE_ID)).thenReturn(List.of(5L, 6L));

        listener.onCoursePublishStatusChanged(event(true));

        ArgumentCaptor<String> title = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<JsonNode> data = ArgumentCaptor.forClass(JsonNode.class);
        verify(notificationService).create(eq(5L), eq(NotificationType.COURSE_STATUS_CHANGED),
                title.capture(), anyString(), data.capture());
        verify(notificationService).create(eq(6L), eq(NotificationType.COURSE_STATUS_CHANGED),
                anyString(), anyString(), any());
        assertThat(title.getValue()).isEqualTo("Khóa học \"JLPT N5\" đã được xuất bản");
        assertThat(data.getValue().get("courseId").asLong()).isEqualTo(COURSE_ID);
        assertThat(data.getValue().get("published").asBoolean()).isTrue();
        assertThat(data.getValue().get("link").asText()).isEqualTo("/courses/10");
    }

    @Test
    void unpublished_usesDraftMessage() {
        when(enrollmentRepository.findActiveUserIdsByCourseId(COURSE_ID)).thenReturn(List.of(5L));

        listener.onCoursePublishStatusChanged(event(false));

        ArgumentCaptor<String> title = ArgumentCaptor.forClass(String.class);
        verify(notificationService).create(eq(5L), eq(NotificationType.COURSE_STATUS_CHANGED),
                title.capture(), anyString(), any());
        assertThat(title.getValue()).isEqualTo("Khóa học \"JLPT N5\" tạm chuyển về bản nháp");
    }

    @Test
    void skipsTheTeacherWhoChangedTheStatus_andNullIds() {
        when(enrollmentRepository.findActiveUserIdsByCourseId(COURSE_ID)).thenReturn(Arrays.asList(TEACHER_ID, null, 5L));

        listener.onCoursePublishStatusChanged(event(true));

        verify(notificationService, never()).create(eq(TEACHER_ID), any(), anyString(), anyString(), any());
        verify(notificationService, times(1)).create(anyLong(), any(), anyString(), anyString(), any());
    }

    @Test
    void failureForOneStudent_doesNotStopOthers_norThrow() {
        when(enrollmentRepository.findActiveUserIdsByCourseId(COURSE_ID)).thenReturn(List.of(5L, 6L));
        when(notificationService.create(eq(5L), any(), anyString(), anyString(), any()))
                .thenThrow(new RuntimeException("db down"));

        assertThatCode(() -> listener.onCoursePublishStatusChanged(event(true))).doesNotThrowAnyException();

        verify(notificationService).create(eq(6L), any(), anyString(), anyString(), any());
    }

    @Test
    void loadingRecipientsFails_doesNotThrow() {
        when(enrollmentRepository.findActiveUserIdsByCourseId(COURSE_ID)).thenThrow(new RuntimeException("db down"));

        assertThatCode(() -> listener.onCoursePublishStatusChanged(event(true))).doesNotThrowAnyException();
        verifyNoInteractions(notificationService);
    }
}
