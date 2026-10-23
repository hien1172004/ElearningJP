package com.jp.elearningjp.service.curriculum.section;

import com.jp.elearningjp.dto.request.curriculum.LessonGrammarExampleCreateRequest;
import com.jp.elearningjp.dto.request.curriculum.LessonGrammarExampleUpsertRequest;
import com.jp.elearningjp.dto.request.curriculum.LessonGrammarItemCreateRequest;
import com.jp.elearningjp.dto.request.curriculum.LessonGrammarItemUpdateRequest;
import com.jp.elearningjp.dto.response.curriculum.LessonGrammarItemResponse;
import com.jp.elearningjp.entity.content.GrammarPoint;
import com.jp.elearningjp.entity.curriculum.LessonGrammarExample;
import com.jp.elearningjp.entity.curriculum.LessonGrammarItem;
import com.jp.elearningjp.entity.curriculum.LessonSection;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jp.elearningjp.exception.AppException;
import com.jp.elearningjp.exception.ErrorCode;
import com.jp.elearningjp.mapper.LessonSectionMapper;
import com.jp.elearningjp.repository.content.GrammarPointRepository;
import com.jp.elearningjp.repository.curriculum.LessonGrammarExampleRepository;
import com.jp.elearningjp.repository.curriculum.LessonGrammarItemRepository;
import com.jp.elearningjp.repository.curriculum.LessonSectionRepository;
import com.jp.elearningjp.shared.enums.LessonSectionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
 * Unit test cho {@link GrammarSectionItemService} — V27+:
 * Examples được xử lý trong payload create/update (giống vocab item).
 */
@ExtendWith(MockitoExtension.class)
class GrammarSectionItemServiceTest {

    @Mock LessonSectionRepository sectionRepository;
    @Mock LessonGrammarItemRepository grammarItemRepository;
    @Mock LessonGrammarExampleRepository exampleRepository;
    @Mock GrammarPointRepository grammarPointRepository;
    @Mock LessonSectionMapper mapper;

    @InjectMocks GrammarSectionItemService service;

    private static final ObjectMapper M = new ObjectMapper();

    private LessonSection section;
    private GrammarPoint grammar;
    private LessonGrammarItem existingItem;

    @BeforeEach
    void setUp() {
        section = LessonSection.builder()
                .sectionType(LessonSectionType.GRAMMAR)
                .orderIndex(1)
                .build();
        section.setId(1L);

        grammar = GrammarPoint.builder()
                .title("～てもいい")
                .structure("Vてもいい")
                .meaningVi("Được phép làm gì")
                .build();
        grammar.setId(7L);

        existingItem = LessonGrammarItem.builder()
                .section(section)
                .grammar(grammar)
                .orderIndex(1)
                .examples(new ArrayList<>())
                .build();
        existingItem.setId(50L);
    }

    // ============================================================
    // addGrammarItem — auto-copy từ grammarPoint.examplesJson (mặc định)
    // ============================================================

