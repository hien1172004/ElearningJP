package com.jp.elearningjp.service.curriculum.section;

import com.jp.elearningjp.dto.request.curriculum.LessonKanjiItemCreateRequest;
import com.jp.elearningjp.dto.request.curriculum.LessonKanjiItemUpdateRequest;
import com.jp.elearningjp.dto.request.curriculum.LessonKanjiRelatedVocabRequest;
import com.jp.elearningjp.dto.response.curriculum.LessonKanjiItemResponse;
import com.jp.elearningjp.dto.response.dictionary.KanjiDetailResponse;
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
import com.jp.elearningjp.shared.enums.CharacterType;
import com.jp.elearningjp.shared.enums.LessonSectionType;
import com.jp.elearningjp.shared.util.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit test cho {@link KanjiSectionItemService}.
 *
 * <p>V25+: Related vocab có thể do giáo viên tự chọn (lưu trong
 * {@code lesson_kanji_related_vocabulary}) — fallback về auto-derive từ
 * {@code vocabulary_characters} nếu giáo viên chưa chọn.</p>
 */
@ExtendWith(MockitoExtension.class)
class KanjiSectionItemServiceTest {

    @Mock LessonSectionRepository sectionRepository;
    @Mock LessonKanjiItemRepository kanjiItemRepository;
    @Mock LessonKanjiRelatedVocabRepository relatedVocabRepository;
    @Mock WritingCharacterRepository writingCharacterRepository;
    @Mock VocabularyRepository vocabularyRepository;
    @Mock VocabularyCharacterRepository vocabularyCharacterRepository;
    @Mock LessonSectionMapper mapper;
    @Mock DictionaryService dictionaryService;
    @Mock CurrentUserService currentUserService;

    @InjectMocks KanjiSectionItemService service;

    private LessonSection section;
    private WritingCharacter character;
    private Vocabulary vocab1;
    private Vocabulary vocab2;
    private User teacher;
    private LessonKanjiItem existingItem;

    @BeforeEach
    void setUp() {
        section = LessonSection.builder()
                .sectionType(LessonSectionType.KANJI)
                .orderIndex(1)
                .build();
        section.setId(1L);

        character = WritingCharacter.builder()
                .character("学")
                .charType(CharacterType.KANJI)
                .build();
        character.setId(10L);

        vocab1 = Vocabulary.builder().word("学生").hiragana("がくせい").build();
        vocab1.setId(100L);
        vocab2 = Vocabulary.builder().word("学校").hiragana("がっこう").build();
        vocab2.setId(200L);

        teacher = User.builder().build();
        teacher.setId(999L);

        existingItem = LessonKanjiItem.builder()
                .section(section)
                .character(character)
                .orderIndex(1)
                .build();
        existingItem.setId(50L);
    }

    // ============================================================
    // getKanjiItem — lấy chi tiết 1 kanji item
    // ============================================================

    @Test
    @DisplayName("getKanjiItem: trả về kanji item kèm related vocab override (nếu có)")
    void getKanjiItem_returnsTeacherOverride() {
        LessonKanjiRelatedVocab existingRv = LessonKanjiRelatedVocab.builder()
                .kanjiItem(existingItem).vocab(vocab1).build();
        existingRv.setId(500L);

        when(kanjiItemRepository.findById(50L)).thenReturn(Optional.of(existingItem));
        when(relatedVocabRepository.findByKanjiItemIdAndDeletedFalse(50L)).thenReturn(List.of(existingRv));
        when(mapper.toKanjiItemResponse(any(LessonKanjiItem.class),
                eq(Map.of()), eq(Map.of(50L, List.of(existingRv)))))
                .thenReturn(LessonKanjiItemResponse.builder().id(50L).build());

        LessonKanjiItemResponse result = service.getKanjiItem(50L);

        assertThat(result).isNotNull();
        verify(mapper).toKanjiItemResponse(eq(existingItem), eq(Map.of()),
                eq(Map.of(50L, List.of(existingRv))));
        // Auto-derive không được gọi vì đã có teacher override
        verify(vocabularyCharacterRepository, never()).findRelatedVocabulariesByCharacterId(anyLong());
    }

