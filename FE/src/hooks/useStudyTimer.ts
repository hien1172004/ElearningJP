import { useCallback, useEffect, useRef } from 'react'
import { progressService } from '@/services/progress.service'
import { useAuthStore } from '@/stores/useAuthStore'

/**
 * ============================================================================
 * useStudyTimer — theo dõi thời gian học thật (Bước 3 / Progress page)
 * ============================================================================
 *
 * ## Vấn đề cần giải quyết
 *
 * 1. **Không đếm thời gian chết**: user mở tab rồi đi pha cà phê → không nên tính.
 *    → Dùng idle detection (`IDLE_MS`), reset mỗi khi có tương tác.
 *
 * 2. **Không đếm trùng khi mở nhiều tab**: user mở 3 tab cùng lesson → mỗi tab
 *    gửi heartbeat → thời gian nhân 3.
 *    → Dùng `BroadcastChannel` để chọn **1 tab leader**, chỉ tab leader gửi heartbeat.
 *
 * 3. **Không mất dữ liệu khi đóng tab**: user đóng tab giữa chừng.
 *    → Gửi heartbeat "dang dở" bằng `fetch keepalive` khi `pagehide`.
 *
 * 4. **Tab ẩn không được tính**: `document.hidden = true` → không đếm.
 *
 * ## Cách hoạt động
 *
 * ```
 * ┌─ Tab A (leader) ─┐        ┌─ Tab B ─┐        ┌─ Tab C ─┐
 * │ interval 30s     │        │ nhận    │        │ nhận    │
 * │   ↓              │        │ "TAKE"  │        │ (bật)   │
 * │ heartbeat(30) ───┼────────┼────────>│        │         │
 * │  ...             │        │ bỏ qua │        │ bỏ qua │
 * └──────────────────┘        └─────────┘        └─────────┘
 * ```
 *
 * - Khi tab mới mount, nó bắn `"WHO_IS_LEADER"` lên channel.
 * - Tab leader nhận thấy → trả lời `"I_AM_LEADER"`.
 * - Tab mới nhận `"I_AM_LEADER"` → đánh dấu mình không phải leader.
 * - Nếu sau `LEADER_ELECTION_MS` không ai phản hồi → tự phong leader.
 *
 * ## Gửi heartbeat
 *
 * Mỗi `HEARTBEAT_INTERVAL_MS` (30s), chỉ khi:
 * - là leader
 * - `document.visibilityState === 'visible'`
 * - user chưa idle quá `IDLE_MS` (60s)
 *
 * Gửi số giây = khoảng thời gian từ lần gửi trước (được `Date.now()` chia),
 * clamp về khoảng [1, 60] cho khớp validate của BE.
 */

export interface UseStudyTimerOptions {
  /** Bật/tắt hook (mặc định `true`). */
  enabled?: boolean
  /** Chu kỳ gửi heartbeat (ms). Mặc định 30_000. */
  intervalMs?: number
  /** Sau thời gian không tương tác này (ms) thì coi là idle. Mặc định 60_000. */
  idleMs?: number
  /** Key BroadcastChannel — dùng chung cho cả các trang học. */
  channelName?: string
}

const DEFAULT_INTERVAL_MS = 30_000
const DEFAULT_IDLE_MS = 60_000
const DEFAULT_CHANNEL = 'elearningjp:study-timer'
/** Chờ leader cũ phản hồi bao lâu thì tự phong mình làm leader. */
const LEADER_ELECTION_MS = 800

/** Key lưu sessionId của tab (để sống qua các lần mount/unmount trong SPA). */
const SESSION_ID_STORAGE_KEY = 'elearningjp:study-timer-session-id'

/** Sinh session id ngẫu nhiên cho tab này. */
function createSessionId(): string {
  if (typeof crypto !== 'undefined' && 'randomUUID' in crypto) {
    return crypto.randomUUID()
  }
  return `s-${Date.now()}-${Math.random().toString(36).slice(2, 10)}`
}

