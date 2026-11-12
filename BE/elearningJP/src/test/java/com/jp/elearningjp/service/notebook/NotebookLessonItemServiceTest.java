package com.jp.elearningjp.service.notebook;

import com.jp.elearningjp.dto.request.notebook.SaveLessonItemsRequest;
import com.jp.elearningjp.dto.response.notebook.SaveLessonItemsResponse;
import com.jp.elearningjp.entity.content.GrammarPoint;
import com.jp.elearningjp.entity.content.Vocabulary;
import com.jp.elearningjp.entity.content.WritingCharacter;
import com.jp.elearningjp.entity.curriculum.Course;
import com.jp.elearningjp.entity.curriculum.Lesson;
import com.jp.elearningjp.entity.curriculum.LessonGrammarItem;
import com.jp.elearningjp.entity.curriculum.LessonKanjiItem;
import com.jp.elearningjp.entity.curriculum.LessonSection;
import com.jp.elearningjp.entity.curriculum.LessonVocabularyExample;
import com.jp.elearningjp.entity.curriculum.LessonVocabularyItem;
import com.jp.elearningjp.entity.notebook.Notebook;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

/** Lưu Chữ Hán / Từ vựng / Ngữ pháp của bài học vào sổ tay. */
class NotebookLessonItemServiceTest {

    private static final long USER_ID = 7L;
    private static final long LESSON_ID = 3L;
    private static final long NOTEBOOK_ID = 11L;

    private LessonRepository lessonRepo;
    private LessonSectionRepository sectionRepo;
    private NotebookRepository notebookRepo;
    private NotebookWordRepository wordRepo;
    private NotebookWordCounter counter;
    private NotebookLessonItemService service;

    private Lesson lesson;
    private Notebook notebook;

    @BeforeEach
    void setUp() {
        lessonRepo = mock(LessonRepository.class);
        sectionRepo = mock(LessonSectionRepository.class);
        notebookRepo = mock(NotebookRepository.class);
        wordRepo = mock(NotebookWordRepository.class);
        counter = mock(NotebookWordCounter.class);
        SectionService sectionService = mock(SectionService.class);
        CurrentUserService currentUser = mock(CurrentUserService.class);

        // V65: nháp của bài X có id = X (quy ước trong test)
        com.jp.elearningjp.service.curriculum.LessonDraftService drafts =
                mock(com.jp.elearningjp.service.curriculum.LessonDraftService.class);
        when(drafts.readDraftId(org.mockito.ArgumentMatchers.any())).thenAnswer(inv -> ((Lesson) inv.getArgument(0)).getId());
        service = new NotebookLessonItemService(lessonRepo, sectionRepo, sectionService, drafts, mock(CourseAccessGuard.class),
                currentUser, notebookRepo, mock(NotebookGroupRepository.class), wordRepo, counter);

        User user = new User();
        user.setId(USER_ID);
        when(currentUser.getCurrentUser()).thenReturn(user);

        lesson = new Lesson();
        lesson.setId(LESSON_ID);
        lesson.setTitle("Bài 1");
        lesson.setCourse(new Course());
        when(lessonRepo.findByIdAndDeletedFalse(LESSON_ID)).thenReturn(Optional.of(lesson));
        when(sectionService.loadNestedCollections(anyList()))
                .thenReturn(new SectionService.RelatedVocabBundle(Map.of(), Map.of()));

        notebook = new Notebook();
        notebook.setId(NOTEBOOK_ID);
        notebook.setName("Sổ của tôi");
        when(notebookRepo.findByIdAndUserIdAndDeletedFalse(NOTEBOOK_ID, USER_ID)).thenReturn(Optional.of(notebook));
        when(notebookRepo.save(any(Notebook.class))).thenAnswer(inv -> {
            Notebook n = inv.getArgument(0);
            if (n.getId() == null) n.setId(99L);
            return n;
        });
    }

