package com.jp.elearningjp.service.curriculum;

import com.jp.elearningjp.dto.request.curriculum.*;
import com.jp.elearningjp.dto.response.curriculum.LessonGrammarExampleResponse;
import com.jp.elearningjp.dto.response.curriculum.LessonGrammarItemResponse;
import com.jp.elearningjp.dto.response.curriculum.LessonKanjiItemResponse;
import com.jp.elearningjp.dto.response.curriculum.LessonRelatedVocabResponse;
import com.jp.elearningjp.dto.response.curriculum.LessonVocabularyExampleResponse;
import com.jp.elearningjp.dto.response.curriculum.LessonVocabularyItemResponse;
import com.jp.elearningjp.entity.content.GrammarPoint;
import com.jp.elearningjp.entity.content.Vocabulary;
import com.jp.elearningjp.entity.content.WritingCharacter;
import com.jp.elearningjp.entity.curriculum.LessonGrammarExample;
import com.jp.elearningjp.entity.curriculum.LessonGrammarItem;
import com.jp.elearningjp.entity.curriculum.LessonKanjiItem;
import com.jp.elearningjp.entity.curriculum.LessonKanjiRelatedVocab;
import com.jp.elearningjp.entity.curriculum.LessonSection;
import com.jp.elearningjp.entity.curriculum.LessonVocabularyExample;
import com.jp.elearningjp.entity.curriculum.LessonVocabularyItem;
import com.jp.elearningjp.exception.AppException;
import com.jp.elearningjp.exception.ErrorCode;
import com.jp.elearningjp.mapper.LessonSectionMapper;
import com.jp.elearningjp.repository.content.GrammarPointRepository;
import com.jp.elearningjp.repository.content.VocabularyRepository;
import com.jp.elearningjp.repository.content.WritingCharacterRepository;
import com.jp.elearningjp.repository.curriculum.LessonGrammarExampleRepository;
import com.jp.elearningjp.repository.curriculum.LessonGrammarItemRepository;
import com.jp.elearningjp.repository.curriculum.LessonKanjiItemRepository;
import com.jp.elearningjp.repository.curriculum.LessonKanjiRelatedVocabRepository;
import com.jp.elearningjp.repository.curriculum.LessonSectionRepository;
import com.jp.elearningjp.repository.curriculum.LessonVocabularyExampleRepository;
import com.jp.elearningjp.repository.curriculum.LessonVocabularyItemRepository;
import com.jp.elearningjp.shared.enums.LessonSectionType;
import com.jp.elearningjp.shared.util.OrderIndexResolver;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service quản lý các item bên trong 1 section:
 * - Kanji items + related vocabulary
 * - Vocabulary items + examples
 * - Grammar items + examples
 */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class LessonItemService {

    // Repositories
    LessonSectionRepository lessonSectionRepository;
    LessonKanjiItemRepository lessonKanjiItemRepository;
    LessonKanjiRelatedVocabRepository lessonKanjiRelatedVocabRepository;
    LessonVocabularyItemRepository lessonVocabularyItemRepository;
    LessonVocabularyExampleRepository lessonVocabularyExampleRepository;
    LessonGrammarItemRepository lessonGrammarItemRepository;
    LessonGrammarExampleRepository lessonGrammarExampleRepository;

    // Dictionary repos
    WritingCharacterRepository writingCharacterRepository;
    VocabularyRepository vocabularyRepository;
    GrammarPointRepository grammarPointRepository;

    // Mapper
    LessonSectionMapper lessonSectionMapper;

    // ============================================================
    // SECTION helpers
    // ============================================================
    private LessonSection getActiveSection(Long sectionId, LessonSectionType expectedType) {
        LessonSection section = lessonSectionRepository.findById(sectionId)
                .orElseThrow(() -> new AppException(ErrorCode.LESSON_SECTION_NOT_FOUND));
        if (section.isDeleted()) {
            throw new AppException(ErrorCode.LESSON_SECTION_NOT_FOUND);
        }
        if (expectedType != null && section.getSectionType() != expectedType) {
            throw new AppException(ErrorCode.LESSON_SECTION_TYPE_INVALID);
        }
        return section;
    }

    private int nextOrderIndex(Long sectionId, int currentMax) {
        return currentMax + 1;
    }

    // ============================================================
    // Bước 6: Helper chung cho check duplicate (tránh lạm dụng INVALID_SRS_REQUEST)
    // ============================================================
    private void assertNoDuplicateKanji(Long sectionId, Long characterId) {
        boolean exists = lessonKanjiItemRepository
                .findBySectionIdAndDeletedFalseOrderByOrderIndexAsc(sectionId)
                .stream()
                .anyMatch(i -> !i.isDeleted()
                        && i.getCharacter() != null
                        && i.getCharacter().getId().equals(characterId));
        if (exists) {
            throw new AppException(ErrorCode.LESSON_KANJI_ITEM_DUPLICATED);
        }
    }

    private void assertNoDuplicateVocabulary(Long sectionId, Long vocabId) {
        boolean exists = lessonVocabularyItemRepository
                .findBySectionIdAndDeletedFalseOrderByOrderIndexAsc(sectionId)
                .stream()
                .anyMatch(i -> !i.isDeleted()
                        && i.getVocab() != null
                        && i.getVocab().getId().equals(vocabId));
        if (exists) {
            throw new AppException(ErrorCode.LESSON_VOCABULARY_ITEM_DUPLICATED);
        }
    }

    private void assertNoDuplicateGrammar(Long sectionId, Long grammarId) {
        boolean exists = lessonGrammarItemRepository
                .findBySectionIdAndDeletedFalseOrderByOrderIndexAsc(sectionId)
                .stream()
                .anyMatch(i -> !i.isDeleted()
                        && i.getGrammar() != null
                        && i.getGrammar().getId().equals(grammarId));
        if (exists) {
            throw new AppException(ErrorCode.LESSON_GRAMMAR_ITEM_DUPLICATED);
        }
    }

    private void assertNoDuplicateRelatedVocab(LessonKanjiItem kanjiItem, Long vocabId) {
        if (kanjiItem.getRelatedVocabulary() == null) {
            return;
        }
        boolean exists = kanjiItem.getRelatedVocabulary().stream()
                .anyMatch(rv -> !rv.isDeleted()
                        && rv.getVocab() != null
                        && rv.getVocab().getId().equals(vocabId));
        if (exists) {
            throw new AppException(ErrorCode.LESSON_KANJI_RELATED_VOCAB_DUPLICATED);
        }
    }

    // ============================================================
    // KANJI ITEMS
    // ============================================================

    @Transactional
    public LessonKanjiItemResponse addKanjiItem(Long sectionId, LessonKanjiItemCreateRequest request) {
        LessonSection section = getActiveSection(sectionId, LessonSectionType.KANJI);

        WritingCharacter character = writingCharacterRepository.findByIdAndDeletedFalse(request.getCharacterId())
                .orElseThrow(() -> new AppException(ErrorCode.CHARACTER_NOT_FOUND));

        // Bước 6: check duplicate dùng ErrorCode riêng (trước đây lạm dụng INVALID_SRS_REQUEST)
        assertNoDuplicateKanji(sectionId, character.getId());

        Integer maxOrder = OrderIndexResolver.maxOrderIndex(
                lessonKanjiItemRepository.findBySectionIdAndDeletedFalseOrderByOrderIndexAsc(sectionId),
                LessonKanjiItem::getOrderIndex
        );

        LessonKanjiItem item = LessonKanjiItem.builder()
                .section(section)
                .character(character)
                .orderIndex(request.getOrderIndex() != null && request.getOrderIndex() > 0
                        ? request.getOrderIndex()
                        : OrderIndexResolver.resolveForCreate(null, () -> maxOrder))
                .note(request.getNote())
                .build();

        section.addKanjiItem(item);
        LessonKanjiItem saved = lessonKanjiItemRepository.save(item);
        log.info("[KANJI-ITEM] Added character id={} into section id={}", character.getId(), sectionId);

        return lessonSectionMapper.toKanjiItemResponse(saved);
    }

    @Transactional
    public LessonKanjiItemResponse updateKanjiItem(Long itemId, LessonKanjiItemUpdateRequest request) {
        LessonKanjiItem item = lessonKanjiItemRepository.findById(itemId)
                .orElseThrow(() -> new AppException(ErrorCode.LESSON_KANJI_ITEM_NOT_FOUND));
        if (item.isDeleted()) {
            throw new AppException(ErrorCode.LESSON_KANJI_ITEM_NOT_FOUND);
        }

        if (request.getOrderIndex() != null && request.getOrderIndex() > 0) {
            item.setOrderIndex(request.getOrderIndex());
        }
        if (request.getNote() != null) {
            item.setNote(request.getNote());
        }
        LessonKanjiItem saved = lessonKanjiItemRepository.save(item);
        log.info("[KANJI-ITEM] Updated kanji item id={}", itemId);
        return lessonSectionMapper.toKanjiItemResponse(saved);
    }

    @Transactional
    public void deleteKanjiItem(Long itemId) {
        LessonKanjiItem item = lessonKanjiItemRepository.findById(itemId)
                .orElseThrow(() -> new AppException(ErrorCode.LESSON_KANJI_ITEM_NOT_FOUND));
        if (item.isDeleted()) return; // idempotent
        item.softDelete();
        lessonKanjiItemRepository.save(item);
        log.info("[KANJI-ITEM] Soft-deleted kanji item id={}", itemId);
    }

    // ============================================================
    // KANJI → RELATED VOCABULARY
    // ============================================================

    @Transactional
    public LessonRelatedVocabResponse addRelatedVocab(Long kanjiItemId, LessonKanjiRelatedVocabRequest request) {
        LessonKanjiItem kanjiItem = lessonKanjiItemRepository.findById(kanjiItemId)
                .orElseThrow(() -> new AppException(ErrorCode.LESSON_KANJI_ITEM_NOT_FOUND));
        if (kanjiItem.isDeleted()) {
            throw new AppException(ErrorCode.LESSON_KANJI_ITEM_NOT_FOUND);
        }

        Vocabulary vocab = vocabularyRepository.findByIdAndDeletedFalse(request.getVocabId())
                .orElseThrow(() -> new AppException(ErrorCode.VOCAB_NOT_FOUND));

        // Bước 6: check duplicate dùng ErrorCode riêng
        assertNoDuplicateRelatedVocab(kanjiItem, vocab.getId());

        LessonKanjiRelatedVocab rv = LessonKanjiRelatedVocab.builder()
                .kanjiItem(kanjiItem)
                .vocab(vocab)
                .note(request.getNote())
                .build();

        kanjiItem.addRelatedVocab(rv);
        LessonKanjiRelatedVocab saved = lessonKanjiRelatedVocabRepository.save(rv);
        log.info("[RELATED-VOCAB] Added vocab id={} to kanji item id={}", vocab.getId(), kanjiItemId);

        return lessonSectionMapper.toRelatedVocabResponse(saved);
    }

    @Transactional
    public void deleteRelatedVocab(Long relatedVocabId) {
        LessonKanjiRelatedVocab rv = lessonKanjiRelatedVocabRepository.findById(relatedVocabId)
                .orElseThrow(() -> new AppException(ErrorCode.LESSON_KANJI_ITEM_NOT_FOUND));
        if (rv.isDeleted()) return; // idempotent
        rv.softDelete();
        lessonKanjiRelatedVocabRepository.save(rv);
        log.info("[RELATED-VOCAB] Soft-deleted related vocab id={}", relatedVocabId);
    }

    // ============================================================
    // VOCABULARY ITEMS
    // ============================================================

    @Transactional
    public LessonVocabularyItemResponse addVocabularyItem(Long sectionId, LessonVocabularyItemCreateRequest request) {
        LessonSection section = getActiveSection(sectionId, LessonSectionType.VOCABULARY);

        Vocabulary vocab = vocabularyRepository.findByIdAndDeletedFalse(request.getVocabId())
                .orElseThrow(() -> new AppException(ErrorCode.VOCAB_NOT_FOUND));

        // Bước 6: check duplicate dùng ErrorCode riêng
        assertNoDuplicateVocabulary(sectionId, vocab.getId());

        Integer maxOrder = OrderIndexResolver.maxOrderIndex(
                lessonVocabularyItemRepository.findBySectionIdAndDeletedFalseOrderByOrderIndexAsc(sectionId),
                LessonVocabularyItem::getOrderIndex
        );

        LessonVocabularyItem item = LessonVocabularyItem.builder()
                .section(section)
                .vocab(vocab)
                .orderIndex(request.getOrderIndex() != null && request.getOrderIndex() > 0
                        ? request.getOrderIndex()
                        : OrderIndexResolver.resolveForCreate(null, () -> maxOrder))
                .customHiragana(request.getCustomHiragana())
                .customMeaning(request.getCustomMeaning())
                .imageUrl(request.getImageUrl())
                .note(request.getNote())
                .build();

        section.addVocabularyItem(item);
        LessonVocabularyItem saved = lessonVocabularyItemRepository.save(item);
        log.info("[VOCAB-ITEM] Added vocab id={} into section id={}", vocab.getId(), sectionId);

        return lessonSectionMapper.toVocabularyItemResponse(saved);
    }

    @Transactional
    public LessonVocabularyItemResponse updateVocabularyItem(Long itemId, LessonVocabularyItemUpdateRequest request) {
        LessonVocabularyItem item = lessonVocabularyItemRepository.findById(itemId)
                .orElseThrow(() -> new AppException(ErrorCode.LESSON_VOCABULARY_ITEM_NOT_FOUND));
        if (item.isDeleted()) {
            throw new AppException(ErrorCode.LESSON_VOCABULARY_ITEM_NOT_FOUND);
        }

        if (request.getOrderIndex() != null && request.getOrderIndex() > 0) {
            item.setOrderIndex(request.getOrderIndex());
        }
        // Cho phép set null để xoá override
        if (request.getCustomHiragana() != null) {
            item.setCustomHiragana(request.getCustomHiragana().isEmpty() ? null : request.getCustomHiragana());
        }
        if (request.getCustomMeaning() != null) {
            item.setCustomMeaning(request.getCustomMeaning().isEmpty() ? null : request.getCustomMeaning());
        }
        if (request.getImageUrl() != null) {
            item.setImageUrl(request.getImageUrl().isEmpty() ? null : request.getImageUrl());
        }
        if (request.getNote() != null) {
            item.setNote(request.getNote());
        }

        LessonVocabularyItem saved = lessonVocabularyItemRepository.save(item);
        log.info("[VOCAB-ITEM] Updated vocab item id={}", itemId);
        return lessonSectionMapper.toVocabularyItemResponse(saved);
    }

    @Transactional
    public void deleteVocabularyItem(Long itemId) {
        LessonVocabularyItem item = lessonVocabularyItemRepository.findById(itemId)
                .orElseThrow(() -> new AppException(ErrorCode.LESSON_VOCABULARY_ITEM_NOT_FOUND));
        if (item.isDeleted()) return;
        item.softDelete();
        lessonVocabularyItemRepository.save(item);
        log.info("[VOCAB-ITEM] Soft-deleted vocab item id={}", itemId);
    }

    // ============================================================
    // VOCABULARY EXAMPLES
    // ============================================================

    @Transactional
    public LessonVocabularyExampleResponse addVocabularyExample(Long vocabItemId, LessonVocabularyExampleRequest request) {
        LessonVocabularyItem item = lessonVocabularyItemRepository.findById(vocabItemId)
                .orElseThrow(() -> new AppException(ErrorCode.LESSON_VOCABULARY_ITEM_NOT_FOUND));
        if (item.isDeleted()) {
            throw new AppException(ErrorCode.LESSON_VOCABULARY_ITEM_NOT_FOUND);
        }

        Integer maxOrder = item.getExamples() == null ? 0
                : item.getExamples().stream()
                .filter(e -> !e.isDeleted())
                .mapToInt(LessonVocabularyExample::getOrderIndex)
                .max().orElse(0);

        // Nếu isPrimary = true, tắt primary ở các example cũ
        boolean isPrimary = Boolean.TRUE.equals(request.getIsPrimary());
        if (isPrimary && item.getExamples() != null) {
            item.getExamples().forEach(e -> e.setPrimary(false));
        }

        LessonVocabularyExample ex = LessonVocabularyExample.builder()
                .vocabItem(item)
                .exampleJp(request.getExampleJp())
                .exampleVi(request.getExampleVi())
                .exampleRomaji(request.getExampleRomaji())
                .audioUrl(request.getAudioUrl())
                .orderIndex(request.getOrderIndex() != null ? request.getOrderIndex() : nextOrderIndex(vocabItemId, maxOrder))
                .primary(isPrimary)
                .build();

        item.addExample(ex);
        LessonVocabularyExample saved = lessonVocabularyExampleRepository.save(ex);
        log.info("[VOCAB-EXAMPLE] Added example id={} to vocab item id={}", saved.getId(), vocabItemId);

        return lessonSectionMapper.toVocabularyExampleResponse(saved);
    }

    @Transactional
    public LessonVocabularyExampleResponse updateVocabularyExample(Long exampleId, LessonVocabularyExampleRequest request) {
        LessonVocabularyExample ex = lessonVocabularyExampleRepository.findById(exampleId)
                .orElseThrow(() -> new AppException(ErrorCode.LESSON_VOCABULARY_ITEM_NOT_FOUND));
        if (ex.isDeleted()) {
            throw new AppException(ErrorCode.LESSON_VOCABULARY_ITEM_NOT_FOUND);
        }

        if (request.getExampleJp() != null) ex.setExampleJp(request.getExampleJp());
        if (request.getExampleVi() != null) ex.setExampleVi(request.getExampleVi());
        if (request.getExampleRomaji() != null) ex.setExampleRomaji(request.getExampleRomaji());
        if (request.getAudioUrl() != null) ex.setAudioUrl(request.getAudioUrl().isEmpty() ? null : request.getAudioUrl());
        if (request.getOrderIndex() != null && request.getOrderIndex() > 0) ex.setOrderIndex(request.getOrderIndex());

        if (Boolean.TRUE.equals(request.getIsPrimary())) {
            ex.setPrimary(true);
            // Tắt primary ở các example khác
            if (ex.getVocabItem() != null && ex.getVocabItem().getExamples() != null) {
                ex.getVocabItem().getExamples().forEach(e -> {
                    if (!e.getId().equals(ex.getId())) e.setPrimary(false);
                });
            }
        }

        LessonVocabularyExample saved = lessonVocabularyExampleRepository.save(ex);
        log.info("[VOCAB-EXAMPLE] Updated example id={}", exampleId);
        return lessonSectionMapper.toVocabularyExampleResponse(saved);
    }

    @Transactional
    public void deleteVocabularyExample(Long exampleId) {
        LessonVocabularyExample ex = lessonVocabularyExampleRepository.findById(exampleId)
                .orElseThrow(() -> new AppException(ErrorCode.LESSON_VOCABULARY_ITEM_NOT_FOUND));
        if (ex.isDeleted()) return;
        ex.softDelete();
        lessonVocabularyExampleRepository.save(ex);
        log.info("[VOCAB-EXAMPLE] Soft-deleted example id={}", exampleId);
    }

    // ============================================================
    // GRAMMAR ITEMS
    // ============================================================

    @Transactional
    public LessonGrammarItemResponse addGrammarItem(Long sectionId, LessonGrammarItemCreateRequest request) {
        LessonSection section = getActiveSection(sectionId, LessonSectionType.GRAMMAR);

        GrammarPoint grammar = grammarPointRepository.findByIdAndDeletedFalse(request.getGrammarId())
                .orElseThrow(() -> new AppException(ErrorCode.GRAMMAR_NOT_FOUND));

        // Bước 6: check duplicate dùng ErrorCode riêng
        assertNoDuplicateGrammar(sectionId, grammar.getId());

        Integer maxOrder = OrderIndexResolver.maxOrderIndex(
                lessonGrammarItemRepository.findBySectionIdAndDeletedFalseOrderByOrderIndexAsc(sectionId),
                LessonGrammarItem::getOrderIndex
        );

        LessonGrammarItem item = LessonGrammarItem.builder()
                .section(section)
                .grammar(grammar)
                .orderIndex(request.getOrderIndex() != null && request.getOrderIndex() > 0
                        ? request.getOrderIndex()
                        : OrderIndexResolver.resolveForCreate(null, () -> maxOrder))
                .customMeaning(request.getCustomMeaning())
                .customStructure(request.getCustomStructure())
                .note(request.getNote())
                .build();

        section.addGrammarItem(item);
        LessonGrammarItem saved = lessonGrammarItemRepository.save(item);
        log.info("[GRAMMAR-ITEM] Added grammar id={} into section id={}", grammar.getId(), sectionId);

        return lessonSectionMapper.toGrammarItemResponse(saved);
    }

    @Transactional
    public LessonGrammarItemResponse updateGrammarItem(Long itemId, LessonGrammarItemUpdateRequest request) {
        LessonGrammarItem item = lessonGrammarItemRepository.findById(itemId)
                .orElseThrow(() -> new AppException(ErrorCode.LESSON_GRAMMAR_ITEM_NOT_FOUND));
        if (item.isDeleted()) {
            throw new AppException(ErrorCode.LESSON_GRAMMAR_ITEM_NOT_FOUND);
        }

        if (request.getOrderIndex() != null && request.getOrderIndex() > 0) {
            item.setOrderIndex(request.getOrderIndex());
        }
        if (request.getCustomMeaning() != null) {
            item.setCustomMeaning(request.getCustomMeaning().isEmpty() ? null : request.getCustomMeaning());
        }
        if (request.getCustomStructure() != null) {
            item.setCustomStructure(request.getCustomStructure().isEmpty() ? null : request.getCustomStructure());
        }
        if (request.getNote() != null) {
            item.setNote(request.getNote());
        }

        LessonGrammarItem saved = lessonGrammarItemRepository.save(item);
        log.info("[GRAMMAR-ITEM] Updated grammar item id={}", itemId);
        return lessonSectionMapper.toGrammarItemResponse(saved);
    }

    @Transactional
    public void deleteGrammarItem(Long itemId) {
        LessonGrammarItem item = lessonGrammarItemRepository.findById(itemId)
                .orElseThrow(() -> new AppException(ErrorCode.LESSON_GRAMMAR_ITEM_NOT_FOUND));
        if (item.isDeleted()) return;
        item.softDelete();
        lessonGrammarItemRepository.save(item);
        log.info("[GRAMMAR-ITEM] Soft-deleted grammar item id={}", itemId);
    }

    // ============================================================
    // GRAMMAR EXAMPLES
    // ============================================================

    @Transactional
    public LessonGrammarExampleResponse addGrammarExample(Long grammarItemId, LessonGrammarExampleRequest request) {
        LessonGrammarItem item = lessonGrammarItemRepository.findById(grammarItemId)
                .orElseThrow(() -> new AppException(ErrorCode.LESSON_GRAMMAR_ITEM_NOT_FOUND));
        if (item.isDeleted()) {
            throw new AppException(ErrorCode.LESSON_GRAMMAR_ITEM_NOT_FOUND);
        }

        Integer maxOrder = item.getExamples() == null ? 0
                : item.getExamples().stream()
                .filter(e -> !e.isDeleted())
                .mapToInt(LessonGrammarExample::getOrderIndex)
                .max().orElse(0);

        LessonGrammarExample ex = LessonGrammarExample.builder()
                .grammarItem(item)
                .exampleJp(request.getExampleJp())
                .exampleVi(request.getExampleVi())
                .orderIndex(request.getOrderIndex() != null ? request.getOrderIndex() : nextOrderIndex(grammarItemId, maxOrder))
                .build();

        item.addExample(ex);
        LessonGrammarExample saved = lessonGrammarExampleRepository.save(ex);
        log.info("[GRAMMAR-EXAMPLE] Added example id={} to grammar item id={}", saved.getId(), grammarItemId);

        return lessonSectionMapper.toGrammarExampleResponse(saved);
    }

    @Transactional
    public LessonGrammarExampleResponse updateGrammarExample(Long exampleId, LessonGrammarExampleRequest request) {
        LessonGrammarExample ex = lessonGrammarExampleRepository.findById(exampleId)
                .orElseThrow(() -> new AppException(ErrorCode.LESSON_GRAMMAR_ITEM_NOT_FOUND));
        if (ex.isDeleted()) {
            throw new AppException(ErrorCode.LESSON_GRAMMAR_ITEM_NOT_FOUND);
        }

        if (request.getExampleJp() != null) ex.setExampleJp(request.getExampleJp());
        if (request.getExampleVi() != null) ex.setExampleVi(request.getExampleVi());
        if (request.getOrderIndex() != null && request.getOrderIndex() > 0) ex.setOrderIndex(request.getOrderIndex());

        LessonGrammarExample saved = lessonGrammarExampleRepository.save(ex);
        log.info("[GRAMMAR-EXAMPLE] Updated example id={}", exampleId);
        return lessonSectionMapper.toGrammarExampleResponse(saved);
    }

    @Transactional
    public void deleteGrammarExample(Long exampleId) {
        LessonGrammarExample ex = lessonGrammarExampleRepository.findById(exampleId)
                .orElseThrow(() -> new AppException(ErrorCode.LESSON_GRAMMAR_ITEM_NOT_FOUND));
        if (ex.isDeleted()) return;
        ex.softDelete();
        lessonGrammarExampleRepository.save(ex);
        log.info("[GRAMMAR-EXAMPLE] Soft-deleted example id={}", exampleId);
    }
}
