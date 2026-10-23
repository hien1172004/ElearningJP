package com.jp.elearningjp.service.curriculum.section;

import com.jp.elearningjp.dto.request.curriculum.ItemBulkReorderRequest;
import com.jp.elearningjp.dto.request.curriculum.LessonVocabularyExampleCreateRequest;
import com.jp.elearningjp.dto.request.curriculum.LessonVocabularyExampleUpsertRequest;
import com.jp.elearningjp.dto.request.curriculum.LessonVocabularyItemCreateRequest;
import com.jp.elearningjp.dto.request.curriculum.LessonVocabularyItemUpdateRequest;
import com.jp.elearningjp.dto.response.curriculum.LessonVocabularyItemResponse;
import com.jp.elearningjp.entity.content.Vocabulary;
import com.jp.elearningjp.entity.content.VocabularyExample;
import com.jp.elearningjp.entity.curriculum.LessonSection;
import com.jp.elearningjp.entity.curriculum.LessonVocabularyExample;
import com.jp.elearningjp.entity.curriculum.LessonVocabularyItem;
import com.jp.elearningjp.exception.AppException;
import com.jp.elearningjp.exception.ErrorCode;
import com.jp.elearningjp.mapper.LessonSectionMapper;
import com.jp.elearningjp.repository.content.VocabularyRepository;
import com.jp.elearningjp.repository.curriculum.LessonSectionRepository;
import com.jp.elearningjp.repository.curriculum.LessonVocabularyExampleRepository;
import com.jp.elearningjp.repository.curriculum.LessonVocabularyItemRepository;
import com.jp.elearningjp.shared.enums.LessonSectionType;
import com.jp.elearningjp.shared.util.CourseAccessGuard;
import com.jp.elearningjp.shared.util.OrderIndexResolver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Service quản lý {@link LessonVocabularyItem} (kèm {@link LessonVocabularyExample})
 * bên trong section VOCABULARY.
 *
 * <p>Vocab item có 4 endpoint: POST, GET, PUT, DELETE.
 * Examples được xử lý trong payload create/update (giống relatedVocab của kanji item)
 * — KHÔNG còn endpoint riêng cho example. Khi xoá vocab item → cascade soft-delete
 * tất cả examples thuộc item đó.</p>
 */
@Service
@Slf4j
public class VocabularySectionItemService extends AbstractSectionItemService {

    /** Số example tối đa cho phép trong 1 request create/update. */
    private static final int MAX_EXAMPLES_PER_REQUEST = 100;

    private final LessonVocabularyItemRepository vocabItemRepository;
    private final LessonVocabularyExampleRepository vocabExampleRepository;
    private final VocabularyRepository vocabularyRepository;
    private final LessonSectionMapper lessonSectionMapper;
    private final CourseAccessGuard courseAccessGuard;

    public VocabularySectionItemService(
            LessonSectionRepository lessonSectionRepository,
            LessonVocabularyItemRepository vocabItemRepository,
            LessonVocabularyExampleRepository vocabExampleRepository,
            VocabularyRepository vocabularyRepository,
            LessonSectionMapper lessonSectionMapper,
            CourseAccessGuard courseAccessGuard
    ) {
        super(lessonSectionRepository);
        this.vocabItemRepository = vocabItemRepository;
        this.vocabExampleRepository = vocabExampleRepository;
        this.vocabularyRepository = vocabularyRepository;
        this.lessonSectionMapper = lessonSectionMapper;
        this.courseAccessGuard = courseAccessGuard;
    }

    // ============================================================
    // VOCABULARY ITEM CRUD (4 endpoints chính)
    // ============================================================

