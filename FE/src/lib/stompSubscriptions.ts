import type { Client, IMessage, StompSubscription } from '@stomp/stompjs'

/**
 * Dùng chung kết nối STOMP của {@link useNotificationSocket} cho các topic khác (vd: báo nội dung
 * khóa học vừa đổi) — không mở thêm WebSocket.
 *
 * Component đăng ký handler theo destination bất kỳ lúc nào; khi client (re)connect, registry
 * subscribe lại mọi destination đang có người nghe (subscription cũ mất theo kết nối cũ).
 */
type Handler = (message: IMessage) => void

const handlers = new Map<string, Set<Handler>>()
const active = new Map<string, StompSubscription>()
let client: Client | null = null

const subscribeOnClient = (destination: string) => {
  if (!client?.connected || active.has(destination)) return
  const subscription = client.subscribe(destination, (message) => {
    handlers.get(destination)?.forEach((handler) => {
      try {
        handler(message)
      } catch {
        // Handler lỗi không được làm chết các handler khác
      }
    })
  })
  active.set(destination, subscription)
}

/** Gọi trong onConnect của client (mỗi lần kết nối lại). */
export function attachStompClient(c: Client) {
  client = c
  active.clear()
  handlers.forEach((_, destination) => subscribeOnClient(destination))
}

/** Gọi khi mất kết nối / logout: subscription cũ không còn dùng được. */
export function detachStompClient() {
  client = null
  active.clear()
}

/** Nghe 1 destination; trả về hàm hủy. */
export function subscribeStomp(destination: string, handler: Handler): () => void {
  let set = handlers.get(destination)
  if (!set) {
    set = new Set()
    handlers.set(destination, set)
  }
  set.add(handler)
  subscribeOnClient(destination)

  return () => {
    const current = handlers.get(destination)
    if (!current) return
    current.delete(handler)
    if (current.size === 0) {
      handlers.delete(destination)
      const subscription = active.get(destination)
      active.delete(destination)
      if (client?.connected) {
        try {
          subscription?.unsubscribe()
        } catch {
          // Kết nối vừa đóng: bỏ qua
        }
      }
    }
  }
}
