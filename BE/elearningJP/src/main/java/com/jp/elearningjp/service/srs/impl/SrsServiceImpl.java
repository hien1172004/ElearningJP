package com.jp.elearningjp.service.srs.impl;

import com.jp.elearningjp.config.SrsProperties;
import com.jp.elearningjp.config.TimeConfig;
import com.jp.elearningjp.dto.request.srs.SrsBatchCreateRequest;
import com.jp.elearningjp.dto.request.srs.SrsItemCreateRequest;
import com.jp.elearningjp.dto.request.srs.SrsReviewRequest;
import com.jp.elearningjp.dto.response.srs.SrsDueItemResponse;
import com.jp.elearningjp.dto.response.srs.SrsForecastDay;
import com.jp.elearningjp.dto.response.srs.SrsForecastResponse;
import com.jp.elearningjp.dto.response.srs.SrsSavedIdsResponse;
import com.jp.elearningjp.dto.response.srs.SrsReviewResultResponse;
import com.jp.elearningjp.dto.response.srs.SrsStatsResponse;
import com.jp.elearningjp.entity.content.GrammarPoint;
import com.jp.elearningjp.entity.content.Vocabulary;
import com.jp.elearningjp.entity.content.WritingCharacter;
import com.jp.elearningjp.entity.curriculum.LessonSection;
import com.jp.elearningjp.entity.srs.SrsItem;
import com.jp.elearningjp.entity.srs.SrsReviewLog;
import com.jp.elearningjp.entity.user.User;
import com.jp.elearningjp.exception.AppException;
import com.jp.elearningjp.exception.ErrorCode;
import com.jp.elearningjp.repository.content.GrammarPointRepository;
import com.jp.elearningjp.repository.content.VocabularyRepository;
import com.jp.elearningjp.repository.content.WritingCharacterRepository;
import com.jp.elearningjp.repository.curriculum.LessonGrammarItemRepository;
import com.jp.elearningjp.repository.curriculum.LessonKanjiItemRepository;
import com.jp.elearningjp.repository.curriculum.LessonSectionRepository;
import com.jp.elearningjp.repository.curriculum.LessonVocabularyItemRepository;
import com.jp.elearningjp.repository.srs.SrsItemRepository;
import com.jp.elearningjp.repository.srs.SrsReviewLogRepository;
import com.jp.elearningjp.repository.user.UserRepository;
import com.jp.elearningjp.service.srs.Sm2Algorithm;
import com.jp.elearningjp.service.srs.SrsService;
import com.jp.elearningjp.shared.enums.SrsCardType;
import com.jp.elearningjp.shared.enums.SrsStatus;
import com.jp.elearningjp.shared.response.PageResponse;
import com.jp.elearningjp.shared.util.CurrentUserService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;

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
    LessonSectionRepository lessonSectionRepository;
    LessonKanjiItemRepository lessonKanjiItemRepository;
    LessonVocabularyItemRepository lessonVocabularyItemRepository;
    LessonGrammarItemRepository lessonGrammarItemRepository;
    UserRepository userRepository;
    CurrentUserService currentUserService;
    SrsProperties srsProperties;
    Clock clock;

    @Override
    @Transactional(readOnly = true)
    public List<SrsDueItemResponse> getDueCards(Long userId, int limit) {
        int pageLimit = Math.min(Math.max(limit, 1), 100);

        // 1. Thẻ ôn đến hạn (LEARNING / REVIEWING / RELEARNING / MASTERED) được ưu tiên
        List<SrsItem> cards = new ArrayList<>(srsItemRepository.findDueCards(
                userId, endOfToday(), PageRequest.of(0, pageLimit)));

        // 2. Bù thêm thẻ NEW trong phạm vi hạn mức thẻ mới còn lại của hôm nay
        int remainingSlots = pageLimit - cards.size();
        int newQuota = remainingNewQuota(userId);
        if (remainingSlots > 0 && newQuota > 0) {
            cards.addAll(srsItemRepository.findNewCards(
                    userId, PageRequest.of(0, Math.min(remainingSlots, newQuota))));
        }

        return cards.stream().map(this::mapToDueItemResponse).toList();
    }

    @Override
    @Transactional
    public SrsDueItemResponse addItem(Long userId, SrsItemCreateRequest request) {
        User user = getUser(userId);

        if (request.getVocabId() == null && request.getCharacterId() == null && request.getGrammarId() == null) {
            throw new AppException(ErrorCode.INVALID_SRS_REQUEST);
        }

        Optional<SrsItem> existing;
        Supplier<SrsItem> creator;
        if (request.getVocabId() != null) {
            Vocabulary vocab = vocabularyRepository.findByIdAndDeletedFalse(request.getVocabId())
                    .orElseThrow(() -> new AppException(ErrorCode.VOCAB_NOT_FOUND));
            existing = srsItemRepository.findByUserIdAndVocabIdAndDeletedFalse(userId, vocab.getId());
            creator = () -> newCard(user).vocab(vocab).build();
        } else if (request.getCharacterId() != null) {
            WritingCharacter character = writingCharacterRepository.findByIdAndDeletedFalse(request.getCharacterId())
                    .orElseThrow(() -> new AppException(ErrorCode.CHARACTER_NOT_FOUND));
            existing = srsItemRepository.findByUserIdAndCharacterIdAndDeletedFalse(userId, character.getId());
            creator = () -> newCard(user).character(character).build();
        } else {
            GrammarPoint grammar = grammarPointRepository.findByIdAndDeletedFalse(request.getGrammarId())
                    .orElseThrow(() -> new AppException(ErrorCode.GRAMMAR_NOT_FOUND));
            existing = srsItemRepository.findByUserIdAndGrammarIdAndDeletedFalse(userId, grammar.getId());
            creator = () -> newCard(user).grammar(grammar).build();
        }

        List<SrsItem> toSave = new ArrayList<>(1);
        if (!addOrReactivate(existing, creator, toSave)) {
            throw new AppException(ErrorCode.SRS_ITEM_ALREADY_EXISTS);
        }
        return mapToDueItemResponse(srsItemRepository.save(toSave.get(0)));
    }

    @Override
    @Transactional
    public int batchAddFromLesson(Long userId, Long lessonId) {
        User user = getUser(userId);

        // Đọc tất cả section (chưa xoá) của lesson, sắp xếp theo orderIndex
        List<LessonSection> sections = lessonSectionRepository
                .findByLessonIdAndDeletedFalseOrderByOrderIndexAsc(lessonId);

        List<SrsItem> toSave = new ArrayList<>();

        for (LessonSection section : sections) {
            switch (section.getSectionType()) {
                case KANJI -> lessonKanjiItemRepository
                        .findBySectionIdAndDeletedFalseOrderByOrderIndexAsc(section.getId())
                        .forEach(item -> addCharacter(user, item.getCharacter(), toSave));
                case VOCABULARY -> lessonVocabularyItemRepository
                        .findBySectionIdAndDeletedFalseOrderByOrderIndexAsc(section.getId())
                        .forEach(item -> addVocab(user, item.getVocab(), toSave));
                case GRAMMAR -> lessonGrammarItemRepository
                        .findBySectionIdAndDeletedFalseOrderByOrderIndexAsc(section.getId())
                        .forEach(item -> addGrammar(user, item.getGrammar(), toSave));
                default -> { /* section khác không tạo thẻ SRS */ }
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

        if (request.getVocabIds() != null) {
            for (Long vocabId : request.getVocabIds()) {
                vocabularyRepository.findByIdAndDeletedFalse(vocabId)
                        .ifPresent(v -> addVocab(user, v, toSave));
            }
        }
        if (request.getCharacterIds() != null) {
            for (Long charId : request.getCharacterIds()) {
                writingCharacterRepository.findByIdAndDeletedFalse(charId)
                        .ifPresent(c -> addCharacter(user, c, toSave));
            }
        }
        if (request.getGrammarIds() != null) {
            for (Long grammarId : request.getGrammarIds()) {
                grammarPointRepository.findByIdAndDeletedFalse(grammarId)
                        .ifPresent(g -> addGrammar(user, g, toSave));
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
        if (srsItem.getStatus() == SrsStatus.ARCHIVED) {
            throw new AppException(ErrorCode.SRS_ITEM_ARCHIVED);
        }

        BigDecimal prevEf = srsItem.getEaseFactor();
        Integer prevInterval = srsItem.getIntervalDays();
        Instant now = Instant.now(clock);

        Sm2Algorithm.CalculationResult calc = Sm2Algorithm.calculate(
                prevEf,
                prevInterval,
                srsItem.getRepetitionCount(),
                srsItem.getLapseCount(),
                srsItem.getStatus(),
                request.getRating(),
                clock,
                appZone()
        );

        // Lần ôn đầu tiên: thẻ rời trạng thái NEW → tính vào hạn mức thẻ mới hôm nay
        if (srsItem.getIntroducedAt() == null) {
            srsItem.setIntroducedAt(now);
        }
        srsItem.setEaseFactor(calc.getNewEf());
        srsItem.setIntervalDays(calc.getNewInterval());
        srsItem.setRepetitionCount(calc.getNewRepetitionCount());
        srsItem.setLapseCount(calc.getNewLapseCount());
        srsItem.setStatus(calc.getNewStatus());
        srsItem.setNextReviewAt(calc.getNextReviewAt());
        srsItem.setLastReviewedAt(now);
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
                .reviewedAt(now)
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
    @Transactional
    public void archiveItem(Long userId, Long srsItemId) {
        SrsItem item = srsItemRepository.findByIdAndUserIdAndDeletedFalse(srsItemId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.SRS_ITEM_NOT_FOUND));
        if (item.getStatus() != SrsStatus.ARCHIVED) {
            item.setStatus(SrsStatus.ARCHIVED);
            srsItemRepository.save(item);
        }
    }

    @Override
    @Transactional
    public SrsDueItemResponse restoreItem(Long userId, Long srsItemId) {
        SrsItem item = srsItemRepository.findByIdAndUserIdAndDeletedFalse(srsItemId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.SRS_ITEM_NOT_FOUND));
        if (item.getStatus() == SrsStatus.ARCHIVED) {
            resetToNew(item);
            item = srsItemRepository.save(item);
        }
        return mapToDueItemResponse(item);
    }

    @Override
    @Transactional(readOnly = true)
    public SrsStatsResponse getStatistics(Long userId) {
        long newCount = srsItemRepository.countByUserIdAndStatusAndDeletedFalse(userId, SrsStatus.NEW);
        long newToday = Math.min(newCount, remainingNewQuota(userId));

        return SrsStatsResponse.builder()
                .totalItems(srsItemRepository.countActiveItems(userId))
                .dueTodayCount(srsItemRepository.countDueCards(userId, endOfToday()) + newToday)
                .newTodayCount(newToday)
                .newCount(newCount)
                .learningCount(srsItemRepository.countByUserIdAndStatusAndDeletedFalse(userId, SrsStatus.LEARNING))
                .reviewingCount(srsItemRepository.countByUserIdAndStatusAndDeletedFalse(userId, SrsStatus.REVIEWING))
                .relearningCount(srsItemRepository.countByUserIdAndStatusAndDeletedFalse(userId, SrsStatus.RELEARNING))
                .masteredCount(srsItemRepository.countByUserIdAndStatusAndDeletedFalse(userId, SrsStatus.MASTERED))
                .archivedCount(srsItemRepository.countByUserIdAndStatusAndDeletedFalse(userId, SrsStatus.ARCHIVED))
                .build();
    }

    // ------------------------------------------------------------------
    // Helpers: tạo / kích hoạt lại thẻ
    // ------------------------------------------------------------------

    /** Builder thẻ mới với giá trị khởi tạo chuẩn. */
    private SrsItem.SrsItemBuilder newCard(User user) {
        return SrsItem.builder()
                .user(user)
                .cardType(SrsCardType.STANDARD)
                .repetitionCount(0)
                .lapseCount(0)
                .easeFactor(Sm2Algorithm.DEFAULT_EF)
                .intervalDays(0)
                .nextReviewAt(Instant.now(clock))
                .status(SrsStatus.NEW);
    }

    /**
     * Thêm thẻ vào {@code toSave} nếu chưa có, hoặc kích hoạt lại nếu đang ARCHIVED.
     *
     * @return {@code false} nếu thẻ đang hoạt động (đã tồn tại)
     */
    private boolean addOrReactivate(Optional<SrsItem> existing, Supplier<SrsItem> creator, List<SrsItem> toSave) {
        if (existing.isEmpty()) {
            toSave.add(creator.get());
            return true;
        }
        SrsItem item = existing.get();
        if (item.getStatus() == SrsStatus.ARCHIVED) {
            resetToNew(item);
            toSave.add(item);
            return true;
        }
        return false;
    }

    /** Đưa thẻ lưu trữ về NEW: học lại từ đầu, giữ lapseCount và lịch sử ôn để thống kê. */
    private void resetToNew(SrsItem item) {
        item.setStatus(SrsStatus.NEW);
        item.setRepetitionCount(0);
        item.setIntervalDays(0);
        item.setEaseFactor(Sm2Algorithm.DEFAULT_EF);
        item.setNextReviewAt(Instant.now(clock));
        item.setIntroducedAt(null);
    }

    private void addVocab(User user, Vocabulary vocab, List<SrsItem> toSave) {
        if (alreadyQueued(toSave, s -> s.getVocab() != null && s.getVocab().getId().equals(vocab.getId()))) return;
        addOrReactivate(srsItemRepository.findByUserIdAndVocabIdAndDeletedFalse(user.getId(), vocab.getId()),
                () -> newCard(user).vocab(vocab).build(), toSave);
    }

    private void addCharacter(User user, WritingCharacter character, List<SrsItem> toSave) {
        if (alreadyQueued(toSave, s -> s.getCharacter() != null && s.getCharacter().getId().equals(character.getId()))) return;
        addOrReactivate(srsItemRepository.findByUserIdAndCharacterIdAndDeletedFalse(user.getId(), character.getId()),
                () -> newCard(user).character(character).build(), toSave);
    }

    private void addGrammar(User user, GrammarPoint grammar, List<SrsItem> toSave) {
        if (alreadyQueued(toSave, s -> s.getGrammar() != null && s.getGrammar().getId().equals(grammar.getId()))) return;
        addOrReactivate(srsItemRepository.findByUserIdAndGrammarIdAndDeletedFalse(user.getId(), grammar.getId()),
                () -> newCard(user).grammar(grammar).build(), toSave);
    }

    /** Tránh tạo trùng khi cùng một mục xuất hiện nhiều lần trong một lô. */
    private boolean alreadyQueued(List<SrsItem> toSave, Predicate<SrsItem> sameTarget) {
        return toSave.stream().anyMatch(sameTarget);
    }

    // ------------------------------------------------------------------
    // Helpers: thời gian & hạn mức
    // ------------------------------------------------------------------

    private ZoneId appZone() {
        return ZoneId.of(TimeConfig.APP_ZONE);
    }

    private Instant startOfToday() {
        return LocalDate.now(clock).atStartOfDay(appZone()).toInstant();
    }

    private Instant endOfToday() {
        return LocalDate.now(clock).atTime(23, 59, 59, 999_000_000).atZone(appZone()).toInstant();
    }

    /** Số thẻ NEW còn được phép học hôm nay. */
    private int remainingNewQuota(Long userId) {
        long introducedToday = srsItemRepository.countIntroducedSince(userId, startOfToday());
        return (int) Math.max(0, srsProperties.getNewCardsPerDay() - introducedToday);
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

    @Override
    @Transactional(readOnly = true)
    public List<SrsDueItemResponse> getDueCards(int limit) {
        return getDueCards(currentUserService.getCurrentUserId(), limit);
    }

    @Override
    @Transactional
    public SrsDueItemResponse addItem(SrsItemCreateRequest request) {
        return addItem(currentUserService.getCurrentUserId(), request);
    }

    @Override
    @Transactional
    public int batchAddFromLesson(Long lessonId) {
        return batchAddFromLesson(currentUserService.getCurrentUserId(), lessonId);
    }

    @Override
    @Transactional
    public int batchAdd(SrsBatchCreateRequest request) {
        return batchAdd(currentUserService.getCurrentUserId(), request);
    }

    @Override
    @Transactional
    public SrsReviewResultResponse reviewCard(Long srsItemId, SrsReviewRequest request) {
        return reviewCard(currentUserService.getCurrentUserId(), srsItemId, request);
    }

    @Override
    @Transactional
    public void archiveItem(Long srsItemId) {
        archiveItem(currentUserService.getCurrentUserId(), srsItemId);
    }

    @Override
    @Transactional
    public SrsDueItemResponse restoreItem(Long srsItemId) {
        return restoreItem(currentUserService.getCurrentUserId(), srsItemId);
    }

    @Override
    @Transactional(readOnly = true)
    public SrsStatsResponse getStatistics() {
        return getStatistics(currentUserService.getCurrentUserId());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<SrsDueItemResponse> getItems(Long userId, SrsStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size));
        Page<SrsItem> result = srsItemRepository.findByUserIdAndStatus(userId, status, pageable);

        List<SrsDueItemResponse> items = result.getContent().stream()
                .map(this::mapToDueItemResponse)
                .toList();

        return PageResponse.<SrsDueItemResponse>builder()
                .items(items)
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<SrsDueItemResponse> getItems(SrsStatus status, int page, int size) {
        return getItems(currentUserService.getCurrentUserId(), status, page, size);
    }

    @Override
    @Transactional(readOnly = true)
    public SrsForecastResponse getForecast(Long userId) {
        ZoneId zone = appZone();
        LocalDate today = LocalDate.now(clock);
        LocalDate monday = today.with(DayOfWeek.MONDAY);
        // Thẻ NEW chỉ tính vào hôm nay, trong phạm vi hạn mức còn lại
        long newToday = Math.min(
                srsItemRepository.countByUserIdAndStatusAndDeletedFalse(userId, SrsStatus.NEW),
                remainingNewQuota(userId));

        // Lịch tuần: Thứ 2 → Chủ nhật của tuần hiện tại
        List<SrsForecastDay> days = new ArrayList<>(7);
        long prevCumulative = 0;
        for (int i = 0; i < 7; i++) {
            LocalDate day = monday.plusDays(i);
            Instant dayStart = day.atStartOfDay(zone).toInstant();
            Instant nextDayStart = day.plusDays(1).atStartOfDay(zone).toInstant();
            boolean isPast = day.isBefore(today);
            boolean isToday = day.equals(today);

            int due = 0;
            if (!isPast) {
                Instant endOfDay = day.atTime(23, 59, 59, 999_000_000).atZone(zone).toInstant();
                long cumulative = srsItemRepository.countDueCards(userId, endOfDay);
                // Hôm nay: toàn bộ thẻ đến hạn (gồm quá hạn) + thẻ mới; ngày sau: phần tăng thêm
                due = (int) (cumulative - prevCumulative + (isToday ? newToday : 0));
                prevCumulative = cumulative;
            }
            int reviewed = day.isAfter(today) ? 0
                    : (int) srsReviewLogRepository.countReviewedItemsBetween(userId, dayStart, nextDayStart);

            days.add(SrsForecastDay.builder()
                    .date(day)
                    .count(due)
                    .reviewedCount(reviewed)
                    .today(isToday)
                    .past(isPast)
                    .build());
        }

        return SrsForecastResponse.builder().days(days).build();
    }

    @Override
    @Transactional(readOnly = true)
    public SrsForecastResponse getForecast() {
        return getForecast(currentUserService.getCurrentUserId());
    }

    @Override
    @Transactional(readOnly = true)
    public SrsSavedIdsResponse getSavedIds(Long userId) {
        return SrsSavedIdsResponse.builder()
                .vocabIds(srsItemRepository.findSavedVocabIds(userId))
                .characterIds(srsItemRepository.findSavedCharacterIds(userId))
                .grammarIds(srsItemRepository.findSavedGrammarIds(userId))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public SrsSavedIdsResponse getSavedIds() {
        return getSavedIds(currentUserService.getCurrentUserId());
    }
}
