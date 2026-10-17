package com.jp.elearningjp.service.curriculum.section;

import com.jp.elearningjp.dto.request.curriculum.ItemBulkReorderRequest;
import com.jp.elearningjp.dto.request.curriculum.LessonGrammarExampleCreateRequest;
import com.jp.elearningjp.dto.request.curriculum.LessonGrammarExampleUpsertRequest;
import com.jp.elearningjp.dto.request.curriculum.LessonGrammarItemCreateRequest;
import com.jp.elearningjp.dto.request.curriculum.LessonGrammarItemUpdateRequest;
import com.jp.elearningjp.dto.response.curriculum.LessonGrammarItemResponse;
import com.jp.elearningjp.entity.content.GrammarPoint;
import com.jp.elearningjp.entity.curriculum.LessonGrammarExample;
import com.jp.elearningjp.entity.curriculum.LessonGrammarItem;
import com.jp.elearningjp.entity.curriculum.LessonSection;
import com.jp.elearningjp.exception.AppException;
import com.jp.elearningjp.exception.ErrorCode;
import com.jp.elearningjp.mapper.LessonSectionMapper;
import com.jp.elearningjp.repository.content.GrammarPointRepository;
import com.jp.elearningjp.repository.curriculum.LessonGrammarExampleRepository;
import com.jp.elearningjp.repository.curriculum.LessonGrammarItemRepository;
import com.jp.elearningjp.repository.curriculum.LessonSectionRepository;
import com.jp.elearningjp.shared.enums.LessonSectionType;
import com.jp.elearningjp.shared.util.CourseAccessGuard;
import com.jp.elearningjp.shared.util.OrderIndexResolver;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Service quản lý {@link LessonGrammarItem} (kèm {@link LessonGrammarExample})
 * bên trong section GRAMMAR.
 *
 * <p>Grammar item có 4 endpoint: POST, GET, PUT, DELETE.
 * Examples được xử lý trong payload create/update (giống example của vocab item)
 * — KHÔNG còn endpoint riêng cho example. Khi xoá grammar item → cascade soft-delete
 * tất cả examples thuộc item đó.</p>
 */
@Service
@Slf4j
public class GrammarSectionItemService extends AbstractSectionItemService {

    /** Số example tối đa cho phép trong 1 request create/update. */
    private static final int MAX_EXAMPLES_PER_REQUEST = 100;

    private final LessonGrammarItemRepository grammarItemRepository;
    private final LessonGrammarExampleRepository grammarExampleRepository;
    private final GrammarPointRepository grammarPointRepository;
    private final LessonSectionMapper lessonSectionMapper;
    private final CourseAccessGuard courseAccessGuard;

    public GrammarSectionItemService(
            LessonSectionRepository lessonSectionRepository,
            LessonGrammarItemRepository grammarItemRepository,
            LessonGrammarExampleRepository grammarExampleRepository,
            GrammarPointRepository grammarPointRepository,
            LessonSectionMapper lessonSectionMapper,
            CourseAccessGuard courseAccessGuard
    ) {
        super(lessonSectionRepository);
        this.grammarItemRepository = grammarItemRepository;
        this.grammarExampleRepository = grammarExampleRepository;
        this.grammarPointRepository = grammarPointRepository;
        this.lessonSectionMapper = lessonSectionMapper;
        this.courseAccessGuard = courseAccessGuard;
    }

    // ============================================================
    // GRAMMAR ITEM CRUD (4 endpoints chính)
    // ============================================================

