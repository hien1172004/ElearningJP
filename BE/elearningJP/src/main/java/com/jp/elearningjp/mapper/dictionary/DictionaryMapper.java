package com.jp.elearningjp.mapper.dictionary;

import com.jp.elearningjp.dto.response.dictionary.*;
import com.jp.elearningjp.entity.content.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface DictionaryMapper {

    VocabularySummaryResponse toVocabSummary(Vocabulary entity);

    List<VocabularySummaryResponse> toVocabSummaryList(List<Vocabulary> entities);

    KanjiSummaryResponse toKanjiSummary(WritingCharacter entity);

    List<KanjiSummaryResponse> toKanjiSummaryList(List<WritingCharacter> entities);

    GrammarSummaryResponse toGrammarSummary(GrammarPoint entity);

    List<GrammarSummaryResponse> toGrammarSummaryList(List<GrammarPoint> entities);

    VocabularySenseResponse toSenseResponse(VocabularySense entity);

    List<VocabularySenseResponse> toSenseResponseList(List<VocabularySense> entities);

    VocabularyExampleResponse toExampleResponse(VocabularyExample entity);

    List<VocabularyExampleResponse> toExampleResponseList(List<VocabularyExample> entities);

    KanjiSimpleResponse toKanjiSimpleResponse(WritingCharacter entity);

    List<KanjiSimpleResponse> toKanjiSimpleResponseList(List<WritingCharacter> entities);

    CharacterStrokeResponse toStrokeResponse(CharacterStroke entity);

    List<CharacterStrokeResponse> toStrokeResponseList(List<CharacterStroke> entities);

    @Mapping(target = "strokes", ignore = true)
    KanaCharacterResponse toKanaCharacterResponse(WritingCharacter entity);

    GrammarDetailResponse toGrammarDetailResponse(GrammarPoint entity);

    @Mapping(target = "vocabId", source = "vocabulary.id")
    @Mapping(target = "word", source = "vocabulary.word")
    @Mapping(target = "hiragana", source = "vocabulary.hiragana")
    @Mapping(target = "meaningVi", source = "vocabulary.meaningVi")
    @Mapping(target = "hanViet", source = "vocabulary.hanViet")
    @Mapping(target = "jlptLevel", source = "vocabulary.jlptLevel")
    KanjiRelatedWordResponse toKanjiRelatedWordResponse(VocabularyCharacter entity);

    @Mapping(target = "senses", ignore = true)
    @Mapping(target = "examples", ignore = true)
    @Mapping(target = "kanjiComponents", ignore = true)
    VocabularyDetailResponse toVocabDetail(Vocabulary entity);
}
