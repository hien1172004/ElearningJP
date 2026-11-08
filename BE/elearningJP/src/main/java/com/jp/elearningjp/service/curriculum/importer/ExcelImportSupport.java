package com.jp.elearningjp.service.curriculum.importer;

import com.jp.elearningjp.dto.response.curriculum.importer.ImportRowResult;
import com.jp.elearningjp.dto.response.curriculum.importer.ImportRowStatus;
import com.jp.elearningjp.dto.response.curriculum.importer.SectionImportPreviewResponse;
import com.jp.elearningjp.entity.curriculum.LessonSection;
import com.jp.elearningjp.shared.enums.LessonSectionType;
import com.jp.elearningjp.shared.util.SlugUtils;
import com.jp.elearningjp.exception.AppException;
import com.jp.elearningjp.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Tiện ích Excel dùng chung cho 3 service import (Kanji / Vocabulary / Grammar).
 *
 * <p>Chỉ chứa phần kỹ thuật lặp lại: mở file an toàn, kiểm tra mẫu (sheet {@code _meta}
 * + dòng tiêu đề), đọc ô và sinh file mẫu. <b>Không chứa nghiệp vụ</b> — mỗi loại
 * section tự định nghĩa cột, kiểm tra và ghi dữ liệu trong service riêng.</p>
 */
@Slf4j
@Component
public class ExcelImportSupport {

    public static final long MAX_FILE_BYTES = 2L * 1024 * 1024;
    public static final String META_SHEET = "_meta";
    public static final String GUIDE_SHEET = "HuongDan";
    /** Ký hiệu trong ô override: để trống trường này trong bài (khác ô trống = lấy theo từ điển). */
    public static final String CLEAR_MARKER = "-";

    private static final DataFormatter FORMATTER = new DataFormatter(Locale.ROOT);

    static {
        // Chống "zip bomb": file .xlsx nén bất thường sẽ bị POI từ chối khi mở
        ZipSecureFile.setMinInflateRatio(0.01);
    }

    // ============================================================
    // ĐỌC FILE
    // ============================================================

