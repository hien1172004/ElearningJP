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
}
