package com.jp.elearningjp.shared.pdf;

import com.jp.elearningjp.entity.content.CharacterStroke;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Vẽ PDF vở luyện viết (ô chữ Mễ, A4 dọc, chuẩn Mazii) — dùng chung cho sổ tay và bài học.
 * Mỗi {@link PracticeSheetBlock} là 1 từ/chữ: các hàng thứ tự nét → hàng đồ chữ mờ kèm cách đọc → 2 hàng trống.
 * Các khối được chia theo {@link PracticeSheetGroup}; nhóm có tiêu đề sẽ in 1 dòng tiêu đề trước khối đầu tiên.
 */
@Slf4j
@Component
public class PracticeSheetRenderer {

    private static final Pattern SVG_CMD_PATTERN = Pattern.compile("([MmLlHhVvCcSsQqTtAaZz])([^MmLlHhVvCcSsQqTtAaZz]*)");
    private static final Pattern SVG_NUM_PATTERN = Pattern.compile("[-+]?(?:\\d*\\.\\d+|\\d+)");
    private static final Pattern DIACRITICAL_MARKS = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
    /** Số ô vẽ nét trên 1 hàng (ô đầu là chữ mẫu). */
    static final int STROKE_COLS_PER_ROW = 14;
    private static final int COLS = 15;
    private static final float GROUP_HEADING_HEIGHT = 24f;
    private static final float BLOCK_GAP = 14f;

    /** Phần tử trên 1 trang: tiêu đề nhóm hoặc khối luyện viết. */
    private record PageEntry(String heading, PracticeSheetBlock block) {
    }

    /**
     * Tạo PDF luyện viết.
     *
     * @param sheetName tên hiển thị trên header (tên sổ tay / bài học)
     * @param groups    các nhóm khối luyện viết theo thứ tự in
     */
    public byte[] render(String sheetName, List<PracticeSheetGroup> groups) {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            PDType0Font font = loadJapaneseFont(document);

            // A4 dọc: 595.28 x 841.89 pt
            float pageWidth = PDRectangle.A4.getWidth();
            float pageHeight = PDRectangle.A4.getHeight();
            float marginX = 25f;
            float cellSize = (pageWidth - 2 * marginX) / COLS; // ~36.35 pt (~12.8mm)
            float topMargin = 42f;
            float bottomMargin = 40f;
            float availableHeight = pageHeight - topMargin - bottomMargin;

            // Phân trang: tiêu đề nhóm luôn đi cùng khối đầu tiên của nhóm (không bị treo cuối trang)
            List<List<PageEntry>> pages = new ArrayList<>();
            List<PageEntry> current = new ArrayList<>();
            float used = 0;
            for (PracticeSheetGroup group : groups) {
                boolean first = true;
                for (PracticeSheetBlock b : group.blocks()) {
                    boolean withHeading = first && group.title() != null && !group.title().isBlank();
                    float content = b.totalRows(STROKE_COLS_PER_ROW) * cellSize + (withHeading ? GROUP_HEADING_HEIGHT : 0);
                    float gap = current.isEmpty() ? 0 : BLOCK_GAP;
                    if (!current.isEmpty() && used + gap + content > availableHeight) {
                        pages.add(current);
                        current = new ArrayList<>();
                        gap = 0;
                        used = 0;
                    }
                    if (withHeading) current.add(new PageEntry(group.title(), null));
                    current.add(new PageEntry(null, b));
                    used += gap + content;
                    first = false;
                }
            }
            if (!current.isEmpty()) pages.add(current);
            if (pages.isEmpty()) pages.add(Collections.emptyList());

            int totalPages = pages.size();
            for (int p = 0; p < totalPages; p++) {
                PDPage page = new PDPage(new PDRectangle(pageWidth, pageHeight));
                document.addPage(page);
                try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                    renderPageHeader(cs, font, sheetName, p + 1, totalPages, marginX, pageWidth, pageHeight);
                    float currentY = pageHeight - topMargin;
                    boolean afterHeading = false;
                    boolean firstOnPage = true;
                    for (PageEntry entry : pages.get(p)) {
                        if (!firstOnPage && !afterHeading) currentY -= BLOCK_GAP;
                        if (entry.heading() != null) {
                            currentY = renderGroupHeading(cs, font, entry.heading(), marginX, currentY, pageWidth - 2 * marginX);
                            afterHeading = true;
                        } else {
                            currentY = renderWordPracticeBlock(cs, font, entry.block(), marginX, currentY, cellSize, COLS);
                            afterHeading = false;
                        }
                        firstOnPage = false;
                    }
                    renderPageFooter(cs, font, p + 1, totalPages, marginX, pageWidth);
                }
            }

