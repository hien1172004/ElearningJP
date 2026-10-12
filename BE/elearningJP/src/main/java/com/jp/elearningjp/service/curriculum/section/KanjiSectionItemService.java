package com.jp.elearningjp.service.curriculum.section;

import com.jp.elearningjp.dto.request.curriculum.LessonKanjiItemCreateRequest;
import com.jp.elearningjp.dto.request.curriculum.LessonKanjiItemUpdateRequest;
import com.jp.elearningjp.dto.request.curriculum.LessonKanjiRelatedVocabRequest;
import com.jp.elearningjp.dto.response.curriculum.LessonKanjiItemResponse;
import com.jp.elearningjp.entity.content.Vocabulary;
import com.jp.elearningjp.entity.content.WritingCharacter;
import com.jp.elearningjp.entity.curriculum.LessonKanjiItem;
import com.jp.elearningjp.entity.curriculum.LessonKanjiRelatedVocab;
import com.jp.elearningjp.entity.curriculum.LessonSection;
import com.jp.elearningjp.entity.user.User;
import com.jp.elearningjp.exception.AppException;
import com.jp.elearningjp.exception.ErrorCode;
import com.jp.elearningjp.mapper.LessonSectionMapper;
import com.jp.elearningjp.repository.content.VocabularyCharacterRepository;
import com.jp.elearningjp.repository.content.VocabularyRepository;
import com.jp.elearningjp.repository.content.WritingCharacterRepository;
import com.jp.elearningjp.repository.curriculum.LessonKanjiItemRepository;
import com.jp.elearningjp.repository.curriculum.LessonKanjiRelatedVocabRepository;
import com.jp.elearningjp.repository.curriculum.LessonSectionRepository;
import com.jp.elearningjp.service.dictionary.DictionaryService;
import com.jp.elearningjp.shared.enums.LessonSectionType;
import com.jp.elearningjp.shared.enums.VocabularySource;
import com.jp.elearningjp.shared.util.CurrentUserService;
import com.jp.elearningjp.shared.util.OrderIndexResolver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Service quản lý {@link LessonKanjiItem} bên trong section KANJI.
 *
 * <p>V25+: Related vocabulary do giáo viên/admin tự chọn (có thể lấy từ từ điển
 * hoặc tạo mới vocab) — lưu trong bảng {@code lesson_kanji_related_vocabulary}.
 * Nếu giáo viên CHƯA chọn vocab nào cho 1 kanji item → response tự động fallback
 * về derive từ bảng {@code vocabulary_characters} (giống hành vi cũ).</p>
 *
 * <p>API cung cấp (4 endpoints chính):</p>
 * <ul>
 *   <li>{@link #getKanjiItem(Long)} — lấy chi tiết 1 kanji item.</li>
 *   <li>{@link #addKanjiItem(Long, LessonKanjiItemCreateRequest)} — tạo kanji item kèm
 *       {@code relatedVocabulary} trong payload.</li>
 *   <li>{@link #updateKanjiItem(Long, LessonKanjiItemUpdateRequest)} — cập nhật kanji item,
 *       sync {@code relatedVocabulary} theo 3 semantics (null = giữ / [] = xoá hết /
 *       mảng = thay thế).</li>
 *   <li>{@link #deleteKanjiItem(Long)} — soft-delete kanji item + cascade soft-delete
 *       toàn bộ related vocab thuộc item đó.</li>
 * </ul>
 */
@Service
@Slf4j
public class KanjiSectionItemService extends AbstractSectionItemService {

    private static final int MAX_RELATED_VOCAB_PER_REQUEST = 50;

    private final LessonKanjiItemRepository kanjiItemRepository;
    private final LessonKanjiRelatedVocabRepository relatedVocabRepository;
    private final WritingCharacterRepository writingCharacterRepository;
    private final VocabularyRepository vocabularyRepository;
    private final VocabularyCharacterRepository vocabularyCharacterRepository;
    private final LessonSectionMapper lessonSectionMapper;
    private final DictionaryService dictionaryService;
    private final CurrentUserService currentUserService;

    public KanjiSectionItemService(
            LessonSectionRepository lessonSectionRepository,
            LessonKanjiItemRepository kanjiItemRepository,
            LessonKanjiRelatedVocabRepository relatedVocabRepository,
            WritingCharacterRepository writingCharacterRepository,
            VocabularyRepository vocabularyRepository,
            VocabularyCharacterRepository vocabularyCharacterRepository,
            LessonSectionMapper lessonSectionMapper,
            DictionaryService dictionaryService,
            CurrentUserService currentUserService
    ) {
        super(lessonSectionRepository);
        this.kanjiItemRepository = kanjiItemRepository;
        this.relatedVocabRepository = relatedVocabRepository;
        this.writingCharacterRepository = writingCharacterRepository;
        this.vocabularyRepository = vocabularyRepository;
        this.vocabularyCharacterRepository = vocabularyCharacterRepository;
        this.lessonSectionMapper = lessonSectionMapper;
        this.dictionaryService = dictionaryService;
        this.currentUserService = currentUserService;
    }

    // ============================================================
    // KANJI ITEM CRUD
    // ============================================================

    /**
     * Lấy chi tiết 1 chữ Hán trong section (kèm related vocab đã chọn hoặc auto-derive).
     */
    @Transactional(readOnly = true)
    public LessonKanjiItemResponse getKanjiItem(Long itemId) {
        LessonKanjiItem item = kanjiItemRepository.findById(itemId)
                .orElseThrow(() -> new AppException(ErrorCode.LESSON_KANJI_ITEM_NOT_FOUND));
        if (item.isDeleted()) {
            throw new AppException(ErrorCode.LESSON_KANJI_ITEM_NOT_FOUND);
        }
        return buildResponse(item);
    }

    /**
     * Thêm 1 chữ Hán vào section.
     *
     * <p>Client gửi {@code character} (chuỗi chữ Hán, ví dụ "学") — service sẽ
     * gọi {@link DictionaryService#getKanjiDetail(String)} để validate tồn tại
     * trong từ điển, sau đó tìm entity {@code WritingCharacter} qua
     * {@code findByCharacterAndDeletedFalse} để gắn vào item.</p>
     *
     * <p>V25+: Client có thể đính kèm {@code relatedVocabulary} — mỗi entry là
     * {@link LessonKanjiRelatedVocabRequest} gồm {@code vocabId} (chọn từ từ điển)
     * hoặc {@code newVocab} (tạo mới vocab trong từ điển rồi gắn vào).</p>
     */
    @Transactional
    public LessonKanjiItemResponse addKanjiItem(Long sectionId, LessonKanjiItemCreateRequest request) {
        LessonSection section = getActiveSection(sectionId, LessonSectionType.KANJI);

        // Validate chữ Hán tồn tại trong từ điển qua DictionaryService
        String character = request.getCharacter();
        if (character == null || character.isBlank()) {
            throw new AppException(ErrorCode.CHARACTER_NOT_FOUND);
        }
        dictionaryService.getKanjiDetail(character); // throws nếu không tồn tại

        WritingCharacter writingChar = writingCharacterRepository.findByCharacterAndDeletedFalse(character)
                .orElseThrow(() -> new AppException(ErrorCode.CHARACTER_NOT_FOUND));

        assertNoDuplicateKanji(sectionId, writingChar.getId());

        // Nhận xét #2: xử lý orderIndex đúng cách (shift các item có index >= target lên +1)
        List<LessonKanjiItem> existingItems = kanjiItemRepository
                .findBySectionIdAndDeletedFalseOrderByOrderIndexAsc(sectionId);
        int maxOrder = OrderIndexResolver.maxOrderIndex(existingItems, LessonKanjiItem::getOrderIndex);
        int resolvedIndex = OrderIndexResolver.resolveForCreate(
                request.getOrderIndex(), () -> maxOrder
        );
        boolean clientChoseIndex = request.getOrderIndex() != null && request.getOrderIndex() > 0
                && resolvedIndex == request.getOrderIndex();
        if (clientChoseIndex) {
            OrderIndexResolver.shiftUpInCollection(
                    existingItems, resolvedIndex,
                    LessonKanjiItem::getOrderIndex, LessonKanjiItem::setOrderIndex
            );
            log.info("[KANJI-ITEM] Shifted up items in section id={} with orderIndex >= {}", sectionId, resolvedIndex);
        }

        LessonKanjiItem item = LessonKanjiItem.builder()
                .section(section)
                .character(writingChar)
                .orderIndex(resolvedIndex)
                .note(request.getNote())
                .build();

        section.addKanjiItem(item);
        // Save tất cả kanji items trong section (kể cả những cái bị shift) trong 1 transaction
        kanjiItemRepository.saveAll(existingItems);
        LessonKanjiItem saved = kanjiItemRepository.save(item);
        log.info("[KANJI-ITEM] Added character='{}' (id={}) into section id={} at orderIndex={}",
                character, writingChar.getId(), sectionId, saved.getOrderIndex());

        // V25+: tạo related vocab do giáo viên chọn (idempotent: nếu danh sách rỗng → không làm gì)
        int created = applyRelatedVocabCreate(saved, request.getRelatedVocabulary());
        if (created > 0) {
            log.info("[KANJI-ITEM] Created {} teacher-selected related vocab(s) for kanji item id={}",
                    created, saved.getId());
        }

        return buildResponse(saved);
    }

    /**
     * Cập nhật kanji item + (tùy chọn) orderIndex, note, relatedVocabulary.
     *
     * <p>Semantics cho {@code relatedVocabulary} (V25+):</p>
     * <ul>
     *   <li>Trường KHÔNG gửi (null) → giữ nguyên.</li>
     *   <li>Trường gửi = [] (rỗng) → xoá hết related vocab do giáo viên chọn.</li>
     *   <li>Trường gửi 1 mảng → thay thế toàn bộ (soft-delete cái cũ, tạo mới theo danh sách).</li>
     * </ul>
     *
     * <p>Nhận xét #2: nếu client đổi orderIndex, dời các item khác để giải phóng slot.</p>
     */
    @Transactional
    public LessonKanjiItemResponse updateKanjiItem(Long itemId, LessonKanjiItemUpdateRequest request) {
        LessonKanjiItem item = kanjiItemRepository.findById(itemId)
                .orElseThrow(() -> new AppException(ErrorCode.LESSON_KANJI_ITEM_NOT_FOUND));
        if (item.isDeleted()) {
            throw new AppException(ErrorCode.LESSON_KANJI_ITEM_NOT_FOUND);
        }

        Integer newOrderIndex = request.getOrderIndex();
        Integer oldOrderIndex = item.getOrderIndex();

        // Nhận xét #2: xử lý di chuyển orderIndex (move up/down với shift)
        if (newOrderIndex != null && newOrderIndex > 0 && !newOrderIndex.equals(oldOrderIndex)) {
            Long sectionId = item.getSection().getId();
            List<LessonKanjiItem> allItems = kanjiItemRepository
                    .findBySectionIdAndDeletedFalseOrderByOrderIndexAsc(sectionId);
            int maxOrder = OrderIndexResolver.maxOrderIndex(allItems, LessonKanjiItem::getOrderIndex);
            int target = OrderIndexResolver.clampToMaxPlusOne(newOrderIndex, () -> maxOrder);
            if (target != oldOrderIndex) {
                // Loại bỏ item hiện tại khỏi list để không bị shift
                allItems = allItems.stream().filter(i -> !i.getId().equals(itemId)).toList();
                if (target < oldOrderIndex) {
                    OrderIndexResolver.shiftUpInCollection(
                            allItems, target,
                            LessonKanjiItem::getOrderIndex, LessonKanjiItem::setOrderIndex
                    );
                    log.info("[KANJI-ITEM] Move-up: shifted items in [{}, {}] for kanji item id={}",
                            target, oldOrderIndex - 1, itemId);
                } else {
                    OrderIndexResolver.shiftDownInCollection(
                            allItems, oldOrderIndex, target,
                            LessonKanjiItem::getOrderIndex, LessonKanjiItem::setOrderIndex
                    );
                    log.info("[KANJI-ITEM] Move-down: shifted items in ({}, {}] for kanji item id={}",
                            oldOrderIndex, target, itemId);
                }
                kanjiItemRepository.saveAll(allItems);
                item.setOrderIndex(target);
            }
        }

        if (request.getNote() != null) {
            item.setNote(request.getNote().isEmpty() ? null : request.getNote());
        }

        LessonKanjiItem saved = kanjiItemRepository.save(item);

        // V25+: sync related vocab nếu client có gửi (kể cả list rỗng = xoá hết)
        if (request.getRelatedVocabulary() != null) {
            int replaced = replaceRelatedVocab(saved, request.getRelatedVocabulary());
            log.info("[KANJI-ITEM] Replaced related vocab for kanji item id={} → {} entry(s)",
                    itemId, replaced);
        }

        log.info("[KANJI-ITEM] Updated kanji item id={}", itemId);
        return buildResponse(saved);
    }

    @Transactional
    public void deleteKanjiItem(Long itemId) {
        LessonKanjiItem item = kanjiItemRepository.findById(itemId)
                .orElseThrow(() -> new AppException(ErrorCode.LESSON_KANJI_ITEM_NOT_FOUND));
        if (item.isDeleted()) return;

        // V25+: cascade soft-delete tất cả related vocab thuộc kanji item này.
        // Lý do cascade thay vì để DB tự cascade: nếu dùng @ManyToOne(cascade=ALL, orphanRemoval=true)
        // thì Hibernate sẽ xoá cứng → mất audit trail. Ta muốn soft-delete để có thể khôi phục.
        List<LessonKanjiRelatedVocab> relatedVocabs =
                relatedVocabRepository.findByKanjiItemIdAndDeletedFalse(itemId);
        if (!relatedVocabs.isEmpty()) {
            relatedVocabs.forEach(LessonKanjiRelatedVocab::softDelete);
            relatedVocabRepository.saveAll(relatedVocabs);
            log.info("[KANJI-ITEM] Cascade soft-deleted {} related vocab(s) for kanji item id={}",
                    relatedVocabs.size(), itemId);
        }

        item.softDelete();
        kanjiItemRepository.save(item);
        log.info("[KANJI-ITEM] Soft-deleted kanji item id={}", itemId);
    }

    // ============================================================
    // HELPERS
    // ============================================================

    private void assertNoDuplicateKanji(Long sectionId, Long characterId) {
        boolean exists = kanjiItemRepository
                .findBySectionIdAndDeletedFalseOrderByOrderIndexAsc(sectionId)
                .stream()
                .anyMatch(i -> !i.isDeleted()
                        && i.getCharacter() != null
                        && i.getCharacter().getId().equals(characterId));
        if (exists) {
            throw new AppException(ErrorCode.LESSON_KANJI_ITEM_DUPLICATED);
        }
    }

    private void assertNoDuplicateRelatedVocab(Long itemId, Long vocabId, Long excludeRvId) {
        boolean exists = relatedVocabRepository.findByKanjiItemIdAndDeletedFalse(itemId).stream()
                .anyMatch(rv -> !rv.isDeleted()
                        && rv.getVocab() != null
                        && rv.getVocab().getId().equals(vocabId)
                        && (excludeRvId == null || !rv.getId().equals(excludeRvId)));
        if (exists) {
            throw new AppException(ErrorCode.LESSON_KANJI_RELATED_VOCAB_DUPLICATED);
        }
    }

    /**
     * Build response cho 1 kanji item (lấy teacher override vocab nếu có, fallback auto-derive).
     */
    private LessonKanjiItemResponse buildResponse(LessonKanjiItem saved) {
        // Lấy teacher override (nếu có)
        List<LessonKanjiRelatedVocab> teacherRvs =
                relatedVocabRepository.findByKanjiItemIdAndDeletedFalse(saved.getId());
        boolean hasTeacherRvs = !teacherRvs.isEmpty();

        if (hasTeacherRvs) {
            // Trả về teacher override (không cần auto-derive)
            return lessonSectionMapper.toKanjiItemResponse(
                    saved, java.util.Collections.emptyMap(),
                    java.util.Map.of(saved.getId(), teacherRvs)
            );
        }

        // Auto-derive từ từ điển — luôn gọi overload 3-arg để mapper biết
        // đây là derived vocab (không phải teacher override).
        WritingCharacter character = saved.getCharacter();
        if (character != null) {
            List<Vocabulary> relatedVocabs = vocabularyCharacterRepository
                    .findRelatedVocabulariesByCharacterId(character.getId());
            if (!relatedVocabs.isEmpty()) {
                return lessonSectionMapper.toKanjiItemResponse(
                        saved,
                        java.util.Map.of(character.getId(), relatedVocabs),
                        java.util.Collections.emptyMap()
                );
            }
        }
        return lessonSectionMapper.toKanjiItemResponse(
                saved, java.util.Collections.emptyMap(), java.util.Collections.emptyMap()
        );
    }

    /**
     * Xử lý {@code relatedVocabulary} trong payload create:
     * <ul>
     *   <li>null hoặc rỗng → không làm gì (kanji item sẽ dùng auto-derive).</li>
     *   <li>Có entry → mỗi entry có thể tạo N entity
     *       ({@code vocabIds.size() + (newVocab != null ? 1 : 0)}).</li>
     * </ul>
     *
     * @return số lượng related vocab đã tạo thành công.
     */
    private int applyRelatedVocabCreate(LessonKanjiItem item, List<LessonKanjiRelatedVocabRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return 0;
        }
        if (requests.size() > MAX_RELATED_VOCAB_PER_REQUEST) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        // Đếm tổng số vocab sẽ tạo (validate trước khi save)
        int totalVocabs = 0;
        for (LessonKanjiRelatedVocabRequest req : requests) {
            if (req == null || !req.isValid()) {
                throw new AppException(ErrorCode.INVALID_REQUEST);
            }
            totalVocabs += req.countVocabs();
        }
        if (totalVocabs > MAX_RELATED_VOCAB_PER_REQUEST) {
            log.warn("[KANJI-ITEM] Too many related vocabs requested for kanji item id={}: {} > {}",
                    item.getId(), totalVocabs, MAX_RELATED_VOCAB_PER_REQUEST);
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        Set<Long> seenVocabIds = new HashSet<>();
        int created = 0;
        for (LessonKanjiRelatedVocabRequest req : requests) {
            List<LessonKanjiRelatedVocab> rvs = buildRelatedVocabs(item, req);
            for (LessonKanjiRelatedVocab rv : rvs) {
                Long vocabId = rv.getVocab().getId();
                if (!seenVocabIds.add(vocabId)) {
                    // duplicate vocab trong cùng request → bỏ qua entry trùng sau
                    log.warn("[KANJI-ITEM] Duplicate vocabId={} in same create request for kanji item id={} — skipped",
                            vocabId, item.getId());
                    continue;
                }
                // Check duplicate với DB hiện tại (không bao gồm các rv vừa tạo trong loop)
                assertNoDuplicateRelatedVocab(item.getId(), vocabId, null);
                relatedVocabRepository.save(rv);
                created++;
            }
        }
        return created;
    }

    /**
     * Thay thế toàn bộ related vocab của 1 kanji item bằng danh sách mới.
     * <p>Cách làm: soft-delete tất cả rv hiện tại → tạo mới theo {@code newRequests}.</p>
     *
     * @return số lượng rv mới đã tạo.
     */
    private int replaceRelatedVocab(LessonKanjiItem item, List<LessonKanjiRelatedVocabRequest> newRequests) {
        // 1) Soft-delete tất cả rv hiện tại (kể cả rỗng)
        List<LessonKanjiRelatedVocab> existing = relatedVocabRepository.findByKanjiItemIdAndDeletedFalse(item.getId());
        if (!existing.isEmpty()) {
            existing.forEach(LessonKanjiRelatedVocab::softDelete);
            relatedVocabRepository.saveAll(existing);
            log.info("[KANJI-ITEM] Soft-deleted {} existing related vocab(s) for kanji item id={}",
                    existing.size(), item.getId());
        }
        // 2) Tạo mới (re-use helper) — nhưng bỏ duplicate-check DB (vì đã xoá hết)
        if (newRequests.isEmpty()) {
            return 0;
        }

        // Validate và đếm tổng số vocab sẽ tạo
        int totalVocabs = 0;
        for (LessonKanjiRelatedVocabRequest req : newRequests) {
            if (req == null || !req.isValid()) {
                throw new AppException(ErrorCode.INVALID_REQUEST);
            }
            totalVocabs += req.countVocabs();
        }
        if (totalVocabs > MAX_RELATED_VOCAB_PER_REQUEST) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        Set<Long> seenVocabIds = new HashSet<>();
        int created = 0;
        for (LessonKanjiRelatedVocabRequest req : newRequests) {
            List<LessonKanjiRelatedVocab> rvs = buildRelatedVocabs(item, req);
            for (LessonKanjiRelatedVocab rv : rvs) {
                Long vocabId = rv.getVocab().getId();
                if (!seenVocabIds.add(vocabId)) {
                    log.warn("[KANJI-ITEM] Duplicate vocabId={} in update request for kanji item id={} — skipped",
                            vocabId, item.getId());
                    continue;
                }
                relatedVocabRepository.save(rv);
                created++;
            }
        }
        return created;
    }

    /**
     * Build danh sách entity {@link LessonKanjiRelatedVocab} từ 1 entry request.
     * Mỗi entry có thể tạo ra N entity:
     * <ul>
     *   <li>Với mỗi id trong {@code vocabIds} → load vocab từ DB và tạo 1 entity.</li>
     *   <li>Nếu có {@code newVocab} (đầy đủ word + hiragana) → tạo 1 vocab mới và 1 entity.</li>
     * </ul>
     *
     * <p>Note dùng chung cho cả danh sách vocab trong entry này. VocabIds bị trùng → bỏ qua.</p>
     */
    private List<LessonKanjiRelatedVocab> buildRelatedVocabs(LessonKanjiItem item,
                                                              LessonKanjiRelatedVocabRequest req) {
        if (req == null || !req.isValid()) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        // Note semantics: rỗng → null, dùng chung cho cả entry
        String note = req.getNote();
        if (note != null && note.isBlank()) {
            note = null;
        }

        List<LessonKanjiRelatedVocab> results = new ArrayList<>();
        Set<Long> seenIdsInEntry = new HashSet<>();

        // 1) Xử lý vocabIds (nhiều vocab có sẵn trong từ điển)
        if (req.getVocabIds() != null) {
            for (Long vocabId : req.getVocabIds()) {
                if (vocabId == null || vocabId <= 0) {
                    log.warn("[KANJI-ITEM] Invalid vocabId={} in entry for kanji item id={} — skipped",
                            vocabId, item.getId());
                    continue;
                }
                if (!seenIdsInEntry.add(vocabId)) {
                    log.warn("[KANJI-ITEM] Duplicate vocabId={} in same entry for kanji item id={} — skipped",
                            vocabId, item.getId());
                    continue;
                }
                Vocabulary vocab = vocabularyRepository.findByIdAndDeletedFalse(vocabId)
                        .orElseThrow(() -> new AppException(ErrorCode.VOCAB_NOT_FOUND));
                results.add(LessonKanjiRelatedVocab.builder()
                        .kanjiItem(item)
                        .vocab(vocab)
                        .note(note)
                        .build());
            }
        }

        // 2) Xử lý newVocab (tạo mới 1 vocab trong từ điển)
        if (req.getNewVocab() != null
                && req.getNewVocab().getWord() != null && !req.getNewVocab().getWord().isBlank()
                && req.getNewVocab().getHiragana() != null && !req.getNewVocab().getHiragana().isBlank()) {
            Vocabulary newVocab = createNewVocabulary(req.getNewVocab());
            results.add(LessonKanjiRelatedVocab.builder()
                    .kanjiItem(item)
                    .vocab(newVocab)
                    .note(note)
                    .build());
        }

        if (results.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
        return results;
    }

    /**
     * Tạo mới 1 {@link Vocabulary} trong từ điển với payload tối thiểu.
     * VocabularySource mặc định = USER_CUSTOM (do giáo viên tự tạo) để phân biệt với CORE.
     *
     * <p>Chống trùng theo cặp {@code (word, hiragana)}: nếu đã tồn tại vocab active
     * với cùng cặp này, REUSE lại vocab cũ thay vì insert mới (tránh vi phạm
     * unique constraint {@code uq_vocabulary_word_hiragana}).</p>
     */
    private Vocabulary createNewVocabulary(LessonKanjiRelatedVocabRequest.NewVocabPayload p) {
        if (p == null || p.getWord() == null || p.getWord().isBlank()
                || p.getHiragana() == null || p.getHiragana().isBlank()) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        String word = p.getWord().trim();
        String hiragana = p.getHiragana().trim();

        // 1) Check đã tồn tại vocab với cùng (word, hiragana) chưa → reuse
        Optional<Vocabulary> existing = vocabularyRepository.findByWordAndHiraganaAndDeletedFalse(word, hiragana);
        if (existing.isPresent()) {
            log.info("[KANJI-ITEM] Reusing existing vocabulary id={} (word='{}', hiragana='{}') instead of creating duplicate",
                    existing.get().getId(), word, hiragana);
            return existing.get();
        }

        // 2) Tạo mới
        User currentUser = currentUserService.getCurrentUser();
        Vocabulary vocab = Vocabulary.builder()
                .word(word)
                .hiragana(hiragana)
                .romaji(p.getRomaji())
                .hanViet(p.getHanViet())
                .meaningVi(p.getMeaningVi())
                .meaningEn(p.getMeaningEn())
                .partOfSpeech(p.getPartOfSpeech())
                .sourceType(VocabularySource.USER_CUSTOM)
                .createdByUser(currentUser)
                .active(true)
                .build();
        Vocabulary saved = vocabularyRepository.save(vocab);
        log.info("[KANJI-ITEM] Created new vocabulary id={} (word='{}') by user id={}",
                saved.getId(), saved.getWord(), currentUser.getId());
        return saved;
    }
}
