package com.jp.elearningjp.dto.response.notebook;

import com.jp.elearningjp.shared.enums.MemoryLevel;
import com.jp.elearningjp.shared.enums.NotebookItemType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotebookReviewItemResponse {

    private Long id;
    private Long notebookId;
    private NotebookItemType itemType;
    private Long vocabularyId;
    private Long characterId;
    private Long grammarId;
    private String jlptLevel;
    private String word;
    private String reading;
    private String sinoVietnamese;
    private String meaning;
    private String partOfSpeech;
    private String audioUrl;
    private String userNote;
    private MemoryLevel memoryLevel;
    private Integer intervalDays;
    private Integer repetitionCount;

    // Ví dụ mẫu (nếu có từ từ điển hoặc người dùng)
    private String exampleSentence;
    private String exampleMeaning;

    // Thông tin riêng theo loại (KANJI: on/kun/số nét; GRAMMAR: cấu trúc/cách dùng)
    private List<String> onyomi;
    private List<String> kunyomi;
    private Integer strokeCount;
    private String grammarStructure;
    private String usageNotes;

    // Các lựa chọn gây nhiễu cho bài Quizz trắc nghiệm (gồm 3 distractors + 1 correct)
    private List<String> quizMeaningOptions;
    private List<String> quizWordOptions;
}

