package com.jp.elearningjp.shared.util;

import com.jp.elearningjp.exception.AppException;
import com.jp.elearningjp.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * Helper xử lý logic {@code orderIndex} cho các entity có thứ tự (Lesson, Section, ...).
 *
 * <p>Trước đây logic này bị lặp ở 4 nơi trong {@code LessonService}
 * ({@code createLesson}, {@code updateLesson}, {@code createSection}, {@code updateSection})
 * → dễ lệch nhau, đặc biệt phần marker tạm và kẹp về MAX+1.</p>
 *
 * <p>Cách dùng cho create:</p>
 * <pre>{@code
 * int orderIndex = OrderIndexResolver.resolveForCreate(
 *         requestedIndex,
 *         () -> lessonRepository.findMaxOrderIndexByCourseId(courseId)
 * );
 * // → service tự save shiftUp nếu cần
 * }</pre>
 *
 * <p>Cách dùng cho update (move up/down):</p>
 * <pre>{@code
 * OrderIndexResolver.Movement plan = OrderIndexResolver.planMove(
 *         entity, newIndex, () -> repo.findMaxOrderIndex(parentId), entityId
 * );
 * plan.execute(entity::setOrderIndex, repo::shiftUp, repo::shiftDown, (id) -> {});
 * }</pre>
 */
@Slf4j
public final class OrderIndexResolver {

    private OrderIndexResolver() {}

    // ============================================================
    // CREATE — tính orderIndex mới khi thêm mới
    // ============================================================

    /**
     * @param requested    index do client gửi (null hoặc &lt;=0 nghĩa là auto-assign)
     * @param currentMaxFn supplier trả về MAX hiện tại (Integer, có thể null nếu chưa có record)
     * @return index hợp lệ, đã được kẹp về {@code max+1} nếu vượt quá
     */
    public static int resolveForCreate(Integer requested, java.util.function.Supplier<Integer> currentMaxFn) {
        int currentMax = nullToZero(currentMaxFn.get());
        if (requested == null || requested <= 0) {
            return currentMax + 1;
        }
        if (requested > currentMax + 1) {
            return currentMax + 1;
        }
        return requested;
    }

    /**
     * Sau khi {@link #resolveForCreate} trả về index hợp lệ, gọi hàm này để
     * dời các record có index &gt;= target lên +1 (giải phóng slot).
     *
     * @param shiftUpFn  callback thực thi shift (return số record bị ảnh hưởng)
     * @return số record đã shift
     */
    public static int shiftUpForCreate(int target, java.util.function.IntSupplier shiftUpFn) {
        int shifted = shiftUpFn.getAsInt();
        if (shifted > 0) {
            log.debug("[ORDER] Shifted {} record(s) with orderIndex >= {}", shifted, target);
        }
        return shifted;
    }

    // ============================================================
    // UPDATE — di chuyển 1 record từ oldIndex sang newIndex
    // ============================================================

    /**
     * Kết quả tính toán di chuyển: hoặc NOOP, hoặc MOVE_UP, hoặc MOVE_DOWN.
     */
    public enum Movement {
        NOOP, MOVE_UP, MOVE_DOWN;

        public boolean shouldShift() {
            return this != NOOP;
        }
    }

    /**
     * Tính xem cần di chuyển theo hướng nào khi update.
     *
     * @param oldIndex     orderIndex hiện tại của entity
     * @param newIndex     orderIndex mới do client yêu cầu
     * @param currentMaxFn supplier trả về MAX hiện tại (sau khi đã tính excluding entity này)
     * @return Movement (NOOP / MOVE_UP / MOVE_DOWN)
     */
    public static Movement planMove(int oldIndex, int newIndex, java.util.function.Supplier<Integer> currentMaxFn) {
        if (newIndex == oldIndex) {
            return Movement.NOOP;
        }
        int currentMax = nullToZero(currentMaxFn.get());
        int target = (newIndex > currentMax + 1) ? (currentMax + 1) : newIndex;
        if (target == oldIndex) {
            return Movement.NOOP;
        }
        return (target < oldIndex) ? Movement.MOVE_UP : Movement.MOVE_DOWN;
    }

    /**
     * Tính {@code target} sau khi kẹp về {@code max+1}.
     */
    public static int clampToMaxPlusOne(int newIndex, java.util.function.Supplier<Integer> currentMaxFn) {
        int currentMax = nullToZero(currentMaxFn.get());
        return (newIndex > currentMax + 1) ? (currentMax + 1) : newIndex;
    }

    // ============================================================
    // Conflict check
    // ============================================================

    /**
     * Check phòng race condition: có record nào (khác excludeId) đang giữ index này không.
     *
     * @param existsFn callback thực thi existsByParentAndOrderIndexExcludingId
     * @throws AppException với ErrorCode.LESSON_ORDER_CONFLICT nếu có conflict
     */
    public static void assertNoConflict(java.util.function.BooleanSupplier existsFn) {
        if (existsFn.getAsBoolean()) {
            throw new AppException(ErrorCode.LESSON_ORDER_CONFLICT);
        }
    }