/**
 * Lấy sessionId ổn định cho tab hiện tại.
 *
 * <p><b>Vì sao cần:</b> BE dùng Redis key {@code study:hb:{userId}} (TTL 90s) làm
 * "leader lock" — chỉ sessionId trùng với key mới được ghi nhận. Nếu mỗi lần
 * mount component lại tạo sessionId mới, thì khi user điều hướng SPA (Dictionary
 * → Progress → Dictionary) thì session mới sẽ bị từ chối tới 90 giây và
 * {@code studySeconds} không bao giờ được ghi.</p>
 *
 * <p>SessionStorage giữ id theo tab, vẫn khác nhau giữa các tab (đúng như thiết kế).</p>
 */
function getOrCreateSessionId(): string {
  if (typeof window === 'undefined') return createSessionId()
  try {
    const existing = window.sessionStorage.getItem(SESSION_ID_STORAGE_KEY)
    if (existing) return existing
    const fresh = createSessionId()
    window.sessionStorage.setItem(SESSION_ID_STORAGE_KEY, fresh)
    return fresh
  } catch {
    // Trình duyệt chặn storage (private mode) → fallback id trong memory.
    return createSessionId()
  }
}

/** Các sự kiện chứng tỏ user vẫn đang "hoạt động". */
const ACTIVITY_EVENTS = [
  'mousedown',
  'mousemove',
  'keydown',
  'scroll',
  'touchstart',
  'wheel',
] as const

