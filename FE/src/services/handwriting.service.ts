import { axiosClient } from './api/axiosClient'
import type { ApiResponse } from '@/types'

/** GUIDED = Hanzi-Writer, FREEHAND = viết tự do + AI chấm */
export type HandwritingPracticeMode = 'GUIDED' | 'FREEHAND'
/** MOUSE = chuột/cảm ứng, AIR = viết bằng tay qua camera */
export type HandwritingInputMode = 'MOUSE' | 'AIR'

export interface HandwritingPracticeLogPayload {
  /** Bài học đang luyện; bỏ trống nếu luyện ở từ điển / sổ tay */
  lessonId?: number
  character: string
  practiceMode: HandwritingPracticeMode
  inputMode: HandwritingInputMode
  totalScore: number
  strokeCountExpected?: number
  strokeCountDrawn?: number
  totalMistakes?: number
  detail?: unknown
}

export interface HandwritingModeStat {
  practiceMode: HandwritingPracticeMode
  inputMode: HandwritingInputMode
  attempts: number
  bestScore: number
  lastScore: number
  passed: boolean
  lastPracticedAt: string
}

export interface HandwritingPracticeSummary {
  character: string
  lessonId: number | null
  totalAttempts: number
  stats: HandwritingModeStat[]
}

export interface HandwritingPracticeLogResult {
  logId: number
  passed: boolean
  newBest: boolean
  summary: HandwritingPracticeSummary
}

/** Đối chiếu HandwritingPracticeController ở BE (yêu cầu đăng nhập). */
export const handwritingService = {
  /** Lưu kết quả một lần luyện viết (gọi sau khi chấm điểm xong). */
  saveLog: async (payload: HandwritingPracticeLogPayload): Promise<ApiResponse<HandwritingPracticeLogResult>> => {
    return axiosClient.post('/handwriting/practice-logs', payload)
  },

  /** Tổng hợp điểm của tôi cho 1 chữ (lessonId bỏ trống = ngoài bài học). */
  getSummary: async (character: string, lessonId?: number): Promise<ApiResponse<HandwritingPracticeSummary>> => {
    return axiosClient.get('/handwriting/practice-logs/summary', { params: { character, lessonId } })
  },
}