    // ============================================================
    // Stream-based MAX (dùng cho Section - vì sectionRepo không có findMaxOrder)
    // ============================================================

    /**
     * Tính MAX orderIndex từ một list &lt;T&gt; có method {@code getOrderIndex()}.
     * Trả về 0 nếu list null/rỗng.
     *
     * <p>Đây là method generic duy nhất — KHÔNG thêm overload chuyên dụng
     * cho từng entity vì Java type erasure sẽ gây name clash.</p>
     */
    public static <T> int maxOrderIndex(List<T> items, java.util.function.ToIntFunction<? super T> extractor) {
        if (items == null || items.isEmpty()) {
            return 0;
        }
        return items.stream().mapToInt(extractor).max().orElse(0);
    }

    // ============================================================
    // IN-MEMORY SHIFT (dùng cho SectionItem — vì item đã load vào collection)
    // ============================================================

    /**
     * Dời orderIndex của các item có index &gt;= target lên +1, bỏ qua các item đã soft-delete.
     * Áp dụng cho collection đã load sẵn (không qua DB).
     *
     * <p>Pattern (sửa nhận xét #2: add với orderIndex từ client):</p>
     * <pre>{@code
     * int resolved = OrderIndexResolver.resolveForCreate(
     *         request.getOrderIndex(),
     *         () -> OrderIndexResolver.maxOrderIndex(items, Item::getOrderIndex)
     * );
     * if (request.getOrderIndex() != null && request.getOrderIndex() > 0
     *         && resolved == request.getOrderIndex()) {
     *     OrderIndexResolver.shiftUpInCollection(items, resolved, Item::getOrderIndex, Item::setOrderIndex);
     * }
     * }</pre>
     *
     * @param items     collection đã load (sẽ mutate)
     * @param target    orderIndex bắt đầu shift (target trở đi → +1)
     * @param getter    function lấy orderIndex
     * @param setter    function set orderIndex
     * @return số item bị shift
     */
    public static <T> int shiftUpInCollection(
            List<T> items,
            int target,
            java.util.function.ToIntFunction<? super T> getter,
            java.util.function.BiConsumer<T, Integer> setter
    ) {
        if (items == null || items.isEmpty()) return 0;
        int count = 0;
        for (T item : items) {
            if (getter.applyAsInt(item) >= target) {
                setter.accept(item, getter.applyAsInt(item) + 1);
                count++;
            }
        }
        if (count > 0) {
            log.debug("[ORDER] Shifted {} item(s) with orderIndex >= {} in collection", count, target);
        }
        return count;
    }

    /**
     * Dời orderIndex của các item có index trong khoảng (lowerExclusive, upperInclusive] xuống -1.
     * Áp dụng cho collection đã load sẵn.
     *
     * @return số item bị shift
     */
    public static <T> int shiftDownInCollection(
            List<T> items,
            int lowerExclusive,
            int upperInclusive,
            java.util.function.ToIntFunction<? super T> getter,
            java.util.function.BiConsumer<T, Integer> setter
    ) {
        if (items == null || items.isEmpty()) return 0;
        int count = 0;
        for (T item : items) {
            int idx = getter.applyAsInt(item);
            if (idx > lowerExclusive && idx <= upperInclusive) {
                setter.accept(item, idx - 1);
                count++;
            }
        }
        if (count > 0) {
            log.debug("[ORDER] Shifted {} item(s) in ({}, {}] in collection", count, lowerExclusive, upperInclusive);
        }
        return count;
    }

    /**
     * Tính next orderIndex dựa trên max hiện tại (kể cả các item đã soft-delete).
     * Khác với {@link #maxOrderIndex}, version này KHÔNG filter deleted, để tránh
     * đụng với index của item đã soft-delete.
     *
     * <p>Sửa nhận xét #2 vấn đề #3: nextExampleOrderIndex bỏ qua deleted → trùng index.</p>
     */
    public static <T> int nextOrderIndexIncludingDeleted(
            List<T> items,
            java.util.function.ToIntFunction<? super T> getter
    ) {
        if (items == null || items.isEmpty()) return 1;
        return items.stream().mapToInt(getter).max().orElse(0) + 1;
    }

    private static int nullToZero(Integer value) {
        return value == null ? 0 : value;
    }

    // ============================================================
    // BULK REORDER (dùng cho drag-drop reorder nhiều item cùng lúc)
    // ============================================================

