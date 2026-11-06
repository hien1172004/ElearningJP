package com.jp.elearningjp.service.curriculum.importer;

import com.jp.elearningjp.dto.request.curriculum.LessonKanjiItemCreateRequest;
import com.jp.elearningjp.dto.request.curriculum.LessonVocabularyItemCreateRequest;
import com.jp.elearningjp.dto.response.curriculum.importer.ImportRowStatus;
import com.jp.elearningjp.dto.response.curriculum.importer.SectionImportPreviewResponse;
import com.jp.elearningjp.entity.content.Vocabulary;
import com.jp.elearningjp.entity.content.WritingCharacter;
import com.jp.elearningjp.entity.curriculum.LessonSection;
import com.jp.elearningjp.exception.AppException;
import com.jp.elearningjp.exception.ErrorCode;
import com.jp.elearningjp.repository.content.VocabularyRepository;
import com.jp.elearningjp.repository.content.WritingCharacterRepository;
import com.jp.elearningjp.repository.curriculum.LessonKanjiItemRepository;
import com.jp.elearningjp.repository.curriculum.LessonVocabularyItemRepository;
import com.jp.elearningjp.service.curriculum.importer.kanji.KanjiImportService;
import com.jp.elearningjp.service.curriculum.importer.vocabulary.VocabularyImportService;
import com.jp.elearningjp.service.curriculum.section.KanjiSectionItemService;
import com.jp.elearningjp.service.curriculum.section.VocabularySectionItemService;
import com.jp.elearningjp.shared.enums.LessonSectionType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Kiểm tra luồng mẫu → điền → preview/commit của import Excel (repository được mock). */
class SectionImportServiceTest {

    private final ExcelImportSupport excel = new ExcelImportSupport();
    private ImportSectionResolver resolver;
    private VocabularyRepository vocabularyRepository;
    private LessonSection section;

    @BeforeEach
    void setUp() {
        resolver = mock(ImportSectionResolver.class);
        vocabularyRepository = mock(VocabularyRepository.class);
        section = new LessonSection();
        section.setId(10L);
        when(resolver.requireEditable(eq(10L), any())).thenReturn(section);
    }

    private static Vocabulary vocab(long id, String word, String hiragana) {
        Vocabulary v = Vocabulary.builder().word(word).hiragana(hiragana).meaningVi("nghĩa " + word).build();
        v.setId(id);
        return v;
    }

