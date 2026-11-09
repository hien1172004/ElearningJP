package com.jp.elearningjp.service.curriculum;

import com.jp.elearningjp.dto.response.curriculum.CourseTeacherResponse;
import com.jp.elearningjp.entity.curriculum.Course;
import com.jp.elearningjp.entity.curriculum.CourseTeacher;
import com.jp.elearningjp.entity.user.Role;
import com.jp.elearningjp.entity.user.User;
import com.jp.elearningjp.exception.AppException;
import com.jp.elearningjp.exception.ErrorCode;
import com.jp.elearningjp.repository.curriculum.CourseRepository;
import com.jp.elearningjp.repository.curriculum.CourseTeacherRepository;
import com.jp.elearningjp.repository.user.UserRepository;
import com.jp.elearningjp.shared.enums.UserStatus;
import com.jp.elearningjp.shared.event.curriculum.CourseTeacherAssignmentChangedEvent;
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
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Phân công giáo viên cho khóa học: ADMIN giao / gỡ, giáo viên được giao xem danh sách. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CourseTeacherServiceTest {

    private static final Long COURSE_ID = 10L;
    private static final Long TEACHER_ID = 5L;
    private static final Long ADMIN_ID = 1L;

    @Mock CourseRepository courseRepository;
    @Mock CourseTeacherRepository courseTeacherRepository;
    @Mock UserRepository userRepository;
    @Mock CurrentUserService currentUserService;
    @Mock CourseAccessGuard courseAccessGuard;
    @Mock ApplicationEventPublisher eventPublisher;

    @InjectMocks CourseTeacherService service;

    private Course course;
    private User teacher;

    private static Role role(String name) {
        Role r = new Role();
        r.setName(name);
        return r;
    }

    private static User user(Long id, String roleName) {
        User u = User.builder().email("u" + id + "@test.local").fullName("U" + id)
                .status(UserStatus.ACTIVE).roles(Set.of(role(roleName))).build();
        u.setId(id);
        return u;
    }

    private static void assertRejected(ThrowingCallable call, ErrorCode expected) {
        assertThatThrownBy(call)
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(expected);
    }

    @BeforeEach
    void setUp() {
        course = new Course();
        course.setId(COURSE_ID);
        course.setTitle("JLPT N5");
        teacher = user(TEACHER_ID, "TEACHER");
        when(courseRepository.findByIdAndDeletedFalse(COURSE_ID)).thenReturn(Optional.of(course));
        when(userRepository.findById(TEACHER_ID)).thenReturn(Optional.of(teacher));
        when(userRepository.getReferenceById(ADMIN_ID)).thenReturn(user(ADMIN_ID, "ADMIN"));
        when(currentUserService.getCurrentUserId()).thenReturn(ADMIN_ID);
        when(courseTeacherRepository.save(any(CourseTeacher.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void assign_savesAssignmentAndPublishesEvent() {
        CourseTeacherResponse res = service.assignTeacher(COURSE_ID, TEACHER_ID);

        assertThat(res.getTeacherId()).isEqualTo(TEACHER_ID);
        ArgumentCaptor<CourseTeacher> saved = ArgumentCaptor.forClass(CourseTeacher.class);
        verify(courseTeacherRepository).save(saved.capture());
        assertThat(saved.getValue().getCourse()).isSameAs(course);
        assertThat(saved.getValue().getTeacher()).isSameAs(teacher);
        assertThat(saved.getValue().getAssignedBy().getId()).isEqualTo(ADMIN_ID);

        ArgumentCaptor<CourseTeacherAssignmentChangedEvent> event =
                ArgumentCaptor.forClass(CourseTeacherAssignmentChangedEvent.class);
        verify(eventPublisher).publishEvent(event.capture());
        assertThat(event.getValue().isAssigned()).isTrue();
        assertThat(event.getValue().getTeacherId()).isEqualTo(TEACHER_ID);
        assertThat(event.getValue().getCourseTitle()).isEqualTo("JLPT N5");
    }

    @Test
    void assign_courseMissing_rejected() {
        when(courseRepository.findByIdAndDeletedFalse(COURSE_ID)).thenReturn(Optional.empty());

        assertRejected(() -> service.assignTeacher(COURSE_ID, TEACHER_ID), ErrorCode.COURSE_NOT_FOUND);
        verify(courseTeacherRepository, never()).save(any());
    }

    @Test
    void assign_userWithoutTeacherRole_rejected() {
        when(userRepository.findById(TEACHER_ID)).thenReturn(Optional.of(user(TEACHER_ID, "USER")));

        assertRejected(() -> service.assignTeacher(COURSE_ID, TEACHER_ID), ErrorCode.COURSE_TEACHER_INVALID);
    }

    @Test
    void assign_bannedTeacher_rejected() {
        teacher.setStatus(UserStatus.BANNED);

        assertRejected(() -> service.assignTeacher(COURSE_ID, TEACHER_ID), ErrorCode.COURSE_TEACHER_INVALID);
    }

    @Test
    void assign_deletedOrMissingUser_rejected() {
        teacher.softDelete();
        assertRejected(() -> service.assignTeacher(COURSE_ID, TEACHER_ID), ErrorCode.COURSE_TEACHER_INVALID);

        when(userRepository.findById(TEACHER_ID)).thenReturn(Optional.empty());
        assertRejected(() -> service.assignTeacher(COURSE_ID, TEACHER_ID), ErrorCode.COURSE_TEACHER_INVALID);
    }

    @Test
    void assign_alreadyAssigned_rejected() {
        when(courseTeacherRepository.existsByCourseIdAndTeacherId(COURSE_ID, TEACHER_ID)).thenReturn(true);

        assertRejected(() -> service.assignTeacher(COURSE_ID, TEACHER_ID), ErrorCode.COURSE_TEACHER_ALREADY_ASSIGNED);
        verify(courseTeacherRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void unassign_deletesAssignmentAndPublishesEvent() {
        CourseTeacher assignment = CourseTeacher.builder().course(course).teacher(teacher).build();
        when(courseTeacherRepository.findByCourseIdAndTeacherId(COURSE_ID, TEACHER_ID))
                .thenReturn(Optional.of(assignment));

        service.unassignTeacher(COURSE_ID, TEACHER_ID);

        verify(courseTeacherRepository).delete(assignment);
        ArgumentCaptor<CourseTeacherAssignmentChangedEvent> event =
                ArgumentCaptor.forClass(CourseTeacherAssignmentChangedEvent.class);
        verify(eventPublisher).publishEvent(event.capture());
        assertThat(event.getValue().isAssigned()).isFalse();
    }

    @Test
    void unassign_notAssigned_rejected() {
        when(courseTeacherRepository.findByCourseIdAndTeacherId(COURSE_ID, TEACHER_ID)).thenReturn(Optional.empty());

        assertRejected(() -> service.unassignTeacher(COURSE_ID, TEACHER_ID), ErrorCode.COURSE_TEACHER_NOT_ASSIGNED);
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void getTeachers_checksAccessAndMapsTeachers() {
        CourseTeacher assignment = CourseTeacher.builder().course(course).teacher(teacher).build();
        when(courseTeacherRepository.findByCourseIdWithTeacher(COURSE_ID)).thenReturn(List.of(assignment));

        List<CourseTeacherResponse> res = service.getTeachers(COURSE_ID);

        verify(courseAccessGuard).requireAssignedOrAdmin(course);
        assertThat(res).extracting(CourseTeacherResponse::getTeacherId).containsExactly(TEACHER_ID);
        assertThat(res.get(0).getFullName()).isEqualTo("U5");
    }

    @Test
    void getTeachers_teacherNotAssigned_rejected() {
        doThrow(new AppException(ErrorCode.FORBIDDEN_COURSE_ACCESS))
                .when(courseAccessGuard).requireAssignedOrAdmin(course);

        assertRejected(() -> service.getTeachers(COURSE_ID), ErrorCode.FORBIDDEN_COURSE_ACCESS);
        verify(courseTeacherRepository, never()).findByCourseIdWithTeacher(any());
    }
}
