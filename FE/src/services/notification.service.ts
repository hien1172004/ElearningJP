import { axiosClient } from './api/axiosClient'
import type {
  ApiResponse,
  AppNotification,
  NotificationListResponse,
  NotificationType,
  UnreadCountResponse,
} from '@/types'

/**
 * notificationService — REST snapshot cho module thông báo.
 *
 * WebSocket chỉ dùng để đẩy thông báo mới; đọc và đánh dấu đã đọc vẫn đi REST
 * để có thể retry an toàn (idempotent).
 */
export const notificationService = {
  /** Danh sách phân trang (trang bắt đầu từ 1), mới nhất trước — cả đã đọc lẫn chưa đọc. */
  getList: async (
    page = 1,
    size = 20,
    filter?: { unreadOnly?: boolean; types?: NotificationType[] },
  ): Promise<ApiResponse<NotificationListResponse>> => {
    return axiosClient.get('/notifications', {
      params: { page, size, unreadOnly: filter?.unreadOnly || undefined, types: filter?.types?.join(',') || undefined },
    })
  },

  delete: async (id: number): Promise<ApiResponse<void>> => {
    return axiosClient.delete(`/notifications/${id}`)
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
