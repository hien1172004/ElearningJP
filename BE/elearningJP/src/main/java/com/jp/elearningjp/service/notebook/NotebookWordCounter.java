package com.jp.elearningjp.service.notebook;

import com.jp.elearningjp.entity.notebook.Notebook;
import com.jp.elearningjp.repository.notebook.NotebookRepository;
import com.jp.elearningjp.repository.notebook.NotebookWordRepository;
import com.jp.elearningjp.shared.enums.NotebookItemType;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Tính lại bộ đếm thẻ của sổ tay: {@code wordCount} (tổng) + số thẻ theo từng loại (V59).
 * Gọi sau mọi thao tác thêm / xoá thẻ.
 */
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class NotebookWordCounter {

    NotebookWordRepository notebookWordRepository;
    NotebookRepository notebookRepository;

    public void refresh(Notebook notebook) {
        notebookWordRepository.flush(); // query đếm phải thấy các thẻ vừa thêm / xoá trong transaction
        Map<NotebookItemType, Long> counts = new EnumMap<>(NotebookItemType.class);
        List<Object[]> rows = notebookWordRepository.countByTypeInNotebook(notebook.getId());
        for (Object[] row : rows) {
            counts.put((NotebookItemType) row[0], (Long) row[1]);
        }
        int vocab = counts.getOrDefault(NotebookItemType.VOCAB, 0L).intValue();
        int kanji = counts.getOrDefault(NotebookItemType.KANJI, 0L).intValue();
        int grammar = counts.getOrDefault(NotebookItemType.GRAMMAR, 0L).intValue();

        notebook.setVocabCount(vocab);
        notebook.setKanjiCount(kanji);
        notebook.setGrammarCount(grammar);
        notebook.setWordCount(vocab + kanji + grammar);
        notebookRepository.save(notebook);
    }
}
