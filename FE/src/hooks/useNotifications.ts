import {
  type InfiniteData,
  type QueryClient,
  useInfiniteQuery,
  useMutation,
  useQuery,
  useQueryClient,
} from '@tanstack/react-query'
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

/** Số thông báo mỗi lần tải trong dropdown chuông. */
export const NOTIFICATION_FEED_PAGE_SIZE = 10

/**
 * Danh sách thông báo cuộn vô hạn cho dropdown chuông — cả đã đọc lẫn chưa đọc,
 * mới nhất trước, mỗi trang {@link NOTIFICATION_FEED_PAGE_SIZE} mục (trang bắt đầu từ 1).
 */
export function useNotificationFeed(options?: { enabled?: boolean }) {
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated)
  const size = NOTIFICATION_FEED_PAGE_SIZE

  return useInfiniteQuery<
    NotificationListResponse,
    AxiosError,
    InfiniteData<NotificationListResponse, number>,
    readonly unknown[],
    number
  >({
    queryKey: QUERY_KEYS.notification.feed(size),
    initialPageParam: 1,
    queryFn: async ({ pageParam }) => {
      const res = await notificationService.getList(pageParam, size)
      return (
        res?.data ?? {
          items: [],
          total: 0,
          page: pageParam,
          size,
          totalPages: 0,
          unreadCount: 0,
        }
      )
    },
    getNextPageParam: (last) =>
      last.page < last.totalPages ? last.page + 1 : undefined,
    enabled: isAuthenticated && (options?.enabled ?? true),
    staleTime: 30_000,
    retry: 1,
  })
}

/**
 * Đánh dấu đã đọc ngay trong cache của feed (optimistic) để dòng thông báo đổi màu
 * tức thì; invalidate sau đó sẽ đồng bộ lại với server.
 */
function markReadInFeed(
  queryClient: QueryClient,
  isTarget: (n: AppNotification) => boolean,
) {
  queryClient.setQueriesData<InfiniteData<NotificationListResponse, number>>(
    { queryKey: QUERY_KEYS.notification.feedAll },
    (data) =>
      data && {
        ...data,
        pages: data.pages.map((page) => ({
          ...page,
          items: page.items.map((n) => (isTarget(n) ? { ...n, read: true } : n)),
        })),
      },
  )
}

/** Danh sách phân trang (trang thông báo đầy đủ, trang bắt đầu từ 1). */
export function useNotifications(page = 1, size = 20) {
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
      markReadInFeed(queryClient, (n) => n.id === id)
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
      markReadInFeed(queryClient, () => true)
    },
    onSettled: () => {
      queryClient.invalidateQueries({ queryKey: QUERY_KEYS.notification.all })
    },
  })
}