    /**
     * Thêm grammar item vào section GRAMMAR, kèm examples do giáo viên cấu hình.
     *
     * <p>Semantics cho các trường override (V23+):</p>
     * <ul>
     *   <li>Nếu client gửi {@code customXxx = null} → auto-fill từ {@code grammarPoint}
     *       (lưu vào DB, dùng để hiển thị). Sửa đổi từ điển sẽ KHÔNG tự động cập nhật item.</li>
     *   <li>Nếu client gửi {@code customXxx = ""} → set NULL (xoá override, hiển thị lấy từ từ điển).</li>
     *   <li>Nếu client gửi giá trị → lưu override đó.</li>
     * </ul>
     *
     * <p>Semantics cho {@code examples}:</p>
     * <ul>
     *   <li>{@code examples == null} (không gửi) HOẶC {@code examples = []} → auto-copy
     *       tất cả examples từ {@code grammarPoint.examplesJson} sang item.</li>
     *   <li>{@code examples = [...]} → dùng danh sách này (không auto-copy từ từ điển).</li>
     * </ul>
     */
    @Transactional
    public LessonGrammarItemResponse addGrammarItem(Long sectionId, LessonGrammarItemCreateRequest request) {
        LessonSection section = getActiveSection(sectionId, LessonSectionType.GRAMMAR);

        GrammarPoint grammar = grammarPointRepository.findByIdAndDeletedFalse(request.getGrammarId())
                .orElseThrow(() -> new AppException(ErrorCode.GRAMMAR_NOT_FOUND));

        assertNoDuplicateGrammar(sectionId, grammar.getId());

        // Nhận xét #2: xử lý orderIndex bằng ADJACENT SWAP (swap chain từng bước).
        // Ví dụ chi tiết: xem comment trong LessonService.createLesson.
        List<LessonGrammarItem> existingItems = grammarItemRepository
                .findBySectionIdAndDeletedFalseOrderByOrderIndexAsc(sectionId);
        int maxOrder = OrderIndexResolver.maxOrderIndex(existingItems, LessonGrammarItem::getOrderIndex);
        Integer requestedIndex = request.getOrderIndex();
        boolean clientChoseIndex = requestedIndex != null && requestedIndex > 0
                && requestedIndex <= maxOrder + 1;

        int tempIndex = maxOrder + 1;
        int finalIndex = clientChoseIndex ? requestedIndex : tempIndex;

        // Resolve các trường override: null = auto-fill từ từ điển, "" = xoá override
        String resolvedMeaning   = resolveOverride(request.getCustomMeaning(), grammar.getMeaningVi());
        String resolvedStructure = resolveOverride(request.getCustomStructure(), grammar.getStructure());

        LessonGrammarItem item = LessonGrammarItem.builder()
                .section(section)
                .grammar(grammar)
                .orderIndex(tempIndex)
                .customMeaning(resolvedMeaning)
                .customStructure(resolvedStructure)
                .note(request.getNote())
                .build();

        // Xử lý examples: teacher-controlled (ưu tiên) HOẶC auto-copy từ grammar
        int totalExamples = applyExamplesOnCreate(item, grammar, request.getExamples());

        section.addGrammarItem(item);
        LessonGrammarItem saved = grammarItemRepository.save(item);
        grammarItemRepository.flush();
        log.info("[GRAMMAR-ITEM] Added grammar id={} into section id={} at tempIndex={} (with {} examples)",
                grammar.getId(), sectionId, tempIndex, totalExamples);

        if (clientChoseIndex && finalIndex != tempIndex) {
            Long newItemId = saved.getId();
            int step = (finalIndex < tempIndex) ? -1 : 1;
            int current = tempIndex;
            int safetyCounter = 0;
            final int MAX_SWAPS = tempIndex + 2;
            while (current != finalIndex && safetyCounter++ < MAX_SWAPS) {
                int nextIndex = current + step;
                Long neighborId = grammarItemRepository.findIdBySectionIdAndOrderIndex(sectionId, nextIndex);
                if (neighborId == null) break;
                grammarItemRepository.swapOrderIndexes(newItemId, nextIndex, neighborId, current);
                grammarItemRepository.flush();
                current = nextIndex;
            }
            saved.setOrderIndex(finalIndex);
            grammarItemRepository.flush();
            log.info("[GRAMMAR-ITEM] Swapped new grammar item id={} from tempIndex={} to orderIndex={} (sectionId={})",
                    newItemId, tempIndex, finalIndex, sectionId);
        }

        return lessonSectionMapper.toGrammarItemResponse(saved);
    }

    /**
     * Lấy chi tiết 1 grammar item theo id.
     */
    @Transactional(readOnly = true)
    public LessonGrammarItemResponse getGrammarItem(Long itemId) {
        LessonGrammarItem item = grammarItemRepository.findById(itemId)
                .orElseThrow(() -> new AppException(ErrorCode.LESSON_GRAMMAR_ITEM_NOT_FOUND));
        if (item.isDeleted()) {
            throw new AppException(ErrorCode.LESSON_GRAMMAR_ITEM_NOT_FOUND);
        }
        return lessonSectionMapper.toGrammarItemResponse(item);
    }

