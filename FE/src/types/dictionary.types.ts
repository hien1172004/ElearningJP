export type JlptLevel = 'N5' | 'N4' | 'N3' | 'N2' | 'N1'
export type CharacterType = 'HIRAGANA' | 'KATAKANA' | 'KANJI'

export interface CharacterStroke {
  strokeNumber: number
  svgPathData: string
}

export interface KanjiSimple {
  id: number
  character: string
  hanViet?: string
  meaningVi?: string
  strokeCount?: number
  onyomi?: string[]
  kunyomi?: string[]
  jlptLevel?: JlptLevel
}

export interface VocabularySense {
  id?: number
  senseNo: number
  meaningVi: string
  meaningEn?: string
  partOfSpeech?: string
  usageNotes?: string
}

export interface VocabularyExample {
  id?: number
  exampleJp: string
  exampleVi: string
  exampleRomaji?: string
  audioUrl?: string
}

export interface VocabularySummary {
  id: number
  word: string
  hiragana: string
  romaji?: string
  hanViet?: string
  meaningVi: string
  jlptLevel?: JlptLevel
  partOfSpeech?: string
  audioUrl?: string
}

export interface VocabularyDetail extends VocabularySummary {
  meaningEn?: string
  exampleJp?: string
  exampleVi?: string
  senses?: VocabularySense[]
  examples?: VocabularyExample[]
  kanjiComponents?: KanjiSimple[]
}

export interface KanjiSummary {
  id: number
  character: string
  strokeCount: number
  onyomi?: string[]
  kunyomi?: string[]
  hanViet?: string
  meaningVi?: string
  jlptLevel?: JlptLevel
  radicals?: string[]
}

export interface KanjiRelatedWord {
  vocabId: number
  word: string
  hiragana: string
  meaningVi: string
  hanViet?: string
  jlptLevel?: JlptLevel
  readingType?: string
  reading?: string
}

export interface KanjiDetail extends KanjiSummary {
  meaningEn?: string
  romaji?: string
  strokes: CharacterStroke[]
  wordsByReading?: Record<string, KanjiRelatedWord[]>
  allRelatedWords?: KanjiRelatedWord[]
}

export interface GrammarSummary {
  id: number
  title: string
  structure: string
  jlptLevel: JlptLevel
  meaningVi: string
}

export interface GrammarExampleItem {
  jp?: string
  vi?: string
  romaji?: string
}

export interface GrammarDetail extends GrammarSummary {
  usageNotes?: string
  examplesJson?: GrammarExampleItem[] | any
}

export interface KanaCharacter {
  id: number
  character: string
  charType: CharacterType
  romaji?: string
  meaningVi?: string
  strokeCount?: number
  audioUrl?: string
  strokes?: CharacterStroke[]
}

export interface KanaRowGroup {
  rowName: string
  rowRomaji: string
  characters: KanaCharacter[]
}

export interface KanaMatrix {
  charType: CharacterType
  seion: KanaRowGroup[]
  dakuon: KanaRowGroup[]
  yoon: KanaRowGroup[]
}

export interface DictionaryUnifiedSearch {
  keyword: string
  vocabularies: VocabularySummary[]
  kanjis: KanjiSummary[]
  grammars: GrammarSummary[]
}
