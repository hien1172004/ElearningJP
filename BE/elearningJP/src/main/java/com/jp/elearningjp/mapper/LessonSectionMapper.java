package com.jp.elearningjp.mapper;

import com.jp.elearningjp.dto.response.curriculum.*;
import com.jp.elearningjp.entity.content.GrammarPoint;
import com.jp.elearningjp.entity.content.Vocabulary;
import com.jp.elearningjp.entity.content.WritingCharacter;
import com.jp.elearningjp.entity.curriculum.*;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
        return toSectionResponse(section, Collections.emptyMap(), Collections.emptyMap());
    }

    /**
     * Build section response kèm related vocab cho kanji items.
     *
     * @param section                          section entity
     * @param relatedVocabsByCharacter         map từ {@code writingCharacter.id} → {@code List<Vocabulary>}
     *                                         đã được pre-load từ từ điển (derive tự động).
     *                                         Có thể empty/null (sẽ trả về relatedVocabulary rỗng).
     * @param teacherRelatedVocabsByKanjiItem  map từ {@code lessonKanjiItem.id} → {@code List<LessonKanjiRelatedVocab>}
     *                                         do giáo viên chọn (lưu trong lesson_kanji_related_vocabulary).
     *                                         Nếu có entry → ưu tiên hiển thị, bỏ qua derive tự động.
     *                                         Có thể empty/null.
     */
    public LessonSectionResponse toSectionResponse(
            LessonSection section,
            Map<Long, List<Vocabulary>> relatedVocabsByCharacter,
            Map<Long, List<LessonKanjiRelatedVocab>> teacherRelatedVocabsByKanjiItem
    ) {
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
                            .map(i -> toKanjiItemResponse(
                                    i,
                                    relatedVocabsByCharacter,
                                    teacherRelatedVocabsByKanjiItem
                            ))
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
        }

        return builder.build();
    }

    // ============================================================
    // KANJI
    // Related vocab do giáo viên tự chọn (lưu trong lesson_kanji_related_vocabulary).
    // Nếu giáo viên CHƯA chọn vocab nào → fallback về derive tự động từ
    // bảng vocabulary_characters.
    // ============================================================
    public LessonKanjiItemResponse toKanjiItemResponse(LessonKanjiItem item) {
        return toKanjiItemResponse(item, Collections.emptyMap(), Collections.emptyMap());
    }

    /**
     * Backward-compat overload — chỉ truyền related vocab derive tự động.
     */
    public LessonKanjiItemResponse toKanjiItemResponse(
            LessonKanjiItem item,
            List<Vocabulary> relatedVocabs
    ) {
        Map<Long, List<Vocabulary>> auto = (relatedVocabs == null)
                ? Collections.emptyMap()
                : Map.of(item.getCharacter() != null ? item.getCharacter().getId() : -1L, relatedVocabs);
        return toKanjiItemResponse(item, auto, Collections.emptyMap());
    }

    /**
     * Build {@link LessonKanjiItemResponse} với related vocab.
     *
     * <p>Quy tắc ưu tiên:</p>
     * <ol>
     *   <li>Nếu giáo viên đã chọn related vocab (có trong {@code teacherRelatedVocabsByKanjiItem})
     *       → hiển thị danh sách đó (override hoàn toàn).</li>
     *   <li>Nếu giáo viên chưa chọn → fallback về derive tự động từ {@code relatedVocabsByCharacter}.</li>
     * </ol>
     */
    public LessonKanjiItemResponse toKanjiItemResponse(
            LessonKanjiItem item,
            Map<Long, List<Vocabulary>> relatedVocabsByCharacter,
            Map<Long, List<LessonKanjiRelatedVocab>> teacherRelatedVocabsByKanjiItem
    ) {
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

        // Resolve related vocab: teacher override → auto-derive
        List<LessonRelatedVocabResponse> relatedResp = resolveRelatedVocab(
                item, relatedVocabsByCharacter, teacherRelatedVocabsByKanjiItem);
        if (relatedResp != null && !relatedResp.isEmpty()) {
            builder.relatedVocabulary(relatedResp);
        }

        return builder.build();
    }

    /**
     * Resolve related vocab cho 1 kanji item theo thứ tự ưu tiên:
     * 1) Teacher override (lưu trong lesson_kanji_related_vocabulary)
     * 2) Auto-derive từ vocabulary_characters
     *
     * @return list response (rỗng nếu không có vocab nào)
     */
    private List<LessonRelatedVocabResponse> resolveRelatedVocab(
            LessonKanjiItem item,
            Map<Long, List<Vocabulary>> relatedVocabsByCharacter,
            Map<Long, List<LessonKanjiRelatedVocab>> teacherRelatedVocabsByKanjiItem
    ) {
        // 1) Teacher override
        List<LessonKanjiRelatedVocab> teacherRvs = (teacherRelatedVocabsByKanjiItem != null)
                ? teacherRelatedVocabsByKanjiItem.get(item.getId())
                : null;
        if (teacherRvs != null && !teacherRvs.isEmpty()) {
            return teacherRvs.stream()
                    .filter(rv -> !rv.isDeleted())
                    .map(this::toRelatedVocabResponse)
                    .toList();
        }

        // 2) Auto-derive
        Long charId = item.getCharacter() != null ? item.getCharacter().getId() : null;
        List<Vocabulary> autoRvs = (charId != null && relatedVocabsByCharacter != null)
                ? relatedVocabsByCharacter.getOrDefault(charId, List.of())
                : List.of();
        if (autoRvs.isEmpty()) {
            return List.of();
        }
        return autoRvs.stream()
                .map(this::toRelatedVocabResponseFromVocab)
                .toList();
    }

    /**
     * Build {@link LessonRelatedVocabResponse} từ entity {@link Vocabulary}.
     * Dùng cho related vocab được derive tự động từ từ điển (id = null vì
     * không có bản ghi lesson_kanji_related_vocabulary tương ứng).
     */
    public LessonRelatedVocabResponse toRelatedVocabResponseFromVocab(Vocabulary v) {
        if (v == null || v.isDeleted()) {
            return null;
        }
        return LessonRelatedVocabResponse.builder()
                .id(null) // null vì derive tự động, không phải row trong lesson_kanji_related_vocabulary
                .note(null)
                .vocabId(v.getId())
                .word(v.getWord())
                .hiragana(v.getHiragana())
                .meaningVi(v.getMeaningVi())
                .build();
    }

    /**
     * Build {@link LessonRelatedVocabResponse} từ entity {@link LessonKanjiRelatedVocab}
     * (vocab do giáo viên tự chọn / tạo mới và lưu trong bảng lesson_kanji_related_vocabulary).
     */
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

        // Ưu tiên customXxx nếu giáo viên override; fallback về từ điển
        String displayHiragana = item.getCustomHiragana() != null
                ? item.getCustomHiragana()
                : v.getHiragana();
        String displayRomaji = item.getCustomRomaji() != null
                ? item.getCustomRomaji()
                : v.getRomaji();
        String displayAudioUrl = item.getCustomAudioUrl() != null
                ? item.getCustomAudioUrl()
                : v.getAudioUrl();
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
                .customRomaji(item.getCustomRomaji())
                .customAudioUrl(item.getCustomAudioUrl())
                .customMeaning(item.getCustomMeaning())
                .imageUrl(item.getImageUrl())
                .displayHiragana(displayHiragana)
                .displayRomaji(displayRomaji)
                .displayAudioUrl(displayAudioUrl)
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
        return toSectionResponses(sections, Collections.emptyMap(), Collections.emptyMap());
    }

    /**
     * Backward-compat overload — chỉ truyền related vocab derive tự động.
     */
    public List<LessonSectionResponse> toSectionResponses(
            List<LessonSection> sections,
            Map<Long, List<Vocabulary>> relatedVocabsByCharacter
    ) {
        return toSectionResponses(sections, relatedVocabsByCharacter, Collections.emptyMap());
    }

    public List<LessonSectionResponse> toSectionResponses(
            List<LessonSection> sections,
            Map<Long, List<Vocabulary>> relatedVocabsByCharacter,
            Map<Long, List<LessonKanjiRelatedVocab>> teacherRelatedVocabsByKanjiItem
    ) {
        if (sections == null) {
            return List.of();
        }
        return sections.stream()
                .filter(s -> !s.isDeleted())
                .map(s -> toSectionResponse(s, relatedVocabsByCharacter, teacherRelatedVocabsByKanjiItem))
                .toList();
    }
}
