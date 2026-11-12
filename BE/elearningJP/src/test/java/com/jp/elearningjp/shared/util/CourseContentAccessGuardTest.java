package com.jp.elearningjp.shared.util;

import com.jp.elearningjp.entity.curriculum.Course;
import com.jp.elearningjp.entity.curriculum.Lesson;
import com.jp.elearningjp.entity.curriculum.LessonSection;
import com.jp.elearningjp.entity.user.User;
import com.jp.elearningjp.exception.AppException;
import com.jp.elearningjp.exception.ErrorCode;
import com.jp.elearningjp.repository.curriculum.CourseEnrollmentRepository;
import com.jp.elearningjp.repository.curriculum.CourseTeacherRepository;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Quy tắc bản quyền: chỉ admin, giáo viên được giao và người đã đăng ký được xem nội dung. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CourseContentAccessGuardTest {

    private static final Long COURSE_ID = 10L;
    private static final Long OWNER_ID = 1L;
    private static final Long VIEWER_ID = 2L;

    @Mock CurrentUserService currentUserService;
    @Mock CourseEnrollmentRepository enrollmentRepository;
    @Mock CourseTeacherRepository courseTeacherRepository;
    @Mock com.jp.elearningjp.service.curriculum.LessonDraftService lessonDraftService;
    @InjectMocks CourseAccessGuard guard;

    private Course course;
    private Lesson lesson;
    private LessonSection section;

    @BeforeEach
    void setUp() {
        User owner = mock(User.class);
        when(owner.getId()).thenReturn(OWNER_ID);
        course = mock(Course.class);
        when(course.getId()).thenReturn(COURSE_ID);
        when(course.getCreatedBy()).thenReturn(owner);
        // OWNER_ID là giáo viên được giao khóa
        when(courseTeacherRepository.existsByCourseIdAndTeacherId(COURSE_ID, OWNER_ID)).thenReturn(true);
        lesson = mock(Lesson.class);
        when(lesson.getCourse()).thenReturn(course);
        section = mock(LessonSection.class);
        when(section.getLesson()).thenReturn(lesson);
        // V65: section thuộc nháp TEACHER của bài (phía người xem kiểm ở LessonDraftService — mock)
        when(section.getDraft()).thenReturn(com.jp.elearningjp.entity.curriculum.LessonDraft.builder().lesson(lesson)
                .kind(com.jp.elearningjp.shared.enums.DraftKind.TEACHER).build());
        when(currentUserService.getCurrentUserId()).thenReturn(VIEWER_ID);
    }

    private void assertRejected(ThrowingCallable call, ErrorCode expected) {
        assertThatThrownBy(call)
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(expected);
    }

    @Test
    void notEnrolled_rejected() {
        assertRejected(() -> guard.requireSectionContentAccess(section), ErrorCode.COURSE_NOT_ENROLLED);
    }

    @Test
    void enrolled_allowed_includingCompletedEnrollment() {
        // Query không lọc status: enrollment COMPLETED vẫn mở lại được bài.
        when(enrollmentRepository.existsByUserIdAndCourseIdAndDeletedFalse(VIEWER_ID, COURSE_ID)).thenReturn(true);
        assertThatCode(() -> guard.requireSectionContentAccess(section)).doesNotThrowAnyException();
    }

    @Test
    void owner_allowedWithoutEnrollment() {
        when(currentUserService.getCurrentUserId()).thenReturn(OWNER_ID);
        assertThatCode(() -> guard.requireLessonContentAccess(lesson)).doesNotThrowAnyException();
    }

    @Test
    void admin_allowedWithoutEnrollment() {
        when(currentUserService.isAdmin()).thenReturn(true);
        assertThatCode(() -> guard.requireLessonContentAccess(lesson)).doesNotThrowAnyException();
    }

    @Test
    void enrolledInLesson_lessonOfDeletedCourse_rejectedEvenIfStillEnrolled() {
        // Dữ liệu cũ: khóa đã xoá nhưng enrollment chưa bị xoá mềm.
        when(enrollmentRepository.existsByUserIdAndCourseIdAndDeletedFalse(VIEWER_ID, COURSE_ID)).thenReturn(true);
        assertThatCode(() -> guard.requireEnrolledInLesson(lesson)).doesNotThrowAnyException();

        when(course.isDeleted()).thenReturn(true);
        assertRejected(() -> guard.requireEnrolledInLesson(lesson), ErrorCode.COURSE_NOT_FOUND);
    }

    @Test
    void deletedParents_treatedAsNotFound_evenForAdmin() {
        when(currentUserService.isAdmin()).thenReturn(true);

        when(course.isDeleted()).thenReturn(true);
        assertRejected(() -> guard.requireLessonContentAccess(lesson), ErrorCode.COURSE_NOT_FOUND);

        when(lesson.isDeleted()).thenReturn(true);
        assertRejected(() -> guard.requireLessonContentAccess(lesson), ErrorCode.LESSON_NOT_FOUND);

        when(section.isDeleted()).thenReturn(true);
        assertRejected(() -> guard.requireSectionContentAccess(section), ErrorCode.LESSON_SECTION_NOT_FOUND);
    }
}
