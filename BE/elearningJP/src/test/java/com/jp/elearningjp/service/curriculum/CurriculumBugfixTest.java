package com.jp.elearningjp.service.curriculum;

import com.jp.elearningjp.dto.request.curriculum.CourseCreateRequest;
import com.jp.elearningjp.dto.request.curriculum.LessonCreateRequest;
import com.jp.elearningjp.dto.request.curriculum.LessonUpdateRequest;
import com.jp.elearningjp.dto.response.curriculum.CourseResponse;
import com.jp.elearningjp.dto.response.curriculum.LessonDetailResponse;
import com.jp.elearningjp.entity.curriculum.Course;
import com.jp.elearningjp.entity.curriculum.Lesson;
import com.jp.elearningjp.entity.curriculum.LessonSection;
import com.jp.elearningjp.entity.user.User;
import com.jp.elearningjp.exception.AppException;
import com.jp.elearningjp.exception.ErrorCode;
import com.jp.elearningjp.mapper.CourseMapper;
import com.jp.elearningjp.mapper.LessonMapper;
import com.jp.elearningjp.mapper.LessonMapperImpl;
import com.jp.elearningjp.mapper.LessonSectionMapper;
import com.jp.elearningjp.repository.curriculum.CourseEnrollmentRepository;
import com.jp.elearningjp.repository.curriculum.CourseRepository;
import com.jp.elearningjp.repository.curriculum.LessonRepository;
import com.jp.elearningjp.repository.curriculum.LessonSectionRepository;
import com.jp.elearningjp.repository.curriculum.QuizRepository;
import com.jp.elearningjp.repository.curriculum.UserLessonProgressRepository;
import com.jp.elearningjp.shared.enums.LessonSectionType;
import com.jp.elearningjp.shared.enums.ProgressStatus;
import com.jp.elearningjp.shared.util.CourseAccessGuard;
import com.jp.elearningjp.shared.util.CurrentUserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Tái hiện các lỗi của module khóa học / bài học. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CurriculumBugfixTest {

    private static final Long COURSE_ID = 10L;
    private static final Long LESSON_ID = 20L;
    private static final Long USER_ID = 1L;

    @Mock CourseRepository courseRepository;
    @Mock LessonRepository lessonRepository;
    @Mock LessonSectionRepository sectionRepository;
    @Mock CourseEnrollmentRepository enrollmentRepository;
    @Mock UserLessonProgressRepository progressRepository;
    @Mock QuizRepository quizRepository;
    @Mock CourseMapper courseMapper;
    @Mock LessonSectionMapper lessonSectionMapper;
    @Mock CurrentUserService currentUserService;
    @Mock CourseAccessGuard courseAccessGuard;
    @Mock SectionService sectionService;
    @Spy LessonMapper lessonMapper = new LessonMapperImpl();

    @InjectMocks CourseService courseService;
    @InjectMocks LessonService lessonService;

    private static Lesson lessonAt(long id, int orderIndex, Course course) {
        Lesson lesson = Lesson.builder().title("L" + id).orderIndex(orderIndex).course(course).build();
        lesson.setId(id);
        return lesson;
    }

    private Course activeCourse() {
        Course course = new Course();
        course.setId(COURSE_ID);
        when(courseRepository.findByIdAndDeletedFalse(COURSE_ID)).thenReturn(Optional.of(course));
        return course;
    }

    // ---- orderIndex tạm khi còn lỗ hổng do xoá mềm

    @Test
    void createLesson_afterMiddleDeleted_tempIndexDoesNotCollide() {
        Course course = activeCourse();
        when(lessonRepository.findByCourseIdAndDeletedFalseOrderByOrderIndexAsc(COURSE_ID))
                .thenReturn(List.of(lessonAt(1, 1, course), lessonAt(3, 3, course)));
        Integer[] indexAtInsert = new Integer[1];
        when(lessonRepository.save(any(Lesson.class))).thenAnswer(i -> {
            Lesson saved = i.getArgument(0);
            indexAtInsert[0] = saved.getOrderIndex();
            saved.setId(4L);
            return saved;
        });

        LessonCreateRequest request = new LessonCreateRequest();
        request.setTitle("Mới");
        LessonDetailResponse response = lessonService.createLesson(COURSE_ID, request);

        assertThat(indexAtInsert[0]).isEqualTo(4); // size+1 = 3 sẽ trùng UNIQUE với bài đang ở vị trí 3
        assertThat(response.getOrderIndex()).isEqualTo(3); // đánh số lại liên tục 1..N
    }

    @Test
    void createSection_afterMiddleDeleted_tempIndexDoesNotCollide() {
        LessonSection first = LessonSection.builder().orderIndex(1).build();
        first.setId(1L);
        LessonSection third = LessonSection.builder().orderIndex(3).build();
        third.setId(3L);
        Lesson lesson = lessonAt(LESSON_ID, 1, new Course());
        when(lessonRepository.findByIdAndDeletedFalse(LESSON_ID)).thenReturn(Optional.of(lesson));
        when(sectionRepository.findByLessonIdAndDeletedFalseOrderByOrderIndexAsc(LESSON_ID))
                .thenReturn(List.of(first, third));
        Integer[] indexAtInsert = new Integer[1];
        when(sectionRepository.save(any(LessonSection.class))).thenAnswer(i -> {
            LessonSection saved = i.getArgument(0);
            indexAtInsert[0] = saved.getOrderIndex();
            saved.setId(4L);
            return saved;
        });

        SectionService service = new SectionService(sectionRepository, lessonRepository, courseRepository,
                courseAccessGuard, null, null, null, null, null, null, null, quizRepository, null,
                lessonSectionMapper);
        service.createSection(LESSON_ID, LessonSectionType.KANJI, "Mới", null);

        assertThat(indexAtInsert[0]).isEqualTo(4);
    }

    // ---- updateLesson không được ghi đè orderIndex đã kẹp

    @Test
    void updateLesson_clampedOrderIndexNotOverwrittenByRawRequest() {
        Course course = new Course();
        course.setId(COURSE_ID);
        Lesson first = lessonAt(1, 1, course);
        Lesson second = lessonAt(2, 2, course);
        when(lessonRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(first));
        when(lessonRepository.findByCourseIdAndDeletedFalseOrderByOrderIndexAsc(COURSE_ID))
                .thenReturn(List.of(first, second));
        when(lessonRepository.save(any(Lesson.class))).thenAnswer(i -> i.getArgument(0));

        LessonDetailResponse response = lessonService.updateLesson(1L, LessonUpdateRequest.builder()
                .title("Tên mới").orderIndex(99).build());

        assertThat(first.getTitle()).isEqualTo("Tên mới");
        assertThat(response.getOrderIndex()).isEqualTo(2);
        assertThat(second.getOrderIndex()).isEqualTo(1);
    }

    // ---- getLessonDetail: giáo viên xem bài của mình

    @Test
    void getLessonDetail_ownerNotEnrolled_seesLessonWithoutCreatingProgress() {
        Course course = new Course();
        course.setId(COURSE_ID);
        when(lessonRepository.findByIdAndDeletedFalse(LESSON_ID))
                .thenReturn(Optional.of(lessonAt(LESSON_ID, 1, course)));
        User owner = mock(User.class);
        when(owner.getId()).thenReturn(USER_ID);
        when(currentUserService.getCurrentUser()).thenReturn(owner);

        LessonDetailResponse response = lessonService.getLessonDetail(LESSON_ID);

        assertThat(response.getStatus()).isEqualTo(ProgressStatus.NOT_STARTED);
        verify(courseAccessGuard).requireLessonContentAccess(any());
        verify(courseAccessGuard, never()).requireEnrolled(anyLong());
        verify(progressRepository, never()).save(any());
    }

    // ---- slug

    private String createCourseAndCaptureSlug(String title) {
        when(currentUserService.getCurrentUser()).thenReturn(mock(User.class));
        when(courseMapper.toCourseEntity(any())).thenReturn(new Course());
        when(courseRepository.save(any(Course.class))).thenAnswer(i -> i.getArgument(0));
        when(courseMapper.toCourseResponse(any())).thenReturn(new CourseResponse());
        CourseCreateRequest request = new CourseCreateRequest();
        request.setTitle(title);

        courseService.createCourse(request);

        ArgumentCaptor<Course> saved = ArgumentCaptor.forClass(Course.class);
        verify(courseRepository).save(saved.capture());
        return saved.getValue().getSlug();
    }

    @Test
    void createCourse_slugTakenByDeletedCourse_getsSuffix() {
        // existsBySlug tính cả khóa đã xoá mềm.
        when(courseRepository.existsBySlug("tieng-nhat")).thenReturn(true);
        assertThat(createCourseAndCaptureSlug("Tiếng Nhật")).isEqualTo("tieng-nhat-1");
    }

    @Test
    void createCourse_japaneseOnlyTitle_getsDefaultSlug() {
        assertThat(createCourseAndCaptureSlug("日本語入門")).isEqualTo("khoa-hoc");
    }

    // ---- khóa chưa publish

    private Course unpublishedCourseWithOwner(Long ownerId) {
        Course course = activeCourse();
        course.setPublished(false);
        when(courseAccessGuard.isOwner(eq(course), eq(ownerId))).thenReturn(true);
        when(courseMapper.toCourseDetailResponse(course))
                .thenReturn(new com.jp.elearningjp.dto.response.curriculum.CourseDetailResponse());
        return course;
    }

    private void assertCourseHidden() {
        assertThatThrownBy(() -> courseService.getCourseDetail(COURSE_ID))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.COURSE_NOT_FOUND);
    }

    @Test
    void getCourseDetail_unpublished_hiddenFromAnonymous() {
        unpublishedCourseWithOwner(USER_ID);
        when(currentUserService.getCurrentUserOrNull()).thenReturn(null);
        assertCourseHidden();
    }

    @Test
    void getCourseDetail_unpublished_hiddenFromOtherUser() {
        unpublishedCourseWithOwner(USER_ID);
        User other = mock(User.class);
        when(other.getId()).thenReturn(99L);
        when(currentUserService.getCurrentUserOrNull()).thenReturn(other);
        assertCourseHidden();
    }

    @Test
    void getCourseDetail_unpublished_visibleToOwner() {
        unpublishedCourseWithOwner(USER_ID);
        User owner = mock(User.class);
        when(owner.getId()).thenReturn(USER_ID);
        when(currentUserService.getCurrentUserOrNull()).thenReturn(owner);
        assertThat(courseService.getCourseDetail(COURSE_ID)).isNotNull();
    }

    @Test
    void getCourseDetail_returnsTotalEnrollments() {
        // Bug cũ: totalEnrollments không được gán → trang quản lý / chi tiết khóa học luôn hiện 0 học viên
        unpublishedCourseWithOwner(USER_ID);
        User owner = mock(User.class);
        when(owner.getId()).thenReturn(USER_ID);
        when(currentUserService.getCurrentUserOrNull()).thenReturn(owner);
        when(enrollmentRepository.countActiveByCourseId(COURSE_ID)).thenReturn(3L);

        assertThat(courseService.getCourseDetail(COURSE_ID).getTotalEnrollments()).isEqualTo(3L);
    }

    // ---- xoá khóa / bài dọn dữ liệu liên quan

    @Test
    void deleteCourse_softDeletesEnrollmentsAndProgress() {
        activeCourse();
        courseService.deleteCourse(COURSE_ID);
        verify(enrollmentRepository).softDeleteByCourseId(eq(COURSE_ID), any());
        verify(progressRepository).softDeleteByCourseId(eq(COURSE_ID), any());
    }

    @Test
    void deleteLesson_softDeletesProgress() {
        when(lessonRepository.findByIdAndDeletedFalse(LESSON_ID))
                .thenReturn(Optional.of(lessonAt(LESSON_ID, 1, new Course())));
        lessonService.deleteLesson(LESSON_ID);
        verify(progressRepository).softDeleteByLessonId(eq(LESSON_ID), any());
    }
}
