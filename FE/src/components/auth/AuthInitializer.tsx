import { useEffect, useRef, type ReactNode } from 'react'
import { storage } from '@/utils/storage'
import { useAuthStore } from '@/stores/useAuthStore'
import {
  decodeJwt,
  extractEmailFromToken,
  extractRolesFromToken,
  extractUserIdFromToken,
} from '@/utils/jwt'
import type { User } from '@/types'

/**
 * AuthInitializer — chạy 1 lần khi app mount.
 *
 * Mục tiêu: giữ user đăng nhập qua reload trang mà KHÔNG cần BE phản hồi
 * thành công (vì mạng chập chờn / BE restart / endpoint lỗi sẽ xoá nhầm
 * session).
 *
 * Ưu tiên:
 * 1. Nếu không có accessToken → set initialized, return.
 * 2. Decode JWT offline (BE là nguồn sự thật về quyền; chỉ dùng để hiển thị UI).
 * 3. Chỉ gọi BE (/auth/introspect) để validate khi cần. Khi BE lỗi mạng /
 *    500 / timeout → GIỮ token, không xoá. Khi token hết hạn (BE trả valid:false)
 *    → mới clearAuth.
 *
 * Lưu ý: thực tế việc token còn hạn hay không vẫn do axiosClient + refresh
 * interceptor xử lý khi user gọi API. AuthInitializer chỉ phục hồi UI state.
 */
export const AuthInitializer = ({ children }: { children: ReactNode }) => {
  const setInitialized = useAuthStore((s) => s.setInitialized)
  const setUser = useAuthStore((s) => s.setUser)
  const ran = useRef(false)

  useEffect(() => {
    if (ran.current) return
    ran.current = true

    const accessToken = storage.getAccessToken()
    if (!accessToken) {
      setInitialized(true)
      return
    }

    // --------------------------------------------------------------
    // Bước 1: phục hồi user từ JWT (offline, tức thì)
    // --------------------------------------------------------------
    const payload = decodeJwt(accessToken)
    const roles = extractRolesFromToken(accessToken)
    const userId = extractUserIdFromToken(accessToken)
    const email = extractEmailFromToken(accessToken)

    // Kiểm tra exp nhanh: nếu token hết hạn rồi → clear & return
    if (payload?.exp && typeof payload.exp === 'number') {
      const nowSec = Math.floor(Date.now() / 1000)
      if (payload.exp <= nowSec) {
        // Token hết hạn: KHÔNG xoá vội, để axiosClient thử refresh.
        // Nếu remember me = false thì request kế tiếp sẽ fail và clear.
        // Ở đây chỉ cần đảm bảo store vẫn còn accessToken để request refresh
        // được gửi đi. KHÔNG clearAuth ở đây.
        setInitialized(true)
        return
      }
    }

    if (userId != null || roles.length > 0) {
      const user: User = {
        id: userId ?? 0,
        email,
        fullName: '',
        role: roles[0] ?? 'STUDENT',
        roles,
      }
      setUser(user)
      setInitialized(true)
      return
    }

    // --------------------------------------------------------------
    // Bước 2: token không decode được (lỗi hiếm gặp) → set initialized
    // để app không kẹt ở loading. KHÔNG clearAuth — để axios xử lý.
    // --------------------------------------------------------------
    setInitialized(true)
  }, [setInitialized, setUser])

  return <>{children}</>
}
