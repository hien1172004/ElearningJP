package com.jp.elearningjp.service.notebook.impl;

import com.jp.elearningjp.dto.request.notebook.CreateNotebookWordRequest;
import com.jp.elearningjp.dto.request.notebook.UpdateNotebookWordRequest;
import com.jp.elearningjp.dto.response.notebook.ExcelImportResultResponse;
import com.jp.elearningjp.dto.response.notebook.NotebookStatsResponse;
import com.jp.elearningjp.dto.response.notebook.NotebookWordResponse;
import com.jp.elearningjp.entity.content.Vocabulary;
import com.jp.elearningjp.entity.content.WritingCharacter;
import com.jp.elearningjp.entity.notebook.Notebook;
import com.jp.elearningjp.entity.notebook.NotebookWord;
import com.jp.elearningjp.exception.AppException;
import com.jp.elearningjp.exception.ErrorCode;
import com.jp.elearningjp.mapper.notebook.NotebookMapper;
import com.jp.elearningjp.repository.content.VocabularyRepository;
import com.jp.elearningjp.repository.content.WritingCharacterRepository;
import com.jp.elearningjp.repository.notebook.NotebookRepository;
import com.jp.elearningjp.repository.notebook.NotebookWordRepository;
import com.jp.elearningjp.service.notebook.NotebookExportService;
import com.jp.elearningjp.service.notebook.NotebookWordService;
import com.jp.elearningjp.shared.enums.MemoryLevel;
import com.jp.elearningjp.dto.response.notebook.NotebookReviewItemResponse;
import com.jp.elearningjp.dto.request.notebook.NotebookReviewSubmitRequest;
import com.jp.elearningjp.dto.response.notebook.NotebookReviewSubmitResponse;
import com.jp.elearningjp.service.srs.Sm2Algorithm;
import com.jp.elearningjp.shared.enums.SrsRating;
import java.util.Collections;
import java.util.Random;

