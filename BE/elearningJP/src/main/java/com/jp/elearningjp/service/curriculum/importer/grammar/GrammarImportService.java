package com.jp.elearningjp.service.curriculum.importer.grammar;

import com.jp.elearningjp.dto.request.curriculum.LessonGrammarExampleCreateRequest;
import com.jp.elearningjp.dto.request.curriculum.LessonGrammarItemCreateRequest;
import com.jp.elearningjp.dto.response.curriculum.importer.ImportRowResult;
import com.jp.elearningjp.dto.response.curriculum.importer.SectionImportCommitResponse;
import com.jp.elearningjp.dto.response.curriculum.importer.SectionImportPreviewResponse;
import com.jp.elearningjp.entity.content.GrammarPoint;
import com.jp.elearningjp.entity.curriculum.LessonGrammarItem;
import com.jp.elearningjp.entity.curriculum.LessonSection;
import com.jp.elearningjp.exception.AppException;
import com.jp.elearningjp.exception.ErrorCode;
import com.jp.elearningjp.repository.content.GrammarPointRepository;
import com.jp.elearningjp.repository.curriculum.LessonGrammarItemRepository;
import com.jp.elearningjp.service.curriculum.importer.ExcelImportSupport;
import com.jp.elearningjp.service.curriculum.importer.ImportColumn;
import com.jp.elearningjp.service.curriculum.importer.ImportSectionResolver;
import com.jp.elearningjp.service.curriculum.importer.ImportTemplateFile;
import com.jp.elearningjp.service.curriculum.section.GrammarSectionItemService;
import com.jp.elearningjp.shared.enums.JlptLevel;
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

