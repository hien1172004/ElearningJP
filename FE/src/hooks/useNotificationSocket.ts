import { useEffect, useRef } from 'react'
import { Client, type IMessage } from '@stomp/stompjs'
import { useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { getWebSocketUrl } from '@/lib/wsUrl'
import { QUERY_KEYS } from '@/lib/queryKeys'
import { useAuthStore } from '@/stores/useAuthStore'
import { useNotificationStore } from '@/stores/useNotificationStore'
import { storage } from '@/utils/storage'
import { authService } from '@/services/auth.service'
import { notificationService } from '@/services/notification.service'
import type { AppNotification } from '@/types'

/** Destination client subscribe — khớp `convertAndSendToUser(id, "/queue/notifications", …)`. */
const NOTIFICATION_DESTINATION = '/user/queue/notifications'

/** Id cố định cho toast lỗi kết nối — tránh spam mỗi 5 giây khi reconnect. */
const RECONNECT_TOAST_ID = 'notification-socket-error'

/** Mutex để nhiều lần reconnect liên tiếp không gọi refresh song song. */
let refreshing: Promise<string | null> | null = null

/**
 * Kết nối STOMP tới BE và đẩy thông báo vào store khi có message.
 *
 * <p><b>Vì sao dùng `beforeConnect` thay vì `connectHeaders`?</b>
 * `connectHeaders` được chụp lại một lần lúc tạo Client; sau khi token hết hạn, mỗi
 * lần auto-reconnect client sẽ gửi lại token CŨ và bị từ chối mãi mãi. `beforeConnect`
 * chạy ngay trước mỗi lần (re)connect nên luôn lấy token mới nhất từ storage.</p>
 *
 * <p>Mount 1 lần ở layout (MainLayout). Hook tự bỏ qua khi chưa đăng nhập và tự
 * deactivate khi logout/unmount.</p>
 */
export function useNotificationSocket() {
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated)
  const queryClient = useQueryClient()

  const pushNotification = useNotificationStore((s) => s.pushNotification)
  const setConnected = useNotificationStore((s) => s.setConnected)
  const setUnreadCount = useNotificationStore((s) => s.setUnreadCount)
  const reset = useNotificationStore((s) => s.reset)

  const clientRef = useRef<Client | null>(null)

  useEffect(() => {
    if (!isAuthenticated) {
      return
    }

    let disposed = false
    let everConnected = false

    const client = new Client({
      brokerURL: getWebSocketUrl(),
      // raw WebSocket — KHÔNG dùng SockJS (BE không bật .withSockJS()).
      reconnectDelay: 5000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      debug: () => {
        /* tắt log STOMP mặc định cho đỡ ồn */
      },

      beforeConnect: (c) => {
        // Chạy trước MỖI lần (re)connect → luôn dùng token mới nhất.
        const token = storage.getAccessToken()
        c.connectHeaders = token ? { Authorization: `Bearer ${token}` } : {}
      },

      onConnect: () => {
        if (disposed) {
          return
        }
        everConnected = true
        setConnected(true)
        toast.dismiss(RECONNECT_TOAST_ID)

        client.subscribe(NOTIFICATION_DESTINATION, (message: IMessage) => {
          try {
            const notification = JSON.parse(message.body) as AppNotification
            pushNotification(notification)
            toast(notification.title, {
              description: notification.body,
              duration: 6000,
            })
          } catch {
            // Payload hỏng: bỏ qua, không để listener chết.
          }
        })

        // Đồng bộ lại sau khi (re)connect — bù các thông báo bị lỡ khi mất mạng.
        queryClient.invalidateQueries({ queryKey: QUERY_KEYS.notification.all })
        void hydrateUnreadCount(setUnreadCount)
      },

      onWebSocketClose: () => {
        if (!disposed) {
          setConnected(false)
        }
      },

      onStompError: (frame) => {
        if (disposed) {
          return
        }
        setConnected(false)

        // Chỉ báo 1 lần (sau khi đã từng kết nối) để không spam toast mỗi 5 giây.
        if (everConnected) {
          toast.error(frame.headers?.message ?? 'Mất kết nối thông báo', {
            id: RECONNECT_TOAST_ID,
            description: 'Đang thử kết nối lại…',
          })
        }

        // CONNECT bị từ chối thường do token hết hạn → refresh để lần reconnect sau
        // (beforeConnect) gửi token mới.
        void refreshAccessToken()
      },
    })

    client.activate()
    clientRef.current = client

    return () => {
      disposed = true
      clientRef.current = null
      setConnected(false)
      void client.deactivate()
    }
  }, [
    isAuthenticated,
    pushNotification,
    queryClient,
    setConnected,
    setUnreadCount,
  ])

  // Reset store khi logout (badge của user cũ không được dính sang user mới).
  useEffect(() => {
    if (!isAuthenticated) {
      reset()
    }
  }, [isAuthenticated, reset])
}

// ============================================================================
// Helpers
// ============================================================================

/** Đồng bộ badge từ REST sau khi reconnect. */
async function hydrateUnreadCount(
  setUnreadCount: (count: number) => void,
): Promise<void> {
  try {
    const res = await notificationService.getUnreadCount()
    if (typeof res?.data?.count === 'number') {
      setUnreadCount(res.data.count)
    }
  } catch {
    // Không quan trọng — React Query sẽ refetch theo refetchInterval.
  }
}

/**
 * Refresh access token dùng chung endpoint `/auth/refresh` như `axiosClient`.
 * Trả về token mới, hoặc null nếu không refresh được (khi đó STOMP sẽ tiếp tục
 * retry với token cũ cho tới khi user đăng nhập lại).
 */
function refreshAccessToken(): Promise<string | null> {
  if (refreshing) {
    return refreshing
  }

  const currentToken = storage.getAccessToken()
  if (!currentToken) {
    return Promise.resolve(null)
  }

  refreshing = (async () => {
    try {
      const res = await authService.refresh(currentToken)
      const newToken = res?.data?.token
      if (!newToken) {
        return null
      }
      useAuthStore.getState().updateAccessToken(newToken)
      return newToken
    } catch {
      return null
    } finally {
      refreshing = null
    }
  })()

  return refreshing
}
