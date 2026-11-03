import type { SrsRating } from './srs.types'
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
  wordCount: number
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
  vocabularyId?: number | null
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
  vocabularyId?: number | null
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

