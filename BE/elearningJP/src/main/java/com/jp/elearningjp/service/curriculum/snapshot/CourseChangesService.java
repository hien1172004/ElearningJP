package com.jp.elearningjp.service.curriculum.snapshot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jp.elearningjp.dto.response.curriculum.CourseChangesResponse;
import com.jp.elearningjp.dto.response.curriculum.CourseChangesResponse.ExamChange;
import com.jp.elearningjp.dto.response.curriculum.CourseChangesResponse.ItemsChange;
import com.jp.elearningjp.dto.response.curriculum.CourseChangesResponse.LessonChange;
import com.jp.elearningjp.dto.response.curriculum.CourseChangesResponse.SectionChange;
import com.jp.elearningjp.dto.response.curriculum.CourseChangesResponse.Status;
import com.jp.elearningjp.dto.response.curriculum.LessonOutlineResponse;
import com.jp.elearningjp.dto.response.curriculum.LessonSectionResponse;
import com.jp.elearningjp.entity.curriculum.Course;
import com.jp.elearningjp.entity.curriculum.Lesson;
import com.jp.elearningjp.exception.AppException;
import com.jp.elearningjp.exception.ErrorCode;
import com.jp.elearningjp.repository.curriculum.CoursePublishedVersionRepository;
import com.jp.elearningjp.repository.curriculum.CourseRepository;
import com.jp.elearningjp.repository.curriculum.LessonRepository;
import com.jp.elearningjp.repository.exam.ExamRepository;
import com.jp.elearningjp.service.exam.snapshot.ExamSnapshotService;
import com.jp.elearningjp.shared.util.CourseAccessGuard;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * So bản đang soạn với bản học viên đang học: bài thêm / bỏ / sửa, trong bài thì section, item,
 * câu hỏi quiz, luyện nói; cùng các đề thi gắn khóa.
 * Bản đang soạn = nháp phía người xem (V65), dựng giống hệt lúc tạo snapshot ({@link CourseSnapshotService#buildDraft},
 * id ổn định) nên so được 1-1.
 * So nội dung bỏ qua {@code version} / {@code orderIndex}; đổi thứ tự báo riêng.
 */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CourseChangesService {

    private static final Set<String> IGNORED_FIELDS = Set.of("version", "orderIndex");

    CourseRepository courseRepository;
    LessonRepository lessonRepository;
    ExamRepository examRepository;
    CoursePublishedVersionRepository versionRepository;
    CourseSnapshotService courseSnapshotService;
    CourseSnapshotReader snapshotReader;
    ExamSnapshotService examSnapshotService;
    CourseAccessGuard courseAccessGuard;
    com.jp.elearningjp.service.curriculum.LessonDraftService lessonDraftService;
    com.jp.elearningjp.repository.curriculum.LessonReviewRequestRepository requestRepository;
    ObjectMapper objectMapper;

    /** Bản chụp lúc gửi duyệt; yêu cầu cũ (trước V65) chưa có → chụp từ nháp đã gửi (đang khóa). */
    private LessonSnapshot submittedContent(com.jp.elearningjp.entity.curriculum.LessonReviewRequest request) {
        if (request.getContent() != null) {
            try {
                return objectMapper.readValue(request.getContent(), LessonSnapshot.class);
            } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                throw new AppException(ErrorCode.UNCAUGHT_EXCEPTION);
            }
        }
        if (request.getDraft() == null) {
            throw new AppException(ErrorCode.LESSON_DRAFT_NOT_FOUND);
        }
        return courseSnapshotService.buildDraft(request.getDraft());
    }

    /**
     * ADMIN hoặc giáo viên được giao. Mỗi phía so nháp của mình với bản học viên; riêng ADMIN, bài giáo viên đang chờ
     * duyệt được so theo bản giáo viên đã gửi (V65/V66).
     */
    @Transactional(readOnly = true)
    public CourseChangesResponse getChanges(Long courseId) {
        Course course = courseRepository.findByIdAndDeletedFalse(courseId)
                .orElseThrow(() -> new AppException(ErrorCode.COURSE_NOT_FOUND));
        courseAccessGuard.requireAssignedOrAdmin(course);

        Long versionId = course.getPublishedVersionId();
        List<LessonOutlineResponse> publishedOutline = snapshotReader.findCourse(versionId)
                .map(CourseSnapshot::getLessons).orElse(List.of());
        Map<Long, LessonOutlineResponse> publishedById = publishedOutline.stream()
                .collect(Collectors.toMap(LessonOutlineResponse::getId, o -> o, (a, b) -> a, LinkedHashMap::new));

        // V66: bài mới phía bên kia tạo (chưa gửi duyệt) bị ẩn
        com.jp.elearningjp.shared.enums.DraftKind kind = lessonDraftService.currentKind();
        List<Lesson> draftLessons = lessonRepository.findReviewableByCourseId(courseId, kind);
        // ADMIN: bài giáo viên đã gửi duyệt → so đúng bản đã gửi (ADMIN không đọc nháp đang soạn của giáo viên)
        Map<Long, com.jp.elearningjp.entity.curriculum.LessonReviewRequest> pendingByLesson =
                kind != com.jp.elearningjp.shared.enums.DraftKind.ADMIN ? Map.of()
                        : requestRepository.findByCourseIdAndStatus(courseId,
                                        com.jp.elearningjp.shared.enums.ReviewRequestStatus.PENDING).stream()
                                .collect(Collectors.toMap(r -> r.getLesson().getId(), r -> r, (a, b) -> a));
        Set<Long> movedIds = movedIds(
                draftLessons.stream().map(Lesson::getId).toList(),
                publishedOutline.stream().map(LessonOutlineResponse::getId).toList());

        List<LessonChange> lessons = new ArrayList<>();
        for (Lesson lesson : draftLessons) {
            com.jp.elearningjp.entity.curriculum.LessonReviewRequest pending = pendingByLesson.get(lesson.getId());
            LessonSnapshot draft = pending != null
                    ? submittedContent(pending)
                    : courseSnapshotService.buildDraft(lessonDraftService.findActive(lesson.getId(), kind)
                            .orElseThrow(() -> new AppException(ErrorCode.LESSON_DRAFT_NOT_FOUND)));
            LessonOutlineResponse publishedLesson = publishedById.get(lesson.getId());
            Optional<LessonSnapshot> published = publishedLesson == null
                    ? Optional.empty()
                    : snapshotReader.findLesson(versionId, lesson.getId());
            lessons.add(published.isEmpty()
                    ? added(lesson, draft)
                    : compare(lesson, draft, published.get(), publishedLesson, movedIds.contains(lesson.getId())));
        }
        Set<Long> draftIds = draftLessons.stream().map(Lesson::getId).collect(Collectors.toSet());
        for (LessonOutlineResponse removed : publishedOutline) {
            if (!draftIds.contains(removed.getId())) {
                lessons.add(LessonChange.builder()
                        .lessonId(removed.getId())
                        .title(removed.getTitle())
                        .publishedOrderIndex(removed.getOrderIndex())
                        .status(Status.REMOVED)
                        .build());
            }
        }

        List<ExamChange> exams = examRepository.findByCourseIdAndDeletedFalse(courseId).stream()
                .map(exam -> ExamChange.builder()
                        .examId(exam.getId())
                        .title(exam.getTitle())
                        .status(exam.isPendingRemoval() ? Status.REMOVED
                                : exam.getPublishedVersionId() == null ? Status.ADDED
                                : examSnapshotService.differsFromPublished(exam.getId()) ? Status.MODIFIED
                                : Status.UNCHANGED)
                        .build())
                .toList();

        CourseChangesResponse.CourseChangesResponseBuilder response = CourseChangesResponse.builder()
                .lessons(lessons)
                .exams(exams)
                .hasChanges(lessons.stream().anyMatch(l -> l.getStatus() != Status.UNCHANGED)
                        || exams.stream().anyMatch(x -> x.getStatus() != Status.UNCHANGED));
        if (versionId != null) {
            versionRepository.findById(versionId).ifPresent(v -> response
                    .publishedVersionNo(v.getVersionNo())
                    .publishedAt(v.getCreatedAt()));
        }
        return response.build();
    }

    // =========================================================
    // SO SÁNH
    // =========================================================

    private LessonChange added(Lesson lesson, LessonSnapshot draft) {
        List<SectionChange> sections = draft.getSections().stream()
                .map(s -> SectionChange.builder()
                        .sectionId(s.getId()).title(s.getTitle()).sectionType(s.getSectionType())
                        .status(Status.ADDED)
                        .items(ItemsChange.builder().added(items(s).size()).build())
                        .build())
                .toList();
        return LessonChange.builder()
                .lessonId(lesson.getId())
                .title(lesson.getTitle())
                .orderIndex(lesson.getOrderIndex())
                .status(Status.ADDED)
                .sections(sections)
                .quiz(draft.getQuiz() != null
                        ? ItemsChange.builder().added(draft.getQuiz().getQuestions().size()).build() : null)
                .speakingChanged(!draft.getSpeaking().isEmpty())
                .build();
    }

    private LessonChange compare(Lesson lesson, LessonSnapshot draft, LessonSnapshot published,
                                 LessonOutlineResponse publishedLesson, boolean moved) {
        boolean infoChanged = !Objects.equals(lesson.getTitle(), publishedLesson.getTitle());

        Map<Long, LessonSectionResponse> publishedSections = published.getSections().stream()
                .collect(Collectors.toMap(LessonSectionResponse::getId, s -> s, (a, b) -> a, LinkedHashMap::new));
        List<SectionChange> sections = new ArrayList<>();
        for (LessonSectionResponse s : draft.getSections()) {
            LessonSectionResponse old = publishedSections.get(s.getId());
            if (old == null) {
                sections.add(SectionChange.builder()
                        .sectionId(s.getId()).title(s.getTitle()).sectionType(s.getSectionType())
                        .status(Status.ADDED)
                        .items(ItemsChange.builder().added(items(s).size()).build())
                        .build());
                continue;
            }
            ItemsChange items = diff(items(s), items(old));
            items.setMetaChanged(!Objects.equals(s.getTitle(), old.getTitle()));
            sections.add(SectionChange.builder()
                    .sectionId(s.getId()).title(s.getTitle()).sectionType(s.getSectionType())
                    .status(items.hasChanges() ? Status.MODIFIED : Status.UNCHANGED)
                    .items(items)
                    .build());
        }
        Set<Long> draftSectionIds = draft.getSections().stream().map(LessonSectionResponse::getId).collect(Collectors.toSet());
        publishedSections.values().stream()
                .filter(s -> !draftSectionIds.contains(s.getId()))
                .forEach(s -> sections.add(SectionChange.builder()
                        .sectionId(s.getId()).title(s.getTitle()).sectionType(s.getSectionType())
                        .status(Status.REMOVED)
                        .items(ItemsChange.builder().removed(items(s).size()).build())
                        .build()));
        boolean sectionsReordered = !movedIds(
                draft.getSections().stream().map(LessonSectionResponse::getId).toList(),
                published.getSections().stream().map(LessonSectionResponse::getId).toList()).isEmpty();

        ItemsChange quiz = diffQuiz(draft.getQuiz(), published.getQuiz());
        boolean speakingChanged = !normalize(draft.getSpeaking()).equals(normalize(published.getSpeaking()));

        boolean modified = infoChanged || moved || sectionsReordered || speakingChanged
                || sections.stream().anyMatch(s -> s.getStatus() != Status.UNCHANGED)
                || (quiz != null && quiz.hasChanges());
        return LessonChange.builder()
                .lessonId(lesson.getId())
                .title(lesson.getTitle())
                .orderIndex(lesson.getOrderIndex())
                .publishedOrderIndex(publishedLesson.getOrderIndex())
                .status(modified ? Status.MODIFIED : Status.UNCHANGED)
                .infoChanged(infoChanged)
                .moved(moved || sectionsReordered)
                .sections(sections)
                .quiz(quiz)
                .speakingChanged(speakingChanged)
                .build();
    }

    private ItemsChange diffQuiz(QuizSnapshot draft, QuizSnapshot published) {
        if (draft == null && published == null) {
            return null;
        }
        if (published == null) {
            return ItemsChange.builder().added(draft.getQuestions().size()).build();
        }
        if (draft == null) {
            return ItemsChange.builder().removed(published.getQuestions().size()).build();
        }
        ItemsChange change = diff(draft.getQuestions(), published.getQuestions());
        change.setMetaChanged(!Objects.equals(draft.getTitle(), published.getTitle())
                || !Objects.equals(draft.getDescription(), published.getDescription())
                || !Objects.equals(draft.getPassingScorePercent(), published.getPassingScorePercent()));
        return change;
    }

    /** So 2 danh sách phần tử có trường "id": thêm / bỏ / sửa nội dung / đổi thứ tự. */
    private ItemsChange diff(Collection<?> draftItems, Collection<?> publishedItems) {
        Map<Long, JsonNode> draft = byId(draftItems);
        Map<Long, JsonNode> published = byId(publishedItems);
        int added = 0;
        int modified = 0;
        for (Map.Entry<Long, JsonNode> e : draft.entrySet()) {
            JsonNode old = published.get(e.getKey());
            if (old == null) {
                added++;
            } else if (!old.equals(e.getValue())) {
                modified++;
            }
        }
        int removed = (int) published.keySet().stream().filter(id -> !draft.containsKey(id)).count();
        boolean reordered = !movedIds(new ArrayList<>(draft.keySet()), new ArrayList<>(published.keySet())).isEmpty();
        return ItemsChange.builder().added(added).removed(removed).modified(modified).reordered(reordered).build();
    }

    private Map<Long, JsonNode> byId(Collection<?> items) {
        Map<Long, JsonNode> result = new LinkedHashMap<>();
        if (items == null) {
            return result;
        }
        for (Object item : items) {
            JsonNode node = normalize(item);
            if (node.hasNonNull("id")) {
                result.put(node.get("id").asLong(), node);
            }
        }
        return result;
    }

    private static List<?> items(LessonSectionResponse s) {
        if (s.getSectionType() == null) {
            return List.of();
        }
        List<?> list = switch (s.getSectionType()) {
            case KANJI -> s.getKanjiItems();
            case VOCABULARY -> s.getVocabularyItems();
            case GRAMMAR -> s.getGrammarItems();
        };
        return list != null ? list : List.of();
    }

    /**
     * Id phần tử đổi vị trí tương đối: chỉ xét phần tử có ở cả 2 bản, nên thêm / bỏ một phần tử
     * không làm mọi phần tử sau nó bị coi là "đổi thứ tự".
     */
    static Set<Long> movedIds(List<Long> draftOrder, List<Long> publishedOrder) {
        Set<Long> common = draftOrder.stream().filter(publishedOrder::contains).collect(Collectors.toSet());
        List<Long> a = draftOrder.stream().filter(common::contains).toList();
        List<Long> b = publishedOrder.stream().filter(common::contains).toList();
        Set<Long> moved = new java.util.HashSet<>();
        for (int i = 0; i < a.size(); i++) {
            if (!a.get(i).equals(b.get(i))) {
                moved.add(a.get(i));
            }
        }
        return moved;
    }

    /** Cây JSON bỏ các trường kỹ thuật (version, orderIndex) để so nội dung. */
    private JsonNode normalize(Object value) {
        JsonNode node = objectMapper.valueToTree(value);
        strip(node);
        return node;
    }

    private static void strip(JsonNode node) {
        if (node instanceof ObjectNode obj) {
            obj.remove(IGNORED_FIELDS);
            obj.elements().forEachRemaining(CourseChangesService::strip);
        } else if (node instanceof ArrayNode arr) {
            arr.forEach(CourseChangesService::strip);
        }
    }
}