    /**
     * Thêm vocab item vào section VOCABULARY, kèm examples do giáo viên cấu hình.
     *
     * <p>Semantics cho các trường override (V23+):</p>
     * <ul>
     *   <li>Nếu client gửi {@code customXxx = null} → auto-fill từ {@code vocabulary}
     *       (lưu vào DB, dùng để hiển thị). Sửa đổi từ điển sẽ KHÔNG tự động cập nhật item.</li>
     *   <li>Nếu client gửi {@code customXxx = ""} → set NULL (xoá override, hiển thị lấy từ từ điển).</li>
     *   <li>Nếu client gửi giá trị → lưu override đó.</li>
     * </ul>
     *
     * <p>Semantics cho {@code examples}:</p>
     * <ul>
     *   <li>{@code examples == null} (không gửi) HOẶC {@code examples = []} → auto-copy
     *       tất cả examples (chưa xoá) từ {@code vocabulary.examples} sang item.</li>
     *   <li>{@code examples = [...]} → dùng danh sách này (không auto-copy từ từ điển).</li>
     * </ul>
     */
    @Transactional
    public LessonVocabularyItemResponse addVocabularyItem(Long sectionId, LessonVocabularyItemCreateRequest request) {
        LessonSection section = getActiveSection(sectionId, LessonSectionType.VOCABULARY);

        // Eager-load vocab kèm examples chỉ khi cần auto-copy (teacher không gửi examples HOẶC gửi []).
        // Khi teacher cung cấp examples, dùng findByIdAndDeletedFalse cho nhẹ.
        Vocabulary vocab;
        if (request.getExamples() == null || request.getExamples().isEmpty()) {
            vocab = vocabularyRepository.findByIdWithActiveExamples(request.getVocabId())
                    .orElseThrow(() -> new AppException(ErrorCode.VOCAB_NOT_FOUND));
        } else {
            vocab = vocabularyRepository.findByIdAndDeletedFalse(request.getVocabId())
                    .orElseThrow(() -> new AppException(ErrorCode.VOCAB_NOT_FOUND));
        }

        assertNoDuplicateVocabulary(sectionId, vocab.getId());

        // Nhận xét #2: xử lý orderIndex bằng ADJACENT SWAP (swap chain từng bước).
        // Ví dụ chi tiết: xem comment trong LessonService.createLesson.
        List<LessonVocabularyItem> existingItems = vocabItemRepository
                .findBySectionIdAndDeletedFalseOrderByOrderIndexAsc(sectionId);
        int maxOrder = OrderIndexResolver.maxOrderIndex(existingItems, LessonVocabularyItem::getOrderIndex);
        Integer requestedIndex = request.getOrderIndex();
        boolean clientChoseIndex = requestedIndex != null && requestedIndex > 0
                && requestedIndex <= maxOrder + 1;

        int tempIndex = maxOrder + 1;
        int finalIndex = clientChoseIndex ? requestedIndex : tempIndex;

        // Resolve các trường override: null = auto-fill từ từ điển, "" = xoá override
        String resolvedHiragana = resolveOverride(request.getCustomHiragana(), vocab.getHiragana());
        String resolvedRomaji   = resolveOverride(request.getCustomRomaji(), vocab.getRomaji());
        String resolvedAudio    = resolveOverride(request.getCustomAudioUrl(), vocab.getAudioUrl());
        String resolvedMeaning  = resolveOverride(request.getCustomMeaning(), vocab.getMeaningVi());

        LessonVocabularyItem item = LessonVocabularyItem.builder()
                .section(section)
                .vocab(vocab)
                .orderIndex(tempIndex)
                .customHiragana(resolvedHiragana)
                .customRomaji(resolvedRomaji)
                .customAudioUrl(resolvedAudio)
                .customMeaning(resolvedMeaning)
                .imageUrl(request.getImageUrl())
                .note(request.getNote())
                .build();

        // Xử lý examples: teacher-controlled (ưu tiên) HOẶC auto-copy từ vocabulary
        int totalExamples = applyExamplesOnCreate(item, vocab, request.getExamples());

        section.addVocabularyItem(item);
        LessonVocabularyItem saved = vocabItemRepository.save(item);
        vocabItemRepository.flush();
        log.info("[VOCAB-ITEM] Added vocab id={} into section id={} at tempIndex={} (with {} examples)",
                vocab.getId(), sectionId, tempIndex, totalExamples);

        if (clientChoseIndex && finalIndex != tempIndex) {
            Long newItemId = saved.getId();
            int step = (finalIndex < tempIndex) ? -1 : 1;
            int current = tempIndex;
            int safetyCounter = 0;
            final int MAX_SWAPS = tempIndex + 2;
            while (current != finalIndex && safetyCounter++ < MAX_SWAPS) {
                int nextIndex = current + step;
                Long neighborId = vocabItemRepository.findIdBySectionIdAndOrderIndex(sectionId, nextIndex);
                if (neighborId == null) break;
                vocabItemRepository.swapOrderIndexes(newItemId, nextIndex, neighborId, current);
                vocabItemRepository.flush();
                current = nextIndex;
            }
            saved.setOrderIndex(finalIndex);
            vocabItemRepository.flush();
            log.info("[VOCAB-ITEM] Swapped new vocab item id={} from tempIndex={} to orderIndex={} (sectionId={})",
                    newItemId, tempIndex, finalIndex, sectionId);
        }

        return lessonSectionMapper.toVocabularyItemResponse(saved);
    }

