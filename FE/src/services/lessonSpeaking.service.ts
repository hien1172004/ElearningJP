import { axiosClient } from './api/axiosClient'
import type { LessonSpeakingScenario } from '@/types/course.types'

export const lessonSpeakingService = {
  /**
   * Lấy danh sách tất cả các bài luyện nói của bài học
   */
  async getSpeakingListForLesson(lessonId: number): Promise<LessonSpeakingScenario[]> {
    const res: any = await axiosClient.get(`/lessons/${lessonId}/speaking`)
    const data = res.data
    if (Array.isArray(data)) return data
    return data ? [data] : []
  },

  /**
   * Lấy thông tin bài luyện nói của bài học (tương thích ngược)
   */
  async getSpeakingForLesson(lessonId: number): Promise<LessonSpeakingScenario | null> {
    const list = await this.getSpeakingListForLesson(lessonId)
    return list.length > 0 ? list[0] : null
  },

  /**
   * Gán kịch bản luyện nói vào bài học (Teacher / Admin)
   */
  async assignSpeakingScenario(
    lessonId: number,
    data: { scenarioId: number; minTurns?: number; orderIndex?: number },
  ): Promise<LessonSpeakingScenario> {
    const res: any = await axiosClient.post(`/lessons/${lessonId}/speaking`, data)
    return res.data
  },

  /**
   * AI tự động phân tích bài học và tạo kịch bản luyện nói (Teacher / Admin)
   */
  async generateSpeakingScenario(lessonId: number): Promise<LessonSpeakingScenario> {
    const res: any = await axiosClient.post(`/lessons/${lessonId}/speaking/generate`)
    return res.data
  },

  /**
   * Gỡ kịch bản luyện nói khỏi bài học (Teacher / Admin)
   */
  async removeSpeakingScenario(lessonId: number, scenarioId?: number): Promise<void> {
    const url = scenarioId ? `/lessons/${lessonId}/speaking/${scenarioId}` : `/lessons/${lessonId}/speaking`
    await axiosClient.delete(url)
  },
}

