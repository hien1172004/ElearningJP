import { axiosClient } from './api/axiosClient'
import type {
  ApiResponse,
  PageResponse,
  ExamSummary,
  ExamDetail,
  ExamTakingData,
  ExamResult,
  ExamReviewData,
  ExamAttemptHistory,
  AdminExamPreview,
  UpdateQuestionInlinePayload,
  JlptLevel,
  ExamType,
  AttemptType,
  LeaderboardData,
} from '@/types'

export const examService = {
  // Lấy bảng xếp hạng thi thử theo chuẩn JLPT
  getLeaderboard: async (params?: {
    level?: JlptLevel
    timeframe?: 'WEEK' | 'MONTH' | 'ALL'
  }): Promise<ApiResponse<LeaderboardData>> => {
    return axiosClient.get('/exams/leaderboard', { params })
  },

  // Lấy danh sách đề thi đã xuất bản
  getPublishedExams: async (params?: {
    level?: JlptLevel
    examType?: ExamType
    page?: number
    size?: number
  }): Promise<ApiResponse<PageResponse<ExamSummary>>> => {
    return axiosClient.get('/exams', { params })
  },

  // Lấy thông tin chi tiết cấu trúc đề thi
  getExamDetail: async (id: number): Promise<ApiResponse<ExamDetail>> => {
    return axiosClient.get(`/exams/${id}`)
  },

  // Bắt đầu làm bài thi
  startExam: async (
    id: number,
    payload: { attemptType: AttemptType; sectionId?: number }
  ): Promise<ApiResponse<ExamTakingData>> => {
    return axiosClient.post(`/exams/${id}/attempts`, payload)
  },

  // Tự động lưu đáp án câu hỏi (Auto-save)
  saveAnswer: async (
    attemptId: number,
    payload: {
      questionId: number
      selectedOptionId?: number
      orderedOptionIds?: number[]
      audioPlayCount?: number
    }
  ): Promise<ApiResponse<void>> => {
    return axiosClient.put(`/exam-attempts/${attemptId}/answers`, payload)
  },

  // Nộp bài thi
  submitExam: async (
    attemptId: number,
    payload?: { answers?: any[] }
  ): Promise<ApiResponse<ExamResult>> => {
    return axiosClient.post(`/exam-attempts/${attemptId}/submit`, payload || {})
  },

  // Xem phiếu điểm kết quả
  getResult: async (attemptId: number): Promise<ApiResponse<ExamResult>> => {
    return axiosClient.get(`/exam-attempts/${attemptId}/result`)
  },
  getExamResult: async (attemptId: number): Promise<ExamResult> => {
    const res: any = await axiosClient.get(`/exam-attempts/${attemptId}/result`)
    return res.data
  },

  // Xem lại toàn bộ đề thi chi tiết dạng cuộn dọc
  getReview: async (attemptId: number): Promise<ApiResponse<ExamReviewData>> => {
    return axiosClient.get(`/exam-attempts/${attemptId}/review`)
  },
  getExamReview: async (attemptId: number): Promise<ExamReviewData> => {
    const res: any = await axiosClient.get(`/exam-attempts/${attemptId}/review`)
    return res.data
  },

  // Lấy toàn bộ lịch sử thi cử của học viên
  getMyHistory: async (params?: {
    page?: number
    size?: number
  }): Promise<ApiResponse<PageResponse<ExamAttemptHistory>>> => {
    return axiosClient.get('/exam-attempts/my-history', { params })
  },
  getMyExamHistory: async (): Promise<ExamAttemptHistory[]> => {
    const res: any = await axiosClient.get('/exam-attempts/my-history')
    const payload = res?.data ?? res
    if (Array.isArray(payload?.content)) return payload.content
    if (Array.isArray(payload?.items)) return payload.items
    if (Array.isArray(payload)) return payload
    if (Array.isArray(res?.content)) return res.content
    if (Array.isArray(res?.items)) return res.items
    return []
  },

  // ===== DÀNH CHO ADMIN / GIÁO VIÊN =====
  getAdminPreview: async (id: number): Promise<ApiResponse<AdminExamPreview>> => {
    return axiosClient.get(`/admin/exams/${id}/preview`)
  },
  getAdminExamPreview: async (id: number): Promise<AdminExamPreview> => {
    const res: any = await axiosClient.get(`/admin/exams/${id}/preview`)
    return res.data
  },

  updateQuestionInline: async (
    examId: number,
    questionId: number,
    payload: UpdateQuestionInlinePayload
  ): Promise<any> => {
    const res: any = await axiosClient.put(
      `/admin/exams/${examId}/questions/${questionId}`,
      payload
    )
    return res.data
  },

  importExam: async (payload: any): Promise<number> => {
    const res: any = await axiosClient.post('/admin/exams', payload)
    return res.data
  },

  addQuestion: async (
    examId: number,
    sectionId: number,
    payload: any
  ): Promise<number> => {
    const res: any = await axiosClient.post(
      `/admin/exams/${examId}/sections/${sectionId}/questions`,
      payload
    )
    return res.data
  },

  deleteQuestion: async (
    examId: number,
    questionId: number
  ): Promise<void> => {
    await axiosClient.delete(
      `/admin/exams/${examId}/questions/${questionId}`
    )
  },

  clearSectionQuestions: async (
    examId: number,
    sectionId: number
  ): Promise<void> => {
    await axiosClient.delete(
      `/admin/exams/${examId}/sections/${sectionId}/questions`
    )
  },

  saveSnapshot: async (
    examId: number,
    snapshot?: any
  ): Promise<ApiResponse<AdminExamPreview>> => {
    return axiosClient.post(`/admin/exams/${examId}/snapshot`, snapshot)
  },

  getSnapshot: async (examId: number): Promise<ApiResponse<AdminExamPreview>> => {
    return axiosClient.get(`/admin/exams/${examId}/snapshot`)
  },

  rollbackExam: async (
    examId: number,
    snapshot?: any
  ): Promise<ApiResponse<AdminExamPreview>> => {
    return axiosClient.post(`/admin/exams/${examId}/rollback`, snapshot)
  },

  getCourseExams: async (courseId: number): Promise<ExamSummary[]> => {
    const res: any = await axiosClient.get(`/exams/course/${courseId}`)
    return res.data || []
  },

  publishExam: async (id: number): Promise<ApiResponse<void>> => {
    return axiosClient.post(`/admin/exams/${id}/publish`)
  },

  updateSectionAudio: async (
    examId: number,
    sectionId: number,
    audioUrl: string
  ): Promise<void> => {
    await axiosClient.put(`/admin/exams/${examId}/sections/${sectionId}/audio`, {
      audioUrl,
    })
  },

  updatePassage: async (
    examId: number,
    passageId: number,
    payload: {
      title?: string
      contentJp?: string
      translationVi?: string
      imageUrl?: string
    }
  ): Promise<void> => {
    await axiosClient.put(`/admin/exams/${examId}/passages/${passageId}`, payload)
  },

  deletePassage: async (
    examId: number,
    passageId: number
  ): Promise<void> => {
    await axiosClient.delete(`/admin/exams/${examId}/passages/${passageId}`)
  },

  deleteExam: async (id: number): Promise<void> => {
    await axiosClient.delete(`/admin/exams/${id}`)
  },

  getAllAdminExams: async (params?: {
    level?: JlptLevel
    examType?: ExamType
    published?: boolean
    page?: number
    size?: number
  }): Promise<ApiResponse<PageResponse<ExamSummary>>> => {
    return axiosClient.get('/admin/exams', { params })
  },

  unpublishExam: async (id: number): Promise<ApiResponse<void>> => {
    return axiosClient.post(`/admin/exams/${id}/unpublish`)
  },

  reorderQuestions: async (examId: number): Promise<AdminExamPreview> => {
    const res = await axiosClient.post<ApiResponse<AdminExamPreview>>(`/admin/exams/${examId}/reorder-questions`)
    return res.data.data!
  },

  cloneExam: async (examId: number): Promise<number> => {
    const res: any = await axiosClient.post(`/admin/exams/${examId}/clone`)
    return res.data
  },

  regradeExam: async (examId: number): Promise<number> => {
    const res: any = await axiosClient.post(`/admin/exams/${examId}/regrade`)
    return res.data
  },
}

