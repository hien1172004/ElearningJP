import { useCallback, useState } from 'react'
import { toast } from 'sonner'
import { authService } from '@/services/auth.service'
import { useAuthStore } from '@/stores/useAuthStore'
import {
  decodeJwt,
  extractEmailFromToken,
  extractRolesFromToken,
  extractUserIdFromToken,
} from '@/utils/jwt'
import { storage } from '@/utils/storage'
import type { User } from '@/types'

/**
 * Hook xử lý đăng nhập bằng Google.
 *
 * Flow:
 *   1. User click "Đăng nhập với Google" trong modal
 *   2. FE redirect tới BE endpoint /auth/google (OAuth2 flow)
 *   3. BE redirect user sang Google consent screen
 *   4. Google redirect về BE /auth/google/callback
 *   5. BE generate JWT rồi redirect về FE: /auth/google/callback?token=...
 *   6. Route handler ở FE đọc token, lưu vào store, chuyển trang
 *
 * Lý do cần 2 hook (useGoogleLogin + useGoogleCallback):
 *   - useGoogleLogin: gọi khi click (ở modal)
 *   - useGoogleCallback: xử lý token từ URL khi BE redirect về
 */
export function useGoogleLogin() {
  const [isLoading] = useState(false)

  const startGoogleLogin = useCallback(() => {
    const url = authService.getGoogleLoginUrl()
    // Lưu redirectTo nếu có để sau khi callback quay lại đúng chỗ.
    // (sẽ được đọc lại ở useGoogleCallback)
    try {
      const pending = sessionStorage.getItem('elearning_google_redirect')
      if (pending) {
        // Đã có → dùng tiếp, không ghi đè
      }
    } catch {
      /* ignore */
    }
    window.location.href = url
  }, [])

  return { startGoogleLogin, isLoading }
}

/**
 * Hook xử lý khi BE redirect về FE với ?token=...
 * Trả về object chứa state + handler để route có thể consume.
 */
export function useGoogleCallback() {
  const setAuth = useAuthStore((s) => s.setAuth)

  const handle = useCallback(
    (token: string | null): { ok: boolean; error?: string } => {
      if (!token) {
        return { ok: false, error: 'Không nhận được token từ Google.' }
      }

      const payload = decodeJwt(token)
      const roles = extractRolesFromToken(token)
      const userId = extractUserIdFromToken(token)
      const email = extractEmailFromToken(token) || payload?.sub || ''

      if (!userId && roles.length === 0) {
        // Token không hợp lệ / không đọc được
        return { ok: false, error: 'Token Google không hợp lệ.' }
      }

      const user: User = {
        id: userId ?? 0,
        email,
        fullName: '',
        role: roles[0] ?? 'STUDENT',
        roles,
      }

      // Lưu Remember flag mặc định = true cho flow Google (UX thân thiện).
      const remember = storage.getRemember() || true

      setAuth({
        user,
        accessToken: token,
        refreshToken: null,
        remember,
      })

      toast.success('Đăng nhập Google thành công', {
        description: 'Chào mừng bạn quay lại với ElearningJP!',
      })

      return { ok: true }
    },
    [setAuth],
  )

  return { handle }
}
