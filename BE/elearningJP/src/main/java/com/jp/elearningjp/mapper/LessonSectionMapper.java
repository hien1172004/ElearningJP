package com.jp.elearningjp.mapper;

import com.jp.elearningjp.dto.response.curriculum.*;
import com.jp.elearningjp.entity.content.GrammarPoint;
import com.jp.elearningjp.entity.content.Vocabulary;
import com.jp.elearningjp.entity.content.WritingCharacter;
import com.jp.elearningjp.entity.curriculum.*;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Mapper thủ công (không dùng MapStruct) cho cấu trúc Section mới.
 * Lý do: MapStruct không hỗ trợ tốt cho việc phân nhánh theo enum + lazy load.
 */
@Component
public class LessonSectionMapper {

    // ============================================================
    // SECTION
    // ============================================================
    public LessonSectionResponse toSectionResponse(LessonSection section) {
        if (section == null || section.isDeleted()) {
            return null;
        }

        LessonSectionResponse.LessonSectionResponseBuilder builder = LessonSectionResponse.builder()
                .id(section.getId())
                .sectionType(section.getSectionType())
                .title(section.getTitle())
                .orderIndex(section.getOrderIndex());

        switch (section.getSectionType()) {
            case KANJI -> builder.kanjiItems(
                    section.getKanjiItems() == null ? List.of()
                            : section.getKanjiItems().stream()
                            .filter(i -> !i.isDeleted())
                            .map(this::toKanjiItemResponse)
                            .toList());
            case VOCABULARY -> builder.vocabularyItems(
                    section.getVocabularyItems() == null ? List.of()
                            : section.getVocabularyItems().stream()
                            .filter(i -> !i.isDeleted())
                            .map(this::toVocabularyItemResponse)
                            .toList());
            case GRAMMAR -> builder.grammarItems(
                    section.getGrammarItems() == null ? List.of()
                            : section.getGrammarItems().stream()
                            .filter(i -> !i.isDeleted())
                            .map(this::toGrammarItemResponse)
                            .toList());
            case TEST -> {
                // Quiz đã được load kèm qua JOIN FETCH trong repository
                if (section.getQuizzes() != null && !section.getQuizzes().isEmpty()) {
                    Quiz quiz = section.getQuizzes().stream()
                            .filter(q -> !q.isDeleted())
                            .findFirst()
                            .orElse(null);
                    if (quiz != null) {
                        builder.quiz(buildQuizSummary(quiz));
                    }
                }
            }
        }

        return builder.build();
    }

    /**
     * Build {@link LessonQuizResponse} từ entity Quiz (kèm questions + options nếu đã load).
     * Dùng trong {@link LessonSectionResponse} khi trả về section TEST.
     */
    private LessonQuizResponse buildQuizSummary(Quiz quiz) {
        if (quiz == null || quiz.isDeleted()) {
            return null;
        }
        List<QuizQuestionResponse> qs = (quiz.getQuestions() == null)
                ? List.of()
                : quiz.getQuestions().stream()
                .filter(q -> !q.isDeleted())
                .map(q -> QuizQuestionResponse.builder()
                        .id(q.getId())
                        .questionText(q.getQuestionText())
                        .orderIndex(q.getOrderIndex())
                        .questionType(q.getQuestionType())
                        .matchingData(q.getMatchingData())
                        .options(q.getOptions() == null ? List.of()
                                : q.getOptions().stream()
                                .filter(o -> !o.isDeleted())
                                .map(o -> QuizOptionResponse.builder()
                                        .id(o.getId())
                                        .optionKey(o.getOptionKey())
                                        .optionText(o.getOptionText())
                                        .build())
                                .toList())
                        .build())
                .toList();

        return LessonQuizResponse.builder()
                .quizId(quiz.getId())
                .lessonId(quiz.getLesson() != null ? quiz.getLesson().getId() : null)
                .title(quiz.getTitle())
                .description(quiz.getDescription())
                .passingScorePercent(quiz.getPassingScorePercent())
                .totalQuestions(qs.size())
                .questions(qs)
                .build();
    }

    // ============================================================
    // KANJI
    // ============================================================
    public LessonKanjiItemResponse toKanjiItemResponse(LessonKanjiItem item) {
        if (item == null || item.isDeleted()) {
            return null;
        }
        WritingCharacter c = item.getCharacter();
        if (c == null) {
            return null;
        }

        LessonKanjiItemResponse.LessonKanjiItemResponseBuilder builder = LessonKanjiItemResponse.builder()
                .id(item.getId())
                .orderIndex(item.getOrderIndex())
                .note(item.getNote())
                .characterId(c.getId())
                .character(c.getCharacter())
                .hanViet(c.getHanViet())
                .meaningVi(c.getMeaningVi())
                .meaningEn(c.getMeaningEn())
                .strokeCount(c.getStrokeCount())
                .onyomi(c.getOnyomi())
                .kunyomi(c.getKunyomi())
                .romaji(c.getRomaji())
                .radicals(c.getRadicals())
                .jlptLevel(c.getJlptLevel() != null ? c.getJlptLevel().name() : null);

        // Ảnh chữ Hán lấy từ SVG path của stroke đầu tiên (nếu có)
        // CharacterStroke không có imageUrl riêng — dùng svgPathData làm tham chiếu
        // (FE có thể render SVG từ path này). Để đơn giản, bỏ qua imageUrl.

        // Từ vựng liên quan (giáo viên tự chọn)
        if (item.getRelatedVocabulary() != null) {
            builder.relatedVocabulary(
                    item.getRelatedVocabulary().stream()
                            .filter(rv -> !rv.isDeleted())
                            .map(this::toRelatedVocabResponse)
                            .toList());
        }

        return builder.build();
    }

