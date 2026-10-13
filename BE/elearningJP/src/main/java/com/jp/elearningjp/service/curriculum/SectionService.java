package com.jp.elearningjp.service.curriculum;

import com.jp.elearningjp.dto.response.curriculum.LessonSectionResponse;
import com.jp.elearningjp.entity.content.Vocabulary;
import com.jp.elearningjp.entity.curriculum.Lesson;
import com.jp.elearningjp.entity.curriculum.LessonKanjiItem;
import com.jp.elearningjp.entity.curriculum.LessonKanjiRelatedVocab;
import com.jp.elearningjp.entity.curriculum.LessonSection;
import com.jp.elearningjp.exception.AppException;
import com.jp.elearningjp.exception.ErrorCode;
import com.jp.elearningjp.mapper.LessonSectionMapper;
import com.jp.elearningjp.repository.content.VocabularyCharacterRepository;
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
import com.jp.elearningjp.shared.util.OrderIndexResolver;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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
     * Tạo section mới trong lesson. orderIndex = max + 1.
     */
    @Transactional
    public LessonSectionResponse createSection(Long lessonId, LessonSectionType sectionType, String title) {
        Lesson lesson = lessonRepository.findByIdAndDeletedFalse(lessonId)
                .orElseThrow(() -> new AppException(ErrorCode.LESSON_NOT_FOUND));

        List<LessonSection> existing = lessonSectionRepository
                .findByLessonIdAndDeletedFalseOrderByOrderIndexAsc(lessonId);
        int newOrderIndex = OrderIndexResolver.maxOrderIndex(existing, LessonSection::getOrderIndex) + 1;

        // Check race condition (hiếm nhưng vẫn nên có)
        if (lessonSectionRepository.existsByLessonIdAndOrderIndexExcludingId(lessonId, newOrderIndex, null)) {
            log.warn("[SECTION] OrderIndex conflict detected at createSection for lesson id={}, orderIndex={}",
                    lessonId, newOrderIndex);
            throw new AppException(ErrorCode.LESSON_ORDER_CONFLICT);
        }

        LessonSection section = LessonSection.builder()
                .lesson(lesson)
                .sectionType(sectionType)
                .title(title)
                .orderIndex(newOrderIndex)
                .build();

        LessonSection saved = lessonSectionRepository.save(section);
        log.info("[SECTION] Created section id={}, type={}, orderIndex={} in lesson id={}",
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
            int currentMax = OrderIndexResolver.maxOrderIndex(
                    lessonSectionRepository.findByLessonIdAndDeletedFalseOrderByOrderIndexAsc(lessonId),
                    LessonSection::getOrderIndex
            );
            int target = OrderIndexResolver.clampToMaxPlusOne(orderIndex, () -> currentMax);

            if (target != section.getOrderIndex()) {
                int oldOrderIndex = section.getOrderIndex();
                // Marker tạm để tránh UNIQUE conflict khi shift
                section.setOrderIndex(-oldOrderIndex);

                if (target < oldOrderIndex) {
                    // Move up: shift [target, oldOrderIndex-1] lên +1
                    int shifted = lessonSectionRepository.shiftOrderIndexesUp(lessonId, target, sectionId);
                    log.info("[SECTION] Move-up: shifted {} section(s) in lesson id={}", shifted, lessonId);
                } else if (target <= currentMax) {
                    // Move down: shift (oldOrderIndex, target] xuống -1
                    int shifted = lessonSectionRepository.shiftOrderIndexesDownInRange(
                            lessonId, oldOrderIndex, target, sectionId);
                    log.info("[SECTION] Move-down: shifted {} section(s) in lesson id={}", shifted, lessonId);
                }
                section.setOrderIndex(target);
            }

            // Check race condition
            if (lessonSectionRepository.existsByLessonIdAndOrderIndexExcludingId(
                    section.getLesson().getId(), section.getOrderIndex(), sectionId)) {
                log.warn("[SECTION] OrderIndex conflict detected at updateSection for section id={}, orderIndex={}",
                        sectionId, section.getOrderIndex());
                throw new AppException(ErrorCode.LESSON_ORDER_CONFLICT);
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
