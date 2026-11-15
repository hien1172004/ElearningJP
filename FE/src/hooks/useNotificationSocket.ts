import { createElement, useEffect, useRef } from 'react'
import { Client, type IMessage } from '@stomp/stompjs'
import { useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { getWebSocketUrl } from '@/lib/wsUrl'
import { attachStompClient, detachStompClient } from '@/lib/stompSubscriptions'
import { QUERY_KEYS } from '@/lib/queryKeys'
import { useAuthStore } from '@/stores/useAuthStore'
import { useNotificationStore } from '@/stores/useNotificationStore'
import { storage } from '@/utils/storage'
import { authService } from '@/services/auth.service'
import { NotificationIcon } from '@/components/notification/NotificationIcon'
import { useOpenNotification } from '@/hooks/useOpenNotification'
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
  const reset = useNotificationStore((s) => s.reset)

  const clientRef = useRef<Client | null>(null)

  // Ref để nút 'Xem' trên toast dùng hàm mở mới nhất mà không phải kết nối lại STOMP
  const openNotification = useOpenNotification()
  const openRef = useRef(openNotification)
  useEffect(() => {
    openRef.current = openNotification
  }, [openNotification])

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
        const isReconnect = everConnected
        everConnected = true
        setConnected(true)
        toast.dismiss(RECONNECT_TOAST_ID)

        client.subscribe(NOTIFICATION_DESTINATION, (message: IMessage) => {
          try {
            const notification = JSON.parse(message.body) as AppNotification
            pushNotification(notification)
            // Duyệt theo bài: hàng chờ (ADMIN) / trạng thái bài (giáo viên) đang mở tự cập nhật
            if (notification.type.startsWith('LESSON_') || notification.type === 'DRAFT_STALE') {
              queryClient.invalidateQueries({ queryKey: QUERY_KEYS.lessonReview.all })
            }
            toast(notification.title, {
              description: notification.body,
              duration: 6000,
              icon: createElement(NotificationIcon, { type: notification.type, className: 'w-6 h-6', iconClassName: 'w-3.5 h-3.5' }),
              // Cùng logic mở như khi bấm trong chuông
              action: { label: 'Xem', onClick: () => void openRef.current(notification) },
            })
          } catch {
            // Payload hỏng: bỏ qua, không để listener chết.
          }
        })

        // Topic khác (vd: nội dung khóa học vừa đổi) dùng chung kết nối này
        attachStompClient(client)

        // Chỉ đồng bộ lại khi kết nối LẠI (bù thông báo lỡ lúc mất mạng); lần đầu useUnreadCount đã tải
        // nên không gọi thêm. Badge store được NotificationBell đồng bộ từ kết quả query.
        if (isReconnect) {
          queryClient.invalidateQueries({ queryKey: QUERY_KEYS.notification.all })
        }
      },

      onWebSocketClose: () => {
        detachStompClient()
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
      detachStompClient()
      setConnected(false)
      void client.deactivate()
    }
  }, [
    isAuthenticated,
    pushNotification,
    queryClient,
    setConnected,
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
