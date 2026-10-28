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

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test cho {@link OrderIndexResolver} — helper xử lý logic orderIndex.
 *
 * <p>Test thuần không cần Spring context; chỉ kiểm thuật toán + edge case.</p>
 */
class OrderIndexResolverTest {

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
    @DisplayName("assignSequential")
    class AssignSequential {

        @Test
        @DisplayName("Gán 1..N theo thứ tự list")
        void assign() {
            List<LessonKanjiItem> items = List.of(kanji(7), kanji(3), kanji(99));
            OrderIndexResolver.assignSequential(items, LessonKanjiItem::setOrderIndex);
            assertThat(items).extracting(LessonKanjiItem::getOrderIndex).containsExactly(1, 2, 3);
        }

        @Test
        @DisplayName("List rỗng → không lỗi")
        void empty() {
            assertDoesNotThrow(() -> OrderIndexResolver.assignSequential(
                    List.<LessonKanjiItem>of(), LessonKanjiItem::setOrderIndex));
        }
    }

    @Nested
    @DisplayName("insertAtPosition")
    class InsertAtPosition {

        @Test
        @DisplayName("Chèn vào giữa → đánh số lại 1..N")
        void insertMiddle() {
            LessonKanjiItem a = kanji(1), b = kanji(2), c = kanji(3);
            List<LessonKanjiItem> list = new ArrayList<>(List.of(a, b, c));
            LessonKanjiItem d = kanji(4);
            OrderIndexResolver.insertAtPosition(list, d, 2, LessonKanjiItem::setOrderIndex);
            assertThat(list).containsExactly(a, d, b, c);
            assertThat(list).extracting(LessonKanjiItem::getOrderIndex).containsExactly(1, 2, 3, 4);
        }

        @Test
        @DisplayName("target vượt size+1 → chèn cuối")
        void insertOverflow() {
            LessonKanjiItem a = kanji(1), b = kanji(2);
            List<LessonKanjiItem> list = new ArrayList<>(List.of(a, b));
            LessonKanjiItem c = kanji(3);
            OrderIndexResolver.insertAtPosition(list, c, 999, LessonKanjiItem::setOrderIndex);
            assertThat(list).containsExactly(a, b, c);
            assertThat(list).extracting(LessonKanjiItem::getOrderIndex).containsExactly(1, 2, 3);
        }

        @Test
        @DisplayName("target <= 0 → chèn đầu")
        void insertHead() {
            LessonKanjiItem a = kanji(1), b = kanji(2);
            List<LessonKanjiItem> list = new ArrayList<>(List.of(a, b));
            LessonKanjiItem z = kanji(9);
            OrderIndexResolver.insertAtPosition(list, z, 0, LessonKanjiItem::setOrderIndex);
            assertThat(list).containsExactly(z, a, b);
            assertThat(list).extracting(LessonKanjiItem::getOrderIndex).containsExactly(1, 2, 3);
        }
    }

    @Nested
    @DisplayName("moveToPosition")
    class MoveToPosition {

        @Test
        @DisplayName("Move xuống → đánh số lại 1..N")
        void moveDown() {
            LessonKanjiItem a = kanji(1), b = kanji(2), c = kanji(3), d = kanji(4);
            List<LessonKanjiItem> list = new ArrayList<>(List.of(a, b, c, d));
            boolean moved = OrderIndexResolver.moveToPosition(list, a, 3, LessonKanjiItem::setOrderIndex);
            assertTrue(moved);
            assertThat(list).containsExactly(b, c, a, d);
            assertThat(list).extracting(LessonKanjiItem::getOrderIndex).containsExactly(1, 2, 3, 4);
        }

        @Test
        @DisplayName("Move lên → đánh số lại 1..N")
        void moveUp() {
            LessonKanjiItem a = kanji(1), b = kanji(2), c = kanji(3), d = kanji(4);
            List<LessonKanjiItem> list = new ArrayList<>(List.of(a, b, c, d));
            boolean moved = OrderIndexResolver.moveToPosition(list, d, 2, LessonKanjiItem::setOrderIndex);
            assertTrue(moved);
            assertThat(list).containsExactly(a, d, b, c);
            assertThat(list).extracting(LessonKanjiItem::getOrderIndex).containsExactly(1, 2, 3, 4);
        }