import java.text.Normalizer;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Import điểm ngữ pháp vào section GRAMMAR từ mẫu Excel riêng của loại section này.
 *
 * <p>Mẫu gồm 2 sheet:</p>
 * <ul>
 *   <li>{@code NguPhap} — mỗi dòng 1 điểm ngữ pháp, tra từ điển theo {@code title} + {@code jlpt_level}
 *       (bảng ngữ pháp không có khoá tự nhiên nào khác). Tên được chuẩn hoá trước khi so
 *       ({@code 〜/～/~}, ngoặc toàn/nửa góc, khoảng trắng, hoa/thường). Override nghĩa/cấu trúc:
 *       ô trống = lấy từ điển, {@code -} = để trống trong bài.</li>
 *   <li>{@code ViDu} (tuỳ chọn) — câu ví dụ, nối theo {@code title} + {@code jlpt_level}.
 *       Điểm ngữ pháp không có câu ví dụ trong file sẽ tự chép ví dụ từ từ điển.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GrammarImportService {

    public static final String TEMPLATE_TYPE = "LESSON_GRAMMAR";
    public static final int TEMPLATE_VERSION = 1;
    static final String TEMPLATE_FILE_NAME = "mau_import_ngu_phap_v" + TEMPLATE_VERSION + ".xlsx";
    static final String ITEM_SHEET = "NguPhap";
    static final String EXAMPLE_SHEET = "ViDu";
    /** Sheet tra cứu: toàn bộ tên điểm ngữ pháp theo cấp, làm nguồn dropdown cho cột title. */
    static final String LOOKUP_SHEET = "DanhSachNguPhap";
    static final int MAX_SUGGESTIONS = 5;
    static final int MAX_ITEMS = 300;
    static final int MAX_EXAMPLES = 2000;
    /** Khớp MAX_EXAMPLES_PER_REQUEST của GrammarSectionItemService. */
    static final int MAX_EXAMPLES_PER_ITEM = 100;
    static final List<String> LEVELS = Arrays.stream(JlptLevel.values()).map(Enum::name).toList();

    // ----- Sheet NguPhap -----
    static final ImportColumn TITLE = ImportColumn.required("title", 24,
            "Tên điểm ngữ pháp — chọn từ danh sách hoặc gõ (〜, ～, ~ đều được). Vd: 〜てもいい");
    static final ImportColumn LEVEL = ImportColumn.required("jlpt_level", 12,
            "Cấp độ JLPT của điểm ngữ pháp (chọn N5..N1)").withAllowedValues(LEVELS);
    static final ImportColumn MEANING = ImportColumn.optional("custom_meaning", 30,
            "Nghĩa riêng cho bài. Trống = lấy từ điển, '-' = để trống");
    static final ImportColumn STRUCTURE = ImportColumn.optional("custom_structure", 30,
            "Cấu trúc riêng cho bài. Trống = lấy từ điển, '-' = để trống");
    static final ImportColumn NOTE = ImportColumn.optional("note", 30,
            "Ghi chú của giáo viên (tối đa 2000 ký tự)");
    static final List<ImportColumn> ITEM_COLUMNS = List.of(TITLE, LEVEL, MEANING, STRUCTURE, NOTE);

    // ----- Sheet ViDu -----
    static final ImportColumn EX_TITLE = ImportColumn.required("title", 24,
            "Tên điểm ngữ pháp — phải có trong sheet NguPhap");
    static final ImportColumn EX_LEVEL = ImportColumn.required("jlpt_level", 12,
            "Cấp độ JLPT giống dòng tương ứng ở sheet NguPhap").withAllowedValues(LEVELS);
    static final ImportColumn EX_JP = ImportColumn.required("example_jp", 40, "Câu ví dụ tiếng Nhật");
    static final ImportColumn EX_VI = ImportColumn.required("example_vi", 40, "Bản dịch tiếng Việt");
    static final List<ImportColumn> EXAMPLE_COLUMNS = List.of(EX_TITLE, EX_LEVEL, EX_JP, EX_VI);

    private final ExcelImportSupport excel;
    private final ImportSectionResolver sectionResolver;
    private final GrammarPointRepository grammarPointRepository;
    private final LessonGrammarItemRepository grammarItemRepository;
    private final GrammarSectionItemService grammarSectionItemService;

    // ============================================================
    // TEMPLATE
    // ============================================================

    @Transactional(readOnly = true)
    public ImportTemplateFile buildTemplate() {
        return new ImportTemplateFile(TEMPLATE_FILE_NAME, excel.toBytes(createWorkbook()));
    }

    /** Workbook mẫu (Hướng dẫn + _meta + các sheet dữ liệu trống) — dùng cho cả tải mẫu và xuất dữ liệu. */
    private XSSFWorkbook createWorkbook() {
        List<String> guide = new ArrayList<>(List.of(
                "Cách dùng:",
                "1. Điền mỗi điểm ngữ pháp 1 dòng ở sheet 'NguPhap'. Chọn tên ở dropdown cột title (lấy từ sheet '"
                        + LOOKUP_SHEET + "') hoặc gõ tay — 〜/～/~ và ngoặc ()/（） được coi là như nhau.",
                "2. (Tuỳ chọn) Điền câu ví dụ ở sheet 'ViDu'. Điểm ngữ pháp nào không có câu ví dụ sẽ tự lấy từ từ điển.",
                "3. Ô trống ở cột tuỳ chỉnh = lấy dữ liệu từ điển; nhập '-' = để trống trường đó trong bài.",
                "4. Không đổi tên sheet, tên cột hoặc thứ tự cột. Không xoá sheet ẩn '_meta'.",
                "5. Tối đa " + MAX_ITEMS + " điểm ngữ pháp, " + MAX_EXAMPLES_PER_ITEM
                        + " câu ví dụ mỗi điểm, file ≤ 2MB.",
                "6. Điểm ngữ pháp đã có trong section sẽ bị báo lỗi. Cột có dấu * là bắt buộc.",
                "",
                "Ví dụ — sheet NguPhap:  〜てもいい | N5 | | V て + もいい |",
                "Ví dụ — sheet ViDu:     〜てもいい | N5 | 写真を撮ってもいいですか。 | Tôi chụp ảnh được không?",
                ""));
        guide.addAll(ExcelImportSupport.describeColumns(ITEM_SHEET, ITEM_COLUMNS));
        guide.addAll(ExcelImportSupport.describeColumns(EXAMPLE_SHEET, EXAMPLE_COLUMNS));

        XSSFWorkbook wb = excel.newTemplate(TEMPLATE_TYPE, TEMPLATE_VERSION,
                "MẪU IMPORT NGỮ PHÁP VÀO BÀI HỌC (v" + TEMPLATE_VERSION + ")", guide);
        Sheet itemSheet = excel.addDataSheet(wb, ITEM_SHEET, ITEM_COLUMNS, MAX_ITEMS);
        Sheet exampleSheet = excel.addDataSheet(wb, EXAMPLE_SHEET, EXAMPLE_COLUMNS, MAX_EXAMPLES);
        int lookupRows = addLookupSheet(wb);
        if (lookupRows > 0) {
            String range = LOOKUP_SHEET + "!$A$2:$A$" + (lookupRows + 1);
            excel.addListValidation(itemSheet, ITEM_COLUMNS.indexOf(TITLE), range, MAX_ITEMS);
            excel.addListValidation(exampleSheet, EXAMPLE_COLUMNS.indexOf(EX_TITLE), range, MAX_EXAMPLES);
        }
        wb.setActiveSheet(wb.getSheetIndex(ITEM_SHEET));
        return wb;
    }

    // ============================================================
    // EXPORT — xuất dữ liệu section theo đúng mẫu import (import lại được sang section khác)
    // ============================================================

    /**
     * Xuất điểm ngữ pháp (+ câu ví dụ) của section ra file Excel cùng định dạng mẫu import.
     * Cột tuỳ chỉnh: null → để trống (lấy theo từ điển), "" → '-' (để trống trong bài).
     */
    @Transactional(readOnly = true)
    public ImportTemplateFile exportSection(Long sectionId) {
        LessonSection section = sectionResolver.requireEditable(sectionId, LessonSectionType.GRAMMAR);
        List<String[]> itemRows = new ArrayList<>();
        List<String[]> exampleRows = new ArrayList<>();
        for (LessonGrammarItem item : grammarItemRepository.findBySectionIdAndDeletedFalseOrderByOrderIndexAsc(sectionId)) {
            GrammarPoint g = item.getGrammar();
            if (g == null) continue;
            String level = g.getJlptLevel() != null ? g.getJlptLevel().name() : null;
            itemRows.add(new String[]{
                    g.getTitle(),
                    level,
                    ExcelImportSupport.fromOverride(item.getCustomMeaning()),
                    ExcelImportSupport.fromOverride(item.getCustomStructure()),
                    item.getNote()});
            item.getExamples().stream()
                    .filter(e -> !e.isDeleted())
                    .forEach(e -> exampleRows.add(new String[]{g.getTitle(), level, e.getExampleJp(), e.getExampleVi()}));
        }
        XSSFWorkbook wb = createWorkbook();
        excel.writeRows(wb.getSheet(ITEM_SHEET), itemRows);
        excel.writeRows(wb.getSheet(EXAMPLE_SHEET), exampleRows);
        log.info("[GRAMMAR-EXPORT] section id={} exported {} items, {} examples",
                sectionId, itemRows.size(), exampleRows.size());
        return new ImportTemplateFile(ExcelImportSupport.exportFileName("ngu_phap", section), excel.toBytes(wb));
    }

    /** Sheet tra cứu: title | jlpt_level | nghĩa, sắp theo cấp N5 → N1 rồi theo tên. Trả về số dòng. */
    private int addLookupSheet(XSSFWorkbook wb) {
        List<ImportColumn> columns = List.of(
                ImportColumn.optional("title", 30, "Tên điểm ngữ pháp trong từ điển"),
                ImportColumn.optional("jlpt_level", 12, "Cấp độ JLPT"),
                ImportColumn.optional("meaning_vi", 50, "Nghĩa tiếng Việt trong từ điển"));
        List<GrammarPoint> all = new ArrayList<>(grammarPointRepository.findByDeletedFalseOrderByIdAsc());
        all.sort(Comparator.comparingInt((GrammarPoint g) -> levelOrder(g.getJlptLevel()))
                .thenComparing(GrammarPoint::getTitle, Comparator.nullsLast(Comparator.naturalOrder())));
        Sheet sheet = excel.addDataSheet(wb, LOOKUP_SHEET, columns, 0);
        int r = 1;
        for (GrammarPoint g : all) {
            if (g.getTitle() == null) continue;
            Row row = sheet.createRow(r++);
            row.createCell(0).setCellValue(g.getTitle());
            row.createCell(1).setCellValue(g.getJlptLevel() == null ? "" : g.getJlptLevel().name());
            row.createCell(2).setCellValue(g.getMeaningVi() == null ? "" : g.getMeaningVi());
        }
        return r - 1;
    }

    /** N5 trước, N1 sau (theo độ khó giảm dần của số cấp). */
    private static int levelOrder(JlptLevel level) {
        if (level == null) return Integer.MAX_VALUE;
        return -Integer.parseInt(level.name().substring(1));
    }

    // ============================================================
    // PREVIEW / COMMIT
    // ============================================================

    @Transactional(readOnly = true)
    public SectionImportPreviewResponse preview(Long sectionId, MultipartFile file) {
        LessonSection section = sectionResolver.requireEditable(sectionId, LessonSectionType.GRAMMAR);
        return parse(section, file).preview();
    }

    @Transactional
    public SectionImportCommitResponse commit(Long sectionId, MultipartFile file) {
        LessonSection section = sectionResolver.requireEditable(sectionId, LessonSectionType.GRAMMAR);
        Parsed parsed = parse(section, file);
        if (!parsed.preview().isCanCommit()) {
            throw new AppException(ErrorCode.IMPORT_HAS_ERRORS);
        }
        int items = grammarSectionItemService.importGrammarItems(section, parsed.requests());
        log.info("[GRAMMAR-IMPORT] section id={} imported {} items, {} examples",
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
                          List<LessonGrammarItemCreateRequest> requests,
                          int childCount) {
    }

    private static final class Draft {
        final ImportRowResult row;
        final LessonGrammarItemCreateRequest request;
        final List<LessonGrammarExampleCreateRequest> examples = new ArrayList<>();
        /** Khoá nối câu ví dụ: tên đã chuẩn hoá + cấp. */
        final String key;

        Draft(ImportRowResult row, LessonGrammarItemCreateRequest request, String key) {
            this.row = row;
            this.request = request;
            this.key = key;
        }
    }

    /** Chỉ mục từ điển ngữ pháp theo tên đã chuẩn hoá (nạp 1 lần mỗi lần parse, ~500 dòng). */
    private static final class GrammarIndex {
        final Map<String, List<GrammarPoint>> byTitleLevel = new HashMap<>();
        final Map<String, List<GrammarPoint>> byTitle = new HashMap<>();
        final List<GrammarPoint> all;

        GrammarIndex(List<GrammarPoint> all) {
            this.all = all;
            for (GrammarPoint g : all) {
                if (g.getTitle() == null || g.getJlptLevel() == null) continue;
                String t = normalizeTitle(g.getTitle());
                byTitleLevel.computeIfAbsent(key(t, g.getJlptLevel().name()), k -> new ArrayList<>()).add(g);
                byTitle.computeIfAbsent(t, k -> new ArrayList<>()).add(g);
            }
        }
    }

    private Parsed parse(LessonSection section, MultipartFile file) {
        List<ImportRowResult> itemResults = new ArrayList<>();
        List<ImportRowResult> exampleResults = new ArrayList<>();
        Map<String, Draft> draftsByKey = new LinkedHashMap<>();
        Workbook wb = excel.open(file);
        try {
            excel.verifyMeta(wb, TEMPLATE_TYPE, TEMPLATE_VERSION);
            Sheet itemSheet = excel.requireSheet(wb, ITEM_SHEET, ITEM_COLUMNS, false);
            Sheet exampleSheet = excel.requireSheet(wb, EXAMPLE_SHEET, EXAMPLE_COLUMNS, true);
            List<Row> itemRows = excel.dataRows(itemSheet, ITEM_COLUMNS.size(), MAX_ITEMS);
            List<Row> exampleRows = excel.dataRows(exampleSheet, EXAMPLE_COLUMNS.size(), MAX_EXAMPLES);
            if (itemRows.isEmpty()) {
                throw new AppException(ErrorCode.IMPORT_NO_ROWS);
            }

            Set<Long> existingGrammarIds = grammarItemRepository
                    .findBySectionIdAndDeletedFalseOrderByOrderIndexAsc(section.getId()).stream()
                    .map(LessonGrammarItem::getGrammar)
                    .filter(Objects::nonNull)
                    .map(GrammarPoint::getId)
                    .collect(Collectors.toSet());
            Map<Long, Integer> seenGrammarRow = new HashMap<>();
            GrammarIndex index = new GrammarIndex(grammarPointRepository.findByDeletedFalseOrderByIdAsc());

            for (Row row : itemRows) {
                ImportRowResult result = excel.newResult(row, ITEM_COLUMNS);
                itemResults.add(result);
                Draft draft = parseItemRow(result, index, existingGrammarIds, seenGrammarRow);
                if (draft != null) {
                    draftsByKey.putIfAbsent(draft.key, draft);
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

        List<LessonGrammarItemCreateRequest> requests = new ArrayList<>();
        int childCount = 0;
        for (Draft d : draftsByKey.values()) {
            if (!d.examples.isEmpty()) {
                d.request.setExamples(d.examples);
                childCount += d.examples.size();
            }
            d.row.getData().put("exampleCount", d.examples.size());
            requests.add(d.request);
        }
        return new Parsed(ExcelImportSupport.summarize(LessonSectionType.GRAMMAR, itemResults, exampleResults),
                requests, childCount);
    }

    private Draft parseItemRow(ImportRowResult r, GrammarIndex index,
                               Set<Long> existingGrammarIds, Map<Long, Integer> seenGrammarRow) {
        String title = excel.text(r, TITLE, 255);
        JlptLevel level = parseLevel(r, LEVEL);
        String meaning = excel.text(r, MEANING, 5000);
        String structure = excel.text(r, STRUCTURE, 5000);
        String note = excel.text(r, NOTE, 2000);
        if (title == null || level == null) {
            return null;
        }

        String normalized = normalizeTitle(title);
        String draftKey = key(normalized, level.name());
        List<GrammarPoint> matches = index.byTitleLevel.getOrDefault(draftKey, List.of());
        if (matches.isEmpty()) {
            r.error(notFoundMessage(index, title, normalized, level));
            return null;
        }
        GrammarPoint grammar = matches.get(0);
        if (matches.size() > 1) {
            r.warn("Có " + matches.size() + " điểm ngữ pháp trùng tên '" + title + "' cấp " + level
                    + ", đã chọn bản đầu tiên");
        }
        r.getData().put("grammarId", grammar.getId());
        r.getData().put("dictTitle", grammar.getTitle());
        r.getData().put("dictMeaning", grammar.getMeaningVi());
        r.getData().put("dictStructure", grammar.getStructure());

        if (existingGrammarIds.contains(grammar.getId())) {
            r.error("Điểm ngữ pháp này đã có trong section");
        }
        Integer firstRow = seenGrammarRow.putIfAbsent(grammar.getId(), r.getRowNumber());
        if (firstRow != null) {
            r.error("Trùng với dòng " + firstRow + " của sheet " + ITEM_SHEET);
            return null;
        }
        LessonGrammarItemCreateRequest request = LessonGrammarItemCreateRequest.builder()
                .grammarId(grammar.getId())
                .customMeaning(ExcelImportSupport.toOverride(meaning))
                .customStructure(ExcelImportSupport.toOverride(structure))
                .note(note)
                .build();
        return new Draft(r, request, draftKey);
    }

    private void parseExampleRow(ImportRowResult r, Map<String, Draft> draftsByKey) {
        String title = excel.text(r, EX_TITLE, 255);
        JlptLevel level = parseLevel(r, EX_LEVEL);
        String jp = excel.text(r, EX_JP, 5000);
        String vi = excel.text(r, EX_VI, 5000);
        if (title == null || level == null) {
            return;
        }
        Draft draft = draftsByKey.get(key(normalizeTitle(title), level.name()));
        if (draft == null) {
            r.error("Không có ngữ pháp '" + title + "' cấp " + level + " hợp lệ trong sheet " + ITEM_SHEET);
            return;
        }
        r.getData().put("linkedRow", draft.row.getRowNumber());
        if (draft.examples.size() >= MAX_EXAMPLES_PER_ITEM) {
            r.error("Mỗi điểm ngữ pháp tối đa " + MAX_EXAMPLES_PER_ITEM + " câu ví dụ");
            return;
        }
        if (r.hasError()) {
            return;
        }
        draft.examples.add(LessonGrammarExampleCreateRequest.builder()
                .exampleJp(jp)
                .exampleVi(vi)
                .build());
    }

    private JlptLevel parseLevel(ImportRowResult r, ImportColumn column) {
        String raw = excel.text(r, column, 10);
        if (raw == null) {
            return null;
        }
        try {
            return JlptLevel.valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            r.error("Cột '" + column.key() + "' chỉ nhận " + String.join(", ", LEVELS));
            return null;
        }
    }

    /** Thông báo khi không khớp: chỉ ra nếu chỉ sai cấp, kèm tối đa vài tên gần đúng cùng cấp. */
    private static String notFoundMessage(GrammarIndex index, String title, String normalized, JlptLevel level) {
        List<GrammarPoint> otherLevels = index.byTitle.getOrDefault(normalized, List.of());
        if (!otherLevels.isEmpty()) {
            String levels = otherLevels.stream().map(g -> g.getJlptLevel().name()).distinct()
                    .collect(Collectors.joining(", "));
            return "Ngữ pháp '" + title + "' không có ở cấp " + level + " mà ở cấp " + levels;
        }
        String core = normalized.replace("~", "");
        List<String> suggestions = core.isEmpty() ? List.of() : index.all.stream()
                .filter(g -> g.getJlptLevel() == level && g.getTitle() != null)
                .filter(g -> normalizeTitle(g.getTitle()).replace("~", "").contains(core))
                .map(GrammarPoint::getTitle)
                .limit(MAX_SUGGESTIONS)
                .toList();
        String message = "Không tìm thấy ngữ pháp '" + title + "' cấp " + level + " trong từ điển";
        return suggestions.isEmpty() ? message : message + ". Có phải: " + String.join(" ; ", suggestions) + "?";
    }

    /**
     * Chuẩn hoá tên để so khớp: NFKC (ngoặc/chữ toàn góc → nửa góc, ～ → ~), mọi kiểu dấu sóng → '~',
     * bỏ khoảng trắng, chữ thường. Vd "〜に（time）", "～に(Time)", "~に (time)" cho cùng một khoá.
     */
    static String normalizeTitle(String title) {
        String n = Normalizer.normalize(title, Normalizer.Form.NFKC);
        n = n.replaceAll("[\\u301C\\u3030\\u223C\\uFF5E~]", "~");
        n = n.replaceAll("\\s+", "");
        return n.toLowerCase(Locale.ROOT);
    }

    private static String key(String normalizedTitle, String level) {
        return normalizedTitle + "\u0000" + (level == null ? "" : level.toUpperCase(Locale.ROOT));
    }
}