    /** Bài gồm: nhóm Từ vựng (食べる – nghĩa ghi đè + 2 ví dụ, 飲む), nhóm Chữ Hán (日), nhóm Ngữ pháp (〜たい). */
    private void givenLessonContent() {
        Vocabulary taberu = vocab(100L, "食べる", "たべる", "ăn");
        LessonVocabularyItem v1 = new LessonVocabularyItem();
        v1.setId(1L);
        v1.setOrderIndex(1);
        v1.setVocab(taberu);
        v1.setCustomMeaning("ăn (cơm)");
        v1.setExamples(new ArrayList<>(List.of(
                example("パンを食べる", "ăn bánh mì", false, 1),
                example("ご飯を食べる", "ăn cơm", true, 2))));
        LessonVocabularyItem v2 = new LessonVocabularyItem();
        v2.setId(2L);
        v2.setOrderIndex(2);
        v2.setVocab(vocab(101L, "飲む", "のむ", "uống"));
        LessonSection vocabSection = section(10L, LessonSectionType.VOCABULARY, 2);
        vocabSection.setVocabularyItems(new ArrayList<>(List.of(v2, v1))); // lưu theo orderIndex, không theo thứ tự list

        WritingCharacter hi = new WritingCharacter();
        hi.setId(200L);
        hi.setCharacter("日");
        hi.setHanViet("nhật, nhựt");
        hi.setMeaningVi("mặt trời, ngày");
        hi.setKunyomi(List.of("ひ", "か"));
        hi.setOnyomi(List.of("ニチ"));
        LessonKanjiItem k1 = new LessonKanjiItem();
        k1.setId(5L);
        k1.setCharacter(hi);
        LessonSection kanjiSection = section(20L, LessonSectionType.KANJI, 1);
        kanjiSection.setKanjiItems(new ArrayList<>(List.of(k1)));

        GrammarPoint tai = new GrammarPoint();
        tai.setId(300L);
        tai.setTitle("〜たい");
        tai.setStructure("V(ます) + たい");
        tai.setMeaningVi("muốn làm gì");
        LessonGrammarItem g1 = new LessonGrammarItem();
        g1.setId(8L);
        g1.setGrammar(tai);
        g1.setCustomStructure("Vます(bỏ ます) + たい");
        LessonSection grammarSection = section(30L, LessonSectionType.GRAMMAR, 3);
        grammarSection.setGrammarItems(new ArrayList<>(List.of(g1)));

        when(sectionRepo.findByDraftIdWithAllItems(LESSON_ID))
                .thenReturn(List.of(vocabSection, kanjiSection, grammarSection));
    }

    @Test
    void saveWholeLesson_snapshotsLessonVersion_skipsItemsAlreadyInNotebook() {
        givenLessonContent();
        when(wordRepo.findVocabularyIdsInNotebook(NOTEBOOK_ID)).thenReturn(List.of(101L)); // 飲む đã có
        when(wordRepo.countByNotebookIdAndDeletedFalse(NOTEBOOK_ID)).thenReturn(4L);

        SaveLessonItemsRequest req = SaveLessonItemsRequest.builder().lessonId(LESSON_ID).notebookId(NOTEBOOK_ID).build();
        SaveLessonItemsResponse res = service.saveLessonItems(req);

        assertThat(res.getAddedCount()).isEqualTo(3);
        assertThat(res.getSkippedCount()).isEqualTo(1);
        assertThat(res.isCreatedNotebook()).isFalse();
        assertThat(res.getAddedVocab()).isEqualTo(1);
        assertThat(res.getAddedKanji()).isEqualTo(1);
        assertThat(res.getAddedGrammar()).isEqualTo(1);

        List<NotebookWord> saved = captureSaved();
        // Thứ tự theo bài: nhóm Chữ Hán (order 1) → Từ vựng (2) → Ngữ pháp (3); nối tiếp sau 4 thẻ có sẵn
        assertThat(saved).extracting(NotebookWord::getItemType)
                .containsExactly(NotebookItemType.KANJI, NotebookItemType.VOCAB, NotebookItemType.GRAMMAR);
        assertThat(saved).extracting(NotebookWord::getDisplayOrder).containsExactly(4, 5, 6);
        assertThat(saved).allSatisfy(w -> {
            assertThat(w.getNotebook()).isSameAs(notebook);
            assertThat(w.getSourceLesson()).isSameAs(lesson);
            assertThat(w.getNextReviewAt()).isNotNull();
        });

        NotebookWord kanji = saved.get(0);
        assertThat(kanji.getCharacter().getId()).isEqualTo(200L);
        assertThat(kanji.getReading()).isEqualTo("ひ");
        assertThat(kanji.getSinoVietnamese()).isEqualTo("NHẬT");
        assertThat(kanji.getVocabulary()).isNull();

        NotebookWord vocab = saved.get(1);
        assertThat(vocab.getWord()).isEqualTo("食べる");
        assertThat(vocab.getMeaning()).isEqualTo("ăn (cơm)");           // nghĩa giáo viên ghi đè
        assertThat(vocab.getExampleSentence()).isEqualTo("ご飯を食べる"); // ví dụ chính (primary)

        NotebookWord grammar = saved.get(2);
        assertThat(grammar.getGrammar().getId()).isEqualTo(300L);
        assertThat(grammar.getReading()).isEqualTo("Vます(bỏ ます) + たい"); // cấu trúc giáo viên ghi đè

        verify(counter).refresh(notebook);
    }