        @Test
        @DisplayName("Vị trí không đổi → no-op (false)")
        void noChange() {
            LessonKanjiItem a = kanji(1), b = kanji(2);
            List<LessonKanjiItem> list = new ArrayList<>(List.of(a, b));
            assertFalse(OrderIndexResolver.moveToPosition(list, b, 2, LessonKanjiItem::setOrderIndex));
            assertThat(list).containsExactly(a, b);
        }

        @Test
        @DisplayName("target vượt size → kẹp về cuối")
        void clampToEnd() {
            LessonKanjiItem a = kanji(1), b = kanji(2), c = kanji(3);
            List<LessonKanjiItem> list = new ArrayList<>(List.of(a, b, c));
            boolean moved = OrderIndexResolver.moveToPosition(list, a, 999, LessonKanjiItem::setOrderIndex);
            assertTrue(moved);
            assertThat(list).containsExactly(b, c, a);
        }

        @Test
        @DisplayName("Lỗ hổng index (soft-delete) vẫn move đúng")
        void withGaps() {
            // Dãy có lỗ hổng: 1, 2, 5, 9 (do soft-delete)
            LessonKanjiItem a = kanji(1), b = kanji(2), c = kanji(5), d = kanji(9);
            List<LessonKanjiItem> list = new ArrayList<>(List.of(a, b, c, d));
            boolean moved = OrderIndexResolver.moveToPosition(list, d, 2, LessonKanjiItem::setOrderIndex);
            assertTrue(moved);
            assertThat(list).containsExactly(a, d, b, c);
            assertThat(list).extracting(LessonKanjiItem::getOrderIndex).containsExactly(1, 2, 3, 4);
        }
    }

    @Nested
    @DisplayName("bulkReorder")
    class BulkReorder {

        @Test
        @DisplayName("Gán 1..N theo list ID client gửi")
        void reorder() {
            LessonVocabularyItem a = vocab(1), b = vocab(2), c = vocab(3);
            a.setId(1L); b.setId(2L); c.setId(3L);
            List<LessonVocabularyItem> list = new ArrayList<>(List.of(a, b, c));
            // Thứ tự mới: c, a, b  → c=1, a=2, b=3
            OrderIndexResolver.bulkReorder(
                    list,
                    List.of(c.getId(), a.getId(), b.getId()),
                    LessonVocabularyItem::getId,
                    LessonVocabularyItem::setOrderIndex
            );
            assertThat(list).extracting(LessonVocabularyItem::getOrderIndex).containsExactly(2, 3, 1);
        }

        @Test
        @DisplayName("ID không tồn tại → AppException")
        void unknownId() {
            LessonVocabularyItem a = vocab(1);
            a.setId(1L);
            List<LessonVocabularyItem> list = new ArrayList<>(List.of(a));
            assertThrows(AppException.class, () -> OrderIndexResolver.bulkReorder(
                    list, List.of(999L),
                    LessonVocabularyItem::getId,
                    LessonVocabularyItem::setOrderIndex
            ));
        }

        @Test
        @DisplayName("List rỗng → AppException")
        void emptyList() {
            AppException ex = assertThrows(AppException.class, () -> OrderIndexResolver.bulkReorder(
                    List.<LessonVocabularyItem>of(), List.of(1L),
                    LessonVocabularyItem::getId,
                    LessonVocabularyItem::setOrderIndex
            ));
            assertEquals(ErrorCode.LESSON_REORDER_LIST_EMPTY, ex.getErrorCode());
        }
    }

    @Nested
    @DisplayName("persistSequential")
    class PersistSequential {

        @Test
        @DisplayName("Pha 1 gán marker âm, pha 2 gán 1..N")
        void twoPhase() {
            LessonKanjiItem a = kanji(5), b = kanji(9), c = kanji(1);
            List<LessonKanjiItem> list = new ArrayList<>(List.of(a, b, c));

            List<List<Integer>> snapshots = new ArrayList<>();
            Runnable flush = () -> snapshots.add(
                    list.stream().map(LessonKanjiItem::getOrderIndex).toList());

            OrderIndexResolver.persistSequential(
                    list, LessonKanjiItem::getId, LessonKanjiItem::setOrderIndex, flush);

            // Đúng 2 lần flush (pha 1 + pha 2)
            assertEquals(2, snapshots.size());
            // Sau pha 1: toàn bộ âm (marker) → giải phóng slot dương
            assertThat(snapshots.get(0)).allMatch(i -> i < 0);
            // Sau pha 2: 1..N theo thứ tự list
            assertThat(snapshots.get(1)).containsExactly(1, 2, 3);
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
    }

    // ============================================================
    // FIXTURES
    // ============================================================

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
