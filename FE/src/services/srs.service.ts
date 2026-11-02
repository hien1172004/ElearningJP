import { axiosClient } from './api/axiosClient'
import type {
  ApiResponse,
  PageResponse,
  SrsAddItemRequest,
  SrsBatchAddRequest,
  SrsDueItem,
  SrsReviewRequest,
  SrsReviewResult,
  SrsStats,
  SrsStatus,
} from '@/types'

/**
 * Service cho module SRS (Spaced Repetition System).
 * Đối chiếu SrsController ở BE — tất cả đều yêu cầu auth.
 */
export const srsService = {
  /** Hàng đợi thẻ đến hạn ôn tập (mặc định 20, tối đa 100). */
  getDueCards: async (limit = 20): Promise<ApiResponse<SrsDueItem[]>> => {
    return axiosClient.get('/srs/due', { params: { limit } })
  },

  /** Danh sách toàn bộ thẻ SRS của user (phân trang + lọc trạng thái). */
  getItems: async (
    params: { status?: SrsStatus; page?: number; size?: number } = {},
  ): Promise<ApiResponse<PageResponse<SrsDueItem>>> => {
    return axiosClient.get('/srs/items', { params })
  },

  /** Thống kê số thẻ (tổng / đến hạn / đang học / đang ôn / thành thạo). */
  getStats: async (): Promise<ApiResponse<SrsStats>> => {
    return axiosClient.get('/srs/stats')
  },

  /** Nộp kết quả lật thẻ (Again/Hard/Good/Easy) → SM-2 cập nhật chu kỳ. */
  reviewCard: async (
    id: number,
    payload: SrsReviewRequest,
  ): Promise<ApiResponse<SrsReviewResult>> => {
    return axiosClient.post(`/srs/items/${id}/review`, payload)
  },

  /** Thêm lẻ 1 từ vựng / chữ Hán / ngữ pháp vào SRS. */
  addItem: async (payload: SrsAddItemRequest): Promise<ApiResponse<SrsDueItem>> => {
    return axiosClient.post('/srs/items', payload)
  },

  /** Thêm toàn bộ kiến thức trong 1 bài học vào SRS. */
  addAllFromLesson: async (lessonId: number): Promise<ApiResponse<number>> => {
    return axiosClient.post(`/srs/lessons/${lessonId}/add-all`)
  },

  /** Thêm hàng loạt theo danh sách ID / lessonId. */
  batchAdd: async (payload: SrsBatchAddRequest): Promise<ApiResponse<number>> => {
    return axiosClient.post('/srs/batch', payload)
  },
}
