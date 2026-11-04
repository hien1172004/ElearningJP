package com.jp.elearningjp.service.curriculum;

import com.jp.elearningjp.dto.response.curriculum.CourseStudentsResponse;
import com.jp.elearningjp.entity.curriculum.Course;
import com.jp.elearningjp.entity.curriculum.CourseEnrollment;
import com.jp.elearningjp.entity.curriculum.Lesson;
import com.jp.elearningjp.entity.curriculum.UserLessonProgress;
import com.jp.elearningjp.entity.user.User;
import com.jp.elearningjp.exception.AppException;
import com.jp.elearningjp.exception.ErrorCode;
import com.jp.elearningjp.repository.curriculum.CourseEnrollmentRepository;
import com.jp.elearningjp.repository.curriculum.CourseRepository;
import com.jp.elearningjp.repository.curriculum.LessonRepository;
import com.jp.elearningjp.repository.curriculum.UserLessonProgressRepository;
import com.jp.elearningjp.shared.enums.ProgressStatus;
import com.jp.elearningjp.shared.util.CourseAccessGuard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CourseStudentServiceTest {

    private CourseEnrollmentRepository enrollmentRepository;
    private LessonRepository lessonRepository;
    private UserLessonProgressRepository progressRepository;
    private CourseAccessGuard guard;
    private CourseStudentService service;
    private Course course;

    @BeforeEach
    void setUp() {
        CourseRepository courseRepository = mock(CourseRepository.class);
        lessonRepository = mock(LessonRepository.class);
        enrollmentRepository = mock(CourseEnrollmentRepository.class);
        progressRepository = mock(UserLessonProgressRepository.class);
        guard = mock(CourseAccessGuard.class);
        service = new CourseStudentService(courseRepository, lessonRepository, enrollmentRepository,
                progressRepository, guard);

        course = new Course();
        course.setId(10L);
        course.setTitle("N5");
        when(courseRepository.findByIdAndDeletedFalse(10L)).thenReturn(Optional.of(course));
        LessonRepository.CourseLessonCount count = new LessonRepository.CourseLessonCount() {
            public Long getCourseId() { return 10L; }
            public Long getLessonCount() { return 4L; }
        };
        when(lessonRepository.countActiveLessonsByCourseIds(List.of(10L))).thenReturn(List.of(count));
    }

    @Test
    void notCourseOwner_forbidden() {
        doThrow(new AppException(ErrorCode.FORBIDDEN_COURSE_ACCESS)).when(guard).requireOwnerOrAdmin(course);

        assertThatThrownBy(() -> service.getStudents(10L, null, null, 1, 20))
                .extracting(e -> ((AppException) e).getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN_COURSE_ACCESS);
        verifyNoInteractions(enrollmentRepository);
    }

    @Test
    void getStudents_mapsRows_computesPercent_andNormalizesParams() {
        Instant now = Instant.now();
        Object[] row = {5L, "An", "an@t.local", null, "ACTIVE", now, 3L, 1L, now, new BigDecimal("87.50")};
        when(enrollmentRepository.findStudentProgress(eq(10L), anyString(), anyString(), any(Pageable.class)))
                .thenAnswer(inv -> new PageImpl<>(List.<Object[]>of(row), inv.getArgument(3), 1));
        when(enrollmentRepository.summarizeCourseStudents(eq(10L), eq(4), any()))
                .thenReturn(List.<Object[]>of(new Object[]{2L, 1L, 0L, 2L, new BigDecimal("2.0")}));

        CourseStudentsResponse res = service.getStudents(10L, "  AN ", "bogus", 0, 999);

        ArgumentCaptor<String> keyword = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> sort = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(enrollmentRepository).findStudentProgress(eq(10L), keyword.capture(), sort.capture(), pageable.capture());
        assertThat(keyword.getValue()).isEqualTo("%an%");
        assertThat(sort.getValue()).isEqualTo("LAST_ACTIVE");
        assertThat(pageable.getValue().getPageSize()).isEqualTo(CourseStudentService.MAX_PAGE_SIZE);

        CourseStudentsResponse.StudentProgress s = res.getStudents().getItems().get(0);
        assertThat(s.getProgressPercent()).isEqualTo(75);
        assertThat(s.getAvgQuizScore()).isEqualTo(87.5);
        assertThat(res.getTotalLessons()).isEqualTo(4);
        assertThat(res.getSummary().getAvgProgressPercent()).isEqualTo(50.0);
        assertThat(res.getSummary().getTotalStudents()).isEqualTo(2);
    }

    @Test
    void studentDetail_notEnrolled_rejected() {
        when(enrollmentRepository.findByUserIdAndCourseIdAndDeletedFalse(5L, 10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getStudentDetail(10L, 5L))
                .extracting(e -> ((AppException) e).getErrorCode()).isEqualTo(ErrorCode.NOT_ENROLLED);
    }

    @Test
    void studentDetail_listsEveryLesson_includingNotStarted() {
        User student = User.builder().email("an@t.local").fullName("An").build();
        student.setId(5L);
        CourseEnrollment enrollment = CourseEnrollment.builder().user(student).course(course).build();
        when(enrollmentRepository.findByUserIdAndCourseIdAndDeletedFalse(5L, 10L)).thenReturn(Optional.of(enrollment));

        Lesson l1 = Lesson.builder().title("Bài 1").orderIndex(1).course(course).build();
        l1.setId(1L);
        Lesson l2 = Lesson.builder().title("Bài 2").orderIndex(2).course(course).build();
        l2.setId(2L);
        when(lessonRepository.findByCourseIdAndDeletedFalseOrderByOrderIndexAsc(10L)).thenReturn(List.of(l1, l2));

        UserLessonProgress p1 = UserLessonProgress.builder().user(student).lesson(l1)
                .status(ProgressStatus.COMPLETED).bestScore(new BigDecimal("90")).lastAccessedAt(Instant.now()).build();
        when(progressRepository.findAllByUserIdAndCourseId(5L, 10L)).thenReturn(List.of(p1));

        CourseStudentsResponse.StudentDetail detail = service.getStudentDetail(10L, 5L);

        assertThat(detail.getLessons()).extracting(CourseStudentsResponse.LessonProgress::getStatus)
                .containsExactly(ProgressStatus.COMPLETED, ProgressStatus.NOT_STARTED);
        assertThat(detail.getStudent().getProgressPercent()).isEqualTo(50);
        assertThat(detail.getStudent().getAvgQuizScore()).isEqualTo(90.0);
    }

    @Test
    void percent_handlesZeroLessons_andCapsAt100() {
        assertThat(CourseStudentService.percent(3, 0)).isZero();
        assertThat(CourseStudentService.percent(5, 4)).isEqualTo(100);
        assertThat(CourseStudentService.percent(1, 3)).isEqualTo(33);
    }
}
