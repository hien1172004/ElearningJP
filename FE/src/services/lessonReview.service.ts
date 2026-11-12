import { axiosClient } from './api/axiosClient'
import type {
  ApiResponse,
  LessonReviewQueueParams,
  LessonReviewRequest,
  LessonReviewState,
  LessonReviewSubmitPayload,
  LessonReviewSubmitResult,
  CourseEditLock,
  LessonDraftSnapshot,
  PageResponse,
} from '@/types'

/** Duyệt theo bài — BE: LessonReviewController. */
export const lessonReviewService = {
  getStates: async (courseId: number): Promise<ApiResponse<LessonReviewState[]>> => {
    return axiosClient.get(`/courses/${courseId}/lesson-review-states`)
  },

  /** Lịch sử gửi duyệt theo bài, mới nhất trước; page bắt đầu từ 1. */
  getHistory: async (
    courseId: number,
    page = 1,
    size = 10,
  ): Promise<ApiResponse<PageResponse<LessonReviewRequest>>> => {
    return axiosClient.get(`/courses/${courseId}/lesson-reviews`, { params: { page, size } })
  },

  /** Gửi một hoặc nhiều bài (kèm draftRevision đang thấy); kết quả từng bài trong data.results. */
  submit: async (courseId: number, payload: LessonReviewSubmitPayload): Promise<ApiResponse<LessonReviewSubmitResult>> => {
    return axiosClient.post(`/courses/${courseId}/lesson-reviews`, {
      lessons: payload.lessons,
      note: payload.note || undefined,
    })
  },

  withdraw: async (lessonId: number): Promise<ApiResponse<LessonReviewRequest>> => {
    return axiosClient.post(`/lessons/${lessonId}/review/withdraw`)
  },

  /** ADMIN đưa nháp ADMIN của bài lên bản học viên đang học (không qua duyệt; cần giữ khóa sửa khóa học). */
  saveToLive: async (lessonId: number, majorChange = false): Promise<ApiResponse<null>> => {
    return axiosClient.post(`/lessons/${lessonId}/review/save`, undefined, { params: { majorChange } })
  },

  getQueue: async (params: LessonReviewQueueParams): Promise<ApiResponse<PageResponse<LessonReviewRequest>>> => {
    return axiosClient.get('/lesson-review-requests', { params })
  },

  /** majorChange: báo học viên đã hoàn thành bài (khi khóa đang xuất bản). */
  approve: async (requestId: number, note?: string, majorChange = false): Promise<ApiResponse<LessonReviewRequest>> => {
    return axiosClient.post(`/lesson-review-requests/${requestId}/approve`, { note: note || undefined, majorChange })
  },

  /** Bỏ nháp của phía mình: nháp dựng lại từ bản học viên đang học (cần giữ khóa sửa khóa học). */
  discardDraft: async (lessonId: number): Promise<ApiResponse<null>> => {
    return axiosClient.post(`/lessons/${lessonId}/review/discard-draft`)
  },

  /** Cập nhật bản mới: nháp hiện tại thành nháp cũ chỉ đọc, nháp mới dựng từ bản học viên đang học. */
  rebaseDraft: async (lessonId: number): Promise<ApiResponse<null>> => {
    return axiosClient.post(`/lessons/${lessonId}/draft/rebase`)
  },

  /** Nội dung nháp cũ chỉ đọc của phía mình. */
  getArchivedDraft: async (lessonId: number): Promise<ApiResponse<LessonDraftSnapshot>> => {
    return axiosClient.get(`/lessons/${lessonId}/draft/archived`)
  },

  /** Người đang giữ khóa sửa khóa học của phía mình (null = chưa ai). */
  getEditLock: async (courseId: number): Promise<ApiResponse<CourseEditLock | null>> => {
    return axiosClient.get(`/courses/${courseId}/edit-lock`)
  },

  /** Bắt đầu sửa khóa học (lấy khóa phía mình); takeover = tiếp quản khi người cùng phía đang sửa. */
  acquireEditLock: async (courseId: number, takeover = false): Promise<ApiResponse<CourseEditLock>> => {
    return axiosClient.post(`/courses/${courseId}/edit-lock`, undefined, { params: { takeover } })
  },

  /** Gia hạn khóa sửa (gọi định kỳ khi trang soạn mở). */
  heartbeatEditLock: async (courseId: number): Promise<ApiResponse<null>> => {
    return axiosClient.put(`/courses/${courseId}/edit-lock`)
  },

  /** "Xong" — nhả khóa; phần đã ghi vẫn ở nháp của phía mình. */
  releaseEditLock: async (courseId: number): Promise<ApiResponse<null>> => {
    return axiosClient.delete(`/courses/${courseId}/edit-lock`)
  },

  /** ADMIN xem bản chụp nội dung bài lúc giáo viên gửi (duyệt sẽ đưa đúng bản này lên). */
  getRequestContent: async (requestId: number): Promise<ApiResponse<LessonDraftSnapshot>> => {
    return axiosClient.get(`/lesson-review-requests/${requestId}/content`)
  },

  reject: async (requestId: number, reason: string): Promise<ApiResponse<LessonReviewRequest>> => {
    return axiosClient.post(`/lesson-review-requests/${requestId}/reject`, { reason })
  },
}
