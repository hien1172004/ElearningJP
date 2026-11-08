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

import com.jp.elearningjp.shared.util.JapaneseReadingService;

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
    private ImportVocabularyLookup lookup;
    /** Kuromoji nạp từ điển khá nặng → dùng chung 1 instance cho cả class. */
    private static final JapaneseReadingService READING = new JapaneseReadingService();

    @BeforeEach
    void setUp() {
        resolver = mock(ImportSectionResolver.class);
        vocabularyRepository = mock(VocabularyRepository.class);
        section = new LessonSection();
        section.setId(10L);
        when(resolver.requireEditable(eq(10L), any())).thenReturn(section);
        lookup = new ImportVocabularyLookup(vocabularyRepository, READING);
    }

    private static Vocabulary vocab(long id, String word, String hiragana) {
        Vocabulary v = Vocabulary.builder().word(word).hiragana(hiragana).romaji("rmj-" + hiragana)
                .meaningVi("nghĩa " + word).build();
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
        return new VocabularyImportService(excel, resolver, lookup, itemRepo, itemService);
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
                lookup, mock(LessonKanjiItemRepository.class), mock(KanjiSectionItemService.class));
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

    @Test
    void vocabulary_readings_fileThenDictThenAuto() throws Exception {
        LessonVocabularyItemRepository itemRepo = mock(LessonVocabularyItemRepository.class);
        VocabularySectionItemService itemService = mock(VocabularySectionItemService.class);
        when(itemRepo.findBySectionIdAndDeletedFalseOrderByOrderIndexAsc(10L)).thenReturn(List.of());
        // Katakana trong file được chuẩn hoá về Hiragana trước khi tra từ điển
        when(vocabularyRepository.findFirstByWordAndHiraganaAndDeletedFalseOrderBySenseNoAsc("食べる", "たべる"))
                .thenReturn(Optional.of(vocab(1L, "食べる", "たべる")));
        // Từ điển thiếu romaji → tự sinh từ hiragana
        Vocabulary noRomaji = vocab(2L, "学校", "がっこう");
        noRomaji.setRomaji(null);
        when(vocabularyRepository.findFirstByWordAndDeletedFalseOrderBySenseNoAsc("学校")).thenReturn(Optional.of(noRomaji));
        // Từ điển đủ dữ liệu → giữ nguyên từ điển (không ghi đè)
        when(vocabularyRepository.findFirstByWordAndDeletedFalseOrderBySenseNoAsc("水"))
                .thenReturn(Optional.of(vocab(3L, "水", "みず")));
        when(itemService.importVocabularyItems(any(), anyList())).thenAnswer(inv -> ((List<?>) inv.getArgument(1)).size());

        VocabularyImportService service = vocabService(itemRepo, itemService);
        MockMultipartFile file = fill(service.buildTemplate().content(),
                new String[][]{{"TuVung"},
                        {"食べる", "タベル", null, "taberu!"},
                        {"学校"},
                        {"水"}},
                new String[][]{{"ViDu"},
                        {"食べる", "たべる", "私は学校へ行きます。", "Tôi đi học."},
                        {"水", null, "水を飲む。", "Uống nước.", "mizu wo nomu (gv)"}});

        SectionImportPreviewResponse preview = service.preview(10L, file);
        assertThat(preview.isCanCommit()).isTrue();
        assertThat(preview.getItemRows().get(0).getData())
                .containsEntry("hiragana", "たべる")
                .containsEntry("hiraganaSource", "FILE")
                .containsEntry("romajiSource", "FILE");
        assertThat(preview.getItemRows().get(1).getData())
                .containsEntry("resolvedRomaji", "gakkou")
                .containsEntry("romajiSource", "AUTO");
        assertThat(preview.getItemRows().get(1).getStatus()).isEqualTo(ImportRowStatus.WARNING);
        assertThat(preview.getItemRows().get(2).getData()).containsEntry("romajiSource", "DICT");
        assertThat(preview.getChildRows().get(0).getData()).containsEntry("romajiSource", "AUTO");

        service.commit(10L, file);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<LessonVocabularyItemCreateRequest>> captor = ArgumentCaptor.forClass(List.class);
        verify(itemService).importVocabularyItems(same(section), captor.capture());
        List<LessonVocabularyItemCreateRequest> requests = captor.getValue();
        assertThat(requests.get(0).getCustomRomaji()).isEqualTo("taberu!");
        assertThat(requests.get(0).getCustomHiragana()).isNull();
        assertThat(requests.get(0).getExamples().get(0).getExampleRomaji())
                .startsWith("watashi wa gakkou e ");
        assertThat(requests.get(1).getCustomRomaji()).isEqualTo("gakkou");
        assertThat(requests.get(2).getCustomRomaji()).isNull();
        assertThat(requests.get(2).getExamples().get(0).getExampleRomaji()).isEqualTo("mizu wo nomu (gv)");
    }

    @Test
    void vocabulary_ambiguousWord_usesKuromojiReadingToPickSense() throws Exception {
        LessonVocabularyItemRepository itemRepo = mock(LessonVocabularyItemRepository.class);
        when(itemRepo.findBySectionIdAndDeletedFalseOrderByOrderIndexAsc(10L)).thenReturn(List.of());
        String guess = READING.toHiragana("今日");
        when(vocabularyRepository.findFirstByWordAndDeletedFalseOrderBySenseNoAsc("今日"))
                .thenReturn(Optional.of(vocab(1L, "今日", "こんにち")));
        when(vocabularyRepository.countByWordAndDeletedFalse("今日")).thenReturn(2L);
        when(vocabularyRepository.findFirstByWordAndHiraganaAndDeletedFalseOrderBySenseNoAsc("今日", guess))
                .thenReturn(Optional.of(vocab(2L, "今日", guess)));

        VocabularyImportService service = vocabService(itemRepo, mock(VocabularySectionItemService.class));
        MockMultipartFile file = fill(service.buildTemplate().content(), new String[][]{{"TuVung"}, {"今日"}});

        SectionImportPreviewResponse preview = service.preview(10L, file);
        assertThat(preview.getItemRows().get(0).getData()).containsEntry("vocabId", 2L);
        assertThat(preview.getItemRows().get(0).getStatus()).isEqualTo(ImportRowStatus.WARNING);
    }

    @Test
    void vocabulary_export_writesTemplateFormat_andCanBeImportedIntoAnotherSection() throws Exception {
        section.setTitle("Từ mới");
        Vocabulary taberu = vocab(1L, "食べる", "たべる");
        var item = com.jp.elearningjp.entity.curriculum.LessonVocabularyItem.builder()
                .vocab(taberu).customMeaning("ăn (riêng)").customRomaji("").note("Động từ nhóm 2").build();
        var primary = com.jp.elearningjp.entity.curriculum.LessonVocabularyExample.builder()
                .exampleJp("ご飯を食べる。").exampleVi("Ăn cơm.").primary(true).build();
        var deleted = com.jp.elearningjp.entity.curriculum.LessonVocabularyExample.builder()
                .exampleJp("xoá").exampleVi("xoá").build();
        deleted.setDeleted(true);
        item.getExamples().addAll(List.of(primary, deleted));

        LessonVocabularyItemRepository exportRepo = mock(LessonVocabularyItemRepository.class);
        when(exportRepo.findBySectionIdAndDeletedFalseOrderByOrderIndexAsc(10L)).thenReturn(List.of(item));
        ImportTemplateFile exported = vocabService(exportRepo, mock(VocabularySectionItemService.class)).exportSection(10L);
        assertThat(exported.fileName()).isEqualTo("tu_vung_tu-moi.xlsx");

        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(exported.content()))) {
            Row row = wb.getSheet("TuVung").getRow(1);
            assertThat(row.getCell(0).getStringCellValue()).isEqualTo("食べる");
            assertThat(row.getCell(1).getStringCellValue()).isEqualTo("たべる");
            assertThat(row.getCell(2).getStringCellValue()).isEqualTo("ăn (riêng)");
            assertThat(row.getCell(3).getStringCellValue()).isEqualTo("-");    // "" → '-' (để trống trong bài)
            assertThat(row.getCell(4)).isNull();                              // null → lấy theo từ điển
            assertThat(wb.getSheet("ViDu").getLastRowNum()).isEqualTo(1);     // bỏ câu ví dụ đã xoá
            assertThat(wb.getSheet("ViDu").getRow(1).getCell(6).getStringCellValue()).isEqualTo("x");
        }

        // Import lại file vừa xuất vào 1 section khác (chưa có từ này) → hợp lệ
        LessonVocabularyItemRepository emptyRepo = mock(LessonVocabularyItemRepository.class);
        when(emptyRepo.findBySectionIdAndDeletedFalseOrderByOrderIndexAsc(10L)).thenReturn(List.of());
        when(vocabularyRepository.findFirstByWordAndHiraganaAndDeletedFalseOrderBySenseNoAsc("食べる", "たべる"))
                .thenReturn(Optional.of(taberu));
        MockMultipartFile file = new MockMultipartFile("file", exported.fileName(),
                ImportTemplateFile.XLSX_MIME, exported.content());
        SectionImportPreviewResponse preview =
                vocabService(emptyRepo, mock(VocabularySectionItemService.class)).preview(10L, file);
        assertThat(preview.isCanCommit()).isTrue();
        assertThat(preview.getTotalItems()).isEqualTo(1);
        assertThat(preview.getTotalChildren()).isEqualTo(1);
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

        KanjiImportService service = new KanjiImportService(excel, resolver, charRepo, lookup,
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
