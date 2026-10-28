import { useEffect, useRef, type ReactNode } from 'react'
import { storage } from '@/utils/storage'
import { useAuthStore } from '@/stores/useAuthStore'
import { userService } from '@/services/user.service'
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
 * 3. Đồng thời gọi BE (/users/me) để nạp thêm thông tin chi tiết (fullName, avatarUrl).
 * 4. Chỉ khi token hết hạn mới clearAuth.
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

    // Kiểm tra exp nhanh: nếu token hết hạn rồi
    if (payload?.exp && typeof payload.exp === 'number') {
      const nowSec = Math.floor(Date.now() / 1000)
      if (payload.exp <= nowSec) {
        setInitialized(true)
        return
      }
    }

    if (userId != null || roles.length > 0) {
      const initialUser: User = {
        id: userId ?? 0,
        email,
        fullName: '',
        role: roles[0] ?? 'STUDENT',
        roles,
      }
      setUser(initialUser)
      setInitialized(true)

      // Bước 2: Nạp thông tin đầy đủ từ API /users/me
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
      return
    }

    setInitialized(true)
  }, [setInitialized, setUser])

  return <>{children}</>
}
