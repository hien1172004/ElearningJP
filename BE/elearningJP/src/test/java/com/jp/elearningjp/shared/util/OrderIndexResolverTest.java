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
