package com.jp.elearningjp.service.curriculum.snapshot;

import com.jp.elearningjp.dto.response.curriculum.CourseChangesResponse;
import com.jp.elearningjp.dto.response.curriculum.CourseChangesResponse.LessonChange;
import com.jp.elearningjp.dto.response.curriculum.CourseChangesResponse.Status;
import com.jp.elearningjp.dto.response.curriculum.LessonKanjiItemResponse;
import com.jp.elearningjp.dto.response.curriculum.LessonOutlineResponse;
import com.jp.elearningjp.dto.response.curriculum.LessonSectionResponse;
import com.jp.elearningjp.entity.curriculum.Course;
import com.jp.elearningjp.entity.curriculum.Lesson;
import com.jp.elearningjp.entity.exam.Exam;
import com.jp.elearningjp.repository.curriculum.CoursePublishedVersionRepository;
import com.jp.elearningjp.repository.curriculum.CourseRepository;
import com.jp.elearningjp.repository.curriculum.LessonRepository;
import com.jp.elearningjp.repository.exam.ExamRepository;
import com.jp.elearningjp.service.exam.snapshot.ExamSnapshotService;
import com.jp.elearningjp.shared.enums.LessonSectionType;
import com.jp.elearningjp.shared.util.CourseAccessGuard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Danh sách thay đổi chờ duyệt: bài / mục / item thêm, bỏ, sửa, đổi thứ tự; đề thi gắn khóa. */
class CourseChangesServiceTest {

    CourseRepository courseRepository = mock(CourseRepository.class);
    LessonRepository lessonRepository = mock(LessonRepository.class);
    ExamRepository examRepository = mock(ExamRepository.class);
    CoursePublishedVersionRepository versionRepository = mock(CoursePublishedVersionRepository.class);
    CourseSnapshotService courseSnapshotService = mock(CourseSnapshotService.class);
    CourseSnapshotReader snapshotReader = mock(CourseSnapshotReader.class);
    ExamSnapshotService examSnapshotService = mock(ExamSnapshotService.class);
    CourseAccessGuard guard = mock(CourseAccessGuard.class);
    CourseChangesService service = new CourseChangesService(courseRepository, lessonRepository, examRepository,
            versionRepository, courseSnapshotService, snapshotReader, examSnapshotService, guard,
            Jackson2ObjectMapperBuilder.json().build());

    Course course;

    private static Lesson lesson(long id, String title, int order) {
        Lesson l = Lesson.builder().title(title).orderIndex(order).build();
        l.setId(id);
        return l;
    }

    private static LessonKanjiItemResponse kanji(long id, String meaning, long version) {
        return LessonKanjiItemResponse.builder().id(id).character("日").meaningVi(meaning).version(version).build();
    }

    private static LessonSnapshot snapshot(LessonKanjiItemResponse... items) {
        return LessonSnapshot.builder().sections(new ArrayList<>(List.of(LessonSectionResponse.builder()
                .id(5L).title("Chữ Hán").sectionType(LessonSectionType.KANJI).kanjiItems(List.of(items)).build())))
                .build();
    }

    @BeforeEach
    void setUp() {
        course = Course.builder().title("N5").slug("n5").build();
        course.setId(1L);
        course.setPublishedVersionId(100L);
        when(courseRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(course));
        when(snapshotReader.findCourse(100L)).thenReturn(Optional.of(CourseSnapshot.builder()
                .lessons(new ArrayList<>(List.of(
                        LessonOutlineResponse.builder().id(10L).title("Bài 1").orderIndex(1).build(),
                        LessonOutlineResponse.builder().id(20L).title("Bài 2").orderIndex(2).build())))
                .build()));
        when(versionRepository.findById(100L)).thenReturn(Optional.empty());
    }