    @Test
    @DisplayName("getKanjiItem: fallback auto-derive khi chưa có related vocab override")
    void getKanjiItem_fallbackAutoDerive() {
        when(kanjiItemRepository.findById(50L)).thenReturn(Optional.of(existingItem));
        when(relatedVocabRepository.findByKanjiItemIdAndDeletedFalse(50L)).thenReturn(List.of());
        when(vocabularyCharacterRepository.findRelatedVocabulariesByCharacterId(10L))
                .thenReturn(List.of(vocab1, vocab2));
        when(mapper.toKanjiItemResponse(any(LessonKanjiItem.class),
                eq(Map.of(10L, List.of(vocab1, vocab2))), eq(Map.of())))
                .thenReturn(LessonKanjiItemResponse.builder().id(50L).build());

        LessonKanjiItemResponse result = service.getKanjiItem(50L);

        assertThat(result).isNotNull();
        verify(vocabularyCharacterRepository).findRelatedVocabulariesByCharacterId(10L);
    }

    @Test
    @DisplayName("getKanjiItem: throw NOT_FOUND khi item không tồn tại")
    void getKanjiItem_notFound_throws() {
        when(kanjiItemRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getKanjiItem(999L))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.LESSON_KANJI_ITEM_NOT_FOUND);
    }

    @Test
    @DisplayName("getKanjiItem: throw NOT_FOUND khi item đã bị xoá mềm")
    void getKanjiItem_deleted_throws() {
        existingItem.softDelete();
        when(kanjiItemRepository.findById(50L)).thenReturn(Optional.of(existingItem));

        assertThatThrownBy(() -> service.getKanjiItem(50L))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.LESSON_KANJI_ITEM_NOT_FOUND);
    }

    // ============================================================
    // addKanjiItem — không có related vocab (fallback auto-derive)
    // ============================================================

    @Test
    @DisplayName("addKanjiItem: không có relatedVocab → fallback auto-derive từ từ điển")
    void addKanjiItem_noRelatedVocab_autoDerives() {
        LessonKanjiItemCreateRequest request = LessonKanjiItemCreateRequest.builder()
                .character("学")
                .note("Chữ Hán đầu tiên")
                .build();

        when(sectionRepository.findById(1L)).thenReturn(Optional.of(section));
        when(dictionaryService.getKanjiDetail("学"))
                .thenReturn(KanjiDetailResponse.builder().id(10L).character("学").build());
        when(writingCharacterRepository.findByCharacterAndDeletedFalse("学")).thenReturn(Optional.of(character));
        when(kanjiItemRepository.findBySectionIdAndDeletedFalseOrderByOrderIndexAsc(1L)).thenReturn(List.of());
        when(kanjiItemRepository.save(any(LessonKanjiItem.class))).thenAnswer(inv -> {
            LessonKanjiItem item = inv.getArgument(0);
            item.setId(50L);
            return item;
        });
        when(relatedVocabRepository.findByKanjiItemIdAndDeletedFalse(50L)).thenReturn(List.of());
        when(vocabularyCharacterRepository.findRelatedVocabulariesByCharacterId(10L))
                .thenReturn(List.of(vocab1, vocab2));
        // Auto-derive path: mapper.toKanjiItemResponse(saved, relatedVocabs) → internally
        // wraps list → Map.of(10L, [vocab1, vocab2]) rồi gọi overload 3-arg.
        when(mapper.toKanjiItemResponse(any(LessonKanjiItem.class), any(Map.class), any(Map.class)))
                .thenReturn(LessonKanjiItemResponse.builder().id(50L).build());

        LessonKanjiItemResponse result = service.addKanjiItem(1L, request);

        assertThat(result).isNotNull();
        verify(dictionaryService).getKanjiDetail("学");
        verify(writingCharacterRepository).findByCharacterAndDeletedFalse("学");
        verify(vocabularyCharacterRepository).findRelatedVocabulariesByCharacterId(10L);
        verify(relatedVocabRepository, never()).save(any());
    }

    @Test
    @DisplayName("addKanjiItem: throw NOT_FOUND khi character không tồn tại trong từ điển")
    void addKanjiItem_characterNotFound_throws() {
        LessonKanjiItemCreateRequest request = LessonKanjiItemCreateRequest.builder()
                .character("???")
                .build();

        when(sectionRepository.findById(1L)).thenReturn(Optional.of(section));
        when(dictionaryService.getKanjiDetail("???"))
                .thenThrow(new AppException(ErrorCode.CHARACTER_NOT_FOUND));

        assertThatThrownBy(() -> service.addKanjiItem(1L, request))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.CHARACTER_NOT_FOUND);
    }

    @Test
    @DisplayName("addKanjiItem: throw DUPLICATED khi chữ Hán đã tồn tại trong section")
    void addKanjiItem_duplicateKanji_throws() {
        LessonKanjiItemCreateRequest request = LessonKanjiItemCreateRequest.builder()
                .character("学")
                .build();

        when(sectionRepository.findById(1L)).thenReturn(Optional.of(section));
        when(dictionaryService.getKanjiDetail("学")).thenReturn(KanjiDetailResponse.builder().id(10L).build());
        when(writingCharacterRepository.findByCharacterAndDeletedFalse("学")).thenReturn(Optional.of(character));
        when(kanjiItemRepository.findBySectionIdAndDeletedFalseOrderByOrderIndexAsc(1L))
                .thenReturn(List.of(existingItem));

        assertThatThrownBy(() -> service.addKanjiItem(1L, request))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.LESSON_KANJI_ITEM_DUPLICATED);
    }

    @Test
    @DisplayName("addKanjiItem: character null/blank → throw NOT_FOUND")
    void addKanjiItem_blankCharacter_throws() {
        LessonKanjiItemCreateRequest request = LessonKanjiItemCreateRequest.builder()
                .character("   ")
                .build();

        when(sectionRepository.findById(1L)).thenReturn(Optional.of(section));

        assertThatThrownBy(() -> service.addKanjiItem(1L, request))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.CHARACTER_NOT_FOUND);
    }

    // ============================================================
    // addKanjiItem — có related vocab (giáo viên chọn từ từ điển hoặc tạo mới)
    // ============================================================

    @Test
    @DisplayName("addKanjiItem: kèm relatedVocab với vocabId → lưu vào DB và trả về teacher override")
    void addKanjiItem_withExistingVocabId_createsRelatedVocab() {
        LessonKanjiRelatedVocabRequest rvReq = LessonKanjiRelatedVocabRequest.builder()
                .vocabIds(List.of(100L))
                .note("Sinh viên")
                .build();
        LessonKanjiItemCreateRequest request = LessonKanjiItemCreateRequest.builder()
                .character("学")
                .relatedVocabulary(List.of(rvReq))
                .build();

        when(sectionRepository.findById(1L)).thenReturn(Optional.of(section));
        when(dictionaryService.getKanjiDetail("学")).thenReturn(KanjiDetailResponse.builder().id(10L).build());
        when(writingCharacterRepository.findByCharacterAndDeletedFalse("学")).thenReturn(Optional.of(character));
        when(kanjiItemRepository.findBySectionIdAndDeletedFalseOrderByOrderIndexAsc(1L)).thenReturn(List.of());
        when(kanjiItemRepository.save(any(LessonKanjiItem.class))).thenAnswer(inv -> {
            LessonKanjiItem item = inv.getArgument(0);
            item.setId(50L);
            return item;
        });
        when(vocabularyRepository.findByIdAndDeletedFalse(100L)).thenReturn(Optional.of(vocab1));

        // Build entity thật để kiểm tra sau khi save
        LessonKanjiRelatedVocab savedRv = LessonKanjiRelatedVocab.builder()
                .kanjiItem(existingItem).vocab(vocab1).note("Sinh viên").build();
        savedRv.setId(500L);
        // First call (dup check trong loop) → rỗng, second call (buildResponse) → có savedRv
        when(relatedVocabRepository.findByKanjiItemIdAndDeletedFalse(50L))
                .thenReturn(List.of())
                .thenReturn(List.of(savedRv));
        when(relatedVocabRepository.save(any(LessonKanjiRelatedVocab.class))).thenReturn(savedRv);
        when(mapper.toKanjiItemResponse(any(LessonKanjiItem.class), eq(Map.of()), eq(Map.of(50L, List.of(savedRv)))))
                .thenReturn(LessonKanjiItemResponse.builder().id(50L).build());

        LessonKanjiItemResponse result = service.addKanjiItem(1L, request);

        assertThat(result).isNotNull();
        verify(vocabularyRepository).findByIdAndDeletedFalse(100L);
        verify(relatedVocabRepository).save(any(LessonKanjiRelatedVocab.class));
    }

    @Test
    @DisplayName("addKanjiItem: 1 entry có nhiều vocabIds + newVocab → tạo nhiều related vocab entity")
    void addKanjiItem_oneEntryMultipleVocabs_createsMultiple() {
        // Mô phỏng đúng request của user: 1 entry có 2 vocabIds [6, 7] + 1 newVocab
        // → tạo 3 entity LessonKanjiRelatedVocab
        Vocabulary vocab6 = Vocabulary.builder().word("vocab6").hiragana("v6").build();
        vocab6.setId(6L);
        Vocabulary vocab7 = Vocabulary.builder().word("vocab7").hiragana("v7").build();
        vocab7.setId(7L);

        LessonKanjiRelatedVocabRequest.NewVocabPayload newVocabPayload =
                LessonKanjiRelatedVocabRequest.NewVocabPayload.builder()
                        .word("学生").hiragana("がくせい").meaningVi("học sinh").build();
        LessonKanjiRelatedVocabRequest rvReq = LessonKanjiRelatedVocabRequest.builder()
                .vocabIds(List.of(6L, 7L))
                .newVocab(newVocabPayload)
                .note("3 vocab liên quan")
                .build();
        LessonKanjiItemCreateRequest request = LessonKanjiItemCreateRequest.builder()
                .character("入")
                .relatedVocabulary(List.of(rvReq))
                .build();

        when(sectionRepository.findById(1L)).thenReturn(Optional.of(section));
        when(dictionaryService.getKanjiDetail("入")).thenReturn(KanjiDetailResponse.builder().id(6L).build());
        when(writingCharacterRepository.findByCharacterAndDeletedFalse("入")).thenReturn(Optional.of(character));
        when(kanjiItemRepository.findBySectionIdAndDeletedFalseOrderByOrderIndexAsc(1L)).thenReturn(List.of());
        when(kanjiItemRepository.save(any(LessonKanjiItem.class))).thenAnswer(inv -> {
            LessonKanjiItem item = inv.getArgument(0);
            item.setId(50L);
            return item;
        });
        when(vocabularyRepository.findByIdAndDeletedFalse(6L)).thenReturn(Optional.of(vocab6));
        when(vocabularyRepository.findByIdAndDeletedFalse(7L)).thenReturn(Optional.of(vocab7));
        when(currentUserService.getCurrentUser()).thenReturn(teacher);
        when(vocabularyRepository.save(any(Vocabulary.class))).thenAnswer(inv -> {
            Vocabulary v = inv.getArgument(0);
            v.setId(300L);
            return v;
        });

        // 3 entity LessonKanjiRelatedVocab sẽ được save
        LessonKanjiRelatedVocab savedRv1 = LessonKanjiRelatedVocab.builder()
                .kanjiItem(existingItem).vocab(vocab6).note("3 vocab liên quan").build();
        savedRv1.setId(501L);
        LessonKanjiRelatedVocab savedRv2 = LessonKanjiRelatedVocab.builder()
                .kanjiItem(existingItem).vocab(vocab7).note("3 vocab liên quan").build();
        savedRv2.setId(502L);
        LessonKanjiRelatedVocab savedRv3 = LessonKanjiRelatedVocab.builder()
                .kanjiItem(existingItem).vocab(vocab1).note("3 vocab liên quan").build();
        savedRv3.setId(503L);
        when(relatedVocabRepository.save(any(LessonKanjiRelatedVocab.class)))
                .thenReturn(savedRv1, savedRv2, savedRv3);
        // 3 lần trong loop (assertNoDuplicate) + 1 lần trong buildResponse = 4 lần total
        when(relatedVocabRepository.findByKanjiItemIdAndDeletedFalse(50L))
                .thenReturn(List.of()) // call 1: trước save Rv1
                .thenReturn(List.of()) // call 2: trước save Rv2
                .thenReturn(List.of()) // call 3: trước save Rv3
                .thenReturn(List.of(savedRv1, savedRv2, savedRv3)); // call 4: trong buildResponse
        when(mapper.toKanjiItemResponse(any(LessonKanjiItem.class), eq(Map.of()),
                eq(Map.of(50L, List.of(savedRv1, savedRv2, savedRv3)))))
                .thenReturn(LessonKanjiItemResponse.builder().id(50L).build());

        LessonKanjiItemResponse result = service.addKanjiItem(1L, request);

        assertThat(result).isNotNull();
        // 3 vocab cùng được save (2 vocabIds + 1 newVocab)
        verify(relatedVocabRepository, times(3)).save(any(LessonKanjiRelatedVocab.class));
        verify(vocabularyRepository).findByIdAndDeletedFalse(6L);
        verify(vocabularyRepository).findByIdAndDeletedFalse(7L);
        verify(vocabularyRepository).save(any(Vocabulary.class)); // newVocab
        verify(currentUserService).getCurrentUser();
    }

    @Test
    @DisplayName("addKanjiItem: kèm relatedVocab với newVocab → tạo vocab mới rồi gắn vào related")
    void addKanjiItem_withNewVocab_createsBoth() {
        LessonKanjiRelatedVocabRequest.NewVocabPayload newVocabPayload =
                LessonKanjiRelatedVocabRequest.NewVocabPayload.builder()
                        .word("学生")
                        .hiragana("がくせい")
                        .meaningVi("học sinh")
                        .build();
        LessonKanjiRelatedVocabRequest rvReq = LessonKanjiRelatedVocabRequest.builder()
                .newVocab(newVocabPayload)
                .build();
        LessonKanjiItemCreateRequest request = LessonKanjiItemCreateRequest.builder()
                .character("学")
                .relatedVocabulary(List.of(rvReq))
                .build();

        when(sectionRepository.findById(1L)).thenReturn(Optional.of(section));
        when(dictionaryService.getKanjiDetail("学")).thenReturn(KanjiDetailResponse.builder().id(10L).build());
        when(writingCharacterRepository.findByCharacterAndDeletedFalse("学")).thenReturn(Optional.of(character));
        when(kanjiItemRepository.findBySectionIdAndDeletedFalseOrderByOrderIndexAsc(1L)).thenReturn(List.of());
        when(kanjiItemRepository.save(any(LessonKanjiItem.class))).thenAnswer(inv -> {
            LessonKanjiItem item = inv.getArgument(0);
            item.setId(50L);
            return item;
        });
        when(currentUserService.getCurrentUser()).thenReturn(teacher);
        when(vocabularyRepository.save(any(Vocabulary.class))).thenAnswer(inv -> {
            Vocabulary v = inv.getArgument(0);
            v.setId(300L);
            return v;
        });
        LessonKanjiRelatedVocab savedRv = LessonKanjiRelatedVocab.builder()
                .kanjiItem(existingItem).vocab(vocab1).build();
        savedRv.setId(500L);
        when(relatedVocabRepository.save(any(LessonKanjiRelatedVocab.class))).thenReturn(savedRv);
        when(relatedVocabRepository.findByKanjiItemIdAndDeletedFalse(50L)).thenReturn(List.of(savedRv));
        when(mapper.toKanjiItemResponse(any(LessonKanjiItem.class), eq(Map.of()), eq(Map.of(50L, List.of(savedRv)))))
                .thenReturn(LessonKanjiItemResponse.builder().id(50L).build());

        LessonKanjiItemResponse result = service.addKanjiItem(1L, request);

        assertThat(result).isNotNull();
        verify(currentUserService).getCurrentUser();
        verify(vocabularyRepository).save(any(Vocabulary.class));
        verify(relatedVocabRepository).save(any(LessonKanjiRelatedVocab.class));
    }

    @Test
    @DisplayName("addKanjiItem: newVocab trùng (word, hiragana) với vocab đã có → REUSE vocab cũ, không insert mới")
    void addKanjiItem_duplicateNewVocab_reusesExisting() {
        // Mock: vocab với cặp (学生, がくせい) đã tồn tại trong DB với id=300L
        Vocabulary existingVocab = Vocabulary.builder()
                .word("学生").hiragana("がくせい").build();
        existingVocab.setId(300L);

        LessonKanjiRelatedVocabRequest.NewVocabPayload duplicatePayload =
                LessonKanjiRelatedVocabRequest.NewVocabPayload.builder()
                        .word("学生")
                        .hiragana("がくせい")
                        .meaningVi("học sinh")
                        .build();
        LessonKanjiRelatedVocabRequest rvReq = LessonKanjiRelatedVocabRequest.builder()
                .newVocab(duplicatePayload)
                .build();
        LessonKanjiItemCreateRequest request = LessonKanjiItemCreateRequest.builder()
                .character("学")
                .relatedVocabulary(List.of(rvReq))
                .build();

        when(sectionRepository.findById(1L)).thenReturn(Optional.of(section));
        when(dictionaryService.getKanjiDetail("学")).thenReturn(KanjiDetailResponse.builder().id(10L).build());
        when(writingCharacterRepository.findByCharacterAndDeletedFalse("学")).thenReturn(Optional.of(character));
        when(kanjiItemRepository.findBySectionIdAndDeletedFalseOrderByOrderIndexAsc(1L)).thenReturn(List.of());
        when(kanjiItemRepository.save(any(LessonKanjiItem.class))).thenAnswer(inv -> {
            LessonKanjiItem item = inv.getArgument(0);
            item.setId(50L);
            return item;
        });
        // Service gọi lookup trùng TRƯỚC khi save → trả về existingVocab
        when(vocabularyRepository.findByWordAndHiraganaAndDeletedFalse("学生", "がくせい"))
                .thenReturn(Optional.of(existingVocab));

        LessonKanjiRelatedVocab savedRv = LessonKanjiRelatedVocab.builder()
                .kanjiItem(existingItem).vocab(existingVocab).build();
        savedRv.setId(500L);
        when(relatedVocabRepository.save(any(LessonKanjiRelatedVocab.class))).thenReturn(savedRv);
        // 2 lần: 1 lần trong assertNoDuplicate (rỗng) + 1 lần trong buildResponse
        when(relatedVocabRepository.findByKanjiItemIdAndDeletedFalse(50L))
                .thenReturn(List.of())
                .thenReturn(List.of(savedRv));
        when(mapper.toKanjiItemResponse(any(LessonKanjiItem.class), eq(Map.of()), eq(Map.of(50L, List.of(savedRv)))))
                .thenReturn(LessonKanjiItemResponse.builder().id(50L).build());

        LessonKanjiItemResponse result = service.addKanjiItem(1L, request);

        assertThat(result).isNotNull();
        // Verify: lookup duplicate được gọi
        verify(vocabularyRepository).findByWordAndHiraganaAndDeletedFalse("学生", "がくせい");
        // Verify: KHÔNG gọi save (vì reuse existing)
        verify(vocabularyRepository, never()).save(any(Vocabulary.class));
        // Verify: KHÔNG cần currentUser (vì không tạo mới)
        verify(currentUserService, never()).getCurrentUser();
        // Vẫn save LessonKanjiRelatedVocab trỏ tới existing vocab
        verify(relatedVocabRepository).save(any(LessonKanjiRelatedVocab.class));
    }

    @Test
    @DisplayName("addKanjiItem: relatedVocab request không có vocabIds và newVocab → throw INVALID_REQUEST")
    void addKanjiItem_invalidRelatedVocabRequest_throws() {
        // newVocab có word nhưng thiếu hiragana → isValid() = false
        LessonKanjiRelatedVocabRequest.NewVocabPayload incomplete =
                LessonKanjiRelatedVocabRequest.NewVocabPayload.builder()
                        .word("学生")
                        .build();
        LessonKanjiRelatedVocabRequest bad = LessonKanjiRelatedVocabRequest.builder()
                .newVocab(incomplete)
                .build();
        LessonKanjiItemCreateRequest request = LessonKanjiItemCreateRequest.builder()
                .character("学")
                .relatedVocabulary(List.of(bad))
                .build();

        when(sectionRepository.findById(1L)).thenReturn(Optional.of(section));
        when(dictionaryService.getKanjiDetail("学")).thenReturn(KanjiDetailResponse.builder().id(10L).build());
        when(writingCharacterRepository.findByCharacterAndDeletedFalse("学")).thenReturn(Optional.of(character));
        when(kanjiItemRepository.findBySectionIdAndDeletedFalseOrderByOrderIndexAsc(1L)).thenReturn(List.of());
        when(kanjiItemRepository.save(any(LessonKanjiItem.class))).thenAnswer(inv -> {
            LessonKanjiItem item = inv.getArgument(0);
            item.setId(50L);
            return item;
        });

        assertThatThrownBy(() -> service.addKanjiItem(1L, request))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_REQUEST);
    }

    // ============================================================
    // updateKanjiItem — note
    // ============================================================

    @Test
    @DisplayName("updateKanjiItem: note = '' → set null")
    void updateKanjiItem_emptyNote_setToNull() {
        existingItem.setNote("note cũ");

        LessonKanjiItemUpdateRequest request = LessonKanjiItemUpdateRequest.builder()
                .note("")
                .build();

        when(kanjiItemRepository.findById(50L)).thenReturn(Optional.of(existingItem));
        when(kanjiItemRepository.save(any(LessonKanjiItem.class))).thenAnswer(inv -> inv.getArgument(0));
        when(relatedVocabRepository.findByKanjiItemIdAndDeletedFalse(50L)).thenReturn(List.of());
        when(vocabularyCharacterRepository.findRelatedVocabulariesByCharacterId(10L)).thenReturn(List.of());
        when(mapper.toKanjiItemResponse(any(LessonKanjiItem.class), eq(Map.of()), eq(Map.of())))
                .thenReturn(LessonKanjiItemResponse.builder().id(50L).build());

        service.updateKanjiItem(50L, request);

        assertThat(existingItem.getNote()).isNull();
    }

    @Test
    @DisplayName("updateKanjiItem: note = 'note mới' → giữ nguyên giá trị")
    void updateKanjiItem_nonEmptyNote_keepValue() {
        existingItem.setNote(null);

        LessonKanjiItemUpdateRequest request = LessonKanjiItemUpdateRequest.builder()
                .note("note mới")
                .build();

        when(kanjiItemRepository.findById(50L)).thenReturn(Optional.of(existingItem));
        when(kanjiItemRepository.save(any(LessonKanjiItem.class))).thenAnswer(inv -> inv.getArgument(0));
        when(relatedVocabRepository.findByKanjiItemIdAndDeletedFalse(50L)).thenReturn(List.of());
        when(vocabularyCharacterRepository.findRelatedVocabulariesByCharacterId(10L)).thenReturn(List.of());
        when(mapper.toKanjiItemResponse(any(LessonKanjiItem.class), eq(Map.of()), eq(Map.of())))
                .thenReturn(LessonKanjiItemResponse.builder().id(50L).build());

        service.updateKanjiItem(50L, request);

        assertThat(existingItem.getNote()).isEqualTo("note mới");
    }

    @Test
    @DisplayName("updateKanjiItem: không truyền relatedVocab → giữ nguyên danh sách cũ")
    void updateKanjiItem_noRelatedVocabInRequest_keepsExisting() {
        LessonKanjiItemUpdateRequest request = LessonKanjiItemUpdateRequest.builder()
                .note("note mới")
                .build();

        when(kanjiItemRepository.findById(50L)).thenReturn(Optional.of(existingItem));
        when(kanjiItemRepository.save(any(LessonKanjiItem.class))).thenAnswer(inv -> inv.getArgument(0));
        when(relatedVocabRepository.findByKanjiItemIdAndDeletedFalse(50L)).thenReturn(List.of());
        when(vocabularyCharacterRepository.findRelatedVocabulariesByCharacterId(10L)).thenReturn(List.of(vocab1));
        // Auto-derive path: buildResponse truyền Map.of(10L, [vocab1]) + Map.of()
        when(mapper.toKanjiItemResponse(any(LessonKanjiItem.class),
                eq(Map.of(10L, List.of(vocab1))), eq(Map.of())))
                .thenReturn(LessonKanjiItemResponse.builder().id(50L).build());

        service.updateKanjiItem(50L, request);

        // Không save thêm related vocab nào
        verify(relatedVocabRepository, never()).save(any());
        verify(relatedVocabRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("updateKanjiItem: truyền relatedVocab = [] → xoá hết related vocab hiện tại")
    void updateKanjiItem_emptyRelatedVocabList_deletesAll() {
        LessonKanjiRelatedVocab existingRv = LessonKanjiRelatedVocab.builder()
                .kanjiItem(existingItem).vocab(vocab1).build();
        existingRv.setId(500L);

        LessonKanjiItemUpdateRequest request = LessonKanjiItemUpdateRequest.builder()
                .note(null)
                .relatedVocabulary(List.of())
                .build();

        when(kanjiItemRepository.findById(50L)).thenReturn(Optional.of(existingItem));
        when(kanjiItemRepository.save(any(LessonKanjiItem.class))).thenAnswer(inv -> inv.getArgument(0));
        when(relatedVocabRepository.findByKanjiItemIdAndDeletedFalse(50L)).thenReturn(List.of(existingRv));

        service.updateKanjiItem(50L, request);

        assertThat(existingRv.isDeleted()).isTrue();
        verify(relatedVocabRepository).saveAll(any());
    }

    @Test
    @DisplayName("updateKanjiItem: truyền relatedVocab mới → thay thế danh sách cũ")
    void updateKanjiItem_replaceRelatedVocab() {
        LessonKanjiRelatedVocab oldRv = LessonKanjiRelatedVocab.builder()
                .kanjiItem(existingItem).vocab(vocab1).build();
        oldRv.setId(500L);

        LessonKanjiRelatedVocabRequest newRv = LessonKanjiRelatedVocabRequest.builder()
                .vocabIds(List.of(200L))
                .build();
        LessonKanjiItemUpdateRequest request = LessonKanjiItemUpdateRequest.builder()
                .relatedVocabulary(List.of(newRv))
                .build();

        when(kanjiItemRepository.findById(50L)).thenReturn(Optional.of(existingItem));
        when(kanjiItemRepository.save(any(LessonKanjiItem.class))).thenAnswer(inv -> inv.getArgument(0));
        when(relatedVocabRepository.findByKanjiItemIdAndDeletedFalse(50L))
                .thenReturn(List.of(oldRv)); // existing → cũ
        when(vocabularyRepository.findByIdAndDeletedFalse(200L)).thenReturn(Optional.of(vocab2));

        LessonKanjiRelatedVocab savedRv = LessonKanjiRelatedVocab.builder()
                .kanjiItem(existingItem).vocab(vocab2).build();
        savedRv.setId(600L);
        when(relatedVocabRepository.save(any(LessonKanjiRelatedVocab.class))).thenReturn(savedRv);

        service.updateKanjiItem(50L, request);

        assertThat(oldRv.isDeleted()).isTrue(); // cái cũ bị soft-delete
        verify(relatedVocabRepository).saveAll(any());  // saveAll existing → soft-delete
        verify(relatedVocabRepository).save(any(LessonKanjiRelatedVocab.class)); // save mới
    }

    @Test
    @DisplayName("updateKanjiItem: item không tồn tại → throw NOT_FOUND")
    void updateKanjiItem_notFound_throws() {
        LessonKanjiItemUpdateRequest request = LessonKanjiItemUpdateRequest.builder()
                .note("note")
                .build();

        when(kanjiItemRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateKanjiItem(999L, request))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.LESSON_KANJI_ITEM_NOT_FOUND);
    }

    // ============================================================
    // deleteKanjiItem
    // ============================================================

    @Test
    @DisplayName("deleteKanjiItem: xoá thành công")
    void deleteKanjiItem_success() {
        when(kanjiItemRepository.findById(50L)).thenReturn(Optional.of(existingItem));
        when(kanjiItemRepository.save(any(LessonKanjiItem.class))).thenAnswer(inv -> inv.getArgument(0));

        service.deleteKanjiItem(50L);

        assertThat(existingItem.isDeleted()).isTrue();
        verify(kanjiItemRepository).save(existingItem);
    }

    @Test
    @DisplayName("deleteKanjiItem: idempotent — gọi 2 lần không lỗi")
    void deleteKanjiItem_idempotent() {
        when(kanjiItemRepository.findById(50L)).thenReturn(Optional.of(existingItem));

        service.deleteKanjiItem(50L);
        assertThat(existingItem.isDeleted()).isTrue();

        service.deleteKanjiItem(50L);

        verify(kanjiItemRepository, times(1)).save(existingItem);
    }

    @Test
    @DisplayName("deleteKanjiItem: throw NOT_FOUND khi item không tồn tại")
    void deleteKanjiItem_notFound_throws() {
        when(kanjiItemRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteKanjiItem(999L))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.LESSON_KANJI_ITEM_NOT_FOUND);
    }

    // ============================================================
    // deleteKanjiItem — cascade soft-delete related vocab
    // ============================================================

    @Test
    @DisplayName("deleteKanjiItem: khi có related vocab → cascade soft-delete tất cả")
    void deleteKanjiItem_cascadesToRelatedVocabs() {
        LessonKanjiRelatedVocab rv1 = LessonKanjiRelatedVocab.builder()
                .kanjiItem(existingItem).vocab(vocab1).build();
        rv1.setId(500L);
        LessonKanjiRelatedVocab rv2 = LessonKanjiRelatedVocab.builder()
                .kanjiItem(existingItem).vocab(vocab2).build();
        rv2.setId(501L);

        when(kanjiItemRepository.findById(50L)).thenReturn(Optional.of(existingItem));
        when(relatedVocabRepository.findByKanjiItemIdAndDeletedFalse(50L))
                .thenReturn(List.of(rv1, rv2));
        when(kanjiItemRepository.save(any(LessonKanjiItem.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        service.deleteKanjiItem(50L);

        // Kanji item bị soft-delete
        assertThat(existingItem.isDeleted()).isTrue();
        // Related vocab cũng bị soft-delete
        assertThat(rv1.isDeleted()).isTrue();
        assertThat(rv2.isDeleted()).isTrue();
        // Verify cascade save được gọi
        verify(relatedVocabRepository).saveAll(List.of(rv1, rv2));
        verify(kanjiItemRepository).save(existingItem);
    }

    @Test
    @DisplayName("deleteKanjiItem: khi không có related vocab → vẫn xoá bình thường")
    void deleteKanjiItem_noRelatedVocabs_works() {
        when(kanjiItemRepository.findById(50L)).thenReturn(Optional.of(existingItem));
        when(relatedVocabRepository.findByKanjiItemIdAndDeletedFalse(50L)).thenReturn(List.of());
        when(kanjiItemRepository.save(any(LessonKanjiItem.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        service.deleteKanjiItem(50L);

        assertThat(existingItem.isDeleted()).isTrue();
        verify(relatedVocabRepository, never()).saveAll(any());
        verify(kanjiItemRepository).save(existingItem);
    }
}