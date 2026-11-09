package com.jp.elearningjp.service.curriculum.snapshot;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jp.elearningjp.dto.response.curriculum.LessonDetailResponse;
import com.jp.elearningjp.dto.response.curriculum.LessonOutlineResponse;
import com.jp.elearningjp.dto.response.curriculum.LessonSpeakingResponse;
import com.jp.elearningjp.entity.curriculum.Course;
import com.jp.elearningjp.entity.curriculum.CoursePublishedLesson;
import com.jp.elearningjp.entity.curriculum.CoursePublishedVersion;
import com.jp.elearningjp.entity.curriculum.Lesson;
import com.jp.elearningjp.entity.curriculum.LessonSection;
import com.jp.elearningjp.entity.user.User;
import com.jp.elearningjp.exception.AppException;
import com.jp.elearningjp.exception.ErrorCode;
import com.jp.elearningjp.mapper.LessonMapper;
import com.jp.elearningjp.mapper.LessonSectionMapper;
import com.jp.elearningjp.repository.curriculum.CoursePublishedLessonRepository;
import com.jp.elearningjp.repository.curriculum.CoursePublishedVersionRepository;
import com.jp.elearningjp.repository.curriculum.LessonRepository;
import com.jp.elearningjp.repository.curriculum.LessonSectionRepository;
import com.jp.elearningjp.repository.curriculum.LessonSpeakingScenarioRepository;
import com.jp.elearningjp.repository.curriculum.QuizRepository;
import com.jp.elearningjp.service.curriculum.LessonSpeakingService;
import com.jp.elearningjp.service.curriculum.QuizService;
import com.jp.elearningjp.service.curriculum.SectionService;
import com.jp.elearningjp.service.exam.snapshot.ExamSnapshotService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Tạo bản xuất bản (snapshot) của khóa học từ bản đang soạn — gọi khi ADMIN duyệt
 * yêu cầu hoặc tự xuất bản. Học viên học trên bản này cho tới lần duyệt sau.
 *
 * <p>Mỗi lần tạo: 1 dòng {@code course_published_versions} (mục lục) + 1 dòng
 * {@code course_published_lessons} mỗi bài; cập nhật {@code courses.published_version_id},
 * cờ {@code lessons.in_published_version} và snapshot các đề thi đang xuất bản của khóa.
 * Nội dung không đổi so với phiên bản hiện tại → giữ phiên bản cũ.</p>
 */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class CourseSnapshotService {

    CoursePublishedVersionRepository versionRepository;
    CoursePublishedLessonRepository publishedLessonRepository;
    LessonRepository lessonRepository;
    LessonSectionRepository lessonSectionRepository;
    QuizRepository quizRepository;
    LessonSpeakingScenarioRepository lessonSpeakingRepo;
    SectionService sectionService;
    QuizService quizService;
    LessonSpeakingService lessonSpeakingService;
    LessonMapper lessonMapper;
    LessonSectionMapper lessonSectionMapper;
    ExamSnapshotService examSnapshotService;
    ObjectMapper objectMapper;

    /**
     * Chụp bản đang soạn thành bản học viên đang học. Phải gọi trong giao dịch của thao tác
     * duyệt / xuất bản; {@code course} là entity đang được quản lý (caller lưu).
     *
     * @param publishedBy ADMIN thực hiện; null khi tạo tự động
     */
    @Transactional
    public CoursePublishedVersion publish(Course course, User publishedBy) {
        Long courseId = course.getId();
        List<Lesson> lessons = lessonRepository.findByCourseIdAndDeletedFalseOrderByOrderIndexAsc(courseId);

        List<LessonOutlineResponse> outlines = new ArrayList<>();
        Map<Long, String> lessonContents = new java.util.LinkedHashMap<>();
        for (Lesson lesson : lessons) {
            LessonSnapshot snapshot = buildLesson(lesson);
            outlines.add(toOutline(lesson, snapshot));
            lessonContents.put(lesson.getId(), write(snapshot));
        }
        String courseContent = write(CourseSnapshot.builder().lessons(outlines).build());

        CoursePublishedVersion version = findUnchanged(course, courseContent, lessonContents)
                .orElseGet(() -> saveVersion(course, publishedBy, courseContent, lessonContents, lessons));
        course.setPublishedVersionId(version.getId());

        List<Long> lessonIds = lessons.stream().map(Lesson::getId).toList();
        if (lessonIds.isEmpty()) {
            lessonRepository.clearPublishedLessons(courseId);
        } else {
            lessonRepository.markPublishedLessons(courseId, lessonIds);
        }

        // Đề thi gắn khóa đi theo cùng luồng duyệt
        examSnapshotService.publishCourseExams(courseId, publishedBy);
        return version;
    }

    private CoursePublishedVersion saveVersion(Course course, User publishedBy, String courseContent,
                                               Map<Long, String> lessonContents, List<Lesson> lessons) {
        CoursePublishedVersion version = versionRepository.save(CoursePublishedVersion.builder()
                .courseId(course.getId())
                .versionNo(versionRepository.findMaxVersionNo(course.getId()) + 1)
                .content(courseContent)
                .publishedBy(publishedBy)
                .build());
        int order = 1;
        for (Lesson lesson : lessons) {
            publishedLessonRepository.save(CoursePublishedLesson.builder()
                    .publishedVersionId(version.getId())
                    .lessonId(lesson.getId())
                    .orderIndex(order++)
                    .content(lessonContents.get(lesson.getId()))
                    .build());
        }
        log.info("[COURSE-SNAPSHOT] Course id={} published version no={} (id={}, {} lessons)",
                course.getId(), version.getVersionNo(), version.getId(), lessons.size());
        return version;
    }

    /** Phiên bản hiện tại nếu nội dung y hệt (tránh tạo phiên bản thừa khi xuất bản lại). */
    private Optional<CoursePublishedVersion> findUnchanged(Course course, String courseContent,
                                                           Map<Long, String> lessonContents) {
        if (course.getPublishedVersionId() == null) {
            return Optional.empty();
        }
        Optional<CoursePublishedVersion> current = versionRepository.findById(course.getPublishedVersionId());
        if (current.isEmpty() || !sameJson(current.get().getContent(), courseContent)) {
            return Optional.empty();
        }
        List<CoursePublishedLesson> rows =
                publishedLessonRepository.findByPublishedVersionIdOrderByOrderIndexAsc(current.get().getId());
        if (rows.size() != lessonContents.size()) {
            return Optional.empty();
        }
        for (CoursePublishedLesson row : rows) {
            String content = lessonContents.get(row.getLessonId());
            if (content == null || !sameJson(row.getContent(), content)) {
                return Optional.empty();
            }
        }
        return current;
    }

    /** Nội dung 1 bài: giống các API đọc của bản đang soạn, bỏ dữ liệu theo người dùng. */
    LessonSnapshot buildLesson(Lesson lesson) {
        Long lessonId = lesson.getId();
        List<LessonSection> sections = lessonSectionRepository.findByLessonIdWithAllItems(lessonId);
        SectionService.RelatedVocabBundle bundle = sectionService.loadNestedCollections(sections);

        QuizSnapshot quiz = quizRepository.findByLessonIdAndDeletedFalse(lessonId)
                .map(quizService::toSnapshot)
                .orElse(null);
        List<LessonSpeakingResponse> speaking = lessonSpeakingRepo
                .findByLessonIdWithScenariosOrderByOrderIndexAsc(lessonId).stream()
                .map(lessonSpeakingService::buildContent)
                .collect(Collectors.toCollection(ArrayList::new));

        LessonDetailResponse detail = lessonMapper.toLessonDetailResponse(lesson);
        detail.setHasQuiz(quiz != null);
        detail.setHasSpeaking(!speaking.isEmpty());
        detail.setSpeakingScenario(null);
        detail.setSpeakingScenarios(null);
        detail.setStatus(null);
        detail.setBestScore(null);
        detail.setLastAccessedAt(null);
        detail.setCompletedAt(null);
        // Cùng dạng GET /lessons/{id} (không kèm từ liên quan)
        detail.setSections(lessonSectionMapper.toSectionResponses(sections));

        return LessonSnapshot.builder()
                .detail(detail)
                .sections(new ArrayList<>(lessonSectionMapper.toSectionResponses(
                        sections, bundle.relatedVocabsByCharacter(), bundle.teacherRelatedVocabsByKanjiItem())))
                .quiz(quiz)
                .speaking(speaking)
                .build();
    }

    private LessonOutlineResponse toOutline(Lesson lesson, LessonSnapshot snapshot) {
        LessonOutlineResponse outline = lessonMapper.toLessonOutlineResponse(lesson);
        outline.setHasQuiz(snapshot.getQuiz() != null);
        outline.setHasSpeaking(!snapshot.getSpeaking().isEmpty());
        outline.setStatus(null);
        outline.setBestScore(null);
        return outline;
    }

    private String write(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            log.error("[COURSE-SNAPSHOT] Cannot serialize snapshot", e);
            throw new AppException(ErrorCode.UNCAUGHT_EXCEPTION);
        }
    }

    private boolean sameJson(String a, String b) {
        try {
            return objectMapper.readTree(a).equals(objectMapper.readTree(b));
        } catch (JsonProcessingException e) {
            return false;
        }
    }
}
