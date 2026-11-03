package com.jp.elearningjp.service.notebook.impl;

import com.jp.elearningjp.dto.response.notebook.KanjiPdfMetaResponse;
import com.jp.elearningjp.dto.response.notebook.KanjiPdfPreviewResponse;
import com.jp.elearningjp.entity.content.CharacterStroke;
import com.jp.elearningjp.entity.content.WritingCharacter;
import com.jp.elearningjp.entity.notebook.Notebook;
import com.jp.elearningjp.entity.notebook.NotebookWord;
import com.jp.elearningjp.exception.AppException;
import com.jp.elearningjp.exception.ErrorCode;
import com.jp.elearningjp.repository.content.CharacterStrokeRepository;
import com.jp.elearningjp.repository.notebook.NotebookRepository;
import com.jp.elearningjp.repository.notebook.NotebookWordRepository;
import com.jp.elearningjp.service.notebook.NotebookExportService;
import com.jp.elearningjp.shared.util.CurrentUserService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class NotebookExportServiceImpl implements NotebookExportService {

    private static final Pattern SVG_CMD_PATTERN = Pattern.compile("([MmLlHhVvCcSsQqTtAaZz])([^MmLlHhVvCcSsQqTtAaZz]*)");
    private static final Pattern SVG_NUM_PATTERN = Pattern.compile("[-+]?(?:\\d*\\.\\d+|\\d+)");
    private static final Pattern DIACRITICAL_MARKS = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
    private static final int STROKE_COLS_PER_ROW = 14;

    NotebookRepository notebookRepository;
    NotebookWordRepository notebookWordRepository;
    CharacterStrokeRepository characterStrokeRepository;
    CurrentUserService currentUserService;

    @Override
    @Transactional(readOnly = true)
    public byte[] exportWordsToExcel(Long notebookId) {
        Long currentUserId = currentUserService.getCurrentUserId();
        Notebook notebook = notebookRepository.findByIdAndUserIdAndDeletedFalse(notebookId, currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.NOTEBOOK_NOT_FOUND));

        List<NotebookWord> words = notebookWordRepository.findByNotebookIdAndDeletedFalseOrderByDisplayOrderAscIdAsc(notebookId);

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(notebook.getName() != null ? notebook.getName() : "Words");

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.ROYAL_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            Row headerRow = sheet.createRow(0);
            headerRow.setHeightInPoints(24);
            String[] headers = {
                    "STT", "Từ vựng (Word)", "Phiên âm (Reading)", "Âm Hán Việt",
                    "Nghĩa (Meaning)", "Ví dụ tiếng Nhật", "Nghĩa câu ví dụ", "Ghi chú", "Mức độ ghi nhớ"
            };

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            dataStyle.setWrapText(true);

            int rowIdx = 1;
            for (NotebookWord w : words) {
                Row row = sheet.createRow(rowIdx);
                row.setHeightInPoints(20);

                row.createCell(0).setCellValue(rowIdx);
                row.createCell(1).setCellValue(w.getWord() != null ? w.getWord() : "");
                row.createCell(2).setCellValue(w.getReading() != null ? w.getReading() : "");
                row.createCell(3).setCellValue(w.getSinoVietnamese() != null ? w.getSinoVietnamese() : "");
                row.createCell(4).setCellValue(w.getMeaning() != null ? w.getMeaning() : "");
                row.createCell(5).setCellValue(w.getExampleSentence() != null ? w.getExampleSentence() : "");
                row.createCell(6).setCellValue(w.getExampleMeaning() != null ? w.getExampleMeaning() : "");
                row.createCell(7).setCellValue(w.getUserNote() != null ? w.getUserNote() : "");
                row.createCell(8).setCellValue(w.getMemoryLevel() != null ? w.getMemoryLevel().name() : "");

                for (int c = 0; c < headers.length; c++) {
                    row.getCell(c).setCellStyle(dataStyle);
                }
                rowIdx++;
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
                int currentWidth = sheet.getColumnWidth(i);
                sheet.setColumnWidth(i, Math.min(Math.max(currentWidth + 1000, 3000), 12000));
            }

            workbook.write(baos);
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Error exporting notebook words to Excel: ", e);
            throw new RuntimeException("Lỗi khi xuất danh sách từ vựng ra Excel", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] generateKanjiPracticePdf(Long notebookId) {
        return generatePdfInternal(notebookId);
    }

    @Override
    @Transactional(readOnly = true)
    public KanjiPdfPreviewResponse generateKanjiPracticePdfPreview(Long notebookId, Integer pageNumber) {
        byte[] pdfBytes = generatePdfInternal(notebookId);
        try (PDDocument document = Loader.loadPDF(pdfBytes);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            int totalPages = document.getNumberOfPages();
            if (totalPages == 0) {
                PDPage emptyPage = new PDPage(PDRectangle.A4);
                document.addPage(emptyPage);
                totalPages = 1;
            }

            int reqPage = (pageNumber != null && pageNumber > 0) ? pageNumber : 1;
            int pageIndex = Math.max(0, Math.min(reqPage - 1, totalPages - 1));

            PDFRenderer renderer = new PDFRenderer(document);
            BufferedImage image = renderer.renderImageWithDPI(pageIndex, 180);
            ImageIO.write(image, "PNG", baos);

            return KanjiPdfPreviewResponse.builder()
                    .imageBytes(baos.toByteArray())
                    .currentPage(pageIndex + 1)
                    .totalPages(totalPages)
                    .build();
        } catch (Exception e) {
            log.error("Error generating PDF preview image: ", e);
            throw new RuntimeException("Lỗi khi tạo ảnh xem trước PDF", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public KanjiPdfMetaResponse getKanjiPracticePdfMeta(Long notebookId) {
        byte[] pdfBytes = generatePdfInternal(notebookId);
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            return KanjiPdfMetaResponse.builder()
                    .totalPages(Math.max(1, document.getNumberOfPages()))
                    .build();
        } catch (Exception e) {
            log.error("Error reading PDF metadata: ", e);
            return KanjiPdfMetaResponse.builder().totalPages(1).build();
        }
    }

    /**
     * Mô hình khối luyện viết cho 1 từ vựng (chuẩn Mazii):
     * Gồm:
     * - Hàng 1..N: Thứ tự từng nét bút thuận cho từng chữ Hán trong từ (strokeKanjis)
     * - Hàng N+1: Ô cách đọc (trắng) + Lặp lại toàn bộ từ vựng bằng chữ mờ (wordKanjis)
     * - Hàng N+2: 15 ô chữ Mễ trống tự luyện
     * - Hàng N+3: 15 ô chữ Mễ trống tự luyện
     */
    private static class KanjiWordPracticeBlock {
        String word;
        String reading;
        List<String> strokeKanjis; // Các chữ Hán độc nhất để viết bút thuận: [訪, 日]
        List<String> wordKanjis;   // Chuỗi chữ Hán của từ: [訪, 日]
        Map<String, List<CharacterStroke>> strokeMap = new LinkedHashMap<>();

        int getTotalRows() {
            int strokeRows = 0;
            for (String k : strokeKanjis) {
                List<CharacterStroke> s = strokeMap.get(k);
                int count = (s != null) ? s.size() : 0;
                int r = Math.max(1, (int) Math.ceil((double) count / STROKE_COLS_PER_ROW));
                strokeRows += r;
            }
            return strokeRows + 1 + 2;
        }
    }

    private byte[] generatePdfInternal(Long notebookId) {
        Long currentUserId = currentUserService.getCurrentUserId();
        Notebook notebook = notebookRepository.findByIdAndUserIdAndDeletedFalse(notebookId, currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.NOTEBOOK_NOT_FOUND));

        List<NotebookWord> words = notebookWordRepository.findByNotebookIdAndDeletedFalseOrderByDisplayOrderAscIdAsc(notebookId);

        // Gom các từ vựng thành các khối luyện viết theo chuẩn Mazii
        List<KanjiWordPracticeBlock> blocks = new ArrayList<>();
        for (NotebookWord w : words) {
            String wordStr = w.getWord() != null ? w.getWord().trim() : "";
            if (wordStr.isEmpty()) continue;

            List<String> kanjis = extractKanji(wordStr);
            if (kanjis.isEmpty()) continue;

            KanjiWordPracticeBlock block = new KanjiWordPracticeBlock();
            block.word = wordStr;
            block.reading = w.getReading() != null && !w.getReading().isEmpty() ? w.getReading() : wordStr;
            block.strokeKanjis = new ArrayList<>(new LinkedHashSet<>(kanjis));
            block.wordKanjis = kanjis;
            for (String k : block.strokeKanjis) {
                block.strokeMap.put(k, characterStrokeRepository.findByCharacterString(k));
            }
            blocks.add(block);
        }

        // Fallback nếu toàn bộ sổ tay không có chữ Hán nào (ví dụ toàn Kana N5)
        if (blocks.isEmpty() && !words.isEmpty()) {
            for (NotebookWord w : words) {
                String wordStr = w.getWord() != null ? w.getWord().trim() : "";
                if (wordStr.isEmpty()) continue;

                List<String> chars = new ArrayList<>();
                for (int i = 0; i < wordStr.length() && i < 4; i++) {
                    chars.add(String.valueOf(wordStr.charAt(i)));
                }

                KanjiWordPracticeBlock block = new KanjiWordPracticeBlock();
                block.word = wordStr;
                block.reading = w.getReading() != null && !w.getReading().isEmpty() ? w.getReading() : wordStr;
                block.strokeKanjis = new ArrayList<>(new LinkedHashSet<>(chars));
                block.wordKanjis = chars;
                for (String k : block.strokeKanjis) {
                    block.strokeMap.put(k, characterStrokeRepository.findByCharacterString(k));
                }
                blocks.add(block);
            }
        }

        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            PDType0Font font = loadJapaneseFont(document);

            // A4 Portrait chuẩn Mazii: 595.28 x 841.89 pt
            float pageWidth = PDRectangle.A4.getWidth();
            float pageHeight = PDRectangle.A4.getHeight();
            PDRectangle portrait = new PDRectangle(pageWidth, pageHeight);

            float marginX = 25f;
            float availableWidth = pageWidth - 2 * marginX; // 545.28 pt
            int cols = 15;
            float cellSize = availableWidth / cols; // 36.35 pt (~12.8mm chuẩn ô viết Nhật Bản)

            float topMargin = 42f;
            float bottomMargin = 40f;
            float blockGap = 14f;
            float availableHeight = pageHeight - topMargin - bottomMargin; // 759.89 pt

            // Phân trang động dựa theo chiều cao thực tế của từng khối từ vựng
            List<List<KanjiWordPracticeBlock>> pages = new ArrayList<>();
            List<KanjiWordPracticeBlock> currentPageBlocks = new ArrayList<>();
            float currentHeight = 0;

            for (KanjiWordPracticeBlock b : blocks) {
                float blockHeight = b.getTotalRows() * cellSize;
                float addedHeight = (currentPageBlocks.isEmpty() ? 0 : blockGap) + blockHeight;

                if (!currentPageBlocks.isEmpty() && currentHeight + addedHeight > availableHeight) {
                    pages.add(currentPageBlocks);
                    currentPageBlocks = new ArrayList<>();
                    currentHeight = blockHeight;
                    currentPageBlocks.add(b);
                } else {
                    currentPageBlocks.add(b);
                    currentHeight += addedHeight;
                }
            }
            if (!currentPageBlocks.isEmpty()) {
                pages.add(currentPageBlocks);
            }
            if (pages.isEmpty()) {
                pages.add(Collections.emptyList());
            }

            int totalPages = pages.size();
            for (int p = 0; p < totalPages; p++) {
                PDPage page = new PDPage(portrait);
                document.addPage(page);

                List<KanjiWordPracticeBlock> pageBlocks = pages.get(p);
                try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                    // Header nhận diện thương hiệu ElearningJP
                    renderPageHeader(cs, font, notebook.getName(), p + 1, totalPages, marginX, pageWidth, pageHeight);

                    // Nội dung các khối luyện viết
                    float currentY = pageHeight - topMargin;
                    for (KanjiWordPracticeBlock b : pageBlocks) {
                        currentY = renderWordPracticeBlock(cs, font, b, marginX, currentY, cellSize, cols);
                        currentY -= blockGap;
                    }

                    // Footer nhận diện thương hiệu ElearningJP
                    renderPageFooter(cs, font, p + 1, totalPages, marginX, pageWidth);
                }
            }

            document.save(baos);
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Error generating PDF: ", e);
            throw new RuntimeException("Lỗi khi tạo file PDF luyện viết Kanji", e);
        }
    }

    /**
     * Vẽ 1 khối luyện viết từ vựng chuẩn Mazii:
     * - Hàng 1..N: Các hàng thứ tự nét của từng chữ Hán (ô 0: chữ mẫu, ô 1..14: nét tăng dần & chữ mờ)
     * - Hàng N+1: Ô cách đọc (ô trắng tinh, bẻ dòng gọn gàng) + Lặp lại từ vựng mờ cách nhau 1 ô trống
     * - Hàng N+2: 15 ô chữ Mễ trống
     * - Hàng N+3: 15 ô chữ Mễ trống
     */
    private float renderWordPracticeBlock(PDPageContentStream cs, PDType0Font font, KanjiWordPracticeBlock block,
                                          float startX, float blockTop, float cellSize, int cols) throws Exception {
        float currentY = blockTop;
        Color faintGray = new Color(195, 195, 195);

        // 1. CÁC HÀNG BÚT THUẬN TỪNG CHỮ HÁN (Tự động xuống hàng nếu chữ nhiều hơn 14 nét)
        for (String kanji : block.strokeKanjis) {
            List<CharacterStroke> strokes = block.strokeMap.getOrDefault(kanji, Collections.emptyList());
            int numStrokes = strokes.size();
            int neededRows = Math.max(1, (int) Math.ceil((double) numStrokes / (cols - 1)));

            for (int r = 0; r < neededRows; r++) {
                float rowY = currentY - cellSize;
                int strokeOffset = r * (cols - 1);

                // Ô 0: Chữ mẫu in đậm rõ ràng
                drawMiGrid(cs, startX, rowY, cellSize);
                drawTextCentered(cs, font, kanji, startX, rowY, cellSize, 22f, Color.BLACK);

                // Ô 1..14: Các nét tiếp theo và chữ mờ nhạt để học viên đồ theo
                for (int c = 1; c < cols; c++) {
                    float x = startX + c * cellSize;
                    drawMiGrid(cs, x, rowY, cellSize);
                    int currentStrokeNum = strokeOffset + c;

                    if (currentStrokeNum <= numStrokes) {
                        drawAccumulatedStrokes(cs, strokes, currentStrokeNum, x, rowY, cellSize, faintGray);
                    } else {
                        // Sau khi đã hoàn thành toàn bộ các nét của chữ, các ô còn lại vẽ chữ mờ để đồ theo
                        drawTextCentered(cs, font, kanji, x, rowY, cellSize, 22f, faintGray);
                    }
                }
                currentY = rowY;
            }
        }

        // 2. HÀNG LUYỆN VIẾT CẢ TỪ VỰNG KÈM Ô CÁCH ĐỌC
        float wordRowY = currentY - cellSize;
        // Ô 0: Ô trắng ghi phiên âm cách đọc
        drawPlainBox(cs, startX, wordRowY, cellSize);
        drawReadingTextCentered(cs, font, block.reading, startX, wordRowY, cellSize, Color.DARK_GRAY);

        // Ô 1..14: Lặp lại cả từ vựng bằng chữ mờ, cách nhau 1 ô trống chuẩn Mazii
        List<String> wKanjis = block.wordKanjis;
        int L = Math.max(1, wKanjis.size());
        int patternLen = L + 1; // [K1, K2, ..., KL, BLANK]
        for (int c = 1; c < cols; c++) {
            float x = startX + c * cellSize;
            drawMiGrid(cs, x, wordRowY, cellSize);
            int pos = (c - 1) % patternLen;
            if (pos < L) {
                String kj = wKanjis.get(pos);
                drawTextCentered(cs, font, kj, x, wordRowY, cellSize, 22f, faintGray);
            }
            // pos == L: để trống ô chữ Mễ
        }
        currentY = wordRowY;

        // 3. HÀNG TỰ LUYỆN 1 (15 ô chữ Mễ trống)
        float blankRow1Y = currentY - cellSize;
        for (int c = 0; c < cols; c++) {
            drawMiGrid(cs, startX + c * cellSize, blankRow1Y, cellSize);
        }
        currentY = blankRow1Y;

        // 4. HÀNG TỰ LUYỆN 2 (15 ô chữ Mễ trống)
        float blankRow2Y = currentY - cellSize;
        for (int c = 0; c < cols; c++) {
            drawMiGrid(cs, startX + c * cellSize, blankRow2Y, cellSize);
        }
        currentY = blankRow2Y;

        return currentY;
    }

    /**
     * Ô chữ Mễ với viền ngoài đậm 0.9f và các đường nét đứt bên trong mảnh mờ 0.45f
     */
    private void drawMiGrid(PDPageContentStream cs, float x, float y, float size) throws Exception {
        cs.saveGraphicsState();

        // 1. Viền ngoài ô chữ Mễ (đậm 0.9f)
        cs.setStrokingColor(new Color(130, 130, 130));
        cs.setLineWidth(0.9f);
        cs.addRect(x, y, size, size);
        cs.stroke();

        // 2. Đường kẻ chữ Mễ nét đứt mảnh nhạt (0.45f)
        cs.setStrokingColor(new Color(225, 225, 225));
        cs.setLineWidth(0.45f);
        cs.setLineDashPattern(new float[]{2f, 2f}, 0);

        // Đường chéo 1: góc dưới trái -> trên phải
        cs.moveTo(x, y);
        cs.lineTo(x + size, y + size);
        cs.stroke();

        // Đường chéo 2: góc trên trái -> dưới phải
        cs.moveTo(x, y + size);
        cs.lineTo(x + size, y);
        cs.stroke();

        // Đường đứng giữa
        cs.moveTo(x + size / 2, y);
        cs.lineTo(x + size / 2, y + size);
        cs.stroke();

        // Đường ngang giữa
        cs.moveTo(x, y + size / 2);
        cs.lineTo(x + size, y + size / 2);
        cs.stroke();

        cs.restoreGraphicsState();
    }

    /**
     * Ô trắng tinh tế chỉ có viền ngoài, KHÔNG CÓ nét chữ Mễ bên trong
     */
    private void drawPlainBox(PDPageContentStream cs, float x, float y, float size) throws Exception {
        cs.saveGraphicsState();
        cs.setNonStrokingColor(Color.WHITE);
        cs.addRect(x, y, size, size);
        cs.fill();

        // Viền ngoài đậm 0.9f
        cs.setStrokingColor(new Color(130, 130, 130));
        cs.setLineWidth(0.9f);
        cs.addRect(x, y, size, size);
        cs.stroke();
        cs.restoreGraphicsState();
    }

    private void drawTextCentered(PDPageContentStream cs, PDType0Font font, String text, float x, float y, float size, float initialFontSize, Color color) throws Exception {
        if (text == null || text.isEmpty()) return;
        String safeText = sanitizeForFont(font, text);
        if (safeText.isEmpty()) return;

        float fontSize = initialFontSize;
        float textWidth = font.getStringWidth(safeText) / 1000 * fontSize;
        float maxWidth = size - 3f;
        if (textWidth > maxWidth && maxWidth > 0) {
            fontSize = fontSize * (maxWidth / textWidth);
            textWidth = font.getStringWidth(safeText) / 1000 * fontSize;
        }

        float textHeight = fontSize * 0.75f;
        float offsetX = x + (size - textWidth) / 2;
        float offsetY = y + (size - textHeight) / 2;

        cs.saveGraphicsState();
        cs.setNonStrokingColor(color);
        cs.beginText();
        cs.setFont(font, fontSize);
        cs.newLineAtOffset(offsetX, offsetY);
        cs.showText(safeText);
        cs.endText();
        cs.restoreGraphicsState();
    }

    /**
     * Vẽ text cách đọc nhiều dòng căn giữa chuẩn Mazii, đảm bảo luôn nằm gọn gàng bên trong ô
     */
    private void drawReadingTextCentered(PDPageContentStream cs, PDType0Font font, String text,
                                         float x, float y, float size, Color color) throws Exception {
        if (text == null || text.isEmpty()) return;
        String safeText = sanitizeForFont(font, text.trim());
        if (safeText.isEmpty()) return;

        int len = safeText.length();
        int numLines;
        if (len <= 4) {
            numLines = 1;
        } else if (len <= 7) {
            numLines = 2;
        } else if (len <= 11) {
            numLines = 3;
        } else {
            numLines = 4;
        }

        int charsPerLine = (int) Math.ceil((double) len / numLines);
        List<String> lines = new ArrayList<>();
        for (int i = 0; i < len; i += charsPerLine) {
            lines.add(safeText.substring(i, Math.min(i + charsPerLine, len)));
        }

        numLines = lines.size();
        float fontSize = 7.5f;
        if (numLines == 2) fontSize = 7.0f;
        else if (numLines == 3) fontSize = 6.2f;
        else if (numLines >= 4) fontSize = 5.5f;

        float maxWidth = size - 4f; // 2pt padding lề trái và phải
        // Tự động thu nhỏ cỡ chữ nếu bất kỳ dòng nào dài hơn chiều rộng an toàn của ô
        for (String line : lines) {
            float lw = font.getStringWidth(line) / 1000f * fontSize;
            if (lw > maxWidth && maxWidth > 0) {
                fontSize = fontSize * (maxWidth / lw);
            }
        }

        float lineHeight = fontSize * 1.2f;
        float totalBlockHeight = numLines * lineHeight;
        float startY = y + (size - totalBlockHeight) / 2 + totalBlockHeight - lineHeight;

        cs.saveGraphicsState();
        cs.setNonStrokingColor(color);

        for (int i = 0; i < numLines; i++) {
            String line = lines.get(i);
            float lineWidth = font.getStringWidth(line) / 1000f * fontSize;
            float lineX = Math.max(x + 2f, x + (size - lineWidth) / 2);
            float lineY = startY - i * lineHeight;

            cs.beginText();
            cs.setFont(font, fontSize);
            cs.newLineAtOffset(lineX, lineY);
            cs.showText(line);
            cs.endText();
        }

        cs.restoreGraphicsState();
    }

    private void drawAccumulatedStrokes(PDPageContentStream cs, List<CharacterStroke> strokes, int count, float x, float y, float size, Color color) throws Exception {
        float scale = (size - 6) / 109f;
        cs.saveGraphicsState();
        cs.setStrokingColor(color);
        cs.setLineWidth(1.1f);
        cs.setLineCapStyle(1);
        cs.setLineJoinStyle(1);

        for (int i = 0; i < count && i < strokes.size(); i++) {
            String pathData = strokes.get(i).getSvgPathData();
            if (pathData != null && !pathData.isEmpty()) {
                drawSvgPath(cs, pathData, x + 3, y + 3, size - 6, scale);
            }
        }

        cs.restoreGraphicsState();
    }

    private void drawSvgPath(PDPageContentStream cs, String pathData, float x, float y, float height, float scale) throws Exception {
        try {
            Path2D path = parseSvgPath(pathData);
            PathIterator pi = path.getPathIterator(null);
            float[] coords = new float[6];

            while (!pi.isDone()) {
                int type = pi.currentSegment(coords);
                switch (type) {
                    case PathIterator.SEG_MOVETO:
                        cs.moveTo(x + coords[0] * scale, y + height - (coords[1] * scale));
                        break;
                    case PathIterator.SEG_LINETO:
                        cs.lineTo(x + coords[0] * scale, y + height - (coords[1] * scale));
                        break;
                    case PathIterator.SEG_QUADTO:
                        cs.curveTo(
                                x + coords[0] * scale, y + height - (coords[1] * scale),
                                x + coords[0] * scale, y + height - (coords[1] * scale),
                                x + coords[2] * scale, y + height - (coords[3] * scale)
                        );
                        break;
                    case PathIterator.SEG_CUBICTO:
                        cs.curveTo(
                                x + coords[0] * scale, y + height - (coords[1] * scale),
                                x + coords[2] * scale, y + height - (coords[3] * scale),
                                x + coords[4] * scale, y + height - (coords[5] * scale)
                        );
                        break;
                    case PathIterator.SEG_CLOSE:
                        cs.closePath();
                        break;
                }
                pi.next();
            }
            cs.stroke();
        } catch (Exception ignored) {
        }
    }

    private Path2D parseSvgPath(String d) {
        Path2D path = new Path2D.Float();
        if (d == null || d.trim().isEmpty()) return path;

        Matcher cmdMatcher = SVG_CMD_PATTERN.matcher(d);
        float currentX = 0, currentY = 0;

        while (cmdMatcher.find()) {
            char cmd = cmdMatcher.group(1).charAt(0);
            String rest = cmdMatcher.group(2);

            Matcher numMatcher = SVG_NUM_PATTERN.matcher(rest);
            List<Float> nums = new ArrayList<>();
            while (numMatcher.find()) {
                try {
                    nums.add(Float.parseFloat(numMatcher.group()));
                } catch (Exception ignored) {
                }
            }

            try {
                if (cmd == 'M' && nums.size() >= 2) {
                    currentX = nums.get(0);
                    currentY = nums.get(1);
                    path.moveTo(currentX, currentY);
                    for (int i = 2; i + 1 < nums.size(); i += 2) {
                        currentX = nums.get(i);
                        currentY = nums.get(i + 1);
                        path.lineTo(currentX, currentY);
                    }
                } else if (cmd == 'm' && nums.size() >= 2) {
                    currentX += nums.get(0);
                    currentY += nums.get(1);
                    path.moveTo(currentX, currentY);
                    for (int i = 2; i + 1 < nums.size(); i += 2) {
                        currentX += nums.get(i);
                        currentY += nums.get(i + 1);
                        path.lineTo(currentX, currentY);
                    }
                } else if (cmd == 'L' && nums.size() >= 2) {
                    for (int i = 0; i + 1 < nums.size(); i += 2) {
                        currentX = nums.get(i);
                        currentY = nums.get(i + 1);
                        path.lineTo(currentX, currentY);
                    }
                } else if (cmd == 'l' && nums.size() >= 2) {
                    for (int i = 0; i + 1 < nums.size(); i += 2) {
                        currentX += nums.get(i);
                        currentY += nums.get(i + 1);
                        path.lineTo(currentX, currentY);
                    }
                } else if (cmd == 'H') {
                    for (float nx : nums) {
                        currentX = nx;
                        path.lineTo(currentX, currentY);
                    }
                } else if (cmd == 'h') {
                    for (float nx : nums) {
                        currentX += nx;
                        path.lineTo(currentX, currentY);
                    }
                } else if (cmd == 'V') {
                    for (float ny : nums) {
                        currentY = ny;
                        path.lineTo(currentX, currentY);
                    }
                } else if (cmd == 'v') {
                    for (float ny : nums) {
                        currentY += ny;
                        path.lineTo(currentX, currentY);
                    }
                } else if (cmd == 'c' && nums.size() >= 6) {
                    for (int i = 0; i + 5 < nums.size(); i += 6) {
                        float c1x = currentX + nums.get(i);
                        float c1y = currentY + nums.get(i + 1);
                        float c2x = currentX + nums.get(i + 2);
                        float c2y = currentY + nums.get(i + 3);
                        float ex = currentX + nums.get(i + 4);
                        float ey = currentY + nums.get(i + 5);
                        path.curveTo(c1x, c1y, c2x, c2y, ex, ey);
                        currentX = ex;
                        currentY = ey;
                    }
                } else if (cmd == 'C' && nums.size() >= 6) {
                    for (int i = 0; i + 5 < nums.size(); i += 6) {
                        float c1x = nums.get(i);
                        float c1y = nums.get(i + 1);
                        float c2x = nums.get(i + 2);
                        float c2y = nums.get(i + 3);
                        float ex = nums.get(i + 4);
                        float ey = nums.get(i + 5);
                        path.curveTo(c1x, c1y, c2x, c2y, ex, ey);
                        currentX = ex;
                        currentY = ey;
                    }
                } else if (cmd == 's' && nums.size() >= 4) {
                    for (int i = 0; i + 3 < nums.size(); i += 4) {
                        float c2x = currentX + nums.get(i);
                        float c2y = currentY + nums.get(i + 1);
                        float ex = currentX + nums.get(i + 2);
                        float ey = currentY + nums.get(i + 3);
                        path.curveTo(currentX, currentY, c2x, c2y, ex, ey);
                        currentX = ex;
                        currentY = ey;
                    }
                } else if (cmd == 'S' && nums.size() >= 4) {
                    for (int i = 0; i + 3 < nums.size(); i += 4) {
                        float c2x = nums.get(i);
                        float c2y = nums.get(i + 1);
                        float ex = nums.get(i + 2);
                        float ey = nums.get(i + 3);
                        path.curveTo(currentX, currentY, c2x, c2y, ex, ey);
                        currentX = ex;
                        currentY = ey;
                    }
                } else if (cmd == 'z' || cmd == 'Z') {
                    path.closePath();
                }
            } catch (Exception ignored) {
            }
        }
        return path;
    }

    private PDType0Font loadJapaneseFont(PDDocument document) {
        try {
            ClassPathResource fontResource = new ClassPathResource("fonts/JapaneseFont.ttf");
            if (fontResource.exists()) {
                try (InputStream is = fontResource.getInputStream()) {
                    return PDType0Font.load(document, is);
                }
            }
        } catch (Exception e) {
            log.warn("Could not load custom Japanese font: {}", e.getMessage());
        }
        return null;
    }

    private void renderPageHeader(PDPageContentStream cs, PDType0Font font, String notebookName,
                                  int currentPage, int totalPages, float marginX, float pageWidth, float pageHeight) throws Exception {
        cs.saveGraphicsState();

        // Đường kẻ ngăn cách header thanh mảnh
        cs.setStrokingColor(new Color(215, 215, 215));
        cs.setLineWidth(0.6f);
        cs.moveTo(marginX, pageHeight - 30f);
        cs.lineTo(pageWidth - marginX, pageHeight - 30f);
        cs.stroke();

        String pageText = "Trang " + currentPage + " / " + totalPages;
        float pageTextWidth = font.getStringWidth(pageText) / 1000f * 8.5f;

        float maxTitleWidth = pageWidth - 2 * marginX - pageTextWidth - 15f;
        String cleanNotebookName = notebookName != null ? removeAccent(notebookName).trim() : "";
        String title = "ElearningJP | 漢字練習帳 (Vo luyen viet)" + (!cleanNotebookName.isEmpty() ? " - " + cleanNotebookName : "");
        title = sanitizeForFont(font, title);
        while (title.length() > 10 && font.getStringWidth(title + "...") / 1000f * 8.5f > maxTitleWidth) {
            title = title.substring(0, title.length() - 1);
            if (font.getStringWidth(title + "...") / 1000f * 8.5f <= maxTitleWidth) {
                title = title + "...";
                break;
            }
        }

        cs.setNonStrokingColor(new Color(110, 110, 110));
        cs.beginText();
        cs.setFont(font, 8.5f);
        cs.newLineAtOffset(marginX, pageHeight - 24f);
        cs.showText(title);
        cs.endText();
        cs.beginText();
        cs.setFont(font, 8.5f);
        cs.newLineAtOffset(pageWidth - marginX - pageTextWidth, pageHeight - 24f);
        cs.showText(pageText);
        cs.endText();

        cs.restoreGraphicsState();
    }

    private void renderPageFooter(PDPageContentStream cs, PDType0Font font,
                                  int currentPage, int totalPages, float marginX, float pageWidth) throws Exception {
        cs.saveGraphicsState();

        // Đường kẻ ngăn cách footer thanh mảnh
        cs.setStrokingColor(new Color(215, 215, 215));
        cs.setLineWidth(0.6f);
        cs.moveTo(marginX, 32f);
        cs.lineTo(pageWidth - marginX, 32f);
        cs.stroke();

        cs.setNonStrokingColor(new Color(125, 125, 125));
        cs.beginText();
        cs.setFont(font, 8.0f);
        cs.newLineAtOffset(marginX, 20f);
        cs.showText("ElearningJP - Nen tang hoc tieng Nhat & Luyen thi JLPT");
        cs.endText();

        String webText = "elearningjp.com";
        float webTextWidth = font.getStringWidth(webText) / 1000f * 8.0f;
        cs.beginText();
        cs.setFont(font, 8.0f);
        cs.newLineAtOffset(pageWidth - marginX - webTextWidth, 20f);
        cs.showText(webText);
        cs.endText();

        cs.restoreGraphicsState();
    }

    private String sanitizeForFont(PDType0Font font, String text) {
        if (text == null || text.isEmpty()) return "";
        if (font == null) return text;

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); ) {
            int codePoint = text.codePointAt(i);
            int charCount = Character.charCount(codePoint);
            String sub = text.substring(i, i + charCount);

            // Ký tự ngoài ASCII và tiếng Nhật (ví dụ ký tự tiếng Việt có dấu) cần xóa dấu để an toàn với font tiếng Nhật
            boolean isJapaneseOrAscii = (codePoint >= 0x20 && codePoint <= 0x7E) ||
                    (codePoint >= 0x3000 && codePoint <= 0x30FF) ||
                    (codePoint >= 0x4E00 && codePoint <= 0x9FAF) ||
                    (codePoint >= 0x3400 && codePoint <= 0x4DBF) ||
                    (codePoint >= 0xFF00 && codePoint <= 0xFFEF);

            if (!isJapaneseOrAscii) {
                sub = removeAccent(sub);
            }

            try {
                font.encode(sub);
                sb.append(sub);
            } catch (Exception e) {
                sb.append(" ");
            }
            i += charCount;
        }
        return sb.toString();
    }

    private String removeAccent(String s) {
        if (s == null) return "";
        String temp = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD);
        return DIACRITICAL_MARKS.matcher(temp).replaceAll("").replace('đ', 'd').replace('Đ', 'D');
    }

    private List<String> extractKanji(String text) {
        List<String> list = new ArrayList<>();
        if (text == null) return list;
        for (int i = 0; i < text.length(); ) {
            int codePoint = text.codePointAt(i);
            int charCount = Character.charCount(codePoint);
            if ((codePoint >= 0x4E00 && codePoint <= 0x9FAF) ||
                (codePoint >= 0x3400 && codePoint <= 0x4DBF)) {
                list.add(text.substring(i, i + charCount));
            }
            i += charCount;
        }
        return list;
    }
}
