package com.jp.elearningjp.service.curriculum;

import com.jp.elearningjp.entity.curriculum.Course;
import com.jp.elearningjp.entity.curriculum.CourseEnrollment;
import com.jp.elearningjp.entity.user.User;
import com.jp.elearningjp.exception.AppException;
import com.jp.elearningjp.exception.ErrorCode;
import com.jp.elearningjp.mapper.curriculum.EnrollmentMapper;
import com.jp.elearningjp.repository.curriculum.CourseEnrollmentRepository;
import com.jp.elearningjp.repository.curriculum.CourseRepository;
import com.jp.elearningjp.repository.curriculum.UserLessonProgressRepository;
import com.jp.elearningjp.shared.util.CurrentUserService;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Đăng ký lại hoặc đăng ký trùng phải trả 409, không phải 500. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EnrollmentServiceTest {

    private static final Long COURSE_ID = 10L;
    private static final Long USER_ID = 1L;

    @Mock CourseEnrollmentRepository enrollmentRepository;
    @Mock CourseRepository courseRepository;
    @Mock UserLessonProgressRepository progressRepository;
    @Mock EnrollmentMapper enrollmentMapper;
    @Mock CurrentUserService currentUserService;
    @Mock ApplicationEventPublisher eventPublisher;
    @Mock com.jp.elearningjp.shared.util.CourseAccessGuard courseAccessGuard;

    @InjectMocks EnrollmentService service;

    @BeforeEach
    void setUp() {
        User user = mock(User.class);
        when(user.getId()).thenReturn(USER_ID);
        when(currentUserService.getCurrentUser()).thenReturn(user);

        Course course = new Course();
        course.setId(COURSE_ID);
        course.setPublished(true);
        when(courseRepository.findByIdAndDeletedFalse(COURSE_ID)).thenReturn(Optional.of(course));
    }

    private void assertRejected(ThrowingCallable call, ErrorCode expected) {
        assertThatThrownBy(call)
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(expected);
    }

    @Test
    void enroll_alreadyHasEnrollmentInAnyStatus_conflict() {
        // Query không lọc status: enrollment COMPLETED cũng bị chặn ở đây, không rơi xuống index.
        when(enrollmentRepository.existsByUserIdAndCourseIdAndDeletedFalse(USER_ID, COURSE_ID)).thenReturn(true);

        assertRejected(() -> service.enrollInCourse(COURSE_ID), ErrorCode.ALREADY_ENROLLED);

        verify(enrollmentRepository, never()).saveAndFlush(any());
    }

    @Test
    void enroll_concurrentRequestHitsUniqueIndex_conflict() {
        when(enrollmentRepository.saveAndFlush(any(CourseEnrollment.class)))
                .thenThrow(new DataIntegrityViolationException("uq_course_enrollments_active"));

        assertRejected(() -> service.enrollInCourse(COURSE_ID), ErrorCode.ALREADY_ENROLLED);

        verify(eventPublisher, never()).publishEvent(any(Object.class));
    }

    private Course draftCourse() {
        Course draft = new Course();
        draft.setId(COURSE_ID);
        draft.setPublished(false);
        when(courseRepository.findByIdAndDeletedFalse(COURSE_ID)).thenReturn(Optional.of(draft));
        return draft;
    }

    @Test
    void enroll_draftCourse_strangerGetsNotFound() {
        draftCourse();
        assertRejected(() -> service.enrollInCourse(COURSE_ID), ErrorCode.COURSE_NOT_FOUND);
    }

    @Test
    void enroll_draftCourse_ownerGetsRealReason() {
        Course draft = draftCourse();
        when(courseAccessGuard.isAssignedTeacher(draft, USER_ID)).thenReturn(true);
        assertRejected(() -> service.enrollInCourse(COURSE_ID), ErrorCode.COURSE_NOT_PUBLISHED);
    }

    @Test
    void getEnrollmentById_deletedOrCourseDeleted_notFound() {
        when(enrollmentRepository.findByIdAndDeletedFalseAndCourse_DeletedFalse(5L)).thenReturn(Optional.empty());

        assertRejected(() -> service.getEnrollmentById(5L), ErrorCode.ENROLLMENT_NOT_FOUND);
    }
}
