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
import com.jp.elearningjp.shared.enums.NotebookItemType;
import com.jp.elearningjp.entity.content.GrammarPoint;
import com.jp.elearningjp.repository.content.GrammarPointRepository;
import com.jp.elearningjp.service.notebook.NotebookWordCounter;
import com.jp.elearningjp.service.notebook.NotebookLessonItemService;
import com.fasterxml.jackson.databind.JsonNode;
import com.jp.elearningjp.dto.response.notebook.NotebookReviewItemResponse;
import com.jp.elearningjp.dto.request.notebook.NotebookReviewSubmitRequest;
import com.jp.elearningjp.dto.response.notebook.NotebookReviewSubmitResponse;
import com.jp.elearningjp.service.srs.Sm2Algorithm;
import com.jp.elearningjp.shared.enums.SrsRating;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import org.springframework.data.domain.PageRequest;

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
    GrammarPointRepository grammarPointRepository;
    NotebookWordCounter notebookWordCounter;

    private static final int MAX_EXCEL_ROWS = 500;
    private static final long MAX_EXCEL_FILE_SIZE = 5 * 1024 * 1024; // 5MB

    @Override
    @Transactional
    public NotebookWordResponse addWord(Long notebookId, CreateNotebookWordRequest request) {
        Long currentUserId = currentUserService.getCurrentUserId();
        Notebook notebook = notebookRepository.findByIdAndUserIdAndDeletedFalse(notebookId, currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.NOTEBOOK_NOT_FOUND));

        NotebookItemType type = request.getItemType() != null ? request.getItemType() : NotebookItemType.VOCAB;

        NotebookWord word = notebookMapper.toNotebookWord(request);
        word.setNotebook(notebook);
        word.setItemType(type);
        word.setMemoryLevel(MemoryLevel.NOT_LEARNED);
        word.setIntervalDays(0);
        word.setRepetitionCount(0);
        word.setLapseCount(0);
        word.setEaseFactor(new BigDecimal("2.50"));
        word.setNextReviewAt(Instant.now());

        switch (type) {
            case KANJI -> applyKanjiSource(word, request.getCharacterId());
            case GRAMMAR -> applyGrammarSource(word, request.getGrammarId());
            default -> {
                // Auto-link từ điển hệ thống (ưu tiên vocabularyId client gửi lên)
                if (request.getVocabularyId() != null) {
                    vocabularyRepository.findByIdAndDeletedFalse(request.getVocabularyId()).ifPresent(word::setVocabulary);
                }
                linkVocabularyIfAvailable(word);
                if (word.getReading() != null) {
                    word.setReading(sanitizeReading(word.getReading()));
                }
            }
        }

        if (isDuplicateInNotebook(notebookId, word)) {
            throw new AppException(ErrorCode.NOTEBOOK_ITEM_ALREADY_EXISTS);
        }

        NotebookWord saved = notebookWordRepository.save(word);
        notebookWordCounter.refresh(notebook);

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
        // Chỉ thẻ Từ vựng mới tự liên kết lại từ điển khi đổi mặt chữ; Chữ Hán / Ngữ pháp giữ liên kết gốc
        if (word.getItemType() == NotebookItemType.VOCAB) {
            if (request.getWord() != null) {
                // Đổi mặt chữ khác từ đang liên kết → bỏ liên kết cũ rồi tra lại theo chữ mới
                if (word.getVocabulary() != null && !word.getWord().trim().equals(word.getVocabulary().getWord())) {
                    word.setVocabulary(null);
                }
                linkVocabularyIfAvailable(word);
            }
            if (word.getReading() != null) {
                word.setReading(sanitizeReading(word.getReading()));
            }
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
        notebookWordCounter.refresh(notebook);
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
        notebookWordCounter.refresh(notebook);
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
    public Page<NotebookWordResponse> searchWords(Long notebookId, String keyword, NotebookItemType itemType, Pageable pageable) {
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
            if (itemType != null) {
                predicates.add(cb.equal(root.get("itemType"), itemType));
            }

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
        if (word.getItemType() != NotebookItemType.GRAMMAR
                && (resp.getSinoVietnamese() == null || resp.getSinoVietnamese().trim().isEmpty())) {
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
                notebookWordCounter.refresh(notebook);
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

        // 1. Tìm trong từ điển hệ thống theo word (hoặc dùng vocabulary client đã chỉ định) rồi điền các trường còn trống
        Optional<Vocabulary> vocabOpt = word.getVocabulary() != null
                ? Optional.of(word.getVocabulary())
                : vocabularyRepository.findFirstByWordAndDeletedFalseOrderBySenseNoAsc(word.getWord().trim());
        vocabOpt.ifPresent(vocab -> {
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

        // 2. Tự động suy luận Hán Việt cho các chữ Hán trong từ nếu chưa có
        if (word.getSinoVietnamese() == null || word.getSinoVietnamese().trim().isEmpty()) {
            String deduced = deduceHanViet(word.getWord());
            if (deduced != null && !deduced.isEmpty()) {
                word.setSinoVietnamese(deduced);
            }
        }
    }

    // =========================================================
    // V59: Thẻ Chữ Hán / Ngữ pháp
    // =========================================================

    /**
     * Liên kết thẻ Chữ Hán với bản ghi {@code writing_characters} (theo id, hoặc theo mặt chữ)
     * rồi điền các trường còn trống. Không tìm thấy theo mặt chữ → giữ thẻ tự do (không liên kết).
     */
    private void applyKanjiSource(NotebookWord word, Long characterId) {
        WritingCharacter wc;
        if (characterId != null) {
            wc = writingCharacterRepository.findByIdAndDeletedFalse(characterId)
                    .orElseThrow(() -> new AppException(ErrorCode.NOTEBOOK_ITEM_SOURCE_NOT_FOUND));
        } else {
            wc = writingCharacterRepository.findByCharacterAndDeletedFalse(word.getWord().trim()).orElse(null);
        }
        word.setPartOfSpeech(NotebookItemType.KANJI.getLabel());
        if (wc == null) return;

        word.setCharacter(wc);
        word.setWord(wc.getCharacter());
        if (isBlank(word.getReading())) {
            word.setReading(NotebookLessonItemService.primaryKanjiReading(wc));
        }
        if (isBlank(word.getSinoVietnamese()) && !isBlank(wc.getHanViet())) {
            word.setSinoVietnamese(wc.getHanViet().split(",")[0].trim().toUpperCase());
        }
        if (isBlank(word.getMeaning())) {
            word.setMeaning(!isBlank(wc.getMeaningVi()) ? wc.getMeaningVi() : wc.getCharacter());
        }
        if (word.getJlptLevel() == null && wc.getJlptLevel() != null) {
            word.setJlptLevel(wc.getJlptLevel().name());
        }
    }

    /**
     * Liên kết thẻ Ngữ pháp với bản ghi {@code grammar_points} (theo id, hoặc theo tên mẫu)
     * rồi điền các trường còn trống. Cột reading của thẻ ngữ pháp = cấu trúc câu.
     */
    private void applyGrammarSource(NotebookWord word, Long grammarId) {
        GrammarPoint g;
        if (grammarId != null) {
            g = grammarPointRepository.findByIdAndDeletedFalse(grammarId)
                    .orElseThrow(() -> new AppException(ErrorCode.NOTEBOOK_ITEM_SOURCE_NOT_FOUND));
        } else {
            g = grammarPointRepository.findFirstByTitleAndDeletedFalseOrderByIdAsc(word.getWord().trim()).orElse(null);
        }
        word.setPartOfSpeech(NotebookItemType.GRAMMAR.getLabel());
        if (g == null) return;

        word.setGrammar(g);
        word.setWord(truncate(g.getTitle(), 100));
        if (isBlank(word.getReading())) {
            word.setReading(truncate(g.getStructure(), 100));
        }
        if (isBlank(word.getMeaning())) {
            word.setMeaning(!isBlank(g.getMeaningVi()) ? g.getMeaningVi() : g.getTitle());
        }
        if (word.getJlptLevel() == null && g.getJlptLevel() != null) {
            word.setJlptLevel(g.getJlptLevel().name());
        }
        if (isBlank(word.getExampleSentence())) {
            String[] ex = firstGrammarExample(g.getExamplesJson());
            if (ex != null) {
                word.setExampleSentence(ex[0]);
                word.setExampleMeaning(ex[1]);
            }
        }
    }

    /** Thẻ đã liên kết bản ghi từ điển và bản ghi đó đã có trong sổ → trùng. Thẻ tự do không kiểm tra. */
    private boolean isDuplicateInNotebook(Long notebookId, NotebookWord word) {
        return switch (word.getItemType()) {
            case VOCAB -> word.getVocabulary() != null && notebookWordRepository
                    .existsByNotebook_IdAndItemTypeAndVocabulary_Id(notebookId, NotebookItemType.VOCAB, word.getVocabulary().getId());
            case KANJI -> word.getCharacter() != null && notebookWordRepository
                    .existsByNotebook_IdAndItemTypeAndCharacter_Id(notebookId, NotebookItemType.KANJI, word.getCharacter().getId());
            case GRAMMAR -> word.getGrammar() != null && notebookWordRepository
                    .existsByNotebook_IdAndItemTypeAndGrammar_Id(notebookId, NotebookItemType.GRAMMAR, word.getGrammar().getId());
        };
    }

    /** Ví dụ đầu tiên trong {@code grammar_points.examples_json} dạng {@code [{"jp","vi"}]} → [jp, vi]. */
    static String[] firstGrammarExample(JsonNode examplesJson) {
        if (examplesJson == null || !examplesJson.isArray()) return null;
        for (JsonNode node : examplesJson) {
            String jp = node.path("jp").asText(null);
            if (jp != null && !jp.isBlank()) {
                return new String[]{jp, node.path("vi").asText(null)};
            }
        }
        return null;
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
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
    public List<NotebookReviewItemResponse> getReviewItems(Long notebookId, Integer limit, Set<NotebookItemType> types) {
        Long currentUserId = currentUserService.getCurrentUserId();
        int fetchLimit = (limit == null || limit <= 0) ? 20 : Math.min(limit, 50);
        Set<NotebookItemType> typeFilter = (types == null || types.isEmpty())
                ? EnumSet.allOf(NotebookItemType.class)
                : EnumSet.copyOf(types);
        PageRequest duePage = PageRequest.of(0, fetchLimit);

        List<NotebookWord> dueWords;
        List<NotebookWord> fullPool;
        if (notebookId == null || notebookId <= 0) {
            // Luyện tập tất cả các sổ tay của user
            dueWords = notebookWordRepository.findDueWordsByUserId(currentUserId, typeFilter, Instant.now(), duePage);
            fullPool = notebookWordRepository.findPoolByUserId(currentUserId, typeFilter);
        } else {
            notebookRepository.findByIdAndUserIdAndDeletedFalse(notebookId, currentUserId)
                    .orElseThrow(() -> new AppException(ErrorCode.NOTEBOOK_NOT_FOUND));
            dueWords = notebookWordRepository.findDueWordsByNotebookId(notebookId, typeFilter, Instant.now(), duePage);
            fullPool = notebookWordRepository.findPoolByNotebookId(notebookId, typeFilter);
        }

        // 1. Ưu tiên thẻ đến hạn, thiếu thì bổ sung theo thứ tự trong sổ
        List<NotebookWord> targetWords = new ArrayList<>(dueWords);
        Set<Long> targetWordIds = new HashSet<>();
        for (NotebookWord w : dueWords) {
            targetWordIds.add(w.getId());
        }
        for (NotebookWord w : fullPool) {
            if (targetWords.size() >= fetchLimit) break;
            if (targetWordIds.add(w.getId())) {
                targetWords.add(w);
            }
        }

        if (targetWords.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. Pool đáp án nhiễu tách theo loại thẻ: từ vựng chỉ nhiễu bằng từ vựng, chữ Hán bằng chữ Hán...
        Map<NotebookItemType, List<String>> poolMeanings = new EnumMap<>(NotebookItemType.class);
        Map<NotebookItemType, List<String>> poolWords = new EnumMap<>(NotebookItemType.class);
        for (NotebookItemType t : NotebookItemType.values()) {
            List<NotebookWord> ofType = fullPool.stream().filter(w -> w.getItemType() == t).toList();
            poolMeanings.put(t, ofType.stream().map(NotebookWord::getMeaning).filter(m -> m != null && !m.isBlank()).distinct().toList());
            poolWords.put(t, ofType.stream().map(NotebookWord::getWord).filter(w -> w != null && !w.isBlank()).distinct().toList());
        }

        Random rng = new Random();
        List<NotebookReviewItemResponse> result = new ArrayList<>();
        for (NotebookWord word : targetWords) {
            NotebookItemType type = word.getItemType() != null ? word.getItemType() : NotebookItemType.VOCAB;
            List<String> meaningOpts = buildOptions(word.getMeaning(), poolMeanings.get(type), rng);
            List<String> wordOpts = buildOptions(word.getWord(), poolWords.get(type), rng);

            String sv = word.getSinoVietnamese();
            if (type != NotebookItemType.GRAMMAR && (sv == null || sv.trim().isEmpty())) {
                sv = deduceHanViet(word.getWord());
            }

            WritingCharacter wc = word.getCharacter();
            GrammarPoint g = word.getGrammar();
            result.add(NotebookReviewItemResponse.builder()
                    .id(word.getId())
                    .notebookId(word.getNotebook().getId())
                    .itemType(type)
                    .vocabularyId(word.getVocabulary() != null ? word.getVocabulary().getId() : null)
                    .characterId(wc != null ? wc.getId() : null)
                    .grammarId(g != null ? g.getId() : null)
                    .jlptLevel(word.getJlptLevel())
                    .word(word.getWord())
                    // Ngữ pháp: reading là cấu trúc câu → giữ nguyên, không cắt theo dấu phân tách
                    .reading(type == NotebookItemType.GRAMMAR ? word.getReading() : sanitizeReading(word.getReading()))
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
                    .onyomi(wc != null && wc.getOnyomi() != null ? new ArrayList<>(wc.getOnyomi()) : null)
                    .kunyomi(wc != null && wc.getKunyomi() != null ? new ArrayList<>(wc.getKunyomi()) : null)
                    .strokeCount(wc != null ? wc.getStrokeCount() : null)
                    .grammarStructure(g != null ? g.getStructure() : null)
                    .usageNotes(g != null ? g.getUsageNotes() : null)
                    .quizMeaningOptions(meaningOpts)
                    .quizWordOptions(wordOpts)
                    .build());
        }

        return result;
    }

    /** 1 đáp án đúng + tối đa 3 đáp án nhiễu lấy ngẫu nhiên từ pool cùng loại, rồi xáo trộn. */
    private static List<String> buildOptions(String correct, List<String> pool, Random rng) {
        List<String> opts = new ArrayList<>();
        opts.add(correct);
        List<String> shuffled = new ArrayList<>(pool != null ? pool : List.of());
        Collections.shuffle(shuffled, rng);
        for (String candidate : shuffled) {
            if (opts.size() >= 4) break;
            if (!candidate.equals(correct)) {
                opts.add(candidate);
            }
        }
        Collections.shuffle(opts, rng);
        return opts;
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
