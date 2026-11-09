import type { SrsRating } from './srs.types'

/** Loại kiến thức của thẻ trong sổ tay (V54): Từ vựng / Chữ Hán / Ngữ pháp */
export type NotebookItemType = 'VOCAB' | 'KANJI' | 'GRAMMAR'
export interface NotebookGroup {
  id: number
  userId: number
  name: string
  displayOrder: number
  notebookCount: number
  createdAt: string
  updatedAt: string
}

export interface CreateNotebookGroupRequest {
  name: string
  displayOrder?: number
}

export interface UpdateNotebookGroupRequest {
  name?: string
  displayOrder?: number
}

export interface Notebook {
  id: number
  userId: number
  authorName?: string
  authorAvatar?: string
  groupId?: number | null
  groupName?: string | null
  name: string
  description?: string | null
  coverColor?: string | null
  coverIcon?: string | null
  isPublic: boolean
  sharingEnabled: boolean
  shareCode?: string | null
  jlptLevel?: 'N1' | 'N2' | 'N3' | 'N4' | 'N5' | null
  /** Tổng số thẻ (mọi loại) */
  wordCount: number
  vocabCount?: number
  kanjiCount?: number
  grammarCount?: number
  likeCount: number
  viewCount: number
  downloadCount: number
  srEnabled: boolean
  isLiked?: boolean
  isOwner?: boolean
  isImported?: boolean
  userNotebookId?: number | null
  createdAt: string
  updatedAt: string
}

export interface CreateNotebookRequest {
  name: string
  description?: string
  groupId?: number | null
  coverColor?: string
  coverIcon?: string
  jlptLevel?: string | null
  isPublic?: boolean
}

export interface UpdateNotebookRequest {
  name?: string
  description?: string
  groupId?: number | null
  coverColor?: string
  coverIcon?: string
  jlptLevel?: string | null
  isPublic?: boolean
  srEnabled?: boolean
}

export interface NotebookGroupWithNotebooks {
  id: number
  name: string
  displayOrder: number
  notebooks: Notebook[]
  createdAt?: string
  updatedAt?: string
}

export interface NotebookSearchParams {
  keyword?: string
  jlptLevel?: string
  groupId?: number
  page?: number
  size?: number
  sort?: string
}

export type MemoryLevel = 'NOT_LEARNED' | 'BARELY' | 'MODERATE' | 'MASTERED'

export interface NotebookWord {
  id: number
  notebookId: number
  itemType?: NotebookItemType
  vocabularyId?: number | null
  characterId?: number | null
  grammarId?: number | null
  sourceLessonId?: number | null
  sourceLessonTitle?: string | null
  /** KANJI: âm On / Kun / số nét (lấy từ từ điển) */
  onyomi?: string[] | null
  kunyomi?: string[] | null
  strokeCount?: number | null
  /** GRAMMAR: cấu trúc đầy đủ + cách dùng (lấy từ từ điển) */
  grammarStructure?: string | null
  usageNotes?: string | null
  word: string
  reading?: string | null
  sinoVietnamese?: string | null
  hanViet?: string | null
  meaning?: string | null
  partOfSpeech?: string | null
  audioUrl?: string | null
  exampleSentence?: string | null
  exampleMeaning?: string | null
  jlptLevel?: string | null
  userNote?: string | null
  displayOrder: number
  memoryLevel: MemoryLevel
  repetitionCount: number
  intervalDays: number
  nextReviewAt?: string | null
  lastReviewedAt?: string | null
  easeFactor: number
  createdAt: string
  updatedAt: string
}

export interface CreateNotebookWordRequest {
  word: string
  reading?: string
  sinoVietnamese?: string
  hanViet?: string
  meaning?: string
  partOfSpeech?: string
  audioUrl?: string
  exampleSentence?: string
  exampleMeaning?: string
  jlptLevel?: string
  vocabularyId?: number
  userNote?: string
  /** Bỏ trống = VOCAB */
  itemType?: NotebookItemType
  characterId?: number
  grammarId?: number
}

export interface UpdateNotebookWordRequest {
  word?: string
  reading?: string
  sinoVietnamese?: string
  hanViet?: string
  meaning?: string
  partOfSpeech?: string
  audioUrl?: string
  exampleSentence?: string
  exampleMeaning?: string
  jlptLevel?: string
  vocabularyId?: number
  userNote?: string
  displayOrder?: number
  memoryLevel?: MemoryLevel
}

export interface ExcelImportResultResponse {
  totalProcessed: number
  successCount: number
  errorCount: number
  errorMessages: string[]
  // Aliases for compatibility
  totalRows?: number
  importedCount?: number
  skippedCount?: number
}

export interface NotebookStatsResponse {
  totalNotebooks?: number
  totalWords: number
  dueReviewCount?: number
  notLearnedCount: number
  unlearnedCount?: number
  barelyCount: number
  moderateCount: number
  masteredCount: number
  srEnabled: boolean
}

// SrsRating imported from srs.types

export interface NotebookReviewItem {
  id: number
  notebookId: number
  itemType?: NotebookItemType
  vocabularyId?: number | null
  characterId?: number | null
  grammarId?: number | null
  onyomi?: string[] | null
  kunyomi?: string[] | null
  strokeCount?: number | null
  grammarStructure?: string | null
  usageNotes?: string | null
  word: string
  reading?: string | null
  sinoVietnamese?: string | null
  hanViet?: string | null
  meaning?: string | null
  partOfSpeech?: string | null
  audioUrl?: string | null
  exampleSentence?: string | null
  exampleMeaning?: string | null
  jlptLevel?: string | null
  userNote?: string | null
  memoryLevel: MemoryLevel
  intervalDays: number
  repetitionCount: number
  quizMeaningOptions: string[]
  quizWordOptions: string[]
}

export interface NotebookReviewSubmitRequest {
  wordId: number
  rating: SrsRating
  timeSpentSeconds?: number
}

export interface NotebookReviewSubmitResponse {
  wordId: number
  memoryLevel: MemoryLevel
  repetitionCount: number
  intervalDays: number
  easeFactor: number
  nextReviewAt: string
}

export interface KanjiPdfMetaResponse {
  totalPages: number
}

export interface KanjiPdfPreviewResult {
  imageUrl: string
  currentPage: number
  totalPages: number
}


// ===== Lưu kiến thức từ bài học vào sổ tay (V54) =====

export interface SaveLessonItemRef {
  type: 'KANJI' | 'VOCABULARY' | 'GRAMMAR'
  itemId: number
}

export interface SaveLessonItemsRequest {
  lessonId: number
  /** Sổ đã có — hoặc truyền newNotebookName để tạo sổ mới */
  notebookId?: number
  newNotebookName?: string
  groupId?: number | null
  /** Ưu tiên: items → sectionIds → cả bài */
  sectionIds?: number[]
  items?: SaveLessonItemRef[]
}

export interface SaveLessonItemsResponse {
  notebookId: number
  notebookName: string
  createdNotebook: boolean
  addedCount: number
  skippedCount: number
  addedVocab: number
  addedKanji: number
  addedGrammar: number
}

export interface NotebookSavedIds {
  vocabularyIds: number[]
  characterIds: number[]
  grammarIds: number[]
}
