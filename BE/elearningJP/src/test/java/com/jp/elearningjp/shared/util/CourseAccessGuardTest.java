package com.jp.elearningjp.shared.util;

import com.jp.elearningjp.entity.curriculum.Course;
import com.jp.elearningjp.entity.user.User;
import com.jp.elearningjp.exception.AppException;
import com.jp.elearningjp.exception.ErrorCode;
import com.jp.elearningjp.repository.curriculum.CourseEnrollmentRepository;
import com.jp.elearningjp.repository.curriculum.CourseRepository;
import com.jp.elearningjp.repository.curriculum.CourseTeacherRepository;
import com.jp.elearningjp.shared.enums.CourseReviewStatus;
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

    @Mock
    private CourseTeacherRepository courseTeacherRepository;

    @Mock
    private CourseRepository courseRepository;

    @InjectMocks
    private CourseAccessGuard courseAccessGuard;

    private Course course;
    /** Giáo viên được giao khóa (bảng course_teachers). */
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
                    .existsByUserIdAndCourseIdAndDeletedFalse(
                            org.mockito.ArgumentMatchers.anyLong(),
                            org.mockito.ArgumentMatchers.anyLong());
        }

        @Test
        @DisplayName("User đã enroll (mọi trạng thái) → pass")
        void enrolledUser_passes() {
            when(currentUserService.isAdmin()).thenReturn(false);
            when(currentUserService.getCurrentUserId()).thenReturn(student.getId());
            when(enrollmentRepository.existsByUserIdAndCourseIdAndDeletedFalse(student.getId(), 100L))
                    .thenReturn(true);

            assertDoesNotThrow(() -> courseAccessGuard.requireEnrolled(100L));
        }

        @Test
        @DisplayName("User chưa enroll → throw COURSE_NOT_ENROLLED")
        void notEnrolled_throws() {
            when(currentUserService.isAdmin()).thenReturn(false);
            when(currentUserService.getCurrentUserId()).thenReturn(student.getId());
            when(enrollmentRepository.existsByUserIdAndCourseIdAndDeletedFalse(student.getId(), 100L))
                    .thenReturn(false);

            AppException ex = assertThrows(AppException.class,
                    () -> courseAccessGuard.requireEnrolled(100L));
            assertEquals(ErrorCode.COURSE_NOT_ENROLLED, ex.getErrorCode());
        }
    }

    @Nested
    @DisplayName("requireAssignedOrAdmin")
    class RequireAssignedOrAdmin {

        @Test
        @DisplayName("ADMIN → pass, không cần check phân công")
        void admin_bypasses() {
            when(currentUserService.isAdmin()).thenReturn(true);

            assertDoesNotThrow(() -> courseAccessGuard.requireAssignedOrAdmin(course));
            verify(courseTeacherRepository, never()).existsByCourseIdAndTeacherId(
                    org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyLong());
        }

        @Test
        @DisplayName("Teacher được giao course → pass")
        void assignedTeacher_passes() {
            when(currentUserService.isAdmin()).thenReturn(false);
            when(currentUserService.getCurrentUserId()).thenReturn(owner.getId());
            when(courseTeacherRepository.existsByCourseIdAndTeacherId(100L, owner.getId())).thenReturn(true);

            assertDoesNotThrow(() -> courseAccessGuard.requireAssignedOrAdmin(course));
        }

        @Test
        @DisplayName("Teacher không được giao → throw FORBIDDEN_COURSE_ACCESS")
        void otherTeacher_throws() {
            when(currentUserService.isAdmin()).thenReturn(false);
            when(currentUserService.getCurrentUserId()).thenReturn(otherTeacher.getId());
            when(courseTeacherRepository.existsByCourseIdAndTeacherId(100L, otherTeacher.getId())).thenReturn(false);

            AppException ex = assertThrows(AppException.class,
                    () -> courseAccessGuard.requireAssignedOrAdmin(course));
            assertEquals(ErrorCode.FORBIDDEN_COURSE_ACCESS, ex.getErrorCode());
        }

        @Test
        @DisplayName("Người tạo course (createdBy) nhưng không được giao → throw (không còn dựa vào createdBy)")
        void creatorWithoutAssignment_throws() {
            course.setCreatedBy(otherTeacher);
            when(currentUserService.isAdmin()).thenReturn(false);
            when(currentUserService.getCurrentUserId()).thenReturn(otherTeacher.getId());
            when(courseTeacherRepository.existsByCourseIdAndTeacherId(100L, otherTeacher.getId())).thenReturn(false);

            AppException ex = assertThrows(AppException.class,
                    () -> courseAccessGuard.requireAssignedOrAdmin(course));
            assertEquals(ErrorCode.FORBIDDEN_COURSE_ACCESS, ex.getErrorCode());
        }

        @Test
        @DisplayName("Overload với custom ErrorCode (vd: FORBIDDEN_LESSON_ACCESS) → dùng error đó")
        void customErrorCode() {
            when(currentUserService.isAdmin()).thenReturn(false);
            when(currentUserService.getCurrentUserId()).thenReturn(otherTeacher.getId());

            AppException ex = assertThrows(AppException.class,
                    () -> courseAccessGuard.requireAssignedOrAdmin(course, ErrorCode.FORBIDDEN_LESSON_ACCESS));
            assertEquals(ErrorCode.FORBIDDEN_LESSON_ACCESS, ex.getErrorCode());
        }
    }

    @Nested
    @DisplayName("requireContentEditable (ghi nội dung, khóa khi chờ duyệt)")
    class RequireContentEditable {

        @Test
        @DisplayName("Giáo viên được giao, khóa đang soạn → pass và đánh dấu có thay đổi chưa duyệt")
        void assignedTeacher_editing_marksChanges() {
            course.setReviewStatus(CourseReviewStatus.EDITING);
            course.setHasUnpublishedChanges(false);
            when(currentUserService.isAdmin()).thenReturn(false);
            when(currentUserService.getCurrentUserId()).thenReturn(owner.getId());
            when(courseTeacherRepository.existsByCourseIdAndTeacherId(100L, owner.getId())).thenReturn(true);

            assertDoesNotThrow(() -> courseAccessGuard.requireContentEditable(course));
            verify(courseRepository).markUnpublishedChanges(100L);
        }

        @Test
        @DisplayName("Đã có thay đổi chưa duyệt → không ghi lại DB")
        void alreadyMarked_skipsUpdate() {
            course.setHasUnpublishedChanges(true);
            when(currentUserService.isAdmin()).thenReturn(false);
            when(currentUserService.getCurrentUserId()).thenReturn(owner.getId());
            when(courseTeacherRepository.existsByCourseIdAndTeacherId(100L, owner.getId())).thenReturn(true);

            assertDoesNotThrow(() -> courseAccessGuard.requireContentEditable(course));
            verify(courseRepository, never()).markUnpublishedChanges(100L);
        }

        @Test
        @DisplayName("Khóa đang chờ duyệt → giáo viên bị khóa sửa (COURSE_PENDING_REVIEW_LOCKED)")
        void pendingReview_teacherLocked() {
            course.setReviewStatus(CourseReviewStatus.PENDING_REVIEW);
            when(currentUserService.isAdmin()).thenReturn(false);
            when(currentUserService.getCurrentUserId()).thenReturn(owner.getId());
            when(courseTeacherRepository.existsByCourseIdAndTeacherId(100L, owner.getId())).thenReturn(true);

            AppException ex = assertThrows(AppException.class,
                    () -> courseAccessGuard.requireContentEditable(course));
            assertEquals(ErrorCode.COURSE_PENDING_REVIEW_LOCKED, ex.getErrorCode());
            verify(courseRepository, never()).markUnpublishedChanges(100L);
        }

        @Test
        @DisplayName("ADMIN chỉ duyệt → không sửa nội dung (COURSE_CONTENT_ADMIN_READ_ONLY), kể cả khi đang soạn")
        void admin_readOnly() {
            course.setReviewStatus(CourseReviewStatus.EDITING);
            when(currentUserService.isAdmin()).thenReturn(true);

            AppException ex = assertThrows(AppException.class,
                    () -> courseAccessGuard.requireContentEditable(course));
            assertEquals(ErrorCode.COURSE_CONTENT_ADMIN_READ_ONLY, ex.getErrorCode());
            verify(courseRepository, never()).markUnpublishedChanges(100L);
        }

        @Test
        @DisplayName("Giáo viên không được giao → FORBIDDEN, không đánh dấu thay đổi")
        void notAssigned_forbidden() {
            when(currentUserService.isAdmin()).thenReturn(false);
            when(currentUserService.getCurrentUserId()).thenReturn(otherTeacher.getId());

            AppException ex = assertThrows(AppException.class,
                    () -> courseAccessGuard.requireContentEditable(course));
            assertEquals(ErrorCode.FORBIDDEN_COURSE_ACCESS, ex.getErrorCode());
            verify(courseRepository, never()).markUnpublishedChanges(100L);
        }
    }

    @Nested
    @DisplayName("requireContentAccess / canViewUnpublished (xem nội dung không cần đăng ký)")
    class RequireContentAccess {

        @Test
        @DisplayName("ADMIN → xem mọi khóa, không cần đăng ký")
        void admin_canPreviewAnyCourse() {
            when(currentUserService.isAdmin()).thenReturn(true);

            assertDoesNotThrow(() -> courseAccessGuard.requireContentAccess(course));
            verify(enrollmentRepository, never())
                    .existsByUserIdAndCourseIdAndDeletedFalse(
                            org.mockito.ArgumentMatchers.anyLong(),
                            org.mockito.ArgumentMatchers.anyLong());
        }

        @Test
        @DisplayName("Giáo viên được giao → xem khóa, không cần đăng ký")
        void assignedTeacher_canPreviewCourse() {
            when(currentUserService.isAdmin()).thenReturn(false);
            when(currentUserService.getCurrentUserId()).thenReturn(owner.getId());
            when(courseTeacherRepository.existsByCourseIdAndTeacherId(100L, owner.getId())).thenReturn(true);

            assertDoesNotThrow(() -> courseAccessGuard.requireContentAccess(course));
        }

        @Test
        @DisplayName("Giáo viên KHÁC chưa đăng ký → throw COURSE_NOT_ENROLLED")
        void otherTeacher_notEnrolled_throws() {
            when(currentUserService.isAdmin()).thenReturn(false);
            when(currentUserService.getCurrentUserId()).thenReturn(otherTeacher.getId());
            when(enrollmentRepository.existsByUserIdAndCourseIdAndDeletedFalse(otherTeacher.getId(), 100L))
                    .thenReturn(false);

            AppException ex = assertThrows(AppException.class,
                    () -> courseAccessGuard.requireContentAccess(course));
            assertEquals(ErrorCode.COURSE_NOT_ENROLLED, ex.getErrorCode());
        }

        @Test
        @DisplayName("Học viên đã đăng ký → pass")
        void studentEnrolled_passes() {
            when(currentUserService.isAdmin()).thenReturn(false);
            when(currentUserService.getCurrentUserId()).thenReturn(student.getId());
            when(enrollmentRepository.existsByUserIdAndCourseIdAndDeletedFalse(student.getId(), 100L))
                    .thenReturn(true);

            assertDoesNotThrow(() -> courseAccessGuard.requireContentAccess(course));
        }

        @Test
        @DisplayName("Khóa chưa xuất bản: giáo viên được giao thấy, giáo viên khác (chưa đăng ký) và khách thì không")
        void canViewUnpublished_onlyAssignedAdminOrEnrolled() {
            when(currentUserService.isAdmin()).thenReturn(false);
            when(courseTeacherRepository.existsByCourseIdAndTeacherId(100L, owner.getId())).thenReturn(true);
            when(enrollmentRepository.existsByUserIdAndCourseIdAndDeletedFalse(otherTeacher.getId(), 100L))
                    .thenReturn(false);

            assertEquals(true, courseAccessGuard.canViewUnpublished(course, owner.getId()));
            assertEquals(false, courseAccessGuard.canViewUnpublished(course, otherTeacher.getId()));
            assertEquals(false, courseAccessGuard.canViewUnpublished(course, null));
        }
    }
}
