package com.jp.elearningjp.service.curriculum;

import com.jp.elearningjp.dto.request.curriculum.PracticeSheetRequest;
import com.jp.elearningjp.dto.response.curriculum.PracticeSheetCheckResponse;
import com.jp.elearningjp.entity.content.CharacterStroke;
import com.jp.elearningjp.entity.content.Vocabulary;
import com.jp.elearningjp.entity.content.WritingCharacter;
import com.jp.elearningjp.entity.curriculum.Lesson;
import com.jp.elearningjp.entity.curriculum.LessonKanjiItem;
import com.jp.elearningjp.entity.curriculum.LessonSection;
import com.jp.elearningjp.entity.curriculum.LessonVocabularyItem;
import com.jp.elearningjp.exception.AppException;
import com.jp.elearningjp.exception.ErrorCode;
import com.jp.elearningjp.repository.content.CharacterStrokeRepository;
import com.jp.elearningjp.repository.curriculum.LessonKanjiItemRepository;
import com.jp.elearningjp.repository.curriculum.LessonRepository;
import com.jp.elearningjp.repository.curriculum.LessonSectionRepository;
import com.jp.elearningjp.repository.curriculum.LessonVocabularyItemRepository;
import com.jp.elearningjp.shared.enums.LessonSectionType;
import com.jp.elearningjp.shared.pdf.PracticeSheetRenderer;
import com.jp.elearningjp.shared.util.CourseAccessGuard;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LessonPracticeSheetServiceTest {

    @Mock LessonRepository lessonRepository;
    @Mock LessonSectionRepository sectionRepository;
    @Mock LessonKanjiItemRepository kanjiItemRepository;
    @Mock LessonVocabularyItemRepository vocabItemRepository;
    @Mock CharacterStrokeRepository strokeRepository;
    @Mock CourseAccessGuard courseAccessGuard;

    LessonPracticeSheetService service;
    Lesson lesson;
    /** Ký tự có dữ liệu nét trong "DB" giả. */
    final Map<String, Integer> strokeData = new HashMap<>(Map.of("家", 10, "族", 11, "学", 8, "校", 10, "ね", 2, "こ", 2));

    /** V65: nháp của bài X có id = X (quy ước trong test). */
    private static LessonDraftService draftService() {
        LessonDraftService drafts = org.mockito.Mockito.mock(LessonDraftService.class);
        org.mockito.Mockito.when(drafts.readDraftId(org.mockito.ArgumentMatchers.any()))
                .thenAnswer(inv -> ((Lesson) inv.getArgument(0)).getId());
        return drafts;
    }

    @BeforeEach
    void setUp() {
        var currentUserService = org.mockito.Mockito.mock(com.jp.elearningjp.shared.util.CurrentUserService.class);
        var subscriptionService = org.mockito.Mockito.mock(com.jp.elearningjp.service.subscription.SubscriptionService.class);
        org.mockito.Mockito.when(subscriptionService.isUserPro(org.mockito.ArgumentMatchers.any())).thenReturn(true);

        service = new LessonPracticeSheetService(lessonRepository, sectionRepository, kanjiItemRepository,
                vocabItemRepository, strokeRepository, courseAccessGuard, new PracticeSheetRenderer(),
                org.mockito.Mockito.mock(com.jp.elearningjp.service.curriculum.snapshot.PublishedLessonResolver.class),
                draftService(),
                currentUserService,
                subscriptionService);
        lesson = new Lesson();
        lesson.setId(1L);
        lesson.setTitle("Bai 1");
        when(lessonRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(lesson));
        when(strokeRepository.findByCharacterStrings(anyCollection())).thenAnswer(inv -> {
            Collection<String> chars = inv.getArgument(0);
            List<CharacterStroke> out = new ArrayList<>();
            for (String c : chars) {
                WritingCharacter wc = WritingCharacter.builder().character(c).build();
                for (int i = 1; i <= strokeData.getOrDefault(c, 0); i++) {
                    out.add(CharacterStroke.builder().character(wc).strokeNumber(i).svgPathData("M10,10L90,90").build());
                }
            }
            return out;
        });
    }

    private LessonSection section(long id, String title) {
        LessonSection s = new LessonSection();
        s.setId(id);
        s.setTitle(title);
        return s;
    }

    private LessonVocabularyItem vocabItem(long id, String word, String hiragana) {
        LessonVocabularyItem item = LessonVocabularyItem.builder()
                .vocab(Vocabulary.builder().word(word).hiragana(hiragana).build()).build();
        item.setId(id);
        return item;
    }

    /** 2 nhóm: Gia đình (家族, ねこ, コーヒー) và Trường học (学校, 自分). */
    private void givenVocabularySections() {
        LessonSection family = section(10L, "Gia đình");
        LessonSection school = section(20L, "Trường học");
        when(sectionRepository.findByDraftIdAndSectionTypeAndDeletedFalseOrderByOrderIndexAsc(1L, LessonSectionType.VOCABULARY))
                .thenReturn(List.of(family, school));
        when(vocabItemRepository.findBySectionIdAndDeletedFalseOrderByOrderIndexAsc(10L)).thenReturn(List.of(
                vocabItem(101L, "家族", "かぞく"), vocabItem(102L, "ねこ", "ねこ"), vocabItem(103L, "コーヒー", "こーひー")));
        when(vocabItemRepository.findBySectionIdAndDeletedFalseOrderByOrderIndexAsc(20L)).thenReturn(List.of(
                vocabItem(201L, "学校", "がっこう"), vocabItem(202L, "自分", "じぶん")));
    }

    @Test
    void check_listsItemsWithMissingStrokes_kanjiWordsCheckKanjiOnly_kanaWordsCheckEachKana() {
        givenVocabularySections();

        PracticeSheetCheckResponse res = service.check(1L,
                new PracticeSheetRequest(LessonSectionType.VOCABULARY, List.of(101L, 102L, 103L, 201L, 202L)));

        assertThat(res.totalItems()).isEqualTo(5);
        assertThat(res.missingItems()).extracting(PracticeSheetCheckResponse.MissingStrokeItem::word)
                .containsExactly("コーヒー", "自分");
        var coffee = res.missingItems().get(0);
        assertThat(coffee.groupTitle()).isEqualTo("Gia đình");
        assertThat(coffee.missingChars()).containsExactly("コ", "ー", "ヒ");
        assertThat(res.missingItems().get(1).missingChars()).containsExactly("自", "分");
        verify(courseAccessGuard).requireLessonContentAccess(lesson);
    }

    @Test
    void check_onlySelectedItems_otherLessonItemIdsIgnored() {
        givenVocabularySections();

        PracticeSheetCheckResponse res = service.check(1L,
                new PracticeSheetRequest(LessonSectionType.VOCABULARY, List.of(101L, 201L, 999L)));

        assertThat(res.totalItems()).isEqualTo(2);
        assertThat(res.missingItems()).isEmpty();
    }

    @Test
    void render_groupsBySection_withGroupHeadings() throws Exception {
        givenVocabularySections();

        var file = service.render(1L, new PracticeSheetRequest(LessonSectionType.VOCABULARY, List.of(101L, 102L, 201L)));

        assertThat(file.fileName()).isEqualTo("luyen_viet_tu_vung_bai_1.pdf");
        try (PDDocument doc = Loader.loadPDF(file.content())) {
            assertThat(doc.getNumberOfPages()).isGreaterThanOrEqualTo(1);
            String text = new PDFTextStripper().getText(doc);
            // Tiêu đề nhóm in không dấu (font tiếng Nhật không có glyph tiếng Việt)
            assertThat(text).contains("Gia dinh").contains("Truong hoc").contains("家").contains("学");
            assertThat(text.indexOf("Gia dinh")).isLessThan(text.indexOf("Truong hoc"));
        }
    }

    @Test
    void render_singleGroup_hasNoGroupHeading() throws Exception {
        givenVocabularySections();

        var file = service.render(1L, new PracticeSheetRequest(LessonSectionType.VOCABULARY, List.of(201L)));

        try (PDDocument doc = Loader.loadPDF(file.content())) {
            assertThat(new PDFTextStripper().getText(doc)).doesNotContain("Truong hoc");
        }
    }

    @Test
    void kanji_usesOnyomiAsReading_andAllStrokesPresent() {
        LessonSection s = section(30L, "Kanji");
        WritingCharacter ka = WritingCharacter.builder().character("家").onyomi(List.of("カ", "ケ")).kunyomi(List.of("いえ")).build();
        LessonKanjiItem item = LessonKanjiItem.builder().character(ka).build();
        item.setId(301L);
        when(sectionRepository.findByDraftIdAndSectionTypeAndDeletedFalseOrderByOrderIndexAsc(1L, LessonSectionType.KANJI))
                .thenReturn(List.of(s));
        when(kanjiItemRepository.findBySectionIdAndDeletedFalseOrderByOrderIndexAsc(30L)).thenReturn(List.of(item));

        PracticeSheetCheckResponse res = service.check(1L, new PracticeSheetRequest(LessonSectionType.KANJI, List.of(301L)));

        assertThat(res.totalItems()).isEqualTo(1);
        assertThat(res.missingItems()).isEmpty();
        assertThat(service.render(1L, new PracticeSheetRequest(LessonSectionType.KANJI, List.of(301L))).content()).isNotEmpty();
    }

    @Test
    void rejects_grammarType_emptySelection_andNothingMatched() {
        givenVocabularySections();

        assertThatThrownBy(() -> service.check(1L, new PracticeSheetRequest(LessonSectionType.GRAMMAR, List.of(1L))))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode()).isEqualTo(ErrorCode.PRACTICE_SHEET_TYPE_INVALID);
        assertThatThrownBy(() -> service.check(1L, new PracticeSheetRequest(LessonSectionType.VOCABULARY, List.of())))
                .extracting(e -> ((AppException) e).getErrorCode()).isEqualTo(ErrorCode.PRACTICE_SHEET_NO_ITEMS);
        assertThatThrownBy(() -> service.check(1L, new PracticeSheetRequest(LessonSectionType.VOCABULARY, List.of(999L))))
                .extracting(e -> ((AppException) e).getErrorCode()).isEqualTo(ErrorCode.PRACTICE_SHEET_NO_ITEMS);
        verify(strokeRepository, never()).findByCharacterStrings(any());
    }

    @Test
    void accessDenied_propagates() {
        givenVocabularySections();
        doThrow(new AppException(ErrorCode.NOT_ENROLLED)).when(courseAccessGuard).requireLessonContentAccess(lesson);

        assertThatThrownBy(() -> service.check(1L, new PracticeSheetRequest(LessonSectionType.VOCABULARY, List.of(101L))))
                .extracting(e -> ((AppException) e).getErrorCode()).isEqualTo(ErrorCode.NOT_ENROLLED);
    }
}
