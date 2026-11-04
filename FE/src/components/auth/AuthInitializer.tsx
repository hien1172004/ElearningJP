import { useEffect, useRef, type ReactNode } from 'react'
import { storage } from '@/utils/storage'
import { useAuthStore } from '@/stores/useAuthStore'
import { userService } from '@/services/user.service'
import {
  extractEmailFromToken,
  extractRolesFromToken,
  extractUserIdFromToken,
} from '@/utils/jwt'
import { refreshAccessToken, secondsUntilExpiry } from '@/utils/tokenRefresh'
import type { User } from '@/types'

/** Refresh chủ động trước khi token hết hạn bao nhiêu giây. */
const REFRESH_AHEAD_SECONDS = 60

/** Dựng user tạm từ JWT (offline) — BE vẫn là nguồn sự thật về quyền. */
function userFromToken(token: string): User | null {
  const roles = extractRolesFromToken(token)
  const userId = extractUserIdFromToken(token)
  if (userId == null && roles.length === 0) return null
  return {
    id: userId ?? 0,
    email: extractEmailFromToken(token),
    fullName: '',
    role: roles[0] ?? 'STUDENT',
    roles,
  }
}

/**
 * AuthInitializer — khôi phục phiên đăng nhập khi app mount và giữ phiên sống.
 *
 * 1. Không có accessToken → initialized.
 * 2. Token đã hết hạn → thử `/auth/refresh` (BE cho phép trong cửa sổ refreshable-duration).
 *    Thành công → tiếp tục; thất bại → clearAuth (về trạng thái khách, không bị kẹt
 *    ở trạng thái "đã đăng nhập nhưng không có user").
 * 3. Phục hồi user từ JWT ngay (offline), đồng thời nạp profile đầy đủ từ `/users/me`.
 * 4. Hẹn giờ refresh chủ động trước khi token hết hạn (giữ cả kết nối WebSocket hợp lệ).
 */
export const AuthInitializer = ({ children }: { children: ReactNode }) => {
  const setInitialized = useAuthStore((s) => s.setInitialized)
  const setUser = useAuthStore((s) => s.setUser)
  const accessToken = useAuthStore((s) => s.accessToken)
  const ran = useRef(false)

  // ---------------- Khôi phục phiên khi mount ----------------
  useEffect(() => {
    if (ran.current) return
    ran.current = true

    const restore = async () => {
      let token = storage.getAccessToken()
      if (!token) {
        setInitialized(true)
        return
      }

      const remaining = secondsUntilExpiry(token)
      if (remaining != null && remaining <= 0) {
        try {
          token = await refreshAccessToken()
        } catch {
          useAuthStore.getState().clearAuth()
          setInitialized(true)
          return
        }
      }

      const initialUser = userFromToken(token)
      if (!initialUser) {
        setInitialized(true)
        return
      }
      setUser(initialUser)
      setInitialized(true)

      userService
        .getMyProfile()
        .then((res) => {
          if (res?.data) {
            setUser({
              id: res.data.id,
              email: res.data.email,
              fullName: res.data.fullName,
              avatarUrl: res.data.avatarUrl,
              role: res.data.roles?.[0] ?? initialUser.role,
              roles: res.data.roles ?? initialUser.roles,
              phone: res.data.phoneNumber,
            })
          }
        })
        .catch(() => {
          // Lỗi mạng hoặc server sẽ được interceptor xử lý
        })
    }

    void restore()
  }, [setInitialized, setUser])

  // ---------------- Refresh chủ động trước khi hết hạn ----------------
  useEffect(() => {
    const remaining = secondsUntilExpiry(accessToken)
    if (remaining == null || remaining <= 0) return

    const delayMs = Math.max(remaining - REFRESH_AHEAD_SECONDS, 5) * 1000
    const timer = window.setTimeout(() => {
      // Lỗi (vd hết cửa sổ refresh) để interceptor xử lý ở request kế tiếp
      refreshAccessToken().catch(() => undefined)
    }, delayMs)
    return () => window.clearTimeout(timer)
  }, [accessToken])

  return <>{children}</>
}
