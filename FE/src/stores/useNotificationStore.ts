import { create } from 'zustand'
import type { AppNotification } from '@/types'

/**
 * Store cho module thông báo.
 *
 * Vai trò: giữ trạng thái "tức thời" (badge + danh sách gần nhất) để chuông báo cập nhật
 * ngay khi STOMP đẩy message, không phải chờ React Query refetch. Snapshot đầy đủ vẫn
 * do React Query quản lý (REST) — store chỉ là lớp đệm real-time.
 */
interface NotificationState {
  items: AppNotification[]
  unreadCount: number
  connected: boolean
  /**
   * Đã nhận được số chưa đọc từ server (REST hoặc sau khi socket kết nối) chưa.
   * Dùng để badge biết khi nào tin store thay vì REST query đang fetch.
   */
  hydrated: boolean
  /** Đặt danh sách (từ REST snapshot). */
  setItems: (items: AppNotification[]) => void
  /** Thêm 1 thông báo mới từ STOMP lên đầu danh sách. */
  pushNotification: (notification: AppNotification) => void
  /** Đánh dấu 1 thông báo đã đọc ở local (badge giảm ngay, không chờ server). */
  markReadLocal: (id: number) => void
  /** Đánh dấu tất cả đã đọc ở local. */
  markAllReadLocal: () => void
  setUnreadCount: (count: number) => void
  setConnected: (connected: boolean) => void
  /** Reset khi logout / unmount socket. */
  reset: () => void
}

/** Số thông báo tối đa giữ trong store — tránh phình bộ nhớ khi chạy lâu. */
const MAX_ITEMS = 50

export const useNotificationStore = create<NotificationState>((set) => ({
  items: [],
  unreadCount: 0,
  connected: false,
  hydrated: false,

  setItems: (items) => set({ items: items.slice(0, MAX_ITEMS) }),

  pushNotification: (notification) =>
    set((state) => {
      // Chống trùng: STOMP có thể gửi lại (reconnect) hoặc REST đã chèn trước đó.
      if (state.items.some((n) => n.id === notification.id)) {
        return state
      }
      return {
        items: [notification, ...state.items].slice(0, MAX_ITEMS),
        unreadCount: notification.read
          ? state.unreadCount
          : state.unreadCount + 1,
      }
    }),

  markReadLocal: (id) =>
    set((state) => {
      const target = state.items.find((n) => n.id === id)
      if (!target || target.read) return state
      return {
        items: state.items.map((n) => (n.id === id ? { ...n, read: true } : n)),
        unreadCount: Math.max(0, state.unreadCount - 1),
      }
    }),

  markAllReadLocal: () =>
    set((state) => ({
      items: state.items.map((n) => (n.read ? n : { ...n, read: true })),
      unreadCount: 0,
    })),

  setUnreadCount: (count) =>
    set({ unreadCount: Math.max(0, count), hydrated: true }),

  setConnected: (connected) => set({ connected }),

  reset: () =>
    set({ items: [], unreadCount: 0, connected: false, hydrated: false }),
}))
