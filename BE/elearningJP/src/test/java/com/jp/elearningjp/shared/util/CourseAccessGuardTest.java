package com.jp.elearningjp.shared.util;

import com.jp.elearningjp.entity.curriculum.Course;
import com.jp.elearningjp.entity.curriculum.Lesson;
import com.jp.elearningjp.entity.user.User;
import com.jp.elearningjp.exception.AppException;
import com.jp.elearningjp.exception.ErrorCode;
import com.jp.elearningjp.repository.curriculum.CourseEnrollmentRepository;
import com.jp.elearningjp.repository.curriculum.CourseRepository;
import com.jp.elearningjp.repository.curriculum.CourseTeacherRepository;
import com.jp.elearningjp.repository.curriculum.LessonRepository;
import com.jp.elearningjp.entity.curriculum.LessonDraft;
import com.jp.elearningjp.service.curriculum.CourseEditLockService;
import com.jp.elearningjp.service.curriculum.LessonDraftService;
import com.jp.elearningjp.shared.enums.DraftKind;
import com.jp.elearningjp.shared.enums.CourseReviewStatus;
import com.jp.elearningjp.shared.event.curriculum.CourseContentChangedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
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

    @Mock
    private LessonRepository lessonRepository;

    @Mock
    private CourseEditLockService courseEditLockService;

    @Mock
    private LessonDraftService lessonDraftService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private CourseAccessGuard courseAccessGuard;

    private Course course;
    /** Giáo viên được giao khóa (bảng course_teachers). */
    private User owner;
    private User otherTeacher;
    private User student;

    private static final Instant NOW = Instant.parse("2026-11-11T03:00:00Z");

    @BeforeEach
    void setUp() {
        // Clock không phải mock → @InjectMocks truyền null; gán đồng hồ cố định
        ReflectionTestUtils.setField(courseAccessGuard, "clock", Clock.fixed(NOW, ZoneOffset.UTC));
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

            when(currentUserService.getCurrentUserOrNull()).thenReturn(owner);

            assertDoesNotThrow(() -> courseAccessGuard.requireContentEditable(course));
            // V65: phải giữ khóa sửa khóa học của phía mình
            verify(courseEditLockService).requireHeld(100L);
            // Đánh dấu có thay đổi + người / thời điểm sửa gần nhất (tên rỗng → email)
            verify(courseRepository).markDraftEdited(100L, owner.getId(), "owner@test.com", NOW);

            // Báo realtime cho người đang soạn khóa (DRAFT, kèm người thao tác để client của họ bỏ qua)
            ArgumentCaptor<CourseContentChangedEvent> captor = ArgumentCaptor.forClass(CourseContentChangedEvent.class);
            verify(eventPublisher).publishEvent(captor.capture());
            assertEquals(100L, captor.getValue().getCourseId());
            assertEquals(CourseContentChangedEvent.Scope.DRAFT, captor.getValue().getScope());
            assertEquals(owner.getId(), captor.getValue().getActorId());
            // Thao tác cấu trúc khóa (thêm / xóa / sắp xếp bài) dùng chung 2 phía → không gắn phía
            assertNull(captor.getValue().getDraftKind());
        }

        @Test
        @DisplayName("Sửa nháp phía mình → tín hiệu realtime kèm phía đó (phía kia không bị nhắc tải lại)")
        void ownDraftEdit_taggedWithSide() {
            course.setReviewStatus(CourseReviewStatus.EDITING);
            when(currentUserService.isAdmin()).thenReturn(false);
            when(currentUserService.getCurrentUserId()).thenReturn(owner.getId());
            when(courseTeacherRepository.existsByCourseIdAndTeacherId(100L, owner.getId())).thenReturn(true);
            when(lessonDraftService.currentKind()).thenReturn(DraftKind.TEACHER);

            assertDoesNotThrow(() -> courseAccessGuard.requireOwnDraftEditable(course, ErrorCode.FORBIDDEN_LESSON_ACCESS));

            ArgumentCaptor<CourseContentChangedEvent> captor = ArgumentCaptor.forClass(CourseContentChangedEvent.class);
            verify(eventPublisher).publishEvent(captor.capture());
            assertEquals(CourseContentChangedEvent.Scope.DRAFT, captor.getValue().getScope());
            assertEquals(DraftKind.TEACHER, captor.getValue().getDraftKind());
            verify(courseEditLockService).requireHeld(100L);
        }

        @Test
        @DisplayName("Cùng người vừa sửa (< 1 phút) → không ghi lại DB")
        void sameEditorRecently_skipsUpdate() {
            course.setHasUnpublishedChanges(true);
            course.setDraftUpdatedById(owner.getId());
            course.setDraftUpdatedAt(NOW.minusSeconds(30));
            when(currentUserService.isAdmin()).thenReturn(false);
            when(currentUserService.getCurrentUserId()).thenReturn(owner.getId());
            when(courseTeacherRepository.existsByCourseIdAndTeacherId(100L, owner.getId())).thenReturn(true);

            assertDoesNotThrow(() -> courseAccessGuard.requireContentEditable(course));
            verify(courseRepository, never()).markDraftEdited(anyLong(), any(), any(), any());
        }

        @Test
        @DisplayName("Đổi người sửa hoặc lần ghi trước đã quá 1 phút → ghi lại người / thời điểm sửa")
        void needsDraftTouch_whenEditorChangesOrStale() {
            course.setHasUnpublishedChanges(true);
            course.setDraftUpdatedById(owner.getId());
            course.setDraftUpdatedAt(NOW.minusSeconds(30));
            assertEquals(false, CourseAccessGuard.needsDraftTouch(course, owner.getId(), NOW));
            assertEquals(true, CourseAccessGuard.needsDraftTouch(course, otherTeacher.getId(), NOW));

            course.setDraftUpdatedAt(NOW.minusSeconds(61));
            assertEquals(true, CourseAccessGuard.needsDraftTouch(course, owner.getId(), NOW));

            course.setDraftUpdatedAt(NOW.minusSeconds(10));
            course.setHasUnpublishedChanges(false);
            assertEquals(true, CourseAccessGuard.needsDraftTouch(course, owner.getId(), NOW));
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
            verify(courseRepository, never()).markDraftEdited(anyLong(), any(), any(), any());
            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("ADMIN toàn quyền → sửa được cả khi khóa đang chờ duyệt, vẫn đánh dấu có thay đổi chưa lưu")
        void admin_canEdit_evenWhilePendingReview() {
            course.setReviewStatus(CourseReviewStatus.PENDING_REVIEW);
            course.setHasUnpublishedChanges(false);
            when(currentUserService.isAdmin()).thenReturn(true);
            when(currentUserService.getCurrentUserId()).thenReturn(99L);

            assertDoesNotThrow(() -> courseAccessGuard.requireContentEditable(course));
            verify(courseRepository).markDraftEdited(eq(100L), eq(99L), any(), eq(NOW));
            verify(eventPublisher).publishEvent(any(CourseContentChangedEvent.class));
        }

        @Test
        @DisplayName("Giáo viên không được giao → FORBIDDEN, không đánh dấu thay đổi")
        void notAssigned_forbidden() {
            when(currentUserService.isAdmin()).thenReturn(false);
            when(currentUserService.getCurrentUserId()).thenReturn(otherTeacher.getId());

            AppException ex = assertThrows(AppException.class,
                    () -> courseAccessGuard.requireContentEditable(course));
            assertEquals(ErrorCode.FORBIDDEN_COURSE_ACCESS, ex.getErrorCode());
            verify(courseRepository, never()).markDraftEdited(anyLong(), any(), any(), any());
            verify(eventPublisher, never()).publishEvent(any());
        }
    }

    @Nested
    @DisplayName("requireContentEditable + khóa sửa khóa học (V65)")
    class CourseEditLock {

        @Test
        @DisplayName("Không giữ khóa sửa của phía mình → COURSE_EDIT_SESSION_REQUIRED, không đánh dấu thay đổi")
        void lockNotHeld_rejected() {
            when(currentUserService.isAdmin()).thenReturn(false);
            when(currentUserService.getCurrentUserId()).thenReturn(owner.getId());
            when(courseTeacherRepository.existsByCourseIdAndTeacherId(100L, owner.getId())).thenReturn(true);
            org.mockito.Mockito.doThrow(new AppException(ErrorCode.COURSE_EDIT_SESSION_REQUIRED))
                    .when(courseEditLockService).requireHeld(100L);

            AppException ex = assertThrows(AppException.class, () -> courseAccessGuard.requireContentEditable(course));
            assertEquals(ErrorCode.COURSE_EDIT_SESSION_REQUIRED, ex.getErrorCode());
            verify(courseRepository, never()).markDraftEdited(anyLong(), any(), any(), any());
            verify(eventPublisher, never()).publishEvent(any());
        }
    }

    @Nested
    @DisplayName("requireLessonEditable / requireDraftEditable (ghi vào nháp phía mình — V65)")
    class RequireLessonEditable {

        private Lesson lesson() {
            Lesson lesson = Lesson.builder().course(course).title("Bài 1").orderIndex(1).build();
            ReflectionTestUtils.setField(lesson, "id", 7L);
            return lesson;
        }

        private LessonDraft draft(Lesson lesson, long id, DraftKind kind) {
            LessonDraft d = LessonDraft.builder().lesson(lesson).kind(kind).build();
            ReflectionTestUtils.setField(d, "id", id);
            return d;
        }

        private void asAssignedTeacher() {
            when(currentUserService.isAdmin()).thenReturn(false);
            when(currentUserService.getCurrentUserId()).thenReturn(owner.getId());
            when(courseTeacherRepository.existsByCourseIdAndTeacherId(100L, owner.getId())).thenReturn(true);
        }

        @Test
        @DisplayName("Giữ khóa sửa → trả nháp phía mình để ghi")
        void lockHeld_returnsWritableDraft() {
            Lesson lesson = lesson();
            LessonDraft teacherDraft = draft(lesson, 70L, DraftKind.TEACHER);
            asAssignedTeacher();
            when(lessonDraftService.writableDraft(lesson)).thenReturn(teacherDraft);

            LessonDraft result = courseAccessGuard.requireLessonEditable(lesson, ErrorCode.FORBIDDEN_LESSON_ACCESS);
            assertEquals(teacherDraft, result);
            verify(courseEditLockService).requireHeld(100L);
        }

        @Test
        @DisplayName("Nháp đã gửi duyệt → LESSON_PENDING_REVIEW_LOCKED")
        void submittedDraft_locked() {
            Lesson lesson = lesson();
            asAssignedTeacher();
            when(lessonDraftService.writableDraft(lesson))
                    .thenThrow(new AppException(ErrorCode.LESSON_PENDING_REVIEW_LOCKED));

            AppException ex = assertThrows(AppException.class,
                    () -> courseAccessGuard.requireLessonEditable(lesson, ErrorCode.FORBIDDEN_LESSON_ACCESS));
            assertEquals(ErrorCode.LESSON_PENDING_REVIEW_LOCKED, ex.getErrorCode());
        }

        @Test
        @DisplayName("Giáo viên không được giao → 403 trước, không lấy nháp")
        void notAssigned_rejectedBeforeTouchingDraft() {
            Lesson lesson = lesson();
            when(currentUserService.isAdmin()).thenReturn(false);
            when(currentUserService.getCurrentUserId()).thenReturn(otherTeacher.getId());
            when(courseTeacherRepository.existsByCourseIdAndTeacherId(100L, otherTeacher.getId())).thenReturn(false);

            AppException ex = assertThrows(AppException.class,
                    () -> courseAccessGuard.requireLessonEditable(lesson, ErrorCode.FORBIDDEN_LESSON_ACCESS));
            assertEquals(ErrorCode.FORBIDDEN_LESSON_ACCESS, ex.getErrorCode());
            verify(lessonDraftService, never()).writableDraft(any());
            verify(courseEditLockService, never()).requireHeld(anyLong());
        }

        @Test
        @DisplayName("Ghi phần tử thuộc nháp phía bên kia → coi như không tồn tại, không ghi")
        void otherSideDraft_notFound() {
            Lesson lesson = lesson();
            LessonDraft adminDraft = draft(lesson, 71L, DraftKind.ADMIN);
            org.mockito.Mockito.doThrow(new AppException(ErrorCode.LESSON_SECTION_NOT_FOUND))
                    .when(lessonDraftService).requireReadable(adminDraft, ErrorCode.LESSON_SECTION_NOT_FOUND);

            AppException ex = assertThrows(AppException.class, () -> courseAccessGuard.requireDraftEditable(
                    adminDraft, ErrorCode.FORBIDDEN_LESSON_ACCESS, ErrorCode.LESSON_SECTION_NOT_FOUND));
            assertEquals(ErrorCode.LESSON_SECTION_NOT_FOUND, ex.getErrorCode());
            verify(lessonDraftService, never()).writableDraft(any());
        }

        @Test
        @DisplayName("Phần tử thuộc nháp cũ của phía mình (không phải nháp đang dùng) → không tồn tại")
        void staleOwnDraft_notFound() {
            Lesson lesson = lesson();
            LessonDraft old = draft(lesson, 72L, DraftKind.TEACHER);
            asAssignedTeacher();
            when(lessonDraftService.writableDraft(lesson)).thenReturn(draft(lesson, 73L, DraftKind.TEACHER));

            AppException ex = assertThrows(AppException.class, () -> courseAccessGuard.requireDraftEditable(
                    old, ErrorCode.FORBIDDEN_LESSON_ACCESS, ErrorCode.LESSON_SECTION_NOT_FOUND));
            assertEquals(ErrorCode.LESSON_SECTION_NOT_FOUND, ex.getErrorCode());
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
