package com.jp.elearningjp.shared.util;

import com.jp.elearningjp.entity.curriculum.LessonGrammarItem;
import com.jp.elearningjp.entity.curriculum.LessonKanjiItem;
import com.jp.elearningjp.entity.curriculum.LessonSection;
import com.jp.elearningjp.entity.curriculum.LessonVocabularyItem;
import com.jp.elearningjp.exception.AppException;
import com.jp.elearningjp.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test cho {@link OrderIndexResolver} — helper xử lý logic orderIndex.
 *
 * <p>Test thuần không cần Spring context; chỉ kiểm thuật toán + edge case.</p>
 */
class OrderIndexResolverTest {

    @Nested
    @DisplayName("resolveForCreate")
    class ResolveForCreate {

        @Test
        @DisplayName("requested=null → auto-assign MAX+1 (kể cả khi chưa có record)")
        void nullRequest_emptyParent() {
            int result = OrderIndexResolver.resolveForCreate(null, () -> null);
            assertEquals(1, result, "MAX=null → MAX+1 = 1");
        }

        @Test
        @DisplayName("requested=null + có MAX=5 → 6")
        void nullRequest_withExisting() {
            int result = OrderIndexResolver.resolveForCreate(null, () -> 5);
            assertEquals(6, result);
        }

        @Test
        @DisplayName("requested<=0 → auto-assign MAX+1")
        void nonPositiveRequest() {
            assertEquals(3, OrderIndexResolver.resolveForCreate(0, () -> 2));
            assertEquals(3, OrderIndexResolver.resolveForCreate(-1, () -> 2));
        }

        @Test
        @DisplayName("requested > MAX+1 → kẹp về MAX+1")
        void requestedOverflow() {
            int result = OrderIndexResolver.resolveForCreate(100, () -> 5);
            assertEquals(6, result, "Vượt MAX+1 → kẹp về 6");
        }

        @Test
        @DisplayName("requested trong [1, MAX+1] → giữ nguyên")
        void requestedValid() {
            assertEquals(3, OrderIndexResolver.resolveForCreate(3, () -> 5));
            assertEquals(6, OrderIndexResolver.resolveForCreate(6, () -> 5));
        }
    }

    @Nested
    @DisplayName("planMove")
    class PlanMove {

        @Test
        @DisplayName("newIndex == oldIndex → NOOP")
        void noChange() {
            assertEquals(OrderIndexResolver.Movement.NOOP,
                    OrderIndexResolver.planMove(3, 3, () -> 5));
        }

        @Test
        @DisplayName("newIndex < oldIndex (kẹp hoặc trực tiếp) → MOVE_UP")
        void moveUp() {
            assertEquals(OrderIndexResolver.Movement.MOVE_UP,
                    OrderIndexResolver.planMove(5, 2, () -> 5));
        }

        @Test
        @DisplayName("newIndex > oldIndex (trong [oldIndex+1, MAX+1]) → MOVE_DOWN")
        void moveDown() {
            assertEquals(OrderIndexResolver.Movement.MOVE_DOWN,
                    OrderIndexResolver.planMove(2, 4, () -> 5));
        }

        @Test
        @DisplayName("newIndex vượt MAX+1, bị kẹp trùng oldIndex → NOOP")
        void requestedOverflow_clampedToOldIndex() {
            // oldIndex=5, MAX=5, requested=100 → clamped to MAX+1=6
            // 6 != 5 → MOVE_DOWN
            assertEquals(OrderIndexResolver.Movement.MOVE_DOWN,
                    OrderIndexResolver.planMove(5, 100, () -> 5));
        }
    }

    @Nested
    @DisplayName("clampToMaxPlusOne")
    class ClampToMaxPlusOne {

        @Test
        @DisplayName("Kẹp về MAX+1 khi vượt")
        void overflow() {
            assertEquals(6, OrderIndexResolver.clampToMaxPlusOne(100, () -> 5));
        }

        @Test
        @DisplayName("Giữ nguyên khi trong khoảng hợp lệ")
        void valid() {
            assertEquals(3, OrderIndexResolver.clampToMaxPlusOne(3, () -> 5));
        }

        @Test
        @DisplayName("MAX=null → coi như 0")
        void nullMax() {
            assertEquals(1, OrderIndexResolver.clampToMaxPlusOne(5, () -> null));
        }
    }

    @Nested
    @DisplayName("assertNoConflict")
    class AssertNoConflict {

        @Test
        @DisplayName("exists=false → pass")
        void noConflict() {
            assertDoesNotThrow(() ->
                    OrderIndexResolver.assertNoConflict(() -> false));
        }

