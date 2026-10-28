package com.jp.elearningjp.service.curriculum;

import com.jp.elearningjp.dto.request.curriculum.ItemBulkReorderRequest;
import com.jp.elearningjp.dto.response.curriculum.LessonSectionResponse;
import com.jp.elearningjp.entity.content.Vocabulary;
import com.jp.elearningjp.entity.curriculum.Course;
import com.jp.elearningjp.entity.curriculum.Lesson;
import com.jp.elearningjp.entity.curriculum.LessonKanjiItem;
import com.jp.elearningjp.entity.curriculum.LessonKanjiRelatedVocab;
import com.jp.elearningjp.entity.curriculum.LessonSection;
import com.jp.elearningjp.exception.AppException;
import com.jp.elearningjp.exception.ErrorCode;
import com.jp.elearningjp.mapper.LessonSectionMapper;
import com.jp.elearningjp.repository.content.VocabularyCharacterRepository;
import com.jp.elearningjp.repository.curriculum.CourseRepository;
import com.jp.elearningjp.repository.curriculum.LessonGrammarExampleRepository;
import com.jp.elearningjp.repository.curriculum.LessonGrammarItemRepository;
import com.jp.elearningjp.repository.curriculum.LessonKanjiItemRepository;
import com.jp.elearningjp.repository.curriculum.LessonKanjiRelatedVocabRepository;
import com.jp.elearningjp.repository.curriculum.LessonRepository;
import com.jp.elearningjp.repository.curriculum.LessonSectionRepository;
import com.jp.elearningjp.repository.curriculum.LessonVocabularyExampleRepository;
import com.jp.elearningjp.repository.curriculum.LessonVocabularyItemRepository;
import com.jp.elearningjp.repository.curriculum.QuizOptionRepository;
import com.jp.elearningjp.repository.curriculum.QuizRepository;
import com.jp.elearningjp.shared.enums.LessonSectionType;
import com.jp.elearningjp.shared.util.CourseAccessGuard;
import com.jp.elearningjp.shared.util.OrderIndexResolver;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service quản lý {@link LessonSection} — nhóm nội dung trong một bài học.
 * <p>Tách ra từ {@code LessonService} để giảm tải cho lesson service và tập trung
 * xử lý logic riêng của section (CRUD, orderIndex, batch fetch nested collections).</p>
 *
 * <p>Phạm vi:</p>
 * <ul>
 *   <li>Tạo / cập nhật / xoá mềm section.</li>
 *   <li>Load section + các collection lồng nhau (kanji, vocab, grammar, examples, ...).</li>
 * </ul>
 *
 * <p><b>SAU REFACTOR (V20/V21):</b> Section TEST đã bị loại bỏ. Nếu client
 * gửi {@code sectionType = TEST}, Jackson sẽ tự reject vì giá trị này
 * không tồn tại trong {@link LessonSectionType} nữa.</p>
 */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class SectionService {

    LessonSectionRepository lessonSectionRepository;
    LessonRepository lessonRepository;
    CourseRepository courseRepository;
    CourseAccessGuard courseAccessGuard;
    LessonKanjiItemRepository kanjiItemRepository;
    LessonVocabularyItemRepository vocabularyItemRepository;
    LessonGrammarItemRepository grammarItemRepository;
    VocabularyCharacterRepository vocabularyCharacterRepository;
    LessonGrammarExampleRepository grammarExampleRepository;
    LessonVocabularyExampleRepository vocabularyExampleRepository;
    LessonKanjiRelatedVocabRepository kanjiRelatedVocabRepository;
    QuizRepository quizRepository;
    QuizOptionRepository quizOptionRepository;
    LessonSectionMapper lessonSectionMapper;

    // ============================================================
    // READ
    // ============================================================

    /**
     * Lấy danh sách sections của 1 lesson (kèm items + related vocab derive tự động).
     */
    @Transactional(readOnly = true)
    public List<LessonSectionResponse> getLessonSections(Long lessonId) {
        if (lessonRepository.findByIdAndDeletedFalse(lessonId).isEmpty()) {
            throw new AppException(ErrorCode.LESSON_NOT_FOUND);
        }
        List<LessonSection> sections = lessonSectionRepository.findByLessonIdWithAllItems(lessonId);
        RelatedVocabBundle bundle = loadNestedCollections(sections);
        return lessonSectionMapper.toSectionResponses(
                sections, bundle.relatedVocabsByCharacter, bundle.teacherRelatedVocabsByKanjiItem);
    }

    /**
     * Lấy chi tiết 1 section (kèm items + related vocab).
     */
    @Transactional(readOnly = true)
    public LessonSectionResponse getSection(Long sectionId) {
        LessonSection section = lessonSectionRepository.findById(sectionId)
                .orElseThrow(() -> new AppException(ErrorCode.LESSON_SECTION_NOT_FOUND));
        if (section.isDeleted()) {
            throw new AppException(ErrorCode.LESSON_SECTION_NOT_FOUND);
        }
        // Load full graph theo lesson
        List<LessonSection> sections = lessonSectionRepository
                .findByLessonIdWithAllItems(section.getLesson().getId());
        RelatedVocabBundle bundle = loadNestedCollections(sections);
        return sections.stream()
                .filter(s -> s.getId().equals(sectionId))
                .findFirst()
                .map(s -> lessonSectionMapper.toSectionResponse(
                        s, bundle.relatedVocabsByCharacter, bundle.teacherRelatedVocabsByKanjiItem))
                .orElseThrow(() -> new AppException(ErrorCode.LESSON_SECTION_NOT_FOUND));
    }

    // ============================================================
    // CREATE
    // ============================================================

    /**
     * Tạo section mới trong lesson.
     *
     * <p>{@code requestedOrderIndex}:</p>
     * <ul>
     *   <li>Null hoặc {@code <= 0} → gán tự động = MAX + 1 (append cuối).</li>
     *   <li>1..MAX+1 → insert vào slot cụ thể, dời các section có index ≥ target lên +1.</li>
     *   <li>&gt; MAX+1 → clamp về MAX + 1 (giống pattern của Lesson).</li>
     * </ul>
     */
    @Transactional
    public LessonSectionResponse createSection(Long lessonId, LessonSectionType sectionType, String title,
                                               Integer requestedOrderIndex) {
        Lesson lesson = lessonRepository.findByIdAndDeletedFalse(lessonId)
                .orElseThrow(() -> new AppException(ErrorCode.LESSON_NOT_FOUND));

        // SEQUENTIAL RENUMBER: chèn section vào vị trí đích trong bộ nhớ, rồi
        // đánh số lại 1..N và ghi an toàn (marker âm → 1..N).
        List<LessonSection> ordered = new ArrayList<>(
                lessonSectionRepository.findByLessonIdAndDeletedFalseOrderByOrderIndexAsc(lessonId));

        LessonSection section = LessonSection.builder()
                .lesson(lesson)
                .sectionType(sectionType)
                .title(title)
                .orderIndex(ordered.size() + 1) // tạm — sẽ gán lại chính xác khi persist
                .build();

        LessonSection saved = lessonSectionRepository.save(section);
        lessonSectionRepository.flush();

        int target = (requestedOrderIndex != null && requestedOrderIndex > 0)
                ? Math.min(requestedOrderIndex, ordered.size() + 1)
                : ordered.size() + 1;
        ordered.add(target - 1, saved);

        OrderIndexResolver.persistSequential(
                ordered,
                LessonSection::getId,
                LessonSection::setOrderIndex,
                lessonSectionRepository::flush
        );
        log.info("[SECTION] Created section id={}, type={} at orderIndex={} in lesson id={}",
                saved.getId(), sectionType, saved.getOrderIndex(), lessonId);

        return lessonSectionMapper.toSectionResponse(saved);
    }

    // ============================================================
    // UPDATE
    // ============================================================

    /**
     * Cập nhật title / orderIndex của section.
     * Nếu client đổi orderIndex sang giá trị khác → shift các section khác
     * (giống logic của lesson).
     */
    @Transactional
    public LessonSectionResponse updateSection(Long sectionId, String title, Integer orderIndex) {
        LessonSection section = lessonSectionRepository.findById(sectionId)
                .orElseThrow(() -> new AppException(ErrorCode.LESSON_SECTION_NOT_FOUND));
        if (section.isDeleted()) {
            throw new AppException(ErrorCode.LESSON_SECTION_NOT_FOUND);
        }

        if (title != null) {
            section.setTitle(title);
        }

        if (orderIndex != null && orderIndex > 0 && !orderIndex.equals(section.getOrderIndex())) {
            Long lessonId = section.getLesson().getId();
            List<LessonSection> ordered = new ArrayList<>(
                    lessonSectionRepository.findByLessonIdAndDeletedFalseOrderByOrderIndexAsc(lessonId));

            boolean moved = OrderIndexResolver.moveToPosition(
                    ordered,
                    section,
                    orderIndex,
                    LessonSection::setOrderIndex
            );

            if (moved) {
                log.info("[SECTION] Moving section id={} to orderIndex={} (lessonId={})",
                        sectionId, orderIndex, lessonId);
                OrderIndexResolver.persistSequential(
                        ordered,
                        LessonSection::getId,
                        LessonSection::setOrderIndex,
                        lessonSectionRepository::flush
                );
            }
        }

        lessonSectionRepository.save(section);
        log.info("[SECTION] Updated section id={}, orderIndex={}", sectionId, section.getOrderIndex());

        return lessonSectionMapper.toSectionResponse(section);
    }

    // ============================================================
    // DELETE
    // ============================================================

    /**
     * Xoá mềm section (idempotent).
     */
    @Transactional
    public void deleteSection(Long sectionId) {
        LessonSection section = lessonSectionRepository.findById(sectionId)
                .orElseThrow(() -> new AppException(ErrorCode.LESSON_SECTION_NOT_FOUND));
        if (section.isDeleted()) {
            return; // đã xoá rồi, idempotent
        }
        section.softDelete();
        lessonSectionRepository.save(section);
        log.info("[SECTION] Soft-deleted section id={}", sectionId);
    }

    /**
     * Sắp xếp lại thứ tự hàng loạt các section trong 1 lesson (drag-drop UI).
     *
     * <p>Quy tắc: list {@code itemIds} phải chứa <b>đúng tất cả</b> section active
     * của lesson hiện tại (không thiếu, không thừa, không trùng).</p>
     *
     * <p>Quyền: Admin hoặc Teacher (chỉ reorder section trong course do mình tạo).</p>
     *
     * @param lessonId lesson cần reorder
     * @param request  danh sách section ID theo thứ tự MỚI
     * @return danh sách section sau khi reorder (theo thứ tự mới)
     */
    @Transactional
    public List<LessonSectionResponse> reorderSections(Long lessonId, ItemBulkReorderRequest request) {
        // Auth check
        Course course = courseRepository.findByIdAndDeletedFalse(
                lessonRepository.findByIdAndDeletedFalse(lessonId)
                        .orElseThrow(() -> new AppException(ErrorCode.LESSON_NOT_FOUND))
                        .getCourse().getId()
        ).orElseThrow(() -> new AppException(ErrorCode.COURSE_NOT_FOUND));
        courseAccessGuard.requireOwnerOrAdmin(course, ErrorCode.FORBIDDEN_LESSON_ACCESS);

        if (request.getItemIds() == null || request.getItemIds().isEmpty()) {
            throw new AppException(ErrorCode.LESSON_REORDER_LIST_EMPTY);
        }

        // Load current sections
        List<LessonSection> currentSections = lessonSectionRepository
                .findByLessonIdAndDeletedFalseOrderByOrderIndexAsc(lessonId);
        Set<Long> currentIds = new HashSet<>();
        for (LessonSection s : currentSections) currentIds.add(s.getId());

        // Validate
        Set<Long> requestedIds = new HashSet<>(request.getItemIds());
        if (requestedIds.size() != request.getItemIds().size()) {
            throw new AppException(ErrorCode.LESSON_REORDER_LIST_INVALID);
        }
        if (!requestedIds.equals(currentIds)) {
            throw new AppException(ErrorCode.LESSON_REORDER_LIST_MISMATCH);
        }

        // Dùng helper generic để gán lại orderIndex theo list mới (1..N)
        OrderIndexResolver.bulkReorder(
                currentSections,
                request.getItemIds(),
                LessonSection::getId,
                LessonSection::setOrderIndex
        );

        // Ghi an toàn 2 pha (marker âm → 1..N), tránh UNIQUE conflict
        OrderIndexResolver.persistSequential(
                currentSections,
                LessonSection::getId,
                LessonSection::setOrderIndex,
                lessonSectionRepository::flush
        );

        log.info("[SECTION] Bulk reordered {} section(s) in lesson id={}", currentSections.size(), lessonId);
        return currentSections.stream()
                .map(lessonSectionMapper::toSectionResponse)
                .toList();
    }

    // ============================================================
    // HELPER
    // ============================================================

    /**
     * Load các collection (kanjiItems, vocabularyItems, grammarItems, quizzes + các
     * collection bên trong: examples, relatedVocabulary, questions, options) theo
     * từng query riêng biệt. Mỗi query chỉ fetch tối đa 1 collection List<>, nên
     * KHÔNG BAO GIỜ trigger MultipleBagFetchException.
     *
     * <p>V25+: Trả về cả related vocab do giáo viên chọn
     * ({@code lesson_kanji_related_vocabulary}) và derived vocab từ từ điển
     * ({@code vocabulary_characters}). Mapper sẽ ưu tiên teacher override,
     * fallback auto-derive.</p>
     *
     * @return {@link RelatedVocabBundle} chứa cả 2 map. Có thể empty nếu lesson
     *         không có kanji items.
     */
    public RelatedVocabBundle loadNestedCollections(List<LessonSection> sections) {
        if (sections == null || sections.isEmpty()) {
            return RelatedVocabBundle.empty();
        }
        Long lessonId = sections.get(0).getLesson().getId();
        List<Long> sectionIds = sections.stream().map(LessonSection::getId).toList();

        // === Level 1: 4 collection trên LessonSection — mỗi query 1 collection List<> ===
        kanjiItemRepository.findByLessonIdWithCharacter(lessonId);
        vocabularyItemRepository.findByLessonIdWithVocab(lessonId);
        grammarItemRepository.findByLessonIdWithGrammar(lessonId);
        quizRepository.findByLessonIdWithQuestions(lessonId);

        // === Level 2: collection bên trong item — mỗi query 1 collection List<> ===
        grammarExampleRepository.findBySectionIds(sectionIds);
        vocabularyExampleRepository.findBySectionIds(sectionIds);
        quizOptionRepository.findByLessonId(lessonId);

        // === Related vocab cho kanji items ===
        // 1) Teacher override từ bảng lesson_kanji_related_vocabulary (group theo kanji item id)
        Map<Long, List<LessonKanjiRelatedVocab>> teacherRvsByKanjiItem =
                loadTeacherRelatedVocabsForKanjiItems(sections);
        // 2) Auto-derive từ bảng vocabulary_characters (group theo character id)
        Map<Long, List<Vocabulary>> relatedVocabsByCharacter = loadRelatedVocabsForKanjiItems(sections);

        // Hibernate persistence context tự merge các collection này vào entity đã load.
        return new RelatedVocabBundle(relatedVocabsByCharacter, teacherRvsByKanjiItem);
    }

    /**
     * Query batch teacher-selected related vocab cho tất cả kanji items trong các section của 1 lesson.
     * Group theo {@code kanjiItemId} để mapper tra cứu O(1).
     */
    private Map<Long, List<LessonKanjiRelatedVocab>> loadTeacherRelatedVocabsForKanjiItems(List<LessonSection> sections) {
        List<Long> kanjiItemIds = collectActiveKanjiItemIds(sections);
        if (kanjiItemIds.isEmpty()) {
            return Map.of();
        }
        List<LessonKanjiRelatedVocab> rows =
                kanjiRelatedVocabRepository.findByKanjiItemIdsWithVocab(kanjiItemIds);
        return rows.stream().collect(Collectors.groupingBy(rv -> rv.getKanjiItem().getId()));
    }

    /**
     * Thu thập danh sách kanji item id (chưa xoá) trong các section của 1 lesson.
     */
    private List<Long> collectActiveKanjiItemIds(List<LessonSection> sections) {
        List<Long> result = new ArrayList<>();
        for (LessonSection s : sections) {
            if (s.isDeleted() || s.getSectionType() != LessonSectionType.KANJI) continue;
            if (s.getKanjiItems() == null) continue;
            for (LessonKanjiItem ki : s.getKanjiItems()) {
                if (!ki.isDeleted()) {
                    result.add(ki.getId());
                }
            }
        }
        return result;
    }

    /**
     * Bundle chứa 2 map related vocab (auto-derive + teacher override) để mapper xử lý ưu tiên.
     */
    public record RelatedVocabBundle(
            Map<Long, List<Vocabulary>> relatedVocabsByCharacter,
            Map<Long, List<LessonKanjiRelatedVocab>> teacherRelatedVocabsByKanjiItem
    ) {
        public static RelatedVocabBundle empty() {
            return new RelatedVocabBundle(Map.of(), Map.of());
        }
    }

    /**
     * Query batch related vocab cho tất cả kanji items trong các section của 1 lesson.
     * Gom theo {@code characterId} để mapper tra cứu O(1).
     */
    private Map<Long, List<Vocabulary>> loadRelatedVocabsForKanjiItems(List<LessonSection> sections) {
        // Lấy tất cả characterId của kanji items trong sections (không bị xoá, có character != null).
        List<Long> characterIds = sections.stream()
                .filter(s -> s.getSectionType() == LessonSectionType.KANJI)
                .filter(s -> !s.isDeleted())
                .flatMap(s -> s.getKanjiItems() == null ? java.util.stream.Stream.empty() : s.getKanjiItems().stream())
                .filter(i -> !i.isDeleted())
                .map(i -> i.getCharacter() != null ? i.getCharacter().getId() : null)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();

        if (characterIds.isEmpty()) {
            return Map.of();
        }

        // Query batch 1 lần — tránh N+1
        List<VocabularyCharacterRepository.CharacterIdVocabularyProjection> rows =
                vocabularyCharacterRepository.findRelatedVocabulariesByCharacterIds(characterIds);

        // Group theo characterId. KHÔNG distinct theo vocabularyId ở đây
        // (1 vocab có thể chứa cùng 1 chữ Hán ở nhiều position — vẫn hợp lệ,
        // nhưng response không cần trùng — nên distinct theo vocabId).
        return rows.stream()
                .collect(Collectors.groupingBy(
                        VocabularyCharacterRepository.CharacterIdVocabularyProjection::getCharacterId,
                        Collectors.mapping(
                                row -> row.getV(),
                                Collectors.filtering(
                                        v -> v != null,
                                        Collectors.collectingAndThen(
                                                Collectors.toMap(Vocabulary::getId, v -> v, (a, b) -> a),
                                                m -> m.values().stream().toList()
                                        )
                                )
                        )
                ));
    }
}
