package com.jp.elearningjp.service.curriculum;

import com.jp.elearningjp.dto.request.curriculum.CourseReviewCommentRequest;
import com.jp.elearningjp.dto.response.curriculum.CourseReviewCommentResponse;
import com.jp.elearningjp.entity.curriculum.Course;
import com.jp.elearningjp.entity.curriculum.CourseReviewComment;
import com.jp.elearningjp.entity.curriculum.CourseReviewRequest;
import com.jp.elearningjp.entity.curriculum.Lesson;
import com.jp.elearningjp.entity.curriculum.LessonSection;
import com.jp.elearningjp.entity.user.User;
import com.jp.elearningjp.exception.AppException;
import com.jp.elearningjp.exception.ErrorCode;
import com.jp.elearningjp.repository.curriculum.CourseRepository;
import com.jp.elearningjp.repository.curriculum.CourseReviewCommentRepository;
import com.jp.elearningjp.repository.curriculum.CourseReviewRequestRepository;
import com.jp.elearningjp.repository.curriculum.LessonRepository;
import com.jp.elearningjp.repository.curriculum.LessonSectionRepository;
import com.jp.elearningjp.repository.user.UserRepository;
import com.jp.elearningjp.shared.enums.ReviewRequestStatus;
import com.jp.elearningjp.shared.util.CourseAccessGuard;
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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Nhận xét duyệt theo bài: ADMIN ghi khi đang chờ duyệt, giáo viên đánh dấu đã sửa. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CourseReviewCommentServiceTest {

    static final Long ADMIN_ID = 1L;
    static final Long TEACHER_ID = 2L;

    @Mock CourseReviewCommentRepository commentRepository;
    @Mock CourseReviewRequestRepository reviewRequestRepository;
    @Mock CourseRepository courseRepository;
    @Mock LessonRepository lessonRepository;
    @Mock LessonSectionRepository sectionRepository;
    @Mock UserRepository userRepository;
    @Mock CourseAccessGuard courseAccessGuard;
    @Mock CurrentUserService currentUserService;
    @InjectMocks CourseReviewCommentService service;

    Course course;
    Course otherCourse;
    CourseReviewRequest request;
    Lesson lesson;

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

    private static User user(Long id) {
        User u = User.builder().email("u" + id + "@test.local").fullName("U" + id).build();
        u.setId(id);
        return u;
    }

    @BeforeEach
    void setUp() {
        course = Course.builder().title("N5").slug("n5").build();
        course.setId(10L);
        otherCourse = Course.builder().title("N4").slug("n4").build();
        otherCourse.setId(11L);
        request = CourseReviewRequest.builder().course(course).status(ReviewRequestStatus.PENDING).build();
        request.setId(100L);
        lesson = Lesson.builder().course(course).title("Bài 1").orderIndex(1).build();
        lesson.setId(20L);
        when(reviewRequestRepository.findById(100L)).thenReturn(Optional.of(request));
        when(lessonRepository.findById(20L)).thenReturn(Optional.of(lesson));
        when(commentRepository.save(any(CourseReviewComment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.getReferenceById(any())).thenAnswer(inv -> user(inv.getArgument(0)));
        when(lessonRepository.findAllById(any())).thenReturn(List.of(lesson));
        when(sectionRepository.findAllById(any())).thenReturn(List.of());
        actAs(ADMIN_ID, true);
    }

    @Test
    void admin_addsLessonComment_whilePending() {
        CourseReviewCommentResponse res = service.add(100L, CourseReviewCommentRequest.builder()
                .lessonId(20L).content("  Bài 1 thiếu ví dụ  ").build());

        assertThat(res.getContent()).isEqualTo("Bài 1 thiếu ví dụ");
        assertThat(res.getLessonId()).isEqualTo(20L);
        assertThat(res.getLessonTitle()).isEqualTo("Bài 1");
        assertThat(res.getAuthorId()).isEqualTo(ADMIN_ID);
        assertThat(res.isResolved()).isFalse();
    }

    @Test
    void add_requestNotPending_rejected() {
        request.setStatus(ReviewRequestStatus.REJECTED);

        assertRejected(() -> service.add(100L, CourseReviewCommentRequest.builder().content("x").build()),
                ErrorCode.COURSE_REVIEW_NOT_PENDING);
        verify(commentRepository, never()).save(any());
    }

    @Test
    void add_lessonOfAnotherCourse_orSectionWithoutLesson_rejected() {
        Lesson foreign = Lesson.builder().course(otherCourse).title("Khác").build();
        foreign.setId(30L);
        when(lessonRepository.findById(30L)).thenReturn(Optional.of(foreign));

        assertRejected(() -> service.add(100L, CourseReviewCommentRequest.builder().lessonId(30L).content("x").build()),
                ErrorCode.COURSE_REVIEW_COMMENT_TARGET_INVALID);
        assertRejected(() -> service.add(100L, CourseReviewCommentRequest.builder().sectionId(5L).content("x").build()),
                ErrorCode.COURSE_REVIEW_COMMENT_TARGET_INVALID);
    }

    @Test
    void add_sectionOfAnotherLesson_rejected() {
        Lesson otherLesson = Lesson.builder().course(course).title("Bài 2").build();
        otherLesson.setId(21L);
        LessonSection section = new LessonSection();
        section.setId(5L);
        section.setLesson(otherLesson);
        when(sectionRepository.findById(5L)).thenReturn(Optional.of(section));

        assertRejected(() -> service.add(100L, CourseReviewCommentRequest.builder()
                        .lessonId(20L).sectionId(5L).content("x").build()),
                ErrorCode.COURSE_REVIEW_COMMENT_TARGET_INVALID);
    }

    @Test
    void add_byTeacher_forbidden() {
        actAs(TEACHER_ID, false);

        assertRejected(() -> service.add(100L, CourseReviewCommentRequest.builder().content("x").build()),
                ErrorCode.FORBIDDEN_COURSE_ACCESS);
    }

    @Test
    void delete_ownCommentWhilePending_only() {
        CourseReviewComment comment = CourseReviewComment.builder()
                .reviewRequest(request).courseId(10L).content("x").author(user(ADMIN_ID)).build();
        comment.setId(7L);
        when(commentRepository.findById(7L)).thenReturn(Optional.of(comment));

        actAs(99L, true); // ADMIN khác
        assertRejected(() -> service.delete(7L), ErrorCode.FORBIDDEN_COURSE_ACCESS);

        actAs(ADMIN_ID, true);
        request.setStatus(ReviewRequestStatus.REJECTED);
        assertRejected(() -> service.delete(7L), ErrorCode.COURSE_REVIEW_NOT_PENDING);

        request.setStatus(ReviewRequestStatus.PENDING);
        service.delete(7L);
        verify(commentRepository).delete(comment);
    }

    @Test
    void teacher_resolvesAndReopens() {
        CourseReviewComment comment = CourseReviewComment.builder()
                .reviewRequest(request).courseId(10L).lessonId(20L).content("x").author(user(ADMIN_ID)).build();
        comment.setId(7L);
        when(commentRepository.findById(7L)).thenReturn(Optional.of(comment));
        request.setStatus(ReviewRequestStatus.REJECTED);
        actAs(TEACHER_ID, false);

        CourseReviewCommentResponse done = service.resolve(7L, true);
        assertThat(done.isResolved()).isTrue();
        assertThat(done.getResolvedByName()).isEqualTo("U2");
        assertThat(comment.getResolvedAt()).isNotNull();

        service.resolve(7L, false);
        assertThat(comment.isResolved()).isFalse();
        assertThat(comment.getResolvedBy()).isNull();
        verify(courseAccessGuard, org.mockito.Mockito.times(2))
                .requireAssignedTeacher(course, ErrorCode.FORBIDDEN_COURSE_ACCESS);
    }

    @Test
    void resolve_notAssigned_rejected() {
        CourseReviewComment comment = CourseReviewComment.builder()
                .reviewRequest(request).courseId(10L).content("x").build();
        comment.setId(7L);
        when(commentRepository.findById(7L)).thenReturn(Optional.of(comment));
        doThrow(new AppException(ErrorCode.FORBIDDEN_COURSE_ACCESS))
                .when(courseAccessGuard).requireAssignedTeacher(course, ErrorCode.FORBIDDEN_COURSE_ACCESS);

        assertRejected(() -> service.resolve(7L, true), ErrorCode.FORBIDDEN_COURSE_ACCESS);
        assertThat(comment.isResolved()).isFalse();
    }

    @Test
    void latestForCourse_usesMostRecentRequest_andChecksAccess() {
        when(courseRepository.findByIdAndDeletedFalse(10L)).thenReturn(Optional.of(course));
        when(reviewRequestRepository.findFirstByCourseIdOrderByCreatedAtDescIdDesc(10L)).thenReturn(Optional.of(request));
        CourseReviewComment comment = CourseReviewComment.builder()
                .reviewRequest(request).courseId(10L).lessonId(20L).content("x").build();
        when(commentRepository.findByReviewRequestIdOrderByCreatedAtAsc(100L)).thenReturn(List.of(comment));

        assertThat(service.getLatestForCourse(10L)).singleElement()
                .extracting(CourseReviewCommentResponse::getReviewStatus).isEqualTo(ReviewRequestStatus.PENDING);
        verify(courseAccessGuard).requireAssignedOrAdmin(course);
    }
}
