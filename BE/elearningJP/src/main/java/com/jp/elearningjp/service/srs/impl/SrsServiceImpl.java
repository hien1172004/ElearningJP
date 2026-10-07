package com.jp.elearningjp.service.srs.impl;

import com.jp.elearningjp.dto.request.srs.SrsBatchCreateRequest;
import com.jp.elearningjp.dto.request.srs.SrsItemCreateRequest;
import com.jp.elearningjp.dto.request.srs.SrsReviewRequest;
import com.jp.elearningjp.dto.response.srs.SrsDueItemResponse;
import com.jp.elearningjp.dto.response.srs.SrsReviewResultResponse;
import com.jp.elearningjp.dto.response.srs.SrsStatsResponse;
import com.jp.elearningjp.entity.content.GrammarPoint;
import com.jp.elearningjp.entity.content.Vocabulary;
import com.jp.elearningjp.entity.content.WritingCharacter;
import com.jp.elearningjp.entity.curriculum.LessonItem;
import com.jp.elearningjp.entity.srs.SrsItem;
import com.jp.elearningjp.entity.srs.SrsReviewLog;
import com.jp.elearningjp.entity.user.User;
import com.jp.elearningjp.exception.AppException;
import com.jp.elearningjp.exception.ErrorCode;
import com.jp.elearningjp.repository.content.GrammarPointRepository;
import com.jp.elearningjp.repository.content.VocabularyRepository;
import com.jp.elearningjp.repository.content.WritingCharacterRepository;
import com.jp.elearningjp.repository.curriculum.LessonItemRepository;
import com.jp.elearningjp.repository.srs.SrsItemRepository;
import com.jp.elearningjp.repository.srs.SrsReviewLogRepository;
import com.jp.elearningjp.repository.user.UserRepository;
import com.jp.elearningjp.service.srs.Sm2Algorithm;
import com.jp.elearningjp.service.srs.SrsService;
import com.jp.elearningjp.shared.enums.SrsCardType;
import com.jp.elearningjp.shared.enums.SrsStatus;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class SrsServiceImpl implements SrsService {

    SrsItemRepository srsItemRepository;
    SrsReviewLogRepository srsReviewLogRepository;
    VocabularyRepository vocabularyRepository;
    WritingCharacterRepository writingCharacterRepository;
    GrammarPointRepository grammarPointRepository;
    LessonItemRepository lessonItemRepository;
    UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<SrsDueItemResponse> getDueCards(Long userId, int limit) {
        int pageLimit = Math.min(Math.max(limit, 1), 100);
        List<SrsItem> dueItems = srsItemRepository.findDueCards(
                userId,
                Instant.now(),
                PageRequest.of(0, pageLimit)
        );

        return dueItems.stream()
                .map(this::mapToDueItemResponse)
                .toList();
    }

    @Override
    @Transactional
    public SrsDueItemResponse addItem(Long userId, SrsItemCreateRequest request) {
        User user = getUser(userId);

        if (request.getVocabId() == null && request.getCharacterId() == null && request.getGrammarId() == null) {
            throw new AppException(ErrorCode.INVALID_SRS_REQUEST);
        }

        SrsItem.SrsItemBuilder builder = SrsItem.builder()
                .user(user)
                .cardType(SrsCardType.STANDARD)
                .repetitionCount(0)
                .lapseCount(0)
                .easeFactor(new BigDecimal("2.50"))
                .intervalDays(0)
                .nextReviewAt(Instant.now())
                .status(SrsStatus.LEARNING);

        if (request.getVocabId() != null) {
            if (srsItemRepository.existsByUserIdAndVocabIdAndDeletedFalse(userId, request.getVocabId())) {
                throw new AppException(ErrorCode.SRS_ITEM_ALREADY_EXISTS);
            }
            Vocabulary vocab = vocabularyRepository.findByIdAndDeletedFalse(request.getVocabId())
                    .orElseThrow(() -> new AppException(ErrorCode.VOCAB_NOT_FOUND));
            builder.vocab(vocab);
        } else if (request.getCharacterId() != null) {
            if (srsItemRepository.existsByUserIdAndCharacterIdAndDeletedFalse(userId, request.getCharacterId())) {
                throw new AppException(ErrorCode.SRS_ITEM_ALREADY_EXISTS);
            }
            WritingCharacter character = writingCharacterRepository.findByIdAndDeletedFalse(request.getCharacterId())
                    .orElseThrow(() -> new AppException(ErrorCode.CHARACTER_NOT_FOUND));
            builder.character(character);
        } else {
            if (srsItemRepository.existsByUserIdAndGrammarIdAndDeletedFalse(userId, request.getGrammarId())) {
                throw new AppException(ErrorCode.SRS_ITEM_ALREADY_EXISTS);
            }
            GrammarPoint grammar = grammarPointRepository.findByIdAndDeletedFalse(request.getGrammarId())
                    .orElseThrow(() -> new AppException(ErrorCode.GRAMMAR_NOT_FOUND));
            builder.grammar(grammar);
        }

        SrsItem savedItem = srsItemRepository.save(builder.build());
        return mapToDueItemResponse(savedItem);
    }

    @Override
    @Transactional
    public int batchAddFromLesson(Long userId, Long lessonId) {
        User user = getUser(userId);
        List<LessonItem> lessonItems = lessonItemRepository.findByLessonIdAndDeletedFalseOrderByOrderIndexAsc(lessonId);

        List<SrsItem> toSave = new ArrayList<>();
        Instant now = Instant.now();

        for (LessonItem item : lessonItems) {
            if (item.getVocab() != null) {
                Long vocabId = item.getVocab().getId();
                if (!srsItemRepository.existsByUserIdAndVocabIdAndDeletedFalse(userId, vocabId)) {
                    toSave.add(SrsItem.builder()
                            .user(user)
                            .vocab(item.getVocab())
                            .nextReviewAt(now)
                            .build());
                }
            } else if (item.getCharacter() != null) {
                Long charId = item.getCharacter().getId();
                if (!srsItemRepository.existsByUserIdAndCharacterIdAndDeletedFalse(userId, charId)) {
                    toSave.add(SrsItem.builder()
                            .user(user)
                            .character(item.getCharacter())
                            .nextReviewAt(now)
                            .build());
                }
            } else if (item.getGrammar() != null) {
                Long grammarId = item.getGrammar().getId();
                if (!srsItemRepository.existsByUserIdAndGrammarIdAndDeletedFalse(userId, grammarId)) {
                    toSave.add(SrsItem.builder()
                            .user(user)
                            .grammar(item.getGrammar())
                            .nextReviewAt(now)
                            .build());
                }
            }
        }

        if (!toSave.isEmpty()) {
            srsItemRepository.saveAll(toSave);
        }
        return toSave.size();
    }

    @Override
    @Transactional
    public int batchAdd(Long userId, SrsBatchCreateRequest request) {
        if (request.getLessonId() != null) {
            return batchAddFromLesson(userId, request.getLessonId());
        }

        User user = getUser(userId);
        List<SrsItem> toSave = new ArrayList<>();
        Instant now = Instant.now();

        if (request.getVocabIds() != null) {
            for (Long vocabId : request.getVocabIds()) {
                if (!srsItemRepository.existsByUserIdAndVocabIdAndDeletedFalse(userId, vocabId)) {
                    vocabularyRepository.findByIdAndDeletedFalse(vocabId).ifPresent(v -> {
                        toSave.add(SrsItem.builder().user(user).vocab(v).nextReviewAt(now).build());
                    });
                }
            }
        }

        if (request.getCharacterIds() != null) {
            for (Long charId : request.getCharacterIds()) {
                if (!srsItemRepository.existsByUserIdAndCharacterIdAndDeletedFalse(userId, charId)) {
                    writingCharacterRepository.findByIdAndDeletedFalse(charId).ifPresent(c -> {
                        toSave.add(SrsItem.builder().user(user).character(c).nextReviewAt(now).build());
                    });
                }
            }
        }

        if (request.getGrammarIds() != null) {
            for (Long grammarId : request.getGrammarIds()) {
                if (!srsItemRepository.existsByUserIdAndGrammarIdAndDeletedFalse(userId, grammarId)) {
                    grammarPointRepository.findByIdAndDeletedFalse(grammarId).ifPresent(g -> {
                        toSave.add(SrsItem.builder().user(user).grammar(g).nextReviewAt(now).build());
                    });
                }
            }
        }

        if (!toSave.isEmpty()) {
            srsItemRepository.saveAll(toSave);
        }
        return toSave.size();
    }

    @Override
    @Transactional
    public SrsReviewResultResponse reviewCard(Long userId, Long srsItemId, SrsReviewRequest request) {
        User user = getUser(userId);

        SrsItem srsItem = srsItemRepository.findByIdAndUserIdAndDeletedFalse(srsItemId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.SRS_ITEM_NOT_FOUND));

        BigDecimal prevEf = srsItem.getEaseFactor();
        Integer prevInterval = srsItem.getIntervalDays();

        // Áp dụng thuật toán SM-2
        Sm2Algorithm.CalculationResult calc = Sm2Algorithm.calculate(
                prevEf,
                prevInterval,
                srsItem.getRepetitionCount(),
                srsItem.getLapseCount(),
                request.getRating()
        );

        // Cập nhật SrsItem
        srsItem.setEaseFactor(calc.getNewEf());
        srsItem.setIntervalDays(calc.getNewInterval());
        srsItem.setRepetitionCount(calc.getNewRepetitionCount());
        srsItem.setLapseCount(calc.getNewLapseCount());
        srsItem.setStatus(calc.getNewStatus());
        srsItem.setNextReviewAt(calc.getNextReviewAt());
        srsItem.setLastReviewedAt(Instant.now());
        srsItemRepository.save(srsItem);

        // Ghi Log vào srs_review_logs
        SrsReviewLog logEntity = SrsReviewLog.builder()
                .srsItem(srsItem)
                .user(user)
                .rating(request.getRating().name())
                .scoreQ(request.getRating().getQualityScore())
                .previousInterval(prevInterval)
                .newInterval(calc.getNewInterval())
                .previousEf(prevEf)
                .newEf(calc.getNewEf())
                .newStatus(calc.getNewStatus())
                .responseTimeMs(request.getResponseTimeMs())
                .clientEventId(request.getClientEventId())
                .reviewedAt(Instant.now())
                .build();
        srsReviewLogRepository.save(logEntity);

        // Điểm XP thưởng tương ứng theo độ tích cực
        int xpEarned = switch (request.getRating()) {
            case AGAIN -> 1;
            case HARD -> 2;
            case GOOD -> 3;
            case EASY -> 5;
        };

        return SrsReviewResultResponse.builder()
                .srsItemId(srsItem.getId())
                .rating(request.getRating().name())
                .previousInterval(prevInterval)
                .newInterval(calc.getNewInterval())
                .previousEf(prevEf)
                .newEf(calc.getNewEf())
                .newStatus(calc.getNewStatus())
                .nextReviewAt(calc.getNextReviewAt())
                .xpEarned(xpEarned)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public SrsStatsResponse getStatistics(Long userId) {
        long totalItems = srsItemRepository.countByUserIdAndDeletedFalse(userId);
        long dueToday = srsItemRepository.countDueCards(userId, Instant.now());
        long learning = srsItemRepository.countByUserIdAndStatusAndDeletedFalse(userId, SrsStatus.LEARNING);
        long reviewing = srsItemRepository.countByUserIdAndStatusAndDeletedFalse(userId, SrsStatus.REVIEWING);
        long mastered = srsItemRepository.countByUserIdAndStatusAndDeletedFalse(userId, SrsStatus.MASTERED);

        return SrsStatsResponse.builder()
                .totalItems(totalItems)
                .dueTodayCount(dueToday)
                .learningCount(learning)
                .reviewingCount(reviewing)
                .masteredCount(mastered)
                .build();
    }

    private SrsDueItemResponse mapToDueItemResponse(SrsItem item) {
        SrsDueItemResponse.SrsDueItemResponseBuilder builder = SrsDueItemResponse.builder()
                .srsItemId(item.getId())
                .cardType(item.getCardType())
                .status(item.getStatus())
                .intervalDays(item.getIntervalDays())
                .repetitionCount(item.getRepetitionCount())
                .easeFactor(item.getEaseFactor())
                .nextReviewAt(item.getNextReviewAt());

        if (item.getVocab() != null) {
            Vocabulary v = item.getVocab();
            builder.itemType("VOCABULARY")
                    .referenceId(v.getId())
                    .frontText(v.getWord())
                    .backMeaning(v.getMeaningVi())
                    .reading(v.getHiragana() != null ? v.getHiragana() : v.getRomaji())
                    .audioUrl(v.getAudioUrl())
                    .exampleSentence(v.getExampleJp())
                    .exampleTranslation(v.getExampleVi());
        } else if (item.getCharacter() != null) {
            WritingCharacter c = item.getCharacter();
            String readingStr = "";
            if (c.getOnyomi() != null && !c.getOnyomi().isEmpty()) {
                readingStr += "On: " + String.join(", ", c.getOnyomi()) + " ";
            }
            if (c.getKunyomi() != null && !c.getKunyomi().isEmpty()) {
                readingStr += "Kun: " + String.join(", ", c.getKunyomi());
            }

            builder.itemType("CHARACTER")
                    .referenceId(c.getId())
                    .frontText(c.getCharacter())
                    .backMeaning(c.getMeaningVi() + (c.getHanViet() != null ? " (Hán Việt: " + c.getHanViet() + ")" : ""))
                    .reading(readingStr.trim());
        } else if (item.getGrammar() != null) {
            GrammarPoint g = item.getGrammar();
            builder.itemType("GRAMMAR")
                    .referenceId(g.getId())
                    .frontText(g.getTitle())
                    .backMeaning(g.getMeaningVi())
                    .reading(g.getStructure());
        }

        return builder.build();
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }

    public User getCurrentUserRequired() {
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        return userRepository.findUserByEmail(auth.getName())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SrsDueItemResponse> getDueCards(int limit) {
        return getDueCards(getCurrentUserRequired().getId(), limit);
    }

    @Override
    @Transactional
    public SrsDueItemResponse addItem(SrsItemCreateRequest request) {
        return addItem(getCurrentUserRequired().getId(), request);
    }

    @Override
    @Transactional
    public int batchAddFromLesson(Long lessonId) {
        return batchAddFromLesson(getCurrentUserRequired().getId(), lessonId);
    }

    @Override
    @Transactional
    public int batchAdd(SrsBatchCreateRequest request) {
        return batchAdd(getCurrentUserRequired().getId(), request);
    }

    @Override
    @Transactional
    public SrsReviewResultResponse reviewCard(Long srsItemId, SrsReviewRequest request) {
        return reviewCard(getCurrentUserRequired().getId(), srsItemId, request);
    }

    @Override
    @Transactional(readOnly = true)
    public SrsStatsResponse getStatistics() {
        return getStatistics(getCurrentUserRequired().getId());
    }
}
