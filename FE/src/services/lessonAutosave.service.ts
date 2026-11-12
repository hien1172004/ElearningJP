import { axiosClient } from './api/axiosClient'
import type { ApiResponse, LessonAutosave } from '@/types'

/** Tự lưu nội dung chưa lưu theo người và theo bài — BE: LessonAutosaveController. */
export const lessonAutosaveService = {
  list: async (lessonId: number): Promise<ApiResponse<LessonAutosave[]>> => {
    return axiosClient.get(`/lessons/${lessonId}/autosaves`)
  },

  save: async (lessonId: number, formKey: string, content: unknown): Promise<ApiResponse<null>> => {
    return axiosClient.put(`/lessons/${lessonId}/autosaves/${encodeURIComponent(formKey)}`, { content })
  },

  remove: async (lessonId: number, formKey: string): Promise<ApiResponse<null>> => {
    return axiosClient.delete(`/lessons/${lessonId}/autosaves/${encodeURIComponent(formKey)}`)
  },
}
