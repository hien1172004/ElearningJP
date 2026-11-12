package com.jp.elearningjp.service.curriculum;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jp.elearningjp.entity.curriculum.Course;
import com.jp.elearningjp.entity.user.User;
import com.jp.elearningjp.exception.AppException;
import com.jp.elearningjp.exception.ErrorCode;
import com.jp.elearningjp.repository.curriculum.CourseRepository;
import com.jp.elearningjp.repository.curriculum.CourseTeacherRepository;
import com.jp.elearningjp.shared.enums.DraftKind;
import com.jp.elearningjp.shared.event.curriculum.CourseContentChangedEvent;
import com.jp.elearningjp.shared.util.CurrentUserService;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Khóa sửa khóa học theo phía (V65) trên Redis thật (script Lua so khớp người giữ, TTL). Cần Docker. */
@Testcontainers
class CourseEditLockServiceIT {

    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    private static LettuceConnectionFactory factory;
    private static StringRedisTemplate redis;

    private final CurrentUserService currentUser = mock(CurrentUserService.class);
    private final CourseRepository courseRepository = mock(CourseRepository.class);
    private final CourseTeacherRepository courseTeacherRepository = mock(CourseTeacherRepository.class);
    private final com.jp.elearningjp.repository.curriculum.LessonReviewRequestRepository requestRepository =
            mock(com.jp.elearningjp.repository.curriculum.LessonReviewRequestRepository.class);
    private Course course;
    private final ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
    private final ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build();
    private CourseEditLockService service;

    private static final Long COURSE_ID = 10L;
    private static final Long ADMIN_A = 1L;
    private static final Long ADMIN_B = 2L;
    private static final Long TEACHER_X = 5L;
    private static final Long TEACHER_Y = 6L;
    private static final Long OUTSIDER = 9L;

    @BeforeAll
    static void connect() {
        factory = new LettuceConnectionFactory(REDIS.getHost(), REDIS.getMappedPort(6379));
        factory.afterPropertiesSet();
        redis = new StringRedisTemplate(factory);
        redis.afterPropertiesSet();
    }

    @AfterAll
    static void close() {
        factory.destroy();
    }

    private void actAs(Long userId, boolean admin) {
        when(currentUser.getCurrentUserId()).thenReturn(userId);
        when(currentUser.isAdmin()).thenReturn(admin);
        User u = User.builder().email("u" + userId + "@t").fullName("Người " + userId).build();
        u.setId(userId);
        when(currentUser.getCurrentUserOrNull()).thenReturn(u);
    }