    public LessonRelatedVocabResponse toRelatedVocabResponse(LessonKanjiRelatedVocab rv) {
        if (rv == null || rv.isDeleted()) {
            return null;
        }
        Vocabulary v = rv.getVocab();
        if (v == null) {
            return null;
        }
        return LessonRelatedVocabResponse.builder()
                .id(rv.getId())
                .note(rv.getNote())
                .vocabId(v.getId())
                .word(v.getWord())
                .hiragana(v.getHiragana())
                .meaningVi(v.getMeaningVi())
                .build();
    }

    // ============================================================
    // VOCABULARY
    // ============================================================
    public LessonVocabularyItemResponse toVocabularyItemResponse(LessonVocabularyItem item) {
        if (item == null || item.isDeleted()) {
            return null;
        }
        Vocabulary v = item.getVocab();
        if (v == null) {
            return null;
        }

        // Ưu tiên customHiragana nếu giáo viên override
        String displayHiragana = item.getCustomHiragana() != null
                ? item.getCustomHiragana()
                : v.getHiragana();
        String displayMeaning = item.getCustomMeaning() != null
                ? item.getCustomMeaning()
                : v.getMeaningVi();

        LessonVocabularyItemResponse.LessonVocabularyItemResponseBuilder builder = LessonVocabularyItemResponse.builder()
                .id(item.getId())
                .orderIndex(item.getOrderIndex())
                .note(item.getNote())
                .vocabId(v.getId())
                .word(v.getWord())
                .hiragana(v.getHiragana())
                .romaji(v.getRomaji())
                .hanViet(v.getHanViet())
                .meaningVi(v.getMeaningVi())
                .meaningEn(v.getMeaningEn())
                .partOfSpeech(v.getPartOfSpeech())
                .jlptLevel(v.getJlptLevel() != null ? v.getJlptLevel().name() : null)
                .customHiragana(item.getCustomHiragana())
                .customMeaning(item.getCustomMeaning())
                .imageUrl(item.getImageUrl())
                .displayHiragana(displayHiragana)
                .displayMeaning(displayMeaning);

        if (item.getExamples() != null) {
            builder.examples(
                    item.getExamples().stream()
                            .filter(e -> !e.isDeleted())
                            .map(this::toVocabularyExampleResponse)
                            .toList());
        }

        return builder.build();
    }

    public LessonVocabularyExampleResponse toVocabularyExampleResponse(LessonVocabularyExample ex) {
        if (ex == null) {
            return null;
        }
        return LessonVocabularyExampleResponse.builder()
                .id(ex.getId())
                .orderIndex(ex.getOrderIndex())
                .exampleJp(ex.getExampleJp())
                .exampleVi(ex.getExampleVi())
                .exampleRomaji(ex.getExampleRomaji())
                .audioUrl(ex.getAudioUrl())
                .isPrimary(ex.isPrimary())
                .build();
    }

    // ============================================================
    // GRAMMAR
    // ============================================================
    public LessonGrammarItemResponse toGrammarItemResponse(LessonGrammarItem item) {
        if (item == null || item.isDeleted()) {
            return null;
        }
        GrammarPoint g = item.getGrammar();
        if (g == null) {
            return null;
        }

        String displayMeaning = item.getCustomMeaning() != null
                ? item.getCustomMeaning()
                : g.getMeaningVi();
        String displayStructure = item.getCustomStructure() != null
                ? item.getCustomStructure()
                : g.getStructure();

        LessonGrammarItemResponse.LessonGrammarItemResponseBuilder builder = LessonGrammarItemResponse.builder()
                .id(item.getId())
                .orderIndex(item.getOrderIndex())
                .note(item.getNote())
                .grammarId(g.getId())
                .title(g.getTitle())
                .structure(g.getStructure())
                .meaningVi(g.getMeaningVi())
                .usageNotes(g.getUsageNotes())
                .jlptLevel(g.getJlptLevel() != null ? g.getJlptLevel().name() : null)
                .customMeaning(item.getCustomMeaning())
                .customStructure(item.getCustomStructure())
                .displayMeaning(displayMeaning)
                .displayStructure(displayStructure);

        if (item.getExamples() != null) {
            builder.examples(
                    item.getExamples().stream()
                            .filter(e -> !e.isDeleted())
                            .map(this::toGrammarExampleResponse)
                            .toList());
        }

        return builder.build();
    }

    public LessonGrammarExampleResponse toGrammarExampleResponse(LessonGrammarExample ex) {
        if (ex == null) {
            return null;
        }
        return LessonGrammarExampleResponse.builder()
                .id(ex.getId())
                .orderIndex(ex.getOrderIndex())
                .exampleJp(ex.getExampleJp())
                .exampleVi(ex.getExampleVi())
                .build();
    }

    // ============================================================
    // Helper: lọc section không bị xoá
    // ============================================================
    public List<LessonSectionResponse> toSectionResponses(List<LessonSection> sections) {
        if (sections == null) {
            return List.of();
        }
        return sections.stream()
                .filter(s -> !s.isDeleted())
                .map(this::toSectionResponse)
                .toList();
    }
}
