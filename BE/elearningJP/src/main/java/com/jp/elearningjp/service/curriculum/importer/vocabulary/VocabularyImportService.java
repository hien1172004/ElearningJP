package com.jp.elearningjp.service.curriculum.importer.vocabulary;

import com.jp.elearningjp.dto.request.curriculum.LessonVocabularyExampleCreateRequest;
import com.jp.elearningjp.dto.request.curriculum.LessonVocabularyItemCreateRequest;
import com.jp.elearningjp.dto.response.curriculum.importer.ImportRowResult;
import com.jp.elearningjp.dto.response.curriculum.importer.SectionImportCommitResponse;
import com.jp.elearningjp.dto.response.curriculum.importer.SectionImportPreviewResponse;
import com.jp.elearningjp.entity.content.Vocabulary;
import com.jp.elearningjp.entity.curriculum.LessonSection;
import com.jp.elearningjp.entity.curriculum.LessonVocabularyItem;
import com.jp.elearningjp.exception.AppException;
import com.jp.elearningjp.exception.ErrorCode;
import com.jp.elearningjp.repository.content.VocabularyRepository;
import com.jp.elearningjp.repository.curriculum.LessonVocabularyItemRepository;
import com.jp.elearningjp.service.curriculum.importer.ExcelImportSupport;
import com.jp.elearningjp.service.curriculum.importer.ImportColumn;
import com.jp.elearningjp.service.curriculum.importer.ImportSectionResolver;
import com.jp.elearningjp.service.curriculum.importer.ImportTemplateFile;
import com.jp.elearningjp.service.curriculum.section.VocabularySectionItemService;
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
import java.util.stream.Collectors;