    /**
     * Cập nhật grammar item.
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
     *       từ {@code grammarPoint.examplesJson} (lấy example tĩnh mới nhất).</li>
     *   <li>{@code examples = [...]} → upsert từng entry theo {@code id}.
     *       Example hiện tại không có trong list sẽ bị soft-delete.</li>
     * </ul>
     */
    @Transactional
    public LessonGrammarItemResponse updateGrammarItem(Long itemId, LessonGrammarItemUpdateRequest request) {
        LessonGrammarItem item = grammarItemRepository.findById(itemId)
                .orElseThrow(() -> new AppException(ErrorCode.LESSON_GRAMMAR_ITEM_NOT_FOUND));
        if (item.isDeleted()) {
            throw new AppException(ErrorCode.LESSON_GRAMMAR_ITEM_NOT_FOUND);
        }

        Integer newOrderIndex = request.getOrderIndex();
        Integer oldOrderIndex = item.getOrderIndex();

        // Nhận xét #2: xử lý di chuyển orderIndex bằng ADJACENT SWAP (xem comment trong KanjiItemService)
        if (newOrderIndex != null && newOrderIndex > 0 && !newOrderIndex.equals(oldOrderIndex)) {
            Long sectionId = item.getSection().getId();
            List<LessonGrammarItem> allItems = grammarItemRepository
                    .findBySectionIdAndDeletedFalseOrderByOrderIndexAsc(sectionId);
            int maxOrder = OrderIndexResolver.maxOrderIndex(allItems, LessonGrammarItem::getOrderIndex);
            int target = OrderIndexResolver.clampToMaxPlusOne(newOrderIndex, () -> maxOrder);
            if (target != oldOrderIndex) {
                log.info("[GRAMMAR-ITEM] Adjacent-swap: moving grammar item id={} from orderIndex={} → target={} (sectionId={})",
                        itemId, oldOrderIndex, target, sectionId);
                int step = (target < oldOrderIndex) ? -1 : 1;
                int current = oldOrderIndex;
                int safetyCounter = 0;
                final int MAX_SWAPS = maxOrder + 2;
                while (current != target && safetyCounter++ < MAX_SWAPS) {
                    int nextIndex = current + step;
                    Long neighborId = grammarItemRepository.findIdBySectionIdAndOrderIndex(sectionId, nextIndex);
                    if (neighborId == null) break;
                    grammarItemRepository.swapOrderIndexes(itemId, nextIndex, neighborId, current);
                    grammarItemRepository.flush();
                    current = nextIndex;
                }
                item.setOrderIndex(target);
            }
        }

        // V23+: customXxx — null = giữ nguyên, "" = xoá override, có giá trị = set
        if (request.getCustomMeaning() != null) {
            item.setCustomMeaning(request.getCustomMeaning().isEmpty() ? null : request.getCustomMeaning());
        }
        if (request.getCustomStructure() != null) {
            item.setCustomStructure(request.getCustomStructure().isEmpty() ? null : request.getCustomStructure());
        }
        if (request.getNote() != null) {
            item.setNote(request.getNote().isEmpty() ? null : request.getNote());
        }

        // Xử lý examples (3-state)
        int examplesAffected = handleExamplesUpdate(item, request.getExamples());

        LessonGrammarItem saved = grammarItemRepository.save(item);
        log.info("[GRAMMAR-ITEM] Updated grammar item id={} (examples affected: {})", itemId, examplesAffected);
        return lessonSectionMapper.toGrammarItemResponse(saved);
    }

    /**
     * Xoá mềm grammar item + cascade soft-delete toàn bộ examples thuộc item.
     */
    @Transactional
    public void deleteGrammarItem(Long itemId) {
        LessonGrammarItem item = grammarItemRepository.findById(itemId)
                .orElseThrow(() -> new AppException(ErrorCode.LESSON_GRAMMAR_ITEM_NOT_FOUND));
        if (item.isDeleted()) return;
        item.softDelete();
        // Cascade soft-delete examples
        int cascadeCount = softDeleteAllExamples(item);
        grammarItemRepository.save(item);
        log.info("[GRAMMAR-ITEM] Soft-deleted grammar item id={} (cascaded {} examples)",
                itemId, cascadeCount);
    }