    @Test
    void addedRemovedModifiedLessons_andItemCounts() {
        Lesson l1 = lesson(10L, "Bài 1", 1);
        Lesson l3 = lesson(30L, "Bài 3 mới", 2);
        when(lessonRepository.findByCourseIdAndDeletedFalseOrderByOrderIndexAsc(1L)).thenReturn(List.of(l1, l3));
        // Bài 1: item 50 sửa nghĩa, item 51 bị bỏ, item 52 thêm; item 53 chỉ đổi version (không tính là sửa)
        when(snapshotReader.findLesson(100L, 10L)).thenReturn(Optional.of(snapshot(
                kanji(50, "ngày", 1), kanji(51, "mặt trời", 1), kanji(53, "năm", 1))));
        when(courseSnapshotService.buildLesson(l1)).thenReturn(snapshot(
                kanji(50, "ngày, mặt trời", 2), kanji(53, "năm", 4), kanji(52, "tháng", 0)));
        when(courseSnapshotService.buildLesson(l3)).thenReturn(snapshot(kanji(60, "người", 0)));
        when(examRepository.findByCourseIdAndDeletedFalse(1L)).thenReturn(List.of());

        CourseChangesResponse res = service.getChanges(1L);

        Map<Long, LessonChange> byId = res.getLessons().stream()
                .collect(Collectors.toMap(LessonChange::getLessonId, Function.identity()));
        assertThat(res.isHasChanges()).isTrue();
        assertThat(byId.get(10L).getStatus()).isEqualTo(Status.MODIFIED);
        var items = byId.get(10L).getSections().get(0).getItems();
        assertThat(items.getAdded()).isEqualTo(1);
        assertThat(items.getRemoved()).isEqualTo(1);
        assertThat(items.getModified()).isEqualTo(1);
        assertThat(byId.get(30L).getStatus()).isEqualTo(Status.ADDED);
        assertThat(byId.get(30L).getSections().get(0).getItems().getAdded()).isEqualTo(1);
        assertThat(byId.get(20L).getStatus()).isEqualTo(Status.REMOVED);
        verify(guard).requireAssignedOrAdmin(course);
    }

    @Test
    void identicalContent_unchanged_evenIfVersionsDiffer() {
        Lesson l1 = lesson(10L, "Bài 1", 1);
        Lesson l2 = lesson(20L, "Bài 2", 2);
        when(lessonRepository.findByCourseIdAndDeletedFalseOrderByOrderIndexAsc(1L)).thenReturn(List.of(l1, l2));
        when(snapshotReader.findLesson(100L, 10L)).thenReturn(Optional.of(snapshot(kanji(50, "ngày", 1))));
        when(snapshotReader.findLesson(100L, 20L)).thenReturn(Optional.of(snapshot()));
        when(courseSnapshotService.buildLesson(l1)).thenReturn(snapshot(kanji(50, "ngày", 9)));
        when(courseSnapshotService.buildLesson(l2)).thenReturn(snapshot());
        Exam exam = Exam.builder().title("Giữa kỳ").publishedVersionId(7L).build();
        exam.setId(70L);
        when(examRepository.findByCourseIdAndDeletedFalse(1L)).thenReturn(List.of(exam));
        when(examSnapshotService.differsFromPublished(70L)).thenReturn(false);

        CourseChangesResponse res = service.getChanges(1L);

        assertThat(res.isHasChanges()).isFalse();
        assertThat(res.getLessons()).allMatch(l -> l.getStatus() == Status.UNCHANGED);
        assertThat(res.getExams()).singleElement().extracting(e -> e.getStatus()).isEqualTo(Status.UNCHANGED);
    }

    @Test
    void lessonOrderSwapped_markedMoved() {
        Lesson l1 = lesson(10L, "Bài 1", 2);
        Lesson l2 = lesson(20L, "Bài 2", 1);
        when(lessonRepository.findByCourseIdAndDeletedFalseOrderByOrderIndexAsc(1L)).thenReturn(List.of(l2, l1));
        when(snapshotReader.findLesson(100L, 10L)).thenReturn(Optional.of(snapshot()));
        when(snapshotReader.findLesson(100L, 20L)).thenReturn(Optional.of(snapshot()));
        when(courseSnapshotService.buildLesson(l1)).thenReturn(snapshot());
        when(courseSnapshotService.buildLesson(l2)).thenReturn(snapshot());
        when(examRepository.findByCourseIdAndDeletedFalse(1L)).thenReturn(List.of());

        CourseChangesResponse res = service.getChanges(1L);

        assertThat(res.getLessons()).allMatch(l -> l.isMoved() && l.getStatus() == Status.MODIFIED);
    }

    @Test
    void movedIds_insertingElement_doesNotMarkOthersMoved() {
        assertThat(CourseChangesService.movedIds(List.of(1L, 9L, 2L, 3L), List.of(1L, 2L, 3L))).isEmpty();
        assertThat(CourseChangesService.movedIds(List.of(2L, 1L, 3L), List.of(1L, 2L, 3L))).containsExactlyInAnyOrder(1L, 2L);
    }
}