import com.jp.elearningjp.shared.util.CurrentUserService;
import jakarta.persistence.criteria.Predicate;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class NotebookWordServiceImpl implements NotebookWordService {

    NotebookWordRepository notebookWordRepository;
    NotebookRepository notebookRepository;
    VocabularyRepository vocabularyRepository;
    WritingCharacterRepository writingCharacterRepository;
    NotebookMapper notebookMapper;
    NotebookExportService notebookExportService;
    CurrentUserService currentUserService;

    private static final int MAX_EXCEL_ROWS = 500;
    private static final long MAX_EXCEL_FILE_SIZE = 5 * 1024 * 1024; // 5MB

    @Override
    @Transactional
    public NotebookWordResponse addWord(Long notebookId, CreateNotebookWordRequest request) {
        Long currentUserId = currentUserService.getCurrentUserId();
        Notebook notebook = notebookRepository.findByIdAndUserIdAndDeletedFalse(notebookId, currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.NOTEBOOK_NOT_FOUND));

        NotebookWord word = notebookMapper.toNotebookWord(request);
        word.setNotebook(notebook);
        word.setMemoryLevel(MemoryLevel.NOT_LEARNED);
        word.setIntervalDays(0);
        word.setRepetitionCount(0);
        word.setLapseCount(0);
        word.setEaseFactor(new BigDecimal("2.50"));
        word.setNextReviewAt(Instant.now());

        // Auto-link từ điển hệ thống nếu chưa truyền vocabularyId
        linkVocabularyIfAvailable(word);
        if (word.getReading() != null) {
            word.setReading(sanitizeReading(word.getReading()));
        }

        NotebookWord saved = notebookWordRepository.save(word);
        updateNotebookWordCount(notebook);

        return notebookMapper.toNotebookWordResponse(saved);
    }

    @Override
    @Transactional
    public NotebookWordResponse updateWord(Long notebookId, Long wordId, UpdateNotebookWordRequest request) {
        Long currentUserId = currentUserService.getCurrentUserId();
        notebookRepository.findByIdAndUserIdAndDeletedFalse(notebookId, currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.NOTEBOOK_NOT_FOUND));

        NotebookWord word = notebookWordRepository.findByIdAndDeletedFalse(wordId)
                .filter(w -> w.getNotebook().getId().equals(notebookId))
                .orElseThrow(() -> new AppException(ErrorCode.NOTEBOOK_WORD_NOT_FOUND));

        notebookMapper.updateNotebookWordFromRequest(request, word);
        if (request.getWord() != null) {
            linkVocabularyIfAvailable(word);
        }
        if (word.getReading() != null) {
            word.setReading(sanitizeReading(word.getReading()));
        }

        NotebookWord saved = notebookWordRepository.save(word);
        return notebookMapper.toNotebookWordResponse(saved);
    }

    @Override
    @Transactional
    public void deleteWord(Long notebookId, Long wordId) {
        Long currentUserId = currentUserService.getCurrentUserId();
        Notebook notebook = notebookRepository.findByIdAndUserIdAndDeletedFalse(notebookId, currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.NOTEBOOK_NOT_FOUND));

        NotebookWord word = notebookWordRepository.findByIdAndDeletedFalse(wordId)
                .filter(w -> w.getNotebook().getId().equals(notebookId))
                .orElseThrow(() -> new AppException(ErrorCode.NOTEBOOK_WORD_NOT_FOUND));

        word.softDelete();
        notebookWordRepository.save(word);
        updateNotebookWordCount(notebook);
    }

    @Override
    @Transactional
    public void batchDeleteWords(Long notebookId, List<Long> wordIds) {
        Long currentUserId = currentUserService.getCurrentUserId();
        Notebook notebook = notebookRepository.findByIdAndUserIdAndDeletedFalse(notebookId, currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.NOTEBOOK_NOT_FOUND));

        List<NotebookWord> words = notebookWordRepository.findAllById(wordIds).stream()
                .filter(w -> w.getNotebook().getId().equals(notebookId) && !w.isDeleted())
                .toList();

        for (NotebookWord w : words) {
            w.softDelete();
        }
        notebookWordRepository.saveAll(words);
        updateNotebookWordCount(notebook);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotebookWordResponse> getWords(Long notebookId) {
        Long currentUserId = currentUserService.getCurrentUserId();
        Notebook notebook = notebookRepository.findByIdAndDeletedFalse(notebookId)
                .orElseThrow(() -> new AppException(ErrorCode.NOTEBOOK_NOT_FOUND));

        if (!notebook.getUser().getId().equals(currentUserId) && !notebook.isPublic() && !notebook.isSharingEnabled()) {
            throw new AppException(ErrorCode.NOTEBOOK_FORBIDDEN);
        }

        List<NotebookWord> words = notebookWordRepository.findByNotebookIdAndDeletedFalseOrderByDisplayOrderAscIdAsc(notebookId);
        return words.stream()
                .map(this::mapToResponseWithDeducedHanViet)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotebookWordResponse> searchWords(Long notebookId, String keyword, Pageable pageable) {
        Long currentUserId = currentUserService.getCurrentUserId();
        Notebook notebook = notebookRepository.findByIdAndDeletedFalse(notebookId)
                .orElseThrow(() -> new AppException(ErrorCode.NOTEBOOK_NOT_FOUND));

        if (!notebook.getUser().getId().equals(currentUserId) && !notebook.isPublic() && !notebook.isSharingEnabled()) {
            throw new AppException(ErrorCode.NOTEBOOK_FORBIDDEN);
        }

        Specification<NotebookWord> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("notebook").get("id"), notebookId));
            predicates.add(cb.isFalse(root.get("deleted")));

            if (keyword != null && !keyword.trim().isEmpty()) {
                String pattern = "%" + keyword.trim().toLowerCase() + "%";
                Predicate wordLike = cb.like(cb.lower(root.get("word")), pattern);
                Predicate readingLike = cb.like(cb.lower(root.get("reading")), pattern);
                Predicate meaningLike = cb.like(cb.lower(root.get("meaning")), pattern);
                predicates.add(cb.or(wordLike, readingLike, meaningLike));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<NotebookWord> page = notebookWordRepository.findAll(spec, pageable);
        return page.map(this::mapToResponseWithDeducedHanViet);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotebookWordResponse> getShuffledWords(Long notebookId) {
        Long currentUserId = currentUserService.getCurrentUserId();
        notebookRepository.findByIdAndUserIdAndDeletedFalse(notebookId, currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.NOTEBOOK_NOT_FOUND));

        List<NotebookWord> words = notebookWordRepository.findShuffledWordsByNotebookId(notebookId);
        return words.stream()
                .map(this::mapToResponseWithDeducedHanViet)
                .toList();
    }

    private NotebookWordResponse mapToResponseWithDeducedHanViet(NotebookWord word) {
        NotebookWordResponse resp = notebookMapper.toNotebookWordResponse(word);
        if (resp.getSinoVietnamese() == null || resp.getSinoVietnamese().trim().isEmpty()) {
            String deduced = deduceHanViet(word.getWord());
            if (deduced != null && !deduced.isEmpty()) {
                resp.setSinoVietnamese(deduced);
            }
        }
        return resp;
    }

    @Override
    @Transactional
    public ExcelImportResultResponse importWordsFromExcel(Long notebookId, MultipartFile file) {
        Long currentUserId = currentUserService.getCurrentUserId();
        Notebook notebook = notebookRepository.findByIdAndUserIdAndDeletedFalse(notebookId, currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.NOTEBOOK_NOT_FOUND));

        if (file == null || file.isEmpty()) {
            throw new AppException(ErrorCode.NOTEBOOK_EXCEL_EMPTY_FILE);
        }

        if (file.getSize() > MAX_EXCEL_FILE_SIZE) {
            throw new AppException(ErrorCode.NOTEBOOK_EXCEL_INVALID_FORMAT);
        }

        List<NotebookWord> newWords = new ArrayList<>();
        List<String> errorMessages = new ArrayList<>();
        int successCount = 0;
        int errorCount = 0;

        try (InputStream is = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(is)) {

            Sheet sheet = workbook.getSheetAt(0);
            int lastRowNum = sheet.getLastRowNum();

            // Kiểm tra số dòng (không tính header)
            int rowCount = 0;
            for (int r = 1; r <= lastRowNum; r++) {
                Row row = sheet.getRow(r);
                if (row != null && !isRowEmpty(row)) {
                    rowCount++;
                }
            }

            if (rowCount > MAX_EXCEL_ROWS) {
                throw new AppException(ErrorCode.NOTEBOOK_EXCEL_ROW_LIMIT_EXCEEDED);
            }

            if (rowCount == 0) {
                throw new AppException(ErrorCode.NOTEBOOK_EXCEL_EMPTY_FILE);
            }

            // Tự động phân tích dòng tiêu đề (Header row tại r = 0)
            Row headerRow = sheet.getRow(0);
            int wordCol = 0;
            int phoneticCol = 1;
            int meanCol = 2;
            int commentCol = 3;
            int partOfSpeechCol = -1;
            int hanVietCol = -1;

            if (headerRow != null) {
                int lastCellNum = headerRow.getLastCellNum();
                for (int c = 0; c < lastCellNum; c++) {
                    String h = getCellStringValue(headerRow.getCell(c));
                    if (h == null) continue;
                    String lower = h.trim().toLowerCase();
                    if (lower.equals("word") || lower.contains("từ vựng") || lower.equals("tu vung")) {
                        wordCol = c;
                    } else if (lower.equals("phonetic") || lower.contains("phiên âm") || lower.contains("phien am") || lower.equals("reading") || lower.contains("cách đọc")) {
                        phoneticCol = c;
                    } else if (lower.equals("mean") || lower.equals("meaning") || lower.contains("nghĩa") || lower.contains("nghia")) {
                        meanCol = c;
                    } else if (lower.equals("comment") || lower.contains("ghi chú") || lower.contains("ghi chu") || lower.equals("note") || lower.equals("user_note")) {
                        commentCol = c;
                    } else if (lower.equals("part_of_speech") || lower.contains("từ loại") || lower.contains("tu loai") || lower.equals("type")) {
                        partOfSpeechCol = c;
                    } else if (lower.equals("han_viet") || lower.contains("hán việt") || lower.contains("han viet") || lower.equals("sino_vietnamese")) {
                        hanVietCol = c;
                    }
                }
            }

            int existingCount = (int) notebookWordRepository.countByNotebookIdAndDeletedFalse(notebookId);

            // Đọc dữ liệu từng dòng
            for (int r = 1; r <= lastRowNum; r++) {
                Row row = sheet.getRow(r);
                if (row == null || isRowEmpty(row)) {
                    continue;
                }

                String wordText = wordCol >= 0 ? getCellStringValue(row.getCell(wordCol)) : null;
                String phonetic = phoneticCol >= 0 ? getCellStringValue(row.getCell(phoneticCol)) : null;
                String meaning = meanCol >= 0 ? getCellStringValue(row.getCell(meanCol)) : null;
                String userNote = commentCol >= 0 ? getCellStringValue(row.getCell(commentCol)) : null;
                String partOfSpeech = partOfSpeechCol >= 0 ? getCellStringValue(row.getCell(partOfSpeechCol)) : null;
                String hanViet = hanVietCol >= 0 ? getCellStringValue(row.getCell(hanVietCol)) : null;

                if (wordText == null || wordText.trim().isEmpty()) {
                    errorCount++;
                    errorMessages.add("Dòng " + (r + 1) + ": Từ vựng không được để trống");
                    continue;
                }

                // Nếu nghĩa trống, thử tìm tự động trong từ điển hệ thống
                if (meaning == null || meaning.trim().isEmpty()) {
                    Optional<Vocabulary> vocabOpt = vocabularyRepository.findFirstByWordAndDeletedFalseOrderBySenseNoAsc(wordText.trim());
                    if (vocabOpt.isPresent() && vocabOpt.get().getMeaningVi() != null && !vocabOpt.get().getMeaningVi().isEmpty()) {
                        meaning = vocabOpt.get().getMeaningVi();
                    } else {
                        errorCount++;
                        errorMessages.add("Dòng " + (r + 1) + ": Nghĩa không được để trống");
                        continue;
                    }
                }

                NotebookWord word = NotebookWord.builder()
                        .notebook(notebook)
                        .word(wordText.trim())
                        .reading(phonetic != null && !phonetic.trim().isEmpty() ? phonetic.trim() : null)
                        .meaning(meaning.trim())
                        .partOfSpeech(partOfSpeech != null && !partOfSpeech.trim().isEmpty() ? partOfSpeech.trim() : "Từ vựng")
                        .sinoVietnamese(hanViet != null && !hanViet.trim().isEmpty() ? hanViet.trim() : null)
                        .userNote(userNote != null && !userNote.trim().isEmpty() ? userNote.trim() : null)
                        .memoryLevel(MemoryLevel.NOT_LEARNED)
                        .intervalDays(0)
                        .repetitionCount(0)
                        .lapseCount(0)
                        .easeFactor(new BigDecimal("2.50"))
                        .nextReviewAt(Instant.now())
                        .displayOrder(existingCount + newWords.size())
                        .build();

                linkVocabularyIfAvailable(word);
                newWords.add(word);
                successCount++;
            }

            if (!newWords.isEmpty()) {
                notebookWordRepository.saveAll(newWords);
                updateNotebookWordCount(notebook);
            }

        } catch (AppException ae) {
            throw ae;
        } catch (Exception e) {
            log.error("Error importing excel file: ", e);
            throw new AppException(ErrorCode.NOTEBOOK_EXCEL_INVALID_FORMAT);
        }

        return ExcelImportResultResponse.builder()
                .totalProcessed(successCount + errorCount)
                .successCount(successCount)
                .errorCount(errorCount)
                .errorMessages(errorMessages)
                .build();
    }

    @Override
    public byte[] exportWordsToExcel(Long notebookId) {
        return notebookExportService.exportWordsToExcel(notebookId);
    }

    @Override
    @Transactional(readOnly = true)
    public NotebookStatsResponse getStatsOverview() {
        Long currentUserId = currentUserService.getCurrentUserId();

        long total = notebookWordRepository.countTotalWordsByUserId(currentUserId);
        long unlearned = notebookWordRepository.countWordsByUserIdAndMemoryLevel(currentUserId, MemoryLevel.NOT_LEARNED);
        long barely = notebookWordRepository.countWordsByUserIdAndMemoryLevel(currentUserId, MemoryLevel.BARELY);
        long moderate = notebookWordRepository.countWordsByUserIdAndMemoryLevel(currentUserId, MemoryLevel.MODERATE);
        long mastered = notebookWordRepository.countWordsByUserIdAndMemoryLevel(currentUserId, MemoryLevel.MASTERED);

        return NotebookStatsResponse.builder()
                .totalWords(total)
                .unlearnedCount(unlearned)
                .notLearnedCount(unlearned)
                .barelyCount(barely)
                .moderateCount(moderate)
                .masteredCount(mastered)
                .srEnabled(true)
                .build();
    }

    private void linkVocabularyIfAvailable(NotebookWord word) {
        if (word == null || word.getWord() == null || word.getWord().trim().isEmpty()) return;

        // 1. Tìm trong từ điển hệ thống theo word
        if (word.getVocabulary() == null) {
            vocabularyRepository.findFirstByWordAndDeletedFalseOrderBySenseNoAsc(word.getWord().trim()).ifPresent(vocab -> {
                word.setVocabulary(vocab);
                if (word.getReading() == null || word.getReading().trim().isEmpty()) {
                    word.setReading(vocab.getHiragana());
                }
                if (word.getAudioUrl() == null && vocab.getAudioUrl() != null) {
                    word.setAudioUrl(vocab.getAudioUrl());
                }
                if (word.getMeaning() == null || word.getMeaning().trim().isEmpty()) {
                    word.setMeaning(vocab.getMeaningVi());
                }
                if (word.getPartOfSpeech() == null && vocab.getPartOfSpeech() != null) {
                    word.setPartOfSpeech(vocab.getPartOfSpeech());
                }
                if ((word.getSinoVietnamese() == null || word.getSinoVietnamese().trim().isEmpty()) && vocab.getHanViet() != null) {
                    word.setSinoVietnamese(vocab.getHanViet());
                }
                if (word.getJlptLevel() == null && vocab.getJlptLevel() != null) {
                    word.setJlptLevel(vocab.getJlptLevel().name());
                }
            });
        }

        // 2. Tự động suy luận Hán Việt cho các chữ Hán trong từ nếu chưa có
        if (word.getSinoVietnamese() == null || word.getSinoVietnamese().trim().isEmpty()) {
            String deduced = deduceHanViet(word.getWord());
            if (deduced != null && !deduced.isEmpty()) {
                word.setSinoVietnamese(deduced);
            }
        }
    }

    private String deduceHanViet(String word) {
        if (word == null || word.trim().isEmpty()) return null;
        List<String> kanjis = extractKanji(word);
        if (kanjis.isEmpty()) return null;

        Map<String, String> kanjiMap = writingCharacterRepository.findByCharacterInAndDeletedFalse(kanjis)
                .stream()
                .filter(wc -> wc.getCharacter() != null && wc.getHanViet() != null && !wc.getHanViet().trim().isEmpty())
                .collect(Collectors.toMap(
                        WritingCharacter::getCharacter,
                        wc -> {
                            String hv = wc.getHanViet().trim();
                            if (hv.contains(",")) {
                                hv = hv.split(",")[0].trim();
                            }
                            return hv.toUpperCase();
                        },
                        (existing, replacement) -> existing
                ));

        List<String> resultParts = new ArrayList<>();
        for (String k : kanjis) {
            String hv = kanjiMap.get(k);
            if (hv != null && !hv.isEmpty()) {
                resultParts.add(hv);
            }
        }

        return resultParts.isEmpty() ? null : String.join(" ", resultParts);
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

    private void updateNotebookWordCount(Notebook notebook) {
        long count = notebookWordRepository.countByNotebookIdAndDeletedFalse(notebook.getId());
        notebook.setWordCount((int) count);
        notebookRepository.save(notebook);
    }

    private boolean isRowEmpty(Row row) {
        for (int c = row.getFirstCellNum(); c < row.getLastCellNum(); c++) {
            Cell cell = row.getCell(c);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                String val = getCellStringValue(cell);
                if (val != null && !val.trim().isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }

    private String getCellStringValue(Cell cell) {
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getCellFormula();
            default -> null;
        };
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotebookReviewItemResponse> getReviewItems(Long notebookId, Integer limit) {
        Long currentUserId = currentUserService.getCurrentUserId();
        int fetchLimit = (limit == null || limit <= 0) ? 20 : Math.min(limit, 50);

        List<NotebookWord> targetWords = new ArrayList<>();
        java.util.Set<Long> targetWordIds = new java.util.HashSet<>();
        List<NotebookWord> fullPool;

        if (notebookId == null || notebookId <= 0) {
            // Luyện tập tất cả các sổ tay của user
            List<NotebookWord> dueWords = notebookWordRepository.findDueWordsByUserId(
                    currentUserId, Instant.now(), org.springframework.data.domain.PageRequest.of(0, fetchLimit)
            );
            targetWords.addAll(dueWords);
            for (NotebookWord w : dueWords) {
                targetWordIds.add(w.getId());
            }

            fullPool = notebookWordRepository.findByUserIdAndDeletedFalseOrderByDisplayOrderAscIdAsc(currentUserId);
            if (targetWords.size() < fetchLimit) {
                for (NotebookWord w : fullPool) {
                    if (!targetWordIds.contains(w.getId())) {
                        targetWords.add(w);
                        targetWordIds.add(w.getId());
                        if (targetWords.size() >= fetchLimit) break;
                    }
                }
            }
        } else {
            notebookRepository.findByIdAndUserIdAndDeletedFalse(notebookId, currentUserId)
                    .orElseThrow(() -> new AppException(ErrorCode.NOTEBOOK_NOT_FOUND));

            // 1. Tìm các từ đến hạn review trong sổ tay này
            List<NotebookWord> dueWords = notebookWordRepository.findDueWordsByNotebookId(
                    notebookId, Instant.now(), org.springframework.data.domain.PageRequest.of(0, fetchLimit)
            );
            targetWords.addAll(dueWords);
            for (NotebookWord w : dueWords) {
                targetWordIds.add(w.getId());
            }

            fullPool = notebookWordRepository.findByNotebookIdAndDeletedFalseOrderByDisplayOrderAscIdAsc(notebookId);
            if (targetWords.size() < fetchLimit) {
                for (NotebookWord w : fullPool) {
                    if (!targetWordIds.contains(w.getId())) {
                        targetWords.add(w);
                        targetWordIds.add(w.getId());
                        if (targetWords.size() >= fetchLimit) break;
                    }
                }
            }
        }

        if (targetWords.isEmpty()) {
            return Collections.emptyList();
        }

        // 3. Chuẩn bị pool các nghĩa và từ để sinh distractors cho quizz trắc nghiệm
        List<String> poolMeanings = fullPool.stream().map(NotebookWord::getMeaning).filter(m -> m != null && !m.isBlank()).distinct().toList();
        List<String> poolWords = fullPool.stream().map(NotebookWord::getWord).filter(w -> w != null && !w.isBlank()).distinct().toList();

        Random rng = new Random();

        List<NotebookReviewItemResponse> result = new ArrayList<>();
        for (NotebookWord word : targetWords) {
            // Sinh 3 distractors cho Nghĩa
            List<String> meaningOpts = new ArrayList<>();
            meaningOpts.add(word.getMeaning());
            List<String> shuffledMeanings = new ArrayList<>(poolMeanings);
            Collections.shuffle(shuffledMeanings, rng);
            for (String m : shuffledMeanings) {
                if (!m.equals(word.getMeaning()) && meaningOpts.size() < 4) {
                    meaningOpts.add(m);
                }
            }
            Collections.shuffle(meaningOpts, rng);

            // Sinh 3 distractors cho Từ
            List<String> wordOpts = new ArrayList<>();
            wordOpts.add(word.getWord());
            List<String> shuffledWords = new ArrayList<>(poolWords);
            Collections.shuffle(shuffledWords, rng);
            for (String w : shuffledWords) {
                if (!w.equals(word.getWord()) && wordOpts.size() < 4) {
                    wordOpts.add(w);
                }
            }
            Collections.shuffle(wordOpts, rng);

            String sv = word.getSinoVietnamese();
            if (sv == null || sv.trim().isEmpty()) {
                sv = deduceHanViet(word.getWord());
            }

            result.add(NotebookReviewItemResponse.builder()
                    .id(word.getId())
                    .notebookId(word.getNotebook().getId())
                    .word(word.getWord())
                    .reading(sanitizeReading(word.getReading()))
                    .sinoVietnamese(sv)
                    .meaning(word.getMeaning())
                    .partOfSpeech(word.getPartOfSpeech())
                    .audioUrl(word.getAudioUrl())
                    .userNote(word.getUserNote())
                    .memoryLevel(word.getMemoryLevel())
                    .intervalDays(word.getIntervalDays())
                    .repetitionCount(word.getRepetitionCount())
                    .exampleSentence(word.getExampleSentence())
                    .exampleMeaning(word.getExampleMeaning())
                    .quizMeaningOptions(meaningOpts)
                    .quizWordOptions(wordOpts)
                    .build());
        }

        return result;
    }

    @Override
    @Transactional
    public NotebookReviewSubmitResponse submitReview(Long notebookId, NotebookReviewSubmitRequest request) {
        Long currentUserId = currentUserService.getCurrentUserId();
        if (notebookId != null && notebookId > 0) {
            notebookRepository.findByIdAndUserIdAndDeletedFalse(notebookId, currentUserId)
                    .orElseThrow(() -> new AppException(ErrorCode.NOTEBOOK_NOT_FOUND));
        }

        NotebookWord word = notebookWordRepository.findByIdAndDeletedFalse(request.getWordId())
                .orElseThrow(() -> new AppException(ErrorCode.NOTEBOOK_NOT_FOUND));

        if (!word.getNotebook().getUser().getId().equals(currentUserId)) {
            throw new AppException(ErrorCode.NOTEBOOK_FORBIDDEN);
        }

        if (notebookId != null && notebookId > 0 && !word.getNotebook().getId().equals(notebookId)) {
            throw new AppException(ErrorCode.NOTEBOOK_FORBIDDEN);
        }

        // Tính toán chu kỳ SM-2 mới
        Sm2Algorithm.CalculationResult calc = Sm2Algorithm.calculate(
                word.getEaseFactor(),
                word.getIntervalDays(),
                word.getRepetitionCount(),
                word.getLapseCount(),
                request.getRating()
        );

        word.setEaseFactor(calc.getNewEf());
        word.setIntervalDays(calc.getNewInterval());
        word.setRepetitionCount(calc.getNewRepetitionCount());
        word.setLapseCount(calc.getNewLapseCount());
        word.setNextReviewAt(calc.getNextReviewAt());
        word.setLastReviewedAt(Instant.now());

        // Cập nhật memoryLevel theo đánh giá và tiến độ
        MemoryLevel newLevel;
        if (request.getRating() == SrsRating.AGAIN || request.getRating() == SrsRating.HARD) {
            newLevel = MemoryLevel.BARELY;
        } else if (request.getRating() == SrsRating.GOOD) {
            newLevel = (calc.getNewRepetitionCount() >= 3) ? MemoryLevel.MASTERED : MemoryLevel.MODERATE;
        } else {
            newLevel = MemoryLevel.MASTERED;
        }
        word.setMemoryLevel(newLevel);

        notebookWordRepository.save(word);

        return NotebookReviewSubmitResponse.builder()
                .wordId(word.getId())
                .memoryLevel(word.getMemoryLevel())
                .repetitionCount(word.getRepetitionCount())
                .intervalDays(word.getIntervalDays())
                .easeFactor(word.getEaseFactor())
                .nextReviewAt(word.getNextReviewAt())
                .build();
    }

    private String sanitizeReading(String reading) {
        if (reading == null || reading.isBlank()) return reading;
        String[] parts = reading.trim().split("[\\s,、;/]+");
        return parts.length > 0 ? parts[0] : reading;
    }

}
