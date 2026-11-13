import { axiosClient } from './api/axiosClient'
import type { ApiResponse } from '@/types'

// ============================================================================
// In phiếu luyện viết (PDF ô chữ Mễ + thứ tự nét) cho mục Chữ Hán / Từ vựng của bài học:
//   POST /lessons/{lessonId}/practice-sheet/check   → các mục có ký tự thiếu dữ liệu nét
//   POST /lessons/{lessonId}/practice-sheet         → file PDF (nhóm theo từng nhóm của bài)
// ============================================================================

export type PracticeSheetType = 'KANJI' | 'VOCABULARY'

export interface PracticeSheetRequest {
  sectionType: PracticeSheetType
  /** Id các item (LessonKanjiItem / LessonVocabularyItem) muốn in. */
  itemIds: number[]
}

export interface MissingStrokeItem {
  itemId: number
  groupTitle: string
  word: string
  missingChars: string[]
}

export interface PracticeSheetCheckResponse {
  totalItems: number
  missingItems: MissingStrokeItem[]
}

export const practiceSheetService = {
  check: (lessonId: number, body: PracticeSheetRequest): Promise<ApiResponse<PracticeSheetCheckResponse>> =>
    axiosClient.post(`/lessons/${lessonId}/practice-sheet/check`, body),

  /** Tạo PDF (axios interceptor trả thẳng `response.data` là Blob). */
  generate: async (lessonId: number, body: PracticeSheetRequest): Promise<Blob> => {
    const data = await axiosClient.post(`/lessons/${lessonId}/practice-sheet`, body, { responseType: 'blob' })
    return new Blob([data as unknown as BlobPart], { type: 'application/pdf' })
  },

  /** Lưu Blob PDF đã tạo về máy. */
  saveFile: (blob: Blob, fileName: string): void => {
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = fileName
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    window.URL.revokeObjectURL(url)
  },
}