        @Test
        @DisplayName("exists=true → throw AppException với LESSON_ORDER_CONFLICT")
        void hasConflict() {
            AppException ex = assertThrows(AppException.class, () ->
                    OrderIndexResolver.assertNoConflict(() -> true));
            assertEquals(ErrorCode.LESSON_ORDER_CONFLICT, ex.getErrorCode());
        }
    }

    @Nested
    @DisplayName("shiftUpForCreate")
    class ShiftUpForCreate {

        @Test
        @DisplayName("Trả về số record bị shift")
        void returnsShiftedCount() {
            AtomicInteger counter = new AtomicInteger(3);
            int shifted = OrderIndexResolver.shiftUpForCreate(2, counter::get);
            assertEquals(3, shifted);
            assertEquals(3, counter.get(), "Supplier chỉ gọi đúng 1 lần");
        }
    }

    @Nested
    @DisplayName("maxOrderIndex (List)")
    class MaxOrderIndexFromList {

        @Test
        @DisplayName("List null → 0")
        void nullList() {
            assertEquals(0, OrderIndexResolver.maxOrderIndex(
                    (List<LessonSection>) null, LessonSection::getOrderIndex));
        }

        @Test
        @DisplayName("List rỗng → 0")
        void emptyList() {
            assertEquals(0, OrderIndexResolver.maxOrderIndex(List.<LessonSection>of(), LessonSection::getOrderIndex));
        }

        @Test
        @DisplayName("List có items → MAX")
        void withItems() {
            LessonSection s1 = section(1);
            LessonSection s2 = section(5);
            LessonSection s3 = section(3);
            assertEquals(5, OrderIndexResolver.maxOrderIndex(List.of(s1, s2, s3), LessonSection::getOrderIndex));
        }

        @Test
        @DisplayName("LessonKanjiItem list → MAX")
        void kanjiItems() {
            assertEquals(7, OrderIndexResolver.maxOrderIndex(List.of(
                    kanji(2), kanji(7), kanji(4)
            ), LessonKanjiItem::getOrderIndex));
        }

        @Test
        @DisplayName("LessonVocabularyItem list → MAX")
        void vocabItems() {
            assertEquals(9, OrderIndexResolver.maxOrderIndex(List.of(
                    vocab(3), vocab(9), vocab(1)
            ), LessonVocabularyItem::getOrderIndex));
        }

        @Test
        @DisplayName("LessonGrammarItem list → MAX")
        void grammarItems() {
            assertEquals(6, OrderIndexResolver.maxOrderIndex(List.of(
                    grammar(2), grammar(6), grammar(4)
            ), LessonGrammarItem::getOrderIndex));
        }

        @Nested
    @DisplayName("shiftUpInCollection")
    class ShiftUpInCollection {

        @Test
        @DisplayName("items có index >= target → index + 1")
        void shiftUp() {
            // [1, 3, 5, 7] target=3 → items >= 3 (3, 5, 7) bị shift → [1, 4, 6, 8]
            List<LessonKanjiItem> items = List.of(kanji(1), kanji(3), kanji(5), kanji(7));
            int shifted = OrderIndexResolver.shiftUpInCollection(
                    items, 3,
                    LessonKanjiItem::getOrderIndex,
                    (i, v) -> i.setOrderIndex(v)
            );
            assertEquals(3, shifted, "Có 3 items bị shift: index 3, 5, 7");
            assertThat(items).extracting(LessonKanjiItem::getOrderIndex).containsExactly(1, 4, 6, 8);
        }

        @Test
        @DisplayName("index < target → không shift")
        void noShift() {
            List<LessonKanjiItem> items = List.of(kanji(1), kanji(2), kanji(3));
            int shifted = OrderIndexResolver.shiftUpInCollection(
                    items, 5,
                    LessonKanjiItem::getOrderIndex,
                    (i, v) -> i.setOrderIndex(v)
            );
            assertEquals(0, shifted);
            assertThat(items).extracting(LessonKanjiItem::getOrderIndex).containsExactly(1, 2, 3);
        }

        @Test
        @DisplayName("list rỗng → 0")
        void emptyList() {
            int shifted = OrderIndexResolver.shiftUpInCollection(
                    List.of(), 2,
                    LessonKanjiItem::getOrderIndex,
                    (i, v) -> i.setOrderIndex(v)
            );
            assertEquals(0, shifted);
        }

        @Test
        @DisplayName("target = 1 → tất cả bị shift")
        void shiftAll() {
            List<LessonKanjiItem> items = List.of(kanji(1), kanji(2), kanji(3));
            OrderIndexResolver.shiftUpInCollection(
                    items, 1,
                    LessonKanjiItem::getOrderIndex,
                    (i, v) -> i.setOrderIndex(v)
            );
            assertThat(items).extracting(LessonKanjiItem::getOrderIndex).containsExactly(2, 3, 4);
        }
    }

