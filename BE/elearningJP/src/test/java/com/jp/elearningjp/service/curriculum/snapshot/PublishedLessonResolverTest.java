package com.jp.elearningjp.service.curriculum.snapshot;

import com.jp.elearningjp.entity.curriculum.Course;
import com.jp.elearningjp.entity.curriculum.Lesson;
import com.jp.elearningjp.entity.user.User;
import com.jp.elearningjp.exception.AppException;
import com.jp.elearningjp.exception.ErrorCode;
import com.jp.elearningjp.repository.curriculum.LessonRepository;
import com.jp.elearningjp.shared.util.CourseAccessGuard;
import com.jp.elearningjp.shared.util.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Ai đọc bản xuất bản, ai đọc bản đang soạn. */
@ExtendWith(MockitoExtension.class)
class PublishedLessonResolverTest {

    @Mock LessonRepository lessonRepository;
    @Mock CourseAccessGuard courseAccessGuard;
    @Mock CurrentUserService currentUserService;
    @Mock CourseSnapshotReader snapshotReader;
    @InjectMocks PublishedLessonResolver resolver;

    Course course;
    Lesson lesson;
    User student;

    @BeforeEach
    void setUp() {
        course = Course.builder().title("N5").slug("n5").build();
        course.setId(1L);
        course.setPublishedVersionId(100L);
        lesson = Lesson.builder().course(course).title("Bài 1").orderIndex(1).build();
        lesson.setId(10L);
        student = User.builder().email("s@test.local").build();
        student.setId(7L);
        lenient().when(lessonRepository.findById(10L)).thenReturn(Optional.of(lesson));
        lenient().when(currentUserService.getCurrentUserOrNull()).thenReturn(student);
    }

    @Test
    void student_readsSnapshot_afterAccessCheck() {
        LessonSnapshot snapshot = LessonSnapshot.builder().build();
        when(snapshotReader.findLesson(100L, 10L)).thenReturn(Optional.of(snapshot));

        Optional<PublishedLessonResolver.PublishedLesson> result = resolver.forReading(10L);

        assertThat(result).isPresent();
        assertThat(result.get().snapshot()).isSameAs(snapshot);
        assertThat(result.get().lesson()).isSameAs(lesson);
        verify(courseAccessGuard).requireContentAccess(course);
    }

    @Test
    void learning_requiresEnrollment() {
        when(snapshotReader.findLesson(100L, 10L)).thenReturn(Optional.of(LessonSnapshot.builder().build()));

        resolver.forLearning(10L);

        verify(courseAccessGuard).requireEnrolled(1L);
        verify(courseAccessGuard, never()).requireContentAccess(any());
    }

    @Test
    void lessonDeletedInDraftButStillPublished_studentCanStillOpen() {
        lesson.softDelete();
        when(snapshotReader.findLesson(100L, 10L)).thenReturn(Optional.of(LessonSnapshot.builder().build()));

        assertThat(resolver.forReading(10L)).isPresent();
    }

    @Test
    void lessonNotInPublishedVersion_notFound() {
        when(snapshotReader.findLesson(100L, 10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> resolver.forReading(10L))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.LESSON_NOT_FOUND);
    }

    @Test
    void notEnrolled_rejectedBeforeReadingSnapshot() {
        doThrow(new AppException(ErrorCode.COURSE_NOT_ENROLLED)).when(courseAccessGuard).requireContentAccess(course);

        assertThatThrownBy(() -> resolver.forReading(10L)).isInstanceOf(AppException.class);
        verify(snapshotReader, never()).findLesson(anyLong(), anyLong());
    }

    @Test
    void admin_andAssignedTeacher_useDraft() {
        when(currentUserService.isAdmin()).thenReturn(true);
        assertThat(resolver.forReading(10L)).isEmpty();

        when(currentUserService.isAdmin()).thenReturn(false);
        when(courseAccessGuard.isAssignedTeacher(course, 7L)).thenReturn(true);
        assertThat(resolver.forLearning(10L)).isEmpty();
        verify(snapshotReader, never()).findLesson(anyLong(), anyLong());
    }

    @Test
    void courseWithoutSnapshot_orDeleted_useDraft() {
        course.setPublishedVersionId(null);
        assertThat(resolver.forReading(10L)).isEmpty();

        course.setPublishedVersionId(100L);
        course.softDelete();
        assertThat(resolver.forReading(10L)).isEmpty();
        verify(courseAccessGuard, never()).requireContentAccess(any());
    }

    @Test
    void publicRead_noAccessCheck_emptyWhenNotPublished() {
        when(snapshotReader.findLesson(100L, 10L)).thenReturn(Optional.empty());

        assertThat(resolver.forPublicRead(10L)).isEmpty();
        verify(courseAccessGuard, never()).requireContentAccess(any());
    }
}