    /**
     * Lấy chi tiết 1 vocab item theo id.
     */
    @Transactional(readOnly = true)
    public LessonVocabularyItemResponse getVocabularyItem(Long itemId) {
        LessonVocabularyItem item = vocabItemRepository.findById(itemId)
                .orElseThrow(() -> new AppException(ErrorCode.LESSON_VOCABULARY_ITEM_NOT_FOUND));
        if (item.isDeleted()) {
            throw new AppException(ErrorCode.LESSON_VOCABULARY_ITEM_NOT_FOUND);
        }
        return lessonSectionMapper.toVocabularyItemResponse(item);
    }

    /**
     * Cập nhật vocab item.
     *
     * <p>Semantics cho các trường override (V23+):</p>
     * <ul>
     *   <li>Trường KHÔNG gửi (null) trong request → giữ nguyên giá trị override hiện tại.</li>
     *   <li>Trường gửi = "" → set NULL (xoá override, hiển thị sẽ fallback về từ điển).</li>
     *   <li>Trường gửi giá trị → cập nhật override.</li>
     * </ul>
     *
     * <p>Semantics cho {@code examples} (3-state):</p>
     * <ul>
     *   <li>{@code examples == null} → giữ nguyên examples hiện tại.</li>
     *   <li>{@code examples = []} → RESET: xoá tất cả examples hiện tại rồi auto-copy lại
     *       từ {@code vocabulary.examples} (lấy example tĩnh mới nhất).</li>
     *   <li>{@code examples = [...]} → upsert từng entry theo {@code id}.
     *       Example hiện tại không có trong list sẽ bị soft-delete.</li>
     * </ul>
     */
    @Transactional
    public LessonVocabularyItemResponse updateVocabularyItem(Long itemId, LessonVocabularyItemUpdateRequest request) {
        LessonVocabularyItem item = vocabItemRepository.findByIdWithExamples(itemId)
                .orElseThrow(() -> new AppException(ErrorCode.LESSON_VOCABULARY_ITEM_NOT_FOUND));
        if (item.isDeleted()) {
            throw new AppException(ErrorCode.LESSON_VOCABULARY_ITEM_NOT_FOUND);
        }

        Integer newOrderIndex = request.getOrderIndex();
        Integer oldOrderIndex = item.getOrderIndex();

        // Nhận xét #2: xử lý di chuyển orderIndex bằng ADJACENT SWAP (xem comment trong KanjiItemService)
        if (newOrderIndex != null && newOrderIndex > 0 && !newOrderIndex.equals(oldOrderIndex)) {
            Long sectionId = item.getSection().getId();
            List<LessonVocabularyItem> allItems = vocabItemRepository
                    .findBySectionIdAndDeletedFalseOrderByOrderIndexAsc(sectionId);
            int maxOrder = OrderIndexResolver.maxOrderIndex(allItems, LessonVocabularyItem::getOrderIndex);
            int target = OrderIndexResolver.clampToMaxPlusOne(newOrderIndex, () -> maxOrder);
            if (target != oldOrderIndex) {
                log.info("[VOCAB-ITEM] Adjacent-swap: moving vocab item id={} from orderIndex={} → target={} (sectionId={})",
                        itemId, oldOrderIndex, target, sectionId);
                int step = (target < oldOrderIndex) ? -1 : 1;
                int current = oldOrderIndex;
                int safetyCounter = 0;
                final int MAX_SWAPS = maxOrder + 2;
                while (current != target && safetyCounter++ < MAX_SWAPS) {
                    int nextIndex = current + step;
                    Long neighborId = vocabItemRepository.findIdBySectionIdAndOrderIndex(sectionId, nextIndex);
                    if (neighborId == null) break;
                    vocabItemRepository.swapOrderIndexes(itemId, nextIndex, neighborId, current);
                    vocabItemRepository.flush();
                    current = nextIndex;
                }
                item.setOrderIndex(target);
            }
        }

        // V23+: customXxx — null = giữ nguyên, "" = xoá override, có giá trị = set
        if (request.getCustomHiragana() != null) {
            item.setCustomHiragana(request.getCustomHiragana().isEmpty() ? null : request.getCustomHiragana());
        }
        if (request.getCustomRomaji() != null) {
            item.setCustomRomaji(request.getCustomRomaji().isEmpty() ? null : request.getCustomRomaji());
        }
        if (request.getCustomAudioUrl() != null) {
            item.setCustomAudioUrl(request.getCustomAudioUrl().isEmpty() ? null : request.getCustomAudioUrl());
        }
        if (request.getCustomMeaning() != null) {
            item.setCustomMeaning(request.getCustomMeaning().isEmpty() ? null : request.getCustomMeaning());
        }
        if (request.getImageUrl() != null) {
            item.setImageUrl(request.getImageUrl().isEmpty() ? null : request.getImageUrl());
        }
        if (request.getNote() != null) {
            item.setNote(request.getNote().isEmpty() ? null : request.getNote());
        }

        // Xử lý examples (3-state):
        //   - examples == null  → giữ nguyên, không làm gì
        //   - examples = []     → RESET: xoá tất cả examples hiện tại rồi auto-copy lại từ vocab
        //   - examples = [...]  → upsert từng entry
        int examplesAffected = handleExamplesUpdate(item, request.getExamples());

        LessonVocabularyItem saved = vocabItemRepository.save(item);
        log.info("[VOCAB-ITEM] Updated vocab item id={} (examples affected: {})", itemId, examplesAffected);
        return lessonSectionMapper.toVocabularyItemResponse(saved);
    }