    @Test
    @DisplayName("addGrammarItem: examples = null → auto-copy từ examplesJson (mặc định)")
    void addGrammarItem_nullExamples_autoCopyFromJson() throws Exception {
        JsonNode examplesJson = M.readTree("""
                [
                  {"jp": "ここで写真をとってもいいですか。", "vi": "Cho phép chụp ảnh ở đây không?"},
                  {"jp": "使ってもいいですよ。", "vi": "Bạn có thể dùng."},
                  {"jp": "食べてもいいですよ。", "vi": "Bạn có thể ăn."}
                ]
                """);
        grammar.setExamplesJson(examplesJson);

        LessonGrammarItemCreateRequest request = LessonGrammarItemCreateRequest.builder()
                .grammarId(7L)
                .note("ngữ pháp quan trọng")
                // KHÔNG gửi examples
                .build();

        when(sectionRepository.findById(1L)).thenReturn(Optional.of(section));
        when(grammarPointRepository.findByIdAndDeletedFalse(7L)).thenReturn(Optional.of(grammar));
        when(grammarItemRepository.findBySectionIdAndDeletedFalseOrderByOrderIndexAsc(1L))
                .thenReturn(List.of());
        when(grammarItemRepository.save(any(LessonGrammarItem.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mapper.toGrammarItemResponse(any(LessonGrammarItem.class)))
                .thenReturn(LessonGrammarItemResponse.builder().id(50L).build());

        LessonGrammarItemResponse result = service.addGrammarItem(1L, request);
        assertThat(result).isNotNull();

        ArgumentCaptor<LessonGrammarItem> captor = ArgumentCaptor.forClass(LessonGrammarItem.class);
        verify(grammarItemRepository).save(captor.capture());
        LessonGrammarItem saved = captor.getValue();

        assertThat(saved.getExamples()).hasSize(3);
        assertThat(saved.getExamples().get(0).getExampleJp()).isEqualTo("ここで写真をとってもいいですか。");
        assertThat(saved.getExamples().get(0).getOrderIndex()).isEqualTo(1);
        assertThat(saved.getExamples().get(1).getOrderIndex()).isEqualTo(2);
        assertThat(saved.getExamples().get(2).getOrderIndex()).isEqualTo(3);
    }

    @Test
    @DisplayName("addGrammarItem: examples = [] → auto-copy từ examplesJson (giống null)")
    void addGrammarItem_emptyExamples_autoCopyFromJson() throws Exception {
        JsonNode examplesJson = M.readTree("[{\"jp\": \"JP1\", \"vi\": \"VI1\"}]");
        grammar.setExamplesJson(examplesJson);

        LessonGrammarItemCreateRequest request = LessonGrammarItemCreateRequest.builder()
                .grammarId(7L)
                .examples(List.of()) // empty → vẫn auto-copy
                .build();

        when(sectionRepository.findById(1L)).thenReturn(Optional.of(section));
        when(grammarPointRepository.findByIdAndDeletedFalse(7L)).thenReturn(Optional.of(grammar));
        when(grammarItemRepository.findBySectionIdAndDeletedFalseOrderByOrderIndexAsc(1L))
                .thenReturn(List.of());
        when(grammarItemRepository.save(any(LessonGrammarItem.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mapper.toGrammarItemResponse(any(LessonGrammarItem.class)))
                .thenReturn(LessonGrammarItemResponse.builder().id(50L).build());

        service.addGrammarItem(1L, request);

        ArgumentCaptor<LessonGrammarItem> captor = ArgumentCaptor.forClass(LessonGrammarItem.class);
        verify(grammarItemRepository).save(captor.capture());
        LessonGrammarItem saved = captor.getValue();

        assertThat(saved.getExamples()).hasSize(1);
        assertThat(saved.getExamples().get(0).getExampleJp()).isEqualTo("JP1");
    }

    @Test
    @DisplayName("addGrammarItem: examplesJson null → không copy gì")
    void addGrammarItem_nullJson_noCopy() {
        grammar.setExamplesJson(null);

        LessonGrammarItemCreateRequest request = LessonGrammarItemCreateRequest.builder()
                .grammarId(7L)
                .build();

        when(sectionRepository.findById(1L)).thenReturn(Optional.of(section));
        when(grammarPointRepository.findByIdAndDeletedFalse(7L)).thenReturn(Optional.of(grammar));
        when(grammarItemRepository.findBySectionIdAndDeletedFalseOrderByOrderIndexAsc(1L))
                .thenReturn(List.of());
        when(grammarItemRepository.save(any(LessonGrammarItem.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mapper.toGrammarItemResponse(any(LessonGrammarItem.class)))
                .thenReturn(LessonGrammarItemResponse.builder().id(50L).build());

        service.addGrammarItem(1L, request);

        ArgumentCaptor<LessonGrammarItem> captor = ArgumentCaptor.forClass(LessonGrammarItem.class);
        verify(grammarItemRepository).save(captor.capture());
        LessonGrammarItem saved = captor.getValue();
        assertThat(saved.getExamples()).isEmpty();
    }

    @Test
    @DisplayName("addGrammarItem: examplesJson thiếu jp/vi → bỏ qua entry đó")
    void addGrammarItem_skipMalformedExample() throws Exception {
        JsonNode examplesJson = M.readTree("""
                [
                  {"jp": "OK1", "vi": "VI1"},
                  {"jp": ""},
                  {"vi": "missing-jp"},
                  {"jp": "OK2", "vi": "VI2"}
                ]
                """);
        grammar.setExamplesJson(examplesJson);

        LessonGrammarItemCreateRequest request = LessonGrammarItemCreateRequest.builder()
                .grammarId(7L)
                .build();

        when(sectionRepository.findById(1L)).thenReturn(Optional.of(section));
        when(grammarPointRepository.findByIdAndDeletedFalse(7L)).thenReturn(Optional.of(grammar));
        when(grammarItemRepository.findBySectionIdAndDeletedFalseOrderByOrderIndexAsc(1L))
                .thenReturn(List.of());
        when(grammarItemRepository.save(any(LessonGrammarItem.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mapper.toGrammarItemResponse(any(LessonGrammarItem.class)))
                .thenReturn(LessonGrammarItemResponse.builder().id(50L).build());

        service.addGrammarItem(1L, request);

        ArgumentCaptor<LessonGrammarItem> captor = ArgumentCaptor.forClass(LessonGrammarItem.class);
        verify(grammarItemRepository).save(captor.capture());
        LessonGrammarItem saved = captor.getValue();

        assertThat(saved.getExamples()).hasSize(2);
        assertThat(saved.getExamples().get(0).getExampleJp()).isEqualTo("OK1");
        assertThat(saved.getExamples().get(1).getExampleJp()).isEqualTo("OK2");
    }

    // ============================================================
    // addGrammarItem — auto-fill custom fields
    // ============================================================

    @Test
    @DisplayName("addGrammarItem: không gửi customMeaning/Structure → auto-fill từ grammarPoint")
    void addGrammarItem_noOverrides_autoFillFromGrammar() {
        LessonGrammarItemCreateRequest request = LessonGrammarItemCreateRequest.builder()
                .grammarId(7L)
                .build();

        when(sectionRepository.findById(1L)).thenReturn(Optional.of(section));
        when(grammarPointRepository.findByIdAndDeletedFalse(7L)).thenReturn(Optional.of(grammar));
        when(grammarItemRepository.findBySectionIdAndDeletedFalseOrderByOrderIndexAsc(1L))
                .thenReturn(List.of());
        when(grammarItemRepository.save(any(LessonGrammarItem.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mapper.toGrammarItemResponse(any(LessonGrammarItem.class)))
                .thenReturn(LessonGrammarItemResponse.builder().id(50L).build());

        service.addGrammarItem(1L, request);

        ArgumentCaptor<LessonGrammarItem> captor = ArgumentCaptor.forClass(LessonGrammarItem.class);
        verify(grammarItemRepository).save(captor.capture());
        LessonGrammarItem saved = captor.getValue();
        assertThat(saved.getCustomMeaning()).isEqualTo("Được phép làm gì");
        assertThat(saved.getCustomStructure()).isEqualTo("Vてもいい");
    }

    @Test
    @DisplayName("addGrammarItem: customMeaning = '' → set null (xoá override)")
    void addGrammarItem_emptyCustomMeaning_setToNull() {
        LessonGrammarItemCreateRequest request = LessonGrammarItemCreateRequest.builder()
                .grammarId(7L)
                .customMeaning("")
                .build();

        when(sectionRepository.findById(1L)).thenReturn(Optional.of(section));
        when(grammarPointRepository.findByIdAndDeletedFalse(7L)).thenReturn(Optional.of(grammar));
        when(grammarItemRepository.findBySectionIdAndDeletedFalseOrderByOrderIndexAsc(1L))
                .thenReturn(List.of());
        when(grammarItemRepository.save(any(LessonGrammarItem.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mapper.toGrammarItemResponse(any(LessonGrammarItem.class)))
                .thenReturn(LessonGrammarItemResponse.builder().id(50L).build());

        service.addGrammarItem(1L, request);

        ArgumentCaptor<LessonGrammarItem> captor = ArgumentCaptor.forClass(LessonGrammarItem.class);
        verify(grammarItemRepository).save(captor.capture());
        LessonGrammarItem saved = captor.getValue();
        assertThat(saved.getCustomMeaning()).isNull();
        assertThat(saved.getCustomStructure()).isEqualTo("Vてもいい");
    }

    // ============================================================
    // addGrammarItem — teacher cung cấp examples trong payload
    // ============================================================

    @Test
    @DisplayName("addGrammarItem: gửi examples list → dùng danh sách đó, KHÔNG auto-copy")
    void addGrammarItem_teacherExamplesList_usesList() {
        LessonGrammarExampleCreateRequest ex1 = LessonGrammarExampleCreateRequest.builder()
                .exampleJp("学生は図書館で勉強してもいいですか。")
                .exampleVi("Học sinh có được phép học ở thư viện không?")
                .orderIndex(1).build();
        LessonGrammarExampleCreateRequest ex2 = LessonGrammarExampleCreateRequest.builder()
                .exampleJp("ここで泳いでもいいですよ。")
                .exampleVi("Bạn có thể bơi ở đây.")
                .orderIndex(2).build();

        LessonGrammarItemCreateRequest request = LessonGrammarItemCreateRequest.builder()
                .grammarId(7L)
                .examples(List.of(ex1, ex2))
                .build();

        when(sectionRepository.findById(1L)).thenReturn(Optional.of(section));
        when(grammarPointRepository.findByIdAndDeletedFalse(7L)).thenReturn(Optional.of(grammar));
        when(grammarItemRepository.findBySectionIdAndDeletedFalseOrderByOrderIndexAsc(1L))
                .thenReturn(List.of());
        when(grammarItemRepository.save(any(LessonGrammarItem.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mapper.toGrammarItemResponse(any(LessonGrammarItem.class)))
                .thenReturn(LessonGrammarItemResponse.builder().id(50L).build());

        service.addGrammarItem(1L, request);

        ArgumentCaptor<LessonGrammarItem> captor = ArgumentCaptor.forClass(LessonGrammarItem.class);
        verify(grammarItemRepository).save(captor.capture());
        LessonGrammarItem saved = captor.getValue();

        assertThat(saved.getExamples()).hasSize(2);
        assertThat(saved.getExamples().get(0).getExampleJp()).isEqualTo("学生は図書館で勉強してもいいですか。");
        assertThat(saved.getExamples().get(1).getExampleJp()).isEqualTo("ここで泳いでもいいですよ。");
    }

    @Test
    @DisplayName("addGrammarItem: teacher examples chứa trùng jp+vi → throw DUPLICATED")
    void addGrammarItem_teacherExamplesDuplicate_throws() {
        LessonGrammarExampleCreateRequest ex1 = LessonGrammarExampleCreateRequest.builder()
                .exampleJp("JP1").exampleVi("VI1").build();
        LessonGrammarExampleCreateRequest ex2 = LessonGrammarExampleCreateRequest.builder()
                .exampleJp("JP1").exampleVi("VI1").build(); // trùng với ex1

        LessonGrammarItemCreateRequest request = LessonGrammarItemCreateRequest.builder()
                .grammarId(7L)
                .examples(List.of(ex1, ex2))
                .build();

        when(sectionRepository.findById(1L)).thenReturn(Optional.of(section));
        when(grammarPointRepository.findByIdAndDeletedFalse(7L)).thenReturn(Optional.of(grammar));
        when(grammarItemRepository.findBySectionIdAndDeletedFalseOrderByOrderIndexAsc(1L))
                .thenReturn(List.of());

        assertThatThrownBy(() -> service.addGrammarItem(1L, request))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.LESSON_GRAMMAR_EXAMPLE_DUPLICATED);
    }

    // ============================================================
    // getGrammarItem
    // ============================================================

    @Test
    @DisplayName("getGrammarItem: tìm thấy → trả về response")
    void getGrammarItem_found_returnsResponse() {
        when(grammarItemRepository.findById(50L)).thenReturn(Optional.of(existingItem));
        when(mapper.toGrammarItemResponse(existingItem))
                .thenReturn(LessonGrammarItemResponse.builder().id(50L).build());

        LessonGrammarItemResponse result = service.getGrammarItem(50L);
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(50L);
    }

    @Test
    @DisplayName("getGrammarItem: id không tồn tại → throw NOT_FOUND")
    void getGrammarItem_notFound_throws() {
        when(grammarItemRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getGrammarItem(99L))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.LESSON_GRAMMAR_ITEM_NOT_FOUND);
    }

    @Test
    @DisplayName("getGrammarItem: id đã bị soft-delete → throw NOT_FOUND")
    void getGrammarItem_softDeleted_throws() {
        existingItem.softDelete();
        when(grammarItemRepository.findById(50L)).thenReturn(Optional.of(existingItem));

        assertThatThrownBy(() -> service.getGrammarItem(50L))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.LESSON_GRAMMAR_ITEM_NOT_FOUND);
    }

    // ============================================================
    // updateGrammarItem — examples 3-state
    // ============================================================

    @Test
    @DisplayName("updateGrammarItem: examples = null → giữ nguyên examples hiện tại")
    void updateGrammarItem_examplesNull_keepsExisting() {
        LessonGrammarExample existingEx = LessonGrammarExample.builder()
                .grammarItem(existingItem).exampleJp("OLD").exampleVi("OLD-vi").orderIndex(1).build();
        existingEx.setId(700L);
        existingItem.getExamples().add(existingEx);

        LessonGrammarItemUpdateRequest request = LessonGrammarItemUpdateRequest.builder()
                .note("note mới")
                // KHÔNG gửi examples
                .build();

        when(grammarItemRepository.findByIdWithExamples(50L)).thenReturn(Optional.of(existingItem));
        when(grammarItemRepository.save(any(LessonGrammarItem.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mapper.toGrammarItemResponse(any(LessonGrammarItem.class)))
                .thenReturn(LessonGrammarItemResponse.builder().id(50L).build());

        service.updateGrammarItem(50L, request);

        assertThat(existingItem.getNote()).isEqualTo("note mới");
        assertThat(existingItem.getExamples()).hasSize(1);
        assertThat(existingItem.getExamples().get(0).getId()).isEqualTo(700L);
        assertThat(existingItem.getExamples().get(0).isDeleted()).isFalse();
    }

    @Test
    @DisplayName("updateGrammarItem: examples = [] → RESET: xoá hết rồi auto-copy từ grammar.examplesJson")
    void updateGrammarItem_emptyExamples_resetsFromGrammar() throws Exception {
        // Item có sẵn 2 custom examples
        LessonGrammarExample ex1 = LessonGrammarExample.builder()
                .grammarItem(existingItem).exampleJp("CUSTOM1").exampleVi("VI1").orderIndex(1).build();
        ex1.setId(701L);
        LessonGrammarExample ex2 = LessonGrammarExample.builder()
                .grammarItem(existingItem).exampleJp("CUSTOM2").exampleVi("VI2").orderIndex(2).build();
        ex2.setId(702L);
        existingItem.getExamples().add(ex1);
        existingItem.getExamples().add(ex2);

        // Grammar gốc có 1 example
        JsonNode examplesJson = M.readTree("[{\"jp\": \"FROM-GRAMMAR\", \"vi\": \"From grammar\"}]");
        grammar.setExamplesJson(examplesJson);

        LessonGrammarItemUpdateRequest request = LessonGrammarItemUpdateRequest.builder()
                .examples(List.of()) // empty → RESET về vocab
                .build();

        when(grammarItemRepository.findByIdWithExamples(50L)).thenReturn(Optional.of(existingItem));
        // grammar đã load sẵn qua item.getGrammar()
        when(grammarItemRepository.save(any(LessonGrammarItem.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mapper.toGrammarItemResponse(any(LessonGrammarItem.class)))
                .thenReturn(LessonGrammarItemResponse.builder().id(50L).build());

        service.updateGrammarItem(50L, request);

        // ex1, ex2 bị soft-delete
        assertThat(ex1.isDeleted()).isTrue();
        assertThat(ex2.isDeleted()).isTrue();
        // Sau reset: item có 1 example auto-copy từ grammar
        long activeCount = existingItem.getExamples().stream()
                .filter(e -> !e.isDeleted()).count();
        assertThat(activeCount).isEqualTo(1);
        LessonGrammarExample copied = existingItem.getExamples().stream()
                .filter(e -> !e.isDeleted()).findFirst().orElseThrow();
        assertThat(copied.getExampleJp()).isEqualTo("FROM-GRAMMAR");
    }

    @Test
    @DisplayName("updateGrammarItem: examples upsert → update ex có id, tạo mới id=null, xoá ex không trong list")
    void updateGrammarItem_upsertExamples() {
        // existing: ex1, ex2 (ex2 sẽ bị xoá)
        LessonGrammarExample ex1 = LessonGrammarExample.builder()
                .grammarItem(existingItem).exampleJp("OLD-JP1").exampleVi("OLD-VI1").orderIndex(1).build();
        ex1.setId(701L);
        LessonGrammarExample ex2 = LessonGrammarExample.builder()
                .grammarItem(existingItem).exampleJp("OLD-JP2").exampleVi("OLD-VI2").orderIndex(2).build();
        ex2.setId(702L);
        existingItem.getExamples().add(ex1);
        existingItem.getExamples().add(ex2);

        // Request: update ex1, tạo mới (id=null)
        LessonGrammarExampleUpsertRequest updateEx1 = LessonGrammarExampleUpsertRequest.builder()
                .id(701L)
                .exampleJp("NEW-JP1").exampleVi("NEW-VI1")
                .build();
        LessonGrammarExampleUpsertRequest newEx = LessonGrammarExampleUpsertRequest.builder()
                .exampleJp("NEW-JP3").exampleVi("NEW-VI3").orderIndex(3).build();

        LessonGrammarItemUpdateRequest request = LessonGrammarItemUpdateRequest.builder()
                .examples(List.of(updateEx1, newEx))
                .build();

        when(grammarItemRepository.findByIdWithExamples(50L)).thenReturn(Optional.of(existingItem));
        when(grammarItemRepository.save(any(LessonGrammarItem.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mapper.toGrammarItemResponse(any(LessonGrammarItem.class)))
                .thenReturn(LessonGrammarItemResponse.builder().id(50L).build());

        service.updateGrammarItem(50L, request);

        // ex1: cập nhật nội dung
        assertThat(ex1.getExampleJp()).isEqualTo("NEW-JP1");
        // ex2: bị soft-delete (không có trong list)
        assertThat(ex2.isDeleted()).isTrue();
        // newEx: được tạo
        assertThat(existingItem.getExamples()).hasSize(3);
        LessonGrammarExample created = existingItem.getExamples().stream()
                .filter(e -> "NEW-JP3".equals(e.getExampleJp()))
                .findFirst().orElseThrow();
        assertThat(created.getOrderIndex()).isEqualTo(3);
    }

    @Test
    @DisplayName("updateGrammarItem: upsert chứa id không tồn tại → throw NOT_FOUND")
    void updateGrammarItem_upsertInvalidId_throws() {
        LessonGrammarExampleUpsertRequest badReq = LessonGrammarExampleUpsertRequest.builder()
                .id(999L) // id không tồn tại
                .exampleJp("JP").exampleVi("VI").build();
        LessonGrammarItemUpdateRequest request = LessonGrammarItemUpdateRequest.builder()
                .examples(List.of(badReq))
                .build();

        when(grammarItemRepository.findByIdWithExamples(50L)).thenReturn(Optional.of(existingItem));

        assertThatThrownBy(() -> service.updateGrammarItem(50L, request))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.LESSON_GRAMMAR_EXAMPLE_NOT_FOUND);
    }

    @Test
    @DisplayName("updateGrammarItem: upsert chứa trùng jp+vi → throw DUPLICATED")
    void updateGrammarItem_upsertDuplicate_throws() {
        LessonGrammarExample ex1 = LessonGrammarExample.builder()
                .grammarItem(existingItem).exampleJp("JP1").exampleVi("VI1").orderIndex(1).build();
        ex1.setId(701L);
        existingItem.getExamples().add(ex1);

        LessonGrammarExampleUpsertRequest dupEx = LessonGrammarExampleUpsertRequest.builder()
                .id(701L)
                .exampleJp("JP1").exampleVi("VI1") // trùng với chính nó → không throw
                .build();
        LessonGrammarExampleUpsertRequest newDup = LessonGrammarExampleUpsertRequest.builder()
                .exampleJp("JP1").exampleVi("VI1") // trùng với ex1
                .build();

        LessonGrammarItemUpdateRequest request = LessonGrammarItemUpdateRequest.builder()
                .examples(List.of(dupEx, newDup))
                .build();

        when(grammarItemRepository.findByIdWithExamples(50L)).thenReturn(Optional.of(existingItem));

        assertThatThrownBy(() -> service.updateGrammarItem(50L, request))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.LESSON_GRAMMAR_EXAMPLE_DUPLICATED);
    }

    @Test
    @DisplayName("updateGrammarItem: customMeaning = '' → set null (xoá override, không ảnh hưởng examples)")
    void updateGrammarItem_emptyCustomMeaning_setToNull() {
        existingItem.setCustomMeaning("cũ");
        LessonGrammarExample ex = LessonGrammarExample.builder()
                .grammarItem(existingItem).exampleJp("JP1").exampleVi("VI1").orderIndex(1).build();
        existingItem.getExamples().add(ex);

        LessonGrammarItemUpdateRequest request = LessonGrammarItemUpdateRequest.builder()
                .customMeaning("")
                .build();

        when(grammarItemRepository.findByIdWithExamples(50L)).thenReturn(Optional.of(existingItem));
        when(grammarItemRepository.save(any(LessonGrammarItem.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mapper.toGrammarItemResponse(any(LessonGrammarItem.class)))
                .thenReturn(LessonGrammarItemResponse.builder().id(50L).build());

        service.updateGrammarItem(50L, request);

        assertThat(existingItem.getCustomMeaning()).isNull();
        assertThat(existingItem.getExamples()).hasSize(1);
        assertThat(existingItem.getExamples().get(0).isDeleted()).isFalse();
    }

    // ============================================================
    // deleteGrammarItem — cascade soft-delete examples
    // ============================================================

    @Test
    @DisplayName("deleteGrammarItem: cascade soft-delete tất cả examples thuộc item")
    void deleteGrammarItem_cascadeSoftDeleteExamples() {
        LessonGrammarExample ex1 = LessonGrammarExample.builder()
                .grammarItem(existingItem).exampleJp("JP1").exampleVi("VI1").orderIndex(1).build();
        ex1.setId(701L);
        LessonGrammarExample ex2 = LessonGrammarExample.builder()
                .grammarItem(existingItem).exampleJp("JP2").exampleVi("VI2").orderIndex(2).build();
        ex2.setId(702L);
        existingItem.getExamples().add(ex1);
        existingItem.getExamples().add(ex2);

        when(grammarItemRepository.findById(50L)).thenReturn(Optional.of(existingItem));
        when(grammarItemRepository.save(any(LessonGrammarItem.class))).thenAnswer(inv -> inv.getArgument(0));

        service.deleteGrammarItem(50L);

        assertThat(existingItem.isDeleted()).isTrue();
        assertThat(ex1.isDeleted()).isTrue();
        assertThat(ex2.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("deleteGrammarItem: item không có example → chỉ xoá item, không lỗi")
    void deleteGrammarItem_noExamples_worksFine() {
        when(grammarItemRepository.findById(50L)).thenReturn(Optional.of(existingItem));
        when(grammarItemRepository.save(any(LessonGrammarItem.class))).thenAnswer(inv -> inv.getArgument(0));

        service.deleteGrammarItem(50L);

        assertThat(existingItem.isDeleted()).isTrue();
        assertThat(existingItem.getExamples()).isEmpty();
    }
}