    /**
     * Reorder nhiều entity cùng lúc bằng cách gán lại {@code orderIndex = 1..N}
     * theo thứ tự trong list. Áp dụng cho bất kỳ entity nào có field {@code orderIndex}.
     *
     * <p>Use case: frontend dùng thư viện kéo-thả (react-beautiful-dnd, dnd-kit, ...) để
     * reorder nhiều lesson/section/item/question cùng lúc. Thay vì gọi N lần
     * {@code update} (gây N+1 query + N lần shift + có thể xung đột), endpoint bulk
     * nhận list ID theo thứ tự MỚI và reassign index liên tục trong 1 transaction.</p>
     *
     * <p>Quy tắc (dùng marker tạm âm để tránh UNIQUE conflict khi swap chỉ số):</p>
     * <ol>
     *   <li>Phase 1: Set TẤT CẢ entity về {@code -|oldIndex|} (giải phóng slot).</li>
     *   <li>Phase 2: Gán lại theo thứ tự list: entity thứ 0 → 1, thứ 1 → 2, ...</li>
     * </ol>
     *
     * <p>Hàm này CHỈ thực hiện logic reorder — caller phải:</p>
     * <ul>
     *   <li>Validate list ID khớp 100% với current active entities (cùng parent).</li>
     *   <li>Check quyền trước khi gọi.</li>
     *   <li>Save entity sau khi gọi method này.</li>
     * </ul>
     *
     * @param entities   list entity ACTIVE theo thứ tự HIỆN TẠI (sẽ bị mutate)
     * @param orderedIds list ID theo thứ tự MỚI (do client gửi)
     * @param idGetter   function lấy ID từ entity
     * @param idxGetter  function lấy orderIndex hiện tại từ entity
     * @param idxSetter  function set orderIndex cho entity
     * @param <T>        loại entity (Lesson / Section / Item / Question / ...)
     * @param <ID>       loại ID (Long cho hầu hết, có thể là String cho mã khác)
     * @return entity đã được reorder (cùng reference, dùng cho caller saveAll)
     * @throws AppException nếu list rỗng / null / không khớp với current entities
     */
    public static <T, ID> List<T> bulkReorder(
            List<T> entities,
            List<ID> orderedIds,
            java.util.function.Function<T, ID> idGetter,
            java.util.function.ToIntFunction<? super T> idxGetter,
            java.util.function.BiConsumer<T, Integer> idxSetter
    ) {
        if (orderedIds == null || orderedIds.isEmpty()) {
            throw new AppException(ErrorCode.LESSON_REORDER_LIST_EMPTY);
        }
        if (entities == null || entities.isEmpty()) {
            throw new AppException(ErrorCode.LESSON_REORDER_LIST_EMPTY);
        }

        java.util.Map<ID, T> byId = new java.util.HashMap<>();
        for (T e : entities) {
            byId.put(idGetter.apply(e), e);
        }

        // Validate: tất cả ID trong orderedIds phải tồn tại trong entities hiện tại
        for (ID id : orderedIds) {
            if (!byId.containsKey(id)) {
                log.warn("[ORDER] Bulk reorder: ID {} không tồn tại trong entities hiện tại", id);
                throw new AppException(ErrorCode.LESSON_NOT_FOUND);
            }
        }

        // Phase 1: set marker tạm âm (dùng -|oldIndex| để tránh xung đột với index dương)
        for (T e : entities) {
            int old = idxGetter.applyAsInt(e);
            int safeOld = (old == Integer.MIN_VALUE) ? 1 : Math.abs(old);
            if (safeOld == 0) safeOld = 1; // tránh marker = 0
            idxSetter.accept(e, -safeOld);
        }

        // Phase 2: gán orderIndex mới 1..N theo thứ tự client gửi
        int newIdx = 1;
        for (ID id : orderedIds) {
            T e = byId.get(id);
            idxSetter.accept(e, newIdx++);
        }

        log.info("[ORDER] Bulk reordered {} entity(s) via marker-temp strategy", entities.size());
        return entities;
    }

    // ============================================================
    // ADJACENT SWAP (thuật toán sắp xếp theo swap liền kề)
    // ============================================================

    /**
     * Swap orderIndex của 2 entity (đã load) trong cùng parent. Hàm này mutate
     * cả 2 entity — caller phải save lại.
     *
     * <p>Ví dụ: items = [A(1), B(2), C(3), D(4)], muốn move D từ 4 → 2:</p>
     * <ul>
     *   <li>Bước 1: swap D ↔ C → DB = [A(1), B(2), D(3), C(4)]</li>
     *   <li>Bước 2: swap D ↔ B → DB = [A(1), D(2), B(3), C(4)] ✓</li>
     * </ul>
     *
     * @return true nếu swap thành công, false nếu không tìm thấy item kề
     */
    public static <T> boolean swapAdjacent(
            List<T> entities,
            T movingItem,
            int movingTargetIndex,
            java.util.function.ToIntFunction<? super T> idxGetter,
            java.util.function.BiConsumer<T, Integer> idxSetter
    ) {
        int currentIndex = idxGetter.applyAsInt(movingItem);
        if (currentIndex == movingTargetIndex) {
            return false; // đã ở đúng vị trí
        }

        // Tìm item ở movingTargetIndex trong collection
        T neighborItem = entities.stream()
                .filter(e -> idxGetter.applyAsInt(e) == movingTargetIndex)
                .findFirst()
                .orElse(null);
        if (neighborItem == null) {
            log.warn("[ORDER] swapAdjacent: không tìm thấy item ở index={}", movingTargetIndex);
            return false;
        }

        // Swap 2 chiều: movingItem ← neighborIndex, neighborItem ← movingIndex
        idxSetter.accept(movingItem, movingTargetIndex);
        idxSetter.accept(neighborItem, currentIndex);
        return true;
    }
}
