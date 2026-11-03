import { useEffect } from 'react'
import { useAuthStore } from '@/stores/useAuthStore'
import { subscribeToPush } from '@/lib/webPush'
import { webPushService } from '@/services/webPush.service'

/**
 * useWebPush — tự động đăng ký nhận thông báo đẩy khi user đăng nhập.
 *
 * - Chỉ chạy khi đã đăng nhập (guard bằng `isAuthenticated`).
 * - `subscribeToPush` + POST `/push/subscribe` là idempotent (browser & BE đều dedupe)
 *   nên gọi lại mỗi lần đăng nhập là an toàn, không prompt lại nếu đã cấp quyền.
 */
export function useWebPush() {
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated)

  useEffect(() => {
    if (!isAuthenticated) return

    let cancelled = false
    void (async () => {
      const subscription = await subscribeToPush()
      if (!subscription || cancelled) return
      await webPushService.subscribe(subscription).catch(() => undefined)
    })()

    return () => {
      cancelled = true
    }
  }, [isAuthenticated])
}
