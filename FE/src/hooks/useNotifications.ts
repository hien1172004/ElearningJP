import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import type { AxiosError } from 'axios'
import { notificationService } from '@/services/notification.service'
import { QUERY_KEYS } from '@/lib/queryKeys'
import { useAuthStore } from '@/stores/useAuthStore'
import { useNotificationStore } from '@/stores/useNotificationStore'
import type {
  AppNotification,
  NotificationListResponse,
  UnreadCountResponse,
} from '@/types'

/**
 * Hooks cho module thông báo (lớp REST).
 *
 * Nguyên tắc: WebSocket lo phần "đẩy nhanh", REST lo phần "đúng". Vì vậy
 * `useUnreadCount` vẫn có `refetchInterval` làm lưới an toàn khi socket rớt.
 */

// ============================================================================
// Queries
// ============================================================================

/** Số chưa đọc — refetch định kỳ 60s làm fallback khi WebSocket mất kết nối. */
export function useUnreadCount() {
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated)

  return useQuery<UnreadCountResponse, AxiosError>({
    queryKey: QUERY_KEYS.notification.unreadCount,
    queryFn: async () => {
      const res = await notificationService.getUnreadCount()
      return res?.data ?? { count: 0 }
    },
    enabled: isAuthenticated,
    staleTime: 30_000,
    refetchInterval: 60_000,
    refetchOnWindowFocus: true,
    retry: 1,
  })
}

/** 20 thông báo chưa đọc mới nhất — cho dropdown trên chuông. */
export function useRecentNotifications(options?: { enabled?: boolean }) {
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated)

  return useQuery<AppNotification[], AxiosError>({
    queryKey: QUERY_KEYS.notification.recent,
    queryFn: async () => {
      const res = await notificationService.getRecent()
      return res?.data ?? []
    },
    enabled: isAuthenticated && (options?.enabled ?? true),
    staleTime: 30_000,
    retry: 1,
  })
}

/** Danh sách phân trang (trang thông báo đầy đủ). */
export function useNotifications(page = 0, size = 20) {
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated)

  return useQuery<NotificationListResponse, AxiosError>({
    queryKey: QUERY_KEYS.notification.list(page),
    queryFn: async () => {
      const res = await notificationService.getList(page, size)
      return (
        res?.data ?? {
          items: [],
          total: 0,
          page,
          size,
          totalPages: 0,
          unreadCount: 0,
        }
      )
    },
    enabled: isAuthenticated,
    staleTime: 30_000,
    retry: 1,
  })
}

// ============================================================================
// Mutations
// ============================================================================

/**
 * Đánh dấu 1 thông báo đã đọc.
 *
 * Cập nhật store local TRƯỚC khi gọi API để badge giảm ngay (optimistic), rồi
 * invalidate để đồng bộ lại với server.
 */
export function useMarkAsRead() {
  const queryClient = useQueryClient()
  const markReadLocal = useNotificationStore((s) => s.markReadLocal)

  return useMutation<void, AxiosError, number>({
    mutationFn: async (id: number) => {
      await notificationService.markAsRead(id)
    },
    onMutate: (id) => {
      markReadLocal(id)
    },
    onSettled: () => {
      queryClient.invalidateQueries({ queryKey: QUERY_KEYS.notification.all })
    },
  })
}

/** Đánh dấu tất cả đã đọc. */
export function useMarkAllAsRead() {
  const queryClient = useQueryClient()
  const markAllReadLocal = useNotificationStore((s) => s.markAllReadLocal)

  return useMutation<void, AxiosError, void>({
    mutationFn: async () => {
      await notificationService.markAllAsRead()
    },
    onMutate: () => {
      markAllReadLocal()
    },
    onSettled: () => {
      queryClient.invalidateQueries({ queryKey: QUERY_KEYS.notification.all })
    },
  })
}