export function useStudyTimer(options: UseStudyTimerOptions = {}) {
  const {
    enabled = true,
    intervalMs = DEFAULT_INTERVAL_MS,
    idleMs = DEFAULT_IDLE_MS,
    channelName = DEFAULT_CHANNEL,
  } = options

  const isAuthenticated = useAuthStore((s) => s.isAuthenticated)

  // Refs để tránh re-create interval mỗi lần render.
  const sessionIdRef = useRef<string>(getOrCreateSessionId())
  const isLeaderRef = useRef(false)
  const lastActivityRef = useRef<number>(Date.now())
  const lastSentAtRef = useRef<number>(Date.now())

  // Đánh dấu hoạt động gần nhất.
  const markActivity = useCallback(() => {
    lastActivityRef.current = Date.now()
  }, [])

  /**
   * Gửi heartbeat cho khoảng thời gian vừa trôi qua.
   *
   * @param viaUnload dùng `fetch keepalive` (khi đóng tab) thay vì axios.
   */
  const sendHeartbeat = useCallback(
    (viaUnload = false) => {
      if (!isAuthenticated) return

      const now = Date.now()
      // Tính số giây đã trôi qua kể từ lần gửi (hoặc mount) trước.
      const elapsedSec = Math.round((now - lastSentAtRef.current) / 1000)

      // Chưa đủ 1 giây → bỏ qua, tránh spam.
      if (elapsedSec < 1) return

      // Clamp về [1, 60] cho khớp validation của BE.
      const seconds = Math.min(60, Math.max(1, elapsedSec))

      const payload = { seconds, sessionId: sessionIdRef.current }

      if (viaUnload) {
        progressService.sendHeartbeatOnUnload(payload)
      } else {
        progressService
          .sendHeartbeat(payload)
          .catch(() => undefined) // fire-and-forget
      }

      lastSentAtRef.current = now
    },
    [isAuthenticated],
  )

  // -------------------------------------------------------------------------
  // Effect chính: thiết lập listener + interval + leader election
  // -------------------------------------------------------------------------
  useEffect(() => {
    if (!enabled || !isAuthenticated) return
    if (typeof window === 'undefined') return

    // Người dùng vừa focus → reset mốc thời gian, tránh tính cả thời gian
    // tab bị ẩn (ví dụ user mở tab khác để đọc tài liệu).
    lastActivityRef.current = Date.now()
    lastSentAtRef.current = Date.now()

    // ---- Activity listeners ----
    ACTIVITY_EVENTS.forEach((evt) =>
      window.addEventListener(evt, markActivity, { passive: true }),
    )

    // ---- Leader election qua BroadcastChannel ----
    let channel: BroadcastChannel | null = null
    if ('BroadcastChannel' in window) {
      channel = new BroadcastChannel(channelName)
      const myId = sessionIdRef.current

      // Tab mới hỏi: "có ai làm leader không?"
      channel.postMessage({ type: 'WHO_IS_LEADER', from: myId })

      channel.onmessage = (event: MessageEvent) => {
        const msg = event.data as { type?: string; from?: string }
        if (!msg || msg.from === myId) return

        if (msg.type === 'WHO_IS_LEADER') {
          // Nếu mình là leader thì đáp lại.
          if (isLeaderRef.current) {
            channel?.postMessage({ type: 'I_AM_LEADER', from: myId })
          }
        } else if (msg.type === 'I_AM_LEADER') {
          // Có tab khác là leader rồi, mình không cạnh tranh.
          isLeaderRef.current = false
        }
      }

      // Nếu sau LEADER_ELECTION_MS không ai đáp, tự phong làm leader.
      // (Trường hợp mở tab đầu tiên, hoặc các tab cũ đã đóng hết.)
      window.setTimeout(() => {
        isLeaderRef.current = true
      }, LEADER_ELECTION_MS)
    } else {
      // Trình duyệt không hỗ trợ BroadcastChannel → luôn là leader.
      isLeaderRef.current = true
    }

    // ---- Interval gửi heartbeat ----
    const intervalId = window.setInterval(() => {
      // Chỉ leader mới gửi.
      if (!isLeaderRef.current) return
      // Tab ẩn → không tính.
      if (document.visibilityState !== 'visible') return
      // Idle quá lâu → không tính (user đi làm việc khác).
      if (Date.now() - lastActivityRef.current > idleMs) return

      sendHeartbeat(false)
    }, intervalMs)

    // ---- Page hidden: reset mốc để không tính thời gian tab ẩn ----
    const handleVisibilityChange = () => {
      if (document.visibilityState === 'visible') {
        markActivity()
        lastSentAtRef.current = Date.now()
      }
    }
    document.addEventListener('visibilitychange', handleVisibilityChange)

    // ---- Đóng tab: gửi nốt phần thời gian còn dư ----
    const handlePageHide = () => {
      if (!isLeaderRef.current) return
      sendHeartbeat(true)
    }
    window.addEventListener('pagehide', handlePageHide)

    // ---- Báo cho tab khác biết mình đã đóng (để chúng giành quyền) ----
    const handleBeforeUnload = () => {
      if (!isLeaderRef.current) return
      channel?.postMessage({ type: 'I_LEFT', from: sessionIdRef.current })
      isLeaderRef.current = false
    }
    window.addEventListener('beforeunload', handleBeforeUnload)

    // ---- Cleanup ----
    return () => {
      window.clearInterval(intervalId)
      ACTIVITY_EVENTS.forEach((evt) =>
        window.removeEventListener(evt, markActivity),
      )
      document.removeEventListener('visibilitychange', handleVisibilityChange)
      window.removeEventListener('pagehide', handlePageHide)
      window.removeEventListener('beforeunload', handleBeforeUnload)

      // Gửi nốt phần thời gian còn dư khi component unmount.
      // `pagehide` chỉ chạy khi đóng tab thật — khi điều hướng SPA
      // (Dictionary → Progress → ...) thì phần thời gian chưa gửi sẽ bị mất,
      // khiến `studySeconds` luôn = 0 nếu user đổi trang thường xuyên.
      if (isLeaderRef.current && document.visibilityState === 'visible') {
        // Bỏ qua nếu user đã idle quá lâu (không tính thời gian chết).
        if (Date.now() - lastActivityRef.current <= idleMs) {
          sendHeartbeat(true)
        }
      }

      // Nếu mình là leader, báo cho tab khác giành quyền ngay.
      if (isLeaderRef.current) {
        channel?.postMessage({ type: 'I_LEFT', from: sessionIdRef.current })
      }
      channel?.close()
      isLeaderRef.current = false
    }
  }, [enabled, isAuthenticated, intervalMs, idleMs, channelName, markActivity, sendHeartbeat])
}