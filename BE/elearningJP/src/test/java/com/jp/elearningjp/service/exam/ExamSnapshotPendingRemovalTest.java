package com.jp.elearningjp.service.exam;

import com.jp.elearningjp.entity.curriculum.Course;
import com.jp.elearningjp.entity.exam.Exam;
import com.jp.elearningjp.repository.exam.ExamAttemptRepository;
import com.jp.elearningjp.repository.exam.ExamPublishedVersionRepository;
import com.jp.elearningjp.repository.exam.ExamRepository;
import com.jp.elearningjp.repository.exam.ExamSectionQuestionRepository;
import com.jp.elearningjp.mapper.ExamMapper;
import com.jp.elearningjp.service.curriculum.snapshot.SnapshotJsonCache;
import com.jp.elearningjp.service.exam.impl.ExamViewBuilder;
import com.jp.elearningjp.service.exam.snapshot.ExamSnapshotService;
import com.jp.elearningjp.shared.enums.AttemptStatus;
import com.jp.elearningjp.shared.util.CourseAccessGuard;
import com.jp.elearningjp.shared.util.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Khóa được duyệt: đề giáo viên yêu cầu gỡ bị gỡ khỏi học viên (không chụp lại). */
class ExamSnapshotPendingRemovalTest {

    ExamRepository examRepository = mock(ExamRepository.class);
    ExamPublishedVersionRepository versionRepository = mock(ExamPublishedVersionRepository.class);
    ExamAttemptRepository attemptRepository = mock(ExamAttemptRepository.class);
    ExamSnapshotService service;
    Exam exam;

    @BeforeEach
    void setUp() {
        var mapper = Jackson2ObjectMapperBuilder.json().build();
        service = new ExamSnapshotService(examRepository, mock(ExamSectionQuestionRepository.class), versionRepository,
                attemptRepository, mock(ExamMapper.class), mock(ExamViewBuilder.class), mock(CurrentUserService.class),
                mock(CourseAccessGuard.class), mapper, new SnapshotJsonCache(mapper, 0));
        Course course = new Course();
        course.setId(10L);
        exam = Exam.builder().title("Giữa kỳ").course(course).published(true).publishedVersionId(5L)
                .pendingRemoval(true).build();
        exam.setId(1L);
        when(examRepository.findByCourseIdAndDeletedFalse(10L)).thenReturn(List.of(exam));
        when(examRepository.save(any(Exam.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void noAttemptInProgress_examRemoved_notSnapshotted() {
        when(attemptRepository.countByExamIdAndStatusAndDeletedFalse(1L, AttemptStatus.IN_PROGRESS)).thenReturn(0L);

        service.publishCourseExams(10L, null);

        assertThat(exam.isDeleted()).isTrue();
        assertThat(exam.isPublished()).isFalse();
        assertThat(exam.isPendingRemoval()).isFalse();
        verify(examRepository, never()).findByIdWithSectionsAndScoreGroups(anyLong());
        verify(versionRepository, never()).save(any());
    }

    @Test
    void attemptsInProgress_hiddenButKept_untilNextApproval() {
        when(attemptRepository.countByExamIdAndStatusAndDeletedFalse(1L, AttemptStatus.IN_PROGRESS)).thenReturn(2L);
        when(examRepository.findByIdWithSectionsAndScoreGroups(anyLong())).thenReturn(Optional.empty());

        service.publishCourseExams(10L, null);

        assertThat(exam.isPublished()).isFalse();
        assertThat(exam.isDeleted()).isFalse();
        assertThat(exam.isPendingRemoval()).isTrue();
    }
}