    /**
     * Sắp xếp lại thứ tự hàng loạt các grammar item trong 1 section (drag-drop UI).
     *
     * <p>Quy tắc: list {@code itemIds} phải chứa <b>đúng tất cả</b> grammar item active
     * của section hiện tại (không thiếu, không thừa, không trùng).</p>
     *
     * <p>Quyền: Admin hoặc Teacher (chỉ reorder trong course do mình tạo).</p>
     */
    @Transactional
    public List<LessonGrammarItemResponse> reorderGrammarItems(Long sectionId, ItemBulkReorderRequest request) {
        LessonSection section = getActiveSection(sectionId, LessonSectionType.GRAMMAR);
        courseAccessGuard.requireOwnerOrAdmin(section.getLesson().getCourse(), ErrorCode.FORBIDDEN_LESSON_ACCESS);

        if (request.getItemIds() == null || request.getItemIds().isEmpty()) {
            throw new AppException(ErrorCode.LESSON_REORDER_LIST_EMPTY);
        }

        List<LessonGrammarItem> currentItems = grammarItemRepository
                .findBySectionIdAndDeletedFalseOrderByOrderIndexAsc(sectionId);
        Set<Long> currentIds = new HashSet<>();
        for (LessonGrammarItem i : currentItems) currentIds.add(i.getId());

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
                LessonGrammarItem::getId,
                LessonGrammarItem::getOrderIndex,
                LessonGrammarItem::setOrderIndex
        );

        List<LessonGrammarItem> saved = grammarItemRepository.saveAll(currentItems);
        grammarItemRepository.flush();

        log.info("[GRAMMAR-ITEM] Bulk reordered {} grammar item(s) in section id={}", saved.size(), sectionId);
        return saved.stream()
                .map(lessonSectionMapper::toGrammarItemResponse)
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
     *       → auto-copy tất cả examples từ {@code grammar.examplesJson}.</li>
     *   <li>{@code teacherExamples != null && !isEmpty()} → tạo từng example theo
     *       danh sách teacher cung cấp (luôn là tạo mới vì đây là create).</li>
     * </ul>
     *
     * @return số lượng examples đã tạo
     */
    private int applyExamplesOnCreate(LessonGrammarItem item, GrammarPoint grammar,
                                      List<LessonGrammarExampleCreateRequest> teacherExamples) {
        if (teacherExamples == null || teacherExamples.isEmpty()) {
            // Auto-copy từ grammar.examplesJson
            return autoCopyExamplesFromGrammar(item, grammar);
        }
        return createTeacherExamples(item, teacherExamples);
    }

    /**
     * Auto-copy tất cả examples từ {@code grammarPoint.examplesJson} (cột JSONB) sang {@code item}.
     *
     * <p>Cấu trúc mỗi example trong JSON: {@code {"jp": "...", "vi": "..."}}.
     * Chỉ copy 2 trường này (grammar_point không có romaji, audioUrl, isPrimary).</p>
     *
     * @return số lượng examples đã copy
     */
    private int autoCopyExamplesFromGrammar(LessonGrammarItem item, GrammarPoint grammar) {
        JsonNode examplesJson = grammar.getExamplesJson();
        if (examplesJson == null || !examplesJson.isArray() || examplesJson.isEmpty()) {
            return 0;
        }

        int count = 0;
        int order = 1;
        Iterator<JsonNode> it = examplesJson.elements();
        while (it.hasNext()) {
            JsonNode node = it.next();
            String jp = node.path("jp").asText(null);
            String vi = node.path("vi").asText(null);
            if (jp == null || jp.isBlank() || vi == null || vi.isBlank()) {
                log.warn("[GRAMMAR-ITEM] Skip malformed example at index {} from grammar id={}: jp={}, vi={}",
                        order - 1, grammar.getId(), jp, vi);
                continue;
            }
            LessonGrammarExample ex = LessonGrammarExample.builder()
                    .grammarItem(item)
                    .exampleJp(jp)
                    .exampleVi(vi)
                    .orderIndex(order)
                    .build();
            item.addExample(ex);
            count++;
            order++;
        }
        log.info("[GRAMMAR-ITEM] Auto-copied {} examples from grammar id={} (JSONB) to new item",
                count, grammar.getId());
        return count;
    }