    @Test
    void saveSelectedItems_onlyThoseItems_intoNewNotebook() {
        givenLessonContent();
        SaveLessonItemsRequest req = SaveLessonItemsRequest.builder()
                .lessonId(LESSON_ID)
                .newNotebookName("Bài 1 - ôn tập")
                .items(List.of(new SaveLessonItemsRequest.ItemRef(LessonSectionType.GRAMMAR, 8L)))
                .build();

        SaveLessonItemsResponse res = service.saveLessonItems(req);

        assertThat(res.isCreatedNotebook()).isTrue();
        assertThat(res.getNotebookName()).isEqualTo("Bài 1 - ôn tập");
        assertThat(res.getAddedCount()).isEqualTo(1);
        assertThat(captureSaved()).extracting(NotebookWord::getItemType).containsExactly(NotebookItemType.GRAMMAR);
    }

    @Test
    void saveSections_onlyItemsOfThoseSections() {
        givenLessonContent();
        when(wordRepo.countByNotebookIdAndDeletedFalse(NOTEBOOK_ID)).thenReturn(0L);
        SaveLessonItemsRequest req = SaveLessonItemsRequest.builder()
                .lessonId(LESSON_ID).notebookId(NOTEBOOK_ID).sectionIds(List.of(10L)).build();

        SaveLessonItemsResponse res = service.saveLessonItems(req);

        assertThat(res.getAddedCount()).isEqualTo(2);
        assertThat(captureSaved()).extracting(NotebookWord::getWord).containsExactly("食べる", "飲む");
    }

    @Test
    void noTargetNotebook_rejected() {
        givenLessonContent();
        SaveLessonItemsRequest req = SaveLessonItemsRequest.builder().lessonId(LESSON_ID).build();

        assertThatThrownBy(() -> service.saveLessonItems(req))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOTEBOOK_TARGET_REQUIRED);
    }

    @Test
    void lessonWithoutContent_rejected() {
        when(sectionRepo.findByDraftIdWithAllItems(LESSON_ID)).thenReturn(List.of());
        SaveLessonItemsRequest req = SaveLessonItemsRequest.builder().lessonId(LESSON_ID).notebookId(NOTEBOOK_ID).build();

        assertThatThrownBy(() -> service.saveLessonItems(req))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOTEBOOK_LESSON_ITEMS_EMPTY);
        verify(wordRepo, never()).saveAll(anyList());
    }

    // ===== helpers =====

    @SuppressWarnings("unchecked")
    private List<NotebookWord> captureSaved() {
        ArgumentCaptor<List<NotebookWord>> captor = ArgumentCaptor.forClass(List.class);
        verify(wordRepo).saveAll(captor.capture());
        return captor.getValue();
    }

    private LessonSection section(long id, LessonSectionType type, int order) {
        LessonSection s = new LessonSection();
        s.setId(id);
        s.setLesson(lesson);
        s.setSectionType(type);
        s.setOrderIndex(order);
        return s;
    }

    private static Vocabulary vocab(long id, String word, String hiragana, String meaning) {
        Vocabulary v = new Vocabulary();
        v.setId(id);
        v.setWord(word);
        v.setHiragana(hiragana);
        v.setMeaningVi(meaning);
        return v;
    }

    private static LessonVocabularyExample example(String jp, String vi, boolean primary, int order) {
        LessonVocabularyExample e = new LessonVocabularyExample();
        e.setExampleJp(jp);
        e.setExampleVi(vi);
        e.setPrimary(primary);
        e.setOrderIndex(order);
        return e;
    }
}