    /** Mở file mẫu do service sinh ra, điền dữ liệu vào các sheet rồi đóng gói thành file upload. */
    private static MockMultipartFile fill(byte[] template, String[][]... sheets) throws Exception {
        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(template))) {
            for (String[][] sheetData : sheets) {
                Sheet sheet = wb.getSheet(sheetData[0][0]);
                for (int r = 1; r < sheetData.length; r++) {
                    Row row = sheet.createRow(r);
                    for (int c = 0; c < sheetData[r].length; c++) {
                        if (sheetData[r][c] != null) row.createCell(c).setCellValue(sheetData[r][c]);
                    }
                }
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return new MockMultipartFile("file", "import.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", out.toByteArray());
        }
    }

    // ============================================================
    // VOCABULARY
    // ============================================================

    private VocabularyImportService vocabService(LessonVocabularyItemRepository itemRepo,
                                                 VocabularySectionItemService itemService) {
        return new VocabularyImportService(excel, resolver, vocabularyRepository, itemRepo, itemService);
    }

    @Test
    void vocabulary_validFile_previewOk_andCommitPassesOverridesAndExamples() throws Exception {
        LessonVocabularyItemRepository itemRepo = mock(LessonVocabularyItemRepository.class);
        VocabularySectionItemService itemService = mock(VocabularySectionItemService.class);
        when(itemRepo.findBySectionIdAndDeletedFalseOrderByOrderIndexAsc(10L)).thenReturn(List.of());
        when(vocabularyRepository.findFirstByWordAndHiraganaAndDeletedFalseOrderBySenseNoAsc("食べる", "たべる"))
                .thenReturn(Optional.of(vocab(1L, "食べる", "たべる")));
        when(vocabularyRepository.findFirstByWordAndDeletedFalseOrderBySenseNoAsc("水"))
                .thenReturn(Optional.of(vocab(2L, "水", "みず")));
        when(itemService.importVocabularyItems(any(), anyList())).thenAnswer(inv -> ((List<?>) inv.getArgument(1)).size());

        VocabularyImportService service = vocabService(itemRepo, itemService);
        MockMultipartFile file = fill(service.buildTemplate().content(),
                new String[][]{{"TuVung"},
                        {"食べる", "たべる", "ăn (riêng)", "-", null, null, "Động từ nhóm 2"},
                        {"水", null, null, null, null, null, null}},
                new String[][]{{"ViDu"},
                        {"食べる", "たべる", "ご飯を食べる。", "Ăn cơm.", null, null, "x"}});

        SectionImportPreviewResponse preview = service.preview(10L, file);
        assertThat(preview.isCanCommit()).isTrue();
        assertThat(preview.getTotalItems()).isEqualTo(2);
        assertThat(preview.getErrorRows()).isZero();

        assertThat(service.commit(10L, file).getImportedChildren()).isEqualTo(1);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<LessonVocabularyItemCreateRequest>> captor = ArgumentCaptor.forClass(List.class);
        verify(itemService).importVocabularyItems(same(section), captor.capture());
        List<LessonVocabularyItemCreateRequest> requests = captor.getValue();
        assertThat(requests).hasSize(2);
        assertThat(requests.get(0).getVocabId()).isEqualTo(1L);
        assertThat(requests.get(0).getCustomMeaning()).isEqualTo("ăn (riêng)");
        assertThat(requests.get(0).getCustomRomaji()).isEmpty();          // '-' = xoá override
        assertThat(requests.get(0).getExamples()).hasSize(1);
        assertThat(requests.get(0).getExamples().get(0).getIsPrimary()).isTrue();
        assertThat(requests.get(1).getCustomMeaning()).isNull();          // trống = lấy từ điển
        assertThat(requests.get(1).getExamples()).isNull();               // → tự chép ví dụ từ điển
    }

    @Test
    void vocabulary_unknownDuplicateAndExisting_markErrors_andCommitRejected() throws Exception {
        LessonVocabularyItemRepository itemRepo = mock(LessonVocabularyItemRepository.class);
        VocabularySectionItemService itemService = mock(VocabularySectionItemService.class);
        var existing = new com.jp.elearningjp.entity.curriculum.LessonVocabularyItem();
        existing.setVocab(vocab(2L, "水", "みず"));
        when(itemRepo.findBySectionIdAndDeletedFalseOrderByOrderIndexAsc(10L)).thenReturn(List.of(existing));
        when(vocabularyRepository.findFirstByWordAndDeletedFalseOrderBySenseNoAsc("水"))
                .thenReturn(Optional.of(vocab(2L, "水", "みず")));
        when(vocabularyRepository.findFirstByWordAndDeletedFalseOrderBySenseNoAsc("猫"))
                .thenReturn(Optional.of(vocab(3L, "猫", "ねこ")));
        when(vocabularyRepository.findFirstByWordAndDeletedFalseOrderBySenseNoAsc("ないよ"))
                .thenReturn(Optional.empty());

        VocabularyImportService service = vocabService(itemRepo, itemService);
        MockMultipartFile file = fill(service.buildTemplate().content(),
                new String[][]{{"TuVung"},
                        {"水"},          // đã có trong section
                        {"ないよ"},       // không có trong từ điển
                        {"猫"},
                        {"猫"},          // trùng dòng 4
                        {"犬", null, null, null, "ftp://x"}},
                new String[][]{{"ViDu"},
                        {"鳥", null, "鳥がいる。", "Có chim."}}); // không có trong sheet TuVung

        SectionImportPreviewResponse preview = service.preview(10L, file);
        assertThat(preview.isCanCommit()).isFalse();
        assertThat(preview.getItemRows()).extracting(r -> r.getStatus()).containsExactly(
                ImportRowStatus.ERROR, ImportRowStatus.ERROR, ImportRowStatus.OK,
                ImportRowStatus.ERROR, ImportRowStatus.ERROR);
        assertThat(preview.getChildRows().get(0).getStatus()).isEqualTo(ImportRowStatus.ERROR);

        assertThatThrownBy(() -> service.commit(10L, file))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.IMPORT_HAS_ERRORS);
        verify(itemService, never()).importVocabularyItems(any(), anyList());
    }

    @Test
    void vocabulary_rejectsTemplateOfOtherSectionType() throws Exception {
        VocabularyImportService vocab = vocabService(mock(LessonVocabularyItemRepository.class),
                mock(VocabularySectionItemService.class));
        KanjiImportService kanji = new KanjiImportService(excel, resolver, mock(WritingCharacterRepository.class),
                vocabularyRepository, mock(LessonKanjiItemRepository.class), mock(KanjiSectionItemService.class));
        MockMultipartFile kanjiFile = fill(kanji.buildTemplate().content(), new String[][]{{"Kanji"}, {"日"}});

        assertThatThrownBy(() -> vocab.preview(10L, kanjiFile))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.IMPORT_TEMPLATE_TYPE_MISMATCH);
    }

    @Test
    void rejectsNonXlsxFile() {
        VocabularyImportService vocab = vocabService(mock(LessonVocabularyItemRepository.class),
                mock(VocabularySectionItemService.class));
        MockMultipartFile csv = new MockMultipartFile("file", "a.csv", "text/csv", "word\n水".getBytes());
        assertThatThrownBy(() -> vocab.preview(10L, csv))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.IMPORT_FILE_INVALID);
    }

    // ============================================================
    // KANJI
    // ============================================================

    @Test
    void kanji_relatedVocabLinkedToKanji_andUnknownKanjiIsError() throws Exception {
        WritingCharacterRepository charRepo = mock(WritingCharacterRepository.class);
        LessonKanjiItemRepository itemRepo = mock(LessonKanjiItemRepository.class);
        KanjiSectionItemService itemService = mock(KanjiSectionItemService.class);
        WritingCharacter sun = WritingCharacter.builder().character("日").meaningVi("mặt trời").build();
        sun.setId(5L);
        when(charRepo.findByCharacterInAndDeletedFalse(anyCollection())).thenReturn(List.of(sun));
        when(itemRepo.findBySectionIdAndDeletedFalseOrderByOrderIndexAsc(10L)).thenReturn(List.of());
        when(vocabularyRepository.findFirstByWordAndHiraganaAndDeletedFalseOrderBySenseNoAsc("日本", "にほん"))
                .thenReturn(Optional.of(vocab(7L, "日本", "にほん")));
        when(itemService.importKanjiItems(any(), anyList())).thenAnswer(inv -> ((List<?>) inv.getArgument(1)).size());

        KanjiImportService service = new KanjiImportService(excel, resolver, charRepo, vocabularyRepository,
                itemRepo, itemService);
        byte[] template = service.buildTemplate().content();

        MockMultipartFile bad = fill(template, new String[][]{{"Kanji"}, {"日"}, {"月"}, {"日本"}});
        SectionImportPreviewResponse badPreview = service.preview(10L, bad);
        assertThat(badPreview.getItemRows()).extracting(r -> r.getStatus())
                .containsExactly(ImportRowStatus.OK, ImportRowStatus.ERROR, ImportRowStatus.ERROR);

        MockMultipartFile good = fill(template,
                new String[][]{{"Kanji"}, {"日", "chú ý nét"}},
                new String[][]{{"TuLienQuan"}, {"日", "日本", "にほん"}});
        assertThat(service.commit(10L, good).getImportedChildren()).isEqualTo(1);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<LessonKanjiItemCreateRequest>> captor = ArgumentCaptor.forClass(List.class);
        verify(itemService).importKanjiItems(same(section), captor.capture());
        LessonKanjiItemCreateRequest req = captor.getValue().get(0);
        assertThat(req.getCharacter()).isEqualTo("日");
        assertThat(req.getRelatedVocabulary()).singleElement()
                .satisfies(rv -> assertThat(rv.getVocabIds()).containsExactly(7L));
        verify(resolver, atLeastOnce()).requireEditable(10L, LessonSectionType.KANJI);
    }
}
