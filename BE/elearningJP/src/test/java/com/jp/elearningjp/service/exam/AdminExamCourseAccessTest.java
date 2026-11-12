package com.jp.elearningjp.service.exam;

import com.jp.elearningjp.service.exam.snapshot.ExamSnapshotService;
import com.jp.elearningjp.dto.request.exam.ImportExamRequest;
import com.jp.elearningjp.entity.curriculum.Course;
import com.jp.elearningjp.entity.exam.Exam;
import com.jp.elearningjp.exception.AppException;
import com.jp.elearningjp.exception.ErrorCode;
import com.jp.elearningjp.mapper.ExamMapper;
import com.jp.elearningjp.repository.content.ReadingPassageRepository;
import com.jp.elearningjp.repository.curriculum.CourseRepository;
import com.jp.elearningjp.repository.exam.*;
import com.jp.elearningjp.repository.user.UserRepository;
import com.jp.elearningjp.service.exam.impl.AdminExamServiceImpl;
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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Quyền quản lý đề thi gắn với khóa học: chỉ ADMIN hoặc giáo viên được giao khóa;
 * xuất bản / thu hồi đề gắn với khóa chỉ ADMIN. Đề độc lập giữ nguyên quyền cũ.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AdminExamCourseAccessTest {

    private static final Long COURSE_EXAM_ID = 1L;
    private static final Long STANDALONE_EXAM_ID = 2L;
    private static final Long COURSE_ID = 10L;

    @Mock ExamRepository examRepository;
    @Mock ExamSectionRepository examSectionRepository;
    @Mock ExamSectionQuestionRepository examSectionQuestionRepository;
    @Mock QuestionRepository questionRepository;
    @Mock MondaiTypeRepository mondaiTypeRepository;
    @Mock ReadingPassageRepository readingPassageRepository;
    @Mock UserRepository userRepository;
    @Mock CourseRepository courseRepository;
    @Mock ExamAttemptRepository examAttemptRepository;
    @Mock StudentAnswerRepository studentAnswerRepository;
    @Mock ExamMapper examMapper;
    @Mock CourseAccessGuard courseAccessGuard;
    @Mock CurrentUserService currentUserService;

    @Mock ExamSnapshotService examSnapshotService;
    @InjectMocks AdminExamServiceImpl service;

    private Course course;
    private Exam courseExam;
    private Exam standaloneExam;

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
        courseExam = Exam.builder().title("Đề của khóa").course(course).build();
        courseExam.setId(COURSE_EXAM_ID);
        standaloneExam = Exam.builder().title("Đề độc lập").build();
        standaloneExam.setId(STANDALONE_EXAM_ID);
        when(examRepository.findByIdAndDeletedFalse(COURSE_EXAM_ID)).thenReturn(Optional.of(courseExam));
        when(examRepository.findByIdAndDeletedFalse(STANDALONE_EXAM_ID)).thenReturn(Optional.of(standaloneExam));
        when(courseRepository.findByIdAndDeletedFalse(COURSE_ID)).thenReturn(Optional.of(course));
        when(examRepository.save(any(Exam.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void courseExam_teacherNotAssigned_cannotManage() {
        doThrow(new AppException(ErrorCode.FORBIDDEN_COURSE_ACCESS))
                .when(courseAccessGuard).requireAssignedOrAdmin(course);
        doThrow(new AppException(ErrorCode.FORBIDDEN_COURSE_ACCESS))
                .when(courseAccessGuard).requireExamContentEditable(course);

        assertRejected(() -> service.deleteExam(COURSE_EXAM_ID), ErrorCode.FORBIDDEN_COURSE_ACCESS);
        assertRejected(() -> service.getExamPreview(COURSE_EXAM_ID), ErrorCode.FORBIDDEN_COURSE_ACCESS);
        assertRejected(() -> service.deleteQuestion(COURSE_EXAM_ID, 99L), ErrorCode.FORBIDDEN_COURSE_ACCESS);
        assertThat(courseExam.isDeleted()).isFalse();
    }

    @Test
    void approvedCourseExam_delete_onlyMarksPendingRemoval_cancelable() {
        courseExam.setPublishedVersionId(77L);
        courseExam.setPublished(true);

        assertThat(service.deleteExam(COURSE_EXAM_ID)).isFalse();
        // Học viên vẫn thi tới khi khóa được duyệt lần tới
        assertThat(courseExam.isDeleted()).isFalse();
        assertThat(courseExam.isPublished()).isTrue();
        assertThat(courseExam.isPendingRemoval()).isTrue();
        verify(courseAccessGuard).requireExamContentEditable(course);

        service.cancelExamRemoval(COURSE_EXAM_ID);
        assertThat(courseExam.isPendingRemoval()).isFalse();
    }

    @Test
    void neverApprovedCourseExam_deletedImmediately() {
        assertThat(service.deleteExam(COURSE_EXAM_ID)).isTrue();
        assertThat(courseExam.isDeleted()).isTrue();
        assertThat(courseExam.isPendingRemoval()).isFalse();
    }

    @Test
    void standaloneExam_skipsCourseAssignmentCheck() {
        service.deleteExam(STANDALONE_EXAM_ID);

        verify(courseAccessGuard, never()).requireAssignedOrAdmin(any());
        verify(courseAccessGuard, never()).requireExamContentEditable(any());
    }

    @Test
    void courseExam_publishAndUnpublish_onlyAdmin() {
        when(currentUserService.isAdmin()).thenReturn(false);
        assertRejected(() -> service.publishExam(COURSE_EXAM_ID), ErrorCode.FORBIDDEN_COURSE_ACCESS);
        assertRejected(() -> service.unpublishExam(COURSE_EXAM_ID), ErrorCode.FORBIDDEN_COURSE_ACCESS);
        assertThat(courseExam.isPublished()).isFalse();

        // ADMIN: đề chưa từng được duyệt không hiện được (lên cùng lần duyệt khóa)
        when(currentUserService.isAdmin()).thenReturn(true);
        assertRejected(() -> service.publishExam(COURSE_EXAM_ID), ErrorCode.EXAM_NOT_YET_APPROVED);
        assertThat(courseExam.isPublished()).isFalse();

        // Đã có bản duyệt → ADMIN hiện lại được
        courseExam.setPublishedVersionId(77L);
        service.publishExam(COURSE_EXAM_ID);
        assertThat(courseExam.isPublished()).isTrue();
    }

    @Test
    void standaloneExam_teacherCanStillPublish() {
        when(currentUserService.isAdmin()).thenReturn(false);

        service.publishExam(STANDALONE_EXAM_ID);

        assertThat(standaloneExam.isPublished()).isTrue();
    }

    @Test
    void importIntoCourse_teacherNotAssigned_rejected() {
        doThrow(new AppException(ErrorCode.FORBIDDEN_COURSE_ACCESS))
                .when(courseAccessGuard).requireExamContentEditable(course);
        ImportExamRequest request = new ImportExamRequest();
        request.setTitle("Đề mới");
        request.setCourseId(COURSE_ID);

        assertRejected(() -> service.importExam(request, 5L), ErrorCode.FORBIDDEN_COURSE_ACCESS);
        verify(examRepository, never()).save(any());
    }

    @Test
    void importIntoMissingCourse_rejected() {
        when(courseRepository.findByIdAndDeletedFalse(COURSE_ID)).thenReturn(Optional.empty());
        ImportExamRequest request = new ImportExamRequest();
        request.setTitle("Đề mới");
        request.setCourseId(COURSE_ID);

        assertRejected(() -> service.importExam(request, 5L), ErrorCode.COURSE_NOT_FOUND);
    }
}
