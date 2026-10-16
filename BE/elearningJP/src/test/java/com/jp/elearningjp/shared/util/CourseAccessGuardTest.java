package com.jp.elearningjp.shared.util;

import com.jp.elearningjp.entity.curriculum.Course;
import com.jp.elearningjp.entity.user.User;
import com.jp.elearningjp.exception.AppException;
import com.jp.elearningjp.exception.ErrorCode;
import com.jp.elearningjp.repository.curriculum.CourseEnrollmentRepository;
import com.jp.elearningjp.shared.enums.EnrollmentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test cho {@link CourseAccessGuard}.
 *
 * <p>Test thuần bằng Mockito — không cần Spring context, chạy rất nhanh.</p>
 */
@ExtendWith(MockitoExtension.class)
class CourseAccessGuardTest {

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private CourseEnrollmentRepository enrollmentRepository;

    @InjectMocks
    private CourseAccessGuard courseAccessGuard;

    private Course course;
    private User owner;
    private User otherTeacher;
    private User student;

    @BeforeEach
    void setUp() {
        // id là field ở BaseEntity (parent) — Lombok @Builder không tự generate method
        // cho field kế thừa, nên dùng ReflectionTestUtils để set.
        owner = User.builder().email("owner@test.com").build();
        ReflectionTestUtils.setField(owner, "id", 1L);

        otherTeacher = User.builder().email("other@test.com").build();
        ReflectionTestUtils.setField(otherTeacher, "id", 2L);

        student = User.builder().email("student@test.com").build();
        ReflectionTestUtils.setField(student, "id", 3L);

        course = Course.builder()
                .title("Test Course")
                .createdBy(owner)
                .build();
        ReflectionTestUtils.setField(course, "id", 100L);
    }

    @Nested
    @DisplayName("requireEnrolled")
    class RequireEnrolled {

        @Test
        @DisplayName("ADMIN → pass, không cần check enrollment")
        void admin_bypasses() {
            when(currentUserService.isAdmin()).thenReturn(true);

            assertDoesNotThrow(() -> courseAccessGuard.requireEnrolled(100L));

            // Quan trọng: không được gọi enrollmentRepository khi là admin
            verify(enrollmentRepository, never())
                    .existsByUser_IdAndCourse_IdAndStatusAndDeletedFalse(
                            org.mockito.ArgumentMatchers.anyLong(),
                            org.mockito.ArgumentMatchers.anyLong(),
                            org.mockito.ArgumentMatchers.any());
        }

        @Test
        @DisplayName("User đã enroll ACTIVE → pass")
        void enrolledUser_passes() {
            when(currentUserService.isAdmin()).thenReturn(false);
            when(currentUserService.getCurrentUserId()).thenReturn(student.getId());
            when(enrollmentRepository.existsByUser_IdAndCourse_IdAndStatusAndDeletedFalse(
                    student.getId(), 100L, EnrollmentStatus.ACTIVE))
                    .thenReturn(true);

            assertDoesNotThrow(() -> courseAccessGuard.requireEnrolled(100L));
        }

        @Test
        @DisplayName("User chưa enroll → throw COURSE_NOT_ENROLLED")
        void notEnrolled_throws() {
            when(currentUserService.isAdmin()).thenReturn(false);
            when(currentUserService.getCurrentUserId()).thenReturn(student.getId());
            when(enrollmentRepository.existsByUser_IdAndCourse_IdAndStatusAndDeletedFalse(
                    student.getId(), 100L, EnrollmentStatus.ACTIVE))
                    .thenReturn(false);

            AppException ex = assertThrows(AppException.class,
                    () -> courseAccessGuard.requireEnrolled(100L));
            assertEquals(ErrorCode.COURSE_NOT_ENROLLED, ex.getErrorCode());
        }
    }

    @Nested
    @DisplayName("requireOwnerOrAdmin")
    class RequireOwnerOrAdmin {

        @Test
        @DisplayName("ADMIN → pass, không cần check ownership")
        void admin_bypasses() {
            when(currentUserService.isAdmin()).thenReturn(true);

            assertDoesNotThrow(() -> courseAccessGuard.requireOwnerOrAdmin(course));
        }

        @Test
        @DisplayName("Teacher là người tạo course → pass")
        void owner_passes() {
            when(currentUserService.isAdmin()).thenReturn(false);
            when(currentUserService.getCurrentUser()).thenReturn(owner);

            assertDoesNotThrow(() -> courseAccessGuard.requireOwnerOrAdmin(course));
        }

        @Test
        @DisplayName("Teacher khác (không phải owner) → throw FORBIDDEN_COURSE_ACCESS")
        void otherTeacher_throws() {
            when(currentUserService.isAdmin()).thenReturn(false);
            when(currentUserService.getCurrentUser()).thenReturn(otherTeacher);

            AppException ex = assertThrows(AppException.class,
                    () -> courseAccessGuard.requireOwnerOrAdmin(course));
            assertEquals(ErrorCode.FORBIDDEN_COURSE_ACCESS, ex.getErrorCode());
        }

        @Test
        @DisplayName("Teacher là owner nhưng course.createdBy = null → throw (an toàn)")
        void orphanCourse_throws() {
            when(currentUserService.isAdmin()).thenReturn(false);
            when(currentUserService.getCurrentUser()).thenReturn(owner);
            course.setCreatedBy(null);

            AppException ex = assertThrows(AppException.class,
                    () -> courseAccessGuard.requireOwnerOrAdmin(course));
            assertEquals(ErrorCode.FORBIDDEN_COURSE_ACCESS, ex.getErrorCode());
        }

        @Test
        @DisplayName("Overload với custom ErrorCode (vd: FORBIDDEN_LESSON_ACCESS) → dùng error đó")
        void customErrorCode() {
            when(currentUserService.isAdmin()).thenReturn(false);
            when(currentUserService.getCurrentUser()).thenReturn(otherTeacher);

            AppException ex = assertThrows(AppException.class,
                    () -> courseAccessGuard.requireOwnerOrAdmin(course, ErrorCode.FORBIDDEN_LESSON_ACCESS));
            assertEquals(ErrorCode.FORBIDDEN_LESSON_ACCESS, ex.getErrorCode());
        }
    }
}
