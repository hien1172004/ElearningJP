package com.jp.elearningjp.service.curriculum.section;

import com.jp.elearningjp.service.curriculum.snapshot.PublishedLessonResolver;
import com.jp.elearningjp.dto.request.curriculum.LessonGrammarItemCreateRequest;
import com.jp.elearningjp.dto.request.curriculum.LessonKanjiItemCreateRequest;
import com.jp.elearningjp.dto.request.curriculum.LessonVocabularyItemCreateRequest;
import com.jp.elearningjp.dto.request.curriculum.LessonVocabularyItemUpdateRequest;
import com.jp.elearningjp.entity.curriculum.Course;
import com.jp.elearningjp.entity.curriculum.Lesson;
import com.jp.elearningjp.entity.curriculum.LessonSection;
import com.jp.elearningjp.entity.curriculum.LessonVocabularyItem;
import com.jp.elearningjp.exception.AppException;
import com.jp.elearningjp.exception.ErrorCode;
import com.jp.elearningjp.mapper.LessonSectionMapper;
import com.jp.elearningjp.repository.content.GrammarPointRepository;
import com.jp.elearningjp.repository.content.VocabularyCharacterRepository;
import com.jp.elearningjp.repository.content.VocabularyRepository;
import com.jp.elearningjp.repository.content.WritingCharacterRepository;
import com.jp.elearningjp.repository.curriculum.*;
import com.jp.elearningjp.service.dictionary.DictionaryService;
import com.jp.elearningjp.shared.util.CurrentUserService;
import com.jp.elearningjp.shared.enums.LessonSectionType;
import com.jp.elearningjp.shared.util.CourseAccessGuard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.util.Optional;

import com.jp.elearningjp.shared.util.JapaneseReadingService;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

/** Giáo viên không phải chủ khóa học không được thêm/sửa/xoá item trong section của khóa đó. */
class SectionItemPermissionTest {

    private final Course course = new Course();
    private LessonSectionRepository sectionRepo;
    private CourseAccessGuard guard;

    @BeforeEach
    void setUp() {
        sectionRepo = mock(LessonSectionRepository.class);
        guard = mock(CourseAccessGuard.class);
        // Ghi item là ghi nội dung nháp của bài → guard kiểm theo nháp chứa section (requireDraftEditable, V65)
        doThrow(new AppException(ErrorCode.FORBIDDEN_LESSON_ACCESS))
                .when(guard).requireDraftEditable(
                        argThat(d -> d != null && d.getLesson() != null && d.getLesson().getCourse() == course),
                        any(ErrorCode.class), any(ErrorCode.class));
    }

    private LessonSection section(LessonSectionType type) {
        Lesson lesson = new Lesson();
        lesson.setCourse(course);
        LessonSection section = new LessonSection();
        section.setId(1L);
        section.setLesson(lesson);
        section.setDraft(com.jp.elearningjp.entity.curriculum.LessonDraft.builder().lesson(lesson)
                .kind(com.jp.elearningjp.shared.enums.DraftKind.TEACHER).build());
        section.setSectionType(type);
        when(sectionRepo.findById(1L)).thenReturn(Optional.of(section));
        return section;
    }

    private static void assertForbidden(Executable call) {
        assertThatThrownBy(call::execute)
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN_LESSON_ACCESS);
    }

    @Test
    void vocabulary_addUpdateDelete_requireAssignedOrAdmin() {
        LessonVocabularyItemRepository itemRepo = mock(LessonVocabularyItemRepository.class);
        VocabularyRepository vocabRepo = mock(VocabularyRepository.class);
        VocabularySectionItemService service = new VocabularySectionItemService(sectionRepo, itemRepo,
                mock(LessonVocabularyExampleRepository.class), vocabRepo, mock(LessonSectionMapper.class), guard,
                mock(PublishedLessonResolver.class), mock(JapaneseReadingService.class));
        LessonSection section = section(LessonSectionType.VOCABULARY);
        LessonVocabularyItem item = new LessonVocabularyItem();
        item.setSection(section);
        when(itemRepo.findById(5L)).thenReturn(Optional.of(item));

        assertForbidden(() -> service.addVocabularyItem(1L, LessonVocabularyItemCreateRequest.builder().vocabId(9L).build()));
        assertForbidden(() -> service.updateVocabularyItem(5L, new LessonVocabularyItemUpdateRequest()));
        assertForbidden(() -> service.deleteVocabularyItem(5L));
        verifyNoInteractions(vocabRepo);
        verify(itemRepo, never()).save(any());
    }

    @Test
    void kanji_add_requiresOwnerOrAdmin() {
        LessonKanjiItemRepository itemRepo = mock(LessonKanjiItemRepository.class);
        KanjiSectionItemService service = new KanjiSectionItemService(sectionRepo, itemRepo,
                mock(LessonKanjiRelatedVocabRepository.class), mock(WritingCharacterRepository.class),
                mock(VocabularyRepository.class), mock(VocabularyCharacterRepository.class),
                mock(LessonSectionMapper.class), mock(DictionaryService.class), mock(CurrentUserService.class), guard,
                mock(PublishedLessonResolver.class));
        section(LessonSectionType.KANJI);

        assertForbidden(() -> service.addKanjiItem(1L, LessonKanjiItemCreateRequest.builder().character("日").build()));
        verify(itemRepo, never()).save(any());
    }

    @Test
    void grammar_add_requiresOwnerOrAdmin() {
        LessonGrammarItemRepository itemRepo = mock(LessonGrammarItemRepository.class);
        GrammarPointRepository grammarRepo = mock(GrammarPointRepository.class);
        GrammarSectionItemService service = new GrammarSectionItemService(sectionRepo, itemRepo,
                mock(LessonGrammarExampleRepository.class), grammarRepo, mock(LessonSectionMapper.class), guard,
                mock(PublishedLessonResolver.class));
        section(LessonSectionType.GRAMMAR);

        assertForbidden(() -> service.addGrammarItem(1L, LessonGrammarItemCreateRequest.builder().grammarId(3L).build()));
        verifyNoInteractions(grammarRepo);
    }
}