    /** Mở file .xlsx do giáo viên upload, kiểm tra kích thước + phần mở rộng. Caller phải close. */
    public Workbook open(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new AppException(ErrorCode.IMPORT_FILE_EMPTY);
        }
        if (file.getSize() > MAX_FILE_BYTES) {
            throw new AppException(ErrorCode.IMPORT_FILE_TOO_LARGE);
        }
        String name = file.getOriginalFilename();
        if (name == null || !name.toLowerCase(Locale.ROOT).endsWith(".xlsx")) {
            throw new AppException(ErrorCode.IMPORT_FILE_INVALID);
        }
        Workbook wb;
        try (InputStream is = file.getInputStream()) {
            wb = WorkbookFactory.create(is);
        } catch (Exception e) {
            log.warn("[IMPORT] Không mở được file Excel '{}': {}", name, e.getMessage());
            throw new AppException(ErrorCode.IMPORT_FILE_INVALID);
        }
        if (!(wb instanceof XSSFWorkbook)) {
            closeQuietly(wb);
            throw new AppException(ErrorCode.IMPORT_FILE_INVALID);
        }
        return wb;
    }

    public void closeQuietly(Workbook wb) {
        try {
            wb.close();
        } catch (IOException ignored) {
            // file chỉ đọc trong bộ nhớ, không cần xử lý
        }
    }

    /** Kiểm tra sheet {@code _meta}: đúng loại mẫu + đúng version. */
    public void verifyMeta(Workbook wb, String expectedType, int expectedVersion) {
        Sheet meta = wb.getSheet(META_SHEET);
        if (meta == null) {
            throw new AppException(ErrorCode.IMPORT_TEMPLATE_INVALID);
        }
        String type = read(meta.getRow(0), 1);
        String version = read(meta.getRow(1), 1);
        if (type == null || version == null) {
            throw new AppException(ErrorCode.IMPORT_TEMPLATE_INVALID);
        }
        if (!expectedType.equals(type)) {
            throw new AppException(ErrorCode.IMPORT_TEMPLATE_TYPE_MISMATCH);
        }
        if (!String.valueOf(expectedVersion).equals(version)) {
            throw new AppException(ErrorCode.IMPORT_TEMPLATE_INVALID);
        }
    }

    /**
     * Lấy sheet dữ liệu và kiểm tra dòng tiêu đề khớp đúng thứ tự cột của mẫu.
     *
     * @param optional true nếu sheet phụ được phép thiếu (trả null)
     */
    public Sheet requireSheet(Workbook wb, String sheetName, List<ImportColumn> columns, boolean optional) {
        Sheet sheet = wb.getSheet(sheetName);
        if (sheet == null) {
            if (optional) return null;
            throw new AppException(ErrorCode.IMPORT_TEMPLATE_INVALID);
        }
        Row header = sheet.getRow(0);
        for (int c = 0; c < columns.size(); c++) {
            String actual = read(header, c);
            String normalized = actual == null ? null : actual.replace("*", "").trim();
            if (!columns.get(c).key().equalsIgnoreCase(normalized)) {
                throw new AppException(ErrorCode.IMPORT_TEMPLATE_INVALID);
            }
        }
        return sheet;
    }

    /** Các dòng dữ liệu (bỏ tiêu đề + dòng trống hoàn toàn). Ném lỗi nếu vượt {@code maxRows}. */
    public List<Row> dataRows(Sheet sheet, int columnCount, int maxRows) {
        List<Row> rows = new ArrayList<>();
        if (sheet == null) return rows;
        for (int r = 1; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (isBlank(row, columnCount)) continue;
            rows.add(row);
            if (rows.size() > maxRows) {
                throw new AppException(ErrorCode.IMPORT_TOO_MANY_ROWS);
            }
        }
        return rows;
    }

    /** Đọc ô dưới dạng chuỗi đã trim (ô số/ngày/công thức đọc theo giá trị hiển thị). Trống → null. */
    public String read(Row row, int col) {
        if (row == null) return null;
        Cell cell = row.getCell(col, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) return null;
        String value = cell.getCellType() == CellType.FORMULA
                ? FORMATTER.formatCellValue(cell,
                        row.getSheet().getWorkbook().getCreationHelper().createFormulaEvaluator())
                : FORMATTER.formatCellValue(cell);
        if (value == null) return null;
        value = value.strip();
        return value.isEmpty() ? null : value;
    }

    /** Tạo kết quả cho 1 dòng, sẵn giá trị các cột trong {@code data}. */
    public ImportRowResult newResult(Row row, List<ImportColumn> columns) {
        ImportRowResult result = ImportRowResult.builder()
                .sheet(row.getSheet().getSheetName())
                .rowNumber(row.getRowNum() + 1)
                .build();
        for (int c = 0; c < columns.size(); c++) {
            result.getData().put(columns.get(c).key(), read(row, c));
        }
        return result;
    }

    /** Lấy giá trị cột, kiểm tra bắt buộc + độ dài tối đa. Trống → null. */
    public String text(ImportRowResult result, ImportColumn column, int maxLength) {
        String value = (String) result.getData().get(column.key());
        if (value == null) {
            if (column.required()) {
                result.error("Cột '" + column.key() + "' không được để trống");
            }
            return null;
        }
        if (value.length() > maxLength) {
            result.error("Cột '" + column.key() + "' tối đa " + maxLength + " ký tự");
        }
        return value;
    }

    /** Kiểm tra URL http(s) (cho phép trống hoặc ký hiệu xoá). */
    public void checkUrl(ImportRowResult result, ImportColumn column, String value) {
        if (value == null || CLEAR_MARKER.equals(value)) return;
        String lower = value.toLowerCase(Locale.ROOT);
        if (!lower.startsWith("http://") && !lower.startsWith("https://")) {
            result.error("Cột '" + column.key() + "' phải là đường dẫn bắt đầu bằng http:// hoặc https://");
        }
    }

    /**
     * Chuyển giá trị ô override theo quy ước service item:
     * trống → null (chép giá trị từ điển), {@code -} → "" (để trống / ẩn trong bài), còn lại giữ nguyên.
     */
    public static String toOverride(String value) {
        if (value == null) return null;
        return CLEAR_MARKER.equals(value) ? "" : value;
    }

    private boolean isBlank(Row row, int columnCount) {
        if (row == null) return true;
        for (int c = 0; c < columnCount; c++) {
            if (read(row, c) != null) return false;
        }
        return true;
    }

    // ============================================================
    // SINH FILE MẪU
    // ============================================================

    /** Workbook mẫu mới, đã có sheet Hướng dẫn (đầu tiên) và sheet {@code _meta} (ẩn). */
    public XSSFWorkbook newTemplate(String type, int version, String title, List<String> guideLines) {
        XSSFWorkbook wb = new XSSFWorkbook();
        Sheet guide = wb.createSheet(GUIDE_SHEET);
        guide.setColumnWidth(0, 120 * 256);
        CellStyle titleStyle = wb.createCellStyle();
        Font bold = wb.createFont();
        bold.setBold(true);
        bold.setFontHeightInPoints((short) 14);
        titleStyle.setFont(bold);

        Cell titleCell = guide.createRow(0).createCell(0);
        titleCell.setCellValue(title);
        titleCell.setCellStyle(titleStyle);
        for (int i = 0; i < guideLines.size(); i++) {
            guide.createRow(i + 2).createCell(0).setCellValue(guideLines.get(i));
        }

        Sheet meta = wb.createSheet(META_SHEET);
        Row r0 = meta.createRow(0);
        r0.createCell(0).setCellValue("type");
        r0.createCell(1).setCellValue(type);
        Row r1 = meta.createRow(1);
        r1.createCell(0).setCellValue("version");
        r1.createCell(1).setCellValue(String.valueOf(version));
        wb.setSheetHidden(wb.getSheetIndex(meta), true);
        return wb;
    }

    /** Thêm sheet dữ liệu: dòng tiêu đề in đậm, cố định, có chú thích + dropdown nếu có. */
    public Sheet addDataSheet(XSSFWorkbook wb, String name, List<ImportColumn> columns, int maxRows) {
        Sheet sheet = wb.createSheet(name);
        CellStyle headerStyle = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        headerStyle.setFont(font);
        headerStyle.setFillForegroundColor(IndexedColors.DARK_TEAL.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        // Định dạng Text cho vùng dữ liệu: Excel không tự đổi chuỗi thành số/ngày
        CellStyle textStyle = wb.createCellStyle();
        textStyle.setDataFormat(wb.createDataFormat().getFormat("@"));

        Drawing<?> drawing = sheet.createDrawingPatriarch();
        CreationHelper helper = wb.getCreationHelper();
        DataValidationHelper dv = sheet.getDataValidationHelper();
        Row header = sheet.createRow(0);
        for (int c = 0; c < columns.size(); c++) {
            ImportColumn col = columns.get(c);
            Cell cell = header.createCell(c);
            cell.setCellValue(col.header());
            cell.setCellStyle(headerStyle);
            sheet.setColumnWidth(c, col.width() * 256);
            sheet.setDefaultColumnStyle(c, textStyle);

            ClientAnchor anchor = helper.createClientAnchor();
            anchor.setCol1(c);
            anchor.setCol2(c + 3);
            anchor.setRow1(0);
            anchor.setRow2(4);
            Comment comment = drawing.createCellComment(anchor);
            comment.setString(helper.createRichTextString(col.description()));
            cell.setCellComment(comment);

            if (col.allowedValues() != null) {
                DataValidation validation = dv.createValidation(
                        dv.createExplicitListConstraint(col.allowedValues().toArray(String[]::new)),
                        new CellRangeAddressList(1, maxRows, c, c));
                validation.setShowErrorBox(true);
                sheet.addValidationData(validation);
            }
        }
        sheet.createFreezePane(0, 1);
        return sheet;
    }

    /** Dropdown cho 1 cột dữ liệu lấy từ vùng ô (vd sheet tra cứu). Cho phép gõ giá trị khác (chỉ cảnh báo). */
    public void addListValidation(Sheet sheet, int column, String rangeFormula, int maxRows) {
        DataValidationHelper dv = sheet.getDataValidationHelper();
        DataValidation validation = dv.createValidation(
                dv.createFormulaListConstraint(rangeFormula),
                new CellRangeAddressList(1, maxRows, column, column));
        validation.setErrorStyle(DataValidation.ErrorStyle.WARNING);
        validation.setShowErrorBox(true);
        sheet.addValidationData(validation);
    }

    // ============================================================
    // XUẤT DỮ LIỆU (dùng đúng mẫu import để có thể import lại sang section khác)
    // ============================================================

    /** Ghi các dòng dữ liệu vào sheet, bắt đầu từ dòng 2 (dưới tiêu đề). Ô null bỏ trống. */
    public void writeRows(Sheet sheet, List<String[]> rows) {
        for (int r = 0; r < rows.size(); r++) {
            Row row = sheet.createRow(r + 1);
            String[] values = rows.get(r);
            for (int c = 0; c < values.length; c++) {
                if (values[c] != null && !values[c].isEmpty()) {
                    row.createCell(c).setCellValue(values[c]);
                }
            }
        }
    }

    /**
     * Ngược với {@link #toOverride}: null → ô trống (lấy theo từ điển),
     * "" → {@code -} (để trống trong bài), còn lại giữ nguyên.
     */
    public static String fromOverride(String value) {
        if (value == null) return null;
        return value.isEmpty() ? CLEAR_MARKER : value;
    }

    /** Tên file xuất: {@code <prefix>_<bài học>_<mục>.xlsx} (không dấu). */
    public static String exportFileName(String prefix, LessonSection section) {
        String lesson = section.getLesson() != null ? SlugUtils.toSlug(section.getLesson().getTitle()) : "";
        String name = prefix
                + (lesson.isEmpty() ? "" : "_" + lesson)
                + "_" + (SlugUtils.toSlug(section.getTitle()).isEmpty() ? "muc-" + section.getId()
                        : SlugUtils.toSlug(section.getTitle()));
        return (name.length() > 120 ? name.substring(0, 120) : name) + ".xlsx";
    }

    public byte[] toBytes(Workbook wb) {
        try (wb; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Không sinh được file mẫu Excel", e);
        }
    }

    /** Các dòng mô tả cột của 1 sheet, dùng cho sheet Hướng dẫn. */
    public static List<String> describeColumns(String sheetName, List<ImportColumn> columns) {
        List<String> lines = new ArrayList<>();
        lines.add("Sheet '" + sheetName + "':");
        for (ImportColumn col : columns) {
            lines.add("   • " + col.header() + " — " + col.description());
        }
        lines.add("");
        return lines;
    }

    // ============================================================
    // TỔNG HỢP KẾT QUẢ
    // ============================================================

    /** Đếm dòng lỗi/cảnh báo của cả 2 sheet; chỉ cho ghi khi không còn dòng lỗi. */
    public static SectionImportPreviewResponse summarize(LessonSectionType type,
                                                        List<ImportRowResult> itemRows,
                                                        List<ImportRowResult> childRows) {
        int errors = 0;
        int warnings = 0;
        for (List<ImportRowResult> rows : List.of(itemRows, childRows)) {
            for (ImportRowResult r : rows) {
                if (r.getStatus() == ImportRowStatus.ERROR) errors++;
                else if (r.getStatus() == ImportRowStatus.WARNING) warnings++;
            }
        }
        return SectionImportPreviewResponse.builder()
                .sectionType(type)
                .totalItems(itemRows.size())
                .totalChildren(childRows.size())
                .errorRows(errors)
                .warningRows(warnings)
                .canCommit(errors == 0 && !itemRows.isEmpty())
                .itemRows(itemRows)
                .childRows(childRows)
                .build();
    }
}