    private static void assertRejected(ThrowingCallable call, ErrorCode expected) {
        assertThatThrownBy(call).isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode()).isEqualTo(expected);
    }

    @BeforeEach
    void setUp() {
        redis.delete(CourseEditLockService.key(DraftKind.TEACHER, COURSE_ID));
        redis.delete(CourseEditLockService.key(DraftKind.ADMIN, COURSE_ID));
        course = Course.builder().title("C").build();
        course.setId(COURSE_ID);
        when(courseRepository.findByIdAndDeletedFalse(COURSE_ID)).thenReturn(Optional.of(course));
        when(courseTeacherRepository.existsByCourseIdAndTeacherId(eq(COURSE_ID), anyLong())).thenReturn(false);
        when(courseTeacherRepository.existsByCourseIdAndTeacherId(COURSE_ID, TEACHER_X)).thenReturn(true);
        when(courseTeacherRepository.existsByCourseIdAndTeacherId(COURSE_ID, TEACHER_Y)).thenReturn(true);
        service = new CourseEditLockService(redis, objectMapper, courseRepository, currentUser,
                courseTeacherRepository, requestRepository, publisher, Clock.fixed(Instant.parse("2026-11-12T03:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void teacherLock_blocksOtherTeacher_butNotAdminSide() {
        actAs(TEACHER_X, false);
        service.acquire(COURSE_ID, false);
        assertThat(redis.getExpire(CourseEditLockService.key(DraftKind.TEACHER, COURSE_ID))).isPositive();
        service.requireHeld(COURSE_ID);

        actAs(TEACHER_Y, false);
        assertThatThrownBy(() -> service.requireHeld(COURSE_ID))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("Người 5 đang sửa khóa học này");
        assertRejected(() -> service.acquire(COURSE_ID, false), ErrorCode.COURSE_EDIT_LOCKED_BY_OTHER);

        // Phía ADMIN có khóa riêng: không bị giáo viên chặn
        actAs(ADMIN_A, true);
        service.acquire(COURSE_ID, false);
        service.requireHeld(COURSE_ID);

        ArgumentCaptor<CourseContentChangedEvent> pushed = ArgumentCaptor.forClass(CourseContentChangedEvent.class);
        verify(publisher, atLeastOnce()).publishEvent(pushed.capture());
        assertThat(pushed.getValue().getScope()).isEqualTo(CourseContentChangedEvent.Scope.LOCK);
    }

    @Test
    void noLock_writeRequiresSession() {
        actAs(TEACHER_X, false);
        assertRejected(() -> service.requireHeld(COURSE_ID), ErrorCode.COURSE_EDIT_SESSION_REQUIRED);
        actAs(ADMIN_A, true);
        assertRejected(() -> service.requireHeld(COURSE_ID), ErrorCode.COURSE_EDIT_SESSION_REQUIRED);
    }

    @Test
    void takeover_oldHolderLosesSession_heartbeatAndReleaseOnlyByOwner() {
        actAs(ADMIN_A, true);
        service.acquire(COURSE_ID, false);

        actAs(ADMIN_B, true);
        service.acquire(COURSE_ID, true);
        assertThat(service.read(DraftKind.ADMIN, COURSE_ID)).get()
                .extracting(CourseEditLockService.LockInfo::userId).isEqualTo(ADMIN_B);

        actAs(ADMIN_A, true);
        assertRejected(() -> service.heartbeat(COURSE_ID), ErrorCode.COURSE_EDIT_SESSION_REQUIRED);
        assertRejected(() -> service.requireHeld(COURSE_ID), ErrorCode.COURSE_EDIT_LOCKED_BY_OTHER);
        service.release(COURSE_ID); // không phải người giữ → không xóa
        assertThat(service.read(DraftKind.ADMIN, COURSE_ID)).isPresent();

        actAs(ADMIN_B, true);
        service.heartbeat(COURSE_ID);
        service.release(COURSE_ID);
        assertThat(service.read(DraftKind.ADMIN, COURSE_ID)).isEmpty();
    }

    @Test
    void reacquireBySameUser_keepsOriginalSince() {
        actAs(TEACHER_X, false);
        CourseEditLockService.LockInfo first = service.acquire(COURSE_ID, false);
        CourseEditLockService.LockInfo again = service.acquire(COURSE_ID, false);
        assertThat(again).isEqualTo(first);
    }

    @Test
    void admin_blockedWhileLessonsPending_listsThem_teacherNotBlocked() {
        when(requestRepository.findPendingLessonTitles(COURSE_ID)).thenReturn(java.util.List.of("Bài 2", "Bài 4"));

        actAs(ADMIN_A, true);
        assertThatThrownBy(() -> service.acquire(COURSE_ID, false))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("2 bài chờ duyệt (\"Bài 2\", \"Bài 4\")")
                .extracting(e -> ((AppException) e).getErrorCode()).isEqualTo(ErrorCode.COURSE_EDIT_BLOCKED_BY_PENDING);
        assertThat(service.read(DraftKind.ADMIN, COURSE_ID)).isEmpty();

        // Giáo viên vẫn sửa được (nháp đã gửi tự khóa theo bài)
        actAs(TEACHER_X, false);
        service.acquire(COURSE_ID, false);
        service.requireHeld(COURSE_ID);
    }

    @Test
    void admin_holdingLock_teacherSubmitsMidSession_adminWritesBlocked() {
        actAs(ADMIN_A, true);
        service.acquire(COURSE_ID, false);
        service.requireHeld(COURSE_ID);

        when(requestRepository.findPendingLessonTitles(COURSE_ID)).thenReturn(java.util.List.of("Bài 3"));
        assertRejected(() -> service.requireHeld(COURSE_ID), ErrorCode.COURSE_EDIT_BLOCKED_BY_PENDING);

        // Cả khóa chờ duyệt (luồng cũ) cũng chặn
        when(requestRepository.findPendingLessonTitles(COURSE_ID)).thenReturn(java.util.List.of());
        course.setReviewStatus(com.jp.elearningjp.shared.enums.CourseReviewStatus.PENDING_REVIEW);
        assertRejected(() -> service.requireHeld(COURSE_ID), ErrorCode.COURSE_EDIT_BLOCKED_BY_PENDING);
    }

    @Test
    void notAssignedTeacher_cannotLock() {
        actAs(OUTSIDER, false);
        assertRejected(() -> service.acquire(COURSE_ID, false), ErrorCode.FORBIDDEN_COURSE_ACCESS);
    }

    @Test
    void corruptedValue_ignored() {
        redis.opsForValue().set(CourseEditLockService.key(DraftKind.TEACHER, COURSE_ID), "{rac");
        actAs(TEACHER_X, false);
        assertThat(service.read(DraftKind.TEACHER, COURSE_ID)).isEmpty();
        service.acquire(COURSE_ID, true); // ghi đè giá trị hỏng
        service.requireHeld(COURSE_ID);
    }
}
