/**
 * Tính URL WebSocket (raw WS, không SockJS).
 *
 * BE mount endpoint handshake ở ROOT `/ws` (không nằm dưới `/api/v1`), trong khi
 * `VITE_API_BASE_URL` mặc định là `http://localhost:8080/api/v1`. Vì vậy không thể
 * nối thẳng base URL API — phải bỏ hậu tố `/api/v1` rồi đổi scheme.
 *
 * Thứ tự ưu tiên:
 *  1. `VITE_WS_URL` — URL đầy đủ, vd `wss://api.example.com/ws`.
 *  2. Suy ra từ `VITE_API_BASE_URL` (bỏ `/api/v1`, http→ws, https→wss).
 *  3. Fallback `ws://localhost:8080/ws`.
 *
 * `@stomp/stompjs` yêu cầu scheme `ws://`/`wss://`; truyền `http://` sẽ không kết nối.
 */
const DEFAULT_WS_URL = 'ws://localhost:8080/ws'

export function getWebSocketUrl(): string {
  const explicit = import.meta.env.VITE_WS_URL as string | undefined
  if (explicit && explicit.trim()) {
    return explicit.trim()
  }

  const apiBase = import.meta.env.VITE_API_BASE_URL as string | undefined
  if (!apiBase || !apiBase.trim()) {
    return DEFAULT_WS_URL
  }

  try {
    const url = new URL(apiBase.trim())
    // Bỏ hậu tố /api/v1 (và mọi phiên bản tương tự) để lấy origin + prefix gốc.
    const pathname = url.pathname.replace(/\/api\/v\d+\/?$/, '').replace(/\/$/, '')
    const scheme = url.protocol === 'https:' ? 'wss:' : 'ws:'
    return `${scheme}//${url.host}${pathname}/ws`
  } catch {
    return DEFAULT_WS_URL
  }
}
