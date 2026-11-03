package com.jp.elearningjp.dto.response.notebook;

import com.jp.elearningjp.shared.enums.MemoryLevel;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

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

