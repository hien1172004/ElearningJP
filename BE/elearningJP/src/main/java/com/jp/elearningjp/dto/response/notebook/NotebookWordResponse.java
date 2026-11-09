package com.jp.elearningjp.dto.response.notebook;

import com.jp.elearningjp.shared.enums.MemoryLevel;
import com.jp.elearningjp.shared.enums.NotebookItemType;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NotebookWordResponse {

    Long id;
    Long notebookId;
    Long vocabularyId;
    NotebookItemType itemType;
    Long characterId;
    Long grammarId;
    Long sourceLessonId;
    String sourceLessonTitle;
    String word;
    String reading;
    String meaning;
    String partOfSpeech;
    String userNote;
    String sinoVietnamese;
    String exampleSentence;
    String exampleMeaning;
    String audioUrl;
    String jlptLevel;
    Integer displayOrder;

    // Thông tin riêng theo loại — đọc trực tiếp từ bản ghi gốc trong từ điển
    /** KANJI: âm On / Kun / số nét. */
    List<String> onyomi;
    List<String> kunyomi;
    Integer strokeCount;
    /** GRAMMAR: cấu trúc đầy đủ + cách dùng. */
    String grammarStructure;
    String usageNotes;

    // SRS
    MemoryLevel memoryLevel;
    String memoryLevelTitle;
    String memoryLevelColor;
    Integer intervalDays;
    Integer repetitionCount;
    Instant nextReviewAt;
    Instant lastReviewedAt;

    Instant createdAt;
    Instant updatedAt;
}