/**
 * Import từ vựng vào section VOCABULARY từ mẫu Excel riêng của loại section này.
 *
 * <p>Mẫu gồm 2 sheet:</p>
 * <ul>
 *   <li>{@code TuVung} — mỗi dòng 1 từ, tra từ điển theo {@code word} (+ {@code hiragana} nếu có);
 *       các cột còn lại là override, ô trống = lấy theo từ điển, {@code -} = để trống trong bài.</li>
 *   <li>{@code ViDu} (tuỳ chọn) — câu ví dụ, nối với từ theo {@code word} + {@code hiragana}.
 *       Từ không có câu ví dụ nào trong file sẽ tự chép ví dụ từ từ điển (giống thêm tay).</li>
 * </ul>
 *
 * <p>Luồng: {@link #preview} chỉ kiểm tra, không ghi DB; {@link #commit} kiểm tra lại toàn bộ
 * rồi ghi trong 1 transaction — file còn dòng lỗi thì không ghi gì.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VocabularyImportService {

    public static final String TEMPLATE_TYPE = "LESSON_VOCABULARY";
    public static final int TEMPLATE_VERSION = 1;
    static final String ITEM_SHEET = "TuVung";
    static final String EXAMPLE_SHEET = "ViDu";
    static final int MAX_ITEMS = 500;
    static final int MAX_EXAMPLES = 2000;
    static final int MAX_EXAMPLES_PER_ITEM = 100;

    // ----- Sheet TuVung -----
    static final ImportColumn WORD = ImportColumn.required("word", 16,
            "Từ vựng (kanji/kana) đúng như trong từ điển. Vd: 食べる");
    static final ImportColumn HIRAGANA = ImportColumn.optional("hiragana", 16,
            "Cách đọc để chọn đúng từ khi 1 chữ có nhiều cách đọc. Bỏ trống = lấy nghĩa đầu tiên");
    static final ImportColumn MEANING = ImportColumn.optional("meaning_vi", 30,
            "Nghĩa tiếng Việt riêng cho bài. Trống = lấy từ điển, '-' = để trống");
    static final ImportColumn ROMAJI = ImportColumn.optional("romaji", 16,
            "Romaji riêng cho bài. Trống = lấy từ điển, '-' = để trống");
    static final ImportColumn AUDIO = ImportColumn.optional("audio_url", 30,
            "Link audio riêng (http/https). Trống = lấy từ điển, '-' = không có audio");
    static final ImportColumn IMAGE = ImportColumn.optional("image_url", 30,
            "Link ảnh minh hoạ (http/https)");
    static final ImportColumn NOTE = ImportColumn.optional("note", 30,
            "Ghi chú của giáo viên (tối đa 2000 ký tự)");
    static final List<ImportColumn> ITEM_COLUMNS = List.of(WORD, HIRAGANA, MEANING, ROMAJI, AUDIO, IMAGE, NOTE);

    // ----- Sheet ViDu -----
    static final ImportColumn EX_WORD = ImportColumn.required("word", 16,
            "Từ vựng của câu ví dụ — phải có trong sheet TuVung");
    static final ImportColumn EX_HIRAGANA = ImportColumn.optional("hiragana", 16,
            "Giống cột hiragana của từ đó ở sheet TuVung (có thể bỏ trống nếu word không trùng)");
    static final ImportColumn EX_JP = ImportColumn.required("example_jp", 40, "Câu ví dụ tiếng Nhật");
    static final ImportColumn EX_VI = ImportColumn.required("example_vi", 40, "Bản dịch tiếng Việt");
    static final ImportColumn EX_ROMAJI = ImportColumn.optional("example_romaji", 30, "Romaji của câu (tuỳ chọn)");
    static final ImportColumn EX_AUDIO = ImportColumn.optional("audio_url", 30, "Link audio câu ví dụ (http/https)");
    static final ImportColumn EX_PRIMARY = ImportColumn.optional("primary", 10,
            "Chọn 'x' để đánh dấu câu ví dụ chính (mỗi từ 1 câu)").withAllowedValues(List.of("x"));
    static final List<ImportColumn> EXAMPLE_COLUMNS =
            List.of(EX_WORD, EX_HIRAGANA, EX_JP, EX_VI, EX_ROMAJI, EX_AUDIO, EX_PRIMARY);

    private final ExcelImportSupport excel;
    private final ImportSectionResolver sectionResolver;
    private final VocabularyRepository vocabularyRepository;
    private final LessonVocabularyItemRepository vocabItemRepository;
    private final VocabularySectionItemService vocabularySectionItemService;

    // ============================================================
    // TEMPLATE
    // ============================================================

    public ImportTemplateFile buildTemplate() {
        List<String> guide = new ArrayList<>(List.of(
                "Cách dùng:",
                "1. Điền mỗi từ vựng 1 dòng ở sheet 'TuVung'. Từ phải có sẵn trong từ điển của hệ thống.",
                "2. (Tuỳ chọn) Điền câu ví dụ ở sheet 'ViDu'. Từ nào không có câu ví dụ sẽ tự lấy ví dụ từ từ điển.",
                "3. Ô trống ở cột tuỳ chỉnh = lấy dữ liệu từ điển; nhập '-' = để trống trường đó trong bài.",
                "4. Không đổi tên sheet, tên cột hoặc thứ tự cột. Không xoá sheet ẩn '_meta'.",
                "5. Tối đa " + MAX_ITEMS + " từ và " + MAX_EXAMPLES + " câu ví dụ mỗi file, file ≤ 2MB.",
                "6. Từ đã có trong section sẽ bị báo lỗi. Cột có dấu * là bắt buộc.",
                "",
                "Ví dụ — sheet TuVung:   食べる | たべる | ăn | | | | Động từ nhóm 2",
                "Ví dụ — sheet ViDu:     食べる | たべる | ご飯を食べる。 | Ăn cơm. | | | x",
                ""));
        guide.addAll(ExcelImportSupport.describeColumns(ITEM_SHEET, ITEM_COLUMNS));
        guide.addAll(ExcelImportSupport.describeColumns(EXAMPLE_SHEET, EXAMPLE_COLUMNS));

        XSSFWorkbook wb = excel.newTemplate(TEMPLATE_TYPE, TEMPLATE_VERSION,
                "MẪU IMPORT TỪ VỰNG VÀO BÀI HỌC (v" + TEMPLATE_VERSION + ")", guide);
        excel.addDataSheet(wb, ITEM_SHEET, ITEM_COLUMNS, MAX_ITEMS);
        excel.addDataSheet(wb, EXAMPLE_SHEET, EXAMPLE_COLUMNS, MAX_EXAMPLES);
        wb.setActiveSheet(wb.getSheetIndex(ITEM_SHEET));
        return new ImportTemplateFile("mau_import_tu_vung_v" + TEMPLATE_VERSION + ".xlsx", excel.toBytes(wb));
    }

    // ============================================================
    // PREVIEW / COMMIT
    // ============================================================

    @Transactional(readOnly = true)
    public SectionImportPreviewResponse preview(Long sectionId, MultipartFile file) {
        LessonSection section = sectionResolver.requireEditable(sectionId, LessonSectionType.VOCABULARY);
        return parse(section, file).preview();
    }

    @Transactional
    public SectionImportCommitResponse commit(Long sectionId, MultipartFile file) {
        LessonSection section = sectionResolver.requireEditable(sectionId, LessonSectionType.VOCABULARY);
        Parsed parsed = parse(section, file);
        if (!parsed.preview().isCanCommit()) {
            throw new AppException(ErrorCode.IMPORT_HAS_ERRORS);
        }
        int items = vocabularySectionItemService.importVocabularyItems(section, parsed.requests());
        log.info("[VOCAB-IMPORT] section id={} imported {} items, {} examples",
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
                          List<LessonVocabularyItemCreateRequest> requests,
                          int childCount) {
    }

    /** 1 từ hợp lệ ở sheet TuVung + các câu ví dụ gắn vào nó. */
    private static final class Draft {
        final ImportRowResult row;
        final LessonVocabularyItemCreateRequest request;
        final List<LessonVocabularyExampleCreateRequest> examples = new ArrayList<>();
        boolean hasPrimary;

        Draft(ImportRowResult row, LessonVocabularyItemCreateRequest request) {
            this.row = row;
            this.request = request;
        }
    }

    private Parsed parse(LessonSection section, MultipartFile file) {
        List<Row> itemRows;
        List<Row> exampleRows;
        Workbook wb = excel.open(file);
        List<ImportRowResult> itemResults = new ArrayList<>();
        List<ImportRowResult> exampleResults = new ArrayList<>();
        Map<String, Draft> draftsByKey = new LinkedHashMap<>();
        try {
            excel.verifyMeta(wb, TEMPLATE_TYPE, TEMPLATE_VERSION);
            Sheet itemSheet = excel.requireSheet(wb, ITEM_SHEET, ITEM_COLUMNS, false);
            Sheet exampleSheet = excel.requireSheet(wb, EXAMPLE_SHEET, EXAMPLE_COLUMNS, true);
            itemRows = excel.dataRows(itemSheet, ITEM_COLUMNS.size(), MAX_ITEMS);
            exampleRows = excel.dataRows(exampleSheet, EXAMPLE_COLUMNS.size(), MAX_EXAMPLES);
            if (itemRows.isEmpty()) {
                throw new AppException(ErrorCode.IMPORT_NO_ROWS);
            }

            Set<Long> existingVocabIds = vocabItemRepository
                    .findBySectionIdAndDeletedFalseOrderByOrderIndexAsc(section.getId()).stream()
                    .map(LessonVocabularyItem::getVocab)
                    .filter(Objects::nonNull)
                    .map(Vocabulary::getId)
                    .collect(Collectors.toSet());
            Map<Long, Integer> seenVocabRow = new HashMap<>();

            for (Row row : itemRows) {
                ImportRowResult result = excel.newResult(row, ITEM_COLUMNS);
                itemResults.add(result);
                Draft draft = parseItemRow(result, existingVocabIds, seenVocabRow);
                if (draft != null) {
                    draftsByKey.putIfAbsent(key(draft.row), draft);
                }
            }
            for (Row row : exampleRows) {
                ImportRowResult result = excel.newResult(row, EXAMPLE_COLUMNS);
                exampleResults.add(result);
                parseExampleRow(result, draftsByKey);
            }
        } finally {
            excel.closeQuietly(wb);
        }

        List<LessonVocabularyItemCreateRequest> requests = new ArrayList<>();
        int childCount = 0;
        for (Draft d : draftsByKey.values()) {
            if (!d.examples.isEmpty()) {
                d.request.setExamples(d.examples);
                childCount += d.examples.size();
            }
            d.row.getData().put("exampleCount", d.examples.size());
            requests.add(d.request);
        }
        return new Parsed(ExcelImportSupport.summarize(LessonSectionType.VOCABULARY, itemResults, exampleResults),
                requests, childCount);
    }

    /** Kiểm tra 1 dòng sheet TuVung. Trả về draft nếu tra được từ điển (dòng có thể vẫn lỗi ở cột khác). */
    private Draft parseItemRow(ImportRowResult r, Set<Long> existingVocabIds, Map<Long, Integer> seenVocabRow) {
        String word = excel.text(r, WORD, 100);
        String hiragana = excel.text(r, HIRAGANA, 100);
        String meaning = excel.text(r, MEANING, 5000);
        String romaji = excel.text(r, ROMAJI, 150);
        String audio = excel.text(r, AUDIO, 500);
        String image = excel.text(r, IMAGE, 500);
        String note = excel.text(r, NOTE, 2000);
        excel.checkUrl(r, AUDIO, audio);
        excel.checkUrl(r, IMAGE, image);
        if (ExcelImportSupport.CLEAR_MARKER.equals(image)) {
            image = null; // ảnh không có giá trị từ điển để xoá — '-' coi như trống
        }
        if (word == null) {
            return null;
        }

        Optional<Vocabulary> found = hiragana != null
                ? vocabularyRepository.findFirstByWordAndHiraganaAndDeletedFalseOrderBySenseNoAsc(word, hiragana)
                : vocabularyRepository.findFirstByWordAndDeletedFalseOrderBySenseNoAsc(word);
        if (found.isEmpty()) {
            String readings = hiragana != null ? readingsOf(word) : null;
            if (readings != null) {
                r.error("Từ '" + word + "' không có cách đọc '" + hiragana + "' trong từ điển. Các cách đọc có: " + readings);
            } else {
                r.error("Không tìm thấy từ '" + word + "' trong từ điển");
            }
            return null;
        }
        Vocabulary vocab = found.get();
        r.getData().put("vocabId", vocab.getId());
        r.getData().put("dictHiragana", vocab.getHiragana());
        r.getData().put("dictMeaning", vocab.getMeaningVi());
        if (hiragana == null && vocabularyRepository.countByWordAndDeletedFalse(word) > 1) {
            r.warn("Từ '" + word + "' có nhiều nghĩa/cách đọc (" + readingsOf(word) + "), đã chọn '"
                    + vocab.getHiragana() + "'. Điền cột hiragana để chọn chính xác");
        }

        if (existingVocabIds.contains(vocab.getId())) {
            r.error("Từ này đã có trong section");
        }
        Integer firstRow = seenVocabRow.putIfAbsent(vocab.getId(), r.getRowNumber());
        if (firstRow != null) {
            r.error("Trùng với dòng " + firstRow + " của sheet " + ITEM_SHEET);
            return null;
        }

        LessonVocabularyItemCreateRequest request = LessonVocabularyItemCreateRequest.builder()
                .vocabId(vocab.getId())
                .customMeaning(ExcelImportSupport.toOverride(meaning))
                .customRomaji(ExcelImportSupport.toOverride(romaji))
                .customAudioUrl(ExcelImportSupport.toOverride(audio))
                .imageUrl(image)
                .note(note)
                .build();
        return new Draft(r, request);
    }

    private void parseExampleRow(ImportRowResult r, Map<String, Draft> draftsByKey) {
        String word = excel.text(r, EX_WORD, 100);
        String hiragana = excel.text(r, EX_HIRAGANA, 100);
        String jp = excel.text(r, EX_JP, 5000);
        String vi = excel.text(r, EX_VI, 5000);
        String romaji = excel.text(r, EX_ROMAJI, 5000);
        String audio = excel.text(r, EX_AUDIO, 500);
        String primary = excel.text(r, EX_PRIMARY, 10);
        excel.checkUrl(r, EX_AUDIO, audio);
        boolean isPrimary = primary != null && primary.equalsIgnoreCase("x");
        if (primary != null && !isPrimary) {
            r.error("Cột 'primary' chỉ nhận 'x' hoặc để trống");
        }
        if (word == null) {
            return;
        }

        Draft draft = findDraft(draftsByKey, word, hiragana);
        if (draft == null) {
            r.error("Không có từ '" + word + "'" + (hiragana != null ? " (" + hiragana + ")" : "")
                    + " hợp lệ trong sheet " + ITEM_SHEET);
            return;
        }
        r.getData().put("linkedRow", draft.row.getRowNumber());
        if (draft.examples.size() >= MAX_EXAMPLES_PER_ITEM) {
            r.error("Mỗi từ tối đa " + MAX_EXAMPLES_PER_ITEM + " câu ví dụ");
            return;
        }
        if (isPrimary && draft.hasPrimary) {
            r.warn("Từ này đã có câu ví dụ chính ở dòng trên, câu này sẽ là câu thường");
            isPrimary = false;
        }
        draft.hasPrimary |= isPrimary;
        if (r.hasError()) {
            return;
        }
        draft.examples.add(LessonVocabularyExampleCreateRequest.builder()
                .exampleJp(jp)
                .exampleVi(vi)
                .exampleRomaji(romaji)
                .audioUrl(audio)
                .isPrimary(isPrimary)
                .build());
    }

    /** Các cách đọc (không trùng) của 1 từ trong từ điển, nối bằng dấu phẩy; null nếu từ không có. */
    private String readingsOf(String word) {
        List<String> readings = vocabularyRepository.findByWordAndDeletedFalseOrderBySenseNoAsc(word).stream()
                .map(Vocabulary::getHiragana)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        return readings.isEmpty() ? null : String.join(", ", readings);
    }

    /** Tìm từ theo word+hiragana; nếu câu ví dụ bỏ trống hiragana thì nhận khi word là duy nhất. */
    private Draft findDraft(Map<String, Draft> draftsByKey, String word, String hiragana) {
        Draft exact = draftsByKey.get(key(word, hiragana));
        if (exact != null || hiragana != null) {
            return exact;
        }
        List<Draft> sameWord = draftsByKey.values().stream()
                .filter(d -> word.equals(d.row.getData().get(WORD.key())))
                .toList();
        return sameWord.size() == 1 ? sameWord.get(0) : null;
    }

    private static String key(ImportRowResult r) {
        return key((String) r.getData().get(WORD.key()), (String) r.getData().get(HIRAGANA.key()));
    }

    private static String key(String word, String hiragana) {
        return word + "\u0000" + (hiragana == null ? "" : hiragana);
    }
}