            document.save(baos);
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Error generating practice sheet PDF: ", e);
            throw new RuntimeException("Lỗi khi tạo file PDF luyện viết", e);
        }
    }

    /** Dòng tiêu đề nhóm: thanh màu nhạt + tên nhóm (bỏ dấu vì font tiếng Nhật không có glyph tiếng Việt). */
    private float renderGroupHeading(PDPageContentStream cs, PDType0Font font, String title,
                                     float x, float top, float width) throws Exception {
        float boxHeight = GROUP_HEADING_HEIGHT - 6f;
        float y = top - boxHeight;
        cs.saveGraphicsState();
        cs.setNonStrokingColor(new Color(238, 242, 255));
        cs.addRect(x, y, width, boxHeight);
        cs.fill();
        cs.setNonStrokingColor(new Color(79, 70, 229));
        cs.addRect(x, y, 3f, boxHeight);
        cs.fill();
        String text = sanitizeForFont(font, removeAccent(title).trim());
        if (font != null && !text.isEmpty()) {
            cs.setNonStrokingColor(new Color(49, 46, 129));
            cs.beginText();
            cs.setFont(font, 10.5f);
            cs.newLineAtOffset(x + 9f, y + 5f);
            cs.showText(text);
            cs.endText();
        }
        cs.restoreGraphicsState();
        return top - GROUP_HEADING_HEIGHT;
    }

    /**
     * Vẽ 1 khối luyện viết từ vựng chuẩn Mazii:
     * - Hàng 1..N: Các hàng thứ tự nét của từng chữ Hán (ô 0: chữ mẫu, ô 1..14: nét tăng dần & chữ mờ)
     * - Hàng N+1: Ô cách đọc (ô trắng tinh, bẻ dòng gọn gàng) + Lặp lại từ vựng mờ cách nhau 1 ô trống
     * - Hàng N+2: 15 ô chữ Mễ trống
     * - Hàng N+3: 15 ô chữ Mễ trống
     */
    private float renderWordPracticeBlock(PDPageContentStream cs, PDType0Font font, PracticeSheetBlock block,
                                          float startX, float blockTop, float cellSize, int cols) throws Exception {
        float currentY = blockTop;
        Color faintGray = new Color(195, 195, 195);

        // 1. CÁC HÀNG BÚT THUẬN TỪNG CHỮ HÁN (Tự động xuống hàng nếu chữ nhiều hơn 14 nét)
        for (String kanji : block.strokeChars()) {
            List<CharacterStroke> strokes = block.strokes().getOrDefault(kanji, Collections.emptyList());
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
        drawReadingTextCentered(cs, font, block.reading(), startX, wordRowY, cellSize, Color.DARK_GRAY);

        // Ô 1..14: Lặp lại cả từ vựng bằng chữ mờ, cách nhau 1 ô trống chuẩn Mazii
        List<String> wKanjis = block.wordChars();
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

    private void renderPageHeader(PDPageContentStream cs, PDType0Font font, String sheetName,
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
        String cleanNotebookName = sheetName != null ? removeAccent(sheetName).trim() : "";
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

    /** Các chữ Hán (CJK) trong chuỗi, theo thứ tự xuất hiện (có lặp). */
    public static List<String> extractKanji(String text) {
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