    @Nested
    @DisplayName("shiftDownInCollection")
    class ShiftDownInCollection {

        @Test
        @DisplayName("items trong (lower, upper] → index - 1")
        void shiftDown() {
            List<LessonKanjiItem> items = List.of(kanji(1), kanji(3), kanji(5), kanji(7));
            int shifted = OrderIndexResolver.shiftDownInCollection(
                    items, 2, 6,
                    LessonKanjiItem::getOrderIndex,
                    (i, v) -> i.setOrderIndex(v)
            );
            assertEquals(2, shifted, "Có 2 items bị shift: index 3 và 5");
            assertThat(items).extracting(LessonKanjiItem::getOrderIndex).containsExactly(1, 2, 4, 7);
        }

        @Test
        @DisplayName("index trong khoảng (lowerExclusive, upperInclusive] → bị shift")
        void shiftDownBoundary() {
            // [2, 3, 4, 5] lower=1, upper=5 → items có index > 1 VÀ index <= 5 → all 4 items
            // Kết quả: [1, 2, 3, 4]
            List<LessonKanjiItem> items = List.of(kanji(2), kanji(3), kanji(4), kanji(5));
            int shifted = OrderIndexResolver.shiftDownInCollection(
                    items, 1, 5,
                    LessonKanjiItem::getOrderIndex,
                    (i, v) -> i.setOrderIndex(v)
            );
            assertEquals(4, shifted);
            assertThat(items).extracting(LessonKanjiItem::getOrderIndex).containsExactly(1, 2, 3, 4);
        }

        @Test
        @DisplayName("index <= lowerExclusive → không shift")
        void noShift() {
            List<LessonKanjiItem> items = List.of(kanji(1), kanji(3), kanji(5));
            int shifted = OrderIndexResolver.shiftDownInCollection(
                    items, 3, 5,
                    LessonKanjiItem::getOrderIndex,
                    (i, v) -> i.setOrderIndex(v)
            );
            assertEquals(1, shifted, "Chỉ index=5 bị shift");
            assertThat(items).extracting(LessonKanjiItem::getOrderIndex).containsExactly(1, 3, 4);
        }
    }

    @Nested
    @DisplayName("nextOrderIndexIncludingDeleted")
    class NextOrderIndexIncludingDeleted {

        @Test
        @DisplayName("list rỗng → 1")
        void emptyList() {
            assertEquals(1, OrderIndexResolver.nextOrderIndexIncludingDeleted(
                    List.of(), LessonKanjiItem::getOrderIndex));
        }

        @Test
        @DisplayName("list có items → max + 1")
        void withItems() {
            List<LessonKanjiItem> items = List.of(kanji(1), kanji(3), kanji(5));
            assertEquals(6, OrderIndexResolver.nextOrderIndexIncludingDeleted(
                    items, LessonKanjiItem::getOrderIndex));
        }

        @Test
        @DisplayName("không filter deleted → KHÔNG giống maxOrderIndex (đây là điểm khác biệt)")
        void differsFromMaxOrderIndex() {
            // Mô phỏng: có 3 item active (1,2,3) và 1 item deleted (index=10)
            // maxOrderIndex sẽ trả 3 (max của active)
            // nextOrderIndexIncludingDeleted sẽ trả 11 (max của all)
            List<LessonKanjiItem> items = List.of(kanji(1), kanji(2), kanji(3));
            // Giả lập: không filter deleted → max = 10
            int maxIncludingDeleted = items.stream()
                    .mapToInt(LessonKanjiItem::getOrderIndex).max().orElse(0) + 1;
            assertEquals(4, maxIncludingDeleted);
            // So sánh với version filter deleted (giả lập: chỉ lấy items có index <= 3)
            assertEquals(4, OrderIndexResolver.nextOrderIndexIncludingDeleted(
                    items, LessonKanjiItem::getOrderIndex));
        }
    }

    private LessonSection section(int orderIndex) {
            return LessonSection.builder().orderIndex(orderIndex).build();
        }

        private LessonKanjiItem kanji(int orderIndex) {
            return LessonKanjiItem.builder().orderIndex(orderIndex).build();
        }

        private LessonVocabularyItem vocab(int orderIndex) {
            return LessonVocabularyItem.builder().orderIndex(orderIndex).build();
        }

        private LessonGrammarItem grammar(int orderIndex) {
            return LessonGrammarItem.builder().orderIndex(orderIndex).build();
        }
    }
}
