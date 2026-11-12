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
import com.jp.elearningjp.entity.curriculum.LessonDraft;
import com.jp.elearningjp.entity.curriculum.LessonKanjiRelatedVocab;
import com.jp.elearningjp.entity.curriculum.LessonSpeakingScenario;
import com.jp.elearningjp.entity.curriculum.Quiz;
import com.jp.elearningjp.entity.curriculum.LessonSection;
import com.jp.elearningjp.entity.user.User;
import com.jp.elearningjp.exception.AppException;
import com.jp.elearningjp.exception.ErrorCode;
import com.jp.elearningjp.mapper.LessonMapper;
import com.jp.elearningjp.mapper.LessonSectionMapper;
import com.jp.elearningjp.repository.curriculum.CoursePublishedLessonRepository;
import com.jp.elearningjp.repository.curriculum.CoursePublishedVersionRepository;
import com.jp.elearningjp.repository.curriculum.LessonDraftRepository;
import com.jp.elearningjp.repository.curriculum.LessonRepository;
import com.jp.elearningjp.repository.curriculum.LessonSectionRepository;
import com.jp.elearningjp.repository.curriculum.LessonSpeakingScenarioRepository;
import com.jp.elearningjp.repository.curriculum.QuizRepository;
import com.jp.elearningjp.service.curriculum.LessonSpeakingService;
import com.jp.elearningjp.service.curriculum.QuizService;
import com.jp.elearningjp.service.curriculum.SectionService;
import com.jp.elearningjp.service.exam.snapshot.ExamSnapshotService;
import com.jp.elearningjp.shared.enums.DraftKind;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
    LessonDraftRepository lessonDraftRepository;
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
     * Chụp cả khóa (từ nháp phía {@code kind} của từng bài) thành bản học viên đang học. Phải gọi trong giao dịch của
     * thao tác duyệt / xuất bản; {@code course} là entity đang được quản lý (caller lưu). Caller cập nhật
     * {@code live_revision} và các nháp sau đó.
     *
     * @param publishedBy ADMIN thực hiện; null khi tạo tự động
     */
    @Transactional
    public CoursePublishedVersion publish(Course course, User publishedBy, DraftKind kind) {
        Long courseId = course.getId();
        // V66: bài mới phía bên kia tạo (chưa lên bản học viên) không thuộc bản chụp của phía này
        List<Lesson> lessons = lessonRepository.findVisibleByCourseId(courseId, kind);

        List<LessonOutlineResponse> outlines = new ArrayList<>();
        Map<Long, String> lessonContents = new java.util.LinkedHashMap<>();
        for (Lesson lesson : lessons) {
            LessonDraft draft = lessonDraftRepository.findActive(lesson.getId(), kind.name())
                    .orElseThrow(() -> new AppException(ErrorCode.LESSON_DRAFT_NOT_FOUND));
            LessonSnapshot snapshot = buildDraft(draft);
            outlines.add(toOutline(lesson, snapshot));
            lessonContents.put(lesson.getId(), write(snapshot));
        }
        String courseContent = write(CourseSnapshot.builder().lessons(outlines).build());

        CoursePublishedVersion version = findUnchanged(course, courseContent, lessonContents)
                .orElseGet(() -> saveVersion(course, publishedBy, courseContent, lessonContents));
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

    /**
     * Duyệt / ADMIN lưu <b>một số bài</b> (luồng duyệt theo bài, V62): phiên bản mới = phiên bản đang học
     * + các bài {@code lessonIds} chụp lại từ bản nháp. Bài khác giữ nguyên nội dung và vị trí của bản đang học
     * (kể cả bài đã đổi / xóa ở bản nháp nhưng chưa duyệt). Bài mới chưa từng duyệt được thêm vào theo thứ tự
     * hiện tại của bản nháp. Đề thi gắn khóa KHÔNG đổi (đi theo luồng ADMIN lưu cả khóa).
     * Phải gọi trong giao dịch của thao tác duyệt; caller lưu {@code course}.
     */
    @Transactional
    public CoursePublishedVersion publishLessons(Course course, Map<Long, LessonSnapshot> lessonSnapshots,
                                                 User publishedBy) {
        Map<Long, LessonOutlineResponse> outlines = new LinkedHashMap<>();
        Map<Long, String> contents = new HashMap<>();
        if (course.getPublishedVersionId() != null) {
            versionRepository.findById(course.getPublishedVersionId()).ifPresent(current -> {
                read(current.getContent(), CourseSnapshot.class).getLessons()
                        .forEach(o -> outlines.put(o.getId(), o));
                publishedLessonRepository.findByPublishedVersionIdOrderByOrderIndexAsc(current.getId())
                        .forEach(row -> contents.put(row.getLessonId(), row.getContent()));
            });
        }
        for (Map.Entry<Long, LessonSnapshot> entry : lessonSnapshots.entrySet()) {
            Long lessonId = entry.getKey();
            Lesson lesson = lessonRepository.findByIdAndDeletedFalse(lessonId)
                    .orElseThrow(() -> new AppException(ErrorCode.LESSON_NOT_FOUND));
            LessonSnapshot snapshot = entry.getValue();
            outlines.put(lessonId, toOutline(lesson, snapshot));
            contents.put(lessonId, write(snapshot));
        }

        // Thứ tự: orderIndex của mục lục (bài vừa duyệt lấy theo bản nháp), trùng thì theo id
        List<LessonOutlineResponse> ordered = outlines.values().stream()
                .filter(o -> contents.containsKey(o.getId()))
                .sorted(Comparator.comparing(LessonOutlineResponse::getOrderIndex,
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(LessonOutlineResponse::getId))
                .toList();
        Map<Long, String> orderedContents = new LinkedHashMap<>();
        ordered.forEach(o -> orderedContents.put(o.getId(), contents.get(o.getId())));

        CoursePublishedVersion version = saveVersion(course, publishedBy,
                write(CourseSnapshot.builder().lessons(new ArrayList<>(ordered)).build()), orderedContents);
        course.setPublishedVersionId(version.getId());
        lessonRepository.markPublishedLessons(course.getId(), orderedContents.keySet());
        return version;
    }

    /** {@code lessonContents} theo đúng thứ tự bài trong phiên bản. */
    private CoursePublishedVersion saveVersion(Course course, User publishedBy, String courseContent,
                                               Map<Long, String> lessonContents) {
        CoursePublishedVersion version = versionRepository.save(CoursePublishedVersion.builder()
                .courseId(course.getId())
                .versionNo(versionRepository.findMaxVersionNo(course.getId()) + 1)
                .content(courseContent)
                .publishedBy(publishedBy)
                .build());
        int order = 1;
        for (Map.Entry<Long, String> entry : lessonContents.entrySet()) {
            publishedLessonRepository.save(CoursePublishedLesson.builder()
                    .publishedVersionId(version.getId())
                    .lessonId(entry.getKey())
                    .orderIndex(order++)
                    .content(entry.getValue())
                    .build());
        }
        log.info("[COURSE-SNAPSHOT] Course id={} published version no={} (id={}, {} lessons)",
                course.getId(), version.getVersionNo(), version.getId(), lessonContents.size());
        return version;
    }

    private <T> T read(String json, Class<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException e) {
            log.error("[COURSE-SNAPSHOT] Cannot read snapshot", e);
            throw new AppException(ErrorCode.UNCAUGHT_EXCEPTION);
        }
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

    /** Trường kỹ thuật / thay đổi theo lần ghi — bỏ qua khi so bản nháp với bản live. */
    private static final java.util.Set<String> VOLATILE_KEYS = java.util.Set.of(
            "id", "version", "quizId", "lessonId", "createdAt", "updatedAt",
            "activeSessionId", "userTurnsCount", "completed");

    /**
     * Bản nháp hiện tại của bài có trùng nội dung với {@code live} không (bỏ qua id / version / thời điểm, và thứ tự
     * bài trong khóa — thuộc cấp khóa). Dùng để tự kiểm sau khi "bỏ nháp, sửa từ bản live".
     */
    public boolean draftMatchesLive(LessonDraft lessonDraft, LessonSnapshot live) {
        com.fasterxml.jackson.databind.JsonNode draft = normalize(objectMapper.valueToTree(buildDraft(lessonDraft)));
        com.fasterxml.jackson.databind.JsonNode target = normalize(objectMapper.valueToTree(live));
        return draft.equals(target);
    }

    private static com.fasterxml.jackson.databind.JsonNode normalize(com.fasterxml.jackson.databind.JsonNode node) {
        if (node instanceof com.fasterxml.jackson.databind.node.ObjectNode obj) {
            obj.remove(VOLATILE_KEYS);
            com.fasterxml.jackson.databind.JsonNode detail = obj.get("detail");
            if (detail instanceof com.fasterxml.jackson.databind.node.ObjectNode d) {
                d.remove("orderIndex");
            }
            obj.elements().forEachRemaining(CourseSnapshotService::normalize);
        } else if (node != null && node.isArray()) {
            node.elements().forEachRemaining(CourseSnapshotService::normalize);
        }
        return node;
    }

    /**
     * Nội dung 1 bài theo 1 bản nháp: giống các API đọc của bản đang soạn, bỏ dữ liệu theo người dùng; id là id ổn
     * định ({@code COALESCE(origin_id, id)}, V65) để trùng id học viên đang thấy.
     */
    public LessonSnapshot buildDraft(LessonDraft draft) {
        Lesson lesson = draft.getLesson();
        Long draftId = draft.getId();
        List<LessonSection> sections = lessonSectionRepository.findByDraftIdWithAllItems(draftId);
        SectionService.RelatedVocabBundle bundle = sectionService.loadNestedCollections(sections);

        Optional<Quiz> quizEntity = quizRepository.findByDraftIdAndDeletedFalse(draftId);
        QuizSnapshot quiz = quizEntity.map(quizService::toSnapshot).orElse(null);
        List<LessonSpeakingScenario> speakingEntities =
                lessonSpeakingRepo.findByDraftIdWithScenariosOrderByOrderIndexAsc(draftId);
        List<LessonSpeakingResponse> speaking = speakingEntities.stream()
                .map(lessonSpeakingService::buildContent)
                .collect(Collectors.toCollection(ArrayList::new));
        Map<Long, List<LessonKanjiRelatedVocab>> teacherRelated = bundle.teacherRelatedVocabsByKanjiItem() != null
                ? bundle.teacherRelatedVocabsByKanjiItem() : Map.of();

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

        LessonSnapshot snapshot = LessonSnapshot.builder()
                .detail(detail)
                .sections(new ArrayList<>(lessonSectionMapper.toSectionResponses(
                        sections, bundle.relatedVocabsByCharacter(), bundle.teacherRelatedVocabsByKanjiItem())))
                .quiz(quiz)
                .speaking(speaking)
                .build();
        DraftIdRemapper.of(sections, teacherRelated, quizEntity.orElse(null), speakingEntities).apply(snapshot);
        return snapshot;
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