    /**
     * Tạo example từ danh sách teacher cung cấp (dùng cho create).
     * Lưu ý: khi create thì id luôn null → tất cả đều là tạo mới.
     */
    private int createTeacherExamples(LessonGrammarItem item,
                                      List<LessonGrammarExampleCreateRequest> teacherExamples) {
        if (teacherExamples.size() > MAX_EXAMPLES_PER_REQUEST) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
        int count = 0;
        for (LessonGrammarExampleCreateRequest req : teacherExamples) {
            if (req == null) continue;
            // Validate duplicate trong cùng item
            assertNoDuplicateExampleInItem(item, req.getExampleJp(), req.getExampleVi(), null);

            int exOrderIndex = req.getOrderIndex() != null && req.getOrderIndex() > 0
                    ? req.getOrderIndex()
                    : OrderIndexResolver.nextOrderIndexIncludingDeleted(
                            item.getExamples(), LessonGrammarExample::getOrderIndex);
            LessonGrammarExample ex = LessonGrammarExample.builder()
                    .grammarItem(item)
                    .exampleJp(req.getExampleJp())
                    .exampleVi(req.getExampleVi())
                    .orderIndex(exOrderIndex)
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
     *       rồi auto-copy lại từ {@code grammarPoint.examplesJson} (lấy example tĩnh mới nhất).</li>
     *   <li>{@code teacherExamples != null && !isEmpty()} → upsert từng entry.</li>
     * </ul>
     */
    private int handleExamplesUpdate(LessonGrammarItem item,
                                     List<LessonGrammarExampleUpsertRequest> teacherExamples) {
        if (teacherExamples == null) {
            return 0; // giữ nguyên
        }
        if (teacherExamples.size() > MAX_EXAMPLES_PER_REQUEST) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
        if (teacherExamples.isEmpty()) {
            // RESET: xoá tất cả examples hiện tại rồi auto-copy lại từ grammar
            int deleted = softDeleteAllExamples(item);
            int copied = autoCopyExamplesFromGrammar(item, item.getGrammar());
            log.info("[GRAMMAR-ITEM] Reset examples for item id={}: deleted={}, auto-copied={}",
                    item.getId(), deleted, copied);
            return deleted + copied;
        }
        return applyExamplesOnUpdate(item, teacherExamples);
    }

    /**
     * Xử lý examples khi update item — chỉ xử lý trường hợp upsert.
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
    private int applyExamplesOnUpdate(LessonGrammarItem item,
                                      List<LessonGrammarExampleUpsertRequest> teacherExamples) {
        // Snapshot examples hiện tại (chưa xoá)
        List<LessonGrammarExample> existing = item.getExamples() == null
                ? List.of()
                : item.getExamples().stream()
                        .filter(e -> !e.isDeleted())
                        .toList();
        Set<Long> existingIds = new HashSet<>();
        for (LessonGrammarExample ex : existing) {
            if (ex.getId() != null) existingIds.add(ex.getId());
        }

        Set<Long> keepIds = new HashSet<>();
        int created = 0;
        int updated = 0;
        int deleted = 0;

        // Track orderIndex đã dùng (existing + vừa tạo trong loop) để chống trùng
        Set<Integer> usedOrderIndexes = new HashSet<>();
        for (LessonGrammarExample ex : existing) {
            if (ex.getOrderIndex() != null && ex.getOrderIndex() > 0) {
                usedOrderIndexes.add(ex.getOrderIndex());
            }
        }

        for (LessonGrammarExampleUpsertRequest req : teacherExamples) {
            if (req == null) continue;
            if (req.getId() != null) {
                // Update existing
                LessonGrammarExample ex = existing.stream()
                        .filter(e -> e.getId().equals(req.getId()))
                        .findFirst()
                        .orElseThrow(() -> new AppException(ErrorCode.LESSON_GRAMMAR_EXAMPLE_NOT_FOUND));
                applyExampleUpdate(ex, req);
                keepIds.add(req.getId());
                updated++;
            } else {
                // Create new
                // Validate duplicate (ngoại trừ chính nó - nhưng ở đây là tạo mới nên exclude null)
                assertNoDuplicateExampleInItem(item, req.getExampleJp(), req.getExampleVi(), null);

                // Resolve orderIndex chống trùng (xem VocabularySectionItemService để biết chi tiết)
                Integer requested = req.getOrderIndex();
                int exOrderIndex;
                if (requested != null && requested > 0 && usedOrderIndexes.add(requested)) {
                    exOrderIndex = requested;
                } else {
                    exOrderIndex = OrderIndexResolver.nextOrderIndexIncludingDeleted(
                            item.getExamples(), LessonGrammarExample::getOrderIndex);
                    usedOrderIndexes.add(exOrderIndex);
                }
                LessonGrammarExample ex = LessonGrammarExample.builder()
                        .grammarItem(item)
                        .exampleJp(req.getExampleJp())
                        .exampleVi(req.getExampleVi())
                        .orderIndex(exOrderIndex)
                        .build();
                item.addExample(ex);
                created++;
            }
        }

        // Soft-delete examples hiện tại không có trong keepIds
        for (LessonGrammarExample ex : existing) {
            if (ex.getId() != null && !keepIds.contains(ex.getId())) {
                ex.softDelete();
                deleted++;
            }
        }

        log.info("[GRAMMAR-ITEM] Update examples for item id={}: created={}, updated={}, deleted={}",
                item.getId(), created, updated, deleted);
        return created + updated + deleted;
    }

    private void applyExampleUpdate(LessonGrammarExample ex, LessonGrammarExampleUpsertRequest req) {
        if (req.getExampleJp() != null || req.getExampleVi() != null) {
            String newJp = req.getExampleJp() != null ? req.getExampleJp() : ex.getExampleJp();
            String newVi = req.getExampleVi() != null ? req.getExampleVi() : ex.getExampleVi();
            // Validate duplicate (loại trừ chính example đang update)
            assertNoDuplicateExampleInItem(ex.getGrammarItem(), newJp, newVi, ex.getId());
        }
        if (req.getExampleJp() != null) ex.setExampleJp(req.getExampleJp());
        if (req.getExampleVi() != null) ex.setExampleVi(req.getExampleVi());
        if (req.getOrderIndex() != null && req.getOrderIndex() > 0) ex.setOrderIndex(req.getOrderIndex());
    }

    private int softDeleteAllExamples(LessonGrammarItem item) {
        if (item.getExamples() == null) return 0;
        int count = 0;
        for (LessonGrammarExample ex : item.getExamples()) {
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

    private void assertNoDuplicateGrammar(Long sectionId, Long grammarId) {
        boolean exists = grammarItemRepository
                .findBySectionIdAndDeletedFalseOrderByOrderIndexAsc(sectionId)
                .stream()
                .anyMatch(i -> !i.isDeleted()
                        && i.getGrammar() != null
                        && i.getGrammar().getId().equals(grammarId));
        if (exists) {
            throw new AppException(ErrorCode.LESSON_GRAMMAR_ITEM_DUPLICATED);
        }
    }

    /**
     * V24+: Validate không có example trùng trong cùng grammar item.
     * 2 example trùng = cùng exampleJp + exampleVi.
     * {@code excludeExampleId} = id của example đang update (để không tự đụng với chính mình).
     */
    private void assertNoDuplicateExampleInItem(
            LessonGrammarItem item, String jp, String vi, Long excludeExampleId
    ) {
        if (item == null || item.getExamples() == null) return;
        boolean duplicate = item.getExamples().stream()
                .filter(e -> !e.isDeleted())
                .filter(e -> excludeExampleId == null || !e.getId().equals(excludeExampleId))
                .anyMatch(e ->
                        (jp != null && jp.equals(e.getExampleJp()))
                        && (vi != null && vi.equals(e.getExampleVi())));
        if (duplicate) {
            throw new AppException(ErrorCode.LESSON_GRAMMAR_EXAMPLE_DUPLICATED);
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