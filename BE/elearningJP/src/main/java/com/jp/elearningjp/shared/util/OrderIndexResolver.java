package com.jp.elearningjp.shared.util;

import com.jp.elearningjp.exception.AppException;
import com.jp.elearningjp.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.ToIntFunction;

/**
 * Helper xử lý logic {@code orderIndex} cho các entity có thứ tự
 * (Lesson, LessonSection, Kanji/Vocabulary/Grammar item, QuizQuestion).
 *
 * <h2>Chiến lược "đánh số lại tuần tự" (sequential renumber)</h2>
 *
 * <p>Mọi thao tác chèn / di chuyển / sắp xếp lại đều quy về cùng một mô hình:</p>
 * <ol>
 *   <li>Load danh sách entity <b>active</b> của parent, đã sắp theo {@code orderIndex ASC}.</li>
 *   <li>Thay đổi thứ tự <b>trong bộ nhớ</b> (chèn vào vị trí / move / theo list client gửi).</li>
 *   <li>Gán lại {@code orderIndex = 1..N} theo đúng thứ tự đó.</li>
 *   <li>Ghi xuống DB qua {@link #persistSequential} (2 pha, an toàn UNIQUE).</li>
 * </ol>
 *
 * <h2>Vì sao phải ghi 2 pha?</h2>
 *
 * <p>Các bảng đều có <b>partial unique index</b> {@code (parent_id, order_index) WHERE deleted = FALSE}
 * (xem {@code V18__add_unique_order_indexes.sql}, {@code V22__add_unique_quiz_questions_order_index.sql}).
 * Không thể UPDATE thẳng sang giá trị đích: khi hoán đổi 2 item (A@1, B@2 → A@2, B@1),
 * Hibernate có thể chạy {@code UPDATE A SET order_index = 2} trước trong khi B vẫn giữ 2
 * → vi phạm UNIQUE.</p>
 *
 * <p>{@link #persistSequential} giải quyết bằng 2 lần flush:</p>
 * <ol>
 *   <li><b>Pha 1:</b> gán mọi row về marker âm duy nhất {@code -id} rồi flush. Vì {@code id}
 *       dương duy nhất nên {@code -id} âm duy nhất → luôn thoả UNIQUE, và không đụng bất kỳ
 *       {@code orderIndex} dương hợp lệ nào.</li>
 *   <li><b>Pha 2:</b> gán {@code 1..N} (đều dương, đôi một khác nhau) rồi flush. Lúc này DB
 *       đang toàn giá trị âm nên thứ tự phát UPDATE không còn quan trọng.</li>
 * </ol>
 *
 * <h2>Vì sao KHÔNG còn "adjacent swap chain"?</h2>
 *
 * <p>Cách cũ swap từng bước liền kề và giả định {@code orderIndex} liên tục 1..N. Nhưng xoá là
 * <b>soft-delete</b> nên dãy index hình thành "lỗ hổng" (1,2,4,5). Khi đó
 * {@code findIdBy...OrderIndex(nextIndex)} trả {@code null} → chuỗi swap dừng giữa đường,
 * item kẹt sai vị trí. Cách mới không phụ thuộc tính liên tục: nó <b>đánh số lại toàn bộ</b>
 * nên tự lấp mọi lỗ hổng và luôn trả về dãy {@code 1..N} chuẩn.</p>
 */
@Slf4j
public final class OrderIndexResolver {

    private OrderIndexResolver() {}

    // ============================================================
    // ĐỌC / CHUẨN HOÁ GIÁ TRỊ
    // ============================================================

    /**
     * Tính MAX orderIndex từ một list entity có method {@code getOrderIndex()}.
     * Trả về 0 nếu list null/rỗng.
     */
    public static <T> int maxOrderIndex(List<T> items, ToIntFunction<? super T> extractor) {
        if (items == null || items.isEmpty()) {
            return 0;
        }
        return items.stream().mapToInt(extractor).max().orElse(0);
    }

    /**
     * Kẹp {@code newIndex} về tối đa {@code MAX+1} (khi client gửi index vượt quá cuối dãy).
     */
    public static int clampToMaxPlusOne(int newIndex, java.util.function.Supplier<Integer> currentMaxFn) {
        int currentMax = nullToZero(currentMaxFn.get());
        return (newIndex > currentMax + 1) ? (currentMax + 1) : newIndex;
    }

    /**
     * Tính next orderIndex dựa trên max hiện tại (kể cả item đã soft-delete).
     * Dùng khi thêm example/related-vocab (không cần đánh số lại toàn bộ collection).
     */
    public static <T> int nextOrderIndexIncludingDeleted(
            List<T> items,
            ToIntFunction<? super T> getter
    ) {
        if (items == null || items.isEmpty()) return 1;
        return items.stream().mapToInt(getter).max().orElse(0) + 1;
    }

    // ============================================================
    // GÁN SỐ TUẦN TỰ 1..N (in-memory)
    // ============================================================

    /**
     * Gán {@code orderIndex = 1..N} cho các entity theo đúng thứ tự trong {@code ordered}
     * (chỉ mutate bộ nhớ — không ghi DB).
     */
    public static <T> void assignSequential(
            List<T> ordered,
            BiConsumer<T, Integer> idxSetter
    ) {
        if (ordered == null || ordered.isEmpty()) return;
        int i = 1;
        for (T e : ordered) {
            idxSetter.accept(e, i++);
        }
    }

    // ============================================================
    // CHÈN / DI CHUYỂN 1 ENTITY TRONG LIST
    // ============================================================

