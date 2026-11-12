package com.jp.elearningjp.service.curriculum.snapshot;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jp.elearningjp.dto.response.curriculum.LessonDetailResponse;
import com.jp.elearningjp.dto.response.curriculum.LessonOutlineResponse;
import com.jp.elearningjp.entity.curriculum.Course;
import com.jp.elearningjp.entity.curriculum.CoursePublishedLesson;
import com.jp.elearningjp.entity.curriculum.CoursePublishedVersion;
import com.jp.elearningjp.entity.curriculum.Lesson;
import com.jp.elearningjp.mapper.LessonMapper;
import com.jp.elearningjp.repository.curriculum.CoursePublishedLessonRepository;
import com.jp.elearningjp.repository.curriculum.CoursePublishedVersionRepository;
import com.jp.elearningjp.repository.curriculum.LessonRepository;
import com.jp.elearningjp.service.exam.snapshot.ExamSnapshotService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Duyệt / lưu theo bài: phiên bản live mới = phiên bản trước + bản chụp của bài vừa duyệt / lưu (V65: bản chụp
 * lúc gửi duyệt hoặc chụp từ nháp ADMIN — caller truyền sẵn).
 * Bài khác giữ nguyên nội dung cũ; bài mới thêm theo thứ tự bản nháp; đề thi không bị chụp lại.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CourseSnapshotPublishLessonsTest {

    @Mock CoursePublishedVersionRepository versionRepository;
    @Mock CoursePublishedLessonRepository publishedLessonRepository;
    @Mock LessonRepository lessonRepository;
    @Mock LessonMapper lessonMapper;
    @Mock ExamSnapshotService examSnapshotService;
    @Spy ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build();
    @Spy @InjectMocks CourseSnapshotService service;

    private Course course;
    private final List<CoursePublishedLesson> savedRows = new ArrayList<>();

    private static LessonOutlineResponse outline(Long id, String title, int order) {
        LessonOutlineResponse o = new LessonOutlineResponse();
        o.setId(id);
        o.setTitle(title);
        o.setOrderIndex(order);
        return o;
    }

    private Lesson lesson(Long id, String title, int order) {
        Lesson l = Lesson.builder().course(course).title(title).orderIndex(order).build();
        l.setId(id);
        when(lessonRepository.findByIdAndDeletedFalse(id)).thenReturn(Optional.of(l));
        when(lessonMapper.toLessonOutlineResponse(l)).thenReturn(outline(id, title, order));
        return l;
    }

    private static LessonSnapshot content(String title) {
        LessonDetailResponse detail = new LessonDetailResponse();
        detail.setTitle(title);
        return LessonSnapshot.builder().detail(detail).build();
    }

    @BeforeEach
    void setUp() throws Exception {
        course = Course.builder().title("N5").published(true).build();
        course.setId(10L);
        course.setPublishedVersionId(70L);

        // Bản live hiện tại: bài 1 ("Cũ 1") và bài 2 ("Cũ 2")
        String courseJson = objectMapper.writeValueAsString(CourseSnapshot.builder()
                .lessons(new ArrayList<>(List.of(outline(1L, "Cũ 1", 1), outline(2L, "Cũ 2", 2)))).build());
        CoursePublishedVersion current = CoursePublishedVersion.builder()
                .courseId(10L).versionNo(4).content(courseJson).build();
        current.setId(70L);
        when(versionRepository.findById(70L)).thenReturn(Optional.of(current));
        when(publishedLessonRepository.findByPublishedVersionIdOrderByOrderIndexAsc(70L)).thenReturn(List.of(
                CoursePublishedLesson.builder().publishedVersionId(70L).lessonId(1L).orderIndex(1)
                        .content("{\"old\":1}").build(),
                CoursePublishedLesson.builder().publishedVersionId(70L).lessonId(2L).orderIndex(2)
                        .content("{\"old\":2}").build()));

        when(versionRepository.findMaxVersionNo(10L)).thenReturn(4);
        when(versionRepository.save(any(CoursePublishedVersion.class))).thenAnswer(inv -> {
            CoursePublishedVersion v = inv.getArgument(0);
            v.setId(71L);
            return v;
        });
        when(publishedLessonRepository.save(any(CoursePublishedLesson.class))).thenAnswer(inv -> {
            savedRows.add(inv.getArgument(0));
            return inv.getArgument(0);
        });
    }

    @Test
    void approvedLessonsReplaced_othersKeptAsLive_newLessonAppended_examsUntouched() throws Exception {
        lesson(2L, "Mới 2", 2);
        lesson(3L, "Mới 3", 3);
        java.util.Map<Long, LessonSnapshot> contents = new java.util.LinkedHashMap<>();
        contents.put(2L, content("Mới 2"));
        contents.put(3L, content("Mới 3"));

        CoursePublishedVersion v = service.publishLessons(course, contents, null);

        assertThat(v.getVersionNo()).isEqualTo(5);
        assertThat(course.getPublishedVersionId()).isEqualTo(71L);
        assertThat(savedRows).extracting(CoursePublishedLesson::getLessonId).containsExactly(1L, 2L, 3L);
        assertThat(savedRows).extracting(CoursePublishedLesson::getOrderIndex).containsExactly(1, 2, 3);
        // Bài 1 giữ nguyên nội dung bản live cũ; bài 2 lấy từ bản nháp
        assertThat(savedRows.get(0).getContent()).isEqualTo("{\"old\":1}");
        assertThat(savedRows.get(1).getContent()).contains("Mới 2");

        CourseSnapshot toc = objectMapper.readValue(v.getContent(), CourseSnapshot.class);
        assertThat(toc.getLessons()).extracting(LessonOutlineResponse::getTitle)
                .containsExactly("Cũ 1", "Mới 2", "Mới 3");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<Long>> ids = ArgumentCaptor.forClass(Collection.class);
        verify(lessonRepository).markPublishedLessons(eq(10L), ids.capture());
        assertThat(ids.getValue()).containsExactly(1L, 2L, 3L);
        verify(examSnapshotService, never()).publishCourseExams(any(), any());
    }

    @Test
    void firstApproval_courseWithoutLiveVersion_containsOnlyApprovedLesson() {
        course.setPublishedVersionId(null);
        when(versionRepository.findMaxVersionNo(10L)).thenReturn(0);
        lesson(3L, "Mới 3", 3);

        CoursePublishedVersion v = service.publishLessons(course, java.util.Map.of(3L, content("Mới 3")), null);

        assertThat(v.getVersionNo()).isEqualTo(1);
        assertThat(savedRows).extracting(CoursePublishedLesson::getLessonId).containsExactly(3L);
    }

    @Test
    void draftMatchesLive_ignoresIdsVersionsAndLessonOrder_butNotContent() {
        Lesson lesson = Lesson.builder().course(course).title("B").orderIndex(1).build();
        lesson.setId(9L);
        LessonDetailResponse liveDetail = new LessonDetailResponse();
        liveDetail.setTitle("B");
        liveDetail.setOrderIndex(1);
        com.jp.elearningjp.dto.response.curriculum.LessonSectionResponse liveSection =
                com.jp.elearningjp.dto.response.curriculum.LessonSectionResponse.builder()
                        .id(1L).version(3L).title("Từ vựng").orderIndex(1).build();
        LessonSnapshot live = LessonSnapshot.builder().detail(liveDetail)
                .sections(new ArrayList<>(List.of(liveSection))).build();

        LessonDetailResponse draftDetail = new LessonDetailResponse();
        draftDetail.setTitle("B");
        draftDetail.setOrderIndex(4); // thứ tự bài là cấp khóa
        com.jp.elearningjp.dto.response.curriculum.LessonSectionResponse draftSection =
                com.jp.elearningjp.dto.response.curriculum.LessonSectionResponse.builder()
                        .id(77L).version(9L).title("Từ vựng").orderIndex(1).build();
        com.jp.elearningjp.entity.curriculum.LessonDraft draft =
                com.jp.elearningjp.entity.curriculum.LessonDraft.builder().lesson(lesson).build();
        doReturn(LessonSnapshot.builder().detail(draftDetail).sections(new ArrayList<>(List.of(draftSection))).build())
                .when(service).buildDraft(draft);
        assertThat(service.draftMatchesLive(draft, live)).isTrue();

        draftSection.setTitle("Đã sửa");
        assertThat(service.draftMatchesLive(draft, live)).isFalse();
    }
}
