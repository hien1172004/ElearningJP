package com.jp.elearningjp.service.gamification;

import com.jp.elearningjp.repository.gamification.UserLearnedItemRepository;
import com.jp.elearningjp.shared.enums.LookupTargetType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test cho {@link LearnedItemService} — dựng khoá chống trùng.
 *
 * <p>Kiểm tra 3 điều:</p>
 * <ol>
 *   <li>Khoá {@code target_key} dựng đúng cho 3 loại: VOCAB/GRAMMAR → {@code id:<id>},
 *       KANJI → {@code ref:<ký tự>}.</li>
 *   <li>Thiếu khoá (VOCAB không có id, KANJI không có ký tự) → trả {@code false},
 *       KHÔNG gọi xuống DB (không chặn luồng tra từ).</li>
 *   <li>{@code affected == 1} → mới; {@code affected == 0} → đã học trước đó.</li>
 * </ol>
 */
@ExtendWith(MockitoExtension.class)
class LearnedItemServiceTest {

    @Mock UserLearnedItemRepository learnedItemRepository;

    @InjectMocks LearnedItemService service;

    private static final Long USER_ID = 42L;

    // ============================================================
    // Dựng target_key
    // ============================================================

    @Nested
    @DisplayName("Dựng target_key")
    class TargetKey {

        @Test
        @DisplayName("VOCAB → 'id:<targetId>'")
        void vocabUsesIdPrefix() {
            when(learnedItemRepository.register(
                    eq(USER_ID), eq("VOCAB"), anyString(), anyLong(), isNull()))
                    .thenReturn(1);

            boolean isNew = service.register(USER_ID, LookupTargetType.VOCAB, 100L, null);

            assertThat(isNew).isTrue();
            verify(learnedItemRepository).register(USER_ID, "VOCAB", "id:100", 100L, null);
        }

        @Test
        @DisplayName("GRAMMAR → 'id:<targetId>'")
        void grammarUsesIdPrefix() {
            when(learnedItemRepository.register(
                    eq(USER_ID), eq("GRAMMAR"), anyString(), anyLong(), isNull()))
                    .thenReturn(1);

            boolean isNew = service.register(USER_ID, LookupTargetType.GRAMMAR, 7L, null);

            assertThat(isNew).isTrue();
            verify(learnedItemRepository).register(USER_ID, "GRAMMAR", "id:7", 7L, null);
        }

        @Test
        @DisplayName("KANJI → 'ref:<ký tự Hán>'")
        void kanjiUsesRefPrefix() {
            when(learnedItemRepository.register(
                    eq(USER_ID), eq("KANJI"), anyString(), isNull(), anyString()))
                    .thenReturn(1);

            boolean isNew = service.register(USER_ID, LookupTargetType.KANJI, null, "日");

            assertThat(isNew).isTrue();
            verify(learnedItemRepository).register(USER_ID, "KANJI", "ref:日", null, "日");
        }

        @Test
        @DisplayName("KANJI → khoá được trim khoảng trắng thừa")
        void kanjiTrimsSourceRef() {
            when(learnedItemRepository.register(
                    eq(USER_ID), eq("KANJI"), anyString(), isNull(), anyString()))
                    .thenReturn(1);

            service.register(USER_ID, LookupTargetType.KANJI, null, "  日  ");

            verify(learnedItemRepository).register(USER_ID, "KANJI", "ref:日", null, "  日  ");
        }
    }

    // ============================================================
    // Guard — thiếu khoá / thiếu tham số
    // ============================================================

    @Nested
    @DisplayName("Guard khi thiếu dữ liệu")
    class Guards {

        @Test
        @DisplayName("VOCAB thiếu targetId → false, không gọi DB")
        void vocabWithoutId_returnsFalse() {
            assertThat(service.register(USER_ID, LookupTargetType.VOCAB, null, null)).isFalse();

            verify(learnedItemRepository, never()).register(
                    anyLong(), anyString(), anyString(), anyLong(), anyString());
        }

        @Test
        @DisplayName("KANJI thiếu sourceRef → false, không gọi DB")
        void kanjiWithoutRef_returnsFalse() {
            assertThat(service.register(USER_ID, LookupTargetType.KANJI, null, null)).isFalse();

            verify(learnedItemRepository, never()).register(
                    anyLong(), anyString(), anyString(), anyLong(), anyString());
        }

        @Test
        @DisplayName("KANJI có sourceRef rỗng → false, không gọi DB")
        void kanjiWithBlankRef_returnsFalse() {
            assertThat(service.register(USER_ID, LookupTargetType.KANJI, null, "   ")).isFalse();

            verify(learnedItemRepository, never()).register(
                    anyLong(), anyString(), anyString(), anyLong(), anyString());
        }

        @Test
        @DisplayName("userId null → false, không gọi DB")
        void nullUserId_returnsFalse() {
            assertThat(service.register(null, LookupTargetType.VOCAB, 100L, null)).isFalse();

            verify(learnedItemRepository, never()).register(
                    anyLong(), anyString(), anyString(), anyLong(), anyString());
        }

        @Test
        @DisplayName("targetType null → false, không gọi DB")
        void nullTargetType_returnsFalse() {
            assertThat(service.register(USER_ID, null, 100L, null)).isFalse();

            verify(learnedItemRepository, never()).register(
                    anyLong(), anyString(), anyString(), anyLong(), anyString());
        }
    }

    // ============================================================
    // Kết quả register
    // ============================================================

    @Nested
    @DisplayName("Kết quả register")
    class Result {

        @Test
        @DisplayName("affected = 1 → mới (lần đầu học)")
        void affectedOne_isNew() {
            when(learnedItemRepository.register(
                    eq(USER_ID), eq("VOCAB"), eq("id:1"), eq(1L), isNull()))
                    .thenReturn(1);

            assertThat(service.register(USER_ID, LookupTargetType.VOCAB, 1L, null)).isTrue();
        }

        @Test
        @DisplayName("affected = 0 → đã học trước đó (conflict)")
        void affectedZero_notNew() {
            when(learnedItemRepository.register(
                    eq(USER_ID), eq("VOCAB"), eq("id:1"), eq(1L), isNull()))
                    .thenReturn(0);

            assertThat(service.register(USER_ID, LookupTargetType.VOCAB, 1L, null)).isFalse();
        }

        @Test
        @DisplayName("Cùng 1 mục đăng ký 2 lần → lần 1 mới, lần 2 không mới")
        void sameItemTwice_secondIsNotNew() {
            when(learnedItemRepository.register(
                    eq(USER_ID), eq("VOCAB"), eq("id:5"), eq(5L), isNull()))
                    .thenReturn(1)   // lần đầu: INSERT thành công
                    .thenReturn(0);  // lần sau: ON CONFLICT DO NOTHING

            assertThat(service.register(USER_ID, LookupTargetType.VOCAB, 5L, null)).isTrue();
            assertThat(service.register(USER_ID, LookupTargetType.VOCAB, 5L, null)).isFalse();
        }
    }
}
