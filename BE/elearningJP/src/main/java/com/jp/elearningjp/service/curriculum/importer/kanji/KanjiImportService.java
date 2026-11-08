package com.jp.elearningjp.service.curriculum.importer.kanji;

import com.jp.elearningjp.dto.request.curriculum.LessonKanjiItemCreateRequest;
import com.jp.elearningjp.dto.request.curriculum.LessonKanjiRelatedVocabRequest;
import com.jp.elearningjp.dto.response.curriculum.importer.ImportRowResult;
import com.jp.elearningjp.dto.response.curriculum.importer.SectionImportCommitResponse;
import com.jp.elearningjp.dto.response.curriculum.importer.SectionImportPreviewResponse;
import com.jp.elearningjp.entity.content.Vocabulary;
import com.jp.elearningjp.entity.content.WritingCharacter;
import com.jp.elearningjp.entity.curriculum.LessonKanjiItem;
import com.jp.elearningjp.entity.curriculum.LessonSection;
import com.jp.elearningjp.exception.AppException;
import com.jp.elearningjp.exception.ErrorCode;
import com.jp.elearningjp.repository.content.WritingCharacterRepository;
import com.jp.elearningjp.repository.curriculum.LessonKanjiItemRepository;
import com.jp.elearningjp.service.curriculum.importer.ExcelImportSupport;
import com.jp.elearningjp.service.curriculum.importer.ImportColumn;
import com.jp.elearningjp.service.curriculum.importer.ImportSectionResolver;
import com.jp.elearningjp.service.curriculum.importer.ImportTemplateFile;
import com.jp.elearningjp.service.curriculum.importer.ImportVocabularyLookup;
import com.jp.elearningjp.service.curriculum.section.KanjiSectionItemService;
import com.jp.elearningjp.shared.enums.LessonSectionType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Import chữ Hán vào section KANJI từ mẫu Excel riêng của loại section này.
 *
 * <p>Mẫu gồm 2 sheet:</p>
 * <ul>
 *   <li>{@code Kanji} — mỗi dòng 1 chữ Hán có sẵn trong từ điển + ghi chú. Âm On/Kun, nghĩa,
 *       số nét… luôn lấy từ từ điển (giáo viên không override).</li>
 *   <li>{@code TuLienQuan} (tuỳ chọn) — từ vựng liên quan của từng chữ, tra từ điển theo
 *       {@code word} (+ {@code hiragana}). Không tạo từ mới.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KanjiImportService {

    public static final String TEMPLATE_TYPE = "LESSON_KANJI";
    public static final int TEMPLATE_VERSION = 1;
    static final String TEMPLATE_FILE_NAME = "mau_import_kanji_v" + TEMPLATE_VERSION + ".xlsx";
    static final String ITEM_SHEET = "Kanji";
    static final String RELATED_SHEET = "TuLienQuan";
    static final int MAX_ITEMS = 500;
    static final int MAX_RELATED = 3000;
    /** Khớp giới hạn MAX_RELATED_VOCAB_PER_REQUEST của KanjiSectionItemService. */
    static final int MAX_RELATED_PER_KANJI = 200;

    // ----- Sheet Kanji -----
    static final ImportColumn KANJI = ImportColumn.required("kanji", 10,
            "1 chữ Hán có trong từ điển. Vd: 日");
    static final ImportColumn NOTE = ImportColumn.optional("note", 40,
            "Ghi chú của giáo viên (tối đa 2000 ký tự)");
    static final List<ImportColumn> ITEM_COLUMNS = List.of(KANJI, NOTE);

    // ----- Sheet TuLienQuan -----
    static final ImportColumn REL_KANJI = ImportColumn.required("kanji", 10,
            "Chữ Hán — phải có trong sheet Kanji");
    static final ImportColumn REL_WORD = ImportColumn.required("word", 16,
            "Từ vựng liên quan có trong từ điển, nên chứa chữ Hán đó. Vd: 日本");
    static final ImportColumn REL_HIRAGANA = ImportColumn.optional("hiragana", 16,
            "Cách đọc (Hiragana/Katakana) để chọn đúng từ. Bỏ trống = hệ thống tự nhận diện, không được thì lấy nghĩa đầu");
    static final ImportColumn REL_NOTE = ImportColumn.optional("note", 30,
            "Ghi chú cho từ liên quan (tối đa 2000 ký tự)");
    static final List<ImportColumn> RELATED_COLUMNS = List.of(REL_KANJI, REL_WORD, REL_HIRAGANA, REL_NOTE);

    private final ExcelImportSupport excel;
    private final ImportSectionResolver sectionResolver;
    private final WritingCharacterRepository writingCharacterRepository;
    private final ImportVocabularyLookup vocabLookup;
    private final LessonKanjiItemRepository kanjiItemRepository;
    private final KanjiSectionItemService kanjiSectionItemService;

    // ============================================================
    // TEMPLATE
    // ============================================================

    public ImportTemplateFile buildTemplate() {
        return new ImportTemplateFile(TEMPLATE_FILE_NAME, excel.toBytes(createWorkbook()));
    }

    /** Workbook mẫu (Hướng dẫn + _meta + các sheet dữ liệu trống) — dùng cho cả tải mẫu và xuất dữ liệu. */
    private XSSFWorkbook createWorkbook() {
        List<String> guide = new ArrayList<>(List.of(
                "Cách dùng:",
                "1. Điền mỗi chữ Hán 1 dòng ở sheet 'Kanji'. Chữ phải có sẵn trong từ điển của hệ thống.",
                "2. (Tuỳ chọn) Điền từ vựng liên quan ở sheet 'TuLienQuan', mỗi dòng 1 cặp chữ Hán – từ vựng.",
                "   Chữ nào không khai báo từ liên quan sẽ hiển thị từ liên quan tự động từ từ điển.",
                "3. Âm On/Kun, nghĩa, số nét luôn lấy từ từ điển.",
                "4. Không đổi tên sheet, tên cột hoặc thứ tự cột. Không xoá sheet ẩn '_meta'.",
                "5. Tối đa " + MAX_ITEMS + " chữ Hán, " + MAX_RELATED_PER_KANJI
                        + " từ liên quan mỗi chữ, file ≤ 2MB.",
                "6. Chữ Hán đã có trong section sẽ bị báo lỗi. Cột có dấu * là bắt buộc.",
                "",
                "Ví dụ — sheet Kanji:        日 | Chữ cơ bản, chú ý nét",
                "Ví dụ — sheet TuLienQuan:   日 | 日本 | にほん |",
                ""));
        guide.addAll(ExcelImportSupport.describeColumns(ITEM_SHEET, ITEM_COLUMNS));
        guide.addAll(ExcelImportSupport.describeColumns(RELATED_SHEET, RELATED_COLUMNS));

        XSSFWorkbook wb = excel.newTemplate(TEMPLATE_TYPE, TEMPLATE_VERSION,
                "MẪU IMPORT CHỮ HÁN VÀO BÀI HỌC (v" + TEMPLATE_VERSION + ")", guide);
        excel.addDataSheet(wb, ITEM_SHEET, ITEM_COLUMNS, MAX_ITEMS);
        excel.addDataSheet(wb, RELATED_SHEET, RELATED_COLUMNS, MAX_RELATED);
        wb.setActiveSheet(wb.getSheetIndex(ITEM_SHEET));
        return wb;
    }

    // ============================================================
    // EXPORT — xuất dữ liệu section theo đúng mẫu import (import lại được sang section khác)
    // ============================================================

    /** Xuất chữ Hán (+ từ liên quan) của section ra file Excel cùng định dạng mẫu import. */
    @Transactional(readOnly = true)
    public ImportTemplateFile exportSection(Long sectionId) {
        LessonSection section = sectionResolver.requireEditable(sectionId, LessonSectionType.KANJI);
        List<String[]> itemRows = new ArrayList<>();
        List<String[]> relatedRows = new ArrayList<>();
        for (LessonKanjiItem item : kanjiItemRepository.findBySectionIdAndDeletedFalseOrderByOrderIndexAsc(sectionId)) {
            if (item.getCharacter() == null) continue;
            String kanji = item.getCharacter().getCharacter();
            itemRows.add(new String[]{kanji, item.getNote()});
            item.getRelatedVocabulary().stream()
                    .filter(rv -> !rv.isDeleted() && rv.getVocab() != null)
                    .forEach(rv -> relatedRows.add(new String[]{
                            kanji, rv.getVocab().getWord(), rv.getVocab().getHiragana(), rv.getNote()}));
        }
        XSSFWorkbook wb = createWorkbook();
        excel.writeRows(wb.getSheet(ITEM_SHEET), itemRows);
        excel.writeRows(wb.getSheet(RELATED_SHEET), relatedRows);
        log.info("[KANJI-EXPORT] section id={} exported {} items, {} related",
                sectionId, itemRows.size(), relatedRows.size());
        return new ImportTemplateFile(ExcelImportSupport.exportFileName("chu_han", section), excel.toBytes(wb));
    }

    // ============================================================
    // PREVIEW / COMMIT
    // ============================================================

    @Transactional(readOnly = true)
    public SectionImportPreviewResponse preview(Long sectionId, MultipartFile file) {
        LessonSection section = sectionResolver.requireEditable(sectionId, LessonSectionType.KANJI);
        return parse(section, file).preview();
    }

    @Transactional
    public SectionImportCommitResponse commit(Long sectionId, MultipartFile file) {
        LessonSection section = sectionResolver.requireEditable(sectionId, LessonSectionType.KANJI);
        Parsed parsed = parse(section, file);
        if (!parsed.preview().isCanCommit()) {
            throw new AppException(ErrorCode.IMPORT_HAS_ERRORS);
        }
        int items = kanjiSectionItemService.importKanjiItems(section, parsed.requests());
        log.info("[KANJI-IMPORT] section id={} imported {} kanji, {} related vocab",
                sectionId, items, parsed.childCount());
        return SectionImportCommitResponse.builder()
                .importedItems(items)
                .importedChildren(parsed.childCount())
                .build();
    }

    // ============================================================
    // PARSE + VALIDATE
    // ============================================================

    private record Parsed(SectionImportPreviewResponse preview,
                          List<LessonKanjiItemCreateRequest> requests,
                          int childCount) {
    }

    /** 1 chữ Hán hợp lệ ở sheet Kanji + các từ liên quan gắn vào nó. */
    private static final class Draft {
        final ImportRowResult row;
        final LessonKanjiItemCreateRequest request;
        final List<LessonKanjiRelatedVocabRequest> related = new ArrayList<>();
        final Map<Long, Integer> relatedVocabRow = new HashMap<>();

        Draft(ImportRowResult row, LessonKanjiItemCreateRequest request) {
            this.row = row;
            this.request = request;
        }
    }

    private Parsed parse(LessonSection section, MultipartFile file) {
        List<ImportRowResult> itemResults = new ArrayList<>();
        List<ImportRowResult> relatedResults = new ArrayList<>();
        Map<String, Draft> draftsByKanji = new LinkedHashMap<>();
        Workbook wb = excel.open(file);
        try {
            excel.verifyMeta(wb, TEMPLATE_TYPE, TEMPLATE_VERSION);
            Sheet itemSheet = excel.requireSheet(wb, ITEM_SHEET, ITEM_COLUMNS, false);
            Sheet relatedSheet = excel.requireSheet(wb, RELATED_SHEET, RELATED_COLUMNS, true);
            List<Row> itemRows = excel.dataRows(itemSheet, ITEM_COLUMNS.size(), MAX_ITEMS);
            List<Row> relatedRows = excel.dataRows(relatedSheet, RELATED_COLUMNS.size(), MAX_RELATED);
            if (itemRows.isEmpty()) {
                throw new AppException(ErrorCode.IMPORT_NO_ROWS);
            }

            for (Row row : itemRows) {
                itemResults.add(excel.newResult(row, ITEM_COLUMNS));
            }
            // Tra từ điển 1 lần cho cả file
            Set<String> characters = itemResults.stream()
                    .map(r -> (String) r.getData().get(KANJI.key()))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            Map<String, WritingCharacter> dictionary = characters.isEmpty() ? Map.of()
                    : writingCharacterRepository.findByCharacterInAndDeletedFalse(characters).stream()
                    .collect(Collectors.toMap(WritingCharacter::getCharacter, Function.identity(), (a, b) -> a));
            Set<Long> existingCharacterIds = kanjiItemRepository
                    .findBySectionIdAndDeletedFalseOrderByOrderIndexAsc(section.getId()).stream()
                    .map(LessonKanjiItem::getCharacter)
                    .filter(Objects::nonNull)
                    .map(WritingCharacter::getId)
                    .collect(Collectors.toSet());

            for (ImportRowResult result : itemResults) {
                Draft draft = parseItemRow(result, dictionary, existingCharacterIds, draftsByKanji);
                if (draft != null) {
                    draftsByKanji.put(draft.request.getCharacter(), draft);
                }
            }
            for (Row row : relatedRows) {
                ImportRowResult result = excel.newResult(row, RELATED_COLUMNS);
                relatedResults.add(result);
                parseRelatedRow(result, draftsByKanji);
            }
        } finally {
            excel.closeQuietly(wb);
        }

        List<LessonKanjiItemCreateRequest> requests = new ArrayList<>();
        int childCount = 0;
        for (Draft d : draftsByKanji.values()) {
            if (!d.related.isEmpty()) {
                d.request.setRelatedVocabulary(d.related);
                childCount += d.related.size();
            }
            d.row.getData().put("relatedCount", d.related.size());
            requests.add(d.request);
        }
        return new Parsed(ExcelImportSupport.summarize(LessonSectionType.KANJI, itemResults, relatedResults),
                requests, childCount);
    }

    private Draft parseItemRow(ImportRowResult r, Map<String, WritingCharacter> dictionary,
                               Set<Long> existingCharacterIds, Map<String, Draft> draftsByKanji) {
        String kanji = excel.text(r, KANJI, 8);
        String note = excel.text(r, NOTE, 2000);
        if (kanji == null) {
            return null;
        }
        if (kanji.codePointCount(0, kanji.length()) != 1) {
            r.error("Mỗi dòng chỉ nhập 1 chữ Hán");
            return null;
        }
        WritingCharacter character = dictionary.get(kanji);
        if (character == null) {
            r.error("Không tìm thấy chữ '" + kanji + "' trong từ điển");
            return null;
        }
        r.getData().put("characterId", character.getId());
        r.getData().put("dictMeaning", character.getMeaningVi());
        r.getData().put("dictHanViet", character.getHanViet());
        // Copy ra list thường: collection của Hibernate không đọc được sau khi transaction đóng
        r.getData().put("dictOnyomi", character.getOnyomi() == null ? List.of() : new ArrayList<>(character.getOnyomi()));
        r.getData().put("dictKunyomi", character.getKunyomi() == null ? List.of() : new ArrayList<>(character.getKunyomi()));

        if (existingCharacterIds.contains(character.getId())) {
            r.error("Chữ Hán này đã có trong section");
        }
        Draft first = draftsByKanji.get(kanji);
        if (first != null) {
            r.error("Trùng với dòng " + first.row.getRowNumber() + " của sheet " + ITEM_SHEET);
            return null;
        }
        LessonKanjiItemCreateRequest request = LessonKanjiItemCreateRequest.builder()
                .character(kanji)
                .note(note)
                .build();
        return new Draft(r, request);
    }

    private void parseRelatedRow(ImportRowResult r, Map<String, Draft> draftsByKanji) {
        String kanji = excel.text(r, REL_KANJI, 8);
        String word = excel.text(r, REL_WORD, 100);
        excel.text(r, REL_HIRAGANA, 100);
        String hiragana = vocabLookup.normalizeHiragana(r, REL_HIRAGANA);
        String note = excel.text(r, REL_NOTE, 2000);
        if (kanji == null || word == null) {
            return;
        }
        Draft draft = draftsByKanji.get(kanji);
        if (draft == null) {
            r.error("Chữ '" + kanji + "' không có (hoặc không hợp lệ) trong sheet " + ITEM_SHEET);
            return;
        }
        r.getData().put("linkedRow", draft.row.getRowNumber());

        Optional<Vocabulary> found = vocabLookup.find(r, word, hiragana);
        if (found.isEmpty()) {
            return;
        }
        Vocabulary vocab = found.get();
        if (!word.contains(kanji)) {
            r.warn("Từ '" + word + "' không chứa chữ '" + kanji + "'");
        }
        Integer firstRow = draft.relatedVocabRow.putIfAbsent(vocab.getId(), r.getRowNumber());
        if (firstRow != null) {
            r.error("Trùng với dòng " + firstRow + " (cùng chữ Hán và từ vựng)");
            return;
        }
        if (draft.related.size() >= MAX_RELATED_PER_KANJI) {
            r.error("Mỗi chữ Hán tối đa " + MAX_RELATED_PER_KANJI + " từ liên quan");
            return;
        }
        if (r.hasError()) {
            return;
        }
        draft.related.add(LessonKanjiRelatedVocabRequest.builder()
                .vocabIds(List.of(vocab.getId()))
                .note(note)
                .build());
    }
}
