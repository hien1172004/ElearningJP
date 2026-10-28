import { axiosClient } from './api/axiosClient'
import type {
  ApiResponse,
  AppNotification,
  NotificationListResponse,
  UnreadCountResponse,
} from '@/types'

/**
 * notificationService — REST snapshot cho module thông báo.
 *
 * WebSocket chỉ dùng để đẩy thông báo mới; đọc và đánh dấu đã đọc vẫn đi REST
 * để có thể retry an toàn (idempotent).
 */
export const notificationService = {
  /** Danh sách phân trang, mới nhất trước. */
  getList: async (
    page = 0,
    size = 20,
  ): Promise<ApiResponse<NotificationListResponse>> => {
    return axiosClient.get('/notifications', { params: { page, size } })
  },

  /** Số chưa đọc — cho badge trên chuông. */
  getUnreadCount: async (): Promise<ApiResponse<UnreadCountResponse>> => {
    return axiosClient.get('/notifications/unread-count')
  },

  /** 20 thông báo chưa đọc mới nhất — cho dropdown. */
  getRecent: async (): Promise<ApiResponse<AppNotification[]>> => {
    return axiosClient.get('/notifications/recent')
  },

  /** Đánh dấu 1 thông báo đã đọc (idempotent). */
  markAsRead: async (id: number): Promise<ApiResponse<void>> => {
    return axiosClient.post(`/notifications/${id}/read`)
  },

  /** Đánh dấu tất cả đã đọc. */
  markAllAsRead: async (): Promise<ApiResponse<void>> => {
    return axiosClient.post('/notifications/read-all')
  },
}