    /**
     * Xoá mềm vocab item + cascade soft-delete toàn bộ examples thuộc item.
     */
    @Transactional
    public void deleteVocabularyItem(Long itemId) {
        LessonVocabularyItem item = vocabItemRepository.findById(itemId)
                .orElseThrow(() -> new AppException(ErrorCode.LESSON_VOCABULARY_ITEM_NOT_FOUND));
        if (item.isDeleted()) return;
        item.softDelete();
        // Cascade soft-delete examples
        int cascadeCount = softDeleteAllExamples(item);
        vocabItemRepository.save(item);
        log.info("[VOCAB-ITEM] Soft-deleted vocab item id={} (cascaded {} examples)",
                itemId, cascadeCount);
    }

    /**
     * Sắp xếp lại thứ tự hàng loạt các vocab item trong 1 section (drag-drop UI).
     *
     * <p>Quy tắc: list {@code itemIds} phải chứa <b>đúng tất cả</b> vocab item active
     * của section hiện tại (không thiếu, không thừa, không trùng).</p>
     *
     * <p>Quyền: Admin hoặc Teacher (chỉ reorder trong course do mình tạo).</p>
     */
    @Transactional
    public List<LessonVocabularyItemResponse> reorderVocabularyItems(Long sectionId, ItemBulkReorderRequest request) {
        LessonSection section = getActiveSection(sectionId, LessonSectionType.VOCABULARY);
        courseAccessGuard.requireOwnerOrAdmin(section.getLesson().getCourse(), ErrorCode.FORBIDDEN_LESSON_ACCESS);

        if (request.getItemIds() == null || request.getItemIds().isEmpty()) {
            throw new AppException(ErrorCode.LESSON_REORDER_LIST_EMPTY);
        }

        List<LessonVocabularyItem> currentItems = vocabItemRepository
                .findBySectionIdAndDeletedFalseOrderByOrderIndexAsc(sectionId);
        Set<Long> currentIds = new HashSet<>();
        for (LessonVocabularyItem i : currentItems) currentIds.add(i.getId());

        Set<Long> requestedIds = new HashSet<>(request.getItemIds());
        if (requestedIds.size() != request.getItemIds().size()) {
            throw new AppException(ErrorCode.LESSON_REORDER_LIST_INVALID);
        }
        if (!requestedIds.equals(currentIds)) {
            throw new AppException(ErrorCode.LESSON_REORDER_LIST_MISMATCH);
        }

        OrderIndexResolver.bulkReorder(
                currentItems,
                request.getItemIds(),
                LessonVocabularyItem::getId,
                LessonVocabularyItem::getOrderIndex,
                LessonVocabularyItem::setOrderIndex
        );

        List<LessonVocabularyItem> saved = vocabItemRepository.saveAll(currentItems);
        vocabItemRepository.flush();

        log.info("[VOCAB-ITEM] Bulk reordered {} vocab item(s) in section id={}", saved.size(), sectionId);
        // Map sang response (examples đã load sẵn trong entity)
        return saved.stream()
                .map(lessonSectionMapper::toVocabularyItemResponse)
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    // ============================================================
    // EXAMPLES HELPERS
    // ============================================================

    /**
     * Xử lý examples khi tạo item:
     * <ul>
     *   <li>{@code teacherExamples == null} HOẶC {@code teacherExamples.isEmpty()}
     *       → auto-copy tất cả examples (chưa xoá) từ {@code vocab}.</li>
     *   <li>{@code teacherExamples != null && !isEmpty()} → tạo từng example theo
     *       danh sách teacher cung cấp (luôn là tạo mới vì đây là create).</li>
     * </ul>
     *
     * @return số lượng examples đã tạo
     */
    private int applyExamplesOnCreate(LessonVocabularyItem item, Vocabulary vocab,
                                      List<LessonVocabularyExampleCreateRequest> teacherExamples) {
        if (teacherExamples == null || teacherExamples.isEmpty()) {
            // Auto-copy từ vocabulary
            return autoCopyExamplesFromVocab(item, vocab);
        }
        return createTeacherExamples(item, teacherExamples);
    }

    /**
     * Auto-copy tất cả examples (chưa bị xoá) từ {@code vocabulary} sang {@code item}.
     */
    private int autoCopyExamplesFromVocab(LessonVocabularyItem item, Vocabulary vocab) {
        if (vocab.getExamples() == null || vocab.getExamples().isEmpty()) {
            return 0;
        }
        List<VocabularyExample> source = vocab.getExamples().stream()
                .filter(e -> !e.isDeleted())
                .sorted(Comparator.comparing(
                        VocabularyExample::getOrderIndex,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        int count = 0;
        for (VocabularyExample src : source) {
            int exOrderIndex = src.getOrderIndex() != null && src.getOrderIndex() > 0
                    ? src.getOrderIndex()
                    : OrderIndexResolver.nextOrderIndexIncludingDeleted(
                            item.getExamples(), LessonVocabularyExample::getOrderIndex);
            LessonVocabularyExample ex = LessonVocabularyExample.builder()
                    .vocabItem(item)
                    .exampleJp(src.getExampleJp())
                    .exampleVi(src.getExampleVi())
                    .exampleRomaji(src.getExampleRomaji())
                    .audioUrl(src.getAudioUrl())
                    .orderIndex(exOrderIndex)
                    .primary(src.isPrimary())
                    .build();
            item.addExample(ex);
            count++;
        }
        log.info("[VOCAB-ITEM] Auto-copied {} examples from vocab id={} to new item",
                count, vocab.getId());
        return count;
    }

    /**
     * Tạo example từ danh sách teacher cung cấp (dùng cho create).
     * Lưu ý: khi create thì id luôn null → tất cả đều là tạo mới.
     * Nếu có 2 entry cùng primary=true → chỉ entry đầu tiên giữ primary.
     */
    private int createTeacherExamples(LessonVocabularyItem item,
                                      List<LessonVocabularyExampleCreateRequest> teacherExamples) {
        if (teacherExamples.size() > MAX_EXAMPLES_PER_REQUEST) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
        int count = 0;
        boolean firstPrimaryApplied = false;

        // Track orderIndex đã dùng (existing trong item + vừa tạo) để chống trùng
        Set<Integer> usedOrderIndexes = new HashSet<>();
        if (item.getExamples() != null) {
            for (LessonVocabularyExample ex : item.getExamples()) {
                if (!ex.isDeleted() && ex.getOrderIndex() != null && ex.getOrderIndex() > 0) {
                    usedOrderIndexes.add(ex.getOrderIndex());
                }
            }
        }

        for (LessonVocabularyExampleCreateRequest req : teacherExamples) {
            if (req == null) continue;
            // Resolve orderIndex chống trùng
            Integer requested = req.getOrderIndex();
            int exOrderIndex;
            if (requested != null && requested > 0 && usedOrderIndexes.add(requested)) {
                exOrderIndex = requested;
            } else {
                exOrderIndex = OrderIndexResolver.nextOrderIndexIncludingDeleted(
                        item.getExamples(), LessonVocabularyExample::getOrderIndex);
                usedOrderIndexes.add(exOrderIndex);
            }
            // Primary rule: nếu có nhiều entry primary=true → chỉ giữ entry đầu tiên
            boolean wantPrimary = Boolean.TRUE.equals(req.getIsPrimary()) && !firstPrimaryApplied;
            if (Boolean.TRUE.equals(req.getIsPrimary())) {
                firstPrimaryApplied = true;
            }
            LessonVocabularyExample ex = LessonVocabularyExample.builder()
                    .vocabItem(item)
                    .exampleJp(req.getExampleJp())
                    .exampleVi(req.getExampleVi())
                    .exampleRomaji(req.getExampleRomaji())
                    .audioUrl(req.getAudioUrl())
                    .orderIndex(exOrderIndex)
                    .primary(wantPrimary)
                    .build();
            item.addExample(ex);
            count++;
        }
        return count;
    }

    /**
     * Xử lý examples khi update item (3-state, entry point):
     * <ul>
     *   <li>{@code teacherExamples == null} → giữ nguyên, KHÔNG làm gì. Trả về 0.</li>
     *   <li>{@code teacherExamples.isEmpty()} → RESET: soft-delete tất cả examples hiện tại,
     *       rồi auto-copy lại từ {@code vocabulary.examples} (lấy example tĩnh mới nhất).</li>
     *   <li>{@code teacherExamples != null && !isEmpty()} → upsert từng entry.</li>
     * </ul>
     */
    private int handleExamplesUpdate(LessonVocabularyItem item,
                                     List<LessonVocabularyExampleUpsertRequest> teacherExamples) {
        if (teacherExamples == null) {
            return 0; // giữ nguyên
        }
        if (teacherExamples.size() > MAX_EXAMPLES_PER_REQUEST) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
        if (teacherExamples.isEmpty()) {
            // RESET: xoá tất cả examples hiện tại rồi auto-copy lại từ vocab
            int deleted = softDeleteAllExamples(item);
            // Load vocab kèm examples để auto-copy
            Vocabulary vocab = vocabularyRepository
                    .findByIdWithActiveExamples(item.getVocab().getId())
                    .orElseThrow(() -> new AppException(ErrorCode.VOCAB_NOT_FOUND));
            int copied = autoCopyExamplesFromVocab(item, vocab);
            log.info("[VOCAB-ITEM] Reset examples for item id={}: deleted={}, auto-copied={}",
                    item.getId(), deleted, copied);
            return deleted + copied;
        }
        return applyExamplesOnUpdate(item, teacherExamples);
    }

    /**
     * Xử lý examples khi update item — chỉ xử lý trường hợp upsert.
     * Trường hợp null/empty đã được xử lý ở {@link #handleExamplesUpdate}.
     *
     * <p>Quy tắc upsert:</p>
     * <ul>
     *   <li>id = null → tạo mới</li>
     *   <li>id != null → cập nhật example hiện tại (404 nếu không tồn tại)</li>
     *   <li>Example hiện tại không có trong list → soft-delete</li>
     * </ul>
     *
     * @return số lượng example đã thay đổi (tạo + cập nhật + xoá)
     */
    private int applyExamplesOnUpdate(LessonVocabularyItem item,
                                      List<LessonVocabularyExampleUpsertRequest> teacherExamples) {
        if (teacherExamples == null) {
            return 0; // giữ nguyên
        }
        if (teacherExamples.size() > MAX_EXAMPLES_PER_REQUEST) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        // Snapshot examples hiện tại (chưa xoá)
        List<LessonVocabularyExample> existing = item.getExamples() == null
                ? List.of()
                : item.getExamples().stream()
                        .filter(e -> !e.isDeleted())
                        .toList();
        Set<Long> existingIds = new HashSet<>();
        for (LessonVocabularyExample ex : existing) {
            if (ex.getId() != null) existingIds.add(ex.getId());
        }

        Set<Long> keepIds = new HashSet<>();
        int created = 0;
        int updated = 0;
        int deleted = 0;

        // Track các orderIndex đã dùng trong request này (để tránh duplicate nếu user
        // gửi 2 example mới cùng orderIndex, hoặc gửi orderIndex trùng với existing).
        // Lưu cả existing (active) + các example mới vừa tạo trong loop này.
        Set<Integer> usedOrderIndexes = new HashSet<>();
        for (LessonVocabularyExample ex : existing) {
            if (ex.getOrderIndex() != null && ex.getOrderIndex() > 0) {
                usedOrderIndexes.add(ex.getOrderIndex());
            }
        }

        // Quy tắc primary:
        // - Nếu user set primary=true cho ÍT NHẤT 1 example (qua upsert) → những example đó là primary,
        //   các example còn lại (kể cả existing) sẽ bị set primary=false.
        // - Nếu user KHÔNG set primary=true cho example nào → giữ nguyên primary của existing.
        boolean userSetAnyPrimary = teacherExamples.stream()
                .anyMatch(r -> r != null && Boolean.TRUE.equals(r.getIsPrimary()));
        boolean hasPrimary = !userSetAnyPrimary
                && existing.stream().anyMatch(LessonVocabularyExample::isPrimary);

        for (LessonVocabularyExampleUpsertRequest req : teacherExamples) {
            if (req == null) continue;
            if (req.getId() != null) {
                // Update existing
                LessonVocabularyExample ex = existing.stream()
                        .filter(e -> e.getId().equals(req.getId()))
                        .findFirst()
                        .orElseThrow(() -> new AppException(ErrorCode.LESSON_VOCAB_EXAMPLE_NOT_FOUND));
                applyExampleUpdate(ex, req, userSetAnyPrimary, hasPrimary);
                if (ex.isPrimary()) {
                    hasPrimary = true;
                }
                keepIds.add(req.getId());
                updated++;
            } else {
                // Create new
                // Resolve orderIndex chống trùng:
                //  Ưu tiên 1: dùng giá trị client gửi (nếu > 0 và chưa bị chiếm)
                //  Ưu tiên 2: nếu client gửi trùng → fallback max+1
                //  Ưu tiên 3: nếu client không gửi → max+1
                // Mục đích: tránh duplicate orderIndex khi insert vào giữa.
                Integer requested = req.getOrderIndex();
                int exOrderIndex;
                if (requested != null && requested > 0 && usedOrderIndexes.add(requested)) {
                    exOrderIndex = requested;
                } else {
                    exOrderIndex = OrderIndexResolver.nextOrderIndexIncludingDeleted(
                            item.getExamples(), LessonVocabularyExample::getOrderIndex);
                    // Cộng dồn vào cả tập usedOrderIndexes để lần lặp sau không trùng
                    usedOrderIndexes.add(exOrderIndex);
                }
                boolean wantPrimary = Boolean.TRUE.equals(req.getIsPrimary())
                        && (userSetAnyPrimary ? !hasPrimary : !hasPrimary);
                // Logic: nếu user set primary=true cho entry này VÀ chưa có primary nào → primary
                if (Boolean.TRUE.equals(req.getIsPrimary()) && !hasPrimary) {
                    wantPrimary = true;
                    hasPrimary = true;
                } else {
                    wantPrimary = false;
                }
                LessonVocabularyExample ex = LessonVocabularyExample.builder()
                        .vocabItem(item)
                        .exampleJp(req.getExampleJp())
                        .exampleVi(req.getExampleVi())
                        .exampleRomaji(req.getExampleRomaji())
                        .audioUrl(req.getAudioUrl())
                        .orderIndex(exOrderIndex)
                        .primary(wantPrimary)
                        .build();
                item.addExample(ex);
                created++;
            }
        }

        // Soft-delete examples hiện tại không có trong keepIds
        for (LessonVocabularyExample ex : existing) {
            if (ex.getId() != null && !keepIds.contains(ex.getId())) {
                ex.softDelete();
                deleted++;
            }
        }

        // Nếu user set primary=true cho ít nhất 1 example → tất cả example KHÔNG được set
        // primary=true (ngoại trừ các example đã được đánh dấu primary trong loop trên) phải bị set false.
        if (userSetAnyPrimary && item.getExamples() != null) {
            for (LessonVocabularyExample ex : item.getExamples()) {
                if (ex.isDeleted()) continue;
                if (!ex.isPrimary()) {
                    // OK, không cần làm gì
                    continue;
                }
                // Đảm bảo chỉ giữ 1 primary: nếu có > 1 primary → set false cho các cái sau
            }
            // Nếu chỉ có 1 primary (do user set đúng 1) → OK
            // Nếu không có primary nào (do logic if sai) → fallback: set primary=true cho example đầu tiên
            boolean anyPrimaryNow = item.getExamples().stream()
                    .anyMatch(e -> !e.isDeleted() && e.isPrimary());
            if (!anyPrimaryNow) {
                item.getExamples().stream()
                        .filter(e -> !e.isDeleted())
                        .findFirst()
                        .ifPresent(e -> e.setPrimary(true));
            }
        }

        log.info("[VOCAB-ITEM] Update examples for item id={}: created={}, updated={}, deleted={}",
                item.getId(), created, updated, deleted);
        return created + updated + deleted;
    }

    private void applyExampleUpdate(LessonVocabularyExample ex, LessonVocabularyExampleUpsertRequest req,
                                    boolean userSetAnyPrimary, boolean hasPrimary) {
        if (req.getExampleJp() != null) ex.setExampleJp(req.getExampleJp());
        if (req.getExampleVi() != null) ex.setExampleVi(req.getExampleVi());
        if (req.getExampleRomaji() != null) ex.setExampleRomaji(req.getExampleRomaji());
        if (req.getAudioUrl() != null) {
            ex.setAudioUrl(req.getAudioUrl().isEmpty() ? null : req.getAudioUrl());
        }
        if (req.getOrderIndex() != null && req.getOrderIndex() > 0) ex.setOrderIndex(req.getOrderIndex());
        if (req.getIsPrimary() != null) {
            if (Boolean.TRUE.equals(req.getIsPrimary())) {
                // User muốn example này primary
                if (userSetAnyPrimary) {
                    // Sẽ được clear các example khác sau; ở đây cứ set true
                    ex.setPrimary(true);
                } else {
                    ex.setPrimary(!hasPrimary);
                }
            } else {
                ex.setPrimary(false);
            }
        }
    }

    private int softDeleteAllExamples(LessonVocabularyItem item) {
        if (item.getExamples() == null) return 0;
        int count = 0;
        for (LessonVocabularyExample ex : item.getExamples()) {
            if (!ex.isDeleted()) {
                ex.softDelete();
                count++;
            }
        }
        return count;
    }

    // ============================================================
    // HELPER
    // ============================================================

    private void assertNoDuplicateVocabulary(Long sectionId, Long vocabId) {
        boolean exists = vocabItemRepository
                .findBySectionIdAndDeletedFalseOrderByOrderIndexAsc(sectionId)
                .stream()
                .anyMatch(i -> !i.isDeleted()
                        && i.getVocab() != null
                        && i.getVocab().getId().equals(vocabId));
        if (exists) {
            throw new AppException(ErrorCode.LESSON_VOCABULARY_ITEM_DUPLICATED);
        }
    }

    /**
     * Resolve giá trị override cho các trường customXxx (V23+):
     * <ul>
     *   <li>{@code clientValue == null} → auto-fill từ {@code fallback} (giá trị từ từ điển)</li>
     *   <li>{@code clientValue.isBlank()} → null (xoá override, mapper sẽ fallback về từ điển lúc hiển thị)</li>
     *   <li>Còn lại → giữ nguyên giá trị client gửi</li>
     * </ul>
     */
    private String resolveOverride(String clientValue, String fallback) {
        if (clientValue == null) {
            return fallback; // auto-fill từ từ điển
        }
        if (clientValue.isBlank()) {
            return null; // xoá override
        }
        return clientValue; // dùng override
    }
}