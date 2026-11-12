package com.jp.elearningjp.service.notebook;

import com.jp.elearningjp.dto.request.notebook.SaveLessonItemsRequest;
import com.jp.elearningjp.dto.response.notebook.NotebookSavedIdsResponse;
import com.jp.elearningjp.dto.response.notebook.SaveLessonItemsResponse;
import com.jp.elearningjp.entity.content.GrammarPoint;
import com.jp.elearningjp.entity.content.Vocabulary;
import com.jp.elearningjp.entity.content.WritingCharacter;
import com.jp.elearningjp.entity.curriculum.Lesson;
import com.jp.elearningjp.entity.curriculum.LessonGrammarExample;
import com.jp.elearningjp.entity.curriculum.LessonGrammarItem;
import com.jp.elearningjp.entity.curriculum.LessonKanjiItem;
import com.jp.elearningjp.entity.curriculum.LessonKanjiRelatedVocab;
import com.jp.elearningjp.entity.curriculum.LessonSection;
import com.jp.elearningjp.entity.curriculum.LessonVocabularyExample;
import com.jp.elearningjp.entity.curriculum.LessonVocabularyItem;
import com.jp.elearningjp.entity.notebook.Notebook;
import com.jp.elearningjp.entity.notebook.NotebookGroup;
import com.jp.elearningjp.entity.notebook.NotebookWord;
import com.jp.elearningjp.entity.user.User;
import com.jp.elearningjp.exception.AppException;
import com.jp.elearningjp.exception.ErrorCode;
import com.jp.elearningjp.repository.curriculum.LessonRepository;
import com.jp.elearningjp.repository.curriculum.LessonSectionRepository;
import com.jp.elearningjp.repository.notebook.NotebookGroupRepository;
import com.jp.elearningjp.repository.notebook.NotebookRepository;
import com.jp.elearningjp.repository.notebook.NotebookWordRepository;
import com.jp.elearningjp.service.curriculum.SectionService;
import com.jp.elearningjp.shared.enums.LessonSectionType;
import com.jp.elearningjp.shared.enums.NotebookItemType;
import com.jp.elearningjp.shared.util.CourseAccessGuard;
import com.jp.elearningjp.shared.util.CurrentUserService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Lưu kiến thức của bài học (Chữ Hán / Từ vựng / Ngữ pháp) vào sổ tay.
 *
 * <ul>
 *   <li>Quyền: phải xem được nội dung bài (admin, chủ khóa, học viên đã đăng ký).</li>
 *   <li>Lưu đúng phiên bản giáo viên soạn trong bài: nghĩa / cách đọc / cấu trúc ghi đè, câu ví dụ, ghi chú.</li>
 *   <li>Mục đã có trong sổ (cùng bản ghi từ điển + cùng loại) → bỏ qua, không báo lỗi.</li>
 *   <li>Thứ tự thẻ = thứ tự trong bài (nhóm → mục).</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class NotebookLessonItemService {

    static final int MAX_ITEMS_PER_SAVE = 500;
    private static final String SHARE_CODE_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    LessonRepository lessonRepository;
    LessonSectionRepository lessonSectionRepository;
    SectionService sectionService;
    com.jp.elearningjp.service.curriculum.LessonDraftService lessonDraftService;
    CourseAccessGuard courseAccessGuard;
    CurrentUserService currentUserService;
    NotebookRepository notebookRepository;
    NotebookGroupRepository notebookGroupRepository;
    NotebookWordRepository notebookWordRepository;
    NotebookWordCounter notebookWordCounter;

    @Transactional
    public SaveLessonItemsResponse saveLessonItems(SaveLessonItemsRequest request) {
        Lesson lesson = lessonRepository.findByIdAndDeletedFalse(request.getLessonId())
                .orElseThrow(() -> new AppException(ErrorCode.LESSON_NOT_FOUND));
        courseAccessGuard.requireLessonContentAccess(lesson);
        User user = currentUserService.getCurrentUser();

        // 1. Gom các mục cần lưu theo đúng thứ tự trong bài
        // Người quản lý: nháp phía mình; còn lại: nháp TEACHER (V65)
        List<LessonSection> sections = lessonSectionRepository.findByDraftIdWithAllItems(lessonDraftService.readDraftId(lesson));
        SectionService.RelatedVocabBundle bundle = sectionService.loadNestedCollections(sections);
        List<NotebookWord> candidates = collectCandidates(sections, bundle, request, lesson);
        if (candidates.isEmpty()) {
            throw new AppException(ErrorCode.NOTEBOOK_LESSON_ITEMS_EMPTY);
        }
        if (candidates.size() > MAX_ITEMS_PER_SAVE) {
            throw new AppException(ErrorCode.NOTEBOOK_LESSON_ITEMS_LIMIT_EXCEEDED);
        }

        // 2. Sổ tay đích: sổ đã có hoặc tạo mới
        boolean created = request.getNotebookId() == null;
        Notebook notebook = created
                ? createNotebook(user, request, lesson)
                : notebookRepository.findByIdAndUserIdAndDeletedFalse(request.getNotebookId(), user.getId())
                        .orElseThrow(() -> new AppException(ErrorCode.NOTEBOOK_NOT_FOUND));

        // 3. Bỏ qua mục sổ đã có (so theo bản ghi từ điển)
        Set<Long> vocabIds = created ? new HashSet<>() : new HashSet<>(notebookWordRepository.findVocabularyIdsInNotebook(notebook.getId()));
        Set<Long> charIds = created ? new HashSet<>() : new HashSet<>(notebookWordRepository.findCharacterIdsInNotebook(notebook.getId()));
        Set<Long> grammarIds = created ? new HashSet<>() : new HashSet<>(notebookWordRepository.findGrammarIdsInNotebook(notebook.getId()));
        int nextOrder = created ? 0 : (int) notebookWordRepository.countByNotebookIdAndDeletedFalse(notebook.getId());

        List<NotebookWord> toSave = new ArrayList<>();
        int skipped = 0, addedVocab = 0, addedKanji = 0, addedGrammar = 0;
        Instant now = Instant.now();
        for (NotebookWord w : candidates) {
            boolean isNew = switch (w.getItemType()) {
                case VOCAB -> vocabIds.add(w.getVocabulary().getId());
                case KANJI -> charIds.add(w.getCharacter().getId());
                case GRAMMAR -> grammarIds.add(w.getGrammar().getId());
            };
            if (!isNew) {
                skipped++;
                continue;
            }
            w.setNotebook(notebook);
            w.setDisplayOrder(nextOrder++);
            w.setNextReviewAt(now);
            toSave.add(w);
            switch (w.getItemType()) {
                case VOCAB -> addedVocab++;
                case KANJI -> addedKanji++;
                case GRAMMAR -> addedGrammar++;
            }
        }

        if (!toSave.isEmpty()) {
            notebookWordRepository.saveAll(toSave);
        }
        notebookWordCounter.refresh(notebook);

        log.info("[NOTEBOOK] User id={} saved lesson id={} → notebook id={} (added={}, skipped={})",
                user.getId(), lesson.getId(), notebook.getId(), toSave.size(), skipped);

        return SaveLessonItemsResponse.builder()
                .notebookId(notebook.getId())
                .notebookName(notebook.getName())
                .createdNotebook(created)
                .addedCount(toSave.size())
                .skippedCount(skipped)
                .addedVocab(addedVocab)
                .addedKanji(addedKanji)
                .addedGrammar(addedGrammar)
                .build();
    }

    @Transactional(readOnly = true)
    public NotebookSavedIdsResponse getSavedIds() {
        Long userId = currentUserService.getCurrentUserId();
        return NotebookSavedIdsResponse.builder()
                .vocabularyIds(notebookWordRepository.findSavedVocabularyIdsByUserId(userId))
                .characterIds(notebookWordRepository.findSavedCharacterIdsByUserId(userId))
                .grammarIds(notebookWordRepository.findSavedGrammarIdsByUserId(userId))
                .build();
    }

    // =========================================================
    // Gom mục trong bài
    // =========================================================

    private List<NotebookWord> collectCandidates(List<LessonSection> sections, SectionService.RelatedVocabBundle bundle,
                                                 SaveLessonItemsRequest request, Lesson lesson) {
        Set<String> pickedItems = new HashSet<>();
        if (request.getItems() != null) {
            request.getItems().forEach(ref -> pickedItems.add(ref.getType() + "-" + ref.getItemId()));
        }
        Set<Long> pickedSections = request.getSectionIds() != null ? new HashSet<>(request.getSectionIds()) : Set.of();

        List<NotebookWord> out = new ArrayList<>();
        sections.stream()
                .filter(s -> !s.isDeleted())
                .sorted(Comparator.comparing(LessonSection::getOrderIndex, Comparator.nullsLast(Integer::compareTo)))
                .forEach(section -> {
                    boolean wholeSection = pickedItems.isEmpty()
                            && (pickedSections.isEmpty() || pickedSections.contains(section.getId()));
                    switch (section.getSectionType()) {
                        case VOCABULARY -> sorted(section.getVocabularyItems(), LessonVocabularyItem::getOrderIndex).stream()
                                .filter(i -> i.getVocab() != null)
                                .filter(i -> wholeSection || pickedItems.contains(LessonSectionType.VOCABULARY + "-" + i.getId()))
                                .forEach(i -> out.add(fromVocabItem(i, lesson)));
                        case KANJI -> sorted(section.getKanjiItems(), LessonKanjiItem::getOrderIndex).stream()
                                .filter(i -> i.getCharacter() != null)
                                .filter(i -> wholeSection || pickedItems.contains(LessonSectionType.KANJI + "-" + i.getId()))
                                .forEach(i -> out.add(fromKanjiItem(i, bundle, lesson)));
                        case GRAMMAR -> sorted(section.getGrammarItems(), LessonGrammarItem::getOrderIndex).stream()
                                .filter(i -> i.getGrammar() != null)
                                .filter(i -> wholeSection || pickedItems.contains(LessonSectionType.GRAMMAR + "-" + i.getId()))
                                .forEach(i -> out.add(fromGrammarItem(i, lesson)));
                    }
                });
        return out;
    }

    /** Từ vựng: ưu tiên giá trị giáo viên ghi đè trong bài, câu ví dụ chính (primary) của bài. */
    private NotebookWord fromVocabItem(LessonVocabularyItem item, Lesson lesson) {
        Vocabulary v = item.getVocab();
        String meaning = firstNonBlank(item.getCustomMeaning(), v.getMeaningVi(), v.getWord());
        String reading = item.getCustomHiragana() != null ? item.getCustomHiragana() : v.getHiragana();

        NotebookWord w = baseCard(NotebookItemType.VOCAB, lesson);
        w.setVocabulary(v);
        w.setWord(truncate(v.getWord(), 100));
        w.setReading(blankToNull(truncate(reading, 100)));
        w.setMeaning(meaning);
        w.setSinoVietnamese(blankToNull(truncate(v.getHanViet(), 100)));
        w.setPartOfSpeech(truncate(firstNonBlank(v.getPartOfSpeech(), NotebookItemType.VOCAB.getLabel()), 50));
        w.setAudioUrl(firstNonBlank(item.getCustomAudioUrl(), v.getAudioUrl()));
        w.setJlptLevel(v.getJlptLevel() != null ? v.getJlptLevel().name() : null);
        w.setUserNote(blankToNull(item.getNote()));

        LessonVocabularyExample ex = sorted(item.getExamples(), LessonVocabularyExample::getOrderIndex).stream()
                .filter(e -> e.getExampleJp() != null && !e.getExampleJp().isBlank())
                .min(Comparator.comparing(e -> e.isPrimary() ? 0 : 1))
                .orElse(null);
        if (ex != null) {
            w.setExampleSentence(ex.getExampleJp());
            w.setExampleMeaning(ex.getExampleVi());
        } else if (v.getExampleJp() != null && !v.getExampleJp().isBlank()) {
            w.setExampleSentence(v.getExampleJp());
            w.setExampleMeaning(v.getExampleVi());
        }
        return w;
    }

    /** Chữ Hán: ví dụ = từ liên quan đầu tiên (giáo viên chọn, không có thì từ tự suy ra). */
    private NotebookWord fromKanjiItem(LessonKanjiItem item, SectionService.RelatedVocabBundle bundle, Lesson lesson) {
        WritingCharacter wc = item.getCharacter();
        NotebookWord w = baseCard(NotebookItemType.KANJI, lesson);
        w.setCharacter(wc);
        w.setWord(wc.getCharacter());
        w.setReading(truncate(primaryKanjiReading(wc), 100));
        w.setMeaning(firstNonBlank(wc.getMeaningVi(), wc.getHanViet(), wc.getCharacter()));
        if (wc.getHanViet() != null && !wc.getHanViet().isBlank()) {
            w.setSinoVietnamese(truncate(wc.getHanViet().split(",")[0].trim().toUpperCase(), 100));
        }
        w.setPartOfSpeech(NotebookItemType.KANJI.getLabel());
        w.setJlptLevel(wc.getJlptLevel() != null ? wc.getJlptLevel().name() : null);
        w.setUserNote(blankToNull(item.getNote()));

        Vocabulary related = firstRelatedVocab(item, bundle);
        if (related != null) {
            String reading = related.getHiragana() != null && !related.getHiragana().equals(related.getWord())
                    ? related.getWord() + "（" + related.getHiragana() + "）"
                    : related.getWord();
            w.setExampleSentence(reading);
            w.setExampleMeaning(related.getMeaningVi());
        }
        return w;
    }

    /** Ngữ pháp: reading = cấu trúc (ưu tiên cấu trúc giáo viên ghi đè), ví dụ đầu tiên của bài. */
    private NotebookWord fromGrammarItem(LessonGrammarItem item, Lesson lesson) {
        GrammarPoint g = item.getGrammar();
        NotebookWord w = baseCard(NotebookItemType.GRAMMAR, lesson);
        w.setGrammar(g);
        w.setWord(truncate(g.getTitle(), 100));
        w.setReading(blankToNull(truncate(item.getCustomStructure() != null ? item.getCustomStructure() : g.getStructure(), 100)));
        w.setMeaning(firstNonBlank(item.getCustomMeaning(), g.getMeaningVi(), g.getTitle()));
        w.setPartOfSpeech(NotebookItemType.GRAMMAR.getLabel());
        w.setJlptLevel(g.getJlptLevel() != null ? g.getJlptLevel().name() : null);
        w.setUserNote(blankToNull(item.getNote()));

        LessonGrammarExample ex = sorted(item.getExamples(), LessonGrammarExample::getOrderIndex).stream()
                .filter(e -> e.getExampleJp() != null && !e.getExampleJp().isBlank())
                .findFirst().orElse(null);
        if (ex != null) {
            w.setExampleSentence(ex.getExampleJp());
            w.setExampleMeaning(ex.getExampleVi());
        }
        return w;
    }

    private Vocabulary firstRelatedVocab(LessonKanjiItem item, SectionService.RelatedVocabBundle bundle) {
        List<LessonKanjiRelatedVocab> teacher = bundle.teacherRelatedVocabsByKanjiItem().get(item.getId());
        if (teacher != null) {
            for (LessonKanjiRelatedVocab rv : teacher) {
                if (!rv.isDeleted() && rv.getVocab() != null) return rv.getVocab();
            }
        }
        List<Vocabulary> derived = bundle.relatedVocabsByCharacter().get(item.getCharacter().getId());
        return derived != null && !derived.isEmpty() ? derived.get(0) : null;
    }

    private NotebookWord baseCard(NotebookItemType type, Lesson lesson) {
        return NotebookWord.builder()
                .itemType(type)
                .sourceLesson(lesson)
                .build(); // chỉ số SRS: giá trị mặc định ban đầu của entity
    }

    private Notebook createNotebook(User user, SaveLessonItemsRequest request, Lesson lesson) {
        String name = request.getNewNotebookName() != null && !request.getNewNotebookName().isBlank()
                ? request.getNewNotebookName().trim()
                : null;
        if (name == null) {
            throw new AppException(ErrorCode.NOTEBOOK_TARGET_REQUIRED);
        }
        NotebookGroup group = null;
        if (request.getGroupId() != null) {
            group = notebookGroupRepository.findByIdAndUserIdAndDeletedFalse(request.getGroupId(), user.getId())
                    .orElseThrow(() -> new AppException(ErrorCode.NOTEBOOK_GROUP_NOT_FOUND));
        }
        String level = lesson.getCourse() != null && lesson.getCourse().getJlptLevel() != null
                ? lesson.getCourse().getJlptLevel().name()
                : null;
        Notebook notebook = Notebook.builder()
                .user(user)
                .group(group)
                .name(truncate(name, 100))
                .description(truncate("Lưu từ bài học: " + lesson.getTitle(), 500))
                .jlptLevel(level)
                .isPublic(false)
                .sharingEnabled(false)
                .shareCode(generateUniqueShareCode())
                .build();
        return notebookRepository.save(notebook);
    }

    private String generateUniqueShareCode() {
        for (int i = 0; i < 10; i++) {
            StringBuilder sb = new StringBuilder(6);
            for (int j = 0; j < 6; j++) {
                sb.append(SHARE_CODE_CHARS.charAt(RANDOM.nextInt(SHARE_CODE_CHARS.length())));
            }
            if (!notebookRepository.existsByShareCode(sb.toString())) {
                return sb.toString();
            }
        }
        return Long.toString(System.currentTimeMillis(), 36);
    }

    // =========================================================
    // Helpers
    // =========================================================

    /** Cách đọc đại diện của chữ Hán: Kun đầu tiên (bỏ dấu okurigana "."), không có thì On đầu tiên. */
    public static String primaryKanjiReading(WritingCharacter wc) {
        if (wc.getKunyomi() != null && !wc.getKunyomi().isEmpty()) {
            return wc.getKunyomi().get(0).replace(".", "");
        }
        if (wc.getOnyomi() != null && !wc.getOnyomi().isEmpty()) {
            return wc.getOnyomi().get(0);
        }
        return null;
    }

    private static <T> List<T> sorted(List<T> items, java.util.function.Function<T, Integer> order) {
        if (items == null) return List.of();
        return items.stream()
                .filter(Objects::nonNull)
                .filter(i -> !(i instanceof com.jp.elearningjp.shared.persitence.SoftDeletableEntity e) || !e.isDeleted())
                .sorted(Comparator.comparing(order, Comparator.nullsLast(Integer::compareTo)))
                .toList();
    }

    private static String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) return v;
        }
        return null;
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s;
    }

    private static String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }
}
