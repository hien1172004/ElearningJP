import { axiosClient } from './api/axiosClient'
import type {
  ApiResponse,
  PageResponse,
  DictionaryUnifiedSearch,
  VocabularySummary,
  VocabularyDetail,
  KanjiSummary,
  KanjiDetail,
  GrammarSummary,
  GrammarDetail,
  KanaMatrix,
  KanaCharacter,
  JlptLevel,
  CharacterType,
} from '@/types'

export const dictionaryService = {
  // Tìm kiếm tổng hợp đa năng
  searchUnified: async (keyword?: string, level?: JlptLevel): Promise<ApiResponse<DictionaryUnifiedSearch>> => {
    return axiosClient.get('/dictionary/search', {
      params: { keyword, level },
    })
  },

  // Tìm kiếm từ vựng phân trang
  searchVocabularies: async (params: {
    keyword?: string
    level?: JlptLevel
    page?: number
    size?: number
  }): Promise<ApiResponse<PageResponse<VocabularySummary>>> => {
    return axiosClient.get('/dictionary/search/vocab', { params })
  },

  // Chi tiết từ vựng
  getVocabularyDetail: async (id: number): Promise<ApiResponse<VocabularyDetail>> => {
    return axiosClient.get(`/dictionary/vocab/${id}`)
  },

  // Tìm kiếm Hán tự phân trang
  searchKanjis: async (params: {
    keyword?: string
    level?: JlptLevel
    page?: number
    size?: number
  }): Promise<ApiResponse<PageResponse<KanjiSummary>>> => {
    return axiosClient.get('/dictionary/search/kanji', { params })
  },

  // Chi tiết Hán tự
  getKanjiDetail: async (character: string): Promise<ApiResponse<KanjiDetail>> => {
    return axiosClient.get(`/dictionary/kanji/${encodeURIComponent(character)}`)
  },

  // Tìm kiếm Ngữ pháp phân trang
  searchGrammars: async (params: {
    keyword?: string
    level?: JlptLevel
    page?: number
    size?: number
  }): Promise<ApiResponse<PageResponse<GrammarSummary>>> => {
    return axiosClient.get('/dictionary/search/grammar', { params })
  },

  // Chi tiết Ngữ pháp
  getGrammarDetail: async (id: number): Promise<ApiResponse<GrammarDetail>> => {
    return axiosClient.get(`/dictionary/grammar/${id}`)
  },

  // Ma trận bảng chữ cái
  getKanaMatrix: async (type: CharacterType = 'HIRAGANA'): Promise<ApiResponse<KanaMatrix>> => {
    return axiosClient.get('/dictionary/kana', { params: { type } })
  },

  // Chi tiết chữ cái Kana
  getKanaDetail: async (character: string): Promise<ApiResponse<KanaCharacter>> => {
    return axiosClient.get(`/dictionary/kana/${encodeURIComponent(character)}`)
  },

  // Tra cứu nhanh popup
  quickLookup: async (text: string): Promise<ApiResponse<VocabularySummary[]>> => {
    return axiosClient.get('/dictionary/quick-lookup', { params: { text } })
  },

  // Thêm mục từ điển vào SRS Flashcard
  addToSrs: async (targetType: 'VOCABULARY' | 'KANJI' | 'GRAMMAR', targetId: number): Promise<ApiResponse<any>> => {
    const payload =
      targetType === 'VOCABULARY'
        ? { vocabId: targetId }
        : targetType === 'KANJI'
        ? { characterId: targetId }
        : { grammarId: targetId }
    return axiosClient.post('/srs/items', payload)
  },
}