    /**
     * Chèn {@code item} vào vị trí {@code targetOneBased} (1-based) trong {@code orderedAsc},
     * rồi gán lại {@code orderIndex = 1..N}.
     *
     * <p>Nếu {@code targetOneBased > size+1} → chèn cuối; nếu {@code <= 0} → chèn đầu.</p>
     */
    public static <T> void insertAtPosition(
            List<T> orderedAsc,
            T item,
            int targetOneBased,
            BiConsumer<T, Integer> idxSetter
    ) {
        int size = (orderedAsc == null) ? 0 : orderedAsc.size();
        int target = Math.max(1, Math.min(targetOneBased, size + 1));
        orderedAsc.add(target - 1, item);
        assignSequential(orderedAsc, idxSetter);
    }

    /**
     * Di chuyển {@code moving} (đang nằm trong {@code orderedAsc}) tới vị trí {@code targetOneBased}.
     *
     * @return {@code true} nếu vị trí thực sự thay đổi, {@code false} nếu no-op.
     */
    public static <T> boolean moveToPosition(
            List<T> orderedAsc,
            T moving,
            int targetOneBased,
            BiConsumer<T, Integer> idxSetter
    ) {
        if (orderedAsc == null || orderedAsc.isEmpty() || moving == null) return false;

        int size = orderedAsc.size();
        int targetPos = Math.max(1, Math.min(targetOneBased, size)) - 1;
        int currentPos = indexOfIdentity(orderedAsc, moving);
        if (currentPos < 0 || currentPos == targetPos) return false;

        orderedAsc.remove(currentPos);
        orderedAsc.add(targetPos, moving);
        assignSequential(orderedAsc, idxSetter);
        return true;
    }

    // ============================================================
    // BULK REORDER (drag-drop: client gửi list ID theo thứ tự mới)
    // ============================================================

    /**
     * Sắp xếp lại {@code entities} theo đúng thứ tự {@code orderedIds} và gán {@code orderIndex = 1..N}.
     *
     * <p>Hàm chỉ reorder <b>in-memory</b>; caller phải gọi {@link #persistSequential} để ghi DB.</p>
     *
     * @throws AppException nếu list rỗng / null / có ID không tồn tại
     */
    public static <T, ID> List<T> bulkReorder(
            List<T> entities,
            List<ID> orderedIds,
            Function<T, ID> idGetter,
            BiConsumer<T, Integer> idxSetter
    ) {
        if (orderedIds == null || orderedIds.isEmpty()) {
            throw new AppException(ErrorCode.LESSON_REORDER_LIST_EMPTY);
        }
        if (entities == null || entities.isEmpty()) {
            throw new AppException(ErrorCode.LESSON_REORDER_LIST_EMPTY);
        }

        Map<ID, T> byId = new HashMap<>();
        for (T e : entities) {
            byId.put(idGetter.apply(e), e);
        }

        for (ID id : orderedIds) {
            if (!byId.containsKey(id)) {
                log.warn("[ORDER] Bulk reorder: ID {} không tồn tại trong entities hiện tại", id);
                throw new AppException(ErrorCode.LESSON_NOT_FOUND);
            }
        }

        int newIdx = 1;
        for (ID id : orderedIds) {
            idxSetter.accept(byId.get(id), newIdx++);
        }

        log.info("[ORDER] Bulk reordered {} entity(s) sequentially (1..{})", orderedIds.size(), orderedIds.size());
        return entities;
    }

    // ============================================================
    // GHI DB AN TOÀN (2 pha: marker âm → 1..N)
    // ============================================================

    /**
     * Ghi thứ tự đã sắp trong {@code ordered} xuống DB an toàn với UNIQUE constraint.
     *
     * <p>Pha 1: gán mọi entity {@code orderIndex = -id} (âm duy nhất) rồi {@code flush}.
     * Pha 2: gán {@code orderIndex = 1..N} theo đúng thứ tự rồi {@code flush}.</p>
     *
     * @param ordered   danh sách entity theo thứ tự mong muốn (đã load, sẽ bị mutate)
     * @param idGetter  lấy ID (dùng để tạo marker âm)
     * @param idxSetter set orderIndex in-memory
     * @param flush     callback flush persistence context (thường {@code repository::flush})
     */
    public static <T, ID> void persistSequential(
            List<T> ordered,
            Function<T, ID> idGetter,
            BiConsumer<T, Integer> idxSetter,
            Runnable flush
    ) {
        if (ordered == null || ordered.isEmpty()) return;

        // Pha 1: marker âm duy nhất -id → giải phóng mọi slot dương
        for (T e : ordered) {
            idxSetter.accept(e, negativeMarker(idGetter.apply(e)));
        }
        flush.run();

        // Pha 2: gán 1..N (đều dương, đôi một khác nhau)
        int i = 1;
        for (T e : ordered) {
            idxSetter.accept(e, i++);
        }
        flush.run();

        log.info("[ORDER] Persisted sequential order for {} row(s)", ordered.size());
    }

    // ============================================================
    // PRIVATE HELPERS
    // ============================================================

    /** Marker âm duy nhất từ id: {@code -id}. id null (chưa persist) → dùng -1 (caller không nên xảy ra). */
    private static int negativeMarker(Object id) {
        if (id instanceof Number n) {
            long v = n.longValue();
            if (v != 0 && v <= Integer.MAX_VALUE) {
                return (int) -v;
            }
        }
        // Fallback cực hiếm: id không phải số hoặc vượt int → dùng Integer.MIN_VALUE
        return Integer.MIN_VALUE;
    }

    /** Tìm vị trí theo <b>identity</b> (==), tránh phụ thuộc equals/hashCode của entity JPA. */
    private static <T> int indexOfIdentity(List<T> list, T target) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i) == target) return i;
        }
        return -1;
    }

    private static int nullToZero(Integer value) {
        return value == null ? 0 : value;
    }
}
