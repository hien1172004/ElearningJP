package com.jp.elearningjp.service.notebook.impl;

import com.jp.elearningjp.dto.response.notebook.KanjiPdfMetaResponse;
import com.jp.elearningjp.dto.response.notebook.KanjiPdfPreviewResponse;
import com.jp.elearningjp.entity.content.CharacterStroke;
import com.jp.elearningjp.entity.notebook.Notebook;
import com.jp.elearningjp.entity.notebook.NotebookWord;
import com.jp.elearningjp.exception.AppException;
import com.jp.elearningjp.exception.ErrorCode;
import com.jp.elearningjp.repository.content.CharacterStrokeRepository;
import com.jp.elearningjp.repository.notebook.NotebookRepository;
import com.jp.elearningjp.repository.notebook.NotebookWordRepository;
import com.jp.elearningjp.service.notebook.NotebookExportService;
import com.jp.elearningjp.shared.pdf.PracticeSheetBlock;
import com.jp.elearningjp.shared.pdf.PracticeSheetGroup;
import com.jp.elearningjp.shared.pdf.PracticeSheetRenderer;
import com.jp.elearningjp.shared.util.CurrentUserService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class NotebookExportServiceImpl implements NotebookExportService {

    NotebookRepository notebookRepository;
    NotebookWordRepository notebookWordRepository;
    CharacterStrokeRepository characterStrokeRepository;
    CurrentUserService currentUserService;
    PracticeSheetRenderer practiceSheetRenderer;

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
     * Gom từ trong sổ tay thành các khối luyện viết (chỉ các chữ Hán của từ) rồi vẽ PDF qua {@link PracticeSheetRenderer}.
     * Sổ tay không có chữ Hán nào (toàn Kana) → luyện tối đa 4 ký tự đầu của mỗi từ.
     */
    private byte[] generatePdfInternal(Long notebookId) {
        Long currentUserId = currentUserService.getCurrentUserId();
        Notebook notebook = notebookRepository.findByIdAndUserIdAndDeletedFalse(notebookId, currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.NOTEBOOK_NOT_FOUND));

        List<NotebookWord> words = notebookWordRepository.findByNotebookIdAndDeletedFalseOrderByDisplayOrderAscIdAsc(notebookId);

        List<PracticeSheetBlock> blocks = new ArrayList<>();
        for (NotebookWord w : words) {
            String wordStr = w.getWord() != null ? w.getWord().trim() : "";
            List<String> kanjis = PracticeSheetRenderer.extractKanji(wordStr);
            if (!kanjis.isEmpty()) blocks.add(toBlock(w, wordStr, kanjis));
        }
        // Fallback nếu toàn bộ sổ tay không có chữ Hán nào (ví dụ toàn Kana N5)
        if (blocks.isEmpty()) {
            for (NotebookWord w : words) {
                String wordStr = w.getWord() != null ? w.getWord().trim() : "";
                if (wordStr.isEmpty()) continue;
                List<String> chars = new ArrayList<>();
                for (int i = 0; i < wordStr.length() && i < 4; i++) {
                    chars.add(String.valueOf(wordStr.charAt(i)));
                }
                blocks.add(toBlock(w, wordStr, chars));
            }
        }
        return practiceSheetRenderer.render(notebook.getName(), List.of(new PracticeSheetGroup(null, blocks)));
    }

    private PracticeSheetBlock toBlock(NotebookWord w, String wordStr, List<String> chars) {
        List<String> strokeChars = new ArrayList<>(new LinkedHashSet<>(chars));
        Map<String, List<CharacterStroke>> strokes = new LinkedHashMap<>();
        for (String c : strokeChars) {
            strokes.put(c, characterStrokeRepository.findByCharacterString(c));
        }
        String reading = w.getReading() != null && !w.getReading().isEmpty() ? w.getReading() : wordStr;
        return new PracticeSheetBlock(wordStr, reading, strokeChars, chars, strokes);
    }
}
