package com.jp.elearningjp.service.notebook.impl;

import com.jp.elearningjp.dto.request.notebook.UpdateNotebookWordRequest;
import com.jp.elearningjp.entity.content.WritingCharacter;
import com.jp.elearningjp.entity.notebook.Notebook;
import com.jp.elearningjp.entity.notebook.NotebookWord;
import com.jp.elearningjp.mapper.notebook.NotebookMapper;
import com.jp.elearningjp.repository.content.GrammarPointRepository;
import com.jp.elearningjp.repository.content.VocabularyRepository;
import com.jp.elearningjp.repository.content.WritingCharacterRepository;
import com.jp.elearningjp.repository.notebook.NotebookRepository;
import com.jp.elearningjp.repository.notebook.NotebookWordRepository;
import com.jp.elearningjp.service.notebook.NotebookExportService;
import com.jp.elearningjp.service.notebook.NotebookWordCounter;
import com.jp.elearningjp.shared.enums.NotebookItemType;
import com.jp.elearningjp.shared.util.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Sửa thẻ Chữ Hán đổi mặt chữ + đọc cột "Loại" khi nhập Excel. */
class NotebookWordServiceImplTest {

    private static final long USER_ID = 7L;
    private static final long NOTEBOOK_ID = 11L;
    private static final long WORD_ID = 21L;

    private NotebookWordRepository wordRepo;
    private WritingCharacterRepository charRepo;
    private NotebookWordServiceImpl service;

    @BeforeEach
    void setUp() {
        wordRepo = mock(NotebookWordRepository.class);
        charRepo = mock(WritingCharacterRepository.class);
        NotebookRepository notebookRepo = mock(NotebookRepository.class);
        NotebookMapper mapper = mock(NotebookMapper.class);
        CurrentUserService currentUser = mock(CurrentUserService.class);

        service = new NotebookWordServiceImpl(wordRepo, notebookRepo, mock(VocabularyRepository.class), charRepo,
                mapper, mock(NotebookExportService.class), currentUser, mock(GrammarPointRepository.class),
                mock(NotebookWordCounter.class));

        Notebook notebook = new Notebook();
        notebook.setId(NOTEBOOK_ID);
        when(currentUser.getCurrentUserId()).thenReturn(USER_ID);
        when(notebookRepo.findByIdAndUserIdAndDeletedFalse(NOTEBOOK_ID, USER_ID)).thenReturn(Optional.of(notebook));
        when(wordRepo.save(any(NotebookWord.class))).thenAnswer(inv -> inv.getArgument(0));
        // Mapper thật chỉ chép các trường khác null của request vào entity — ở đây chỉ cần mặt chữ
        doAnswer(inv -> {
            UpdateNotebookWordRequest req = inv.getArgument(0);
            NotebookWord w = inv.getArgument(1);
            if (req.getWord() != null) w.setWord(req.getWord());
            return null;
        }).when(mapper).updateNotebookWordFromRequest(any(), any());
    }

    @Test
    void updateKanjiCard_renamedToOtherKanji_relinksToNewCharacter() {
        WritingCharacter hi = kanji(1L, "日");
        WritingCharacter tsuki = kanji(2L, "月");
        NotebookWord card = kanjiCard(hi);
        when(wordRepo.findByIdAndDeletedFalse(WORD_ID)).thenReturn(Optional.of(card));
        when(charRepo.findByCharacterAndDeletedFalse("月")).thenReturn(Optional.of(tsuki));

        service.updateWord(NOTEBOOK_ID, WORD_ID, UpdateNotebookWordRequest.builder().word("月").build());

        assertThat(card.getCharacter()).isSameAs(tsuki);
        assertThat(card.getWord()).isEqualTo("月");
    }

    @Test
    void updateKanjiCard_renamedToUnknownText_dropsOldLink() {
        NotebookWord card = kanjiCard(kanji(1L, "日"));
        when(wordRepo.findByIdAndDeletedFalse(WORD_ID)).thenReturn(Optional.of(card));
        when(charRepo.findByCharacterAndDeletedFalse("abc")).thenReturn(Optional.empty());

        service.updateWord(NOTEBOOK_ID, WORD_ID, UpdateNotebookWordRequest.builder().word("abc").build());

        assertThat(card.getCharacter()).isNull();
    }

    @Test
    void updateKanjiCard_noteOnly_keepsLink() {
        WritingCharacter hi = kanji(1L, "日");
        NotebookWord card = kanjiCard(hi);
        when(wordRepo.findByIdAndDeletedFalse(WORD_ID)).thenReturn(Optional.of(card));

        service.updateWord(NOTEBOOK_ID, WORD_ID, UpdateNotebookWordRequest.builder().userNote("ghi chú").build());

        assertThat(card.getCharacter()).isSameAs(hi);
        verifyNoInteractions(charRepo);
    }

    @Test
    void parseItemType_readsExportLabelOrEnumName_defaultsToVocab() {
        assertThat(NotebookWordServiceImpl.parseItemType("Chữ Hán")).isEqualTo(NotebookItemType.KANJI);
        assertThat(NotebookWordServiceImpl.parseItemType(" ngữ pháp ")).isEqualTo(NotebookItemType.GRAMMAR);
        assertThat(NotebookWordServiceImpl.parseItemType("KANJI")).isEqualTo(NotebookItemType.KANJI);
        assertThat(NotebookWordServiceImpl.parseItemType(null)).isEqualTo(NotebookItemType.VOCAB);
        assertThat(NotebookWordServiceImpl.parseItemType("lạ")).isEqualTo(NotebookItemType.VOCAB);
    }

    private NotebookWord kanjiCard(WritingCharacter wc) {
        Notebook notebook = new Notebook();
        notebook.setId(NOTEBOOK_ID);
        NotebookWord card = NotebookWord.builder()
                .notebook(notebook)
                .itemType(NotebookItemType.KANJI)
                .character(wc)
                .word(wc.getCharacter())
                .meaning("mặt trời")
                .build();
        card.setId(WORD_ID);
        return card;
    }

    private static WritingCharacter kanji(long id, String ch) {
        WritingCharacter wc = new WritingCharacter();
        wc.setId(id);
        wc.setCharacter(ch);
        return wc;
    }
}
