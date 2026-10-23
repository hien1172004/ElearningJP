package com.jp.elearningjp.service.curriculum.section;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit test cho {@link VocabularySectionItemService} — focus vào P1/P2:
 * <ul>
 *   <li>isPrimary validation (P2: chỉ 1 example được primary, throw lỗi rõ ràng)</li>
 *   <li>Validate trùng example trong request (P1-2)</li>
 *   <li>note = "" → set null (P1-3)</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class VocabularySectionItemServiceTest {

    @Mock LessonSectionRepository sectionRepository;
    @Mock LessonVocabularyItemRepository vocabItemRepository;
    @Mock LessonVocabularyExampleRepository exampleRepository;
    @Mock VocabularyRepository vocabularyRepository;
    @Mock LessonSectionMapper mapper;

    @InjectMocks VocabularySectionItemService service;

    private LessonSection section;
    private Vocabulary vocab;
    private LessonVocabularyItem existingItem;

    @BeforeEach
    void setUp() {
        section = LessonSection.builder()
                .sectionType(LessonSectionType.VOCABULARY)
                .orderIndex(1)
                .build();
        section.setId(1L);

        vocab = Vocabulary.builder()
                .word("学生")
                .hiragana("がくせい")
                .romaji("gakusei")
                .meaningVi("học sinh")
                .audioUrl("https://audio.example.com/gakusei.mp3")
                .build();
        vocab.setId(100L);

        existingItem = LessonVocabularyItem.builder()
                .section(section)
                .vocab(vocab)
                .orderIndex(1)
                .examples(new ArrayList<>())
                .build();
        existingItem.setId(50L);
    }

    // ============================================================
    // V24+ addVocabularyItem — LUÔN auto-copy từ vocabulary (không còn client examples)
    // ============================================================

    // ============================================================
    // updateVocabularyItem — note = "" → set null
    // ============================================================

    @Test
    @DisplayName("updateVocabularyItem: note = '' → set null")
    void updateVocabularyItem_emptyNote_setToNull() {
        existingItem.setNote("cũ");

        LessonVocabularyItemUpdateRequest request = LessonVocabularyItemUpdateRequest.builder()
                .note("")
                .build();

        when(vocabItemRepository.findByIdWithExamples(50L)).thenReturn(Optional.of(existingItem));
        when(vocabItemRepository.save(any(LessonVocabularyItem.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mapper.toVocabularyItemResponse(any(LessonVocabularyItem.class)))
                .thenReturn(LessonVocabularyItemResponse.builder().id(50L).build());

        service.updateVocabularyItem(50L, request);

        assertThat(existingItem.getNote()).isNull();
    }

    @Test
    @DisplayName("updateVocabularyItem: customHiragana = '' → set null (xóa override)")
    void updateVocabularyItem_emptyCustomHiragana_setToNull() {
        existingItem.setCustomHiragana("がくせい");

        LessonVocabularyItemUpdateRequest request = LessonVocabularyItemUpdateRequest.builder()
                .customHiragana("")
                .build();

        when(vocabItemRepository.findByIdWithExamples(50L)).thenReturn(Optional.of(existingItem));
        when(vocabItemRepository.save(any(LessonVocabularyItem.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mapper.toVocabularyItemResponse(any(LessonVocabularyItem.class)))
                .thenReturn(LessonVocabularyItemResponse.builder().id(50L).build());

        service.updateVocabularyItem(50L, request);

        assertThat(existingItem.getCustomHiragana()).isNull();
    }

    // ============================================================
    // V23+ addVocabularyItem — auto-fill từ từ điển
    // ============================================================

    @Test
    @DisplayName("addVocabularyItem: KHÔNG gửi customXxx → auto-fill từ vocabulary")
    void addVocabularyItem_noOverrides_autoFillFromVocab() {
        LessonVocabularyItemCreateRequest request = LessonVocabularyItemCreateRequest.builder()
                .vocabId(100L)
                // Bỏ qua customHiragana, customRomaji, customAudioUrl, customMeaning
                .build();

        when(sectionRepository.findById(1L)).thenReturn(Optional.of(section));
        // V24+: KHÔNG gửi examples → gọi findByIdWithActiveExamples
        when(vocabularyRepository.findByIdWithActiveExamples(100L)).thenReturn(Optional.of(vocab));
        when(vocabItemRepository.findBySectionIdAndDeletedFalseOrderByOrderIndexAsc(1L))
                .thenReturn(List.of());
        when(vocabItemRepository.save(any(LessonVocabularyItem.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mapper.toVocabularyItemResponse(any(LessonVocabularyItem.class)))
                .thenReturn(LessonVocabularyItemResponse.builder().id(50L).build());

        service.addVocabularyItem(1L, request);

        assertThat(existingItem).isNotNull(); // sanity
        // Verify lưu kết quả đúng từng field custom
        org.mockito.ArgumentCaptor<LessonVocabularyItem> captor =
                org.mockito.ArgumentCaptor.forClass(LessonVocabularyItem.class);
        verify(vocabItemRepository).save(captor.capture());
        LessonVocabularyItem saved = captor.getValue();
        assertThat(saved.getCustomHiragana()).isEqualTo("がくせい");
        assertThat(saved.getCustomRomaji()).isEqualTo("gakusei");
        assertThat(saved.getCustomAudioUrl()).isEqualTo("https://audio.example.com/gakusei.mp3");
        assertThat(saved.getCustomMeaning()).isEqualTo("học sinh");
    }

    @Test
    @DisplayName("addVocabularyItem: gửi customHiragana = '' → set null (không auto-fill hiragana, các trường khác vẫn auto-fill)")
    void addVocabularyItem_emptyOverride_setNull() {
        LessonVocabularyItemCreateRequest request = LessonVocabularyItemCreateRequest.builder()
                .vocabId(100L)
                .customHiragana("")  // xoá override hiragana
                .build();

        when(sectionRepository.findById(1L)).thenReturn(Optional.of(section));
        when(vocabularyRepository.findByIdWithActiveExamples(100L)).thenReturn(Optional.of(vocab));
        when(vocabItemRepository.findBySectionIdAndDeletedFalseOrderByOrderIndexAsc(1L))
                .thenReturn(List.of());
        when(vocabItemRepository.save(any(LessonVocabularyItem.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mapper.toVocabularyItemResponse(any(LessonVocabularyItem.class)))
                .thenReturn(LessonVocabularyItemResponse.builder().id(50L).build());

        service.addVocabularyItem(1L, request);

        org.mockito.ArgumentCaptor<LessonVocabularyItem> captor =
                org.mockito.ArgumentCaptor.forClass(LessonVocabularyItem.class);
        verify(vocabItemRepository).save(captor.capture());
        LessonVocabularyItem saved = captor.getValue();
        assertThat(saved.getCustomHiragana()).isNull();
        assertThat(saved.getCustomRomaji()).isEqualTo("gakusei");
        assertThat(saved.getCustomAudioUrl()).isEqualTo("https://audio.example.com/gakusei.mp3");
        assertThat(saved.getCustomMeaning()).isEqualTo("học sinh");
    }

    @Test
    @DisplayName("addVocabularyItem: gửi customHiragana = 'がくせい!' → dùng override, các trường khác vẫn auto-fill")
    void addVocabularyItem_withOverride_keepValue() {
        LessonVocabularyItemCreateRequest request = LessonVocabularyItemCreateRequest.builder()
                .vocabId(100L)
                .customHiragana("がくせい！")
                .build();

        when(sectionRepository.findById(1L)).thenReturn(Optional.of(section));
        when(vocabularyRepository.findByIdWithActiveExamples(100L)).thenReturn(Optional.of(vocab));
        when(vocabItemRepository.findBySectionIdAndDeletedFalseOrderByOrderIndexAsc(1L))
                .thenReturn(List.of());
        when(vocabItemRepository.save(any(LessonVocabularyItem.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mapper.toVocabularyItemResponse(any(LessonVocabularyItem.class)))
                .thenReturn(LessonVocabularyItemResponse.builder().id(50L).build());

        service.addVocabularyItem(1L, request);

        org.mockito.ArgumentCaptor<LessonVocabularyItem> captor =
                org.mockito.ArgumentCaptor.forClass(LessonVocabularyItem.class);
        verify(vocabItemRepository).save(captor.capture());
        LessonVocabularyItem saved = captor.getValue();
        assertThat(saved.getCustomHiragana()).isEqualTo("がくせい！");
        assertThat(saved.getCustomRomaji()).isEqualTo("gakusei");
        assertThat(saved.getCustomMeaning()).isEqualTo("học sinh");
    }

    @Test
    @DisplayName("addVocabularyItem: vocab.romaji = null (dữ liệu thiếu) → customRomaji cũng null")
    void addVocabularyItem_vocabMissingRomaji_nullOverride() {
        Vocabulary vocabNoRomaji = Vocabulary.builder()
                .word("学校")
                .hiragana("がっこう")
                .romaji(null)  // không có romaji trong từ điển
                .meaningVi("trường học")
                .audioUrl(null)
                .build();
        vocabNoRomaji.setId(200L);

        LessonVocabularyItemCreateRequest request = LessonVocabularyItemCreateRequest.builder()
                .vocabId(200L)
                .build();

        when(sectionRepository.findById(1L)).thenReturn(Optional.of(section));
        when(vocabularyRepository.findByIdWithActiveExamples(200L)).thenReturn(Optional.of(vocabNoRomaji));
        when(vocabItemRepository.findBySectionIdAndDeletedFalseOrderByOrderIndexAsc(1L))
                .thenReturn(List.of());
        when(vocabItemRepository.save(any(LessonVocabularyItem.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mapper.toVocabularyItemResponse(any(LessonVocabularyItem.class)))
                .thenReturn(LessonVocabularyItemResponse.builder().id(50L).build());

        service.addVocabularyItem(1L, request);

        org.mockito.ArgumentCaptor<LessonVocabularyItem> captor =
                org.mockito.ArgumentCaptor.forClass(LessonVocabularyItem.class);
        verify(vocabItemRepository).save(captor.capture());
        LessonVocabularyItem saved = captor.getValue();
        assertThat(saved.getCustomRomaji()).isNull();
        assertThat(saved.getCustomAudioUrl()).isNull();
        assertThat(saved.getCustomHiragana()).isEqualTo("がっこう");
        assertThat(saved.getCustomMeaning()).isEqualTo("trường học");
    }

    // ============================================================
    // V23+ updateVocabularyItem — customXxx mới
    // ============================================================

    @Test
    @DisplayName("updateVocabularyItem: customRomaji giá trị mới → set; các trường khác KHÔNG gửi → giữ nguyên")
    void updateVocabularyItem_setCustomRomaji() {
        existingItem.setCustomRomaji("gakusei");

        LessonVocabularyItemUpdateRequest request = LessonVocabularyItemUpdateRequest.builder()
                .customRomaji("gakusei alt")
                .build();

        when(vocabItemRepository.findByIdWithExamples(50L)).thenReturn(Optional.of(existingItem));
        when(vocabItemRepository.save(any(LessonVocabularyItem.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mapper.toVocabularyItemResponse(any(LessonVocabularyItem.class)))
                .thenReturn(LessonVocabularyItemResponse.builder().id(50L).build());

        service.updateVocabularyItem(50L, request);

        assertThat(existingItem.getCustomRomaji()).isEqualTo("gakusei alt");
    }

    @Test
    @DisplayName("updateVocabularyItem: customRomaji = '' → set null (xoá override)")
    void updateVocabularyItem_emptyCustomRomaji_setToNull() {
        existingItem.setCustomRomaji("gakusei old");

        LessonVocabularyItemUpdateRequest request = LessonVocabularyItemUpdateRequest.builder()
                .customRomaji("")
                .build();

        when(vocabItemRepository.findByIdWithExamples(50L)).thenReturn(Optional.of(existingItem));
        when(vocabItemRepository.save(any(LessonVocabularyItem.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mapper.toVocabularyItemResponse(any(LessonVocabularyItem.class)))
                .thenReturn(LessonVocabularyItemResponse.builder().id(50L).build());

        service.updateVocabularyItem(50L, request);

        assertThat(existingItem.getCustomRomaji()).isNull();
    }

    @Test
    @DisplayName("updateVocabularyItem: customAudioUrl = '' → set null (xoá override)")
    void updateVocabularyItem_emptyCustomAudioUrl_setToNull() {
        existingItem.setCustomAudioUrl("https://old.example.com/a.mp3");

        LessonVocabularyItemUpdateRequest request = LessonVocabularyItemUpdateRequest.builder()
                .customAudioUrl("")
                .build();

        when(vocabItemRepository.findByIdWithExamples(50L)).thenReturn(Optional.of(existingItem));
        when(vocabItemRepository.save(any(LessonVocabularyItem.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mapper.toVocabularyItemResponse(any(LessonVocabularyItem.class)))
                .thenReturn(LessonVocabularyItemResponse.builder().id(50L).build());

        service.updateVocabularyItem(50L, request);

        assertThat(existingItem.getCustomAudioUrl()).isNull();
    }

    // ============================================================
    // V24+ addVocabularyItem — auto-copy examples từ vocabulary
    // ============================================================

    /** Helper: tạo vocab entity kèm danh sách examples để test auto-copy. */
    private Vocabulary vocabWithExamples(Vocabulary base, VocabularyExample... exs) {
        Vocabulary v = Vocabulary.builder()
                .word(base.getWord())
                .hiragana(base.getHiragana())
                .romaji(base.getRomaji())
                .meaningVi(base.getMeaningVi())
                .audioUrl(base.getAudioUrl())
                .examples(new ArrayList<>(List.of(exs)))
                .build();
        v.setId(base.getId());
        return v;
    }

    @Test
    @DisplayName("V24 addVocabularyItem: LUÔN auto-copy tất cả examples từ vocabulary.examples (sort theo orderIndex)")
    void addVocabularyItem_autoCopyFromVocab() {
        VocabularyExample src1 = VocabularyExample.builder()
                .exampleJp("私は学生です。")
                .exampleVi("Tôi là học sinh.")
                .exampleRomaji("watashi wa gakusei desu.")
                .orderIndex(1)
                .primary(true)
                .build();
        src1.setId(1001L);
        VocabularyExample src2 = VocabularyExample.builder()
                .exampleJp("学生です。")
                .exampleVi("Là học sinh.")
                .exampleRomaji("gakusei desu.")
                .orderIndex(2)
                .primary(false)
                .build();
        src2.setId(1002L);
        Vocabulary vocab = vocabWithExamples(this.vocab, src1, src2);

        LessonVocabularyItemCreateRequest request = LessonVocabularyItemCreateRequest.builder()
                .vocabId(100L)
                // KHÔNG gửi examples
                .build();

        when(sectionRepository.findById(1L)).thenReturn(Optional.of(section));
        // V24+: KHÔNG gửi examples → service gọi findByIdWithActiveExamples
        when(vocabularyRepository.findByIdWithActiveExamples(100L)).thenReturn(Optional.of(vocab));
        when(vocabItemRepository.findBySectionIdAndDeletedFalseOrderByOrderIndexAsc(1L))
                .thenReturn(List.of());
        when(vocabItemRepository.save(any(LessonVocabularyItem.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mapper.toVocabularyItemResponse(any(LessonVocabularyItem.class)))
                .thenReturn(LessonVocabularyItemResponse.builder().id(50L).build());

        service.addVocabularyItem(1L, request);

        org.mockito.ArgumentCaptor<LessonVocabularyItem> captor =
                org.mockito.ArgumentCaptor.forClass(LessonVocabularyItem.class);
        verify(vocabItemRepository).save(captor.capture());
        LessonVocabularyItem saved = captor.getValue();

        // Verify 2 examples đã auto-copy đúng nội dung + orderIndex
        assertThat(saved.getExamples()).hasSize(2);
        LessonVocabularyExample ex1 = saved.getExamples().get(0);
        assertThat(ex1.getExampleJp()).isEqualTo("私は学生です。");
        assertThat(ex1.getExampleVi()).isEqualTo("Tôi là học sinh.");
        assertThat(ex1.getExampleRomaji()).isEqualTo("watashi wa gakusei desu.");
        assertThat(ex1.getOrderIndex()).isEqualTo(1);
        assertThat(ex1.isPrimary()).isTrue();

        LessonVocabularyExample ex2 = saved.getExamples().get(1);
        assertThat(ex2.getExampleJp()).isEqualTo("学生です。");
        assertThat(ex2.getExampleVi()).isEqualTo("Là học sinh.");
        assertThat(ex2.getOrderIndex()).isEqualTo(2);
        assertThat(ex2.isPrimary()).isFalse();

        // Verify KHÔNG gọi findByIdAndDeletedFalse (vì đã gọi findWithActiveExamples)
        verify(vocabularyRepository, never()).findByIdAndDeletedFalse(any());
    }

    @Test
    @DisplayName("V24 addVocabularyItem: auto-copy bỏ qua examples đã bị soft-delete trong vocabulary")
    void addVocabularyItem_autoCopy_skipsDeleted() {
        VocabularyExample src1 = VocabularyExample.builder()
                .exampleJp("JP-active").exampleVi("VI-active").orderIndex(1).build();
        src1.setId(1001L);
        // src2 đã bị soft-delete → KHÔNG copy (dùng setter vì builder không có deleted())
        VocabularyExample src2 = VocabularyExample.builder()
                .exampleJp("JP-deleted").exampleVi("VI-deleted").orderIndex(2).build();
        src2.setId(1002L);
        src2.softDelete();
        Vocabulary vocab = vocabWithExamples(this.vocab, src1, src2);

        LessonVocabularyItemCreateRequest request = LessonVocabularyItemCreateRequest.builder()
                .vocabId(100L)
                .build();

        when(sectionRepository.findById(1L)).thenReturn(Optional.of(section));
        when(vocabularyRepository.findByIdWithActiveExamples(100L)).thenReturn(Optional.of(vocab));
        when(vocabItemRepository.findBySectionIdAndDeletedFalseOrderByOrderIndexAsc(1L))
                .thenReturn(List.of());
        when(vocabItemRepository.save(any(LessonVocabularyItem.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mapper.toVocabularyItemResponse(any(LessonVocabularyItem.class)))
                .thenReturn(LessonVocabularyItemResponse.builder().id(50L).build());

        service.addVocabularyItem(1L, request);

        org.mockito.ArgumentCaptor<LessonVocabularyItem> captor =
                org.mockito.ArgumentCaptor.forClass(LessonVocabularyItem.class);
        verify(vocabItemRepository).save(captor.capture());
        LessonVocabularyItem saved = captor.getValue();

        assertThat(saved.getExamples()).hasSize(1);
        assertThat(saved.getExamples().get(0).getExampleJp()).isEqualTo("JP-active");
    }

    // ============================================================
    // V26+ addVocabularyItem — teacher cung cấp examples trong payload
    // ============================================================

    @Test
    @DisplayName("V26 addVocabularyItem: gửi examples = [] → AUTO-COPY từ vocab (giống null)")
    void addVocabularyItem_emptyExamplesList_autoCopyFromVocab() {
        // Vocab có 2 examples sẵn
        VocabularyExample src1 = VocabularyExample.builder()
                .exampleJp("JP1").exampleVi("VI1").orderIndex(1).primary(true).build();
        src1.setId(1001L);
        VocabularyExample src2 = VocabularyExample.builder()
                .exampleJp("JP2").exampleVi("VI2").orderIndex(2).build();
        src2.setId(1002L);
        Vocabulary vocab = vocabWithExamples(this.vocab, src1, src2);

        LessonVocabularyItemCreateRequest request = LessonVocabularyItemCreateRequest.builder()
                .vocabId(100L)
                .examples(List.of()) // empty → VẪN auto-copy
                .build();

        when(sectionRepository.findById(1L)).thenReturn(Optional.of(section));
        // Vì examples = [] → gọi findByIdWithActiveExamples (eager)
        when(vocabularyRepository.findByIdWithActiveExamples(100L)).thenReturn(Optional.of(vocab));
        when(vocabItemRepository.findBySectionIdAndDeletedFalseOrderByOrderIndexAsc(1L))
                .thenReturn(List.of());
        when(vocabItemRepository.save(any(LessonVocabularyItem.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mapper.toVocabularyItemResponse(any(LessonVocabularyItem.class)))
                .thenReturn(LessonVocabularyItemResponse.builder().id(50L).build());

        service.addVocabularyItem(1L, request);

        org.mockito.ArgumentCaptor<LessonVocabularyItem> captor =
                org.mockito.ArgumentCaptor.forClass(LessonVocabularyItem.class);
        verify(vocabItemRepository).save(captor.capture());
        LessonVocabularyItem saved = captor.getValue();

        // VẪN auto-copy 2 examples từ vocab
        assertThat(saved.getExamples()).hasSize(2);
        assertThat(saved.getExamples().get(0).getExampleJp()).isEqualTo("JP1");
        assertThat(saved.getExamples().get(1).getExampleJp()).isEqualTo("JP2");
    }

    @Test
    @DisplayName("V26 addVocabularyItem: gửi examples = null → AUTO-COPY từ vocab (mặc định)")
    void addVocabularyItem_nullExamples_autoCopyFromVocab() {
        LessonVocabularyItemCreateRequest request = LessonVocabularyItemCreateRequest.builder()
                .vocabId(100L)
                // KHÔNG gửi examples (null)
                .build();

        when(sectionRepository.findById(1L)).thenReturn(Optional.of(section));
        when(vocabularyRepository.findByIdWithActiveExamples(100L)).thenReturn(Optional.of(vocab));
        when(vocabItemRepository.findBySectionIdAndDeletedFalseOrderByOrderIndexAsc(1L))
                .thenReturn(List.of());
        when(vocabItemRepository.save(any(LessonVocabularyItem.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mapper.toVocabularyItemResponse(any(LessonVocabularyItem.class)))
                .thenReturn(LessonVocabularyItemResponse.builder().id(50L).build());

        service.addVocabularyItem(1L, request);

        org.mockito.ArgumentCaptor<LessonVocabularyItem> captor =
                org.mockito.ArgumentCaptor.forClass(LessonVocabularyItem.class);
        verify(vocabItemRepository).save(captor.capture());
        LessonVocabularyItem saved = captor.getValue();

        // KHÔNG gọi findByIdAndDeletedFalse (vì đã gọi findWithActiveExamples)
        verify(vocabularyRepository, never()).findByIdAndDeletedFalse(any());
        assertThat(saved).isNotNull();
    }

    @Test
    @DisplayName("V26 addVocabularyItem: gửi examples list → tạo example theo list, KHÔNG auto-copy từ vocab")
    void addVocabularyItem_teacherExamplesList_createsFromList() {
        LessonVocabularyExampleCreateRequest ex1 = LessonVocabularyExampleCreateRequest.builder()
                .exampleJp("私は学生です。").exampleVi("Tôi là học sinh.")
                .isPrimary(true).orderIndex(1).build();
        LessonVocabularyExampleCreateRequest ex2 = LessonVocabularyExampleCreateRequest.builder()
                .exampleJp("学生は毎日勉強します。").exampleVi("Học sinh học mỗi ngày.")
                .orderIndex(2).build();

        LessonVocabularyItemCreateRequest request = LessonVocabularyItemCreateRequest.builder()
                .vocabId(100L)
                .examples(List.of(ex1, ex2))
                .build();

        when(sectionRepository.findById(1L)).thenReturn(Optional.of(section));
        when(vocabularyRepository.findByIdAndDeletedFalse(100L)).thenReturn(Optional.of(vocab));
        when(vocabItemRepository.findBySectionIdAndDeletedFalseOrderByOrderIndexAsc(1L))
                .thenReturn(List.of());
        when(vocabItemRepository.save(any(LessonVocabularyItem.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mapper.toVocabularyItemResponse(any(LessonVocabularyItem.class)))
                .thenReturn(LessonVocabularyItemResponse.builder().id(50L).build());

        service.addVocabularyItem(1L, request);

        org.mockito.ArgumentCaptor<LessonVocabularyItem> captor =
                org.mockito.ArgumentCaptor.forClass(LessonVocabularyItem.class);
        verify(vocabItemRepository).save(captor.capture());
        LessonVocabularyItem saved = captor.getValue();

        assertThat(saved.getExamples()).hasSize(2);
        assertThat(saved.getExamples().get(0).getExampleJp()).isEqualTo("私は学生です。");
        assertThat(saved.getExamples().get(0).isPrimary()).isTrue();
        assertThat(saved.getExamples().get(1).isPrimary()).isFalse();
    }

    @Test
    @DisplayName("V26 addVocabularyItem: examples list có nhiều primary=true → chỉ giữ entry đầu tiên")
    void addVocabularyItem_teacherExamplesList_multiplePrimary_keepsFirst() {
        LessonVocabularyExampleCreateRequest ex1 = LessonVocabularyExampleCreateRequest.builder()
                .exampleJp("JP1").exampleVi("VI1").isPrimary(true).orderIndex(1).build();
        LessonVocabularyExampleCreateRequest ex2 = LessonVocabularyExampleCreateRequest.builder()
                .exampleJp("JP2").exampleVi("VI2").isPrimary(true).orderIndex(2).build();

        LessonVocabularyItemCreateRequest request = LessonVocabularyItemCreateRequest.builder()
                .vocabId(100L)
                .examples(List.of(ex1, ex2))
                .build();

        when(sectionRepository.findById(1L)).thenReturn(Optional.of(section));
        when(vocabularyRepository.findByIdAndDeletedFalse(100L)).thenReturn(Optional.of(vocab));
        when(vocabItemRepository.findBySectionIdAndDeletedFalseOrderByOrderIndexAsc(1L))
                .thenReturn(List.of());
        when(vocabItemRepository.save(any(LessonVocabularyItem.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mapper.toVocabularyItemResponse(any(LessonVocabularyItem.class)))
                .thenReturn(LessonVocabularyItemResponse.builder().id(50L).build());

        service.addVocabularyItem(1L, request);

        org.mockito.ArgumentCaptor<LessonVocabularyItem> captor =
                org.mockito.ArgumentCaptor.forClass(LessonVocabularyItem.class);
        verify(vocabItemRepository).save(captor.capture());
        LessonVocabularyItem saved = captor.getValue();

        // Chỉ example đầu tiên primary=true
        assertThat(saved.getExamples().get(0).isPrimary()).isTrue();
        assertThat(saved.getExamples().get(1).isPrimary()).isFalse();
    }

    // ============================================================
    // V26+ getVocabularyItem
    // ============================================================

    @Test
    @DisplayName("getVocabularyItem: tìm thấy → trả về response")
    void getVocabularyItem_found_returnsResponse() {
        when(vocabItemRepository.findById(50L)).thenReturn(Optional.of(existingItem));
        when(mapper.toVocabularyItemResponse(existingItem))
                .thenReturn(LessonVocabularyItemResponse.builder().id(50L).build());

        LessonVocabularyItemResponse result = service.getVocabularyItem(50L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(50L);
    }

    @Test
    @DisplayName("getVocabularyItem: id không tồn tại → throw NOT_FOUND")
    void getVocabularyItem_notFound_throws() {
        when(vocabItemRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getVocabularyItem(99L))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.LESSON_VOCABULARY_ITEM_NOT_FOUND);
    }

    @Test
    @DisplayName("getVocabularyItem: id đã bị soft-delete → throw NOT_FOUND")
    void getVocabularyItem_softDeleted_throws() {
        existingItem.softDelete();
        when(vocabItemRepository.findById(50L)).thenReturn(Optional.of(existingItem));

        assertThatThrownBy(() -> service.getVocabularyItem(50L))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.LESSON_VOCABULARY_ITEM_NOT_FOUND);
    }

    // ============================================================
    // V26+ updateVocabularyItem — examples (3-state semantics)
    // ============================================================

    @Test
    @DisplayName("V26 updateVocabularyItem: examples = null → giữ nguyên examples hiện tại")
    void updateVocabularyItem_examplesNull_keepsExisting() {
        LessonVocabularyExample existingEx = LessonVocabularyExample.builder()
                .exampleJp("OLD").exampleVi("OLD-vi").orderIndex(1).primary(true).build();
        existingEx.setId(700L);
        existingItem.getExamples().add(existingEx);

        LessonVocabularyItemUpdateRequest request = LessonVocabularyItemUpdateRequest.builder()
                .note("note mới")
                // KHÔNG gửi examples
                .build();

        when(vocabItemRepository.findByIdWithExamples(50L)).thenReturn(Optional.of(existingItem));
        when(vocabItemRepository.save(any(LessonVocabularyItem.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mapper.toVocabularyItemResponse(any(LessonVocabularyItem.class)))
                .thenReturn(LessonVocabularyItemResponse.builder().id(50L).build());

        service.updateVocabularyItem(50L, request);

        assertThat(existingItem.getNote()).isEqualTo("note mới");
        // examples không thay đổi
        assertThat(existingItem.getExamples()).hasSize(1);
        assertThat(existingItem.getExamples().get(0).getId()).isEqualTo(700L);
        assertThat(existingItem.getExamples().get(0).isDeleted()).isFalse();
    }

    @Test
    @DisplayName("V26 updateVocabularyItem: examples = [] → RESET: xoá hết rồi auto-copy từ vocab")
    void updateVocabularyItem_emptyExamplesList_resetsFromVocab() {
        // Item có sẵn 2 examples (custom của giáo viên)
        LessonVocabularyExample ex1 = LessonVocabularyExample.builder()
                .exampleJp("JP1").exampleVi("VI1").orderIndex(1).build();
        ex1.setId(701L);
        LessonVocabularyExample ex2 = LessonVocabularyExample.builder()
                .exampleJp("JP2").exampleVi("VI2").orderIndex(2).build();
        ex2.setId(702L);
        existingItem.getExamples().add(ex1);
        existingItem.getExamples().add(ex2);

        // Vocab gốc có 1 example
        VocabularyExample vocabEx = VocabularyExample.builder()
                .exampleJp("VOCAB-EX").exampleVi("Vocab example").orderIndex(1).build();
        vocabEx.setId(9001L);
        Vocabulary vocabWithEx = vocabWithExamples(this.vocab, vocabEx);

        LessonVocabularyItemUpdateRequest request = LessonVocabularyItemUpdateRequest.builder()
                .examples(List.of()) // empty → RESET về vocab
                .build();

        when(vocabItemRepository.findByIdWithExamples(50L)).thenReturn(Optional.of(existingItem));
        when(vocabularyRepository.findByIdWithActiveExamples(100L)).thenReturn(Optional.of(vocabWithEx));
        when(vocabItemRepository.save(any(LessonVocabularyItem.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mapper.toVocabularyItemResponse(any(LessonVocabularyItem.class)))
                .thenReturn(LessonVocabularyItemResponse.builder().id(50L).build());

        service.updateVocabularyItem(50L, request);

        // ex1, ex2 bị soft-delete
        assertThat(ex1.isDeleted()).isTrue();
        assertThat(ex2.isDeleted()).isTrue();
        // Sau reset: item có 1 example mới auto-copy từ vocab
        long activeCount = existingItem.getExamples().stream()
                .filter(e -> !e.isDeleted()).count();
        assertThat(activeCount).isEqualTo(1);
        LessonVocabularyExample copied = existingItem.getExamples().stream()
                .filter(e -> !e.isDeleted()).findFirst().orElseThrow();
        assertThat(copied.getExampleJp()).isEqualTo("VOCAB-EX");
    }

    @Test
    @DisplayName("V26 updateVocabularyItem: examples = [upsert] → cập nhật example có id, tạo mới id=null")
    void updateVocabularyItem_upsertExamples() {
        // existing example
        LessonVocabularyExample ex1 = LessonVocabularyExample.builder()
                .exampleJp("OLD-JP1").exampleVi("OLD-VI1").orderIndex(1).primary(true).build();
        ex1.setId(701L);
        // existing example sẽ bị xoá (không có trong list)
        LessonVocabularyExample ex2 = LessonVocabularyExample.builder()
                .exampleJp("OLD-JP2").exampleVi("OLD-VI2").orderIndex(2).build();
        ex2.setId(702L);
        existingItem.getExamples().add(ex1);
        existingItem.getExamples().add(ex2);

        // Request: update ex1, tạo mới (id=null)
        LessonVocabularyExampleUpsertRequest updateEx1 = LessonVocabularyExampleUpsertRequest.builder()
                .id(701L)
                .exampleJp("NEW-JP1").exampleVi("NEW-VI1")
                .isPrimary(false) // set primary=false → ex1 bỏ primary
                .build();
        LessonVocabularyExampleUpsertRequest newEx = LessonVocabularyExampleUpsertRequest.builder()
                .exampleJp("NEW-JP3").exampleVi("NEW-VI3").orderIndex(3).isPrimary(true).build();

        LessonVocabularyItemUpdateRequest request = LessonVocabularyItemUpdateRequest.builder()
                .examples(List.of(updateEx1, newEx))
                .build();

        when(vocabItemRepository.findByIdWithExamples(50L)).thenReturn(Optional.of(existingItem));
        when(vocabItemRepository.save(any(LessonVocabularyItem.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mapper.toVocabularyItemResponse(any(LessonVocabularyItem.class)))
                .thenReturn(LessonVocabularyItemResponse.builder().id(50L).build());

        service.updateVocabularyItem(50L, request);

        // ex1: cập nhật nội dung, primary=false
        assertThat(ex1.getExampleJp()).isEqualTo("NEW-JP1");
        assertThat(ex1.isPrimary()).isFalse();
        // ex2: bị soft-delete (không có trong list)
        assertThat(ex2.isDeleted()).isTrue();
        // newEx: được tạo, primary=true
        assertThat(existingItem.getExamples()).hasSize(3);
        LessonVocabularyExample created = existingItem.getExamples().stream()
                .filter(e -> "NEW-JP3".equals(e.getExampleJp()))
                .findFirst().orElseThrow();
        assertThat(created.isPrimary()).isTrue();
    }

    @Test
    @DisplayName("V26 updateVocabularyItem: examples chứa id không tồn tại → throw NOT_FOUND")
    void updateVocabularyItem_upsertWithInvalidId_throws() {
        LessonVocabularyExampleUpsertRequest badReq = LessonVocabularyExampleUpsertRequest.builder()
                .id(999L) // id không tồn tại
                .exampleJp("JP").exampleVi("VI").build();
        LessonVocabularyItemUpdateRequest request = LessonVocabularyItemUpdateRequest.builder()
                .examples(List.of(badReq))
                .build();

        when(vocabItemRepository.findByIdWithExamples(50L)).thenReturn(Optional.of(existingItem));

        assertThatThrownBy(() -> service.updateVocabularyItem(50L, request))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.LESSON_VOCAB_EXAMPLE_NOT_FOUND);
    }

    // ============================================================
    // V26+ deleteVocabularyItem — cascade soft-delete examples
    // ============================================================

    @Test
    @DisplayName("V26 deleteVocabularyItem: cascade soft-delete tất cả examples thuộc item")
    void deleteVocabularyItem_cascadeSoftDeleteExamples() {
        LessonVocabularyExample ex1 = LessonVocabularyExample.builder()
                .exampleJp("JP1").exampleVi("VI1").orderIndex(1).build();
        ex1.setId(701L);
        LessonVocabularyExample ex2 = LessonVocabularyExample.builder()
                .exampleJp("JP2").exampleVi("VI2").orderIndex(2).build();
        ex2.setId(702L);
        existingItem.getExamples().add(ex1);
        existingItem.getExamples().add(ex2);

        when(vocabItemRepository.findById(50L)).thenReturn(Optional.of(existingItem));
        when(vocabItemRepository.save(any(LessonVocabularyItem.class))).thenAnswer(inv -> inv.getArgument(0));

        service.deleteVocabularyItem(50L);

        assertThat(existingItem.isDeleted()).isTrue();
        assertThat(ex1.isDeleted()).isTrue();
        assertThat(ex2.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("V26 deleteVocabularyItem: item không có example → chỉ xoá item, không lỗi")
    void deleteVocabularyItem_noExamples_worksFine() {
        when(vocabItemRepository.findById(50L)).thenReturn(Optional.of(existingItem));
        when(vocabItemRepository.save(any(LessonVocabularyItem.class))).thenAnswer(inv -> inv.getArgument(0));

        service.deleteVocabularyItem(50L);

        assertThat(existingItem.isDeleted()).isTrue();
        assertThat(existingItem.getExamples()).isEmpty();
    }
}
