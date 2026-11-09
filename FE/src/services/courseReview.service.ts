import { axiosClient } from './api/axiosClient'
import type {
  ApiResponse,
  CourseChanges,
  CourseReviewComment,
  CourseReviewCommentPayload,
  CourseReviewQueueParams,
  CourseReviewRequest,
  PageResponse,
} from '@/types'

/** Luồng duyệt khóa học — BE: CourseReviewController. */
export const courseReviewService = {
  getHistory: async (courseId: number): Promise<ApiResponse<CourseReviewRequest[]>> => {
    return axiosClient.get(`/courses/${courseId}/review-requests`)
  },

  submit: async (courseId: number, note?: string): Promise<ApiResponse<CourseReviewRequest>> => {
    return axiosClient.post(`/courses/${courseId}/review-requests`, { note: note || undefined })
  },

  withdraw: async (courseId: number): Promise<ApiResponse<CourseReviewRequest>> => {
    return axiosClient.post(`/courses/${courseId}/review-requests/withdraw`)
  },

  getQueue: async (params: CourseReviewQueueParams): Promise<ApiResponse<PageResponse<CourseReviewRequest>>> => {
    return axiosClient.get('/course-review-requests', { params })
  },

  approve: async (requestId: number, note?: string): Promise<ApiResponse<CourseReviewRequest>> => {
    return axiosClient.post(`/course-review-requests/${requestId}/approve`, { note: note || undefined })
  },

  reject: async (requestId: number, reason: string): Promise<ApiResponse<CourseReviewRequest>> => {
    return axiosClient.post(`/course-review-requests/${requestId}/reject`, { reason })
  },

  /** Thay đổi của bản đang soạn so với bản học viên đang học. */
  getChanges: async (courseId: number): Promise<ApiResponse<CourseChanges>> => {
    return axiosClient.get(`/courses/${courseId}/pending-changes`)
  },

  /** Nhận xét của lượt gửi duyệt gần nhất. */
  getLatestComments: async (courseId: number): Promise<ApiResponse<CourseReviewComment[]>> => {
    return axiosClient.get(`/courses/${courseId}/review-comments`)
  },

  addComment: async (requestId: number, payload: CourseReviewCommentPayload): Promise<ApiResponse<CourseReviewComment>> => {
    return axiosClient.post(`/course-review-requests/${requestId}/comments`, payload)
  },

  deleteComment: async (commentId: number): Promise<ApiResponse<void>> => {
    return axiosClient.delete(`/course-review-comments/${commentId}`)
  },

  resolveComment: async (commentId: number, resolved: boolean): Promise<ApiResponse<CourseReviewComment>> => {
    return axiosClient.patch(`/course-review-comments/${commentId}/resolve`, { resolved })
  },
}
